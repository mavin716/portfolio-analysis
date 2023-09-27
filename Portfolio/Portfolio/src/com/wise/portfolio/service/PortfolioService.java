
package com.wise.portfolio.service;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.poi.ss.formula.functions.Irr;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.time.Day;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.orsonpdf.PDFDocument;
import com.orsonpdf.PDFGraphics2D;
import com.orsonpdf.Page;
import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.graph.FundRankCell;
import com.wise.portfolio.portfolio.ManagedPortfolio;
import com.wise.portfolio.portfolio.Portfolio;

public class PortfolioService {

	public static final int CURRENCY_SCALE = 4;
	public static final Charset INPUT_CHARSET = StandardCharsets.ISO_8859_1;

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a");
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

	private static final String HISTORICAL_PRICES_FILE = "historical.csv";
	private static final String HISTORICAL_VALUES_FILE = "historicalvalues.csv";
	private static final String HISTORICAL_SHARES_FILE = "historicalshares.csv";

	private LocalDate mostRecentSharePriceDay = LocalDate.of(1999, 1, 1);
	private String basePath;

	private Map<FundCategory, BigDecimal> desiredCategoryAllocation = new HashMap<>();

	private Integer[] rankDaysArray = { 1, 3, 5, 15, 30, 60, 90, 120, 180, 270, 365, 480, 560, 740, 900, 1300 };
	private List<Long> enhancedRankDaysList = new LinkedList<>();
	private ManagedPortfolio portfolio;

	private static java.awt.Color[] axisPaints = { java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.GREEN,
			java.awt.Color.CYAN, java.awt.Color.ORANGE, java.awt.Color.PINK, java.awt.Color.DARK_GRAY,
			java.awt.Color.GRAY, java.awt.Color.MAGENTA, java.awt.Color.YELLOW, java.awt.Color.BLACK,
			new java.awt.Color(162, 42, 42), // Brown
			new java.awt.Color(251, 72, 196), // Hot Pink
			new java.awt.Color(0, 0, 139), // Lt Blue
			new java.awt.Color(95, 158, 160), // Cadet Blue
			new java.awt.Color(104, 30, 126), // Purple
			new java.awt.Color(138, 43, 226), // Blue Violet
			new java.awt.Color(210, 105, 30), // Chocolate
			new java.awt.Color(188, 143, 143), // Rosy Brown
			new java.awt.Color(250, 128, 114), // Salmon

			new java.awt.Color(0, 158, 71), // Med Green
			java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.GREEN, java.awt.Color.CYAN, java.awt.Color.ORANGE,
			java.awt.Color.PINK, java.awt.Color.DARK_GRAY, java.awt.Color.GRAY, java.awt.Color.MAGENTA,
			java.awt.Color.YELLOW, java.awt.Color.BLACK, new java.awt.Color(162, 42, 42), // Brown

			new java.awt.Color(104, 30, 126), // Purple
			new java.awt.Color(0, 0, 139), // Lt Blue
			new java.awt.Color(251, 72, 196), // Hot Pink
			new java.awt.Color(0, 158, 71), // Med Green
			java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.GREEN, java.awt.Color.CYAN, java.awt.Color.ORANGE,
			java.awt.Color.PINK, java.awt.Color.DARK_GRAY, java.awt.Color.GRAY, java.awt.Color.MAGENTA,
			java.awt.Color.YELLOW, java.awt.Color.BLACK, new java.awt.Color(104, 30, 126), // Purple
			new java.awt.Color(0, 0, 139), // Lt Blue
			new java.awt.Color(251, 72, 196), // Hot Pink
			new java.awt.Color(0, 158, 71), // Med Green
			java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.GREEN, new java.awt.Color(104, 30, 126), // Purple
			new java.awt.Color(0, 121, 231), // Lt Blue
			new java.awt.Color(251, 72, 196), // Hot Pink
			new java.awt.Color(0, 158, 71) // Med Green
	};

	private static Map<String, java.awt.Color> fundPaints = new HashMap<>();

	/**
	 * @param portfolio
	 * @throws Exception
	 * 
	 */
	public void rebalanceFunds(BigDecimal exchangeIncrement, Map<String, BigDecimal> currentfundAdjustments)
			throws Exception {
		System.out.println("\n\nRebalancing Funds based on desired fund allocation...");

		// Split the adjustments into additions and subtractions sorted by
		// absolute value
		Pair<Map<String, BigDecimal>, Map<String, BigDecimal>> surplusDeficitPair = createSurplusDeficitPair(
				currentfundAdjustments);

		Map<String, BigDecimal> surplusFundMap = surplusDeficitPair.getLeft();
		Map<String, BigDecimal> deficitFundMap = surplusDeficitPair.getRight();
		for (Entry<String, BigDecimal> surplusEntry : surplusFundMap.entrySet()) {
			String exchangeFromFundSymbol = surplusEntry.getKey();
			BigDecimal exchangeFromValue = surplusEntry.getValue().divide(new BigDecimal(2), RoundingMode.DOWN);
			System.out.println(String.format("%-60s", portfolio.getFundName(exchangeFromFundSymbol))
					+ "Surplus amount:  " + CurrencyHelper.formatAsCurrencyString(surplusEntry.getValue())
					+ ", exchange from value (1/2 surplus):  "
					+ CurrencyHelper.formatAsCurrencyString(exchangeFromValue));

			int exchangeFromIncrements = exchangeFromValue.divide(exchangeIncrement).intValue();
			if (exchangeFromIncrements > 0) {
				// look for deficits to exchange to
				for (Entry<String, BigDecimal> deficitToEntry : deficitFundMap.entrySet()) {
					String exchangeToFundSymbol = deficitToEntry.getKey();
					PortfolioFund fund = portfolio.getFund(exchangeToFundSymbol);
					if (fund.isFixedExpensesAccount()) {
						System.out.println(String.format("%-24s", fund.getShortName())
								+ "fixed expense fund, don't include in reallocation");
						continue;
					}
					BigDecimal exchangeToValue = deficitToEntry.getValue();
					System.out.println(String.format("%-24s", portfolio.getFundName(exchangeToFundSymbol))
							+ "Deficit amount:  " + CurrencyHelper.formatAsCurrencyString(exchangeToValue));

					int exchangeToIncrements = exchangeToValue.divide(exchangeIncrement).intValue();
					if (exchangeToIncrements > 0) {
						// determine exchange value for this transaction
						BigDecimal exchangeValue = exchangeIncrement
								.multiply(new BigDecimal(Math.min(exchangeFromIncrements, exchangeToIncrements)));

						System.out.println("\nExchange:  " + CurrencyHelper.formatAsCurrencyString(exchangeValue)
								+ " from:  " + String.format("%-24s", portfolio.getFundName(exchangeFromFundSymbol))
								+ " to:  " + String.format("%-24s", portfolio.getFundName(exchangeToFundSymbol))
								+ "\n");

						portfolio.adjustValue(exchangeFromFundSymbol, exchangeValue.negate());
						portfolio.adjustValue(exchangeToFundSymbol, exchangeValue);

						System.out.println(String.format("%-60s", portfolio.getFundName(exchangeFromFundSymbol))
								+ "Adjusted value:  " + CurrencyHelper
										.formatAsCurrencyString(portfolio.getFund(exchangeFromFundSymbol).getValue()));
						System.out.println(String.format("%-60s", portfolio.getFundName(exchangeToFundSymbol))
								+ "Adjusted value:  " + CurrencyHelper
										.formatAsCurrencyString(portfolio.getFund(exchangeToFundSymbol).getValue()));

						System.out.println("\n");
						exchangeFromValue = exchangeFromValue.subtract(exchangeValue);
						surplusEntry.setValue(exchangeFromValue);
						exchangeFromIncrements = exchangeFromValue.divide(exchangeIncrement).intValue();

						exchangeToValue = exchangeToValue.subtract(exchangeValue);
						deficitToEntry.setValue(exchangeToValue);
						exchangeToIncrements = exchangeToValue.divide(exchangeIncrement).intValue();

					}
					if (exchangeFromIncrements == 0) {
						break;
					}
				}
			}
		}

	}

	/**
	 * Create a pair of maps, the first containing the surplus fund adjustments and
	 * the second containing the deficit fund adjustments. Adjustments are absolute
	 * values. Then sort the new maps and return as a pair of maps.
	 * 
	 * @param fundAdjustments
	 * @return
	 */
	private Pair<Map<String, BigDecimal>, Map<String, BigDecimal>> createSurplusDeficitPair(
			Map<String, BigDecimal> fundAdjustments) {

		Map<String, BigDecimal> surplusFundMap = new LinkedHashMap<>();
		Map<String, BigDecimal> deficitFundMap = new LinkedHashMap<>();

		for (Entry<String, BigDecimal> entry : fundAdjustments.entrySet()) {
			if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
				surplusFundMap.put(entry.getKey(), entry.getValue());
			} else if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
				deficitFundMap.put(entry.getKey(), BigDecimal.ZERO.subtract(entry.getValue()));
			}
		}

		Map<String, BigDecimal> sortedSurplusFundMap = surplusFundMap.entrySet().stream()
				.sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		Map<String, BigDecimal> sortedDeficitFundMap = deficitFundMap.entrySet().stream()
				.sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		return Pair.of(sortedSurplusFundMap, sortedDeficitFundMap);
	}

	private BigDecimal performAdjustments(Map<String, BigDecimal> adjustments, BigDecimal surplus) {
		for (Entry<String, BigDecimal> entry : adjustments.entrySet()) {
			String fundSymbol = entry.getKey();
			BigDecimal adjustment = entry.getValue();

			// Make the changes
			if (adjustment.intValue() < 0) {
				surplus = surplus.subtract(adjustment);
				System.out.println("fund:  " + String.format("%60s", portfolio.getFundName(fundSymbol))
						+ " subtraction:  " + adjustment.negate());
				portfolio.adjustValue(fundSymbol, adjustment);
				System.out.println("Running surplus:  " + CurrencyHelper.formatAsCurrencyString(surplus));
			} else if (adjustment.intValue() > 0) {
				surplus = surplus.subtract(adjustment); // adjustment is
														// negative, deficit is
														// positive
				portfolio.adjustValue(fundSymbol, adjustment);
				System.out.println("fund:  " + String.format("%60s", portfolio.getFundName(fundSymbol)) + " addition:  "
						+ adjustment);
				System.out.println("Running surplus:  " + CurrencyHelper.formatAsCurrencyString(surplus));
			}
		}
		return surplus;
	}

	public void printRanking(Document document) {

		Collection<PortfolioFund> funds = portfolio.getFundMap().values();

		// Ranking is used to generate trend status text
		Map<String, LinkedList<Integer>> fundRanking = createFundRankingList(funds);

		document.add(new Paragraph("Ranking by 1 day change"));
		Table table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(1), 1);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 3 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(3), 3);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 5 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(5), 5);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 15 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(15), 15);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 30 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(30), 30);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 60 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(60), 60);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 90 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(90), 90);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 120 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(120), 120);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 180 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(180), 180);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 365 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(365), 365);
		document.add(table);
		document.add(new AreaBreak());

		long ytdDays = getYtdDays();
		document.add(new Paragraph("Ranking by YTD change (" + ytdDays + ")"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(ytdDays), ytdDays);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 480 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(480), 480);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 560 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(560), 560);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 650 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(650), 650);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 740 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(740), 740);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 830 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(830), 830);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by 920 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(920), 920);

		document.add(new Paragraph("Ranking by 1010 day change"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(1010), 1010);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking by oldest day change"));
		long oldestDay = portfolio.getPriceHistory().getDaysSinceOldestDate();
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(oldestDay), oldestDay);
		document.add(table);
		document.add(new AreaBreak());

		MutualFundPerformance performance = new MutualFundPerformance(portfolio, portfolio.getFund("VFIAX"));
		LocalDate maxPriceDate = performance.getMaxPricePair().getKey();
		long maxPriceDays = maxPriceDate.until(LocalDate.now(), ChronoUnit.DAYS);
//		table = createFundsRankingTable(portfolio, fundRanking, new CompareByPerformanceDays(maxPriceDays), maxPriceDays);

		LocalDate minPriceDate = performance.getMinPricePair().getKey();
		long minPriceDays = minPriceDate.until(LocalDate.now(), ChronoUnit.DAYS);
