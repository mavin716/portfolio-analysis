package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.wise.portfolio.data.MutualFund.FundCategory;

public class PerformanceService {

	// < <Fund symbol>, <Date, Price> >
	private static PortfolioPriceHistory portfolioPriceHistory;

	public static void setPriceHistory(PortfolioPriceHistory priceHistory) {
		portfolioPriceHistory = priceHistory;
	}

	private static Portfolio portfolio;

	public static void setPortfolio(Portfolio portfolio) {
		PerformanceService.portfolio = portfolio;
	}

	public Float getPortfolioTrendByDays(int trendDays) {

		BigDecimal currentValue = portfolio.getTotalValue();
		// TODO get historical value
		BigDecimal historicalValue = portfolio.getTotalValue();

		return currentValue.subtract(historicalValue).divide(historicalValue, 4, RoundingMode.HALF_UP).floatValue();
	}

	public Float getTrendByYear(String symbol, int trendYears) {

		LocalDate today = LocalDate.now();

		// Find the nearest date
		LocalDate date = today.minusYears(trendYears);
		BigDecimal historicalPrice = getClosestHistoricalPrice(symbol, date, 15);
		if (historicalPrice == null) {
			return null;
		}

		BigDecimal currentPrice = portfolio.getTotalValue();
		return currentPrice.subtract(historicalPrice).divide(historicalPrice, 4, RoundingMode.HALF_UP).floatValue();
	}

	public static BigDecimal getValueByDate(String symbol, LocalDate date) {

		BigDecimal value = new BigDecimal(0);

		BigDecimal price = getClosestHistoricalPrice(symbol, date, 10);
		double shares = portfolio.getFund(symbol).getShares();
		if (price != null && shares > 0) {
			value = price.multiply(new BigDecimal(shares));
		}

		return value;
	}

	private static BigDecimal getClosestHistoricalPrice(String symbol, LocalDate date, int days) {
		LocalDate closestHistoricalDate = null;

		int tries = 0;
		while (closestHistoricalDate == null && tries++ < days) {
			BigDecimal historicalValue = portfolioPriceHistory.getPriceByDate(symbol, date.plusDays(tries));
			if (historicalValue != null) {
				return historicalValue;
			}
			historicalValue = portfolioPriceHistory.getPriceByDate(symbol, date.minusDays(tries));
			if (historicalValue != null) {
				return historicalValue;
			}
		}
		return BigDecimal.ZERO;
	}

