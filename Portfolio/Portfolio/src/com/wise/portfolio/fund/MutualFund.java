package com.wise.portfolio.fund;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MutualFund implements Comparable<MutualFund>, Fund {

	public class CompareByValue implements Comparator<PortfolioFund> {

		@Override
		public int compare(PortfolioFund f1, PortfolioFund f2) {

			return f2.getCurrentValue().compareTo(f1.getCurrentValue());
		}

	}

	public enum FundCategory {
		TOTAL, CASH, BOND, STOCK, INTL
	}

	protected String name;
	protected String symbol;
	protected BigDecimal cost = new BigDecimal(0);
	protected BigDecimal currentPrice;
	protected BigDecimal minimumAmount;

	public BigDecimal getMinimumAmount() {
		return minimumAmount;
	}

	public void setMinimumAmount(BigDecimal minimumAmount) {
		this.minimumAmount = minimumAmount;
	}

	protected Map<FundCategory, BigDecimal> categoriesMap = new HashMap<>();
	private LocalDate currentPriceDate;

	public String getName() {
		return name;
	}

	public String getShortName() {
		String shortName = name.replace("Vanguard ", "").replace(" Fund", "").replace(" Stock", "")
				.replace(" and", " &").replace(" Class", "").replace(" Shares", "").replace(" Share", "")
				.replace(" Money Market", " MM").replace("Intermediate", "Int").replace("International", "Intl")
				.replace("Investor", "Inv").replace("Income", "Inc").replace("Investment", "Invmt")
				.replace("Index", "Idx").replace("Admiral", "Adm").replace("Growth", "Grwth").replace("Small", "Sm")
				.replace("Federal", "Fed").replace("Equity", "Eq").replace("Market", "Mkt").replace("Reserves", "Rsv");
		return shortName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BigDecimal getCurrentPrice() {
		return currentPrice;
	}

	public void setCurrentPrice(BigDecimal currentPrice, LocalDate currentPriceDate) {
		// TODO iff currentPriceDate is later than replace
		
		if (this.currentPriceDate == null || currentPriceDate.isAfter(this.currentPriceDate)) {
			this.currentPrice = currentPrice;
			this.currentPriceDate = currentPriceDate;
		}
	}

	public Map<FundCategory, BigDecimal> getCategoriesMap() {
		return categoriesMap;
	}

	public void setCategoriesMap(Map<FundCategory, BigDecimal> categoriesMap) {
		this.categoriesMap = categoriesMap;
	}

	public void addCategory(FundCategory category, BigDecimal percentage) {
		categoriesMap.put(category, percentage);
	}

	public BigDecimal getPercentageByCategory(FundCategory category) {
		return categoriesMap.get(category);
	}

	@Override
	public int compareTo(MutualFund f) {
		return name.compareTo(f.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MutualFund) {
			return symbol.equals(((MutualFund) obj).getSymbol());
		}
		return false;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

}