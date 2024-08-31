package com.wise.portfolio.report.graph;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.time.Day;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;

import com.wise.portfolio.fund.FundPriceHistory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.ManagedPortfolio;

public class PortfolioFundsPerformanceGraph {

	 protected static final Logger logger = LogManager.getLogger(PortfolioFundsPerformanceGraph.class);

	private ManagedPortfolio portfolio;
	private Map<String, java.awt.Color> fundPaints = new HashMap<>();

	public Map<String, java.awt.Color> getFundPaints() {
		return fundPaints;
	}

	public void setFundPaints(Map<String, java.awt.Color> fundPaints) {
		this.fundPaints = fundPaints;
	}

	public Color[] getAxisPaints() {
		return axisPaints;
	}

	public void setAxisPaints(Color[] axisPaints) {
		this.axisPaints = axisPaints;
	}

	private Color[] axisPaints;

	public PortfolioFundsPerformanceGraph(ManagedPortfolio portfolio) {
		this.portfolio = portfolio;
	}

	public JFreeChart createChart(LocalDate startDate, LocalDate endDate, int i, String title,
			List<String> fundSynbols, boolean includeMovingAverage) {

		List<TimeSeriesCollection> datasets = new ArrayList<>();

		datasets.addAll(createFundPriceHistoryDatasets(fundSynbols, startDate, endDate, 5, includeMovingAverage));
		List<XYLineAndShapeRenderer> renderers = new ArrayList<>();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);

		// datasets.add(createFundWithdrawalDataset(fundSynbols, startDate, endDate));
		// XYBarRenderer barRenderer = new XYBarRenderer();
		// renderer = new XYLineAndShapeRenderer();
		// renderer.setDefaultItemLabelsVisible(true);
		// barRenderer.setShadowVisible(false);
		// renderers.add(renderer);

