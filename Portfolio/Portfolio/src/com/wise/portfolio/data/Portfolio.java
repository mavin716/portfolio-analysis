package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.wise.portfolio.data.MutualFund.FundCategory;

public class Portfolio {

	// Lazy load
	private BigDecimal totalValue = null;
	private Map<String, String> fundSymbolNameMap;

	public void setFundSymbolNameMap(Map<String, String> fundSymbolNameMap) {
		this.fundSymbolNameMap = fundSymbolNameMap;
	}
	private Map<String, PortfolioFund> fundMap = new TreeMap<String, PortfolioFund>();

//	private Map<String, PortfolioFund> fundMap = new TreeMap<String, PortfolioFund>(new Comparator<String>() {
//
//		@Override
//		public int compare(String symbol1, String symbol12) {
//			String fundName1 = fundSymbolNameMap.get(symbol1);
//			String fundName2 = fundSymbolNameMap.get(symbol12);
//			return fundName1.compareTo(fundName2);
//		}
//	});
	private PortfolioPriceHistory priceHistory = new PortfolioPriceHistory();


	public PortfolioPriceHistory getPriceHistory() {
		return priceHistory;
	}

	public void setPriceHistory(PortfolioPriceHistory priceHistory) {
		this.priceHistory = priceHistory;
	}

	public Portfolio() {
		super();

	}

	public Portfolio(Portfolio originalPortfolio) {
		super();
		this.totalValue = originalPortfolio.totalValue;

		for (Entry<String, PortfolioFund> entry : originalPortfolio.getFundMap().entrySet()) {
			PortfolioFund fund = new PortfolioFund();
		}

	}

	public Map<String, PortfolioFund> getFundMap() {
		return fundMap;
	}

	public PortfolioFund getFund(String symbol) {
		return fundMap.get(symbol);
	}

	public void setFundMap(Map<String, PortfolioFund> map) {
		this.fundMap = map;
		totalValue = null;
	}

	public void addFund(PortfolioFund fund) {
		this.fundMap.put(fund.getSymbol(), fund);
		totalValue = null;
	}

	public BigDecimal getTotalValue() {
		// change to lazy load and cache total - no, will not exclude prespent value

		loadTotalValue();
		return totalValue;
	}

	private void loadTotalValue() {
		totalValue = fundMap.values().stream().filter(fund -> !fund.isClosed()).map(PortfolioFund::getValue)
				.reduce(new BigDecimal(0, MathContext.DECIMAL32), (total, fundValue) -> total = total.add(fundValue))
				.setScale(2, BigDecimal.ROUND_UP);
	}

	public BigDecimal getValueByCategory(FundCategory category) {

		return fundMap.values().stream().filter(fund -> fund.getCategoriesMap().containsKey(category))
				.map(fund -> fund.getValueByCategory(category))
				.reduce(new BigDecimal(0, MathContext.DECIMAL32), (value, fundValue) -> value = value.add(fundValue))
				.setScale(2, BigDecimal.ROUND_UP);
	}

	/**
	 * Get current percentage for this category
	 * 
	 * @param category
	 * @return
	 */
	public Float getPercentageByCategory(FundCategory category) {
		Float percentage = new Float(0);

		BigDecimal totalValue = getTotalValue();
		BigDecimal categoryValue = getValueByCategory(category);

		if (categoryValue != null) {
			percentage = categoryValue.divide(totalValue, 4, RoundingMode.DOWN).setScale(2, BigDecimal.ROUND_UP)
					.floatValue();
		}
		return percentage;

	}

	public void adjustValue(String symbol, BigDecimal value) {
		PortfolioFund fund = getFund(symbol);

		// Determine number of shares
//        System.out.println("Before adjusting fund:  " + String.format("%60s", fund.getName()) + " shares:  "
//                + NumberFormat.getNumberInstance().format(fund.getShares()) + " value:  "
//                + NumberFormat.getCurrencyInstance().format(fund.getValue()));
		BigDecimal numSharesAdjust = value.divide(fund.getCurrentPrice(), 4, BigDecimal.ROUND_HALF_UP).setScale(2,
				BigDecimal.ROUND_HALF_UP);
//        System.out.println("numSharesAdjust:  " + numSharesAdjust);
		double adjustedShares = fund.getShares() + numSharesAdjust.doubleValue();
		fund.setShares(adjustedShares);
//        System.out.println("After adjusting fund :  " + String.format("%60s", fund.getName()) + " shares:  "
//                + NumberFormat.getNumberInstance().format(fund.getShares()) + " value:  "
//                + NumberFormat.getCurrencyInstance().format(fund.getValue()));
		totalValue = null;

	}

	public BigDecimal getPrespentValue() {
		BigDecimal prespentValue = fundMap.values().stream().map(PortfolioFund::getPreSpent)
				.reduce(new BigDecimal(0, MathContext.DECIMAL32),
						(total, prespentFundValue) -> total = total.add(prespentFundValue))
				.setScale(2, BigDecimal.ROUND_UP);
		return prespentValue;
	}

	public BigDecimal getAvailableValue() {
		return getTotalValue().subtract(getPrespentValue());
	}
}
