package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.wise.portfolio.data.MutualFund.FundCategory;

public class ManagedPortfolio extends Portfolio {

	private Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps = new HashMap<>();


	public Map<String, Map<FundCategory, BigDecimal>> getDesiredFundAllocationMaps() {
		return desiredFundAllocationMaps;
	}


	public void setDesiredFundAllocationMaps(Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps) {
		this.desiredFundAllocationMaps = desiredFundAllocationMaps;
	}


	public BigDecimal getFundDeviation(PortfolioFund fund) {
		if (fund.isClosed()) {
			return BigDecimal.ZERO;
		}
		BigDecimal availableFundValue = fund.getAvailableValue();
		Map<FundCategory, BigDecimal> map = desiredFundAllocationMaps.get(fund.getSymbol());
		BigDecimal desiredFundPercentage = map.get(FundCategory.TOTAL);
		BigDecimal desiredFundValue = getTotalValue().multiply(desiredFundPercentage, MathContext.UNLIMITED);
		// If minimum is greater than available then only use amount greater than
		// minimum
		BigDecimal difference = availableFundValue.subtract(desiredFundValue);
		if (fund.getMinimumAmount() != null) {
			if (availableFundValue.subtract(difference).compareTo(fund.getMinimumAmount()) < 0) {
				if (availableFundValue.compareTo(fund.getMinimumAmount()) > 0) {
					difference = availableFundValue.subtract(fund.getMinimumAmount());
				} else {
					difference = BigDecimal.ZERO;
				}
			}
		}

		BigDecimal fundTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);
		BigDecimal targetValue = getTotalValue().multiply(fundTargetPercentage);
		if (fund.getMinimumAmount() != null) {
			if (targetValue.compareTo(fund.getMinimumAmount()) < 0) {
				fundTargetPercentage = fund.getMinimumAmount().divide(getTotalValue(), 6,
						RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		if (fund.getValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = fund.getValue().divide(getTotalValue(), 6, RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}


	public Map<FundCategory, BigDecimal> getDesiredFundAllocationMap(String symbol) {
		return desiredFundAllocationMaps.get(symbol);
	}


	public BigDecimal getFundDeviation(PortfolioFund fund, BigDecimal portfolioAdjustment) {
		if (fund.isClosed()) {
			return BigDecimal.ZERO;
		}
		BigDecimal totalPortfolioValueAfterAdjustment = getTotalValue().subtract(portfolioAdjustment);
		BigDecimal fundTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);

		if (fund.getMinimumAmount() != null) {
			// If miminum is greater than target amount then use minimum plus cushion of
			// $500 to override fundTargetPercentage
			BigDecimal targetValue = totalPortfolioValueAfterAdjustment.multiply(fundTargetPercentage);
			if (targetValue.compareTo(fund.getMinimumAmount()) < 0) {
				fundTargetPercentage = fund.getMinimumAmount().add(new BigDecimal(500))
						.divide(totalPortfolioValueAfterAdjustment, 4, RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		if (fund.getValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = fund.getValue().divide(totalPortfolioValueAfterAdjustment, 4,
					RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}
}
