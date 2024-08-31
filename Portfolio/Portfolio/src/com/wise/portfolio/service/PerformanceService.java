package com.wise.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.price.PortfolioPriceHistory;

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

		PortfolioFund fund = portfolio.getFund(symbol);
		if (fund != null) {
			BigDecimal historicalPrice = getClosestHistoricalPrice(symbol, date, 1);
			if (historicalPrice != null) {

				Double historicalShares = portfolioPriceHistory.getSharesByDate(fund, date);
				if (historicalShares != null && historicalShares > 0) {
					value = historicalPrice.multiply(new BigDecimal(historicalShares)).setScale(2, RoundingMode.DOWN);
				}
			}
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
		LocalDate firstOfYearDate = getFirstOfYearDate();
		LocalDate firstOfLastYearDate = firstOfYearDate.minusYears(1);
		LocalDate oneYearAgoDate = LocalDate.now().minusYears(1);
		LocalDate threeYearAgoDate = LocalDate.now().minusYears(3);
		LocalDate previousDayDate = LocalDate.now().minusDays(1);
		if (LocalDateTime.now().getHour() < 18) {
			previousDayDate = previousDayDate.minusDays(1);
		}
		if (previousDayDate.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
			previousDayDate = previousDayDate.minusDays(2);
		}
		if (previousDayDate.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
			previousDayDate = previousDayDate.minusDays(1);
		}

		performanceData.setPortfolioCurrentValue(portfolio.getTotalValue());
		for (PortfolioFund fund : portfolio.getFunds()) {

			performanceData.setPortfolioPreviousDayValue(performanceData.getPortfolioPreviousDayValue()
					.add(getValueByDate(fund.getSymbol(), previousDayDate)));

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
		performanceData.setPortfolioYtdValueChange(
				portfolio.getTotalValue().add(performanceData.getPortfolioYtdWithdrawals().abs())
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
		BigDecimal historicalValue = performanceData.getPortfolioYearAgoValue();
		BigDecimal fundReturns = portfolio.getTotalValue().add(performanceData.getPortfolioYearAgoWithdrawals())
				.subtract(historicalValue).divide(portfolio.getTotalValue(), 4, RoundingMode.HALF_UP);
		performanceData.setPortfolioYearAgoReturns(fundReturns);

		// 3 year
		historicalValue = performanceData.getPortfolioThreeYearAgoValue();
		fundReturns = portfolio.getTotalValue().add(performanceData.getPortfolioThreeYearAgoWithdrawals())
				.subtract(historicalValue).divide(historicalValue, 4, RoundingMode.HALF_UP);

		try {
			double rate = PortfolioService.calculateAnnualizedReturn(fundReturns, 3);
			performanceData.setPortfolioThreeYearsAgoReturns(new BigDecimal(rate));

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return performanceData;
	}

	public static LocalDate getFirstOfYearDate() {
		return LocalDate.of(LocalDate.now().getYear(), 1, 1);
	}

	public static LocalDate getFirstOfLastYearDate() {
		return LocalDate.of(LocalDate.now().getYear() - 1, 1, 1);
	}

}
