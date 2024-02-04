package com.wise.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.ManagedPortfolio;
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

//		if (!portfolio.getFund(symbol).isClosed()) {
		BigDecimal historicalPrice = getClosestHistoricalPrice(symbol, date, 10);
		double historicalShares = portfolioPriceHistory.getSharesByDate(portfolio.getFund(symbol), date, false);
		if (historicalPrice != null && historicalShares > 0) {
			value = historicalPrice.multiply(new BigDecimal(historicalShares)).setScale(2, RoundingMode.DOWN);
		}
//		}

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

	public static PortfolioPerformanceData calculatePortfolioPerformanceData(ManagedPortfolio portfolio) {

		PortfolioPerformanceData performanceData = new PortfolioPerformanceData();
		LocalDate firstOfYearDate = getFirstOfYearDate();
		LocalDate firstOfLastYearDate = firstOfYearDate.minusYears(1);
		LocalDate oneYearAgoDate = LocalDate.now().minusYears(1);
		LocalDate threeYearAgoDate = LocalDate.now().minusYears(1);

		performanceData.setPortfolioCurrentValue(portfolio.getTotalValue());
		for (PortfolioFund fund : portfolio.getFundMap().values()) {

			performanceData.setPortfolioPreviousDayValue(performanceData.getPortfolioPreviousDayValue()
					.add(getValueByDate(fund.getSymbol(), LocalDate.now().minusDays(1))));

			performanceData.setPortfolioYtdWithdrawals(
					performanceData.getPortfolioYtdWithdrawals().add(fund.getWithdrawalsUpToDate(firstOfYearDate)));
			performanceData.setPortfolioYtdDividends(
					performanceData.getPortfolioYtdDividends().add(fund.getDistributionsAfterDate(firstOfYearDate)));
			performanceData.setPortfolioLastYearDividends(performanceData.getPortfolioLastYearDividends()
					.add(fund.getDistributionsBetweenDates(firstOfLastYearDate, firstOfYearDate.minusDays(1))));

			BigDecimal fundFirstOfYearValue = getValueByDate(fund.getSymbol(), firstOfYearDate);
			performanceData.setPortfolioFirstOfYearValue(
					performanceData.getPortfolioFirstOfYearValue().add(fundFirstOfYearValue));

			BigDecimal fundFirstOfLastYearValue = getValueByDate(fund.getSymbol(), firstOfLastYearDate);
			performanceData.setPortfolioFirstOfLastYearValue(
					performanceData.getPortfolioFirstOfLastYearValue().add(fundFirstOfLastYearValue));

			BigDecimal fundYearAgoValue = getValueByDate(fund.getSymbol(), oneYearAgoDate);
			performanceData.setPortfolioYearAgoValue(performanceData.getPortfolioYearAgoValue().add(fundYearAgoValue));

			BigDecimal fundThreeYearAgoValue = getValueByDate(fund.getSymbol(), threeYearAgoDate);
			performanceData.setPortfolioThreeYearAgoValue(
					performanceData.getPortfolioThreeYearAgoValue().add(fundThreeYearAgoValue));

			performanceData.setPortfolioYtdWithdrawals(performanceData.getPortfolioYtdWithdrawals()
					.add(fund.geWithdrawalsBetweenDates(firstOfYearDate, LocalDate.now())));
			performanceData.setPortfolioLastYearWithdrawals(performanceData.getPortfolioLastYearWithdrawals()
					.add(fund.geWithdrawalsBetweenDates(firstOfLastYearDate, firstOfYearDate)));
			performanceData.setPortfolioYearAgoWithdrawals(
					performanceData.getPortfolioYearAgoWithdrawals().add(fund.getWithdrawalsUpToDate(oneYearAgoDate)));
			performanceData.setPortfolioThreeYearAgoWithdrawals(performanceData.getPortfolioThreeYearAgoWithdrawals()
					.add(fund.getWithdrawalsUpToDate(threeYearAgoDate)));

			performanceData.setPortfolioTotalCurrentPercentage(performanceData.getPortfolioTotalCurrentPercentage()
					.add(CurrencyHelper.calculatePercentage(fund.getCurrentValue(), portfolio.getTotalValue())));
			performanceData.setPortfolioTotalTargetPercentage(performanceData.getPortfolioTotalTargetPercentage()
					.add(fund.getPercentageByCategory(FundCategory.TOTAL)));

		}

		performanceData.setPortfolioPreviousDayValueChange(
				portfolio.getTotalValue().subtract(performanceData.getPortfolioPreviousDayValue()));
		performanceData
				.setPortfolioYtdValueChange(portfolio.getTotalValue().add(performanceData.getPortfolioYtdWithdrawals())
						.subtract(performanceData.getPortfolioFirstOfYearValue()));

		performanceData.setPortfolioYtdFederalWithholding(
				portfolio.getFederalWithholdingBetweenDates(getFirstOfYearDate(), LocalDate.now()));
		performanceData.setPortfolioLastYearFederalWithholding(portfolio
				.getFederalWithholdingBetweenDates(getFirstOfLastYearDate(), getFirstOfYearDate().minusDays(1)));

		performanceData.setPortfolioYtdStateWithholding(
				portfolio.getStateWithholdingBetweenDates(getFirstOfYearDate(), LocalDate.now()));
		performanceData.portfolioLastYearStateWithholding = portfolio
				.getStateWithholdingBetweenDates(firstOfLastYearDate, firstOfYearDate.minusDays(1));

		// 1 year
		BigDecimal historicalValue = performanceData.getPortfolioYearAgoValue()
				.subtract(performanceData.getPortfolioYearAgoWithdrawals());
		BigDecimal fundReturns = portfolio.getTotalValue().subtract(historicalValue).divide(historicalValue, 4,
				RoundingMode.HALF_UP);
		performanceData.setPortfolioYearAgoReturns(fundReturns);

		// 3 year
		historicalValue = performanceData.getPortfolioThreeYearAgoValue()
				.subtract(performanceData.getPortfolioThreeYearAgoWithdrawals());
		fundReturns = portfolio.getTotalValue().subtract(historicalValue).divide(historicalValue, 4,
				RoundingMode.HALF_UP);

		double rate = PortfolioService.calculateAnnualizedReturn(fundReturns, 3);
		performanceData.setPortfolioThreeYearsAgoReturns(new BigDecimal(rate));
		return performanceData;
	}

	public static LocalDate getFirstOfYearDate() {
		return LocalDate.of(LocalDate.now().getYear(), 1, 1);
	}

	public static LocalDate getFirstOfLastYearDate() {
		return LocalDate.of(LocalDate.now().getYear() - 1, 1, 1);
	}

}
