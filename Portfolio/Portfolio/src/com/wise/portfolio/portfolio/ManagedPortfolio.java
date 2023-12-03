package com.wise.portfolio.portfolio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.fund.FundTransaction;
import com.wise.portfolio.service.MutualFundPerformance;
import com.wise.portfolio.service.PortfolioPriceHistory;

public class ManagedPortfolio extends Portfolio {

	private Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps = new HashMap<>();
	private List<PortfolioTransaction> portfolioTransactions = new ArrayList<>();

	public List<PortfolioTransaction> getPortfolioTransactions() {
		return portfolioTransactions;
	}

	public Map<String, Map<FundCategory, BigDecimal>> getDesiredFundAllocationMaps() {
		return desiredFundAllocationMaps;
	}

	public void setDesiredFundAllocationMaps(Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps) {
		this.desiredFundAllocationMaps = desiredFundAllocationMaps;
	}

	public BigDecimal getFundDeviation(PortfolioFund fund) {
		if (fund.isClosed()) {
			//return BigDecimal.ZERO;
		}
		if (fund.getValue().compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal currentPercentage = fund.getValue().divide(getTotalValue(), 6, RoundingMode.HALF_DOWN);
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
			//return BigDecimal.ZERO;
		}
		BigDecimal totalPortfolioValueAfterAdjustment = getTotalValue().subtract(portfolioAdjustment);
		BigDecimal fundTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);

