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
	LocalDate endRecurringDate;
	public LocalDate getEndRecurringDate() {
		return endRecurringDate;
	}
	public void setEndRecurringDate(LocalDate endRecurringDate) {
		this.endRecurringDate = endRecurringDate;
	}
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
	boolean adjustForInflation;
	
	public boolean isAdjustForInflation() {
		return adjustForInflation;
	}
	public void setAdjustForInflation(boolean adjustForInflation) {
		this.adjustForInflation = adjustForInflation;
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
	private boolean isNetAmount;
	public boolean isNetAmount() {
		return isNetAmount;
	}
	public void setNetAmount(boolean isNetAmount) {
		this.isNetAmount = isNetAmount;
	}
}
