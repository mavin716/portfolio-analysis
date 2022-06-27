package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.wise.portfolio.data.MutualFund.FundCategory;

public class MutualFundPerformance {

	private Portfolio portfolio;
	private PortfolioPriceHistory portfolioPriceHistory;
	private PortfolioFund portfolioFund;

	public MutualFundPerformance(Portfolio portfolio, PortfolioFund fund) {
		super();

		this.portfolio = portfolio;
		this.portfolioPriceHistory = portfolio.getPriceHistory();
		this.portfolioFund = fund;

		BigDecimal beginYearPrice = getClosestHistoricalPrice(fund, getFirstOfYearDate(), 30);
		if (beginYearPrice == null) {
			return;
		}
		setFirstOfYearPrice(beginYearPrice);
		setCurrentSharePrice(fund.getCurrentPrice());
		setYtdChange(
				CurrencyHelper.calculatePercentage(fund.getCurrentPrice().subtract(beginYearPrice), beginYearPrice));
		Map<FundCategory, BigDecimal> currentPercentageOfTotal = new HashMap<>();
		setCurrentPercentageTotal(currentPercentageOfTotal);

	}

	public Float getPerformanceRateByDate(PortfolioFund fund, LocalDate historicalDate) {

		BigDecimal historicalPrice = portfolioPriceHistory.getPriceByDate(fund, historicalDate, false);
		Float historicalShares = portfolioPriceHistory.getSharesByDate(fund, historicalDate, false);
		BigDecimal historicalValue = portfolioPriceHistory.getValueByDate(fund, historicalDate, false);
		BigDecimal returns = getReturnsByDate(fund, historicalDate, false);
		if (historicalPrice == null || historicalShares == null || historicalValue == null || returns == null) {
			return null;
		}

		if (historicalPrice == null || historicalPrice.compareTo(BigDecimal.ZERO) == 0) {
			// check for linked account
			PortfolioFund oldFund = fund.getOldFund(portfolio);
			if (oldFund != null) {
				historicalPrice = portfolioPriceHistory.getPriceByDate(oldFund, fund.getOldFundConverted(), false);
				if (historicalPrice == null) {
					return null;
				}
				historicalShares = oldFund.getConversionsSharesUpToDate(historicalDate);
				// will be conversion out so negative shares
				historicalShares = 1 - historicalShares;
			} else {
				return null;
			}
		}

		BigDecimal currentValue = fund.getValue();
		if (currentValue.intValue() == 0 || historicalValue.intValue() == 0) {
			return null;
		}

		// Adjust for dividends
		// historicalValue =
		// historicalValue.subtract(fund.getDistributionsAfterDate(historicalDate));
		// Adjust for withdrawas
		BigDecimal withdrawals = fund.getWithdrawalsUpToDate(historicalDate);
		BigDecimal exchanges = fund.getExchangeTotalFromDate(historicalDate);
		historicalValue = historicalValue.subtract(withdrawals).subtract(exchanges);
		Float rate = currentValue.subtract(historicalValue)
				.divide(currentValue, PortfolioService.CURRENCY_SCALE, RoundingMode.HALF_UP).floatValue();
		return rate;

	}

	private static LocalDate getFirstOfYearDate() {
		return LocalDate.of(LocalDate.now().getYear(), 1, 1);
	}

	private BigDecimal getClosestHistoricalPrice(PortfolioFund fund, LocalDate date, int days) {

		BigDecimal historicalValue = portfolioPriceHistory.getPriceByDate(fund, date, true);
		if (historicalValue != null) {
			return historicalValue;
		}
		int tries = 0;
		while (tries++ < days) {

			historicalValue = portfolioPriceHistory.getPriceByDate(fund, date.minusDays(tries), true);
			if (historicalValue != null) {
				return historicalValue;
			}
			historicalValue = portfolioPriceHistory.getPriceByDate(fund, date.plusDays(tries), true);
			if (historicalValue != null) {
				return historicalValue;
			}
		}
		return null;
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
		return minPricePair;
	}

	public void setMinPricePair(Pair<LocalDate, BigDecimal> minPricePair) {
		this.minPricePair = minPricePair;
	}

	public Pair<LocalDate, BigDecimal> getMaxPricePair() {
		return maxPricePair;
	}

	public void setMaxPricePair(Pair<LocalDate, BigDecimal> maxPricePair) {
		this.maxPricePair = maxPricePair;
	}

	public BigDecimal getFirstOfYearPrice() {
		return firstOfYearPrice;
	}

