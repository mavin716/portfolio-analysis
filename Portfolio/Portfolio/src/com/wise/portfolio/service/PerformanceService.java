package com.wise.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;

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
		return currentPrice.subtract(historicalPrice).divide(historicalPrice, 6, RoundingMode.HALF_UP).floatValue();
	}

	public static BigDecimal getValueByDate(String symbol, LocalDate date) {

		BigDecimal value = new BigDecimal(0);

		BigDecimal historicalPrice = getClosestHistoricalPrice(symbol, date, 10);
		double historicalShares = portfolioPriceHistory.getSharesByDate(portfolio.getFund(symbol), date, false);
		if (historicalPrice != null && historicalShares > 0) {
			value = historicalPrice.multiply(new BigDecimal(historicalShares));
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

//			if (fund.isClosed()) {
//				continue;
//			}
			performanceData.setPortfolioPreviousDayValue(performanceData.getPortfolioPreviousDayValue()
					.add(getValueByDate(fund.getSymbol(), LocalDate.now().minusDays(1))));

			performanceData.setPortfolioYtdWithdrawals(performanceData.getPortfolioYtdWithdrawals()
					.add(fund.getWithdrawalsUpToDate(getFirstOfYearDate())));
			performanceData.setPortfolioYtdDividends(performanceData.getPortfolioYtdDividends()
					.add(fund.getDistributionsAfterDate(getFirstOfYearDate())));
			performanceData.setPortfolioLastYearDividends(performanceData.getPortfolioLastYearDividends()
					.add(fund.getDistributionsBetweenDates(getFirstOfYearDate().minus(1, ChronoUnit.YEARS),
							getFirstOfYearDate().minus(1, ChronoUnit.DAYS))));

			BigDecimal fundFirstOfYearValue = getValueByDate(fund.getSymbol(), getFirstOfYearDate());
			performanceData.setPortfolioFirstOfYearValue(
					performanceData.getPortfolioFirstOfYearValue().add(fundFirstOfYearValue));

			BigDecimal fundFirstOfLastYearValue = getValueByDate(fund.getSymbol(), getFirstOfLastYearDate());
			performanceData.setPortfolioFirstOfLastYearValue(
					performanceData.getPortfolioFirstOfLastYearValue().add(fundFirstOfLastYearValue));

			BigDecimal fundYearAgoValue = getValueByDate(fund.getSymbol(), LocalDate.now().minusYears(1));
			performanceData.setPortfolioYearAgoValue(performanceData.getPortfolioYearAgoValue().add(fundYearAgoValue));

			BigDecimal fundThreeYearAgoValue = getValueByDate(fund.getSymbol(), LocalDate.now().minusYears(3));
			performanceData.setPortfolioThreeYearAgoValue(
					performanceData.getPortfolioThreeYearAgoValue().add(fundThreeYearAgoValue));

//			performanceData.portfolioYtdWithdrawals = performanceData.portfolioYtdWithdrawals
//					.add(fund.geWithdrawalsBetweenDates(getFirstOfYearDate(),LocalDate.now()));
//			performanceData.portfolioLastYearWithdrawals = performanceData.portfolioLastYearWithdrawals
//					.add(fund.geWithdrawalsBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate()));
//			performanceData.portfolioYearAgoWithdrawals = performanceData.portfolioYearAgoWithdrawals
//					.add(fund.getWithdrawalsUpToDate(LocalDate.now().minusYears(1)));
//			performanceData.portfolioThreeYearAgoWithdrawals = performanceData.portfolioThreeYearAgoWithdrawals
//					.add(fund.getWithdrawalsUpToDate(LocalDate.now().minusYears(3)));

			performanceData.setPortfolioTotalCurrentPercentage(performanceData.getPortfolioTotalCurrentPercentage()
					.add(CurrencyHelper.calculatePercentage(fund.getValue(), portfolio.getTotalValue())));
			performanceData.setPortfolioTotalTargetPercentage(performanceData.getPortfolioTotalTargetPercentage()
					.add(fund.getPercentageByCategory(FundCategory.TOTAL)));

		}

		performanceData.setPortfolioPreviousDayValueChange(
				portfolio.getTotalValue().subtract(performanceData.getPortfolioPreviousDayValue()));
		performanceData.setPortfolioYtdValueChange(
				portfolio.getTotalValue().add(performanceData.getPortfolioYtdWithdrawals())
						.subtract(performanceData.getPortfolioFirstOfYearValue()));

		performanceData.setPortfolioYtdFederalWithholding(
				portfolio.getFederalWithholdingBetweenDates(getFirstOfYearDate(), LocalDate.now()));
		performanceData.setPortfolioLastYearFederalWithholding(portfolio
				.getFederalWithholdingBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate().minusDays(1)));

		performanceData.setPortfolioYtdStateWithholding(
				portfolio.getStateWithholdingBetweenDates(getFirstOfYearDate(), LocalDate.now()));
		performanceData.portfolioLastYearStateWithholding = portfolio
				.getStateWithholdingBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate().minusDays(1));

		BigDecimal historicalValue = performanceData.getPortfolioYearAgoValue()
				.subtract(performanceData.getPortfolioYearAgoWithdrawals());
		performanceData.setPortfolioYearAgoReturns(portfolio.getTotalValue().subtract(historicalValue));

		historicalValue = performanceData.getPortfolioThreeYearAgoValue()
				.subtract(performanceData.getPortfolioThreeYearAgoWithdrawals());
		performanceData.setPortfolioThreeYearsAgoReturns(portfolio.getTotalValue().subtract(historicalValue)
				.divide(historicalValue, 4, RoundingMode.HALF_UP).divide(new BigDecimal(3), 4, RoundingMode.HALF_UP));
		return performanceData;
	}

	public static LocalDate getFirstOfYearDate() {
		return LocalDate.of(LocalDate.now().getYear(), 1, 1);
	}

	public static LocalDate getFirstOfLastYearDate() {
		return LocalDate.of(LocalDate.now().getYear() - 1, 1, 1);
	}

}
