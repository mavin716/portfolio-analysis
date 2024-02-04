package com.wise.portfolio.fund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

public class FundPriceHistory {
	public static final int CURRENCY_SCALE = 6;

	
	public FundPriceHistory(String fundSymbol) {
		super();
		this.fundSymbol = fundSymbol;
	}

	private String fundSymbol;
	public String getFundSymbol() {
		return fundSymbol;
	}

	public void setFundSymbol(String fundSymbol) {
		this.fundSymbol = fundSymbol;
	}

	private Map<LocalDate, BigDecimal> fundPriceMap = new TreeMap<>();


	
	public void addFundPrice(LocalDate date, BigDecimal price) {

		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		price = price.setScale(CURRENCY_SCALE, RoundingMode.HALF_DOWN);
		fundPriceMap.put(date, price);
		

	}

	public BigDecimal getPriceByDate(LocalDate date) {
		BigDecimal price = fundPriceMap.get(date);
		if (price == null) {
			for (int i = 1; i <= 7; i++) {
				date = date.minusDays(1);
				price = fundPriceMap.get(date);
				if (price != null) {
					break;
				}
			}
		}
		return price;
	}

	public Map<LocalDate, BigDecimal>getFundPricesMap() {
		return fundPriceMap;
	}

	public LocalDate getOldestDate() {
		// Sorted map
		LocalDate date =  fundPriceMap.keySet().iterator().next();
		return date;
	}
}