		JFreeChart lineChart = createTimeSeriesChart(title, datasets, renderers, null, true, true, false);
		return lineChart;
	}

	private List<TimeSeriesCollection> createFundPriceHistoryDatasets(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate, int years, boolean includeMovingAverage) {

		List<TimeSeriesCollection> datasets = new ArrayList<>();
		for (String symbol : fundSynbols) {
			PortfolioFund fund = portfolio.getFund(symbol);

			FundPriceHistory priceHistory = portfolio.getPriceHistory().getVanguardPriceHistory().get(symbol);
			LocalDate firstVanguardPriceDate = LocalDate.now().plusDays(1);
			TimeSeries timeSeries = new TimeSeries(fund.getSymbol());
			if (priceHistory != null) {

				for (Entry<LocalDate, BigDecimal> fundPriceEntry : portfolio.getPriceHistory().getVanguardPriceHistory()
						.get(symbol).getFundPricesMap().entrySet()) {

					LocalDate priceHistoryDate = fundPriceEntry.getKey();
					if (priceHistoryDate.isBefore(firstVanguardPriceDate)) {
						firstVanguardPriceDate = priceHistoryDate;
					}
					if (startDate != null) {
						if (priceHistoryDate.isBefore(startDate)) {
							continue;
						}
					}
					if (endDate != null) {
						if (priceHistoryDate.isAfter(endDate)) {
							continue;
						}
					}
					timeSeries.add(new Day(priceHistoryDate.getDayOfMonth(), priceHistoryDate.getMonthValue(),
							priceHistoryDate.getYear()), fundPriceEntry.getValue());

				}
				priceHistory = portfolio.getPriceHistory().getAlphaVantagePriceHistory().get(symbol);
				if (priceHistory != null) {

					for (Entry<LocalDate, BigDecimal> fundPriceEntry : priceHistory.getFundPricesMap().entrySet()) {
						LocalDate priceHistoryDate = fundPriceEntry.getKey();

						if (priceHistoryDate.isAfter(LocalDate.now().minusYears(years))
								&& priceHistoryDate.isBefore(firstVanguardPriceDate)) {
							try {
								timeSeries.add(new Day(priceHistoryDate.getDayOfMonth(), priceHistoryDate.getMonthValue(),
										priceHistoryDate.getYear()), fundPriceEntry.getValue());
							} catch (Exception e) {
								logger.error(
										"Exception adding alpha date " + priceHistoryDate + e.getLocalizedMessage(), e);
							}
						}
					}
				}
				TimeSeriesCollection dataset = new TimeSeriesCollection();
				dataset.addSeries(timeSeries);
				if (includeMovingAverage) {
					dataset.addSeries(MovingAverage.createMovingAverage(timeSeries, symbol + " MA", 50, 0));
					dataset.addSeries(MovingAverage.createMovingAverage(timeSeries, symbol + " 10DayMA", 10, 0));
				}
				datasets.add(dataset);
			}


		}
		return datasets;
	}

	public JFreeChart createTimeSeriesChart(String title, 
			List<TimeSeriesCollection> datasets, List<XYLineAndShapeRenderer> renderers,
			IntervalXYDataset withdrawalIntervalDataset, boolean legend, boolean tooltips, boolean urls) {

		XYPlot plot = new XYPlot();

		ValueAxis dateAxis = new DateAxis();
		// reduce the default margins
		dateAxis.setLowerMargin(0.02);
		dateAxis.setUpperMargin(0.02);
		plot.setDomainAxis(dateAxis);

		// Configure the renderer - @TODO simplify since this is customized for graph type
		for (int datasetIndex = 0; datasetIndex < datasets.size(); datasetIndex++) {
			TimeSeriesCollection timeSeriesCollection = datasets.get(datasetIndex);

			XYLineAndShapeRenderer renderer;
			if (renderers != null & renderers.size() > datasetIndex) {
				renderer = renderers.get(datasetIndex);
			} else {
				renderer = new XYLineAndShapeRenderer();
				renderer.setDefaultShapesVisible(false);
			}
			
			plot.setRenderer(datasetIndex, renderer);

			// note: use first series because moving average uses Float
			boolean isStdDeviationDataset = false;
			boolean isCurrencyFormat = timeSeriesCollection.getSeries(0).getDataItem(0)
					.getValue() instanceof BigDecimal;
			NumberFormat currencyInstance = NumberFormat.getCurrencyInstance();
			currencyInstance.setMaximumFractionDigits(0);

			String key = "";
			for (int seriesIndex = 0; seriesIndex < timeSeriesCollection.getSeries().size(); seriesIndex++) {
				java.awt.Color seriesColor = null;

				TimeSeries series = timeSeriesCollection.getSeries(seriesIndex);
				 key = (String) series.getKey();
				int indexOfSpace = key.indexOf(' ');
				String symbol = key;
				String extra = "";
				if (indexOfSpace > 0) {
					symbol = key.substring(0, key.indexOf(' '));
					extra = key.substring(indexOfSpace + 1);
				}
				if (symbol != null) {
					PortfolioFund fund = portfolio.getFund(symbol);
					if (fund != null) {
						series.setKey(fund.getShortName() + " (" + fund.getSymbol() + ") " + extra);
						seriesColor = fundPaints.get(symbol);
						if (extra.contains("MA")) {
							seriesColor = seriesColor.darker();
						}
					}
				}
				if (seriesColor == null) {
					seriesColor = axisPaints[seriesIndex];
				}
				if (key.contains("Dividends")) {
					currencyInstance.setMaximumFractionDigits(0);
					renderer.setSeriesShape(seriesIndex, ShapeUtils.createDiamond(2f));
				} else if (key.contains("Withdrawals")) {
					currencyInstance.setMaximumFractionDigits(0);
					renderer.setSeriesShape(seriesIndex, ShapeUtils.createDownTriangle(2f));
				} else if (key.contains("Shares")) {
					seriesColor = java.awt.Color.DARK_GRAY;
				} else if (key.contains("History")) {
					seriesColor = java.awt.Color.MAGENTA;
					// currencyInstance.setMaximumFractionDigits(0);
				} else if (key.contains("Balance")) {
					currencyInstance.setMaximumFractionDigits(0);
					seriesColor = new java.awt.Color(0, 0, 139); // Light Blue
					seriesColor = java.awt.Color.YELLOW;
				}
				if (extra.contains("Target")) {
					seriesColor = java.awt.Color.GREEN;
				}
				if (extra.contains("MA")) {
					seriesColor = java.awt.Color.MAGENTA.brighter().brighter();
//					seriesColor = java.awt.Color.PINK;
				}
				if (extra.contains("Std")) {
					seriesColor = seriesColor.darker().darker();
					isStdDeviationDataset = true;
				}
				renderer.setDefaultItemLabelPaint(seriesColor);
				renderer.setSeriesFillPaint(seriesIndex, seriesColor);
				renderer.setSeriesPaint(seriesIndex, seriesColor);
				renderer.setSeriesOutlinePaint(seriesIndex, seriesColor);
			}

			plot.setDataset(datasetIndex, timeSeriesCollection);

			NumberAxis valueAxis = new NumberAxis(key);
			if (isStdDeviationDataset) {
				valueAxis.setAutoRangeIncludesZero(true); // override default
			} else {
				valueAxis.setAutoRangeIncludesZero(false); // override default
			}
			if (isCurrencyFormat) {
				valueAxis.setNumberFormatOverride(currencyInstance);
			}
			plot.setRangeAxis(datasetIndex, valueAxis);

			// Map the data to the appropriate axis
			plot.mapDatasetToRangeAxis(datasetIndex, datasetIndex);

		}
		ClusteredXYBarRenderer barRenderer = new ClusteredXYBarRenderer();
		barRenderer.setUseYInterval(true);
		barRenderer.setShadowVisible(false);
		barRenderer.setDefaultFillPaint(java.awt.Color.BLACK);
		barRenderer.setDefaultItemLabelPaint(java.awt.Color.BLACK);
		barRenderer.setSeriesFillPaint(0, java.awt.Color.BLACK);
		barRenderer.setDefaultFillPaint(java.awt.Color.BLACK);
		barRenderer.setDefaultPaint(java.awt.Color.BLACK);
		barRenderer.setDefaultOutlinePaint(java.awt.Color.BLACK);
		StandardXYBarPainter painter = new StandardXYBarPainter();

		barRenderer.setBarPainter(painter);

		plot.setDataset(datasets.size(), withdrawalIntervalDataset);
		plot.setRenderer(datasets.size(), barRenderer);

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);

		return chart;

	}

}