	public void setFirstOfYearPrice(BigDecimal firstOfYearPrice) {
		this.firstOfYearPrice = firstOfYearPrice;
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

	public Float getShares() {
		return shares;
	}

	public void setShares(Float shares) {
		this.shares = shares;
	}

	public Map<FundCategory, BigDecimal> getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(Map<FundCategory, BigDecimal> currentValue) {
		this.currentValue = currentValue;
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

	public Map<FundCategory, BigDecimal> getTargetValue() {
		return targetValue;
	}

	public void setTargetValue(Map<FundCategory, BigDecimal> targetValue) {
		this.targetValue = targetValue;
	}

	public Float getDeviation() {
		return deviation;
	}

	public void setDeviation(Float deviation) {
		this.deviation = deviation;
	}

	public Map<FundCategory, BigDecimal> getSurplusDeficit() {
		return surplusDeficit;
	}

	public void setSurplusDeficit(Map<FundCategory, BigDecimal> surplusDeficit) {
		this.surplusDeficit = surplusDeficit;
	}

	private Pair<LocalDate, BigDecimal> minPricePair;
	private Pair<LocalDate, BigDecimal> maxPricePair;
	private BigDecimal firstOfYearPrice;
	private BigDecimal ytdChange;
	private BigDecimal ytdDividends;
	private BigDecimal currentSharePrice;
	private Float shares;
	private Map<FundCategory, BigDecimal> currentValue;
	private Map<FundCategory, BigDecimal> currentPercentageTotal;
	private Map<FundCategory, BigDecimal> targetPercentageTotal;
	private Map<FundCategory, BigDecimal> targetValue;
	private Float deviation;
	private Map<FundCategory, BigDecimal> surplusDeficit;

	public BigDecimal getYtdPriceChange() {
		BigDecimal ytdPriceChange = CurrencyHelper.calculatePercentage(
				portfolioFund.getCurrentPrice().subtract(getFirstOfYearPrice()), getFirstOfYearPrice());
		return ytdPriceChange;
	}

	public BigDecimal getYtdValueChange() {
		BigDecimal withdrawals = portfolioFund.getWithdrawalsUpToDate(getFirstOfYearDate());
		BigDecimal exchanges = portfolioFund.getExchangeTotalFromDate(getFirstOfYearDate());
		BigDecimal historicalValue = getHistoricalValue(portfolioFund, getYtdDays() + 1);
		BigDecimal ytdValueChange = portfolioFund.getValue().subtract(historicalValue).add(withdrawals).add(exchanges);
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
		Float historicalShares = getSharesByDate(fund, historicalDate, false);
		if (historicalShares == null) {
			historicalShares = new Float(0);
		}
		historicalValue = historicPrice.multiply(new BigDecimal(historicalShares));

		return historicalValue;
	}

	public BigDecimal getPriceByDate(Fund fund, LocalDate date, boolean isExactDate) {
		BigDecimal value = null;

		Map<LocalDate, BigDecimal> fundPriceMap = portfolioPriceHistory.getFundPrices().get(fund.getSymbol());
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

	public Float getSharesByDate(Fund fund, LocalDate date, boolean isExactDate) {
		Float value = null;

		Map<LocalDate, Float> fundSharesMap = portfolioPriceHistory.getFundShares().get(fund.getSymbol());
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
		}

		// used by some functions expecting number so for now just return zero
		return null;
	}

	// TODO Returns are the increase/decrease in value of the fund, not the raw
	// difference.
	public BigDecimal getReturnsByDate(PortfolioFund fund, LocalDate date, boolean isExactDate) {

		BigDecimal returns = null;

		Map<LocalDate, BigDecimal> fundReturnMap = portfolioPriceHistory.getFundReturns().get(fund.getSymbol());
		if (fundReturnMap == null) {
			fundReturnMap = new HashMap<>();
			portfolioPriceHistory.getFundReturns().put(fund.getSymbol(), fundReturnMap);
		}
		returns = fundReturnMap.get(date);
		if (returns == null) {
			returns = BigDecimal.ZERO;
			BigDecimal conversions = fund.getConversionsUpToDate(date);
			BigDecimal withdrawals = fund.getWithdrawalsUpToDate(date);
			BigDecimal exchanges = fund.getExchangeTotalFromDate(date);
			BigDecimal dividends = fund.getDistributionsAfterDate(date);
			BigDecimal currentValue = fund.getValue();
			BigDecimal historicalValue = portfolioPriceHistory.getValueByDate(fund, date, isExactDate);
			if (currentValue != null && historicalValue != null) {
				returns = currentValue.add(withdrawals).add(exchanges).subtract(historicalValue).subtract(conversions);
				if (withdrawals.compareTo(BigDecimal.ZERO) > 0) {
					System.out.println("fund:  " + fund.getShortName() + " date:  " + date);
					System.out.println("currentValue:  " + CurrencyHelper.formatAsCurrencyString(currentValue));
					System.out.println("historicalValue:  " + CurrencyHelper.formatAsCurrencyString(historicalValue));
					System.out.println("withdrawals:  " + CurrencyHelper.formatAsCurrencyString(withdrawals));
					System.out.println("conversions:  " + CurrencyHelper.formatAsCurrencyString(conversions));
					System.out.println("exchanges:  " + CurrencyHelper.formatAsCurrencyString(exchanges));
					System.out.println("dividends:  " + CurrencyHelper.formatAsCurrencyString(dividends));
					System.out.println("returns:  " + CurrencyHelper.formatAsCurrencyString(returns));
				}
			}
			fundReturnMap.put(date, returns);
		}

		return returns;
	}

}