		if (fund.getMinimumAmount() != null) {
			// If miminum is greater than target amount then use minimum plus cushion of
			// $500 to override fundTargetPercentage
			BigDecimal targetValue = totalPortfolioValueAfterAdjustment.multiply(fundTargetPercentage);
			if (targetValue.compareTo(fund.getMinimumAmount()) < 0) {
				fundTargetPercentage = fund.getMinimumAmount().add(new BigDecimal(500))
						.divide(totalPortfolioValueAfterAdjustment, 6, RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		if (fund.getValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = fund.getValue().divide(totalPortfolioValueAfterAdjustment, 6,
					RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}

	public double calculateAnnualizedReturn(PortfolioFund fund, int years) {
		// (1 + 2.5) ^ 1/5 - 1 = 0.28
		double returns = 0f;
		MutualFundPerformance performance = new MutualFundPerformance(this, fund);
		LocalDate date = LocalDate.now().minusYears(years);
		Double fundReturns = performance.getPerformanceRateByDate(date);
		returns = Math.pow(1 + fundReturns,
				BigDecimal.ONE.divide(new BigDecimal(years), 4, RoundingMode.HALF_DOWN).doubleValue());

		return returns - 1;
	}

	public List<PortfolioFund> getFundsByCategory(FundCategory category) {

		return getFundMap().values().stream()
				.filter(fund -> fund.getCategoriesMap().get(category) != null && fund.getCategoriesMap().get(category).compareTo(BigDecimal.ZERO) > 0).sorted()
				.collect(Collectors.toList());

	}

	public BigDecimal getFundNewBalanceDeviation(PortfolioFund fund, BigDecimal newFundBalance,
			BigDecimal totalWithdrawalAmount) {

		BigDecimal totalAfterWithdrawal = getTotalValue().subtract(totalWithdrawalAmount);
		BigDecimal fundTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);
		BigDecimal fundTargetValue = totalAfterWithdrawal.multiply(fundTargetPercentage);

		if (fund.getMinimumAmount() != null) {
			if (fundTargetValue.compareTo(fund.getMinimumAmount()) < 0) {
				// Don't withdrawal all of the excess
				fundTargetPercentage = fund.getMinimumAmount().add(new BigDecimal(500)).divide(totalAfterWithdrawal, 4,
						RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		if (fund.getValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = newFundBalance.divide(totalAfterWithdrawal, 4, RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}

	public LocalDate getClosestHistoricalDate(long rankDays) {
		LocalDate historicalDate = LocalDate.now().minusDays(rankDays);

		// Find the nearest date
		if (historicalDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			rankDays--;
			historicalDate = historicalDate.minusDays(1);
		}
		if (historicalDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			rankDays--;
			historicalDate = historicalDate.minusDays(1);
		}

		Map<LocalDate, BigDecimal> fundPriceMap = getPriceHistory().getFundPrices().get("VFIAX");
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
		return LocalDate.now().minusDays(rankDays);

	}

	public BigDecimal getHistoricalValue(PortfolioFund fund, long days) {
		PortfolioPriceHistory priceHistory = getPriceHistory();

		BigDecimal historicalValue = BigDecimal.ZERO;

		LocalDate historicalDate = LocalDate.now().minusDays(days);
		BigDecimal historicPrice = priceHistory.getPriceByDate(fund, historicalDate, false);
		if (historicPrice == null) {
			historicPrice = BigDecimal.ZERO;
		}
		Double historicalShares = priceHistory.getSharesByDate(fund, historicalDate, false);
		if (historicalShares == null) {
			historicalShares = (double) 0;
		}
		historicalValue = historicPrice.multiply(new BigDecimal(historicalShares));

		return historicalValue;
	}

	public BigDecimal getClosestHistoricalPrice(PortfolioFund fund, LocalDate date, int days) {

		PortfolioPriceHistory priceHistory = getPriceHistory();
		BigDecimal historicalValue = priceHistory.getPriceByDate(fund, date, true);
		if (historicalValue != null) {
			return historicalValue;
		}
		int tries = 0;
		while (tries++ < days) {

			historicalValue = priceHistory.getPriceByDate(fund, date.minusDays(tries), true);
			if (historicalValue != null) {
				return historicalValue;
			}
			historicalValue = priceHistory.getPriceByDate(fund, date.plusDays(tries), true);
			if (historicalValue != null) {
				return historicalValue;
			}
		}
		return null;
	}

	public BigDecimal getTotalValueByDate(LocalDate date) {
		BigDecimal totalValueByDate = null;
		totalValueByDate = getFundMap().values().stream().map(f -> getValueByDate(f, date)).filter(x -> x != null)
				.reduce(new BigDecimal(0, MathContext.DECIMAL32), (total, fundValue) -> total = total.add(fundValue))
				.setScale(2, BigDecimal.ROUND_UP);
		return totalValueByDate;
	}

	public BigDecimal getValueByDate(PortfolioFund fund, LocalDate date) {

		if (fund.isClosed()) {
			return BigDecimal.ZERO;
		}
		PortfolioPriceHistory priceHistory = getPriceHistory();
		BigDecimal value = new BigDecimal(0);

		BigDecimal price = priceHistory.getPriceByDate(fund, date, false);
		double shares = priceHistory.getSharesByDate(fund, date, false);
		if (shares <= 0) {
			return value;
		}
		int tries = 30;
		while (tries-- > 0) {
			if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
				value = price.multiply(new BigDecimal(shares));
				if (value != null) {
					return value;
				}
			}
			date = date.minus(1, ChronoUnit.DAYS);
			price = priceHistory.getPriceByDate(fund, date, true);
		}

		return value;
	}

	public BigDecimal getTargetValue(String fundSynbol) {
		BigDecimal targetPercentage = desiredFundAllocationMaps.get(fundSynbol).get(FundCategory.TOTAL);
		return this.getTotalValue().multiply(targetPercentage);
	}

	public List<Entry<LocalDate, FundTransaction>> getRecentTransactions(List<String> transactionTypes, int days) {

		LocalDate startDate = LocalDate.now().minusDays(days);
		List<Entry<LocalDate, FundTransaction>> transactions = new ArrayList<>();
		for (PortfolioFund fund : getFundMap().values()) {
			for (Entry<LocalDate, FundTransaction> transactionEntry : fund.getTransactionsBetweenDates(startDate,
					LocalDate.now())) {
				if (transactionTypes != null) {
					for (String filterdType : transactionTypes) {
						if (transactionEntry.getValue().getTransactionType().contains(filterdType)) {
							transactions.add(transactionEntry);
							break;
						}
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
		List<String> transactionTypes = new ArrayList<>();
		transactionTypes.add("Sell");
		List<Entry<LocalDate, FundTransaction>> transactions = getRecentTransactions(transactionTypes, days);
		for (Entry<LocalDate, FundTransaction> entry : transactions) {
			FundTransaction transaction = entry.getValue();
			recentWithdrawalTotal = recentWithdrawalTotal.add(transaction.getTransastionPrincipal());
		}
		return recentWithdrawalTotal;
	}

	public void addPortfolioTransaction(PortfolioTransaction portfolioTransaction) {
		portfolioTransactions.add(portfolioTransaction);
		
	}

}
