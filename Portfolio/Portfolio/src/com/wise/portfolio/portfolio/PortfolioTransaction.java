package com.wise.portfolio.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioTransaction {

	String type;
	String subType;
	public String getSubType() {
		return subType;
	}
	public void setSubType(String subType) {
		this.subType = subType;
	}
	LocalDate date;
	String fundSymbol;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
		this.date = date;
	}
	public String getFundSymbol() {
		return fundSymbol;
	}
	public void setFundSymbol(String fundSymbol) {
		this.fundSymbol = fundSymbol;
	}
	public boolean isRecurring() {
		return isRecurring;
	}
	public void setRecurring(boolean isRecurring) {
		this.isRecurring = isRecurring;
	}
	public String getRecurringPeriod() {
		return recurringPeriod;
	}
	public void setRecurringPeriod(String recurringPeriod) {
		this.recurringPeriod = recurringPeriod;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	boolean isRecurring;
	String recurringPeriod;
	String description;
	BigDecimal amount;
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}
