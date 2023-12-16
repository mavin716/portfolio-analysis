package com.wise.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.wise.portfolio.fund.Fund;
import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;

public class MutualFundPerformance {

	private Portfolio portfolio;
	private PortfolioPriceHistory portfolioPriceHistory;
	private PortfolioFund portfolioFund;

	public MutualFundPerformance(Portfolio portfolio, PortfolioFund fund) {
		super();

		this.portfolio = portfolio;
		this.portfolioPriceHistory = portfolio.getPriceHistory();
		this.portfolioFund = fund;

		BigDecimal beginYearPrice = getClosestHistoricalPrice(getFirstOfYearDate(), 30);
		if (beginYearPrice == null) {
			return;
		}
		setCurrentSharePrice(fund.getCurrentPrice());
		setYtdChange(
				CurrencyHelper.calculatePercentage(fund.getCurrentPrice().subtract(beginYearPrice), beginYearPrice));
		Map<FundCategory, BigDecimal> currentPercentageOfTotal = new HashMap<>();
		setCurrentPercentageTotal(currentPercentageOfTotal);

	}

	public Double getPerformanceRateByDate(LocalDate historicalDate) {

		BigDecimal historicalPrice = portfolioPriceHistory.getPriceByDate(portfolioFund, historicalDate, false);
		Double historicalShares = portfolioPriceHistory.getSharesByDate(portfolioFund, historicalDate, false);
		BigDecimal historicalValue = portfolioPriceHistory.getFundValueByDate(portfolioFund, historicalDate, false);
		BigDecimal returns = getReturnsByDate(historicalDate, false);
		if (historicalPrice == null || historicalShares == null || historicalValue == null || returns == null) {
			return Double.valueOf(0);
		}

		if (historicalPrice == null || historicalPrice.compareTo(BigDecimal.ZERO) == 0) {
			// check for linked account
			PortfolioFund oldFund = portfolioFund.getOldFund(portfolio);
			if (oldFund != null) {
				historicalPrice = portfolioPriceHistory.getPriceByDate(oldFund, portfolioFund.getOldFundConverted(),
						false);
				if (historicalPrice == null) {
					return Double.valueOf(0);
				}
				historicalShares = oldFund.getConversionsSharesUpToDate(historicalDate);
				// will be conversion out so negative shares
				historicalShares = 1 - historicalShares;
			} else {
				return Double.valueOf(0);
			}
		}

		BigDecimal currentValue = portfolioFund.getValue();
		if (currentValue.intValue() == 0 || historicalValue.intValue() == 0) {
			return Double.valueOf(0);
		}

		// Adjust historical value
		BigDecimal withdrawals = portfolioFund.getWithdrawalsUpToDate(historicalDate);
		BigDecimal exchanges = portfolioFund.getExchangeTotalFromDate(historicalDate);
		BigDecimal converions = portfolioFund.getConversionsTotalFromDate(getFirstOfYearDate());
		historicalValue = historicalValue.subtract(withdrawals).subtract(exchanges).add(converions);

		Double rate = currentValue.subtract(historicalValue)
				.divide(currentValue, PortfolioService.CURRENCY_SCALE, RoundingMode.HALF_UP).doubleValue();
		return rate;

	}

	private static LocalDate getFirstOfYearDate() {
		LocalDate firstOfYearDate = LocalDate.of(LocalDate.now().getYear(), 1, 1);

		if (firstOfYearDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			firstOfYearDate = firstOfYearDate.minusDays(2);
		}
		if (firstOfYearDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			firstOfYearDate = firstOfYearDate.minusDays(1);
		}
		return firstOfYearDate;
	}

