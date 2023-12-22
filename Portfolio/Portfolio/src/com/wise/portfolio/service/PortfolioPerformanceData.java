package com.wise.portfolio.service;

import java.math.BigDecimal;

public class PortfolioPerformanceData {

	public void setPortfolioCurrentValue(BigDecimal portfolioCurrentValue) {
		this.portfolioCurrentValue = portfolioCurrentValue;
	}


	public void setPortfolioYtdDividends(BigDecimal portfolioYtdDividends) {
		this.portfolioYtdDividends = portfolioYtdDividends;
	}


	public void setPortfolioLastYearDividends(BigDecimal portfolioLastYearDividends) {
		this.portfolioLastYearDividends = portfolioLastYearDividends;
	}


	public void setPortfolioTotalCurrentPercentage(BigDecimal portfolioTotalCurrentPercentage) {
		this.portfolioTotalCurrentPercentage = portfolioTotalCurrentPercentage;
	}


	public void setPortfolioTotalTargetPercentage(BigDecimal portfolioTotalTargetPercentage) {
		this.portfolioTotalTargetPercentage = portfolioTotalTargetPercentage;
	}


	public void setPortfolioYtdValueChange(BigDecimal portfolioYtdValueChange) {
		this.portfolioYtdReturns = portfolioYtdValueChange;
	}


	public void setPortfolioYtdWithdrawals(BigDecimal portfolioYtdWithdrawals) {
		this.portfolioYtdWithdrawals = portfolioYtdWithdrawals;
	}


	public void setPortfolioFirstOfYearValue(BigDecimal portfolioFirstOfYearValue) {
		this.portfolioFirstOfYearValue = portfolioFirstOfYearValue;
	}


	public void setPortfolioPreviousDayValue(BigDecimal portfolioPreviousDayValue) {
		this.portfolioPreviousDayValue = portfolioPreviousDayValue;
	}



	public void setPortfolioYearAgoValue(BigDecimal portfolioYearAgoValue) {
		this.portfolioYearAgoValue = portfolioYearAgoValue;
	}


	public void setPortfolioYearAgoWithdrawals(BigDecimal portfolioYearAgoWithdrawals) {
		this.portfolioYearAgoWithdrawals = portfolioYearAgoWithdrawals;
	}


	public void setPortfolioThreeYearAgoValue(BigDecimal portfolioThreeYearAgoValue) {
		this.portfolioThreeYearAgoValue = portfolioThreeYearAgoValue;
	}


	public void setPortfolioThreeYearAgoWithdrawals(BigDecimal portfolioThreeYearAgoWithdrawals) {
		this.portfolioThreeYearAgoWithdrawals = portfolioThreeYearAgoWithdrawals;
	}


	public BigDecimal getPortfolioCurrentValue() {
		return portfolioCurrentValue;
	}


	public BigDecimal getPortfolioYtdDividends() {
		return portfolioYtdDividends;
	}


	public BigDecimal getPortfolioLastYearDividends() {
		return portfolioLastYearDividends;
	}


	public BigDecimal getPortfolioTotalCurrentPercentage() {
		return portfolioTotalCurrentPercentage;
	}


	public BigDecimal getPortfolioTotalTargetPercentage() {
		return portfolioTotalTargetPercentage;
	}


	public BigDecimal getPortfolioYtdReturns() {
		return portfolioYtdReturns;
	}


	public BigDecimal getPortfolioYtdWithdrawals() {
		return portfolioYtdWithdrawals;
	}


	public BigDecimal getPortfolioFirstOfYearValue() {
		return portfolioFirstOfYearValue;
	}


	public BigDecimal getPortfolioPreviousDayValue() {
		return portfolioPreviousDayValue;
	}




	public BigDecimal getPortfolioYearAgoValue() {
		return portfolioYearAgoValue;
	}


	public BigDecimal getPortfolioYearAgoWithdrawals() {
		return portfolioYearAgoWithdrawals;
	}


	public BigDecimal getPortfolioThreeYearAgoValue() {
		return portfolioThreeYearAgoValue;
	}


	public BigDecimal getPortfolioThreeYearAgoWithdrawals() {
		return portfolioThreeYearAgoWithdrawals;
	}


	private BigDecimal portfolioCurrentValue = BigDecimal.ZERO;

