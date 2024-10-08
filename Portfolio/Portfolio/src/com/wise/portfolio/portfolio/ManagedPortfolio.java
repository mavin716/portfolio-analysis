package com.wise.portfolio.portfolio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.wise.portfolio.fund.FundTransaction;
import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.price.PortfolioPriceHistory;
import com.wise.portfolio.service.CurrencyHelper;
import com.wise.portfolio.service.MutualFundPerformance;

public class ManagedPortfolio extends Portfolio {

	protected static final Logger logger = LogManager.getLogger(ManagedPortfolio.class);
	private static final BigDecimal MINIMUM_BUFFER = new BigDecimal(500);
	private Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps = new HashMap<>();
	private List<PortfolioTransaction> portfolioScheduledTransactions = new ArrayList<>();
	private Map<String, List<PortfolioTransaction>> projectedScenarios = new HashMap<>();

	public List<PortfolioTransaction> getPortfolioTransactions() {

		Collections.sort(portfolioScheduledTransactions, new Comparator<PortfolioTransaction>() {
			@Override
			public int compare(PortfolioTransaction o1, PortfolioTransaction o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		return portfolioScheduledTransactions;

	}
	public void addScenario(String scenario, List<PortfolioTransaction> scheduledTransactions) {
		projectedScenarios.put(scenario, scheduledTransactions);
	}

	public List<PortfolioTransaction> getScenarioScheduledTransactions(String scenario) {

		List<PortfolioTransaction> scenarioTransactions = projectedScenarios.get(scenario);
		Collections.sort(scenarioTransactions, new Comparator<PortfolioTransaction>() {
			@Override
			public int compare(PortfolioTransaction o1, PortfolioTransaction o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		return scenarioTransactions;

	}

	public Map<String, Map<FundCategory, BigDecimal>> getDesiredFundAllocationMaps() {
		return desiredFundAllocationMaps;
	}

	public void setDesiredFundAllocationMaps(Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps) {
		this.desiredFundAllocationMaps = desiredFundAllocationMaps;
	}

	public BigDecimal getFundDeviation(PortfolioFund fund) {
		if (fund.isClosed()) {
			return BigDecimal.ZERO;
		}
		if (fund.getCurrentValue().compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal currentPercentage = fund.getCurrentValue().divide(getTotalValue(), 6, RoundingMode.HALF_DOWN);
		BigDecimal targetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);

//		BigDecimal minimumAmount = fund.getMinimumAmount();
//		if (minimumAmount != null) {
//			BigDecimal targetValue = getTotalValue().multiply(targetPercentage);
//			if (targetValue.compareTo(minimumAmount) < 0) {
//				targetPercentage = minimumAmount.divide(getTotalValue(), 6, RoundingMode.HALF_DOWN);
//			}
//		}

		return currentPercentage.subtract(targetPercentage);
	}

	public Map<FundCategory, BigDecimal> getDesiredFundAllocationMap(String symbol) {
		return desiredFundAllocationMaps.get(symbol);
	}

	public BigDecimal getFundDeviation(PortfolioFund fund, BigDecimal portfolioAdjustment) {
		if (fund.isClosed()) {
			return BigDecimal.ZERO;
		}
		BigDecimal totalPortfolioValueAfterAdjustment = getTotalValue().subtract(portfolioAdjustment);
		BigDecimal fundTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);

		if (fund.getMinimumAmount() != null) {
			// If miminum is greater than target amount then use minimum plus cushion of
			// $500 to override fundTargetPercentage
			BigDecimal targetValue = totalPortfolioValueAfterAdjustment.multiply(fundTargetPercentage);
			if (targetValue.compareTo(fund.getMinimumAmount()) < 0) {
				fundTargetPercentage = fund.getMinimumAmount().add(MINIMUM_BUFFER)
						.divide(totalPortfolioValueAfterAdjustment, 6, RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		BigDecimal fundValue = fund.getCurrentValue();
		if (fundValue.compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = fundValue.divide(totalPortfolioValueAfterAdjustment, 6,
					RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}

	public double calculateAnnualizedRateOfReturn(PortfolioFund fund, int years) {

		double rateOfReturn = 0f;

		MutualFundPerformance performance = new MutualFundPerformance(this, fund);
		LocalDate date = LocalDate.now().minusYears(years);
		Double fundReturns = performance.getPerformanceReturnsByDate(date);

		// Ex.
		// (1 + 2.5) ^ 1/5 - 1 = 0.28
		// where 2.5 is total returns for 5 years
		rateOfReturn = Math.pow(1 + fundReturns,
				BigDecimal.ONE.divide(new BigDecimal(years), 4, RoundingMode.HALF_DOWN).doubleValue());

		return rateOfReturn - 1;
	}

	public List<PortfolioFund> getFundsByCategory(FundCategory category) {

		return getFunds().stream()
				.filter(fund -> fund.getCategoriesMap().get(category) != null
						&& fund.getCategoriesMap().get(category).compareTo(BigDecimal.ZERO) > 0)
				.sorted().collect(Collectors.toList());

	}

	public BigDecimal getFundNewBalanceDeviation(PortfolioFund fund, BigDecimal newFundBalance,
			BigDecimal totalWithdrawalAmount) {

		BigDecimal totalAfterWithdrawal = getTotalValue().subtract(totalWithdrawalAmount);
		BigDecimal fundTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);
		BigDecimal fundTargetValue = totalAfterWithdrawal.multiply(fundTargetPercentage);

		if (fund.getMinimumAmount() != null) {
			if (fundTargetValue.compareTo(fund.getMinimumAmount()) < 0) {
				// Don't withdrawal all of the excess
				fundTargetPercentage = fund.getMinimumAmount().add(MINIMUM_BUFFER).divide(totalAfterWithdrawal, 5,
						RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		if (fund.getCurrentValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = newFundBalance.divide(totalAfterWithdrawal, 4, RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}

	public LocalDate getClosestHistoricalDate(long days) {
		LocalDate historicalDate = LocalDate.now().minusDays(days);

		// Find the nearest date
		if (historicalDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			days--;
			historicalDate = historicalDate.minusDays(1);
		}
		if (historicalDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			days--;
			historicalDate = historicalDate.minusDays(1);
		}

		Map<LocalDate, BigDecimal> fundPriceMap = getPriceHistory().getVanguardPriceHistory().get("VFIAX")
				.getFundPricesMap();
		BigDecimal value = fundPriceMap.get(historicalDate);
		if (value == null) {
			int tries = 30;
			while (tries-- > 0) {
				historicalDate = historicalDate.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(historicalDate);
				if (value != null) {
					return historicalDate;
				}
			}
		}
		return LocalDate.now().minusDays(days);

	}

	public BigDecimal getHistoricalValue(PortfolioFund fund, long days) {
		PortfolioPriceHistory priceHistory = getPriceHistory();

		BigDecimal historicalValue = BigDecimal.ZERO;
		historicalValue.setScale(2);

		LocalDate historicalDate = LocalDate.now().minusDays(days);
		BigDecimal historicPrice = priceHistory.getPriceByDate(fund, historicalDate, false);
		if (historicPrice == null) {
			historicPrice = BigDecimal.ZERO;
		}
		Double historicalShares = priceHistory.getSharesByDate(fund, historicalDate);
		if (historicalShares == null) {
			historicalShares = (double) 0;
		}
		historicalValue = historicPrice.multiply(new BigDecimal(historicalShares));

		return historicalValue;
	}

	public BigDecimal getClosestHistoricalPrice(PortfolioFund fund, LocalDate date, int window) {

		PortfolioPriceHistory priceHistory = getPriceHistory();
		BigDecimal historicalPrice = priceHistory.getPriceByDate(fund, date, true);
		if (historicalPrice != null) {
			return historicalPrice;
		}
		int tries = 0;
		while (tries++ < window) {

			historicalPrice = priceHistory.getPriceByDate(fund, date.minusDays(tries), true);
			if (historicalPrice != null) {
				return historicalPrice;
			}
			historicalPrice = priceHistory.getPriceByDate(fund, date.plusDays(tries), true);
			if (historicalPrice != null) {
				return historicalPrice;
			}
		}
		return null;
	}

	public BigDecimal getTotalValueByDate(LocalDate date) {

		BigDecimal totalValueByDate = getFundMap().values().stream().filter(f -> !f.isClosed())
				.map(f -> getValueByDate(f, date)).filter(x -> x != null)
				.reduce(new BigDecimal(0, MathContext.DECIMAL32), (total, fundValue) -> total = total.add(fundValue))
				.setScale(2, RoundingMode.UP);
		return totalValueByDate;
	}

	public BigDecimal getValueByDate(PortfolioFund fund, LocalDate date) {

		PortfolioPriceHistory priceHistory = getPriceHistory();
		double shares = priceHistory.getSharesByDate(fund, date);
		if (shares <= 0) {
			if (fund.getOldFund() != null) {
				PortfolioFund oldFund = fund.getOldFund();
				BigDecimal oldFundValue = getValueByDate(oldFund, date);
//				logger.trace("value for old fund:  " + oldFund.getName() + " for date:  " + date + " value: "
//						+ CurrencyHelper.formatAsCurrencyString(oldFundValue));
				return oldFundValue;
			} else {
				return BigDecimal.ZERO;
			}

		}

		BigDecimal price = priceHistory.getPriceByDate(fund, date, false);
		if (price != null) {
			BigDecimal value = price.multiply(new BigDecimal(shares));
			return value;
		} else {
			logger.debug("Price is null for fund:  " + fund.getName() + " for date:  " + date);
			if (fund.getOldFund() != null) {
				PortfolioFund oldFund = fund.getOldFund();
				BigDecimal oldFundValue = getValueByDate(oldFund, date);
				logger.trace("value for old fund:  " + oldFund.getName() + " for date:  " + date + " value: "
						+ CurrencyHelper.formatAsCurrencyString(oldFundValue));
				return oldFundValue;
			} else {
				return BigDecimal.ZERO;
			}
		}

	}

	public BigDecimal getTargetValue(String fundSymbol) {

		BigDecimal targetPercentage = getTargetPercentage(fundSymbol);

		return this.getTotalValue().multiply(targetPercentage);
	}

	private BigDecimal getTargetPercentage(String fundSymbol) {
		return desiredFundAllocationMaps.get(fundSymbol).get(FundCategory.TOTAL);
	}

	public List<Entry<LocalDate, FundTransaction>> getRecentTransactions(List<String> transactionTypes, long days) {

		List<Entry<LocalDate, FundTransaction>> transactions = new ArrayList<>();

		LocalDate startDate = LocalDate.now().minusDays(days);
		LocalDate endDate = LocalDate.now();
		for (PortfolioFund fund : getFundMap().values()) {
			for (Entry<LocalDate, FundTransaction> transactionEntry : fund.getTransactionsBetweenDates(startDate,
					endDate)) {

				if (transactionTypes != null) {
					String transactionType = transactionEntry.getValue().getTransactionType();
					if (transactionTypes.contains(transactionType)) {
						transactions.add(transactionEntry);
					}
				} else {
					transactions.add(transactionEntry);

				}
			}
		}
		return transactions;

	}

	public BigDecimal getRecentWithdrawalAmount(int days) {

		BigDecimal recentWithdrawalTotal = BigDecimal.ZERO;

		List<String> transactionTypes = new ArrayList<String>();
		transactionTypes.add("Sell");
		List<Entry<LocalDate, FundTransaction>> transactions = getRecentTransactions(transactionTypes, days);

		for (Entry<LocalDate, FundTransaction> entry : transactions) {
			FundTransaction transaction = entry.getValue();
			recentWithdrawalTotal = recentWithdrawalTotal.add(transaction.getTransastionPrincipal());
		}
		return recentWithdrawalTotal;
	}

	public void addPortfolioScheduledTransaction(PortfolioTransaction portfolioTransaction) {
		portfolioScheduledTransactions.add(portfolioTransaction);

	}

	public BigDecimal getWithdrawalsUpToDate(LocalDate historicalDate) {
		BigDecimal withdrawals = BigDecimal.ZERO;

//		getFunds().stream()
//				.filter(fund -> fund.getWithdrawalsUpToDate(historicalDate).reduce(BigDecimal.ZERO, BigDecimal::add));

		return withdrawals;
	}

	public BigDecimal getValueByDate(LocalDate graphDate, List<String> fundSymbols) {
		BigDecimal totalValueByDate = getFundMap().values().stream().filter(f -> fundSymbols.contains(f.getSymbol()))
				.map(f -> getValueByDate(f, graphDate)).filter(value -> value != null)
				.reduce(new BigDecimal(0, MathContext.DECIMAL32), (total, fundValue) -> total = total.add(fundValue))
				.setScale(2, RoundingMode.UP);
		return totalValueByDate;
	}

	public List<PortfolioFund> getFunds(List<String> fundSymbols) {
		return getFunds().stream().filter(fund -> fundSymbols.contains(fund.getSymbol())).sorted()
				.collect(Collectors.toList());
	}

}