//		table = createFundsRankingTable(portfolio, fundRanking, new CompareByPerformanceDays(minPriceDays), minPriceDays);

		document.add(new Paragraph("Ranking by compounding difference of ranking"));
		table = createFundsRankingTable(fundRanking, new Comparator<PortfolioFund>() {
			@Override
			public int compare(PortfolioFund f1, PortfolioFund f2) {
				String fund1Symbol = f1.getSymbol();
				String fund2Symbol = f2.getSymbol();
				int numFundRankings = fundRanking.get(fund1Symbol).size();
				Float rank1Difference = fundRanking.get(fund1Symbol).get(0).floatValue();
				Float rank2Difference = fundRanking.get(fund2Symbol).get(0).floatValue();
				for (int i = numFundRankings - 1; i > 0; i--) {
					Integer f1Rank = fundRanking.get(fund1Symbol).get(i);
					rank1Difference += f1Rank / i;
					Integer f2Rank = fundRanking.get(fund2Symbol).get(i);
					rank2Difference += f2Rank / i;
				}
				return rank1Difference.compareTo(rank2Difference);
			}
		}, 360);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph("Ranking at market low (" + DATE_FORMATTER.format(minPriceDate) + ")"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(minPriceDays), minPriceDays);

		document.add(new Paragraph("Ranking at market high (" + DATE_FORMATTER.format(maxPriceDate) + ")"));
		table = createFundsRankingTable(fundRanking, new CompareByPerformanceDays(maxPriceDays), maxPriceDays);
		document.add(table);
		document.add(new AreaBreak());

		document.add(new Paragraph(
				"Ranking by name (natural sort order for Fund).  Trend shows rank movement, trend timeline, rank position"));
		table = createFundsRankingTable(fundRanking, null, 360);
		document.add(table);
		document.add(new AreaBreak());

	}

	private class CompareByPerformanceDays implements Comparator<PortfolioFund> {

		public CompareByPerformanceDays(long days2) {
			super();

			this.days = days2;

			LocalDate date = LocalDate.now();
			date = date.minusDays(days2);
			if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
				this.days++;
				date = date.minusDays(1);
			}
			if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
				this.days++;
				date = date.minusDays(1);
			}

		}

		private long days;

		@Override
		public int compare(PortfolioFund f1, PortfolioFund f2) {
			LocalDate historicalDate = portfolio.getClosestHistoricalDate(days);
			MutualFundPerformance performance = new MutualFundPerformance(portfolio, f1);

			Float fund1Rate = performance.getPerformanceRateByDate(historicalDate);
			if (fund1Rate == null) {
				return 1;
			}
			performance = new MutualFundPerformance(portfolio, f2);
			Float fund2Rate = performance.getPerformanceRateByDate(historicalDate);
			if (fund2Rate == null) {
				return -1;
			}

			return fund2Rate.compareTo(fund1Rate);
		}

	}

	private Table createFundsRankingTable(Map<String, LinkedList<Integer>> fundRanking, Comparator<PortfolioFund> c,
			long numDays) {

		// Creating a table
		float[] pointColumnWidths = { 10F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F,
				5F, 5F };
		Table table = new Table(pointColumnWidths);
		table.setFontSize(12);

		table.addHeaderCell(new Cell().add("Fund").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Returns\nDiv.\nPrice").setTextAlignment(TextAlignment.CENTER));

		for (Long rankdays : enhancedRankDaysList) {

			Cell cell1 = new Cell(); // Creating a cell
			LocalDate historicalDate = LocalDate.now().minusDays(rankdays);
			if (rankdays.equals(Long.valueOf(numDays))) {
				cell1.add(String.format("%3d", rankdays) + "*");
			} else {
				cell1.add(String.format("%3d", rankdays));
			}
			cell1.add(String.format("%8s", historicalDate.format(DateTimeFormatter.ofPattern("M/d/yy"))));

			cell1.setFontSize(10);
			table.addHeaderCell(cell1).setTextAlignment(TextAlignment.CENTER);
		}

		// Filter funds which aren't associated with fund
		Stream<PortfolioFund> fundStream = portfolio.getFundMap().values().stream()
				.filter(f -> fundRanking.get(f.getSymbol()) != null && !f.isFixedExpensesAccount());

		// Sort, if comparable provided
		if (c != null) {
			fundStream = fundStream.sorted(c);
		}

		// Print fund ranking text for each fund
		fundStream.forEach(f -> addFundRankingToTalbe(table, f, fundRanking.get(f.getSymbol()), numDays));

		BigDecimal totalChange = portfolio.getFundMap().values().stream()
				.map(f -> f.getValue().subtract(portfolio.getHistoricalValue(f, numDays)))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		LocalDate historicalDate = LocalDate.now().minusDays(numDays);
		BigDecimal totalWithdrawal = portfolio.getFundMap().values().stream()
				.map(f -> f.getWithdrawalsUpToDate(historicalDate)).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal returns = totalChange.add(totalWithdrawal);
		try {
			table.addCell(new Cell().add("Total").setTextAlignment(TextAlignment.LEFT));
			Cell cell = new Cell().setFontSize(10).add(CurrencyHelper.formatAsCurrencyString(returns));
			if (returns.compareTo(BigDecimal.ZERO) > 0) {
				cell.setBackgroundColor(Color.GREEN);
			} else {
				cell.setBackgroundColor(Color.RED);

			}
			table.addCell(cell);
			table.startNewRow();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return table;

	}

	private void addFundRankingToTalbe(Table table, PortfolioFund fund, List<Integer> rankingDays, long numDays) {

		PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
		MutualFundPerformance fundPerformance = new MutualFundPerformance(portfolio, fund);

		try {

			LocalDate historicalDate = portfolio.getClosestHistoricalDate(numDays);

			BigDecimal returns = fundPerformance.getReturnsByDate(historicalDate, false);
			if (returns == null) {
				returns = new BigDecimal(0);
			}

//			//Float rate = fundPerformance.getPerformanceRateByDate(historicalDate);
//			if (rate == null) {
//				rate = 0f;
//			}
			
			
//			LocalDateTime historicalTime = historicalDate.atTime(0, 0);
//			int years = 1;
//			long duration = Duration.between(historicalTime, LocalDateTime.now()).toDays();
//			if (duration > 365*2) {
//				years = (int) (duration / 365);
//				rate = (float) portfolio.calculateAnnualizedReturn(fund, years);
//			}

			BigDecimal income = fund.getDistributionsAfterDate(historicalDate);
			// Create a map of ranking to rates
			// Pair<index, Pair<rank, rate>>
			// TODO If symbol stored instead of fund data then can calculate data on fly
			List<Pair<Integer, Pair<Integer, MutualFundPerformance>>> ranks = new LinkedList<>();
			for (int rankDayIndex = 0; rankDayIndex < rankingDays.size(); rankDayIndex++) {
				long rankDays = enhancedRankDaysList.get(rankDayIndex);

//				Float historicalRate = null;
				LocalDate rankHistoricalDate = portfolio.getClosestHistoricalDate(rankDays);
//				historicalRate = fundPerformance.getPerformanceRateByDate(rankHistoricalDate);
//				if (historicalRate == null) {
//					historicalRate = new Float(0);
//				}

//				LocalDateTime historicalTime = rankHistoricalDate.atTime(0, 0);
//				int years = 1;
//				long duration = Duration.between(historicalTime, LocalDateTime.now()).toDays();
//				if (duration > 365*2) {
//					years = (int) (duration / 365);
//					historicalRate = (float) portfolio.calculateAnnualizedReturn(fund, years);
//				}

				int rank = rankingDays.get(rankDayIndex);
				LocalDate fundStartDate = portfolio.getPriceHistory().getFundStart(fund.getSymbol());
				if (fundStartDate.isAfter(rankHistoricalDate)) {
					rank = 0;
				}
				ranks.add(Pair.of(rankDayIndex, Pair.of(rank, fundPerformance)));
			}

			BigDecimal historicPrice = portfolio.getClosestHistoricalPrice(fund, historicalDate, 5);
			if (historicPrice == null) {
				historicPrice = BigDecimal.ZERO;
			}
			Float historicalShares = priceHistory.getSharesByDate(fund, historicalDate, false);
			if (historicalShares == null) {
				historicalShares = new Float(0);
			}
			BigDecimal historicValue = priceHistory.getFundValueByDate(fund, historicalDate, false);
			if (historicValue == null) {
				historicValue = BigDecimal.ZERO;
			}

			// Fund Name
			table.addCell(new Cell().setFontSize(10).add(fund.getShortName()).setTextAlignment(TextAlignment.LEFT));

			// Fund Returns
			Cell cell = new Cell().setMargin(0f).setFontSize(10)
					.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(returns))
							.setBackgroundColor(calculateCurrencyFontColor(returns)))
					.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(income)))
					.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(fund.getCurrentPrice()))
							.setBackgroundColor(calculateCurrentPriceColor(fundPerformance, LocalDate.now()),
									calculateCurrenPriceOpacity(fundPerformance, LocalDate.now())));
			table.addCell(cell);

			ranks.stream().forEach(rankPair -> table.addCell(new FundRankCell(rankPair.getLeft(),
					rankPair.getRight().getLeft(), rankPair.getRight().getRight(), numDays, enhancedRankDaysList)));

			table.startNewRow();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private float calculateCurrenPriceOpacity(MutualFundPerformance fundPerformance, LocalDate date) {
		LocalDate threeYearsAgo = LocalDate.now().minusYears(3);
		BigDecimal minPrice = fundPerformance.getMinPricePairFromDate(threeYearsAgo).getRight();
		BigDecimal maxPrice = fundPerformance.getMaxPricePairFromDate(threeYearsAgo).getRight();
		BigDecimal fundPrice = fundPerformance.getPriceByDate(fundPerformance.getFund(), date, false);
		BigDecimal halfRange = maxPrice.subtract(minPrice).divide(new BigDecimal(2), RoundingMode.HALF_UP);
		BigDecimal midPrice = maxPrice.subtract(halfRange);

		if (fundPrice == null) {
			return 0f;
		}
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

	private float calculateMovingAveragePriceOpacity(BigDecimal currentFundPrice, BigDecimal movingAveragePrice) {
		if (currentFundPrice == null) {
			return 0f;
		}
		BigDecimal opacity = currentFundPrice.subtract(movingAveragePrice).abs()
				.divide((currentFundPrice), RoundingMode.HALF_UP).multiply(new BigDecimal(10));
		return opacity.floatValue();
	}

	private Color calculateCurrentPriceColor(MutualFundPerformance fundPerformance, LocalDate date) {
		LocalDate threeYearsAgo = LocalDate.now().minusYears(5);
		BigDecimal minPrice = fundPerformance.getMinPricePairFromDate(threeYearsAgo).getRight();
		BigDecimal maxPrice = fundPerformance.getMaxPricePairFromDate(threeYearsAgo).getRight();
		BigDecimal fundPrice = fundPerformance.getPriceByDate(fundPerformance.getFund(), date, false);
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

	/**
	 * Create a map, key is fund symbol and the values are ordered linked list
	 * containing ranking for the days in the rankDaysArray
	 * 
	 * @param funds
	 * @return Map<fundSymbol, LinkedList<ranking>>
	 */
	private Map<String, LinkedList<Integer>> createFundRankingList(Collection<PortfolioFund> funds) {
		Map<String, LinkedList<Integer>> fundRanking = new HashMap<>();

		// Initialize ranking list
		LocalDate maxPriceDate = LocalDate.MAX;
		LocalDate minPriceDate = LocalDate.MIN;
		boolean oneDayRanking = true;
		for (PortfolioFund fund : funds) {
			if (fund.getValue().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			fundRanking.put(fund.getSymbol(), new LinkedList<>());

			if (fund.getSymbol().contentEquals("VFIAX")) {
				MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);
				double fundRate = performance.getPerformanceRateByDate(portfolio.getClosestHistoricalDate(1));
				if (fundRate == 0) {
					oneDayRanking = false;
				}
				maxPriceDate = performance.getMaxPricePair().getKey();
				minPriceDate = performance.getMinPricePair().getKey();
			}
		}

		// Create dynamic rank days list to include ytd and oldest day
		long ytdDays = getYtdDays();

		//
		Long maxPriceDays = maxPriceDate.until(LocalDate.now(), ChronoUnit.DAYS);
		Long minPriceDays = minPriceDate.until(LocalDate.now(), ChronoUnit.DAYS);
		for (int i = 0; i < rankDaysArray.length; i++) {

			if (i > 1 && ytdDays > rankDaysArray[i - 1] && ytdDays < rankDaysArray[i]) {
				enhancedRankDaysList.add(Long.valueOf(ytdDays));
			}
			if (i > 1 && maxPriceDays > rankDaysArray[i - 1] && maxPriceDays < rankDaysArray[i]) {
				enhancedRankDaysList.add(maxPriceDays);
			}
			if (i > 1 && minPriceDays > rankDaysArray[i - 1] && minPriceDays < rankDaysArray[i]) {
				enhancedRankDaysList.add(minPriceDays);
			}
			enhancedRankDaysList.add(Long.valueOf(rankDaysArray[i]));
		}
		enhancedRankDaysList.add(Long.valueOf(portfolio.getPriceHistory().getDaysSinceOldestDate()));

		AtomicInteger rank = new AtomicInteger();
		for (long numDays : enhancedRankDaysList) {
			// @TODO if no change for today then don't add to fundRanking (messes up
			// weighted order), instead use 3 day
			long days = numDays;
			if (!oneDayRanking && numDays == 1) {
				days = numDays + 2;
			}
			rank.set(1);
			funds.stream().filter(f -> fundRanking.get(f.getSymbol()) != null)
					.sorted(new CompareByPerformanceDays(days))
					.forEachOrdered(fund -> fundRanking.get(fund.getSymbol()).add(rank.getAndIncrement()));
		}

		return fundRanking;
	}

	/**
	 * Given a list of rankings, generate a text string which describes the trend
	 * performance
	 * 
	 * @param ranking
	 * @return
	 */
	private static String generateTrendStatusText(LinkedList<Integer> ranking) {
		int longTermRankIndex = ranking.size() - 1;
		int shortTermRankIndex = 2;
		int currentRankIndex = 0;

		return String.format(String.format("%30s",
				" LONG TERM RANK "
						+ generateTrendDescription(ranking.get(longTermRankIndex), ranking.get(currentRankIndex)))
				+ String.format("%30s", " SHORT TERM RANK "
						+ generateTrendDescription(ranking.get(shortTermRankIndex), ranking.get(currentRankIndex))));
	}

	/**
	 * Given a list of rankings, generate a text string which describes the trend
	 * performance
	 * 
	 * @param ranking
	 * @return
	 */
	private String generatePerformanceStatusText(PortfolioFund fund) {

		int days = rankDaysArray[rankDaysArray.length - 1];
		LocalDate historicalDate = portfolio.getClosestHistoricalDate(days);
		MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);

		StringBuilder description = new StringBuilder(" LONG TERM RETURN (" + days + "d): ");
		Float longTermPerformanceRate = performance.getPerformanceRateByDate(historicalDate);
		if (longTermPerformanceRate == null) {
			description.append(String.format("%25s", "N/A "));
		} else {
			description.append(generateRateOfReturnDescription(longTermPerformanceRate / days));
		}

		description.append("\tSHORT TERM RETURN (5d): ");
		Float shortTermPerformanceRate = performance.getPerformanceRateByDate(portfolio.getClosestHistoricalDate(5));
		if (shortTermPerformanceRate == null) {
			description.append(String.format("%25s", "N/A "));
		} else {
			description.append(generateRateOfReturnDescription(shortTermPerformanceRate / days));
		}

		return description.toString();
	}

	private static String generateTrendRankDescription(Integer rank) {
		if (rank < 6) {
			return "HIGH ";
		} else if (rank < 16) {
			return "MID ";
		} else {
			return "LOW ";
		}

	}

//	private static String generateTrendScaleDescription(LinkedList<Integer> ranking) {
//
//		int sumDifference = 0;
//		String trendScale;
//		int scale = 0;
//		// start with one week change
//		for (int i = 2; i <= ranking.size() - 1; i++) {
//			int difference = Math.abs(ranking.get(i - 1) - ranking.get(i));
//			sumDifference += difference;
//			if (difference >= 8) {
//				scale += 8;
//			} else if (difference >= 4) {
//				scale += 4;
//			} else if (difference >= 1) {
//				scale += 1;
//			}
//		}
//
//		// if (scale >= 32)
//		// {
//		// trendScale = "VOLATILE ";
//		// }
//		// else if (scale >= 24)
//		// {
//		// trendScale = "MODERATE ";
//		// }
//		// else if (scale >= 12)
//		// {
//		// trendScale = "GRADUAL ";
//		// }
//		// else if (scale >= 8)
//		// {
//		// trendScale = "SLOW ";
//		// }
//		// else
//		// {
//		// trendScale = "STEADY ";
//		// }
//
//		if (sumDifference >= 75) {
//			trendScale = "VOLATILE ";
//		} else if (sumDifference >= 60) {
//			trendScale = "MODERATE ";
//		} else if (sumDifference >= 49) {
//			trendScale = "GRADUAL ";
//		} else if (sumDifference >= 38) {
//			trendScale = "SLOW ";
//		} else if (sumDifference > 21) {
//			trendScale = "SLIGHTLY";
//		} else {
//			trendScale = "STEADY ";
//		}
//
//		return trendScale;
//	}

	private static String generateTrendDescription(Integer oldRank, Integer newRank) {
		StringBuilder description = new StringBuilder();

		if (Math.abs(oldRank - newRank) <= 2) {
			description.append("STEADY ");
		} else if (oldRank < newRank) {
			description.append("DOWN ");
		} else if (oldRank > newRank) {
			description.append("UP ");
		}
		if (Math.abs(oldRank - newRank) >= 16) {
			description.append("SIGNIFICANTLY ");
		} else if (Math.abs(oldRank - newRank) >= 8) {
			description.append("MODERATELY ");
		} else if (Math.abs(oldRank - newRank) >= 4) {
			description.append("SLIGHTLY ");
		}
		return description.toString();
	}

	private static String generateRateOfReturnDescription(float f) {
		if (f == 0D) {

		}
		// convert daily rate to yearly
		Float yearRateOfReturn = f * 365;
		StringBuilder description = new StringBuilder(CurrencyHelper.formatPercentageString(yearRateOfReturn))
				.append(" ");

		String adjective = "";
		if (yearRateOfReturn > .20d) {
			adjective = String.format("%16s", "EXTREMELY HIGH GROWTH ");
		} else if (yearRateOfReturn > .10d) {
			adjective = String.format("%16s", "VERY HIGH GROWTH ");
		} else if (yearRateOfReturn > .07d) {
			adjective = String.format("%16s", "HIGH GROWTH ");
		} else if (yearRateOfReturn > .04d) {
			adjective = String.format("%16s", "MODERATE GROWTH ");
		} else if (yearRateOfReturn > .01d) {
			adjective = String.format("%16s", "LOW GROWTH ");
		} else if (yearRateOfReturn >= 0d) {
			adjective = String.format("%16s", "STEADY ");
		} else if (yearRateOfReturn > -.01d) {
			adjective = String.format("%16s", "SLIGHTLY DOWN ");
		} else if (yearRateOfReturn > -.04d) {
			adjective = String.format("%16s", "MODERATELY DOWN ");
		} else if (yearRateOfReturn > -.07d) {
			adjective = String.format("%16s", "DOWN ");
		} else {
			adjective = String.format("%16s", "SIGNIFICANTLY DOWN ");
		}

		description.append(adjective);
		return description.toString();
	}

	public PortfolioService(String basePath) {
		this.basePath = basePath;
	}

	public void setFundColors() {

		int index = 0;
		for (String symbol : portfolio.getFundMap().keySet()) {
			fundPaints.put(symbol, axisPaints[index++]);
		}

	}

	public ManagedPortfolio createPortfolio(String fundSymbolFileName) {
		this.portfolio = new ManagedPortfolio();
		loadFundSymbolMap(fundSymbolFileName);
		setFundColors();

		return portfolio;
	}

	/**
	 * @param currentDownloadFile
	 * @param path                to find price history files
	 * 
	 * @throws IOException
	 */
	public void loadPortfolioDownloadFiles(Portfolio portfolio, String downloadFileNamePrefix,
			String currentDownloadFile) throws IOException {

		PerformanceService.setPortfolio(portfolio);
		PerformanceService.setPriceHistory(portfolio.getPriceHistory());

		// Load all download files
		LocalDate earliestDate = portfolio.getPriceHistory().getOldestDate();
		try (Stream<Path> stream = Files.list(Paths.get(basePath))) {

			List<String> filenames = stream.filter(p -> p.getFileName().toString().startsWith(downloadFileNamePrefix))
					.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
//			List<LocalDate> fileDates = new ArrayList<>();
			BigDecimal yesterdayWithdrawals = BigDecimal.ZERO;
			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			for (String filename : filenames) {
				LocalDateTime date = getDownloadFileDate(filename, true);

				if (date == null) {
					// Not a valid download file
					continue;
				}

				// TBD: analyze date to see if belongs to previous day
				// for now subtract one day

				// date = date.minus(1, ChronoUnit.DAYS);
//				if (fileDates.contains(date)) {
				// Use last file of the day, files before 6 will be dated the day before
				// continue;
//				}
//				fileDates.add(date);
				if (date.toLocalDate().isBefore(earliestDate)) {
					earliestDate = date.toLocalDate();

				}
				LocalDate dateOnly = date.toLocalDate();
				priceHistory.loadPortfolioDownloadFile(portfolio, dateOnly, basePath + "\\" + filename);

				BigDecimal withdrawals = BigDecimal.ZERO;
				for (PortfolioFund fund : portfolio.getFundMap().values()) {
					if (fund != null) {
						BigDecimal todayWithdrawals = fund.getWithdrawalTotalForDate(dateOnly);
						// Check if Sell transaction didn't get included in yesterdays download but
						// trade date was yesterday
						if (todayWithdrawals.compareTo(BigDecimal.ZERO) == 0
								&& yesterdayWithdrawals.compareTo(BigDecimal.ZERO) == 0) {
							todayWithdrawals = fund.getWithdrawalTotalForDate(dateOnly.minusDays(1));
						}
						withdrawals = withdrawals.add(todayWithdrawals);
					}
				}
				yesterdayWithdrawals = withdrawals;
				BigDecimal income = BigDecimal.ZERO;
				for (PortfolioFund fund : portfolio.getFundMap().values()) {
					if (fund != null) {
						income = income.add(fund.getDistributionsForDate(dateOnly));
					}
				}

			}
//			priceHistory.setOldestDate(earliestDate);
		}

		if (portfolio.getPriceHistory().getMostRecentDay().isAfter(mostRecentSharePriceDay)) {
			mostRecentSharePriceDay = portfolio.getPriceHistory().getMostRecentDay();
		}

		// TODO read saved values to fill in
		// portfolio.getPriceHistory().loadFundSharesHistoryFile(portfolio, basePath,
		// "historical.csv");
		// portfolio.getPriceHistory().loadPortfolioSharesFile(portfolio, basePath,
		// "historicalshares.csv");

		// Use earliest date for cost, not true cost but don't have enough history to
		// get actual cost
		for (Entry<String, Map<LocalDate, BigDecimal>> entry : portfolio.getPriceHistory().getFundPrices().entrySet()) {
			String symbol = entry.getKey();
			BigDecimal oldestPrice = entry.getValue().values().iterator().next();
			if (oldestPrice.compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			PortfolioFund fund = portfolio.getFund(symbol);
			if (fund != null) {
				fund.setCost(oldestPrice);
			}
		}

		// Load current download file
		// If current download file timestamp before 6pm, then the fund prices are from
		// the previous day.
		LocalDate currentDownloadFilePriceDate = LocalDate.now();
		if (LocalTime.now().isBefore(LocalTime.of(18, 0))) {
			currentDownloadFilePriceDate = currentDownloadFilePriceDate.minusDays(1);
		}
		loadPortfolioDownloadFile(currentDownloadFilePriceDate, currentDownloadFile);

	}

	private LocalDateTime getDownloadFileDate(String filename, boolean adjust) {

		LocalDateTime date = null;
		File file = new File(basePath + "\\" + filename);
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

			date = LocalDateTime.ofEpochSecond(attr.creationTime().to(TimeUnit.SECONDS), 0,
					ZonedDateTime.now().getOffset());

			// time = LocalTime.ofNanoOfDay(attr.creationTime().to(TimeUnit.NANOSECONDS));
//			System.out.println("File creation date:   " + date.format(DATE_FORMATTER));
//			System.out.println("File creation time:   " + date.format(TIME_FORMATTER));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return LocalDateTime.now();
		}
		
		if (adjust) {
		LocalTime cutoffTime = LocalTime.of(18, 0);
		if (date.toLocalTime().isBefore(cutoffTime)) {
			date = date.minusHours(12);
//			System.out.println("Adjusted date:   " + date.format(DATE_FORMATTER));
//			System.out.println("Adjusted time:   " + date.format(TIME_FORMATTER));
		}
	}

		return date;
//		LocalDate fileNameDate = null;
//		LocalTime fileNameTime = null;
//		LocalDateTime fileNameDateTime = null;
//		if (filename.length() == 39) {
//			String datestring = filename.substring(14, 24);
//			// Get time and create policy for multiple files on oneday, morning vs. night
//			fileNameDate = LocalDate.parse(datestring);
//			StringBuffer timestring = new StringBuffer(filename.substring(25, 31));
//			timestring.insert(2, ":");
//			timestring.insert(5, ":");
//			// Get time and create policy for multiple files on oneday, morning vs. night
//			fileNameTime = LocalTime.parse(timestring);
//			fileNameDateTime = fileNameTime.atDate(fileNameDate);
//			LocalTime cutoffTime = LocalTime.of(18, 0);
//			if (fileNameTime.isBefore(cutoffTime)) {
//				fileNameDateTime = fileNameDateTime.minusDays(1);
//			}
//			System.out.println("File name date:   " + fileNameDateTime.format(DATE_FORMATTER));
//			System.out.println("File name time:   " + fileNameDateTime.format(TIME_FORMATTER));
//
//		} else if (filename.length() == 15) {
//			// Assume this is the current downlaod for today
//			fileNameDateTime = LocalDateTime.now();
//		} else {
//			System.out.println("Ignore, filename has Invalid format:  " + filename + " size: " + filename.length());
//		}
//
//		if (adjust) {
//			LocalTime cutoffTime = LocalTime.of(18, 0);
//			if (fileNameDateTime.toLocalTime().isBefore(cutoffTime)) {
//				fileNameDateTime = fileNameDateTime.truncatedTo(ChronoUnit.DAYS).minusHours(5);
//			}
//		}
//
//		return fileNameDateTime;
	}

	private LocalDateTime getDownloadFileCreationDate(String filename, boolean adjust) {

		LocalDateTime date = null;
		File file = new File(basePath + "\\" + filename);
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

			date = LocalDateTime.ofEpochSecond(attr.creationTime().to(TimeUnit.SECONDS), 0,
					ZonedDateTime.now().getOffset());

			// time = LocalTime.ofNanoOfDay(attr.creationTime().to(TimeUnit.NANOSECONDS));
//			System.out.println("File creation date:   " + date.format(DATE_FORMATTER));
//			System.out.println("File creation time:   " + time.format(JUST_TIME_FORMATTER));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return LocalDateTime.now();
		}
//		LocalDate fileNameDate = null;
//		LocalTime fileNameTime = null;
//		if (filename.length() == 39) {
//			String datestring = filename.substring(14, 24);
//			// Get time and create policy for multiple files on oneday, morning vs. night
//			fileNameDate = LocalDate.parse(datestring);
//			StringBuffer timestring = new StringBuffer(filename.substring(25, 31));
//			timestring.insert(2, ":");
//			timestring.insert(5, ":");
//			// Get time and create policy for multiple files on oneday, morning vs. night
//			fileNameTime = LocalTime.parse(timestring);
//			LocalTime cutoffTime = LocalTime.of(18, 0);
//			if (fileNameTime.isBefore(cutoffTime)) {
//				fileNameDate = date.minusDays(1);
//			}
//			System.out.println("File name date:   " + fileNameDate.format(DATE_FORMATTER));
//			System.out.println("File name time:   " + fileNameTime.format(TIME_FORMATTER));
//
//		} else if (filename.length() == 15) {
//			// Assume this is the current downlaod for today
//			fileNameDate = LocalDate.now();
//		} else {
//			System.out.println("Ignore, filename has Invalid format:  " + filename + " size: " + filename.length());
//		}

		if (adjust) {
			LocalTime cutoffTime = LocalTime.of(18, 0);
			if (date.toLocalTime().isBefore(cutoffTime)) {
				date = date.truncatedTo(ChronoUnit.DAYS).minusHours(5);
			}
		}

		return date;
	}

	public String getFundSymbol(String name) {
		for (Entry<String, String> fundEntry : portfolio.getFundSymbolNameMap().entrySet()) {
			if (fundEntry.getValue().contains(name)) {
				return fundEntry.getKey();
			}
		}
		return null;

	}

	/**
	 * calculateWithdrawal - calculates the amounts to withdraw from each fund to
	 * achieve the totalWithdrawalAmount. The calculation uses the target
	 * percentages to determine how much to withdraw from each fund. Fixed
	 * withdrawals from the money market funds are provided separately as they are
	 * not calculated into the withdrawal strategy.
	 * 
	 * @param totalWithdrawalAmount
	 * @param cashReservesWithdrawalAmount
	 * @param federalMMWithdrawalAmount
	 * @return Map<fundSymbol, withdrawalAmount>
	 */
	public Map<String, BigDecimal> calculateWithdrawal(BigDecimal totalWithdrawalAmount,
			BigDecimal cashReservesWithdrawalAmount, BigDecimal federalMMWithdrawalAmount) {

		Map<String, BigDecimal> withdrawalMap = new LinkedHashMap<>();

		withdrawalMap.put("VMRXX", cashReservesWithdrawalAmount);
		withdrawalMap.put("VMFXX", federalMMWithdrawalAmount);

		// MM withdrawas are subtracted from withdrawal amount
		BigDecimal nonFixedWithdrawalAmount = totalWithdrawalAmount;
		nonFixedWithdrawalAmount = nonFixedWithdrawalAmount.subtract(cashReservesWithdrawalAmount);
		nonFixedWithdrawalAmount = nonFixedWithdrawalAmount.subtract(federalMMWithdrawalAmount);

		// Create map sorted by descending value of deviation
		Map<String, Pair<BigDecimal, PortfolioFund>> sortedDifferenceMap = createSortedDeviationMap(
				totalWithdrawalAmount);

		// Initialize deviation to highest fund deviation
		BigDecimal nextDeviation = BigDecimal.ZERO;
		for (Entry<String, Pair<BigDecimal, PortfolioFund>> entry : sortedDifferenceMap.entrySet()) {
			nextDeviation = entry.getValue().getLeft().setScale(4, RoundingMode.HALF_DOWN);
			break;
		}

		// withdrawl increment is one hundredth of a percent of portfolio value (why not
		// fund value???)
		BigDecimal withdrawalIncrement = portfolio.getTotalValue().divide(new BigDecimal(10000), 0,
				RoundingMode.HALF_DOWN);
		// Round down to $5
		withdrawalIncrement = withdrawalIncrement.divide(new BigDecimal(5), 0, RoundingMode.HALF_DOWN)
				.setScale(2, RoundingMode.HALF_DOWN).multiply(new BigDecimal(5));

		System.out.println("Starting deviation:  " + CurrencyHelper.formatPercentageString4(nextDeviation));

		BigDecimal runningWithdrawal = BigDecimal.ZERO;
		while (runningWithdrawal.compareTo(nonFixedWithdrawalAmount) < 0) {

			for (String fundSymbol : sortedDifferenceMap.keySet()) {

				Pair<BigDecimal, PortfolioFund> fundDifferencePair = sortedDifferenceMap.get(fundSymbol);
				BigDecimal fundDeviation = fundDifferencePair.getLeft();
				PortfolioFund fund = fundDifferencePair.getRight();

				if (fund.isClosed()) {
					continue;
				}
				if (fund.isFixedExpensesAccount()) {
					continue;
				}

				if (fundDeviation.compareTo(nextDeviation) < 0) {
					// sorted map implies all remaining funds will also be less than deviation
					break;
				}

				BigDecimal fundWithdrawalIncrement = withdrawalIncrement;
				while (fundDeviation.compareTo(nextDeviation) >= 0) {
					System.out.println("Fund:  " + fund.getShortName() + "  fund dev "
							+ CurrencyHelper.formatPercentageString4(fundDeviation));

					if (runningWithdrawal.add(fundWithdrawalIncrement).compareTo(nonFixedWithdrawalAmount) > 0) {
						fundWithdrawalIncrement = nonFixedWithdrawalAmount.subtract(runningWithdrawal);
					}
					BigDecimal runningFundWithdrawalAmount = withdrawalMap.get(fund.getSymbol());
					if (runningFundWithdrawalAmount == null) {
						runningFundWithdrawalAmount = BigDecimal.ZERO;
					}
					if (fund.getMinimumAmount() != null
							&& fund.getValue().subtract(runningFundWithdrawalAmount.add(fundWithdrawalIncrement))
									.compareTo(fund.getMinimumAmount()) <= 0) {
						break;
					}

					runningFundWithdrawalAmount = runningFundWithdrawalAmount.add(fundWithdrawalIncrement);

					BigDecimal newFundBalance = fund.getValue().subtract(runningFundWithdrawalAmount);
					BigDecimal newFundDeviation = portfolio.getFundNewBalanceDeviation(fund, newFundBalance,
							totalWithdrawalAmount);
					System.out.println("Fund:  " + fund.getShortName() + " New Fund Balance:  "
							+ CurrencyHelper.formatAsCurrencyString(newFundBalance) + " New Fund Deviation:  "
							+ CurrencyHelper.formatPercentageString4(newFundDeviation));
					fundDeviation = newFundDeviation;
					sortedDifferenceMap.put(fund.getSymbol(), Pair.of(newFundDeviation, fund));

					System.out.println("Fund:  " + fund.getShortName() + " running fund withdrawal amount "
							+ CurrencyHelper.formatAsCurrencyString(runningFundWithdrawalAmount));
					withdrawalMap.put(fund.getSymbol(), runningFundWithdrawalAmount);

					runningWithdrawal = runningWithdrawal.add(fundWithdrawalIncrement);
					System.out
							.println("running Withdrawal " + CurrencyHelper.formatAsCurrencyString(runningWithdrawal));

					if (runningWithdrawal.subtract(nonFixedWithdrawalAmount).compareTo(BigDecimal.ZERO) <= 0) {
						break;
					}

				}
			}
			nextDeviation = nextDeviation.subtract(new BigDecimal(.0001));
			System.out.println("next deviation:  " + CurrencyHelper.formatPercentageString4(nextDeviation));
		}

		BigDecimal totalWithdrawal = withdrawalMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		System.out.println(
				"total  withdrawal calculatedn from map:  " + CurrencyHelper.formatAsCurrencyString(totalWithdrawal));

		return withdrawalMap;

	}

	public Map<String, BigDecimal> calculateFixedExpensesTransfer(BigDecimal cashReservesTransferAmount,
			BigDecimal federalMMTransferAmount) {

		BigDecimal cashReservesNetWithdrawal = BigDecimal.ZERO;
		BigDecimal federalMMNetWithdrawal = BigDecimal.ZERO;
		Map<String, BigDecimal> withdrawalMap = new LinkedHashMap<>();
		if (cashReservesTransferAmount.compareTo(BigDecimal.ZERO) > 0) {
			cashReservesNetWithdrawal = cashReservesNetWithdrawal.subtract(cashReservesTransferAmount);
		}
		if (federalMMTransferAmount.compareTo(BigDecimal.ZERO) > 0) {
			federalMMNetWithdrawal = federalMMNetWithdrawal.subtract(federalMMTransferAmount);
		}
		withdrawalMap.put("VMRXX", cashReservesNetWithdrawal);
		withdrawalMap.put("VMFXX", federalMMNetWithdrawal);

		// Transfers are not incuded in deviation map because don't contriubte to bottom
		// line
		BigDecimal actualMoveMoneyAount = BigDecimal.ZERO;
		actualMoveMoneyAount = actualMoveMoneyAount.add(cashReservesTransferAmount);
		actualMoveMoneyAount = actualMoveMoneyAount.add(federalMMTransferAmount);
		// BigDecimal
		// Create map sorted by descending value of deviation
		Map<String, Pair<BigDecimal, PortfolioFund>> sortedDifferenceMap = createSortedDeviationMap(BigDecimal.ZERO);

		// Initialize deviation to first deviation found in sorted difference map
		BigDecimal nextDeviation = BigDecimal.ZERO;
		for (Entry<String, Pair<BigDecimal, PortfolioFund>> entry : sortedDifferenceMap.entrySet()) {
			nextDeviation = entry.getValue().getLeft().setScale(4, RoundingMode.HALF_DOWN);
			break;
		}

		// withdrawl increment is one percent of fund value
		BigDecimal transferIncrement = portfolio.getTotalValue().divide(new BigDecimal(10000), 0,
				RoundingMode.HALF_DOWN);
		// Round up to nearest 5
		transferIncrement = transferIncrement.divide(new BigDecimal(5), 0, RoundingMode.HALF_DOWN)
				.setScale(2, RoundingMode.HALF_DOWN).multiply(new BigDecimal(5));

		System.out.println("Starting deviation:  " + CurrencyHelper.formatPercentageString4(nextDeviation));
		BigDecimal runningWithdrawal = BigDecimal.ZERO;
		while (runningWithdrawal.compareTo(actualMoveMoneyAount) < 0) {
			for (String fundSymbol : sortedDifferenceMap.keySet()) {

				Pair<BigDecimal, PortfolioFund> fundDifferencePair = sortedDifferenceMap.get(fundSymbol);
				BigDecimal fundDeviation = fundDifferencePair.getLeft();
				PortfolioFund fund = fundDifferencePair.getRight();
				if (fund.isClosed()) {
					continue;
				}

				System.out.println("Fund:  " + fund.getShortName() + " starting dev "
						+ CurrencyHelper.formatPercentageString4(fundDeviation));

				BigDecimal fundWithdrawalIncrement = transferIncrement;

				if (fundDeviation.compareTo(nextDeviation) < 0) {
					// go to next deviation because it is an ordered list so the next funds will not
					// be
					break;
				}
				while (fundDeviation.compareTo(nextDeviation) >= 0) {
					if (runningWithdrawal.add(fundWithdrawalIncrement).compareTo(actualMoveMoneyAount) > 0) {
						fundWithdrawalIncrement = actualMoveMoneyAount.subtract(runningWithdrawal);
					}
					BigDecimal runningFundWithdrawalAmount = withdrawalMap.get(fund.getSymbol());
					if (runningFundWithdrawalAmount == null) {
						runningFundWithdrawalAmount = BigDecimal.ZERO;
					}
					if (fund.getMinimumAmount() != null
							&& fund.getValue().subtract(runningFundWithdrawalAmount.add(fundWithdrawalIncrement))
									.compareTo(fund.getMinimumAmount().add(new BigDecimal(500))) <= 0) {
						break;
					}

					runningFundWithdrawalAmount = runningFundWithdrawalAmount.add(fundWithdrawalIncrement);

					BigDecimal newFundBalance = fund.getValue().subtract(runningFundWithdrawalAmount);
					// No money is being withdrawn, total balance is same
					BigDecimal newFundDeviation = portfolio.getFundNewBalanceDeviation(fund, newFundBalance,
							BigDecimal.ZERO);
					System.out.println("Fund:  " + fund.getShortName() + " New Fund Balance:  "
							+ CurrencyHelper.formatAsCurrencyString(newFundBalance) + " New Fund Deviation:  "
							+ CurrencyHelper.formatPercentageString4(newFundDeviation));
					fundDeviation = newFundDeviation;
					sortedDifferenceMap.put(fund.getSymbol(), Pair.of(newFundDeviation, fund));

					System.out.println("Fund:  " + fund.getShortName() + " running withdrawal amount "
							+ CurrencyHelper.formatAsCurrencyString(runningFundWithdrawalAmount));
					withdrawalMap.put(fund.getSymbol(), runningFundWithdrawalAmount);

					runningWithdrawal = runningWithdrawal.add(fundWithdrawalIncrement);
					System.out.println("runningWithdrawal " + CurrencyHelper.formatAsCurrencyString(runningWithdrawal));

					if (runningWithdrawal.compareTo(actualMoveMoneyAount) >= 0) {
						break;
					}

				}
			}
			nextDeviation = nextDeviation.subtract(new BigDecimal(.0001));
			System.out.println("next deviation:  " + CurrencyHelper.formatPercentageString4(nextDeviation));
		}

		BigDecimal totalWithdrawal = withdrawalMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		System.out.println("total  withdrawalfrom map:  " + CurrencyHelper.formatAsCurrencyString(totalWithdrawal));

		return withdrawalMap;

	}

	private Map<String, Pair<BigDecimal, PortfolioFund>> createSortedDeviationMap(BigDecimal portfolioAdjustment) {

		// Sort by descending value of fund deviation
		Map<String, Pair<BigDecimal, PortfolioFund>> sortedDifferenceMap = new TreeMap<String, Pair<BigDecimal, PortfolioFund>>(
				new Comparator<String>() {
					@Override
					public int compare(String symbol1, String symbol2) {
						return portfolio.getFundDeviation(portfolio.getFund(symbol2), portfolioAdjustment)
								.compareTo(portfolio.getFundDeviation(portfolio.getFund(symbol1), portfolioAdjustment));
					}
				});

		// Populate sorted map
		for (PortfolioFund fund : portfolio.getFundMap().values()) {
			if (fund.isClosed()) {
				continue;
			}
			String symbol = fund.getSymbol();
			if (symbol == null) {
				continue;
			}
			Map<FundCategory, BigDecimal> map = portfolio.getDesiredFundAllocationMap(symbol);
			if (map == null) {
				continue;
			}

			BigDecimal deviation = portfolio.getFundDeviation(fund, portfolioAdjustment);
			sortedDifferenceMap.put(fund.getSymbol(), Pair.of(deviation, fund));
		}

		return sortedDifferenceMap;
	}

	public double calculatePortfolioAnnualizedReturn(BigDecimal portfolioReturns, int years) {

		// annReturns = (1 + ret) ^ 1/years - 1 = 0.28

//		BigDecimal portfolioReturns = currentValue.subtract(portfolioHistoricalValue).divide(currentValue, 4,
//				RoundingMode.HALF_DOWN);

		double returns = Math.pow(1 + portfolioReturns.doubleValue(), 1 / (new Double(years))) - 1;

		return returns;
	}

	/**
	 * NOTE This isn't accurate because the historical price data is only as good as
	 * the downlaoded files. There is a gap in prices so it unfairly weights the
	 * ones that are present.... I'm happy to get this to work and understand IRR.
	 * 
	 * @param symbol
	 * @return
	 */
	public Float getInternalRateReturn(PortfolioFund fund) {

		LocalDate today = LocalDate.now();

		List<Double> prices = new LinkedList<>();

		// First value is cost
		prices.add(new Double(1));

		// Add remaining prices
		long oldestDay = portfolio.getPriceHistory().getDaysSinceOldestDate();
		for (int i = 1; i < oldestDay; i++) {
			long day = oldestDay - i;
			BigDecimal value = portfolio.getClosestHistoricalPrice(fund, today.minusDays(day - 1), 30);
			if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
				prices.add(value.doubleValue());
			}
		}

		// Average first and last to make a guess
		// double guess = ((prices.get(0) + prices.get(prices.size() - 1))/
		// prices.get(0))/100;
		double guess = .1d;

		// Convert list of Double prices to array of primitive doubles
		double[] priceArray = ArrayUtils.toPrimitive(prices.toArray(new Double[0]));

		// Calculate the Internal Rate of Return using the guess
		float irr = new Float(Irr.irr(priceArray, guess)) - 1f;

		return irr;
	}

	public Double getTrendByYear(PortfolioFund fund, int trendYears) {

		PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();

		LocalDate today = LocalDate.now();

		// Find the nearest date
		LocalDate date = today.minusYears(trendYears);
		BigDecimal historicalPrice = priceHistory.getPriceByDate(fund, date, true);
		if (historicalPrice == null) {
			historicalPrice = portfolio.getClosestHistoricalPrice(fund, date, 90);
			if (historicalPrice == null) {
				return null;
			}
		}
		BigDecimal currentPrice = priceHistory.getPriceByDate(fund, today, true);
		return currentPrice.subtract(historicalPrice).divide(historicalPrice, CURRENCY_SCALE, RoundingMode.HALF_UP)
				.doubleValue();
	}

	/**
	 * @throws Exception
	 * 
	 */
	public void loadFundAllocation(String allocationFile) throws Exception {
		Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps = new HashMap<>();

		BigDecimal totalCashPercentage = BigDecimal.ZERO;
		BigDecimal totalBondPercentage = BigDecimal.ZERO;
		BigDecimal totalStockPercentage = BigDecimal.ZERO;
		BigDecimal totalIntlPercentage = BigDecimal.ZERO;
		BigDecimal totalsPercentage = BigDecimal.ZERO;

		List<List<String>> fundAllocationValues = readAllocationFile(allocationFile);
		for (List<String> allocationValues : fundAllocationValues) {
			String symbol = allocationValues.get(1);
			PortfolioFund fund = portfolio.getFund(symbol);
			if (fund == null) {
				continue;
			}

			Map<FundCategory, BigDecimal> desiredCategoryFundAllocation = new HashMap<>();

			MathContext mc = new MathContext(CURRENCY_SCALE);
			BigDecimal cashPercentage = new BigDecimal(allocationValues.get(3), mc);
			BigDecimal stockPercentage = new BigDecimal(allocationValues.get(4), mc);
			BigDecimal bondPercentage = new BigDecimal(allocationValues.get(5), mc);
			BigDecimal intlPercentage = new BigDecimal(allocationValues.get(6), mc);
			BigDecimal totalPercentage = new BigDecimal(allocationValues.get(7), mc);
			if (allocationValues.size() > 8) {
				String value = allocationValues.get(8);
				if (StringUtils.isNotEmpty(value)) {
					BigDecimal minAmount = new BigDecimal(value, mc);
					fund.setMinimumAmount(minAmount);
				}
			}
			if (allocationValues.size() > 9) {
				String value = allocationValues.get(9);
				if (StringUtils.isNotEmpty(value)) {
					BigDecimal prespent = new BigDecimal(value, mc);
					fund.setPreSpent(prespent);
				}
			}
			if (allocationValues.size() > 10 && allocationValues.get(10).length() > 0) {
				String value = allocationValues.get(10);
				fund.setLinkedFundSymbol(value);
			}

			if (allocationValues.size() > 11 && allocationValues.get(11).length() > 0) {
				fund.setFixedExpensesAccount(true);
			}

			if (allocationValues.size() > 12 && allocationValues.get(12).length() > 0) {
				fund.setClosed(true);
			}

			BigDecimal fundTotalPercentage = cashPercentage.add(stockPercentage).add(bondPercentage).add(intlPercentage)
					.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
			if (totalPercentage.compareTo(fundTotalPercentage) != 0) {
				throw new Exception("Fund:  " + fund.getShortName() + " percentages don't add up to total:  "
						+ totalPercentage + " and " + fundTotalPercentage);
			}
			totalsPercentage = totalsPercentage.add(fundTotalPercentage);
			totalCashPercentage = totalBondPercentage.add(cashPercentage);
			totalBondPercentage = totalBondPercentage.add(bondPercentage);
			totalStockPercentage = totalStockPercentage.add(stockPercentage);
			totalIntlPercentage = totalIntlPercentage.add(intlPercentage);
			if (fundTotalPercentage.compareTo(BigDecimal.ZERO) == 0) {
				fund.addCategory(FundCategory.CASH, BigDecimal.ZERO);
				fund.addCategory(FundCategory.STOCK, BigDecimal.ZERO);
				fund.addCategory(FundCategory.BOND, BigDecimal.ZERO);
				fund.addCategory(FundCategory.INTL, BigDecimal.ZERO);
				fund.addCategory(FundCategory.TOTAL, BigDecimal.ZERO);
			} else {
				fund.addCategory(FundCategory.CASH,
						CurrencyHelper.calculatePercentage(cashPercentage, fundTotalPercentage));
				fund.addCategory(FundCategory.STOCK,
						CurrencyHelper.calculatePercentage(stockPercentage, fundTotalPercentage));
				fund.addCategory(FundCategory.BOND,
						CurrencyHelper.calculatePercentage(bondPercentage, fundTotalPercentage));
				fund.addCategory(FundCategory.INTL,
						CurrencyHelper.calculatePercentage(intlPercentage, fundTotalPercentage));
				fund.addCategory(FundCategory.TOTAL, fundTotalPercentage);
			}
			desiredCategoryFundAllocation.put(FundCategory.TOTAL, fundTotalPercentage);
			desiredCategoryFundAllocation.put(FundCategory.CASH, cashPercentage);
			desiredCategoryFundAllocation.put(FundCategory.STOCK, stockPercentage);
			desiredCategoryFundAllocation.put(FundCategory.BOND, bondPercentage);
			desiredCategoryFundAllocation.put(FundCategory.INTL, intlPercentage);
			desiredFundAllocationMaps.put(fund.getSymbol(), desiredCategoryFundAllocation);

		}

		desiredCategoryAllocation.put(FundCategory.CASH, totalCashPercentage);
		desiredCategoryAllocation.put(FundCategory.BOND, totalBondPercentage);
		desiredCategoryAllocation.put(FundCategory.STOCK, totalStockPercentage);
		desiredCategoryAllocation.put(FundCategory.INTL, totalIntlPercentage);

		portfolio.setDesiredFundAllocationMaps(desiredFundAllocationMaps);
	}

	private void loadFundSymbolMap(String filename) {
		Map<String, String> fundSymbolNameMap = new HashMap<>();
		List<List<String>> fundAllocationValues = readAllocationFile(basePath + filename);
		for (List<String> values : fundAllocationValues) {
			if (values.size() >= 2) {
				String symbol = values.get(1);
				String name = values.get(2);
				fundSymbolNameMap.put(symbol, name);
			} else {
				System.out.println("invalid allocation record.  size:  " + values.size());
				break;
			}
		}
		portfolio.setFundSymbolNameMap(fundSymbolNameMap);
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByFundPrice(Map<K, V> map) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		list.sort(Entry.comparingByValue());

		Map<K, V> result = new LinkedHashMap<>();
		for (Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	public void saveHistoricalPrices(String filename) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(basePath + "tempHistoricalPrices.csv"))) {
			StringBuilder headingLineStringBuilder = new StringBuilder("Symbol,Name");

			// Build ordered set of dates (some funds may have different set of
			// populated dates)
			Set<LocalDate> dates = new TreeSet<>();
			for (Map<LocalDate, BigDecimal> entry : portfolio.getPriceHistory().getFundPrices().values()) {
				for (LocalDate date : entry.keySet()) {
					dates.add(date);
				}
			}
			for (LocalDate date : dates) {
				headingLineStringBuilder.append(",").append(date.toString());
			}
			headingLineStringBuilder.append("\n");
			writer.write(headingLineStringBuilder.toString());

			// Write fund lines for each date
			for (String symbol : portfolio.getFundMap().keySet()) {
				if (symbol == null) {
					continue;
				}
				StringBuilder fundStringBuilder = new StringBuilder(symbol).append("," + portfolio.getFundName(symbol));
				Map<LocalDate, BigDecimal> fundHistoricalPrices = portfolio.getPriceHistory().getFundPrices()
						.get(symbol);
				for (LocalDate date : dates) {
					BigDecimal price = fundHistoricalPrices.get(date);
					if (price == null) {
						fundStringBuilder.append(",");
					} else {
						fundStringBuilder.append(",").append(price.toString());
					}
				}
				fundStringBuilder.append("\n");
				writer.write(fundStringBuilder.toString());

			}
			writer.flush();
			writer.close();
			Files.copy(Paths.get(basePath + "tempHistoricalPrices.csv"), Paths.get(basePath + filename),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Exception:  " + e.getMessage());
		}
	}

	public void saveHistoricalValue(String historicalValuesFile) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(basePath + historicalValuesFile))) {
			StringBuilder headingLineStringBuilder = new StringBuilder("Symbol,Name");

			// Build ordered set of dates (some funds may have different set of
			// populated dates)
			Set<LocalDate> dates = new TreeSet<>();
			for (Map<LocalDate, BigDecimal> entry : portfolio.getPriceHistory().getFundPrices().values()) {
				LocalDate previousDate = null;
				for (LocalDate date : entry.keySet()) {
					if (previousDate != null && previousDate.plusDays(5).compareTo(date) > 0) {
						continue;
					}
					dates.add(date);
					previousDate = date;
				}
			}

			// Filter the dates to once a week at most
			for (LocalDate date : dates) {
				headingLineStringBuilder.append(",").append(date.toString());
			}
			headingLineStringBuilder.append("\n");
			writer.write(headingLineStringBuilder.toString());

			Map<LocalDate, BigDecimal> totalsByDate = new TreeMap<>();
			// Write fund lines for each date
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				String symbol = fund.getSymbol();
				if (symbol == null) {
					continue;
				}
				StringBuilder fundStringBuilder = new StringBuilder(symbol).append("," + portfolio.getFundName(symbol));
				for (LocalDate date : dates) {
					BigDecimal fundValueByDate = portfolio.getValueByDate(fund, date);
					if (fundValueByDate.compareTo(BigDecimal.ZERO) == 0) {
						fundStringBuilder.append(",");
					} else {
						fundStringBuilder.append(",").append(fundValueByDate.setScale(2, BigDecimal.ROUND_UP));
					}
					BigDecimal runningTotalByDate = totalsByDate.get(date);
					if (runningTotalByDate == null) {
						runningTotalByDate = BigDecimal.ZERO;
					}
					runningTotalByDate = runningTotalByDate.add(fundValueByDate);
					totalsByDate.put(date, runningTotalByDate);

				}
				fundStringBuilder.append("\n");
				writer.write(fundStringBuilder.toString());

			}
			StringBuilder totalstringBuilder = new StringBuilder("\n,TOTAL");
			for (LocalDate date : dates) {
				BigDecimal total = totalsByDate.get(date);

				totalstringBuilder.append(",").append(total.setScale(2, BigDecimal.ROUND_UP));
			}
			writer.write(totalstringBuilder.toString());

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveHistoricalShares(String historicalSharesFile) {
		PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(basePath + historicalSharesFile))) {
			StringBuilder headingLineStringBuilder = new StringBuilder("Symbol,Name");

			// Build ordered set of dates (some funds may have different set of
			// populated dates)
			Set<LocalDate> dates = new TreeSet<>();
			for (Map<LocalDate, Float> entry : priceHistory.getFundShares().values()) {
				LocalDate previousDate = null;
				for (LocalDate date : entry.keySet()) {
					if (previousDate != null && previousDate.plusDays(5).compareTo(date) > 0) {
						continue;
					}
					dates.add(date);
					previousDate = date;
				}
			}

			// Filter the dates to once a week at most
			for (LocalDate date : dates) {
				headingLineStringBuilder.append(",").append(date.toString());
			}
			headingLineStringBuilder.append("\n");
			writer.write(headingLineStringBuilder.toString());

			// Write fund lines for each date
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				String symbol = fund.getSymbol();
				if (symbol == null) {
					continue;
				}
				StringBuilder fundStringBuilder = new StringBuilder(symbol).append("," + portfolio.getFundName(symbol));
				for (LocalDate date : dates) {
					Float fundSharesByDate = priceHistory.getSharesByDate(fund, date, false);
					if (fundSharesByDate == null) {
						fundStringBuilder.append(",");
					} else {
						fundStringBuilder.append(",").append(fundSharesByDate);
					}

				}
				fundStringBuilder.append("\n");
				writer.write(fundStringBuilder.toString());

			}

			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Exception:  " + e.getMessage());
		}
	}

	private List<List<String>> readAllocationFile(String filename) {
		List<List<String>> fundAllocationValues = null;
		try (BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
			String firstLine = br.readLine(); // first line is headings
			if (firstLine == null)
				return null;
			fundAllocationValues = br.lines().map(line -> Arrays.asList(line.split(",")))
					.filter(line -> line.size() > 2).collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fundAllocationValues;
	}

	/**
	 * @return Map of symbol, adjustmentAmount
	 * @throws Exception
	 */
	public Map<String, BigDecimal> calculateAdjustments() throws Exception {
		// Map of <Symbol, Adjustment>
		Map<String, BigDecimal> rawAdjustments = new TreeMap<>();

		BigDecimal totalValue = portfolio.getTotalValue();
		BigDecimal totalAdjustments = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentage = BigDecimal.ZERO;
		BigDecimal totalDesiredPercentage = BigDecimal.ZERO;

		Map<String, BigDecimal> runningBalances = new HashMap<>();

		for (PortfolioFund fund : getFundsSortedByDifferenceInValue()) {
			BigDecimal currentFundValue = fund.getValue();
			runningBalances.put(fund.getSymbol(), currentFundValue);
			BigDecimal currentPercentage = CurrencyHelper.calculatePercentage(currentFundValue, totalValue);
			totalCurrentPercentage = totalCurrentPercentage.add(currentPercentage);
			BigDecimal desiredValue = getDesiredFundValue(fund.getSymbol());
			BigDecimal desiredPercentage = CurrencyHelper.calculatePercentage(desiredValue, totalValue);
			totalDesiredPercentage = totalDesiredPercentage.add(desiredPercentage);

			if (currentFundValue.equals(BigDecimal.ZERO) && desiredValue.equals(BigDecimal.ZERO)) {
				continue;
			}
			if (desiredValue == null) {
				throw new Exception("No desired value for fund:  " + fund.getShortName());
			}
			BigDecimal difference = currentFundValue.subtract(desiredValue);
			BigDecimal fundAdjustment = difference;
			BigDecimal afterValue = fund.getValue().subtract(fundAdjustment.abs());
			if (afterValue.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
//			if (difference.compareTo(BigDecimal.ZERO) > 0 && fund.getMinimumAmount() != null) {
//				BigDecimal overAmount = afterValue.subtract(fund.getMinimumAmount());
//				if (overAmount.compareTo(new BigDecimal(100)) < 0) {
//					fundAdjustment = BigDecimal.ZERO;
//				}
//			}

			rawAdjustments.put(fund.getSymbol(), fundAdjustment);
			totalAdjustments = totalAdjustments.add(fundAdjustment);

		}
		Pair<Map<String, BigDecimal>, Map<String, BigDecimal>> surplusDeficitPair = createSurplusDeficitPair(
				rawAdjustments);

		Map<String, BigDecimal> adjustments = new HashMap<>();
		BigDecimal exchangeIncrement = new BigDecimal(100);
		Map<String, BigDecimal> surplusFundMap = surplusDeficitPair.getLeft();
		Map<String, BigDecimal> deficitFundMap = surplusDeficitPair.getRight();
		for (Entry<String, BigDecimal> surplusEntry : surplusFundMap.entrySet()) {
			String exchangeFromFundSymbol = surplusEntry.getKey();
			BigDecimal exchangeFromValue = surplusEntry.getValue().divide(new BigDecimal(2), RoundingMode.DOWN);
			if (exchangeFromValue.compareTo(new BigDecimal(100)) < 0) {
				continue;
			}
			System.out.println(String.format("%-60s", portfolio.getFundName(exchangeFromFundSymbol))
					+ "Surplus amount:  " + CurrencyHelper.formatAsCurrencyString(surplusEntry.getValue())
					+ ", exchange from value (1/2 surplus):  "
					+ CurrencyHelper.formatAsCurrencyString(exchangeFromValue));

			int exchangeFromIncrements = exchangeFromValue.divide(exchangeIncrement).intValue();
			if (exchangeFromIncrements > 0) {
				// look for deficits to exchange to
				for (Entry<String, BigDecimal> deficitToEntry : deficitFundMap.entrySet()) {
					String exchangeToFundSymbol = deficitToEntry.getKey();
					PortfolioFund fund = portfolio.getFund(exchangeToFundSymbol);
					if (fund.isFixedExpensesAccount()) {
						System.out.println(String.format("%-24s", fund.getShortName())
								+ "fixed expense fund, don't include in reallocation");
						continue;
					}
					BigDecimal exchangeToValue = deficitToEntry.getValue();
					System.out.println(String.format("%-24s", portfolio.getFundName(exchangeToFundSymbol))
							+ "Deficit amount:  " + CurrencyHelper.formatAsCurrencyString(exchangeToValue));

					int exchangeToIncrements = exchangeToValue.divide(exchangeIncrement).intValue();
					if (exchangeToIncrements > 0) {
						// determine exchange value for this transaction
						BigDecimal exchangeValue = exchangeIncrement
								.multiply(new BigDecimal(Math.min(exchangeFromIncrements, exchangeToIncrements)));

						System.out.println("\nExchange:  " + CurrencyHelper.formatAsCurrencyString(exchangeValue)
								+ " from:  " + String.format("%-24s", portfolio.getFundName(exchangeFromFundSymbol))
								+ " to:  " + String.format("%-24s", portfolio.getFundName(exchangeToFundSymbol))
								+ "\n");

						BigDecimal fundExchangeFromValue = adjustments.get(exchangeFromFundSymbol);
						if (fundExchangeFromValue == null) {
							fundExchangeFromValue = BigDecimal.ZERO;
						}
						adjustments.put(exchangeFromFundSymbol, fundExchangeFromValue.add(exchangeValue));
						runningBalances.put(exchangeFromFundSymbol,
								runningBalances.get(exchangeFromFundSymbol).subtract(exchangeValue));
						// portfolio.adjustValue(exchangeFromFundSymbol, exchangeValue.negate());
						BigDecimal fundExchangeToValue = adjustments.get(exchangeToFundSymbol);
						if (fundExchangeToValue == null) {
							fundExchangeToValue = BigDecimal.ZERO;
						}
						adjustments.put(exchangeToFundSymbol, fundExchangeToValue.add(exchangeValue.negate()));
						runningBalances.put(exchangeToFundSymbol,
								runningBalances.get(exchangeToFundSymbol).add(exchangeValue));
						// portfolio.adjustValue(exchangeToFundSymbol, exchangeValue);

						System.out.println(String.format("%-60s", portfolio.getFundName(exchangeFromFundSymbol))
								+ "Adjusted value:  " + CurrencyHelper
										.formatAsCurrencyString(portfolio.getFund(exchangeFromFundSymbol).getValue()));
						System.out.println(String.format("%-60s", portfolio.getFundName(exchangeToFundSymbol))
								+ "Adjusted value:  " + CurrencyHelper
										.formatAsCurrencyString(portfolio.getFund(exchangeToFundSymbol).getValue()));

						System.out.println("\n");
						exchangeFromValue = exchangeFromValue.subtract(exchangeValue);
						surplusEntry.setValue(exchangeFromValue);
						exchangeFromIncrements = exchangeFromValue.divide(exchangeIncrement).intValue();

						exchangeToValue = exchangeToValue.subtract(exchangeValue);
						deficitToEntry.setValue(exchangeToValue);
						exchangeToIncrements = exchangeToValue.divide(exchangeIncrement).intValue();

					}
					if (exchangeFromIncrements == 0) {
						break;
					}
				}
			}
		}

		return adjustments;

	}

	private Collection<PortfolioFund> getFundsSortedByDifferenceInValue() {
		return portfolio.getFundMap().values().stream()
				.sorted((f1, f2) -> getDifferenceInFundValue(f2).compareTo(getDifferenceInFundValue(f1)))
				.collect(Collectors.toList());

	}

	public BigDecimal getDifferenceInFundValue(PortfolioFund fund) {
		return fund.getValue().subtract(getDesiredFundValue(fund.getSymbol()));
	}

	public BigDecimal getDesiredFundValue(String symbol) {
		BigDecimal fundTotalPercentage = null;
		try {
			PortfolioFund fund = portfolio.getFund(symbol);
			BigDecimal minAmount = fund.getMinimumAmount();
			if (minAmount != null) {
				fundTotalPercentage = minAmount.divide(portfolio.getTotalValue(), 4, RoundingMode.UP);
			} else {
				fundTotalPercentage = portfolio.getDesiredFundAllocationMap(symbol).get(FundCategory.TOTAL);
			}

		} catch (Exception e) {
			System.out.println("Exception getting desired fund value for " + symbol + "error:  " + e.getMessage());
			return new BigDecimal(0);
		}
		if (fundTotalPercentage.floatValue() == 0f) {
			return BigDecimal.ZERO;
		}
		BigDecimal desiredValue = portfolio.getTotalValue().multiply(fundTotalPercentage);
		return desiredValue;
	}

	public static double xcalculateInternalRateOfReturn(double[] values, double guess) {
		int maxIterationCount = 10000;
		double absoluteAccuracy = 1E-7;
		double x0 = guess;
		double x1;
		int i = 0;
		while (i < maxIterationCount) {
			// the value of the function (NPV) and its derivate can be
			// calculated in the same loop
			double fValue = 0;
			double fDerivative = 0;
			for (int k = 0; k < values.length; k++) {
				if (values[k] == 0) {
					continue;
				}
				fValue += values[k] / Math.pow(1.0 + x0, k);
				fDerivative += -k * values[k] / Math.pow(1.0 + x0, k + 1);
				if (fDerivative == 0) {
					System.out.println("fValue:  " + fValue + " fDerivitave:  " + fDerivative);
					return Double.NaN;
				}
			}
			// the essense of the Newton-Raphson Method
			x1 = x0 - fValue / fDerivative;
			if (Math.abs(x1 - x0) <= absoluteAccuracy) {
				return x1;
			}
			x0 = x1;
			++i;
		}
		// maximum number of iterations is exceeded
		return Double.NaN;
	}

	public void printPerformanceLineGraphs(List<String> fundSynbols, Document document, PdfDocument pdfDocument,
			LocalDate startDate, LocalDate endDate) {

		List<TimeSeriesCollection> datasets = new ArrayList<>();
		datasets.add(createFundPriceHistoryDataset(fundSynbols, startDate, endDate, true));
		List<XYItemRenderer> renderers = new ArrayList<>();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);

		// datasets.add(createFundWithdrawalDataset(fundSynbols, startDate, endDate));
		// XYBarRenderer barRenderer = new XYBarRenderer();
		// renderer = new XYLineAndShapeRenderer();
		// renderer.setDefaultItemLabelsVisible(true);
		// barRenderer.setShadowVisible(false);
		// renderers.add(renderer);

		JFreeChart lineChart = createTimeSeriesChart("Fund Price History", null, null, datasets, renderers, true, true,
				false);

		addChartToDocument(lineChart, pdfDocument, document);

	}

	public void printFundPerformanceLineGraph(String fundSynbol, Document document, PdfDocument pdfDocument,
			LocalDate startDate, LocalDate endDate) {

		PortfolioFund fund = portfolio.getFund(fundSynbol);

		TimeSeriesCollection dataset;
		portfolio.getFund(fundSynbol).getName();
		List<TimeSeriesCollection> datasets = new ArrayList<>();
		List<String> fundSynbols = new ArrayList<>();
		fundSynbols.add(fundSynbol);

		dataset = createFundBalanceDataset(fundSynbols, startDate, endDate);
		dataset.getSeries(0).setKey("Fund Balance");
		datasets.add(dataset);
		List<XYItemRenderer> renderers = new ArrayList<>();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);

		dataset = createFundShareDataset(fundSynbols, startDate, endDate);
		dataset.getSeries(0).setKey("Fund Shares");
		datasets.add(dataset);
		renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderer.setSeriesVisibleInLegend(0, true);
		renderers.add(renderer);

		dataset = createFundPriceHistoryDataset(fundSynbols, startDate, endDate, false);
		dataset.getSeries(0).setKey("Price History");
		datasets.add(dataset);
		renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);