	private BigDecimal portfolioYtdDividends = BigDecimal.ZERO;
	private BigDecimal portfolioLastYearDividends = BigDecimal.ZERO;
	private BigDecimal portfolioLastYearWithdrawals = BigDecimal.ZERO;
	public BigDecimal getPortfolioLastYearWithdrawals() {
		return portfolioLastYearWithdrawals;
	}


	private BigDecimal portfolioTotalCurrentPercentage = BigDecimal.ZERO;
	private BigDecimal portfolioTotalTargetPercentage = BigDecimal.ZERO;
	
	private BigDecimal portfolioYtdReturns = BigDecimal.ZERO;
	private BigDecimal portfolioYtdWithdrawals = BigDecimal.ZERO;
	private BigDecimal portfolioFirstOfYearValue = BigDecimal.ZERO;
	private BigDecimal portfolioFirstOfLastYearValue = BigDecimal.ZERO;
	private BigDecimal portfolioPreviousDayValue = BigDecimal.ZERO;
	public BigDecimal getPortfolioFirstOfLastYearValue() {
		return portfolioFirstOfLastYearValue;
	}


	private BigDecimal portfolioPreviousDayValueChange = BigDecimal.ZERO;
	public BigDecimal getPortfolioPreviousDayValueChange() {
		return portfolioPreviousDayValueChange;
	}

	private BigDecimal portfolioYtdValueChange = BigDecimal.ZERO;
	public BigDecimal getPortfolioYtdValueChange() {
		return portfolioYtdValueChange;
	}

	private BigDecimal portfolioYearAgoValue = BigDecimal.ZERO;
	private BigDecimal portfolioYearAgoReturns = BigDecimal.ZERO;
	public BigDecimal getPortfolioYearAgoReturns() {
		return portfolioYearAgoReturns;
	}

	private BigDecimal portfolioThreeYearsAgoReturns = BigDecimal.ZERO;
	public BigDecimal getPortfolioThreeYearsAgoReturns() {
		return portfolioThreeYearsAgoReturns;
	}

	private BigDecimal portfolioYearAgoWithdrawals = BigDecimal.ZERO;
	private BigDecimal portfolioThreeYearAgoValue = BigDecimal.ZERO;
	private BigDecimal portfolioThreeYearAgoWithdrawals = BigDecimal.ZERO;
	
	private BigDecimal portfolioYtdFederalWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioYtdFederalWithholding() {
		return portfolioYtdFederalWithholding;
	}


	private BigDecimal portfolioLastYearFederalWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioLastYearFederalWithholding() {
		return portfolioLastYearFederalWithholding;
	}

	private BigDecimal portfolioYtdStateWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioYtdStateWithholding() {
		return portfolioYtdStateWithholding;
	}


	BigDecimal portfolioLastYearStateWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioLastYearStateWithholding() {
		return portfolioLastYearStateWithholding;
	}


	public BigDecimal getPortfolioLastYearReturns() {
		// TODO yearago or last year????
		return getPortfolioYearAgoReturns();
	}


	public void setPortfolioFirstOfLastYearValue(BigDecimal portfolioFirstOfLastYearValue) {
		this.portfolioFirstOfLastYearValue = portfolioFirstOfLastYearValue;
	}


	public void setPortfolioYearAgoReturns(BigDecimal portfolioYearAgoReturns) {
		this.portfolioYearAgoReturns = portfolioYearAgoReturns;
	}


	public void setPortfolioYtdStateWithholding(BigDecimal portfolioYtdStateWithholding) {
		this.portfolioYtdStateWithholding = portfolioYtdStateWithholding;
	}


	public void setPortfolioThreeYearsAgoReturns(BigDecimal portfolioThreeYearsAgoReturns) {
		this.portfolioThreeYearsAgoReturns = portfolioThreeYearsAgoReturns;
	}


	public void setPortfolioYtdFederalWithholding(BigDecimal portfolioYtdFederalWithholding) {
		this.portfolioYtdFederalWithholding = portfolioYtdFederalWithholding;
	}


	public void setPortfolioLastYearFederalWithholding(BigDecimal portfolioLastYearFederalWithholding) {
		this.portfolioLastYearFederalWithholding = portfolioLastYearFederalWithholding;
	}


	public void setPortfolioPreviousDayValueChange(BigDecimal portfolioPreviousDayValueChange) {
		this.portfolioPreviousDayValueChange = portfolioPreviousDayValueChange;
	}


	
}
