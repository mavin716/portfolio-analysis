package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class CurrencyHelper {

	public static BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
		return amount.divide(total, 4, BigDecimal.ROUND_HALF_UP).setScale(4, BigDecimal.ROUND_UP);
	}


	public static String formatAsCurrencyString(BigDecimal n) {
		return NumberFormat.getCurrencyInstance().format(n);
	}

	public static String formatPercentageString(Float desiredCategoryPercentage) {
		return desiredCategoryPercentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.2f", (desiredCategoryPercentage * 100)) + "%";
	}

	public static String formatPercentageString(BigDecimal desiredCategoryPercentage) {
		return desiredCategoryPercentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.2f", (desiredCategoryPercentage.multiply(new BigDecimal(100)))) + "%";
	}
	public static String formatPercentageString3(BigDecimal desiredCategoryPercentage) {
		return desiredCategoryPercentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.3f", (desiredCategoryPercentage.multiply(new BigDecimal(100)))) + "%";
	}
	public static String formatPercentageString4(BigDecimal desiredCategoryPercentage) {
		return desiredCategoryPercentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.4f", (desiredCategoryPercentage.multiply(new BigDecimal(100)))) + "%";
	}

}
