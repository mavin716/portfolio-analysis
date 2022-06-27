package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {

	private String transactionType;
	private Float transastionShares;
	private BigDecimal transastionSharePrice;
	private BigDecimal transastionPrincipal;
	private String transactionSourceFile;

	public String getTransactionSourceFile() {
		return transactionSourceFile;
	}
	public void setTransactionSourceFile(String transactionSourceFile) {
		this.transactionSourceFile = transactionSourceFile;
	}
	public BigDecimal getTransastionPrincipal() {
		return transastionPrincipal;
	}
	public void setTransastionPrincipal(BigDecimal transastionPrincipal) {
		this.transastionPrincipal = transastionPrincipal;
	}
	public Transaction(LocalDate transactionDate, String transactionType, Float transastionShares,
			BigDecimal transastionSharePrice, BigDecimal transastionPrincipal, String transactionSourceFile) {
		super();
		this.transactionDate = transactionDate;
		this.transactionType = transactionType;
		this.transastionShares = transastionShares;
		this.transastionSharePrice = transastionSharePrice;
		this.transastionPrincipal = transastionPrincipal;
		this.transactionSourceFile = transactionSourceFile;
	}
	LocalDate transactionDate;
	public LocalDate getTransactionDate() {
		return transactionDate;
	}
	public void setTransactionDate(LocalDate transactionDate) {
		this.transactionDate = transactionDate;
	}
	public String getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}
	public Float getTransastionShares() {
		return transastionShares;
	}
	public void setTransastionShares(Float transastionShares) {
		this.transastionShares = transastionShares;
	}
	public BigDecimal getTransastionSharePrice() {
		return transastionSharePrice;
	}
	public void setTransastionSharePrice(BigDecimal transastionSharePrice) {
		this.transastionSharePrice = transastionSharePrice;
	}
}
