package com.wise.portfolio.graph;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.itextpdf.kernel.color.Color;
import com.itextpdf.layout.element.Cell;
import com.wise.portfolio.service.CurrencyHelper;
import com.wise.portfolio.service.MutualFundPerformance;

public class FundRankCell extends Cell {

	public FundRankCell(Integer rankDayIndex, Integer rank, MutualFundPerformance fundPerformance, long numDays,
			List<Long> enhancedRankDaysList) {

		LocalDate historicalDate = LocalDate.now().minusDays(enhancedRankDaysList.get(rankDayIndex));
		this.setFontSize(10).setMargin(0);
		if (enhancedRankDaysList.get(rankDayIndex) == numDays) {
			this.setBold();
		}

		this.add(rank == 0 ? new Cell().add("n/a")
				: new Cell() // rank
						.setBackgroundColor(calculateRankBackgroundColor(rank), calculateRankBackgroundOpacity(rank))
						.add(String.format("%2d", rank)))
				.add(new Cell() // rate
						.setMargin(0)
						.setBackgroundColor(
								calculateCurrencyFontColor(
										new BigDecimal(fundPerformance.getPerformanceReturnsByDate(historicalDate))),
								new BigDecimal(fundPerformance.getPerformanceReturnsByDate(historicalDate)).abs()
										.multiply(new BigDecimal(1000))
										.divide(new BigDecimal(enhancedRankDaysList.get(rankDayIndex)), 4,
												RoundingMode.HALF_DOWN)
										.floatValue())
						.add(CurrencyHelper
								.formatPercentageString(fundPerformance.getPerformanceReturnsByDate(historicalDate))))
				.add(new Cell() // share price
						.setMargin(0)
						.setBackgroundColor(calculateCurrentPriceColor(fundPerformance, historicalDate),
								calculateCurrentPriceOpacity(fundPerformance, historicalDate))
						.add(CurrencyHelper.formatAsCurrencyString(
								fundPerformance.getClosestHistoricalPrice(historicalDate, 30))));
	}

	private float calculateCurrentPriceOpacity(MutualFundPerformance fundPerformance, LocalDate date) {

		BigDecimal fundPrice = fundPerformance.getPriceByDate(fundPerformance.getFund(), date, false);
		if (fundPrice == null) {
			return 0f;
		}

		BigDecimal minPrice = fundPerformance.getMinPricePairFromDate(LocalDate.now().minusYears(5)).getRight();
		BigDecimal maxPrice = fundPerformance.getMaxPricePairFromDate(fundPerformance.getOldestDate()).getRight();
		BigDecimal halfRange = maxPrice.subtract(minPrice).divide(new BigDecimal(2), RoundingMode.HALF_UP);
		BigDecimal midPrice = maxPrice.subtract(halfRange);

		if (halfRange.compareTo(BigDecimal.ZERO) == 0) {
			return 0f;
		}
		if (fundPrice.compareTo(midPrice) > 0) {
			return fundPrice.subtract(midPrice).divide(halfRange, RoundingMode.HALF_UP).floatValue();
		} else if (fundPrice.compareTo(midPrice) < 0) {
			return midPrice.subtract(fundPrice).divide(halfRange, RoundingMode.HALF_UP).floatValue();
		}
		return 0f;
	}

	private Color calculateCurrentPriceColor(MutualFundPerformance fundPerformance, LocalDate date) {

		BigDecimal minPrice = fundPerformance.getMinPricePairFromDate(LocalDate.now().minusYears(5)).getRight();
		BigDecimal maxPrice = fundPerformance.getMaxPricePairFromDate(fundPerformance.getOldestDate()).getRight();
		BigDecimal fundPrice = fundPerformance.getClosestHistoricalPrice(date, 30);
		BigDecimal range = maxPrice.subtract(minPrice);
		BigDecimal midPrice = maxPrice.subtract(range.divide(new BigDecimal(2), RoundingMode.HALF_UP));
		if (fundPrice == null) {
			return Color.BLACK;
		}
		if (fundPrice.compareTo(midPrice) > 0) {
			return Color.GREEN;
		} else if (fundPrice.compareTo(midPrice) < 0) {
			return Color.RED;
		}
		return Color.BLACK;
	}

	private Color calculateCurrencyFontColor(BigDecimal value) {
		Color fontColor = Color.GREEN;
		if (value != null) {
			if (value.compareTo(BigDecimal.ZERO) < 0) {
				fontColor = Color.RED;
			} else {
				fontColor = Color.GREEN;
			}
		}
		return fontColor;
	}


	private Color calculateRankBackgroundColor(int rank) {
		Color backgroundColor = rank <= 12 ? (rank == 0 ? Color.WHITE : Color.GREEN) : Color.RED;
		return backgroundColor;
	}

	private float calculateRankBackgroundOpacity(int rank) {
		float backgroundOpacity = rank <= 12 ? (13 - rank) / 12f : (rank - 12) / 12f;
		return backgroundOpacity;
	}

}