	public static PortfolioPerformanceData calculatePortfolioPerformanceData(Portfolio portfolio) {

		PortfolioPerformanceData performanceData = new PortfolioPerformanceData();

		performanceData.setPortfolioCurrentValue(portfolio.getTotalValue());
		for (PortfolioFund fund : portfolio.getFundMap().values()) {

			performanceData.portfolioPreviousDayValue = performanceData.portfolioPreviousDayValue
					.add(getValueByDate(fund.getSymbol(), LocalDate.now().minusDays(1)));

			performanceData.portfolioYtdWithdrawals = performanceData.portfolioYtdWithdrawals
					.add(fund.getWithdrawalsUpToDate(getFirstOfYearDate()));
			performanceData.portfolioYtdDividends = performanceData.portfolioYtdDividends
					.add(fund.getDistributionsAfterDate(getFirstOfYearDate()));
			performanceData.portfolioLastYearDividends = performanceData.portfolioLastYearDividends
					.add(fund.getDistributionsBetweenDates(getFirstOfYearDate().minus(1, ChronoUnit.YEARS),
							getFirstOfYearDate().minus(1, ChronoUnit.DAYS)));

			BigDecimal fundFirstOfYearValue = getValueByDate(fund.getSymbol(), getFirstOfYearDate());
			performanceData.portfolioFirstOfYearValue = performanceData.portfolioFirstOfYearValue
					.add(fundFirstOfYearValue);

			BigDecimal fundFirstOfLastYearValue = getValueByDate(fund.getSymbol(), getFirstOfLastYearDate());
			performanceData.portfolioFirstOfLastYearValue = performanceData.portfolioFirstOfLastYearValue
					.add(fundFirstOfLastYearValue);

			BigDecimal fundYearAgoValue = getValueByDate(fund.getSymbol(), LocalDate.now().minusYears(1));
			performanceData.portfolioYearAgoValue = performanceData.portfolioYearAgoValue.add(fundYearAgoValue);

			BigDecimal fundThreeYearAgoValue = getValueByDate(fund.getSymbol(), LocalDate.now().minusYears(3));
			performanceData.portfolioThreeYearAgoValue = performanceData.portfolioThreeYearAgoValue
					.add(fundThreeYearAgoValue);

//			performanceData.portfolioYtdWithdrawals = performanceData.portfolioYtdWithdrawals
//					.add(fund.geWithdrawalsBetweenDates(getFirstOfYearDate(),LocalDate.now()));
//			performanceData.portfolioLastYearWithdrawals = performanceData.portfolioLastYearWithdrawals
//					.add(fund.geWithdrawalsBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate()));
//			performanceData.portfolioYearAgoWithdrawals = performanceData.portfolioYearAgoWithdrawals
//					.add(fund.getWithdrawalsUpToDate(LocalDate.now().minusYears(1)));
//			performanceData.portfolioThreeYearAgoWithdrawals = performanceData.portfolioThreeYearAgoWithdrawals
//					.add(fund.getWithdrawalsUpToDate(LocalDate.now().minusYears(3)));

			performanceData.portfolioTotalCurrentPercentage = performanceData.portfolioTotalCurrentPercentage
					.add(CurrencyHelper.calculatePercentage(fund.getValue(), portfolio.getTotalValue()));
			performanceData.portfolioTotalTargetPercentage = performanceData.portfolioTotalTargetPercentage
					.add(fund.getPercentageByCategory(FundCategory.TOTAL));

		}

		performanceData.portfolioPreviousDayValueChange = portfolio.getTotalValue().subtract(performanceData.portfolioPreviousDayValue)
				.divide(portfolio.getTotalValue(), 4, RoundingMode.HALF_UP);
		performanceData.portfolioYtdValueChange = portfolio.getTotalValue().subtract(performanceData.portfolioYtdValueChange)
				.divide(portfolio.getTotalValue(), 4, RoundingMode.HALF_UP);
		
		performanceData.portfolioYtdFederalWithholding = portfolio
				.getFederalWithholdingBetweenDates(getFirstOfYearDate(), LocalDate.now());
		performanceData.portfolioLastYearFederalWithholding = portfolio
				.getFederalWithholdingBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate().minusDays(1));

		performanceData.portfolioYtdStateWithholding = portfolio.getStateWithholdingBetweenDates(getFirstOfYearDate(),
				LocalDate.now());
		performanceData.portfolioLastYearStateWithholding = portfolio
				.getStateWithholdingBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate().minusDays(1));

		BigDecimal historicalValue = performanceData.portfolioYearAgoValue
				.subtract(performanceData.portfolioYearAgoWithdrawals);
		performanceData.portfolioYearAgoReturns = portfolio.getTotalValue().subtract(historicalValue)
				.divide(historicalValue, 4, RoundingMode.HALF_UP);

		historicalValue = performanceData.portfolioThreeYearAgoValue
				.subtract(performanceData.portfolioThreeYearAgoWithdrawals);
		performanceData.portfolioThreeYearsAgoReturns = portfolio.getTotalValue().subtract(historicalValue)
				.divide(historicalValue, 4, RoundingMode.HALF_UP).divide(new BigDecimal(3), 4, RoundingMode.HALF_UP);
		return performanceData;
	}

	public static LocalDate getFirstOfYearDate() {
		return LocalDate.of(LocalDate.now().getYear(), 1, 1);
	}

	
	public static LocalDate getFirstOfLastYearDate() {
		return LocalDate.of(LocalDate.now().getYear() - 1, 1, 1);
	}

}
