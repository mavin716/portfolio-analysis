package com.wise.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class CurrencyHelper {

	public static BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
		if (total.compareTo(BigDecimal.ZERO) == 0 || amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		} else {
			return amount.divide(total, 6, RoundingMode.DOWN).setScale(6, RoundingMode.DOWN);
		}
	}

	public static String formatAsCurrencyString(BigDecimal n) {
		return NumberFormat.getCurrencyInstance().format(n);
	}

	public static String formatPercentageString(Double percentage) {
		return percentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.2f", (percentage * 100)) + "%";
	}

	public static String formatPercentageString(BigDecimal percentage) {
		return percentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.2f", (percentage.multiply(new BigDecimal(100)))) + "%";
	}

	public static String formatPercentageString3(BigDecimal percentage) {
		return percentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.3f", (percentage.multiply(new BigDecimal(100)))) + "%";
	}

	public static String formatPercentageString4(BigDecimal percentage) {
		return percentage == null ? String.format("%6s", "N/A%")
				: String.format("%(6.4f", (percentage.multiply(new BigDecimal(100)))) + "%";
	}



}