	public BigDecimal getClosestHistoricalPrice(LocalDate date, int days) {

		BigDecimal historicalPrice = portfolioPriceHistory.getPriceByDate(portfolioFund, date, true);
		if (historicalPrice != null) {
			return historicalPrice;
		}
		int tries = 0;
		while (tries++ < days) {

			historicalPrice = portfolioPriceHistory.getPriceByDate(portfolioFund, date.minusDays(tries), true);
			if (historicalPrice != null) {
				return historicalPrice;
			}
			historicalPrice = portfolioPriceHistory.getPriceByDate(portfolioFund, date.plusDays(tries), true);
			if (historicalPrice != null) {
				return historicalPrice;
			}
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal getDeviation(FundCategory category) {

		BigDecimal currentPercentage = portfolioFund.getValue()
				.multiply(portfolioFund.getPercentageByCategory(category))
				.divide(portfolio.getTotalValue(), 6, RoundingMode.HALF_DOWN);

		BigDecimal fundTotalPercentage = portfolioFund.getPercentageByCategory(FundCategory.TOTAL);
		BigDecimal fundCategoryPercentage = portfolioFund.getPercentageByCategory(category);
		BigDecimal targetCategoryPercentage = fundCategoryPercentage.multiply(fundTotalPercentage).setScale(4,
				RoundingMode.UP);

		BigDecimal deviation = currentPercentage.subtract(targetCategoryPercentage);

		return deviation;
	}

	public BigDecimal getSurplusDeficit(FundCategory category) {

		BigDecimal fundTotalPercentage = portfolioFund.getPercentageByCategory(FundCategory.TOTAL);
		BigDecimal fundCategoryPercentage = portfolioFund.getPercentageByCategory(category);
		BigDecimal targetCategoryPercentage = fundCategoryPercentage.multiply(fundTotalPercentage).setScale(4,
				RoundingMode.UP);

		BigDecimal diffValue = portfolioFund.getValue().multiply(fundCategoryPercentage)
				.subtract(portfolio.getTotalValue().multiply(targetCategoryPercentage));

		return diffValue;
	}

	public PortfolioFund getPortfolioFund() {
		return portfolioFund;
	}

	public void setPortfolioFund(PortfolioFund portfolioFund) {
		this.portfolioFund = portfolioFund;
	}

	public Pair<LocalDate, BigDecimal> getMinPricePair() {
		return portfolioPriceHistory.getMinPriceFromDate(portfolioFund, portfolioPriceHistory.getOldestDate());
	}

	public Pair<LocalDate, BigDecimal> getMinPricePairFromDate(LocalDate date) {
		return portfolioPriceHistory.getMinPriceFromDate(portfolioFund, date);
	}

	public Pair<LocalDate, BigDecimal> getMaxPricePair() {
		return portfolioPriceHistory.getMaxPriceFromDate(portfolioFund, portfolioPriceHistory.getOldestDate());
	}

	public Pair<LocalDate, BigDecimal> getMaxPricePairFromDate(LocalDate date) {
		return portfolioPriceHistory.getMaxPriceFromDate(portfolioFund, date);
	}

	public BigDecimal getYtdChange() {
		return ytdChange;
	}

	public void setYtdChange(BigDecimal ytdPriceChange) {
		this.ytdChange = ytdPriceChange;
	}

	public BigDecimal getYtdDividends() {
		return ytdDividends;
	}

	public void setYtdDividends(BigDecimal ytdDividends) {
		this.ytdDividends = ytdDividends;
	}

	public BigDecimal getCurrentSharePrice() {
		return currentSharePrice;
	}

	public void setCurrentSharePrice(BigDecimal currentSharePrice) {
		this.currentSharePrice = currentSharePrice;
	}

	public Map<FundCategory, BigDecimal> getCurrentPercentageTotal() {
		return currentPercentageTotal;
	}

	public void setCurrentPercentageTotal(Map<FundCategory, BigDecimal> currentPercentageTotal) {
		this.currentPercentageTotal = currentPercentageTotal;
	}

	public Map<FundCategory, BigDecimal> getTargetPercentageTotal() {
		return targetPercentageTotal;
	}

	public void setTargetPercentageTotal(Map<FundCategory, BigDecimal> targetPercentageTotal) {
		this.targetPercentageTotal = targetPercentageTotal;
	}

//	private BigDecimal firstOfYearPrice;
	private BigDecimal ytdChange;
	private BigDecimal ytdDividends;
	private BigDecimal currentSharePrice;
	private Map<FundCategory, BigDecimal> currentPercentageTotal;
	private Map<FundCategory, BigDecimal> targetPercentageTotal;
//	private Map<FundCategory, BigDecimal> targetValue;

	public BigDecimal getDayPriceChange() {

		LocalDate lastBusinessDay = LocalDate.now();
		if (LocalTime.now().getHour() < 18) {
			lastBusinessDay = lastBusinessDay.minusDays(1);
		}

		if (lastBusinessDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
			lastBusinessDay = lastBusinessDay.minusDays(1);
		}
		if (lastBusinessDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
			lastBusinessDay = lastBusinessDay.minusDays(1);
		}
		LocalDate previousBusinessDay = lastBusinessDay.minusDays(1);
		if (previousBusinessDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
			previousBusinessDay = previousBusinessDay.minusDays(1);
		}
		if (previousBusinessDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
			previousBusinessDay = previousBusinessDay.minusDays(1);
		}

		BigDecimal previousPrice = getPriceByDate(portfolioFund, previousBusinessDay, true);
		BigDecimal currentPrice = portfolioFund.getCurrentPrice();
		BigDecimal dayPriceChange = BigDecimal.ZERO;
		if (previousPrice != null) {
			dayPriceChange = CurrencyHelper.calculatePercentage(currentPrice.subtract(previousPrice), previousPrice);
		}
		return dayPriceChange;
	}

	public BigDecimal getYtdPriceChange() {
		BigDecimal ytdPriceChange = CurrencyHelper.calculatePercentage(
				portfolioFund.getCurrentPrice().subtract(getFirstOfYearPrice()), getFirstOfYearPrice());
		return ytdPriceChange;
	}

	public BigDecimal getFirstOfYearPrice() {
		// TODO Auto-generated method stub
		return getClosestHistoricalPrice(getFirstOfYearDate(), 5);
	}

	public BigDecimal getYtdValueChange() {
		BigDecimal withdrawals = portfolioFund.getWithdrawalsUpToDate(getFirstOfYearDate());
		BigDecimal exchanges = portfolioFund.getExchangeTotalFromDate(getFirstOfYearDate());
		BigDecimal converions = portfolioFund.getConversionsTotalFromDate(getFirstOfYearDate());
		BigDecimal historicalValue = getHistoricalValue(portfolioFund, getYtdDays() + 2);
		BigDecimal ytdValueChange = portfolioFund.getValue().subtract(historicalValue).add(withdrawals).add(exchanges)
				.subtract(converions);
		return ytdValueChange;
	}

	private static int getYtdDays() {
		Period period = getFirstOfYearDate().until(LocalDate.now());
		Integer ytdDays = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
		return ytdDays;
	}

	private BigDecimal getHistoricalValue(PortfolioFund fund, int days) {
		BigDecimal historicalValue = BigDecimal.ZERO;

		LocalDate historicalDate = LocalDate.now().minusDays(days);
		BigDecimal historicPrice = getPriceByDate(fund, historicalDate, false);
		if (historicPrice == null) {
			historicPrice = BigDecimal.ZERO;
		}
		Double historicalShares = getSharesByDate(fund, historicalDate, false);
		if (historicalShares == null) {
			historicalShares = (double) 0;
		}
		historicalValue = historicPrice.multiply(new BigDecimal(historicalShares));

		return historicalValue;
	}

	public BigDecimal getPriceByDate(Fund fund, LocalDate date, boolean isExactDate) {
		BigDecimal value = null;

		Map<LocalDate, BigDecimal> fundPriceMap = portfolioPriceHistory.getVanguardPriceHistory().get(fund.getSymbol()).getFundPricesMap();
		value = fundPriceMap.get(date);
		if (value == null && !isExactDate) {
			int tries = 30;
			while (tries-- > 0) {
				date = date.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(date);
				if (value != null) {
					return value;
				}
			}
		}

		return value;
	}

	public Double getSharesByDate(Fund fund, LocalDate date, boolean isExactDate) {
		Double value;

		Map<LocalDate, Double> fundSharesMap = portfolioPriceHistory.getFundShares().get(fund.getSymbol());
		value = fundSharesMap.get(date);
		if (!isExactDate) {
			int tries = 30;
			while (tries-- > 0) {
				value = fundSharesMap.get(date);
				if (value != null) {
					return value;
				}
				date = date.minus(1, ChronoUnit.DAYS);
			}
			return (double) 0;
		}

		// used by some functions expecting number so for now just return zero
		return null;
	}

	// TODO Returns are the increase/decrease in value of the fund, not the raw
	// difference.
	public BigDecimal getReturnsByDate(LocalDate date, boolean isExactDate) {

		Map<LocalDate, BigDecimal> fundReturnMap = portfolioPriceHistory.getFundReturnsMap()
				.get(portfolioFund.getSymbol());
		if (fundReturnMap == null) {
			fundReturnMap = new HashMap<>();
			portfolioPriceHistory.getFundReturnsMap().put(portfolioFund.getSymbol(), fundReturnMap);
		}
		BigDecimal returns = fundReturnMap.get(date);
//		if (returns == null) {
		returns = BigDecimal.ZERO;
		BigDecimal conversions = portfolioFund.getConversionsUpToDate(date);
		BigDecimal withdrawals = portfolioFund.getWithdrawalsUpToDate(date);
		BigDecimal exchanges = portfolioFund.getExchangeTotalFromDate(date);
		BigDecimal dividends = portfolioFund.getDistributionsAfterDate(date);
		BigDecimal currentValue = portfolioFund.getValue();
		BigDecimal historicalValue = portfolioPriceHistory.getFundValueByDate(portfolioFund, date, isExactDate);
		if (currentValue != null && historicalValue != null) {
			returns = currentValue.subtract(historicalValue).add(dividends).add(withdrawals).add(exchanges);
				returns = currentValue.subtract(historicalValue).add(dividends).add(withdrawals).add(exchanges).subtract(conversions);
//				System.out.println("fund:  " + portfolioFund.getShortName() + " date:  " + date);
//				System.out.println("currentValue:  " + CurrencyHelper.formatAsCurrencyString(currentValue));
//				System.out.println("historicalValue:  " + CurrencyHelper.formatAsCurrencyString(historicalValue));
//				System.out.println("historical shares:  " + portfolioPriceHistory.getSharesByDate(portfolioFund, date, isExactDate));
//				System.out.println("historical price  " + CurrencyHelper.formatAsCurrencyString(portfolioPriceHistory.getPriceByDate(portfolioFund, date, isExactDate)));
//				System.out.println("withdrawals:  " + CurrencyHelper.formatAsCurrencyString(withdrawals));
//				System.out.println("conversions:  " + CurrencyHelper.formatAsCurrencyString(conversions));
//				System.out.println("exchanges:  " + CurrencyHelper.formatAsCurrencyString(exchanges));
//				System.out.println("dividends:  " + CurrencyHelper.formatAsCurrencyString(dividends));
//				System.out.println("returns:  " + CurrencyHelper.formatAsCurrencyString(returns));
		} else {
//				System.out.println("currentValue:  " + CurrencyHelper.formatAsCurrencyString(currentValue));
//				System.out.println("historicalValue:  " + CurrencyHelper.formatAsCurrencyString(historicalValue));

		}

		fundReturnMap.put(date, returns);
//		} else {
//			System.out
//					.println("cached returns:  " + CurrencyHelper.formatAsCurrencyString(returns) + " date:  " + date);

//		}

		return returns;
	}

	public PortfolioFund getFund() {
		return portfolioFund;
	}

	public BigDecimal getDividendsFromDate(LocalDate historicalDate) {
		// TODO Auto-generated method stub
		return null;
	}

	public LocalDate getOldestDate() {
		// TODO Auto-generated method stub
		return portfolioPriceHistory.getOldestDate();
	}

}
