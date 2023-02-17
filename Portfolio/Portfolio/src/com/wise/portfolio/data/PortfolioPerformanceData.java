package com.wise.portfolio.data;

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


	BigDecimal portfolioCurrentValue = BigDecimal.ZERO;

	BigDecimal portfolioYtdDividends = BigDecimal.ZERO;
	BigDecimal portfolioLastYearDividends = BigDecimal.ZERO;
	BigDecimal portfolioLastYearWithdrawals = BigDecimal.ZERO;
	public BigDecimal getPortfolioLastYearWithdrawals() {
		return portfolioLastYearWithdrawals;
	}


	BigDecimal portfolioTotalCurrentPercentage = BigDecimal.ZERO;
	BigDecimal portfolioTotalTargetPercentage = BigDecimal.ZERO;
	
	BigDecimal portfolioYtdReturns = BigDecimal.ZERO;
	BigDecimal portfolioYtdWithdrawals = BigDecimal.ZERO;
	BigDecimal portfolioFirstOfYearValue = BigDecimal.ZERO;
	BigDecimal portfolioFirstOfLastYearValue = BigDecimal.ZERO;
	BigDecimal portfolioPreviousDayValue = BigDecimal.ZERO;
	public BigDecimal getPortfolioFirstOfLastYearValue() {
		return portfolioFirstOfLastYearValue;
	}


	BigDecimal portfolioPreviousDayValueChange = BigDecimal.ZERO;
	public BigDecimal getPortfolioPreviousDayValueChange() {
		return portfolioPreviousDayValueChange;
	}

	BigDecimal portfolioYtdValueChange = BigDecimal.ZERO;
	public BigDecimal getPortfolioYtdValueChange() {
		return portfolioYtdValueChange;
	}

	BigDecimal portfolioYearAgoValue = BigDecimal.ZERO;
	BigDecimal portfolioYearAgoReturns = BigDecimal.ZERO;
	public BigDecimal getPortfolioYearAgoReturns() {
		return portfolioYearAgoReturns;
	}

	BigDecimal portfolioThreeYearsAgoReturns = BigDecimal.ZERO;
	public BigDecimal getPortfolioThreeYearsAgoReturns() {
		return portfolioThreeYearsAgoReturns;
	}

	BigDecimal portfolioYearAgoWithdrawals = BigDecimal.ZERO;
	BigDecimal portfolioThreeYearAgoValue = BigDecimal.ZERO;
	BigDecimal portfolioThreeYearAgoWithdrawals = BigDecimal.ZERO;
	
	BigDecimal portfolioYtdFederalWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioYtdFederalWithholding() {
		return portfolioYtdFederalWithholding;
	}


	BigDecimal portfolioLastYearFederalWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioLastYearFederalWithholding() {
		return portfolioLastYearFederalWithholding;
	}

	BigDecimal portfolioYtdStateWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioYtdStateWithholding() {
		return portfolioYtdStateWithholding;
	}


	BigDecimal portfolioLastYearStateWithholding = BigDecimal.ZERO;
	public BigDecimal getPortfolioLastYearStateWithholding() {
		return portfolioLastYearStateWithholding;
	}


	public BigDecimal getPortfolioLastYearReturns() {
		// TODO yearago or last year????
		return portfolioYearAgoReturns;
	}


	
}