//		TimeSeriesCollection stdDataset = new TimeSeriesCollection();
//		TimeSeries priceHistoryTimeSeries = dataset.getSeries(0);
//		stdDataset.addSeries(createStdDeviationSeries(priceHistoryTimeSeries, fundSynbols.get(0) + " STD", 50, 50));
//		stdDataset.getSeries(0).setKey("Price Std Deviation");
//		datasets.add(stdDataset);
//		renderer = new XYLineAndShapeRenderer();
//		renderer.setDefaultShapesVisible(false);
//		renderers.add(renderer);

		JFreeChart lineChart = createTimeSeriesChart(fund.getName() + " Price History", null, null, datasets, renderers,
				true, true, false);

		addChartToDocument(lineChart, pdfDocument, document);

	}

	private TimeSeriesCollection createFundDividendDataset(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {
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
			dataset.addSeries(dividendTimeSeries);
		}
		return dataset;
	}

	private static void addChartToDocument(JFreeChart lineChart, PdfDocument pdfDocument, Document document) {
		// Draw the chart into a transition PDF
		PDFDocument doc = new PDFDocument();
		Rectangle bounds = new Rectangle(1200, 720);
		Page page = doc.createPage(bounds);
		PDFGraphics2D g2 = page.getGraphics2D();
		lineChart.draw(g2, bounds);

		// Add the graph PDF image into the document
		try {
			PdfReader reader = new PdfReader(new ByteArrayInputStream(doc.getPDFBytes()));
			PdfDocument chartDoc = new PdfDocument(reader);
			PdfFormXObject chart = chartDoc.getFirstPage().copyAsFormXObject(pdfDocument);
			chartDoc.close();
			Image chartImage = new Image(chart);
			document.add(chartImage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printBalanceLineGraphs(Document document, PdfDocument pdfDocument, LocalDate startDate,
			LocalDate endDate) {

		StandardXYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator();

		List<XYItemRenderer> renderers = new ArrayList<>();
		List<TimeSeriesCollection> datasets = new ArrayList<>();
		datasets.add(createBalanceDataset(startDate, endDate));
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesToolTipGenerator(0, toolTipGenerator);
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);
		datasets.add(createDividendDataset(startDate, endDate));
		renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesToolTipGenerator(0, toolTipGenerator);
		renderer.setDefaultShapesVisible(true);
		renderers.add(renderer);
		TimeSeriesCollection withdrawalDataset = createWithdrawalDataset(startDate, endDate);
		datasets.add(withdrawalDataset);
		ClusteredXYBarRenderer barRenderer = new ClusteredXYBarRenderer();
//		List<String> toolTips = new ArrayList<String>();
//		toolTips.add("tooltip1");
//		toolTips.add("tooltip2");
//		toolTips.add("tooltip3");
//		toolTipGenerator.
		barRenderer.setSeriesToolTipGenerator(0, toolTipGenerator);

		barRenderer.setShadowVisible(false);
		renderers.add(barRenderer);
		JFreeChart lineChart = createTimeSeriesChart("Balance", null, null, datasets, renderers, true, true, false);

		// TODO Attempt to configure stroke to be darker, doesn't seem to work....
//		XYPlot plot = (XYPlot) lineChart.getPlot();
//		XYItemRenderer r = plot.getRenderer();
//		if (r instanceof XYLineAndShapeRenderer) {
//			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
//			renderer.setDefaultStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL), false);
//			renderer.setDefaultOutlineStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
//		}

		addChartToDocument(lineChart, pdfDocument, document);

	}

	public JFreeChart createTimeSeriesChart(String title, String timeAxisLabel, String valueAxisLabel,
			List<TimeSeriesCollection> datasets, List<XYItemRenderer> renderers, boolean legend, boolean tooltips,
			boolean urls) {

		XYPlot plot = new XYPlot();

		// reduce the default margins
		ValueAxis dateAxis = new DateAxis(timeAxisLabel);
		dateAxis.setLowerMargin(0.02);
		dateAxis.setUpperMargin(0.02);
		plot.setDomainAxis(dateAxis);

		for (int datasetIndex = 0; datasetIndex < datasets.size(); datasetIndex++) {
			TimeSeriesCollection timeSeriesCollection = datasets.get(datasetIndex);

			XYItemRenderer renderer;
			if (renderers != null & renderers.size() > datasetIndex) {
				renderer = renderers.get(datasetIndex);
			} else {
				renderer = new XYLineAndShapeRenderer();
			}
			plot.setRenderer(datasetIndex, renderer);

			// note: use first series because moving average uses Float
			boolean isStdDeviationDataset = false;
			boolean isCurrencyFormat = timeSeriesCollection.getSeries(0).getDataItem(0)
					.getValue() instanceof BigDecimal;
			NumberFormat currencyInstance = NumberFormat.getCurrencyInstance();

			for (int seriesIndex = 0; seriesIndex < timeSeriesCollection.getSeries().size(); seriesIndex++) {
				java.awt.Color seriesColor = null;

				TimeSeries series = timeSeriesCollection.getSeries(seriesIndex);
				String key = (String) series.getKey();
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
						series.setKey(fund.getShortName() + " " + extra);
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
					seriesColor = java.awt.Color.BLACK;
				} else if (key.contains("History")) {
					seriesColor = java.awt.Color.RED;
					// currencyInstance.setMaximumFractionDigits(0);
				} else if (key.contains("Balance")) {
					currencyInstance.setMaximumFractionDigits(0);
					seriesColor = java.awt.Color.GREEN;
				}
				if (extra.contains("Target")) {
					seriesColor = java.awt.Color.YELLOW;
				}
				if (extra.contains("MA")) {
					seriesColor = seriesColor.darker();
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

			NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
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

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		// ChartFactory.getChartTheme().apply(chart);
		return chart;

	}

	public void printFundBalanceLineGraphs(String title, List<String> fundSynbols, Document document,
			PdfDocument pdfDocument, LocalDate startDate, LocalDate endDate) {

		List<XYItemRenderer> renderers = new ArrayList<>();
		List<TimeSeriesCollection> datasets = new ArrayList<>();
		datasets.add(createFundBalanceDataset(fundSynbols, startDate, endDate));
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);
		datasets.add(createFundDividendDataset(fundSynbols, startDate, endDate));
		renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(true);
		renderers.add(renderer);
		datasets.add(createFundWithdrawalDataset(fundSynbols, startDate, endDate));
		XYBarRenderer barRenderer = new XYBarRenderer();
		renderer = new XYLineAndShapeRenderer();
		// renderer.setDefaultItemLabelsVisible(true);
		barRenderer.setShadowVisible(false);
		renderers.add(renderer);
		JFreeChart lineChart = createTimeSeriesChart(title, null, null, datasets, renderers, true, true, false);

		addChartToDocument(lineChart, pdfDocument, document);

	}

	private TimeSeriesCollection createFundWithdrawalDataset(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		TimeSeries timeSeries;
		for (String symbol : fundSynbols) {
			PortfolioFund fund = portfolio.getFund(symbol);
			BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;

			LocalDate graphDate = startDate;
			if (graphDate == null) {
				graphDate = portfolio.getPriceHistory().getOldestDate();
			}
			if (endDate == null) {
				endDate = LocalDate.now();
			}
			timeSeries = new TimeSeries(symbol + " Withdrawals");
			timeSeries.add(new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
					cumulativeWithdrawals);

			while (!graphDate.isAfter(endDate)) {
				final LocalDate date = graphDate;
				BigDecimal withdrawalsByDate = fund.getWithdrawalTotalForDate(date);
				if (withdrawalsByDate != null && withdrawalsByDate.compareTo(BigDecimal.ZERO) > 0) {
					cumulativeWithdrawals = cumulativeWithdrawals.subtract(withdrawalsByDate);
					timeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
							cumulativeWithdrawals);

				}
				graphDate = graphDate.plusDays(1);
			}
			dataset.addSeries(timeSeries);
		}

		return dataset;
	}

	private TimeSeriesCollection createFundBalanceDataset(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate) {

		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {

			PortfolioFund fund = portfolio.getFund(symbol);
			BigDecimal minumumAmount = fund.getMinimumAmount();
			LocalDate graphDate = startDate;
			if (graphDate == null) {
				graphDate = portfolio.getPriceHistory().getOldestDate();
			}
			if (endDate == null) {
				endDate = LocalDate.now();
			}
			TimeSeries balanceTimeSeries = new TimeSeries(fund.getShortName());
			TimeSeries targetBalanceTimeSeries = new TimeSeries(fund.getShortName() + " Target Balance");

			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			while (!graphDate.isAfter(endDate)) {

				BigDecimal fundBalanceByDate = priceHistory.getFundValueByDate(fund, graphDate, true);

				if (fundBalanceByDate != null && fundBalanceByDate.compareTo(BigDecimal.ZERO) > 0) {
					balanceTimeSeries.add(
							new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
							fundBalanceByDate);

					BigDecimal portfolioBalanceByDate = portfolio.getTotalValueByDate(graphDate);
					BigDecimal fundTargetAllocation = portfolio.getDesiredFundAllocationMaps().get(fund.getSymbol())
							.get(FundCategory.TOTAL);
					BigDecimal fundTargetBalanceByDate = portfolioBalanceByDate.multiply(fundTargetAllocation);
					if (minumumAmount == null || fundTargetBalanceByDate.compareTo(minumumAmount) > 0) {
						targetBalanceTimeSeries.add(
								new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
								fundTargetBalanceByDate);
					} else {
						targetBalanceTimeSeries.add(
								new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
								minumumAmount);

					}

				}
				graphDate = graphDate.plusDays(1);
			}
			dataset.addSeries(balanceTimeSeries);
			dataset.addSeries(targetBalanceTimeSeries);
		}
		return dataset;
	}

	private TimeSeriesCollection createFundPriceHistoryDataset(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate, boolean isWeekly) {

		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {
			PortfolioFund fund = portfolio.getFund(symbol);

			LocalDate lastPriceDate = LocalDate.MIN;
			TimeSeries timeSeries = new TimeSeries(fund.getSymbol());
			for (Entry<LocalDate, BigDecimal> fundPriceEntry : portfolio.getPriceHistory().getFundPrices().get(symbol)
					.entrySet()) {

				LocalDate priceHistoryDate = fundPriceEntry.getKey();
				if (isWeekly && priceHistoryDate.compareTo(lastPriceDate.plusDays(7)) < 0) {
					continue;
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
			dataset.addSeries(timeSeries);

			dataset.addSeries(MovingAverage.createMovingAverage(timeSeries, symbol + " MA", 50, 0));

		}
		return dataset;
	}

	public TimeSeries createStdDeviationSeries(TimeSeries source, String name, int periodCount, int skip) {

		Args.nullNotPermitted(source, "source");
		if (periodCount < 1) {
			throw new IllegalArgumentException("periodCount must be greater " + "than or equal to 1.");
		}

		TimeSeries result = new TimeSeries(name);

		if (source.getItemCount() > 0) {

			// if the initial averaging period is to be excluded, then
			// calculate the index of the
			// first data item to have an average calculated...
			long firstSerial = source.getTimePeriod(0).getSerialIndex() + skip;
			StandardDeviation stdDeviation = new StandardDeviation(false);
			double[] items = new double[periodCount];

			int itemIndex = 0;
			for (int i = source.getItemCount() - 1; i >= 0; i--) {

				// get the current data item...
				RegularTimePeriod period = source.getTimePeriod(i);
				// TimeSeriesDataItem item = source.getDataItem(i - offset);
				TimeSeriesDataItem item1 = source.getDataItem(i);

				items[itemIndex++] = item1.getValue().doubleValue();
				long serial = period.getSerialIndex();

				double stdDeviationResult = stdDeviation.evaluate(items);
				result.add(period, stdDeviationResult);
//				if (serial >= firstSerial) {
//					// work out the average for the earlier values...
//					int n = 0;
//					double sum = 0.0;
//					long serialLimit = period.getSerialIndex() - periodCount;
//					int offset = 0;
//					boolean finished = false;
//
//					while ((offset < periodCount) && (!finished)) {
//						if ((i - offset) >= 0) {
//							TimeSeriesDataItem item = source.getDataItem(i - offset);
//							RegularTimePeriod p = item.getPeriod();
//							Number v = item.getValue();
//
//							long currentIndex = p.getSerialIndex();
//							if (currentIndex > serialLimit) {
//								if (v != null && v.doubleValue() != 0) {
//									sum = sum + v.doubleValue();
//									n = n + 1;
//									items[itemIndex++] = v.doubleValue();
//									// stdDeviation.increment(v.doubleValue());
//								}
//							} else {
//								finished = true;
//							}
//						}
//						offset = offset + 1;
//					}
//	                    double stdDeviationResult = stdDeviation.getResult();

//	                    if (stdDeviationResult > 0) {
//	                    }
//	                    else {
//	                        result.add(period, null);
//	                    }
//				}

			}
		}

		return result;

	}

	private TimeSeriesCollection createFundShareDataset(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate) {

		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {
			TimeSeries timeSeries = new TimeSeries("Shares");
			Float shares = 0f;
			for (Entry<LocalDate, Float> fundPriceEntry : portfolio.getPriceHistory().getFundShares().get(symbol)
					.entrySet()) {

				LocalDate priceHistoryDate = fundPriceEntry.getKey();
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
				if (fundPriceEntry.getValue().compareTo(shares) == 0) {
					// only add changes to shares
					continue;
				}
				shares = fundPriceEntry.getValue();
				if (shares.compareTo(0f) <= 0) {
					continue;
				}
				timeSeries.add(new Day(priceHistoryDate.getDayOfMonth(), priceHistoryDate.getMonthValue(),
						priceHistoryDate.getYear()), shares);

			}
			dataset.addSeries(timeSeries);

		}
		return dataset;
	}

	private List<TimeSeriesCollection> createFundPriceHistoryDatasets(List<String> fundSynbols, LocalDate startDate,
			LocalDate endDate) {

		List<TimeSeriesCollection> datasets = new ArrayList<TimeSeriesCollection>();

		for (String symbol : fundSynbols) {

			TimeSeriesCollection dataset = new TimeSeriesCollection();
			PortfolioFund fund = portfolio.getFund(symbol);
			TimeSeries timeSeries = new TimeSeries(fund.getShortName());
			for (Entry<LocalDate, BigDecimal> fundPriceEntry : portfolio.getPriceHistory().getFundPrices().get(symbol)
					.entrySet()) {

				LocalDate priceHistoryDate = fundPriceEntry.getKey();
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
			dataset.addSeries(timeSeries);
			datasets.add(dataset);
		}
		return datasets;
	}

	private TimeSeriesCollection createBalanceDataset(LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		TimeSeries timeSeries = new TimeSeries("Portfolio Balance");

		if (startDate == null) {
			startDate = portfolio.getPriceHistory().getOldestDate();
		}
		if (endDate == null) {
			endDate = LocalDate.now();
		}
		LocalDate graphDate = startDate;
		while (!graphDate.isBefore(startDate) && !graphDate.isAfter(endDate)) {
			BigDecimal totalByDate = portfolio.getTotalValueByDate(graphDate);
			if (totalByDate != null && totalByDate.compareTo(BigDecimal.ZERO) > 0) {
				timeSeries.add(new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
						totalByDate);

			}
			graphDate = graphDate.plusDays(1);
		}
		dataset.addSeries(timeSeries);

		return dataset;
	}

	private TimeSeriesCollection createDividendDataset(LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		TimeSeries dividendTimeSeries = new TimeSeries("Dividends");
		BigDecimal cumulativeDividends = BigDecimal.ZERO;
		if (startDate == null) {
			startDate = portfolio.getPriceHistory().getOldestDate();
		}
		if (endDate == null) {
			endDate = LocalDate.now();
		}
		LocalDate graphDate = startDate;
		while (!graphDate.isBefore(startDate) && !graphDate.isAfter(endDate)) {
			final LocalDate date = graphDate;
			BigDecimal dividendsByDate = portfolio.getFundMap().values().stream()
					.map(f -> f.getDistributionsForDate(date)).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (dividendsByDate != null && dividendsByDate.compareTo(BigDecimal.ZERO) > 0) {
				cumulativeDividends = cumulativeDividends.add(dividendsByDate);
				dividendTimeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
						cumulativeDividends);

			}
			graphDate = graphDate.plusDays(1);
		}
		dataset.addSeries(dividendTimeSeries);
		return dataset;
	}

	private TimeSeriesCollection createWithdrawalDataset(LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		TimeSeries withdrawalTimeSeries = new TimeSeries("Withdrawals");
		BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;
		if (startDate == null) {
			startDate = portfolio.getPriceHistory().getOldestDate();
		}
		if (endDate == null) {
			endDate = LocalDate.now();
		}
		LocalDate graphDate = startDate;
		while (!graphDate.isBefore(startDate) && !graphDate.isAfter(endDate)) {
			final LocalDate date = graphDate;
			BigDecimal withdrawalsByDate = portfolio.getFundMap().values().stream()
					.map(f -> f.getWithdrawalTotalForDate(date)).reduce(BigDecimal.ZERO, BigDecimal::subtract);
			if (withdrawalsByDate != null && withdrawalsByDate.compareTo(BigDecimal.ZERO) < 0) {
				cumulativeWithdrawals = cumulativeWithdrawals.add(withdrawalsByDate);
//				withdrawalTimeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
//						cumulativeWithdrawals);
				withdrawalTimeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
						withdrawalsByDate);

			}
			graphDate = graphDate.plusDays(1);
		}
		dataset.addSeries(withdrawalTimeSeries);
		return dataset;
	}

	public void printPerformanceTable(Document document) {

		float[] pointColumnWidths = { 15F, 2F, 4F, 4F, 5F, 5F, 5F, 5F, 5F, 25F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F };
		Table table = new Table(pointColumnWidths);

		table.setFontSize(12);
		table.setTextAlignment(TextAlignment.RIGHT);

		// Print table headings
		table.addHeaderCell(new Cell().add("Fund").setTextAlignment(TextAlignment.LEFT));
		table.addHeaderCell(new Cell().add("%"));
		table.addHeaderCell(new Cell().add("High Share Price/\n1yr High Price").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Low Share Price/\n1yr Low Price").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Begin " + LocalDate.now().getYear() + "\nPrice")
				.setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Day % Change").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Change % YTD/\n1 yr/\n3 yr").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("YTD Div./\nRecent Div.").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Last Year Dividends").setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
		table.addHeaderCell(
				new Cell().add("YTD\nReturns /\nWithdrawals\n/ Exch.\n/ Diff").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Share Price\n" + mostRecentSharePriceDay.format(DATE_FORMATTER) + "\n/ 50d MA")
						.setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Shares /\nYTD Change").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Current Value\nCategory\n Total").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Current %").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Target %\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Target Value\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Deviation\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Surplus/Deficit\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));

		// Cash Funds
		table.addCell(new Cell().add("Cash Funds").setItalic().setTextAlignment(TextAlignment.LEFT));
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell());
		table.startNewRow();
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.CASH)) {
			addFundsToTable(fund, table, FundCategory.CASH);
		}
		addCategoryTotalsToTable(table, FundCategory.CASH);

		// Bond Funds
		table.addCell(new Cell().add("Bond Funds").setTextAlignment(TextAlignment.LEFT).setItalic());
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell());
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.BOND)) {
			addFundsToTable(fund, table, FundCategory.BOND);
		}
		addCategoryTotalsToTable(table, FundCategory.BOND);

		// Stock Funds
		table.addCell(
				new Cell().add("Stock Funds").setTextAlignment(TextAlignment.LEFT).setKeepWithNext(true).setItalic());
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell());
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.STOCK)) {
			addFundsToTable(fund, table, FundCategory.STOCK);
		}
		addCategoryTotalsToTable(table, FundCategory.STOCK);

		// Intl Funds
		table.addCell(new Cell().add("Intl Funds").setTextAlignment(TextAlignment.LEFT).setItalic());
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell());
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.INTL)) {
			addFundsToTable(fund, table, FundCategory.INTL);
		}
		addCategoryTotalsToTable(table, FundCategory.INTL);

		addTotalsToTable(table);

		document.add(table);
		document.add(new AreaBreak());

	}

	public void printPortfolioPerformanceTable(Document document) {

		float[] pointColumnWidths = { 6F, 6F, 6F, 5F, 5F, 5F, 6F, 5F, 5F, 6F, 4F, 4F, 4F };
		Table table = new Table(pointColumnWidths);

		table.setFontSize(12);
		table.setTextAlignment(TextAlignment.RIGHT);

		// Print table headings
		table.addHeaderCell(new Cell().add("Current Value").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Start of Year Value").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Start of Last Year Value").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Day % Change").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("YTD % Change/\n1 yr/\n3 yr").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("YTD Dividends").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Last Year Dividends").setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
		table.addHeaderCell(new Cell().add("YTD\nReturns /\nWithd. /\nExch.").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Last Year Returns /\nWithdrwawals /\nExch.")
				.setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
		table.addHeaderCell(new Cell().add("YTD Fed\nWithholding").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Last Year Fed\nWithholding").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("YTD State\nWithholding").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Last Year State\nWithholding").setTextAlignment(TextAlignment.CENTER));

		PortfolioPerformanceData performanceData = PerformanceService.calculatePortfolioPerformanceData(portfolio);

		table.addCell(
				new Cell().add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioCurrentValue())));
		table.addCell(
				new Cell().add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioFirstOfYearValue()))); //
		table.addCell(new Cell()
				.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioFirstOfLastYearValue()))); //

		table.addCell(new Cell()
				.add(CurrencyHelper.formatPercentageString(performanceData.getPortfolioPreviousDayValueChange()))); // Day
																													// %
																													// change
		// Change
		table.addCell(new Cell().add(new Cell().setMargin(0f)
				.add(CurrencyHelper.formatPercentageString(performanceData.getPortfolioYtdValueChange()))
				.setBackgroundColor(calculatePercentageFontColor(performanceData.getPortfolioYtdReturns()),
						performanceData.getPortfolioYtdReturns().multiply(new BigDecimal(10)).abs().floatValue())
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatPercentageString(performanceData.getPortfolioYearAgoReturns()))
						.setBackgroundColor(calculatePercentageFontColor(performanceData.getPortfolioYearAgoReturns()),
								performanceData.getPortfolioYearAgoReturns().multiply(new BigDecimal(10)).abs()
										.floatValue()))
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatPercentageString(performanceData.getPortfolioThreeYearsAgoReturns()))
						.setBackgroundColor(
								calculatePercentageFontColor(performanceData.getPortfolioThreeYearsAgoReturns()),
								performanceData.getPortfolioThreeYearsAgoReturns().multiply(new BigDecimal(10)).abs()
										.floatValue())))); // YTD % Change

		table.addCell(
				new Cell().add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioYtdDividends())));
		table.addCell(
				new Cell().add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioLastYearDividends())));

		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioYtdReturns()))
						.setFontColor(calculateCurrencyFontColor(performanceData.getPortfolioYtdReturns())))
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioYtdWithdrawals()))));

		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioLastYearReturns()))
						.setFontColor(calculateCurrencyFontColor(performanceData.getPortfolioLastYearReturns())))
				.add(new Cell().setMargin(0f).add(
						CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioLastYearWithdrawals()))));

		table.addCell(new Cell()
				.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioYtdFederalWithholding()))
				.setFontSize(12f));
		table.addCell(new Cell()
				.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioLastYearFederalWithholding()))
				.setFontSize(12f));
		table.addCell(
				new Cell().add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioYtdStateWithholding()))
						.setFontSize(12f));
		table.addCell(new Cell()
				.add(CurrencyHelper.formatAsCurrencyString(performanceData.getPortfolioLastYearStateWithholding()))
				.setFontSize(12f));

		document.add(table);
		document.add(new AreaBreak());

	}

	public void printWithdrawalSpreadsheet(String title, ManagedPortfolio portfolio, BigDecimal netWithdrawalAmount,
			Map<String, BigDecimal> withdrawals, Document document) {

		document.add(new Paragraph(title));

		// Creating a table object
		float[] pointColumnWidths = { 8F, 5F, 5F, 5F, 5F, 5F, 10F, 5F, 5F, 5F, 5F, 5F, 5F };
		Table table = new Table(pointColumnWidths);

		table.setFontSize(14);
		table.setTextAlignment(TextAlignment.RIGHT);

		// Print table headings
		table.addHeaderCell(new Cell().add("Fund"));
		table.addHeaderCell(new Cell().add("Current Value\nCategory\n Total").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Current %\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Target %\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Target Value\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Deviation\nCategory\nTotal\nMinimum").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Surplus/\nDeficit").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Wthdr.").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Post Wthdr. Value").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Post Wthdr.\n%").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Post Wthdr.\nTarget Value").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Post Wthdr.\nDev.").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Post Wthdr.\nSurplus/\nDeficit").setTextAlignment(TextAlignment.CENTER));

		table.addCell(new Cell().add("Cash Funds"));
		table.startNewRow();
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.CASH)) {
			addFundsToWithdrawalTable(fund, table, FundCategory.CASH, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(table, FundCategory.CASH, withdrawals);

		table.addCell(new Cell().add("Bond Funds").setBold());
		table.startNewRow();
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.BOND)) {
			addFundsToWithdrawalTable(fund, table, FundCategory.BOND, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(table, FundCategory.BOND, withdrawals);

		table.startNewRow();
		table.addCell(new Cell().add("Stock Funds").setKeepWithNext(true).setBold());
		table.startNewRow();
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.STOCK)) {
			addFundsToWithdrawalTable(fund, table, FundCategory.STOCK, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(table, FundCategory.STOCK, withdrawals);

		table.startNewRow();
		table.addCell(new Cell().add("Intl Funds").setBold());
		table.startNewRow();
		for (PortfolioFund fund : portfolio.getFundsByCategory(FundCategory.INTL)) {
			addFundsToWithdrawalTable(fund, table, FundCategory.INTL, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(table, FundCategory.INTL, withdrawals);

		addTotalsToWithdrawalTable(table, netWithdrawalAmount, withdrawals);

		document.add(table);
		document.add(new AreaBreak());

	}

	private void addFundsToWithdrawalTable(PortfolioFund fund, Table table, FundCategory category,
			BigDecimal netWithdrawalAmount, Map<String, BigDecimal> withdrawals) {

		BigDecimal postWithdrawalPortfolioValue = portfolio.getTotalValue().subtract(netWithdrawalAmount);
		BigDecimal currentPortfolioValue = portfolio.getTotalValue();

		BigDecimal currentFundValue = fund.getValue();

		BigDecimal fundTotalTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);
		BigDecimal targetValue = currentPortfolioValue.multiply(fundTotalTargetPercentage);
		BigDecimal surpusDeficit = currentFundValue.subtract(targetValue);

		BigDecimal fundCategoryPercentageofTotal = fund.getPercentageByCategory(category);
		BigDecimal targetCategoryPercentage = fundCategoryPercentageofTotal.multiply(fundTotalTargetPercentage)
				.setScale(4, RoundingMode.UP);
		BigDecimal targetValueByCategory = currentPortfolioValue.multiply(targetCategoryPercentage);

		BigDecimal currentValueByCategory = fund.getValue().multiply(fundCategoryPercentageofTotal);

		BigDecimal currentPercentageByCategory = currentValueByCategory.divide(currentPortfolioValue, 6,
				RoundingMode.HALF_DOWN);
		BigDecimal currentPercentage = currentFundValue.divide(currentPortfolioValue, 6, RoundingMode.HALF_DOWN);
		BigDecimal deviation = portfolio.getFundDeviation(fund);

		BigDecimal surplusDeficitByCategory = currentValueByCategory.subtract(targetValueByCategory);
		BigDecimal deviationByCategory = currentPercentageByCategory.subtract(targetCategoryPercentage);

		BigDecimal withdrawalAmount = withdrawals.get(fund.getSymbol());
		if (withdrawalAmount == null) {
			withdrawalAmount = BigDecimal.ZERO;
		}
		BigDecimal postWithdrawalTargetValue = postWithdrawalPortfolioValue.multiply(fundTotalTargetPercentage);
		BigDecimal postWithdrawalTargetCategoryValue = postWithdrawalPortfolioValue.multiply(targetCategoryPercentage);

		BigDecimal postWithdrawalFundValue = fund.getValue().subtract(withdrawalAmount);
		BigDecimal withdrawalPerCategoryAmount = withdrawalAmount.multiply(fundCategoryPercentageofTotal);
		BigDecimal postWithdrawalCategoryValue = postWithdrawalFundValue.multiply(fundCategoryPercentageofTotal);
		BigDecimal postWithdrawalCategoryPercentage = postWithdrawalFundValue
				.divide(postWithdrawalPortfolioValue, 6, RoundingMode.HALF_DOWN)
				.multiply(fundCategoryPercentageofTotal);
		BigDecimal postWithdrawalTotalPercentage = postWithdrawalFundValue.divide(postWithdrawalPortfolioValue, 6,
				RoundingMode.HALF_DOWN);

		BigDecimal postWithdrawalSurplusDeficit = postWithdrawalCategoryValue
				.subtract(postWithdrawalTargetCategoryValue);
		BigDecimal postWithdrawalDeviation = postWithdrawalCategoryPercentage.subtract(targetCategoryPercentage);
		BigDecimal postWithdrawalTotalDeviation = postWithdrawalTotalPercentage.subtract(fundTotalTargetPercentage);
		BigDecimal postWithdrawalTotalSurplusDeficit = postWithdrawalFundValue.subtract(postWithdrawalTargetValue);

		BigDecimal minimumAdjustedTargetValue = null;
		BigDecimal adjustedMinimumTargetPerentage = null;
		BigDecimal minimumAdjustedSurplusDeficit = null;
		BigDecimal minimumAdjustedDeviation = null;
		BigDecimal postWithdrawalMinimumAdjustedSurplusDeficit = null;
		BigDecimal postWithdrawalMinimumAdjustedDeviation = null;
		if (fund.getMinimumAmount() != null && fund.getMinimumAmount().compareTo(BigDecimal.ZERO) > 0) {
			minimumAdjustedTargetValue = fund.getMinimumAmount();
			adjustedMinimumTargetPerentage = minimumAdjustedTargetValue.divide(postWithdrawalPortfolioValue, 6,
					RoundingMode.HALF_DOWN);
			minimumAdjustedSurplusDeficit = currentFundValue.subtract(minimumAdjustedTargetValue);
			minimumAdjustedDeviation = minimumAdjustedSurplusDeficit.divide(postWithdrawalPortfolioValue, 6,
					RoundingMode.HALF_DOWN);
			postWithdrawalMinimumAdjustedSurplusDeficit = postWithdrawalFundValue.subtract(fund.getMinimumAmount());
			postWithdrawalMinimumAdjustedDeviation = postWithdrawalMinimumAdjustedSurplusDeficit
					.divide(postWithdrawalPortfolioValue, 6, RoundingMode.HALF_DOWN);
		}

		// FUnd Name
		table.addCell(new Cell().add("  " + fund.getShortName()));

		// Current Value
		table.addCell(createCurrentValueCell(fund.isFixedExpensesAccount(), currentValueByCategory, deviationByCategory,
				currentFundValue, deviation, minimumAdjustedTargetValue, minimumAdjustedDeviation));

		// Current Percentage
		table.addCell(createCurrentPercentageCell(fund.isFixedExpensesAccount(), currentPercentageByCategory,
				targetCategoryPercentage, fundTotalTargetPercentage, adjustedMinimumTargetPerentage,
				currentPercentage));

		// Target Percentage by Category
		table.addCell(createTargetPercentageCell(fund.isFixedExpensesAccount(), targetCategoryPercentage,
				fundTotalTargetPercentage, adjustedMinimumTargetPerentage));

		// Target Value by Category / Minimum Target Value by Category
		table.addCell(createTargetValueCell(fund.isFixedExpensesAccount(), targetValueByCategory, targetValue,
				minimumAdjustedTargetValue));

		// Deviation / Adjusted Deviation for Minimum and Category
		table.addCell(createDeviationCell(fund.isFixedExpensesAccount(), deviationByCategory, targetValueByCategory,
				targetValue, deviation, minimumAdjustedDeviation));

		// Surplus / Deficit
		table.addCell(
				createSurplusDeficitCell(fund.isFixedExpensesAccount(), surplusDeficitByCategory, deviationByCategory,
						surpusDeficit, deviation, minimumAdjustedSurplusDeficit, minimumAdjustedDeviation));

		// Withdrawal amount
		if (withdrawalPerCategoryAmount.compareTo(withdrawalAmount) == 0) {
			table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(withdrawalPerCategoryAmount.abs()))
					.setFontColor(calculateWithdrawalFontColor(withdrawalPerCategoryAmount)));
		} else {
			table.addCell(new Cell()
					.add(CurrencyHelper.formatAsCurrencyString(withdrawalPerCategoryAmount.abs()) + "\n"
							+ CurrencyHelper.formatAsCurrencyString(withdrawalAmount.abs()))
					.setFontColor(calculateWithdrawalFontColor(withdrawalPerCategoryAmount)));
		}

		// Post withdrawal value
//		table.addCell(createTargetValueCell(fund, targetValueByCategory, targetTotalValue, minimumAdjustedTargetValue));
//		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(postWithdrawalCategoryValue)));
		table.addCell(createCurrentValueCell(fund.isFixedExpensesAccount(), postWithdrawalCategoryValue,
				postWithdrawalDeviation, postWithdrawalFundValue, postWithdrawalTotalDeviation,
				minimumAdjustedTargetValue, postWithdrawalMinimumAdjustedDeviation));

		// Post withdrawal %
//		table.addCell(new Cell().add(CurrencyHelper.formatPercentageString(postWithdrawalCategoryPercentage)));
		table.addCell(createCurrentPercentageCell(fund.isFixedExpensesAccount(), postWithdrawalCategoryPercentage,
				targetCategoryPercentage, fundTotalTargetPercentage, adjustedMinimumTargetPerentage,
				postWithdrawalTotalPercentage));

		// Post withdrawal target value
		BigDecimal postWithdrawalTargetValueByCategory = postWithdrawalPortfolioValue
				.multiply(targetCategoryPercentage);
//		BigDecimal postWithdrawalTargetValue = currentTargetValue.subtract(withdrawalAmount);
		BigDecimal postWithdrawalMinimumTargetValue = null;
		if (fund.getMinimumAmount() != null) {
			if (postWithdrawalTargetValue.compareTo(fund.getMinimumAmount()) > 0) {
				postWithdrawalMinimumTargetValue = postWithdrawalTargetValue;
			} else {
				postWithdrawalMinimumTargetValue = fund.getMinimumAmount();
			}
		}
		table.addCell(createTargetValueCell(fund.isFixedExpensesAccount(), postWithdrawalTargetValueByCategory,
				postWithdrawalTargetValue, postWithdrawalMinimumTargetValue));
//		table.addCell(new Cell().add(
//				CurrencyHelper.formatAsCurrencyString(targetValueByCategory.subtract(withdrawalPerCategoryAmount))));

		// Post Withdrawal Deviation
		table.addCell(createDeviationCell(fund.isFixedExpensesAccount(), postWithdrawalDeviation, targetValueByCategory,
				targetValue, postWithdrawalTotalDeviation, postWithdrawalMinimumAdjustedDeviation));

		// Post Withdrawal Surplus / Deficit
		table.addCell(createSurplusDeficitCell(fund.isFixedExpensesAccount(), postWithdrawalSurplusDeficit,
				postWithdrawalDeviation, postWithdrawalTotalSurplusDeficit, postWithdrawalTotalDeviation,
				postWithdrawalMinimumAdjustedSurplusDeficit, postWithdrawalMinimumAdjustedDeviation));

	}

	private void addCategoryTotalsToWithdrawalTable(Table table, FundCategory category,
			Map<String, BigDecimal> withdrawals) {

		BigDecimal totalCurrentValueByCategory = BigDecimal.ZERO;
		BigDecimal totalCurrentValue = BigDecimal.ZERO;
		BigDecimal totalDividendsByCategory = BigDecimal.ZERO;
		BigDecimal totalYtdValueChangeByCategory = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentage = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentageByCategory = BigDecimal.ZERO;
		BigDecimal totalTargetPercentageByCategory = BigDecimal.ZERO;
		BigDecimal totalTargetValue = BigDecimal.ZERO;
		BigDecimal totalTargetPercentage = BigDecimal.ZERO;
		BigDecimal totalTargetValueByCategory = BigDecimal.ZERO;
		BigDecimal totalAdjustedMinimumTargetValue = BigDecimal.ZERO;
		BigDecimal totalAdjustedMinimumTargetPercentage = BigDecimal.ZERO;

		for (PortfolioFund fund : portfolio.getFundsByCategory(category)) {
			BigDecimal fundCurrentValue = fund.getValue();
			BigDecimal fundCurrentValueByCategory = fund.getValueByCategory(category);
			BigDecimal fundTotalTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);
			BigDecimal fundCategoryTargetPercentage = fund.getPercentageByCategory(category)
					.multiply(fundTotalTargetPercentage);

			// Current Value
			totalCurrentValue = totalCurrentValue.add(fundCurrentValue);
			totalCurrentValueByCategory = totalCurrentValueByCategory.add(fundCurrentValueByCategory);

			// Current Percentage
			totalCurrentPercentage = totalCurrentPercentage
					.add(CurrencyHelper.calculatePercentage(fundCurrentValue, portfolio.getTotalValue()));
			totalCurrentPercentageByCategory = totalCurrentPercentageByCategory
					.add(CurrencyHelper.calculatePercentage(fundCurrentValueByCategory, portfolio.getTotalValue()));

			// Target Percentage
			totalTargetPercentageByCategory = totalTargetPercentageByCategory.add(fundCategoryTargetPercentage);
			totalTargetPercentage = totalTargetPercentage.add(fundTotalTargetPercentage);

			// Target Value
			BigDecimal fundTargetValue = portfolio.getTotalValue().multiply(fundTotalTargetPercentage);
			totalTargetValue = totalTargetValue.add(fundTargetValue);
			totalTargetValueByCategory = totalTargetValueByCategory
					.add(fundCategoryTargetPercentage.multiply(portfolio.getTotalValue()));

			BigDecimal fundDividendByCategory = fund.getDistributionsAfterDate(getFirstOfYearDate())
					.multiply(fund.getPercentageByCategory(category));
			totalDividendsByCategory = totalDividendsByCategory.add(fundDividendByCategory);

			totalYtdValueChangeByCategory = totalYtdValueChangeByCategory
					.add(fund.getValue().subtract(portfolio.getHistoricalValue(fund, getYtdDays())));

			if (fund.getMinimumAmount() != null) {
				totalAdjustedMinimumTargetValue = totalAdjustedMinimumTargetValue.add(fund.getMinimumAmount());
				totalAdjustedMinimumTargetPercentage = totalAdjustedMinimumTargetPercentage
						.add(CurrencyHelper.calculatePercentage(fund.getMinimumAmount(), portfolio.getTotalValue()));
			} else {
				totalAdjustedMinimumTargetValue = totalAdjustedMinimumTargetValue.add(fundTargetValue);
				totalAdjustedMinimumTargetPercentage = totalAdjustedMinimumTargetPercentage
						.add(fundTotalTargetPercentage);

			}

		}

		BigDecimal totalDeviation = totalCurrentPercentage.subtract(totalTargetPercentage);
		BigDecimal totalAdjustedMinimumDeviation = totalCurrentPercentage
				.subtract(totalAdjustedMinimumTargetPercentage);
		BigDecimal totalDeviationByCategory = totalCurrentPercentageByCategory
				.subtract(totalTargetPercentageByCategory);
		BigDecimal totalSurplusDeficit = totalCurrentValue.subtract(totalTargetValue);
		BigDecimal surpusDeficitByCategory = totalCurrentValueByCategory.subtract(totalTargetValueByCategory);
		BigDecimal adjustedMinimumSurplusDeficit = totalCurrentValue.subtract(totalAdjustedMinimumTargetValue);

		BigDecimal totalWithdrawals = BigDecimal.ZERO;
		BigDecimal totalWithdrawalsByCategory = BigDecimal.ZERO;
		for (Entry<String, BigDecimal> fundWithdrawal : withdrawals.entrySet()) {
			String fundSymbol = fundWithdrawal.getKey();
			PortfolioFund fund = portfolio.getFund(fundSymbol);
			totalWithdrawals = totalWithdrawals.add(fundWithdrawal.getValue());
			totalWithdrawalsByCategory = totalWithdrawalsByCategory
					.add(fundWithdrawal.getValue().multiply(fund.getPercentageByCategory(category)));
		}

		BigDecimal postWithdrawalValueByCategory = totalCurrentValueByCategory.subtract(totalWithdrawalsByCategory);
		BigDecimal postWithdrawalValue = totalCurrentValue.subtract(totalWithdrawals);

		table.addCell(new Cell().add("Cat. Total").setItalic());

		// Current Value
		table.addCell(createCurrentValueCell(false, totalCurrentValueByCategory, totalDeviation, totalCurrentValue,
				totalDeviation, totalAdjustedMinimumTargetValue, totalDeviation));

		// Current Percentage
		table.addCell(
				createCurrentPercentageCell(false, totalCurrentPercentageByCategory, totalTargetPercentageByCategory,
						totalCurrentValue, totalAdjustedMinimumTargetPercentage, totalCurrentPercentage));

		// Target Percentage
		table.addCell(createTargetPercentageCell(false, totalTargetPercentageByCategory, totalTargetPercentage,
				totalAdjustedMinimumTargetPercentage));

		// Target Value
		table.addCell(createTargetValueCell(false, totalTargetValueByCategory, totalTargetValue,
				totalAdjustedMinimumTargetValue));

		// Deviation
		table.addCell(createDeviationCell(false, totalDeviationByCategory, totalTargetValueByCategory, totalTargetValue,
				totalDeviation, totalAdjustedMinimumDeviation));

		// Surplus / Deficit
		table.addCell(createSurplusDeficitCell(false, surpusDeficitByCategory, totalDeviationByCategory,
				totalSurplusDeficit, totalDeviation, adjustedMinimumSurplusDeficit, totalAdjustedMinimumDeviation));

		// Withdrawal
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalWithdrawalsByCategory.abs()))
				.setFontColor(calculateWithdrawalFontColor(totalWithdrawalsByCategory)));

		// Post Withdrawal Value
		table.addCell(createCurrentValueCell(false, postWithdrawalValueByCategory, totalDeviation, postWithdrawalValue,
				totalDeviation, totalAdjustedMinimumTargetValue, totalDeviation));

		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
	}

	private void addTotalsToTable(Table table) {

		BigDecimal portfolioYtdDividends = BigDecimal.ZERO;
		BigDecimal portfolioLastYearDividends = BigDecimal.ZERO;
		BigDecimal portfolioTotalCurrentPercentage = BigDecimal.ZERO;
		BigDecimal portfolioTotalTargetPercentage = BigDecimal.ZERO;
		BigDecimal portfolioYtdValueChange = BigDecimal.ZERO;
		BigDecimal portfolioYtdWithdrawals = BigDecimal.ZERO;
		BigDecimal portfolioFirstOfYearValue = BigDecimal.ZERO;
		BigDecimal portfolioPreviousDayValue = BigDecimal.ZERO;
		BigDecimal portfolioYearAgoValue = BigDecimal.ZERO;
		BigDecimal portfolioYearAgoWithdrawals = BigDecimal.ZERO;
		BigDecimal portfolioThreeYearAgoValue = BigDecimal.ZERO;
		BigDecimal portfolioThreeYearAgoWithdrawals = BigDecimal.ZERO;
		BigDecimal portfolioYtdReturns = BigDecimal.ZERO;

		BigDecimal currentPortfolioValue = portfolio.getTotalValue();

		for (PortfolioFund fund : portfolio.getFundMap().values()) {
			MutualFundPerformance fundPerformance = new MutualFundPerformance(portfolio, fund);

			portfolioPreviousDayValue = portfolioPreviousDayValue.add(portfolio.getHistoricalValue(fund, 1));

			portfolioYtdValueChange = portfolioYtdValueChange.add(fundPerformance.getYtdValueChange());
			portfolioYtdWithdrawals = portfolioYtdWithdrawals.add(fund.getWithdrawalsUpToDate(getFirstOfYearDate()));
			portfolioYtdDividends = portfolioYtdDividends.add(fund.getDistributionsAfterDate(getFirstOfYearDate()));
			portfolioLastYearDividends = portfolioLastYearDividends.add(fund.getDistributionsBetweenDates(
					getFirstOfYearDate().minus(1, ChronoUnit.YEARS), getFirstOfYearDate().minus(1, ChronoUnit.DAYS)));

			portfolioFirstOfYearValue = portfolioFirstOfYearValue.add(portfolio.getHistoricalValue(fund, getYtdDays()));
			BigDecimal fundYearAgoValue = portfolio.getHistoricalValue(fund, 365);
			portfolioYearAgoValue = portfolioYearAgoValue.add(fundYearAgoValue);

			BigDecimal fundThreeYearAgoValue = portfolio.getHistoricalValue(fund, 365 * 3);
			portfolioThreeYearAgoValue = portfolioThreeYearAgoValue.add(fundThreeYearAgoValue);

			portfolioYearAgoWithdrawals = portfolioYearAgoWithdrawals
					.add(fund.getWithdrawalsUpToDate(LocalDate.now().minusYears(1)));
			portfolioThreeYearAgoWithdrawals = portfolioThreeYearAgoWithdrawals
					.add(fund.getWithdrawalsUpToDate(LocalDate.now().minusYears(3)));

			portfolioTotalCurrentPercentage = portfolioTotalCurrentPercentage
					.add(CurrencyHelper.calculatePercentage(fund.getValue(), currentPortfolioValue));
			portfolioTotalTargetPercentage = portfolioTotalTargetPercentage
					.add(fund.getPercentageByCategory(FundCategory.TOTAL));

		}

		BigDecimal portfolioPreviousValueChange = CurrencyHelper
				.calculatePercentage(currentPortfolioValue.subtract(portfolioPreviousDayValue), currentPortfolioValue);
		portfolioFirstOfYearValue = portfolioFirstOfYearValue.subtract(portfolioYtdWithdrawals);
		portfolioYtdReturns = currentPortfolioValue.subtract(portfolioFirstOfYearValue).divide(currentPortfolioValue, 4,
				RoundingMode.HALF_DOWN);

		portfolioYearAgoValue = portfolioYearAgoValue.subtract(portfolioYearAgoWithdrawals);
		BigDecimal yearAgoReturns = currentPortfolioValue.subtract(portfolioYearAgoValue).divide(currentPortfolioValue,
				4, RoundingMode.HALF_DOWN);

		portfolioThreeYearAgoValue = portfolioThreeYearAgoValue.subtract(portfolioThreeYearAgoWithdrawals);

		BigDecimal portfolioThreeYearReturns = currentPortfolioValue.subtract(portfolioThreeYearAgoValue)
				.divide(currentPortfolioValue, 4, RoundingMode.HALF_DOWN);

		BigDecimal threeYearAnnualizedRate = new BigDecimal(
				calculatePortfolioAnnualizedReturn(portfolioThreeYearReturns, 3));

		table.addCell(new Cell().add("Grand Total").setBold());
		table.addCell(new Cell().add(" ")); // %
		table.addCell(new Cell().add("")); // High share price
		table.addCell(new Cell().add("")); // Low share price n/a
		table.addCell(new Cell().add("")); // Begin year price n/a
		table.addCell(new Cell().add(CurrencyHelper.formatPercentageString(portfolioPreviousValueChange))); // Day %

		// % Change
		table.addCell(new Cell().add(new Cell().setMargin(0f)
				.add(CurrencyHelper.formatPercentageString(portfolioYtdReturns))
				.setBackgroundColor(calculatePercentageFontColor(portfolioYtdReturns),
						portfolioYtdReturns.multiply(new BigDecimal(10)).abs().floatValue())
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatPercentageString(yearAgoReturns))
						.setBackgroundColor(calculatePercentageFontColor(yearAgoReturns),
								yearAgoReturns.multiply(new BigDecimal(10)).abs().floatValue()))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatPercentageString(threeYearAnnualizedRate))
						.setBackgroundColor(calculatePercentageFontColor(threeYearAnnualizedRate),
								yearAgoReturns.multiply(new BigDecimal(10)).abs().floatValue()))));
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(portfolioYtdDividends)).setFontSize(12f));
		table.addCell(
				new Cell().add(CurrencyHelper.formatAsCurrencyString(portfolioLastYearDividends)).setFontSize(12f));

		// YTD Returns / withdrawals
		portfolioYtdWithdrawals = portfolioYtdWithdrawals.negate();
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(portfolioYtdValueChange))
						.setFontColor(calculateCurrencyFontColor(portfolioYtdValueChange)))
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(portfolioYtdWithdrawals))
						.setFontColor(calculateCurrencyFontColor(portfolioYtdWithdrawals))));

		// Share price
		table.addCell(new Cell().add(""));

		// Shares
		table.addCell(new Cell().add(""));

		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(currentPortfolioValue)));
		table.addCell(new Cell().add(CurrencyHelper.formatPercentageString(portfolioTotalCurrentPercentage)));
		table.addCell(new Cell().add(CurrencyHelper.formatPercentageString(portfolioTotalTargetPercentage)));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
	}

	private void addTotalsToWithdrawalTable(Table table, BigDecimal netWithdrawalAmount,
			Map<String, BigDecimal> withdrawals) {

		BigDecimal totalCurrentValue = BigDecimal.ZERO;
		BigDecimal total = portfolio.getTotalValue();
		BigDecimal totalYtdValueChange = BigDecimal.ZERO;
		BigDecimal totalWithdrawals = BigDecimal.ZERO;

		for (PortfolioFund fund : portfolio.getFundMap().values()) {
			totalCurrentValue = totalCurrentValue.add(fund.getValue());
			totalYtdValueChange = totalYtdValueChange
					.add(fund.getValue().subtract(portfolio.getHistoricalValue(fund, getYtdDays())));

		}

		for (BigDecimal withdrawal : withdrawals.values()) {
			totalWithdrawals = totalWithdrawals.add(withdrawal);
		}

		BigDecimal federalIncomeTax = totalWithdrawals.multiply(new BigDecimal(.12));
		BigDecimal stateIncomeTax = totalWithdrawals.multiply(new BigDecimal(.03));

		table.addCell(new Cell().add("Grand Total").setBold());

		// Current Value
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalCurrentValue)));

		// Current %
		table.addCell(new Cell().add("n/a"));

		// Target %
		table.addCell(new Cell().add("n/a"));

		// Target Value
		table.addCell(new Cell().add("n/a"));

		// Deviation
		table.addCell(new Cell().add("n/a"));

		// Surplus Deficit
		table.addCell(new Cell().add("n/a"));

		// Withdrawals
		table.addCell(new Cell().add(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalWithdrawals)))
				.add(new Cell().add("federal " + CurrencyHelper.formatAsCurrencyString(federalIncomeTax)))
				.add(new Cell().add("state " + CurrencyHelper.formatAsCurrencyString(stateIncomeTax)))
				.add(new Cell().add("net " + CurrencyHelper.formatAsCurrencyString(netWithdrawalAmount))));

		// Post Withdrawal Value
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(total.subtract(totalWithdrawals))));

		// Post Withdrawal Target %
		table.addCell(new Cell().add("n/a"));

		// Post Withdrawal Target Value
		table.addCell(new Cell().add("n/a"));

		// Post Withdrawal Deviation
		table.addCell(new Cell().add("n/a"));

		// Post Withdrawal Surplus Deficit
		table.addCell(new Cell().add("n/a"));
	}

	private void addFundsToTable(PortfolioFund fund, Table table, FundCategory category) {

		List<String> fundSymbols = new ArrayList<>();
		fundSymbols.add(fund.getSymbol());
		TimeSeriesCollection fundTimeSeries = createFundPriceHistoryDataset(fundSymbols, null, null, false);
		TimeSeries fundPriceMovingAverageTimeSeries = fundTimeSeries.getSeries(1);

		PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
		MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);

		BigDecimal currentPrice = fund.getCurrentPrice();
		BigDecimal dayPriceChange = performance.getDayPriceChange();

		BigDecimal movingAveragePrice = new BigDecimal(fundPriceMovingAverageTimeSeries
				.getDataItem(fundPriceMovingAverageTimeSeries.getItemCount() - 1).getValue().doubleValue());
		BigDecimal movingAverageDifference = currentPrice.subtract(movingAveragePrice);

		LocalDate fiftyTwoWeeksAgo = LocalDate.now().minus(1, ChronoUnit.YEARS);
		Float fiftyTwoWeekPerformanceRate = performance.getPerformanceRateByDate(fiftyTwoWeeksAgo);

		Float annualizedThreeYearPriceChange = (float) portfolio.calculateAnnualizedReturn(fund, 3);
		Float annualizedFiveYearPriceChange = (float) portfolio.calculateAnnualizedReturn(fund, 5);

		Float ytdPerformanceRate = performance.getPerformanceRateByDate(getFirstOfYearDate());
		// Does this include exchanges and withdrawals?
		BigDecimal ytdValueChange = performance.getYtdValueChange();
		BigDecimal ytdWithdrawals = fund.getWithdrawalsUpToDate(getFirstOfYearDate());
		Color ytdWithdrawalsFontColor = ytdWithdrawals.compareTo(BigDecimal.ZERO) > 0 ? Color.RED : Color.BLACK;

		BigDecimal ytdExchanges = BigDecimal.ZERO.subtract(fund.getExchangeTotalFromDate(getFirstOfYearDate()));
		Color ytdExchangesFontColor = ytdExchanges.compareTo(BigDecimal.ZERO) < 0 ? Color.RED : Color.GREEN;

		BigDecimal ytdDifference = ytdValueChange.subtract(ytdWithdrawals).add(ytdExchanges);
		Color ytdDifferenceFontColor = ytdDifference.compareTo(BigDecimal.ZERO) < 0 ? Color.RED : Color.GREEN;

		BigDecimal fundTotalPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);

		// Current Value
		BigDecimal currentValue = fund.getValue();
		BigDecimal currentValueByCategory = fund.getValueByCategory(category);

		// Target Value
		BigDecimal targetValue = portfolio.getTotalValue().multiply(fundTotalPercentage);
		BigDecimal fundCategoryPercentage = fund.getPercentageByCategory(category);
		BigDecimal targetCategoryPercentage = fundCategoryPercentage.multiply(fundTotalPercentage).setScale(4,
				RoundingMode.UP);
		BigDecimal targetValueByCategory = portfolio.getTotalValue().multiply(targetCategoryPercentage);

		// Adjust minimum values
		BigDecimal adjustedMinimumTargetValue = null;
		BigDecimal adjustedMinimumTargetPerentage = null;
		BigDecimal adjustedMinimumSurplusDeficit = null;
		BigDecimal adjustedMinimumDeviation = null;
		if (fund.getMinimumAmount() != null && fund.getMinimumAmount().compareTo(BigDecimal.ZERO) > 0) {
			adjustedMinimumTargetValue = fund.getMinimumAmount();
			adjustedMinimumTargetPerentage = fund.getMinimumAmount().divide(portfolio.getTotalValue(), 6,
					RoundingMode.HALF_DOWN);
			adjustedMinimumSurplusDeficit = currentValue.subtract(adjustedMinimumTargetValue);
			adjustedMinimumDeviation = adjustedMinimumSurplusDeficit.divide(portfolio.getTotalValue(), 6,
					RoundingMode.HALF_DOWN);
		}

		// Current Shares / YTD Change
		Double currentShares = fund.getShares();
		Float ytdShares = performance.getSharesByDate(fund, getFirstOfYearDate(), false);
		Double ytdSharesChange = currentShares - ytdShares.doubleValue();

		// Current %
		BigDecimal currentPercentageByCategory = currentValue.multiply(fund.getPercentageByCategory(category))
				.divide(portfolio.getTotalValue(), 6, RoundingMode.HALF_DOWN);
		BigDecimal currentPercentage = currentValue.divide(portfolio.getTotalValue(), 6, RoundingMode.HALF_DOWN);

		// Surplus / Deficit
		BigDecimal surpusDeficitByCategory = currentValueByCategory.subtract(targetValueByCategory);
		BigDecimal surpusDeficit = currentValue.subtract(targetValue);
		BigDecimal deviationByCategory = currentPercentageByCategory.subtract(targetCategoryPercentage);
		BigDecimal deviation = currentPercentage.subtract(fundTotalPercentage);

		// YTD Dividends
		BigDecimal currentDividends = fund.getDistributionsAfterDate(LocalDate.now().minusDays(3));
		BigDecimal ytdDividends = fund.getDistributionsAfterDate(getFirstOfYearDate());
		BigDecimal lastYearDividends = fund.getDistributionsBetweenDates(
				getFirstOfYearDate().minus(1, ChronoUnit.YEARS), getFirstOfYearDate().minus(1, ChronoUnit.DAYS));

		// Min/max priceso
		// LocalDate oldestDate = LocalDate.now().minusDays(oldestDay);
		LocalDate earliestDate = priceHistory.getOldestDate();
		Pair<LocalDate, BigDecimal> maxPricePair = priceHistory.getMaxPriceFromDate(fund, earliestDate);
		Pair<LocalDate, BigDecimal> minPricePair = priceHistory.getMinPriceFromDate(fund, earliestDate);
		Pair<LocalDate, BigDecimal> maxPrice1YRPair = priceHistory.getMaxPriceFromDate(fund, fiftyTwoWeeksAgo);
		Pair<LocalDate, BigDecimal> minPrice1YRPair = priceHistory.getMinPriceFromDate(fund, fiftyTwoWeeksAgo);
		Color minPrice1YRFontColor = Color.BLACK;
		Color maxPrice1YRFontColor = Color.BLACK;
		Color maxPriceFontColor = Color.BLACK;
		Color minPriceFontColor = Color.BLACK;
		Color currentPriceFontColor = calculateCurrencyFontColor(
				currentPrice.subtract(performance.getFirstOfYearPrice()));
		if (currentPrice.compareTo(minPrice1YRPair.getRight()) <= 0
				&& currentPrice.compareTo(maxPrice1YRPair.getRight()) != 0) {
			currentPriceFontColor = Color.RED;
			minPrice1YRFontColor = Color.RED;
		}
		if (currentPrice.compareTo(maxPrice1YRPair.getRight()) >= 0
				&& currentPrice.compareTo(minPrice1YRPair.getRight()) != 0) {
			currentPriceFontColor = Color.GREEN;
			maxPrice1YRFontColor = Color.GREEN;
		}
		if (currentPrice.compareTo(maxPricePair.getRight()) >= 0
				&& currentPrice.compareTo(minPrice1YRPair.getRight()) != 0) {
			currentPriceFontColor = Color.GREEN;
			maxPriceFontColor = Color.GREEN;
		}
		if (currentPrice.compareTo(minPricePair.getRight()) <= 0
				&& currentPrice.compareTo(maxPrice1YRPair.getRight()) != 0) {
			currentPriceFontColor = Color.RED;
			minPriceFontColor = Color.RED;
		}

		// Fund name
		table.addCell(new Cell().add("  " + fund.getShortName()).setTextAlignment(TextAlignment.LEFT));

		// Category Percentage
		if (fundCategoryPercentage.compareTo(new BigDecimal(1)) < 0) {
			table.addCell(new Cell()
					.add(String.format("%(3.0f", (fundCategoryPercentage.multiply(new BigDecimal(100)))) + "%"));
		} else {
			table.addCell(new Cell());
		}

		// Max Price
		if (fund.isMMFund()) {
			table.addCell(new Cell().setMargin(0f).add("n/a"));
		} else {
			table.addCell(new Cell().setMargin(0f)
					.add(new Cell().setMargin(0f)
							.add(CurrencyHelper.formatAsCurrencyString(maxPricePair.getRight()) + "\n"
									+ maxPricePair.getLeft().format(DATE_FORMATTER))
							.setFontColor(maxPriceFontColor))
					.add(new Cell().setMargin(0f)
							.add(CurrencyHelper.formatAsCurrencyString(maxPrice1YRPair.getRight()) + "\n"
									+ maxPrice1YRPair.getLeft().format(DATE_FORMATTER))
							.setFontColor(maxPrice1YRFontColor).setFontSize(12)));
		}

		// Min Price
		if (fund.isMMFund()) {
			table.addCell(new Cell().setMargin(0f).add("n/a"));
		} else {
			table.addCell(new Cell().setMargin(0f)
					.add(new Cell().setMargin(0f)
							.add(CurrencyHelper.formatAsCurrencyString(minPricePair.getRight()) + "\n"
									+ minPricePair.getLeft().format(DATE_FORMATTER))
							.setFontColor(minPriceFontColor))
					.add(new Cell().setMargin(0f)
							.add(CurrencyHelper.formatAsCurrencyString(minPrice1YRPair.getRight()) + "\n"
									+ minPrice1YRPair.getLeft().format(DATE_FORMATTER))
							.setFontColor(minPrice1YRFontColor).setFontSize(12)));
		}

		// Begin Year Price
		table.addCell(new Cell().add(
				CurrencyHelper.formatAsCurrencyString(performance.getClosestHistoricalPrice(getFirstOfYearDate(), 5))));

		// Day Price Change
		if (fund.isMMFund()) {
			table.addCell(new Cell().setMargin(0f).add("n/a"));
		} else {
			table.addCell(new Cell().add(CurrencyHelper.formatPercentageString(dayPriceChange)).setBackgroundColor(
					calculatePercentageFontColor(dayPriceChange),
					dayPriceChange.multiply(new BigDecimal(100)).abs().floatValue()));
		}

		// YTD / 1 year / 3 yr annualized Price Change
		if (fund.isMMFund()) {
			table.addCell(new Cell().setMargin(0f).add("n/a"));
		} else {
			table.addCell(new Cell().setMargin(0f)
					.add(new Cell().add(CurrencyHelper.formatPercentageString(ytdPerformanceRate)).setBackgroundColor(
							calculatePercentageFontColor(new BigDecimal(ytdPerformanceRate)),
							new BigDecimal(ytdPerformanceRate * 10f).abs().floatValue()))
					.add(new Cell().add(CurrencyHelper.formatPercentageString(fiftyTwoWeekPerformanceRate))
							.setBackgroundColor(
									calculatePercentageFontColor(new BigDecimal(fiftyTwoWeekPerformanceRate)),
									new BigDecimal(fiftyTwoWeekPerformanceRate * 10f).abs().floatValue()))
					.add(new Cell().add(CurrencyHelper.formatPercentageString(annualizedThreeYearPriceChange))
							.setBackgroundColor(
									calculatePercentageFontColor(new BigDecimal(annualizedThreeYearPriceChange)),
									new BigDecimal(annualizedThreeYearPriceChange * 10f).abs().floatValue()))
					.add(new Cell().add(CurrencyHelper.formatPercentageString(annualizedFiveYearPriceChange))
							.setBackgroundColor(
									calculatePercentageFontColor(new BigDecimal(annualizedFiveYearPriceChange)),
									new BigDecimal(annualizedFiveYearPriceChange * 10f).abs().floatValue())));
		}

		// YTD Dividends
		table.addCell(
				new Cell().setFontSize(12f).add(new Cell().add(CurrencyHelper.formatAsCurrencyString(ytdDividends)))
						.add((currentDividends.compareTo(BigDecimal.ZERO) == 0) ? new Cell()
								: new Cell().add(CurrencyHelper.formatAsCurrencyString(currentDividends))
										.setFontColor(calculatePercentageFontColor(currentDividends))));

		// Last Year Dividends
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(lastYearDividends)).setFontSize(12f));

		// YTD Returns (withdrawals and exchanges)
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdValueChange))
						.setFontColor(calculateCurrencyFontColor(ytdValueChange)))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdWithdrawals))
						.setFontColor(ytdWithdrawalsFontColor))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdExchanges))
						.setFontColor(ytdExchangesFontColor))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdDifference))
						.setFontColor(ytdDifferenceFontColor)))
				.setFontSize(12);

		// Current share price
		Color maColor = Color.GREEN;
		if (currentPrice.compareTo(movingAveragePrice) < 0) {
			maColor = Color.RED;
		}
		table.addCell(new Cell()
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(currentPrice)).setBackgroundColor(
						currentPriceFontColor, calculateCurrenPriceOpacity(performance, getFirstOfYearDate())))
				.add(new Cell().add("ma:" + CurrencyHelper.formatAsCurrencyString(movingAveragePrice)))
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(movingAverageDifference)).setBackgroundColor(
						maColor, calculateMovingAveragePriceOpacity(currentPrice, movingAveragePrice))));

		// YTD Change in number of Shares
		table.addCell(new Cell().add(new Cell().add(String.format("%(6.2f", currentShares)))
				.add(new Cell().add(String.format("%(6.2f", ytdSharesChange))
						.setFontColor(ytdSharesChange < 0 ? Color.RED : Color.GREEN)));

		// Current Value
		table.addCell(createCurrentValueCell(fund.isFixedExpensesAccount(), currentValueByCategory, deviationByCategory,
				currentValue, deviation, adjustedMinimumTargetValue, adjustedMinimumDeviation));

		// Current Percentage
		table.addCell(createCurrentPercentageCell(fund.isFixedExpensesAccount(), currentPercentageByCategory,
				targetCategoryPercentage, fundTotalPercentage, adjustedMinimumTargetPerentage, currentPercentage));

		// Target Category Percentage
		table.addCell(createTargetPercentageCell(fund.isFixedExpensesAccount(), targetCategoryPercentage,
				fundTotalPercentage, adjustedMinimumTargetPerentage));

		// Target Value
		table.addCell(createTargetValueCell(fund.isFixedExpensesAccount(), targetValueByCategory, targetValue,
				adjustedMinimumTargetValue));

		// Deviation
		table.addCell(createDeviationCell(fund.isFixedExpensesAccount(), deviationByCategory, targetValueByCategory,
				targetValue, deviation, adjustedMinimumDeviation));

		// Surplus / Deficit
		table.addCell(
				createSurplusDeficitCell(fund.isFixedExpensesAccount(), surpusDeficitByCategory, deviationByCategory,
						surpusDeficit, deviation, adjustedMinimumSurplusDeficit, adjustedMinimumDeviation));

	}

	private Cell createSurplusDeficitCell(Boolean isFixedExpensesAccount, BigDecimal surpusDeficitByCategory,
			BigDecimal deviationByCategory, BigDecimal surpusDeficit, BigDecimal deviation,
			BigDecimal adjustedMinimumSurplusDeficit, BigDecimal adjustedMinimumDeviation) {
		Cell cell = new Cell()
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(surpusDeficitByCategory)).setBackgroundColor(
						calculateCurrencyFontColor(deviationByCategory),
						deviationByCategory.abs().multiply(new BigDecimal(100)).multiply(new BigDecimal(2))
								.floatValue()))
				.add(surpusDeficit.subtract(surpusDeficitByCategory).abs().compareTo(new BigDecimal(.01)) <= 0
						? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(surpusDeficit)).setBackgroundColor(
								calculateCurrencyFontColor(deviation),
								deviation.abs().multiply(new BigDecimal(100)).multiply(new BigDecimal(2)).floatValue()))
				.add(adjustedMinimumSurplusDeficit == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(adjustedMinimumSurplusDeficit))
								.setBackgroundColor(calculateCurrencyFontColor(adjustedMinimumDeviation),
										adjustedMinimumDeviation.abs().multiply(new BigDecimal(100))
												.multiply(new BigDecimal(2)).floatValue()));
		return cell;
	}

	private Cell createDeviationCell(Boolean isFixedExpensesAccount, BigDecimal deviationByCategory,
			BigDecimal targetValueByCategory, BigDecimal targetValue, BigDecimal deviation,
			BigDecimal adjustedMinimumDeviation) {
		Cell cell = new Cell()
				.add(new Cell().add(CurrencyHelper.formatPercentageString3(deviationByCategory)).setBackgroundColor(
						calculateCurrencyFontColor(deviationByCategory),
						calculateCurrencyFontOpacity(deviationByCategory)))
				.add(targetValue.compareTo(targetValueByCategory) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString3(deviation)).setBackgroundColor(
								calculateCurrencyFontColor(deviation), calculateCurrencyFontOpacity(deviation)))
				.add(adjustedMinimumDeviation == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString3(adjustedMinimumDeviation))
								.setBackgroundColor(calculateCurrencyFontColor(adjustedMinimumDeviation),
										calculateCurrencyFontOpacity(adjustedMinimumDeviation)));
		return cell;
	}

	private Cell createTargetValueCell(Boolean isFixedExpensesAccount, BigDecimal targetValueByCategory,
			BigDecimal targetValue, BigDecimal adjustedMinimumTargetValue) {
		Cell cell = new Cell()
//				.add(isFixedExpensesAccount ? new Cell().add("n/a")
//						: new Cell().add(CurrencyHelper.formatAsCurrencyString(targetValueByCategory)))
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(targetValueByCategory)))
				.add(targetValue.compareTo(targetValueByCategory) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(targetValue)))
				.add(adjustedMinimumTargetValue == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(adjustedMinimumTargetValue)));
		return cell;
	}

	private Cell createTargetPercentageCell(Boolean isFixedExpensesAccount, BigDecimal targetCategoryPercentage,
			BigDecimal fundTotalPercentage, BigDecimal adjustedMinimumTargetPerentage) {
		Cell cell = new Cell().add(new Cell().add(CurrencyHelper.formatPercentageString(targetCategoryPercentage)))
				.add(fundTotalPercentage.compareTo(targetCategoryPercentage) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(fundTotalPercentage)))
				.add(adjustedMinimumTargetPerentage == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(adjustedMinimumTargetPerentage)));
		return cell;
	}

	private Cell createCurrentPercentageCell(Boolean isFixedExpensesAccount, BigDecimal currentPercentageByCategory,
			BigDecimal targetCategoryPercentage, BigDecimal fundTotalPercentage,
			BigDecimal adjustedMinimumTargetPerentage, BigDecimal currentPercentage) {

		Cell cell = new Cell().add(new Cell().add(CurrencyHelper.formatPercentageString(currentPercentageByCategory)))
				.add(fundTotalPercentage.compareTo(targetCategoryPercentage) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(currentPercentage)))
				.add(adjustedMinimumTargetPerentage == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(currentPercentage)));
		return cell;
	}

	private Cell createCurrentValueCell(Boolean isFixedExpensesAccount, BigDecimal currentValueByCategory,
			BigDecimal deviationByCategory, BigDecimal currentValue, BigDecimal deviation,
			Object adjustedMinimumTargetValue, BigDecimal adjustedMinimumDeviation) {
		Cell cell = new Cell()
				.add(new Cell().add(CurrencyHelper.formatAsCurrencyString(currentValueByCategory)).setBackgroundColor(
						calculateCurrencyFontColor(deviationByCategory),
						deviationByCategory.abs().multiply(new BigDecimal(100)).multiply(new BigDecimal(2))
								.floatValue()))
				.add(currentValue.subtract(currentValueByCategory).compareTo(new BigDecimal(.01)) <= 0
						? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(currentValue)).setBackgroundColor(
								calculateCurrencyFontColor(deviation),
								deviation.abs().multiply(new BigDecimal(100)).multiply(new BigDecimal(2)).floatValue()))
				.add(adjustedMinimumTargetValue == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(currentValue)).setBackgroundColor(
								calculateCurrencyFontColor(adjustedMinimumDeviation), adjustedMinimumDeviation.abs()
										.multiply(new BigDecimal(100)).multiply(new BigDecimal(2)).floatValue()));
		return cell;
	}

	private static LocalDate getFirstOfYearDate() {
		return LocalDate.of(LocalDate.now().getYear(), 1, 1);
	}

	public static long getYtdDays() {
		long ytdDays = getFirstOfYearDate().until(LocalDate.now(), ChronoUnit.DAYS);
		return ytdDays;
	}

	private Color calculatePercentageFontColor(BigDecimal value) {
		Color fontColor = Color.BLACK;
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			fontColor = Color.RED;
		}
		if (value.compareTo(BigDecimal.ZERO) > 0) {
			fontColor = Color.GREEN;
		}
		return fontColor;
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

	private Color calculateWithdrawalFontColor(BigDecimal value) {
		Color fontColor = Color.BLACK;
		if (value != null) {
			if (value.compareTo(BigDecimal.ZERO) > 0) {
				fontColor = Color.RED;
			} else if (value.compareTo(BigDecimal.ZERO) < 0) {
				fontColor = Color.GREEN;
			}
		}
		return fontColor;
	}

	private Float calculateCurrencyFontOpacity(BigDecimal value) {
		return value.abs().multiply(new BigDecimal(100)).multiply(new BigDecimal(2)).floatValue();
	}

	public void loadPortfolioDownloadFile(LocalDate date, String filename) {

		try {
			portfolio.getPriceHistory().loadPortfolioDownloadFile(portfolio, date, filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void addCategoryTotalsToTable(Table table, FundCategory category) {

		BigDecimal totalCurrentValueByCategory = BigDecimal.ZERO;
		BigDecimal totalCurrentValue = BigDecimal.ZERO;
		BigDecimal totalDividendsByCategory = BigDecimal.ZERO;
		BigDecimal totalLastYearDividends = BigDecimal.ZERO;

		BigDecimal totalYtdValueChangeByCategory = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentage = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentageByCategory = BigDecimal.ZERO;
		BigDecimal totalTargetPercentageByCategory = BigDecimal.ZERO;
		BigDecimal totalTargetValue = BigDecimal.ZERO;
		BigDecimal totalTargetPercentage = BigDecimal.ZERO;
		BigDecimal totalTargetValueByCategory = BigDecimal.ZERO;
		BigDecimal totalAdjustedMinimumTargetValue = BigDecimal.ZERO;
		BigDecimal totalAdjustedMinimumTargetPercentage = BigDecimal.ZERO;

		BigDecimal totalYtdWithdrawals = BigDecimal.ZERO;
		BigDecimal totalYtdExchanges = BigDecimal.ZERO;
		for (PortfolioFund fund : portfolio.getFundsByCategory(category)) {
			BigDecimal fundCurrentValue = fund.getValue();
			BigDecimal fundCurrentValueByCategory = fund.getValueByCategory(category);
			BigDecimal fundTotalTargetPercentage = fund.getPercentageByCategory(FundCategory.TOTAL);
			BigDecimal fundCategoryTargetPercentage = fund.getPercentageByCategory(category)
					.multiply(fundTotalTargetPercentage);

			// Current Value
			totalCurrentValue = totalCurrentValue.add(fundCurrentValue);
			totalCurrentValueByCategory = totalCurrentValueByCategory.add(fundCurrentValueByCategory);

			// Current Percentage
			totalCurrentPercentage = totalCurrentPercentage
					.add(CurrencyHelper.calculatePercentage(fundCurrentValue, portfolio.getTotalValue()));
			totalCurrentPercentageByCategory = totalCurrentPercentageByCategory
					.add(CurrencyHelper.calculatePercentage(fundCurrentValueByCategory, portfolio.getTotalValue()));

			// Target Percentage
			totalTargetPercentageByCategory = totalTargetPercentageByCategory.add(fundCategoryTargetPercentage);
			totalTargetPercentage = totalTargetPercentage.add(fundTotalTargetPercentage);

			// Target Value
			BigDecimal fundTargetValue = portfolio.getTotalValue().multiply(fundTotalTargetPercentage);
			totalTargetValue = totalTargetValue.add(fundTargetValue);
			totalTargetValueByCategory = totalTargetValueByCategory
					.add(fundCategoryTargetPercentage.multiply(portfolio.getTotalValue()));

			BigDecimal fundDividendByCategory = fund.getDistributionsAfterDate(getFirstOfYearDate())
					.multiply(fund.getPercentageByCategory(category));
			totalDividendsByCategory = totalDividendsByCategory.add(fundDividendByCategory);

			totalLastYearDividends = totalLastYearDividends.add(fund.getDistributionsBetweenDates(
					getFirstOfYearDate().minus(1, ChronoUnit.YEARS), getFirstOfYearDate().minus(1, ChronoUnit.DAYS)));

			MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);
			totalYtdValueChangeByCategory = totalYtdValueChangeByCategory.add(performance.getYtdValueChange());
			totalYtdWithdrawals = totalYtdWithdrawals.add(fund.getWithdrawalsUpToDate(getFirstOfYearDate()));

			totalYtdExchanges = totalYtdExchanges
					.add(BigDecimal.ZERO.subtract(fund.getExchangeTotalFromDate(getFirstOfYearDate())));

			if (fund.getMinimumAmount() != null) {
				totalAdjustedMinimumTargetValue = totalAdjustedMinimumTargetValue.add(fund.getMinimumAmount());
				totalAdjustedMinimumTargetPercentage = totalAdjustedMinimumTargetPercentage
						.add(CurrencyHelper.calculatePercentage(fund.getMinimumAmount(), portfolio.getTotalValue()));
			} else {
				totalAdjustedMinimumTargetValue = totalAdjustedMinimumTargetValue.add(fundTargetValue);
				totalAdjustedMinimumTargetPercentage = totalAdjustedMinimumTargetPercentage
						.add(fundTotalTargetPercentage);

			}

		}

		BigDecimal totalAdjustedMinimumDeviation = totalCurrentPercentage
				.subtract(totalAdjustedMinimumTargetPercentage);
		BigDecimal totalDeviationByCategory = totalCurrentPercentageByCategory
				.subtract(totalTargetPercentageByCategory);
		BigDecimal totalSurplusDeficit = totalCurrentValue.subtract(totalTargetValue);
		BigDecimal totalSurplusDeficitDerivation = totalCurrentValue.subtract(totalTargetValue);
		BigDecimal surpusDeficitByCategory = totalCurrentValueByCategory.subtract(totalTargetValueByCategory);
		BigDecimal adjustedMinimumSurplusDeficit = totalCurrentValue.subtract(totalAdjustedMinimumTargetValue);

		table.startNewRow();
		table.addCell(new Cell().add("Category Total").setItalic());
		table.addCell(new Cell().add(" "));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalDividendsByCategory)).setFontSize(12f));
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalLastYearDividends)).setFontSize(12f));
		Color totalYtdWithdrawalsFontColor = totalYtdWithdrawals.compareTo(BigDecimal.ZERO) > 0 ? Color.RED
				: Color.BLACK;
		Color totalYtdExchangesFontColor = totalYtdExchanges.compareTo(BigDecimal.ZERO) < 0 ? Color.RED : Color.GREEN;
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(totalYtdValueChangeByCategory))
						.setFontColor(calculateCurrencyFontColor(totalYtdValueChangeByCategory)))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(totalYtdWithdrawals))
						.setFontColor(totalYtdWithdrawalsFontColor))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(totalYtdExchanges))
						.setFontColor(totalYtdExchangesFontColor)));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));

		// Current Value
		table.addCell(createCurrentValueCell(false, totalCurrentValueByCategory, totalDeviationByCategory,
				totalCurrentValue, BigDecimal.ZERO, totalAdjustedMinimumTargetValue, totalAdjustedMinimumDeviation));

		// Current Percentage
		table.addCell(
				createCurrentPercentageCell(false, totalCurrentPercentageByCategory, totalTargetPercentageByCategory,
						totalCurrentValue, totalAdjustedMinimumTargetPercentage, totalCurrentPercentage));

		// Target Percentage
		table.addCell(createTargetPercentageCell(false, totalTargetPercentageByCategory,
				totalAdjustedMinimumTargetPercentage, null));

		// Target Value
		table.addCell(createTargetValueCell(false, totalTargetValueByCategory, totalAdjustedMinimumTargetValue, null));

		// Deviation
		table.addCell(createDeviationCell(false, totalDeviationByCategory, totalTargetValueByCategory, BigDecimal.ZERO,
				totalAdjustedMinimumDeviation, null));

		// Surplus / Deficit
		table.addCell(createSurplusDeficitCell(false, surpusDeficitByCategory, totalDeviationByCategory,
				totalSurplusDeficit, BigDecimal.ZERO, totalAdjustedMinimumDeviation, adjustedMinimumSurplusDeficit));

	}

	public void savePortfolioData() {
		saveHistoricalPrices(HISTORICAL_PRICES_FILE);
		saveHistoricalValue(HISTORICAL_VALUES_FILE);
		saveHistoricalShares(HISTORICAL_SHARES_FILE);
	}

	public void updateDownloadFilenames(String downloadFilenamePrefix) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss.SSS");

		try (Stream<Path> stream = Files.list(Paths.get(basePath))) {

			List<String> filenames = stream.filter(p -> p.getFileName().toString().startsWith(downloadFilenamePrefix))
					.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
			for (String filename : filenames) {
				LocalDateTime date = getDownloadFileDate(filename, false);

				if (date == null) {
					// Not a valid download file
					continue;
				}

				// rename file to correct mistake
				String newFileName = downloadFilenamePrefix + " - " + date.format(formatter) + ".csv";
				if (Files.exists(Paths.get(basePath, filename)) && !Files.exists(Paths.get(basePath, newFileName))) {
					Files.move(Paths.get(basePath, filename), Paths.get(basePath, newFileName));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ManagedPortfolio getPortfolio() {
		return portfolio;
	}

}
