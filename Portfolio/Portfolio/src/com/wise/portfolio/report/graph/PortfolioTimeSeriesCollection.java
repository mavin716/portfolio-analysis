package com.wise.portfolio.report.graph;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;

public class PortfolioTimeSeriesCollection extends TimeSeriesCollection {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public void addDividendTimeSeries(Portfolio portfolio, LocalDate startDate, LocalDate endDate) {
		for (String symbol : portfolio.getFundSymbols()) {
			PortfolioFund fund = portfolio.getFund(symbol);
			TimeSeries dividendTimeSeries = new TimeSeries(fund.getShortName() + " Dividends");

			LocalDate graphDate = startDate;
			if (graphDate == null) {
				graphDate = portfolio.getPriceHistory().getOldestDate();
			}
			if (endDate == null) {
				endDate = LocalDate.now();
			}
			BigDecimal cumulativeDividends = BigDecimal.ZERO;
			while (!graphDate.isAfter(endDate)) {
				final LocalDate date = graphDate;
				BigDecimal dividendsByDate = fund.getDistributionsForDate(date);
				if (dividendsByDate != null && dividendsByDate.compareTo(BigDecimal.ZERO) > 0) {
					cumulativeDividends = cumulativeDividends.add(dividendsByDate);
					dividendTimeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
							cumulativeDividends);

				}
				graphDate = graphDate.plusDays(1);
			}
			this.addSeries(dividendTimeSeries);
		}
		
	}

}
