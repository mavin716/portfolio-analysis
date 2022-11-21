package com.wise.portfolio.data;

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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
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
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.formula.functions.Irr;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.MinMaxCategoryRenderer;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.time.Day;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

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
import com.wise.portfolio.data.MutualFund.FundCategory;

public class PortfolioService {

	public static final int CURRENCY_SCALE = 4;
	public static final Charset INPUT_CHARSET = StandardCharsets.ISO_8859_1;
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a");

	private LocalDate mostRecentSharePrice = LocalDate.of(2000, 1, 1);
	private int oldestDay = 640;
	private String basePath;

	public static Map<String, String> fundSymbolNameMap = new HashMap<>();

	private Map<FundCategory, BigDecimal> desiredCategoryAllocation = new HashMap<>();

	private Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocationMaps = new HashMap<>();

	private Integer[] rankDaysArray = { 1, 3, 5, 15, 30, 60, 90, 120, 180, 270, 365, 480, 560, 740, 830, 920, 1010 };
	private List<Integer> enhancedRankDaysList = new LinkedList<>();
	private Portfolio portfolio;

	private final static String MAIL_HOST = "smtp.mail.yahoo.com";
	private final static String YAHOO_MAIL_APP_PASSWORD = "hgcqlgrdhxdvgmjm";
	private final static String MAIL_TO = "mavin14534@yahoo.com";
	private final static String MAIL_FROM = "mavin14534@yahoo.com";

	private static java.awt.Color[] axisPaints = { java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.GREEN,
			new java.awt.Color(104, 30, 126), // Purple
			new java.awt.Color(0, 121, 231), // Lt Blue
			new java.awt.Color(255, 116, 53), // Orange
			new java.awt.Color(255, 240, 53), // Yellow
			new java.awt.Color(251, 72, 196), // Hot Pink
			new java.awt.Color(0, 158, 71), // Med Green
			java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.GREEN, new java.awt.Color(104, 30, 126), // Purple
			new java.awt.Color(0, 121, 231), // Lt Blue
			new java.awt.Color(255, 88, 0), // Orange
			new java.awt.Color(255, 240, 53), // Yellow
			new java.awt.Color(251, 72, 196), // Hot Pink
			new java.awt.Color(0, 158, 71), // Med Green
	};

	public void sendMail(String subject, String textBody, File portfolioPdfFile) {
		Properties properties = System.getProperties();

		properties.put("mail.smtp.host", MAIL_HOST);
		properties.put("mail.smtp.port", "587");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.auth", "true");

		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("mavin14534@yahoo.com", YAHOO_MAIL_APP_PASSWORD);
			}
		});

//		session.setDebug(true);
		try {
			Multipart multipart = new MimeMultipart();
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(textBody, "utf-8", "html");
			multipart.addBodyPart(messageBodyPart);

			MimeBodyPart attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(portfolioPdfFile, "application/pdf", null);
			multipart.addBodyPart(attachmentBodyPart);
			MimeMessage message = new MimeMessage(session);
			message.setContent(multipart);

			message.setFrom(new InternetAddress(MAIL_FROM));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(MAIL_TO));
			message.setSubject(subject);

			Transport.send(message);
		} catch (MessagingException | IOException mex) {
			mex.printStackTrace();
		}
	}

	/**
	 * @param portfolio
	 * @throws Exception
	 * 
	 */
	public void rebalanceFunds(Portfolio portfolio, BigDecimal exchangeIncrement,
			Map<String, BigDecimal> currentfundAdjustments) throws Exception {
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
			System.out.println(String.format("%-60s", fundSymbolNameMap.get(exchangeFromFundSymbol))
					+ "Surplus amount:  " + CurrencyHelper.formatAsCurrencyString(surplusEntry.getValue())
					+ ", exchange from value:  " + CurrencyHelper.formatAsCurrencyString(exchangeFromValue));

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
					System.out.println(String.format("%-24s", fundSymbolNameMap.get(exchangeToFundSymbol))
							+ "Deficit amount:  " + CurrencyHelper.formatAsCurrencyString(exchangeToValue));

					int exchangeToIncrements = exchangeToValue.divide(exchangeIncrement).intValue();
					if (exchangeToIncrements > 0) {
						// determine exchange value for this transaction
						BigDecimal exchangeValue = exchangeIncrement
								.multiply(new BigDecimal(Math.min(exchangeFromIncrements, exchangeToIncrements)));

						System.out.println("\nExchange:  " + CurrencyHelper.formatAsCurrencyString(exchangeValue)
								+ " from:  " + String.format("%-24s", fundSymbolNameMap.get(exchangeFromFundSymbol))
								+ " to:  " + String.format("%-24s", fundSymbolNameMap.get(exchangeToFundSymbol))
								+ "\n");

						portfolio.adjustValue(exchangeFromFundSymbol, exchangeValue.negate());
						portfolio.adjustValue(exchangeToFundSymbol, exchangeValue);

						System.out.println(String.format("%-60s", fundSymbolNameMap.get(exchangeFromFundSymbol))
								+ "Adjusted value:  " + CurrencyHelper
										.formatAsCurrencyString(portfolio.getFund(exchangeFromFundSymbol).getValue()));
						System.out.println(String.format("%-60s", fundSymbolNameMap.get(exchangeToFundSymbol))
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

	public void printTrends(Portfolio portfolio) {
		System.out.println("\n\nPerformance sorted by name");
		Collection<PortfolioFund> funds = portfolio.getFundMap().values();
		for (Integer days : rankDaysArray) {
			printFundsPerformance(days, funds,
					(PortfolioFund o1, PortfolioFund o2) -> o1.getName().compareTo(o2.getName()));
		}

		System.out.println("\n\nPerformance sorted by IRR");
		for (Integer days : rankDaysArray) {
			printFundsPerformance(days, funds,
					(PortfolioFund f1, PortfolioFund f2) -> getInternalRateReturn(f2, f2.getCost())
							.compareTo(getInternalRateReturn(f1, f1.getCost())));
		}

		for (Integer days : rankDaysArray) {
			System.out.println("\n\nPerformance sorted by descending " + days + " days change");
			printFundsPerformance(days, funds, new CompareByPerformanceDays(days));
		}
	}

	private BigDecimal performAdjustments(Portfolio portfolio, Map<String, BigDecimal> adjustments,
			BigDecimal surplus) {
		for (Entry<String, BigDecimal> entry : adjustments.entrySet()) {
			String fundSymbol = entry.getKey();
			BigDecimal adjustment = entry.getValue();

			// Make the changes
			if (adjustment.intValue() < 0) {
				surplus = surplus.subtract(adjustment);
				System.out.println("fund:  " + String.format("%60s", getFundName(fundSymbol)) + " subtraction:  "
						+ adjustment.negate());
				portfolio.adjustValue(fundSymbol, adjustment);
				System.out.println("Running surplus:  " + CurrencyHelper.formatAsCurrencyString(surplus));
			} else if (adjustment.intValue() > 0) {
				surplus = surplus.subtract(adjustment); // adjustment is
														// negative, deficit is
														// positive
				portfolio.adjustValue(fundSymbol, adjustment);
				System.out.println(
						"fund:  " + String.format("%60s", getFundName(fundSymbol)) + " addition:  " + adjustment);
				System.out.println("Running surplus:  " + CurrencyHelper.formatAsCurrencyString(surplus));
			}
		}
		return surplus;
	}

	/**
	 * Print each funds performance in order determined by given comparator
	 * 
	 * @param days
	 * @param funds
	 * @param c
	 */
	private void printFundsPerformance(Integer days, Collection<PortfolioFund> funds,
			Comparator<? super PortfolioFund> c) {
		// Create map of fund symbol to list of rankings per rankDays
		Map<String, LinkedList<Integer>> fundRanking = createFundRankingList(funds);

		// Print each fund performance details
		System.out.println("\nfund performance by days:  " + days);
		funds.stream().sorted(c)
				.forEach(f -> System.out.println(generateFundPeformanceText(f, days, fundRanking.get(f.getSymbol()))));
	}

	private String generateFundPeformanceText(PortfolioFund fund, Integer days, LinkedList<Integer> fundRanking) {

		MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);
		LocalDate historicalDate = getClosestHistoricalDate(days);
		Float irr = getInternalRateReturn(fund, fund.getCost());
		StringBuffer displayString = new StringBuffer(String.format("%-24s", fund.getShortName()) + " irr: "
				+ CurrencyHelper.formatPercentageString(irr) + "% ");
		displayString.append(String.valueOf(days)).append("d:")
				.append(CurrencyHelper.formatPercentageString(performance.getPerformanceRateByDate(historicalDate)))
				.append("% ");

		displayString.append(generatePerformanceStatusText(fund));

		return displayString.toString();

	}

	public void printRanking(Portfolio portfolio, Document document) {

		Collection<PortfolioFund> funds = portfolio.getFundMap().values();

		// Ranking is used to generate trend status text
		Map<String, LinkedList<Integer>> fundRanking = createFundRankingList(funds);

		document.add(new Paragraph("Ranking by 1 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(1), 1);

		document.add(new Paragraph("Ranking by 3 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(3), 3);

		document.add(new Paragraph("Ranking by 5 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(5), 5);

		document.add(new Paragraph("Ranking by 15 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(15), 15);

		document.add(new Paragraph("Ranking by 30 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(30), 30);

		document.add(new Paragraph("Ranking by 60 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(60), 60);

		document.add(new Paragraph("Ranking by 90 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(90), 90);

		document.add(new Paragraph("Ranking by 120 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(120), 120);

		document.add(new Paragraph("Ranking by 180 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(180), 180);

		document.add(new Paragraph("Ranking by 365 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(365), 365);

		int ytdDays = getYtdDays();
		document.add(new Paragraph("Ranking by YTD change (" + ytdDays + ")"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(ytdDays), ytdDays);
		document.add(new Paragraph("Ranking by 480 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(480), 480);

		document.add(new Paragraph("Ranking by 560 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(560), 560);

		document.add(new Paragraph("Ranking by 650 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(650), 650);

		document.add(new Paragraph("Ranking by 740 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(740), 740);

		document.add(new Paragraph("Ranking by 830 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(830), 830);

		document.add(new Paragraph("Ranking by 920 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(920), 920);

		document.add(new Paragraph("Ranking by 1010 day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(1010), 1010);

		document.add(new Paragraph("Ranking by oldest day change"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(oldestDay), oldestDay);

		document.add(new Paragraph("Ranking by compounding difference of ranking"));
		printFundsRanking(document, portfolio, fundRanking, new Comparator<PortfolioFund>() {
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

		MutualFundPerformance performance = new MutualFundPerformance(portfolio, portfolio.getFund("VFIAX"));
		LocalDate maxPriceDate = performance.getMaxPricePair().getKey();
		LocalDate minPriceDate = performance.getMinPricePair().getKey();
		Period period = maxPriceDate.until(LocalDate.now());
		Integer maxPriceDays = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
		period = minPriceDate.until(LocalDate.now());
		Integer minPriceDays = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();

		document.add(new Paragraph("Ranking by min price"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(minPriceDays), minPriceDays);

		document.add(new Paragraph("Ranking by max price"));
		printFundsRanking(document, portfolio, fundRanking, new CompareByPerformanceDays(maxPriceDays), maxPriceDays);

		document.add(new Paragraph(
				"Ranking by name (natural sort order for Fund).  Trend shows rank movement, trend timeline, rank position"));
		printFundsRanking(document, portfolio, fundRanking, null, 360);

	}

	private class CompareByPerformanceDays implements Comparator<PortfolioFund> {

		public CompareByPerformanceDays(int days) {
			super();

			this.days = days;

			LocalDate date = LocalDate.now();
			date = date.minusDays(days);
			if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
				this.days++;
				date = date.minusDays(1);
			}
			if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
				this.days++;
				date = date.minusDays(1);
			}

		}

		private int days;

		@Override
		public int compare(PortfolioFund f1, PortfolioFund f2) {
			LocalDate historicalDate = getClosestHistoricalDate(days);
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

	private void printFundsRanking(Document document, Portfolio portfolio, Map<String, LinkedList<Integer>> fundRanking,
			Comparator<PortfolioFund> c, int days) {

		// Creating a table
		float[] pointColumnWidths = { 10F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F,
				5F, 5F, 5F };
		Table table = new Table(pointColumnWidths);
		table.setFontSize(12);

		table.addHeaderCell(new Cell().add("Fund").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Returns").setTextAlignment(TextAlignment.CENTER));

		for (Integer rankdays : enhancedRankDaysList) {

			Cell cell1 = new Cell(); // Creating a cell
			LocalDate historicalDate = LocalDate.now().minusDays(rankdays);
			if (rankdays.equals(new Integer(days))) {
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
				.filter(f -> fundRanking.get(f.getSymbol()) != null);

		// Sort, if comparable provided
		if (c != null) {
			fundStream = fundStream.sorted(c);
		}

		// Print fund ranking text for each fund
		fundStream.forEach(f -> addFundRankingToTalbe(table, f, fundRanking.get(f.getSymbol()), days));

		BigDecimal totalChange = portfolio.getFundMap().values().stream()
				.map(f -> f.getValue().subtract(getHistoricalValue(f, days))).reduce(BigDecimal.ZERO, BigDecimal::add);
		LocalDate historicalDate = LocalDate.now().minusDays(days);
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
			document.add(table);
			document.add(new AreaBreak());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private BigDecimal getHistoricalValue(PortfolioFund fund, int days) {
		BigDecimal historicalValue = BigDecimal.ZERO;

		LocalDate historicalDate = LocalDate.now().minusDays(days);
		BigDecimal historicPrice = getPriceByDate(fund, historicalDate, false);
		if (historicPrice == null) {
			historicPrice = BigDecimal.ZERO;
		}
		Float historicalShares = getSharesByDate(fund, historicalDate, false);
		if (historicalShares == null) {
			historicalShares = new Float(0);
		}
		historicalValue = historicPrice.multiply(new BigDecimal(historicalShares));

		return historicalValue;
	}

	private void addFundRankingToTalbe(Table table, PortfolioFund fund, List<Integer> rankingDays, int days) {

		try {
			MutualFundPerformance fundPerformance = new MutualFundPerformance(portfolio, fund);

			LocalDate historicalDate = getClosestHistoricalDate(days);
			BigDecimal returns = fundPerformance.getReturnsByDate(historicalDate, false);
			if (returns == null) {
				returns = new BigDecimal(0);
			}
			Float rate = fundPerformance.getPerformanceRateByDate(historicalDate);
			if (rate == null) {
				rate = new Float(0);
			}

			// Create a map of ranking to rates
			// Pair<index, Pair<rank, rate>>
			// TODO If symbol stored instead of fund data then can calculate data on fly
			List<Pair<Integer, Pair<Integer, MutualFundPerformance>>> ranks = new LinkedList<>();
			for (int rankDayIndex = 0; rankDayIndex < rankingDays.size(); rankDayIndex++) {
				int rankDays = enhancedRankDaysList.get(rankDayIndex);

				Float historicalRate = null;
				LocalDate rankHistoricalDate = getClosestHistoricalDate(rankDays);
				historicalRate = fundPerformance.getPerformanceRateByDate(rankHistoricalDate);
				if (historicalRate == null) {
					historicalRate = new Float(0);
				}

				int rank = rankingDays.get(rankDayIndex);
				LocalDate fundStartDate = portfolio.getPriceHistory().getFundStart(fund.getSymbol());
				if (fundStartDate.isAfter(rankHistoricalDate)) {
					rank = 0;
				}
				ranks.add(Pair.of(rankDayIndex, Pair.of(rank, fundPerformance)));
			}

			BigDecimal historicPrice = getClosestHistoricalPrice(fund, historicalDate, 5);
			if (historicPrice == null) {
				historicPrice = BigDecimal.ZERO;
			}
			Float historicalShares = getSharesByDate(fund, historicalDate, false);
			if (historicalShares == null) {
				historicalShares = new Float(0);
			}
			BigDecimal historicValue = portfolio.getPriceHistory().getFundValueByDate(fund, historicalDate, false);
			if (historicValue == null) {
				historicValue = BigDecimal.ZERO;
			}

			// Fund Name
			table.addCell(new Cell().setFontSize(10).add(fund.getShortName()).setTextAlignment(TextAlignment.LEFT));

			// Fund Returns
			Cell cell = new Cell().add(CurrencyHelper.formatAsCurrencyString(returns)).setMargin(0f).setFontSize(10);
			if (returns.compareTo(BigDecimal.ZERO) < 0) {
				cell.setBackgroundColor(Color.RED, 0.5f);
			} else {
				cell.setBackgroundColor(Color.GREEN, 0.5f);
			}
			table.addCell(cell);

			ranks.stream().forEach(rankPair -> table.addCell(
					new Cell()
							.setFontSize(12).setMargin(0)
							.add(new Cell()
									.setBackgroundColor(calculateRankBackgroundColor(rankPair.getRight().getLeft()),
											calculateRankBackgroundOpacity(rankPair.getRight().getLeft()))
									.add(String.format("%2d", rankPair.getRight().getLeft())))
							.add(new Cell().setMargin(0).setFontSize(10).setBackgroundColor(
									calculateCurrencyFontColor(new BigDecimal(fundPerformance.getPerformanceRateByDate(
											getClosestHistoricalDate(enhancedRankDaysList.get(rankPair.getLeft()))))),
									new BigDecimal(fundPerformance.getPerformanceRateByDate(
											getClosestHistoricalDate(enhancedRankDaysList.get(rankPair.getLeft()))))
													.abs().multiply(new BigDecimal(1000))
													.divide(new BigDecimal(
															enhancedRankDaysList.get(rankPair.getLeft())), 4,
															RoundingMode.HALF_DOWN)
													.floatValue())
									.add(CurrencyHelper.formatPercentageString(fundPerformance.getPerformanceRateByDate(
											getClosestHistoricalDate(enhancedRankDaysList.get(rankPair.getLeft()))))))
							.add(new Cell().setMargin(0).setFontSize(10)
									.setBackgroundColor(
											calculateCurrentPriceColor(rankPair.getRight().getRight(),
													getClosestHistoricalDate(
															enhancedRankDaysList.get(rankPair.getLeft()))),
											calculateCurrenPriceOpacity(rankPair.getRight().getRight(),
													getClosestHistoricalDate(
															enhancedRankDaysList.get(rankPair.getLeft()))))
									.add(CurrencyHelper.formatAsCurrencyString(getPriceByDate(
											rankPair.getRight().getRight().getFund(),
											getClosestHistoricalDate(enhancedRankDaysList.get(rankPair.getLeft())),
											false))))));

			table.startNewRow();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private float calculateCurrenPriceOpacity(MutualFundPerformance fundPerformance, LocalDate date) {
		BigDecimal minPrice = fundPerformance.getMinPricePair().getRight();
		BigDecimal maxPrice = fundPerformance.getMaxPricePair().getRight();
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

	private Color calculateCurrentPriceColor(MutualFundPerformance fundPerformance, LocalDate date) {
		BigDecimal minPrice = fundPerformance.getMinPricePair().getRight();
		BigDecimal maxPrice = fundPerformance.getMaxPricePair().getRight();
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

	private Color calculateRankBackgroundColor(int rank) {
		Color backgroundColor = rank <= 12 ? (rank == 0 ? Color.WHITE : Color.GREEN) : Color.RED;
		return backgroundColor;
	}

	private float calculateRankBackgroundOpacity(int rank) {
		float backgroundOpacity = rank <= 12 ? (13 - rank) / 12f : (rank - 12) / 12f;
		return backgroundOpacity;
	}

	private Pair<Color, Float> calculatePriceBackgroundOpacity(BigDecimal price, BigDecimal highPrice,
			BigDecimal lowPrice) {

		BigDecimal range = highPrice.subtract(lowPrice);
		Color color = Color.BLACK;
		Float opacity = 1f;
		BigDecimal midPrice = range.divide(new BigDecimal(2), RoundingMode.HALF_DOWN);
		if (price.compareTo(midPrice) > 0) {
			color = Color.GREEN;
			opacity = highPrice.subtract(price)
					.divide(range.divide(new BigDecimal(2), RoundingMode.HALF_DOWN), RoundingMode.HALF_DOWN)
					.floatValue();
		} else {
			color = Color.RED;
			opacity = price.subtract(lowPrice)
					.divide(range.divide(new BigDecimal(2), RoundingMode.HALF_DOWN), RoundingMode.HALF_DOWN)
					.floatValue();
		}

		return Pair.of(color, opacity);
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
				double fundRate = performance.getPerformanceRateByDate(getClosestHistoricalDate(1));
				if (fundRate == 0) {
					oneDayRanking = false;
				}
				maxPriceDate = performance.getMaxPricePair().getKey();
				minPriceDate = performance.getMinPricePair().getKey();
			}
		}

		// Create dynamic rank days list to include ytd and oldest day
		int ytdDays = getYtdDays();
		Period period = maxPriceDate.until(LocalDate.now());
		Integer maxPriceDays = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
		period = minPriceDate.until(LocalDate.now());
		Integer minPriceDays = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
		for (int i = 0; i < rankDaysArray.length; i++) {

			if (i > 1 && ytdDays > rankDaysArray[i - 1] && ytdDays < rankDaysArray[i]) {
				enhancedRankDaysList.add(ytdDays);
			}
			if (i > 1 && maxPriceDays > rankDaysArray[i - 1] && maxPriceDays < rankDaysArray[i]) {
				enhancedRankDaysList.add(maxPriceDays);
			}
			if (i > 1 && minPriceDays > rankDaysArray[i - 1] && minPriceDays < rankDaysArray[i]) {
				enhancedRankDaysList.add(minPriceDays);
			}
			enhancedRankDaysList.add(rankDaysArray[i]);
		}
		enhancedRankDaysList.add(oldestDay);

		AtomicInteger rank = new AtomicInteger();
		for (int numDays : enhancedRankDaysList) {
			// @TODO if no change for today then don't add to fundRanking (messes up
			// weighted order), instead use 3 day
			int days = numDays;
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
		LocalDate historicalDate = getClosestHistoricalDate(days);
		MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);

		StringBuilder description = new StringBuilder(" LONG TERM RETURN (" + days + "d): ");
		Float longTermPerformanceRate = performance.getPerformanceRateByDate(historicalDate);
		if (longTermPerformanceRate == null) {
			description.append(String.format("%25s", "N/A "));
		} else {
			description.append(generateRateOfReturnDescription(longTermPerformanceRate / days));
		}

		description.append("\tSHORT TERM RETURN (5d): ");
		Float shortTermPerformanceRate = performance.getPerformanceRateByDate(getClosestHistoricalDate(5));
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

	private static String generateTrendScaleDescription(LinkedList<Integer> ranking) {

		int sumDifference = 0;
		String trendScale;
		int scale = 0;
		// start with one week change
		for (int i = 2; i <= ranking.size() - 1; i++) {
			int difference = Math.abs(ranking.get(i - 1) - ranking.get(i));
			sumDifference += difference;
			if (difference >= 8) {
				scale += 8;
			} else if (difference >= 4) {
				scale += 4;
			} else if (difference >= 1) {
				scale += 1;
			}
		}

		// if (scale >= 32)
		// {
		// trendScale = "VOLATILE ";
		// }
		// else if (scale >= 24)
		// {
		// trendScale = "MODERATE ";
		// }
		// else if (scale >= 12)
		// {
		// trendScale = "GRADUAL ";
		// }
		// else if (scale >= 8)
		// {
		// trendScale = "SLOW ";
		// }
		// else
		// {
		// trendScale = "STEADY ";
		// }

		if (sumDifference >= 75) {
			trendScale = "VOLATILE ";
		} else if (sumDifference >= 60) {
			trendScale = "MODERATE ";
		} else if (sumDifference >= 49) {
			trendScale = "GRADUAL ";
		} else if (sumDifference >= 38) {
			trendScale = "SLOW ";
		} else if (sumDifference > 21) {
			trendScale = "SLIGHTLY";
		} else {
			trendScale = "STEADY ";
		}

		return trendScale;
	}

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

	public PortfolioService(String basePath, String fundSymbolFileName) {
		this.basePath = basePath;
		loadFundSymbolMap(fundSymbolFileName);
	}

	public Portfolio createPortfolio() {
		this.portfolio = new Portfolio();
		portfolio.setFundSymbolNameMap(fundSymbolNameMap);
		return portfolio;
	}

	/**
	 * @param path to find price history files
	 * 
	 * @throws IOException
	 */
	public void loadPriceHistory(Portfolio portfolio, String downloadFileNamePrefix) throws IOException {

		// Load all download files
		LocalDate earliestDate = portfolio.getPriceHistory().getOldestDay();
		try (Stream<Path> stream = Files.list(Paths.get(basePath))) {

			List<String> filenames = stream.filter(p -> p.getFileName().toString().startsWith(downloadFileNamePrefix))
					.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
//			List<LocalDate> fileDates = new ArrayList<>();
			BigDecimal yesterdayWithdrawals = BigDecimal.ZERO;
			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			for (String filename : filenames) {
				LocalDate date = getDownloadFileDate(filename);

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
				if (date.isBefore(earliestDate)) {
					earliestDate = date;

				}
				priceHistory.loadPortfolioFile(portfolio, date, basePath + "\\" + filename);

				BigDecimal withdrawals = BigDecimal.ZERO;
				for (PortfolioFund fund : portfolio.getFundMap().values()) {
					if (fund != null) {
						BigDecimal todayWithdrawals = fund.getWithdrawalTotalForDate(date);
						// Check if Sell transaction didn't get included in yesterdays download but
						// trade date was yesterday
						if (todayWithdrawals.compareTo(BigDecimal.ZERO) == 0
								&& yesterdayWithdrawals.compareTo(BigDecimal.ZERO) == 0) {
							todayWithdrawals = fund.getWithdrawalTotalForDate(date.minusDays(1));
						}
						withdrawals = withdrawals.add(todayWithdrawals);
					}
				}
				yesterdayWithdrawals = withdrawals;
				BigDecimal income = BigDecimal.ZERO;
				for (PortfolioFund fund : portfolio.getFundMap().values()) {
					if (fund != null) {
						income = income.add(fund.getDistributionsForDate(date));
					}
				}

			}
			priceHistory.setOldestDay(earliestDate);
		}

		// Get price history via alphaVantage
		int numFunds = portfolio.getFundMap().size();
		System.out.println("will take an extra " + (int) numFunds / 5 + " minutes to call alphaVantage API");
		for (Entry<String, PortfolioFund> entry : portfolio.getFundMap().entrySet()) {
			String symbol = entry.getKey();
			if (entry.getValue().getShares() == 0) {
				continue;
			}
			System.out.println("Get price history for: " + entry.getValue().getShortName() + " " + symbol);

			AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, true);
//			AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
			earliestDate = portfolio.getPriceHistory().getOldestDay();
		}

		// Calculate oldest day
		Period period = earliestDate.until(LocalDate.now());
		oldestDay = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();

		if (portfolio.getPriceHistory().getMostRecentDay().isAfter(mostRecentSharePrice)) {
			mostRecentSharePrice = portfolio.getPriceHistory().getMostRecentDay();
		}

		// TODO read saved values to fill in
		portfolio.getPriceHistory().loadPortfolioHistoryFile(portfolio, basePath, "historical.csv");
		portfolio.getPriceHistory().loadPortfolioSharesFile(portfolio, basePath, "historicalshares.csv");

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

	}

	private LocalDate getDownloadFileDate(String filename) {
		LocalDate date = null;
		LocalTime time = null;
		if (filename.length() == 39) {
			String datestring = filename.substring(14, 24);
			// Get time and create policy for multiple files on oneday, morning vs. night
			date = LocalDate.parse(datestring);
			StringBuffer timestring = new StringBuffer(filename.substring(25, 31));
			timestring.insert(2, ":");
			timestring.insert(5, ":");
			// Get time and create policy for multiple files on oneday, morning vs. night
			time = LocalTime.parse(timestring);
			LocalTime cutoffTime = LocalTime.of(18, 0);
			if (time.isBefore(cutoffTime)) {
				date = date.minusDays(1);
			}

		} else if (filename.length() == 15) {
			// Assume this is the current downlaod for today
			date = LocalDate.now();
		} else {
			System.out.println("Ignore, filename has Invalid format:  " + filename + " size: " + filename.length());
		}
		return date;
	}

	public String getFundName(String symbol) {
		return fundSymbolNameMap.get(symbol);
	}

	public String getFundSymbol(String name) {
		for (Entry<String, String> fundEntry : fundSymbolNameMap.entrySet()) {
			if (fundEntry.getValue().contains(name)) {
				return fundEntry.getKey();
			}
		}
		return null;

	}

	public Map<String, BigDecimal> calculateWithdrawal(Portfolio portfolio, BigDecimal withdrawalAmount,
			BigDecimal cashReservesWithdrawalAmount, BigDecimal federalMMWithdrawalAmount) {

		Map<String, BigDecimal> withdrawalMap = new LinkedHashMap<>();

		// Create map sorted by descending value of deviation
		Map<String, Pair<BigDecimal, PortfolioFund>> sortedDifferenceMap = createSortedDeviationMap(portfolio,
				withdrawalAmount);

		BigDecimal nextDeviation = BigDecimal.ZERO;
		for (Entry<String, Pair<BigDecimal, PortfolioFund>> entry : sortedDifferenceMap.entrySet()) {
			nextDeviation = entry.getValue().getLeft().setScale(4, RoundingMode.HALF_DOWN);
			break;
		}

		// withdrawl increment is one percent of fund value
		BigDecimal withdrawalIncrement = portfolio.getTotalValue().divide(new BigDecimal(10000), 4,
				RoundingMode.HALF_DOWN);
		// Round up to nearest 5
		withdrawalIncrement = withdrawalIncrement.divide(new BigDecimal(5), 0, RoundingMode.HALF_DOWN)
				.setScale(2, RoundingMode.HALF_DOWN).multiply(new BigDecimal(5));

		System.out.println("Starting deviation:  " + CurrencyHelper.formatPercentageString4(nextDeviation));
		BigDecimal runningWithdrawal = BigDecimal.ZERO;
		while (runningWithdrawal.compareTo(withdrawalAmount) < 0) {
			for (Entry<String, Pair<BigDecimal, PortfolioFund>> entry : sortedDifferenceMap.entrySet()) {
				BigDecimal fundDeviation = entry.getValue().getLeft();
				PortfolioFund fund = entry.getValue().getRight();
				System.out.println("Fund:  " + fund.getShortName() + " dev "
						+ CurrencyHelper.formatPercentageString4(fundDeviation));

				BigDecimal fundWithdrawalIncrement = withdrawalIncrement;

				if (fundDeviation.compareTo(nextDeviation) >= 0) {
					if (runningWithdrawal.add(fundWithdrawalIncrement).compareTo(withdrawalAmount) > 0) {
						fundWithdrawalIncrement = withdrawalAmount.subtract(runningWithdrawal);
					}
					BigDecimal runningFundWithdrawalAmount = withdrawalMap.get(fund.getSymbol());
					if (runningFundWithdrawalAmount == null) {
						runningFundWithdrawalAmount = BigDecimal.ZERO;
					}
					if (fund.getMinimumAmount() != null
							&& fund.getValue().subtract(runningFundWithdrawalAmount.add(fundWithdrawalIncrement))
									.compareTo(fund.getMinimumAmount()) <= 0) {
						fundWithdrawalIncrement = BigDecimal.ZERO;
					}

					runningFundWithdrawalAmount = runningFundWithdrawalAmount.add(fundWithdrawalIncrement);

					System.out.println("Fund:  " + fund.getShortName() + " running withdrawal amount "
							+ CurrencyHelper.formatAsCurrencyString(runningFundWithdrawalAmount));
					withdrawalMap.put(fund.getSymbol(), runningFundWithdrawalAmount);

					runningWithdrawal = runningWithdrawal.add(fundWithdrawalIncrement);
					System.out.println("runningWithdrawal " + CurrencyHelper.formatAsCurrencyString(runningWithdrawal));
					if (runningWithdrawal.compareTo(withdrawalAmount) == 0) {
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

	public BigDecimal getFundDeviation(PortfolioFund fund) {
		if (fund.isClosed()) {
			return BigDecimal.ZERO;
		}
		BigDecimal availableFundValue = fund.getAvailableValue();
		Map<FundCategory, BigDecimal> map = desiredFundAllocationMaps.get(fund.getSymbol());
		BigDecimal desiredFundPercentage = map.get(FundCategory.TOTAL);
		BigDecimal desiredFundValue = portfolio.getTotalValue().multiply(desiredFundPercentage, MathContext.UNLIMITED);
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
		BigDecimal targetValue = portfolio.getTotalValue().multiply(fundTargetPercentage);
		if (fund.getMinimumAmount() != null) {
			if (targetValue.compareTo(fund.getMinimumAmount()) < 0) {
				fundTargetPercentage = fund.getMinimumAmount().divide(portfolio.getTotalValue(), 6,
						RoundingMode.HALF_DOWN);
			}
		}

		BigDecimal deviation = BigDecimal.ZERO;
		if (fund.getValue().compareTo(BigDecimal.ZERO) != 0) {
			BigDecimal currentPercentage = fund.getValue().divide(portfolio.getTotalValue(), 6, RoundingMode.HALF_DOWN);
			deviation = currentPercentage.subtract(fundTargetPercentage);
		}
		return deviation;
	}

	private Map<String, Pair<BigDecimal, PortfolioFund>> createSortedDeviationMap(Portfolio portfolio,
			BigDecimal portfolioAdjustment) {

		Map<String, Pair<BigDecimal, PortfolioFund>> sortedDifferenceMap = new TreeMap<String, Pair<BigDecimal, PortfolioFund>>(
				new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						PortfolioFund f1 = portfolio.getFund(o1);
						PortfolioFund f2 = portfolio.getFund(o2);
						return getFundDeviation(f2).compareTo(getFundDeviation(f1));
					}

				});

		// Populate sorted map
		for (PortfolioFund fund : portfolio.getFundMap().values()) {
			String symbol = fund.getSymbol();
			if (symbol == null) {
				continue;
			}
			Map<FundCategory, BigDecimal> map = desiredFundAllocationMaps.get(symbol);
			if (map == null) {
				continue;
			}

			BigDecimal deviation = getFundDeviation(fund);
			sortedDifferenceMap.put(fund.getSymbol(), Pair.of(deviation, fund));
		}

		return sortedDifferenceMap;
	}

	public List<PortfolioFund> getFundsByCategory(Portfolio portfolio, FundCategory category) {

		return portfolio.getFundMap().values().stream()
				.filter(fund -> fund.getCategoriesMap().get(category).compareTo(BigDecimal.ZERO) > 0).sorted()
				.collect(Collectors.toList());

	}

	public BigDecimal getPriceByDate(Fund fund, LocalDate date, boolean isExactDate) {
		BigDecimal value = null;

		Map<LocalDate, BigDecimal> fundPriceMap = portfolio.getPriceHistory().getFundPrices().get(fund.getSymbol());
		value = fundPriceMap.get(date);
		if (value == null) {
			if (!isExactDate) {
				int tries = 30;
				while (value == null && tries-- > 0) {
					date = date.minus(1, ChronoUnit.DAYS);
					value = fundPriceMap.get(date);
				}
				if (value == null) {
					return BigDecimal.ZERO;
				}
			} else {
				return BigDecimal.ZERO;
			}
		}

		return value;
	}

	public Float getSharesByDate(Fund fund, LocalDate date, boolean isExactDate) {
		Float value = null;

		Map<LocalDate, Float> fundSharesMap = portfolio.getPriceHistory().getFundShares().get(fund.getSymbol());
		value = fundSharesMap.get(date);
		if (!isExactDate) {
			int tries = 30;
			while (tries-- > 0) {
				value = fundSharesMap.get(date);
				if (value != null) {
					return value;
				}
				date = date.minus(1, ChronoUnit.DAYS);
			}
		}

		// used by some functions expecting number so for now just return zero
		return null;
	}

	public BigDecimal getValueByDate(PortfolioFund fund, LocalDate date) {

		BigDecimal value = new BigDecimal(0);

		BigDecimal price = getPriceByDate(fund, date, false);
		double shares = fund.getShares();
		if (shares <= 0) {
			return value;
		}
		int tries = 30;
		while (tries-- > 0) {
			if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
				value = price.multiply(new BigDecimal(shares));
				if (value != null) {
					return value;
				}
			}
			date = date.minus(1, ChronoUnit.DAYS);
			price = getPriceByDate(fund, date, true);
		}

		return value;
	}

	/**
	 * NOTE This isn't accurate because the historical price data is only as good as
	 * the downlaoded files. There is a gap in prices so it unfairly weights the
	 * ones that are present.... I'm happy to get this to work and understand IRR.
	 * 
	 * @param symbol
	 * @return
	 */
	public Float getInternalRateReturn(PortfolioFund fund, BigDecimal cost) {

		LocalDate today = LocalDate.now();

		List<Double> prices = new LinkedList<>();

		// First value is cost
		prices.add(-cost.doubleValue());

		// Add remaining prices
		for (int i = 1; i < oldestDay; i++) {
			int day = oldestDay - i;
			BigDecimal value = getClosestHistoricalPrice(fund, today.minusDays(day - 1), 30);
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

	public LocalDate getClosestHistoricalDate(int trendDays) {
		LocalDate historicalDate = LocalDate.now().minusDays(trendDays);

		// Find the nearest date
		if (historicalDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
			trendDays--;
			historicalDate = historicalDate.minusDays(1);
		}
		if (historicalDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			trendDays--;
			historicalDate = historicalDate.minusDays(1);
		}

		Map<LocalDate, BigDecimal> fundPriceMap = portfolio.getPriceHistory().getFundPrices().get("VFIAX");
		BigDecimal value = fundPriceMap.get(historicalDate);
		if (value == null) {
			int tries = 30;
			while (tries-- > 0) {
				historicalDate = historicalDate.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(historicalDate);
				if (value != null) {
					return historicalDate;
				}
			}
		}
		return LocalDate.now().minusDays(trendDays);

	}

	public Double getTrendByYear(PortfolioFund fund, int trendYears) {

		LocalDate today = LocalDate.now();

		// Find the nearest date
		LocalDate date = today.minusYears(trendYears);
		BigDecimal historicalPrice = getPriceByDate(fund, date, true);
		if (historicalPrice == null) {
			historicalPrice = getClosestHistoricalPrice(fund, date, 90);
			if (historicalPrice == null) {
				return null;
			}
		}
		BigDecimal currentPrice = getPriceByDate(fund, today, true);
		return currentPrice.subtract(historicalPrice).divide(historicalPrice, CURRENCY_SCALE, RoundingMode.HALF_UP)
				.doubleValue();
	}

	private BigDecimal getClosestHistoricalPrice(PortfolioFund fund, LocalDate date, int days) {

		BigDecimal historicalValue = getPriceByDate(fund, date, true);
		if (historicalValue != null) {
			return historicalValue;
		}
		int tries = 0;
		while (tries++ < days) {

			historicalValue = getPriceByDate(fund, date.minusDays(tries), true);
			if (historicalValue != null) {
				return historicalValue;
			}
			historicalValue = getPriceByDate(fund, date.plusDays(tries), true);
			if (historicalValue != null) {
				return historicalValue;
			}
		}
		return null;
	}

	public BigDecimal getTotalValueByDate(Portfolio portfolio, LocalDate date) {
		BigDecimal totalValueByDate = null;
		totalValueByDate = portfolio.getFundMap().values().stream().map(f -> getValueByDate(f, date))
				.filter(x -> x != null)
				.reduce(new BigDecimal(0, MathContext.DECIMAL32), (total, fundValue) -> total = total.add(fundValue))
				.setScale(2, BigDecimal.ROUND_UP);
		return totalValueByDate;
	}

	/**
	 * @throws Exception
	 * 
	 */
	public void loadFundAllocation(Portfolio portfolio, String allocationFile) throws Exception {
		desiredFundAllocationMaps = new HashMap<>();

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

			if (allocationValues.size() > 12 && allocationValues.get(11).length() > 0) {
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

	}

	private void loadFundSymbolMap(String filename) {
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
	}

	public void saveHistoricalPrices(Portfolio portfolio, String filename) {
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
				StringBuilder fundStringBuilder = new StringBuilder(symbol).append("," + this.getFundName(symbol));
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

	public void saveHistoricalValue(Portfolio portfolio, String historicalValuesFile) {
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
				StringBuilder fundStringBuilder = new StringBuilder(symbol).append("," + this.getFundName(symbol));
				for (LocalDate date : dates) {
					BigDecimal fundValueByDate = getValueByDate(fund, date);
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
			// TODO Auto-generated catch block
			System.out.println("Exception:  " + e.getMessage());
		}
	}

	public void saveHistoricalShares(Portfolio portfolio, String historicalSharesFile) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(basePath + historicalSharesFile))) {
			StringBuilder headingLineStringBuilder = new StringBuilder("Symbol,Name");

			// Build ordered set of dates (some funds may have different set of
			// populated dates)
			Set<LocalDate> dates = new TreeSet<>();
			for (Map<LocalDate, Float> entry : portfolio.getPriceHistory().getFundShares().values()) {
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
				StringBuilder fundStringBuilder = new StringBuilder(symbol).append("," + this.getFundName(symbol));
				for (LocalDate date : dates) {
					Float fundSharesByDate = getSharesByDate(fund, date, false);
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
	public Map<String, BigDecimal> calculateAdjustments(Portfolio portfolio) throws Exception {
		// Map of <Symbol, Adjustment>
		Map<String, BigDecimal> adjustments = new TreeMap<>();

		BigDecimal totalValue = portfolio.getTotalValue();
		BigDecimal totalAdjustments = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentage = BigDecimal.ZERO;
		BigDecimal totalDesiredPercentage = BigDecimal.ZERO;

		for (PortfolioFund fund : getFundsSortedByDifferenceInValue(portfolio)) {
			BigDecimal currentFundValue = fund.getValue();
			BigDecimal currentPercentage = CurrencyHelper.calculatePercentage(currentFundValue, totalValue);
			totalCurrentPercentage = totalCurrentPercentage.add(currentPercentage);
			BigDecimal desiredValue = getDesiredFundValue(portfolio, fund.getSymbol());
			BigDecimal desiredPercentage = CurrencyHelper.calculatePercentage(desiredValue, totalValue);
			totalDesiredPercentage = totalDesiredPercentage.add(desiredPercentage);

			if (currentFundValue.equals(BigDecimal.ZERO) && desiredValue.equals(BigDecimal.ZERO)) {
				continue;
			}
			if (desiredValue == null) {
				throw new Exception("No desired value for fund:  " + fund.getShortName());
			}
			BigDecimal difference = currentFundValue.subtract(desiredValue);
			BigDecimal percentDifference = difference.divide(totalValue, 4, RoundingMode.HALF_DOWN);
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
			MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);

			adjustments.put(fund.getSymbol(), fundAdjustment);
			totalAdjustments = totalAdjustments.add(fundAdjustment);

		}
		return adjustments;

	}

	private Collection<PortfolioFund> getFundsSortedByDifferenceInValue(Portfolio portfolio) {
		return portfolio.getFundMap().values().stream().sorted(
				(f1, f2) -> getDifferenceInFundValue(portfolio, f2).compareTo(getDifferenceInFundValue(portfolio, f1)))
				.collect(Collectors.toList());

	}

	public BigDecimal getDifferenceInFundValue(Portfolio portfolio, PortfolioFund fund) {
		return fund.getValue().subtract(getDesiredFundValue(portfolio, fund.getSymbol()));
	}

	public BigDecimal getDesiredFundValue(Portfolio portfolio, String symbol) {
		BigDecimal fundTotalPercentage = null;
		try {
			PortfolioFund fund = portfolio.getFund(symbol);
			BigDecimal minAmount = fund.getMinimumAmount();
			BigDecimal currentValue = fund.getValue();
			if (minAmount != null && currentValue.compareTo(minAmount) < 0) {
				fundTotalPercentage = minAmount.divide(portfolio.getTotalValue(), 4, RoundingMode.UP);
			} else {
				fundTotalPercentage = desiredFundAllocationMaps.get(symbol).get(FundCategory.TOTAL);
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

	public void prinPerformanceLineGraphs(Portfolio portfolio, List<String> fundSynbols, Document document,
			PdfDocument pdfDocument, LocalDate startDate, LocalDate endDate) {

		// JFreeChart lineChart = ChartFactory.createTimeSeriesChart(null, null, null,
		// createFundPriceHistoryDataset(portfolio, fundSynbols, startDate, endDate),
		// true, false, false);
		List<TimeSeriesCollection> datasets = new ArrayList<>();
		datasets.add(createFundPriceHistoryDataset(portfolio, fundSynbols, startDate, endDate));
		List<XYItemRenderer> renderers = new ArrayList<>();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);
		JFreeChart lineChart = createTimeSeriesChart("Fund Price History", null, null, datasets, renderers, true, true,
				false);

		// TODO Attempt to configure stroke to be darker, doesn't seem to work....
		XYPlot plot = (XYPlot) lineChart.getPlot();
//		XYItemRenderer r = plot.getRenderer();
//		if (r instanceof XYLineAndShapeRenderer) {
//			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
//			renderer.setDefaultStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL), false);
//			renderer.setDefaultOutlineStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
//		}

		addChartToDocument(lineChart, pdfDocument, document);

	}

	private TimeSeriesCollection createFundDividendDataset(Portfolio portfolio2, List<String> fundSynbols,
			LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {
			PortfolioFund fund = portfolio.getFund(symbol);
			TimeSeries dividendTimeSeries = new TimeSeries(fund.getShortName() + " Dividends");

			LocalDate graphDate = startDate;
			if (graphDate == null) {
				graphDate = portfolio.getPriceHistory().getOldestDay();
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

	private void addChartToDocument(JFreeChart lineChart, PdfDocument pdfDocument, Document document) {
		// Draw the chart into a transition PDF
		PDFDocument doc = new PDFDocument();
		Rectangle bounds = new Rectangle(1200, 760);
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

	public void prinBalanceLineGraphs(Portfolio portfolio, Document document, PdfDocument pdfDocument,
			LocalDate startDate, LocalDate endDate) {

		List<XYItemRenderer> renderers = new ArrayList<>();
		List<TimeSeriesCollection> datasets = new ArrayList<>();
		datasets.add(createBalanceDataset(portfolio, startDate, endDate));
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);
		datasets.add(createDividendDataset(portfolio, startDate, endDate));
		renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(true);
		renderers.add(renderer);
		datasets.add(createWithdrawalDataset(portfolio, startDate, endDate));
		ClusteredXYBarRenderer barRenderer = new ClusteredXYBarRenderer();
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

	public static JFreeChart createTimeSeriesChart(String title, String timeAxisLabel, String valueAxisLabel,
			List<TimeSeriesCollection> datasets, List<XYItemRenderer> renderers, boolean legend, boolean tooltips,
			boolean urls) {

//		CategoryPlot plot = new CategoryPlot();
		XYPlot plot = new XYPlot();

		ValueAxis timeAxis = new DateAxis(timeAxisLabel);
		timeAxis.setLowerMargin(0.02); // reduce the default margins
		timeAxis.setUpperMargin(0.02);
		plot.setDomainAxis(timeAxis);

		for (int datasetIndex = 0; datasetIndex < datasets.size(); datasetIndex++) {
			TimeSeriesCollection timeSeriesCollection = datasets.get(datasetIndex);

			// MinMaxCategoryRenderer renderer = new MinMaxCategoryRenderer();
			XYItemRenderer renderer;
			if (renderers != null & renderers.size() > datasetIndex) {
				renderer = renderers.get(datasetIndex);
			} else {
				renderer = new XYLineAndShapeRenderer();
			}
			plot.setRenderer(datasetIndex, renderer);

			for (int seriesIndex = 0; seriesIndex < timeSeriesCollection.getSeries().size(); seriesIndex++) {
				TimeSeries series = timeSeriesCollection.getSeries(seriesIndex);
				String key = (String) series.getKey();
				java.awt.Color seriesColor = axisPaints[seriesIndex];
				if (key.contains("Dividends")) {
					renderer.setSeriesShape(seriesIndex, ShapeUtils.createDiamond(2f));
				} else if (key.contains("Withdrawals")) {
					renderer.setSeriesShape(seriesIndex, ShapeUtils.createDownTriangle(2f));
				} else if (key.contains("MA")) {
					seriesColor = axisPaints[seriesIndex - 1];
				}
				if (!key.contains("Withdrawals")) {
					renderer.setSeriesFillPaint(seriesIndex, seriesColor);
					renderer.setSeriesPaint(seriesIndex, seriesColor);
					renderer.setSeriesFillPaint(seriesIndex, seriesColor);
					renderer.setSeriesOutlinePaint(seriesIndex, seriesColor);
				}
			}

			plot.setDataset(datasetIndex, timeSeriesCollection);

			NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
			valueAxis.setAutoRangeIncludesZero(false); // override default

			plot.setRangeAxis(datasetIndex, valueAxis);

			// Map the data to the appropriate axis
			plot.mapDatasetToRangeAxis(datasetIndex, datasetIndex);

		}

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		// ChartFactory.getChartTheme().apply(chart);
		return chart;

	}

	public void printFundBalanceLineGraphs(Portfolio portfolio, String title, List<String> fundSynbols,
			Document document, PdfDocument pdfDocument, LocalDate startDate, LocalDate endDate) {

		List<XYItemRenderer> renderers = new ArrayList<>();
		List<TimeSeriesCollection> datasets = new ArrayList<>();
		datasets.add(createFundBalanceDataset(portfolio, fundSynbols, startDate, endDate));
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(false);
		renderers.add(renderer);
		datasets.add(createFundDividendDataset(portfolio, fundSynbols, startDate, endDate));
		renderer = new XYLineAndShapeRenderer();
		renderer.setDefaultShapesVisible(true);
		renderers.add(renderer);
		datasets.add(createFundWithdrawalDataset(portfolio, fundSynbols, startDate, endDate));
		XYBarRenderer barRenderer = new XYBarRenderer();
		renderer = new XYLineAndShapeRenderer();
		// renderer.setDefaultItemLabelsVisible(true);
		barRenderer.setShadowVisible(false);
		renderers.add(renderer);
		JFreeChart lineChart = createTimeSeriesChart(title, null, null, datasets, renderers, true, true, false);

//		JFreeChart lineChart = ChartFactory.createTimeSeriesChart(title, null, null,
//				createFundBalanceDataset(portfolio, fundSynbols, startDate, endDate), true, false, false);

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

	private TimeSeriesCollection createFundWithdrawalDataset(Portfolio portfolio2, List<String> fundSynbols,
			LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;
		TimeSeries timeSeries;
		for (String symbol : fundSynbols) {
			PortfolioFund fund = portfolio.getFund(symbol);

			LocalDate graphDate = startDate;
			if (graphDate == null) {
				graphDate = portfolio.getPriceHistory().getOldestDay();
			}
			if (endDate == null) {
				endDate = LocalDate.now();
			}
			timeSeries = new TimeSeries(fund.getShortName() + " Withdrawals");
			while (!graphDate.isAfter(endDate)) {
				final LocalDate date = graphDate;
				BigDecimal withdrawalsByDate = fund.getWithdrawalTotalForDate(date);
				if (withdrawalsByDate != null && withdrawalsByDate.compareTo(BigDecimal.ZERO) > 0) {
					cumulativeWithdrawals = cumulativeWithdrawals.subtract(withdrawalsByDate);
					timeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
							cumulativeWithdrawals);
//					timeSeries.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()),
//							withdrawalsByDate);

				}
				graphDate = graphDate.plusDays(1);
			}
			dataset.addSeries(timeSeries);
		}

		return dataset;
	}

	private TimeSeriesCollection createFundBalanceDataset(Portfolio portfolio, List<String> fundSynbols,
			LocalDate startDate, LocalDate endDate) {

		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {

			PortfolioFund fund = portfolio.getFund(symbol);
			LocalDate graphDate = startDate;
			if (graphDate == null) {
				graphDate = portfolio.getPriceHistory().getOldestDay();
			}
			if (endDate == null) {
				endDate = LocalDate.now();
			}
			TimeSeries timeSeries = new TimeSeries(fund.getShortName());
			while (!graphDate.isAfter(endDate)) {

				BigDecimal fundBalanceByDate = portfolio.getPriceHistory().getFundValueByDate(fund, graphDate, true);
				if (fundBalanceByDate != null && fundBalanceByDate.compareTo(BigDecimal.ZERO) > 0) {
					timeSeries.add(new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
							fundBalanceByDate);
				}
				graphDate = graphDate.plusDays(1);
			}
			dataset.addSeries(timeSeries);
		}
		return dataset;
	}

	private TimeSeriesCollection createFundPriceHistoryDataset(Portfolio portfolio, List<String> fundSynbols,
			LocalDate startDate, LocalDate endDate) {

		TimeSeriesCollection dataset = new TimeSeriesCollection();

		for (String symbol : fundSynbols) {
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
			dataset.addSeries(MovingAverage.createMovingAverage(timeSeries, "MA", 90, 0));

		}
		return dataset;
	}

	private List<TimeSeriesCollection> createFundPriceHistoryDatasets(Portfolio portfolio, List<String> fundSynbols,
			LocalDate startDate, LocalDate endDate) {

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

	private TimeSeriesCollection createBalanceDataset(Portfolio portfolio, LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		TimeSeries timeSeries = new TimeSeries("Portfolio Balance");

		if (startDate == null) {
			startDate = portfolio.getPriceHistory().getOldestDay();
		}
		if (endDate == null) {
			endDate = LocalDate.now();
		}
		LocalDate graphDate = startDate;
		while (!graphDate.isBefore(startDate) && !graphDate.isAfter(endDate)) {
			BigDecimal totalByDate = getTotalValueByDate(portfolio, graphDate);
			if (totalByDate != null && totalByDate.compareTo(BigDecimal.ZERO) > 0) {
				timeSeries.add(new Day(graphDate.getDayOfMonth(), graphDate.getMonthValue(), graphDate.getYear()),
						totalByDate);

			}
			graphDate = graphDate.plusDays(1);
		}
		dataset.addSeries(timeSeries);

		return dataset;
	}

	private TimeSeriesCollection createDividendDataset(Portfolio portfolio, LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		TimeSeries dividendTimeSeries = new TimeSeries("Dividends");
		BigDecimal cumulativeDividends = BigDecimal.ZERO;
		if (startDate == null) {
			startDate = portfolio.getPriceHistory().getOldestDay();
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

	private TimeSeriesCollection createWithdrawalDataset(Portfolio portfolio, LocalDate startDate, LocalDate endDate) {
		TimeSeriesCollection dataset = new TimeSeriesCollection();

		TimeSeries withdrawalTimeSeries = new TimeSeries("Withdrawals");
		BigDecimal cumulativeWithdrawals = BigDecimal.ZERO;
		if (startDate == null) {
			startDate = portfolio.getPriceHistory().getOldestDay();
		}
		if (endDate == null) {
			endDate = LocalDate.now();
		}
		LocalDate graphDate = startDate;
		while (!graphDate.isBefore(startDate) && !graphDate.isAfter(endDate)) {
			final LocalDate date = graphDate;
			BigDecimal withdrawalsByDate = portfolio.getFundMap().values().stream()
					.map(f -> f.getWithdrawalTotalForDate(date)).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (withdrawalsByDate != null && withdrawalsByDate.compareTo(BigDecimal.ZERO) > 0) {
				cumulativeWithdrawals = cumulativeWithdrawals.subtract(withdrawalsByDate);
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

	public void printPerformanceTable(Portfolio portfolio, Document document) {
		// Creating a table object
		float[] pointColumnWidths = { 15F, 2F, 4F, 4F, 5F, 5F, 5F, 5F, 5F, 25F, 5F, 5F, 5F, 5F, 5F, 5F, 5F, 5F };
		Table table = new Table(pointColumnWidths);

		table.setFontSize(12);
		table.setTextAlignment(TextAlignment.RIGHT);

		// Print table headings
		table.addHeaderCell(new Cell().add("Fund (report run at " + LocalDateTime.now().format(TIME_FORMATTER) + ")")
				.setTextAlignment(TextAlignment.LEFT));
		table.addHeaderCell(new Cell().add("%"));
		table.addHeaderCell(new Cell().add("High Share Price/\n1yr High Price").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Low Share Price/\n1yr Low Price").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Begin 2022\nPrice").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Day % Change").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("YTD % Change / 1 yr / 3 yr annualized / 5 yr ann.")
				.setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("YTD Dividends").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(
				new Cell().add("Last Year Dividends").setTextAlignment(TextAlignment.CENTER).setFontSize(12f));
		table.addHeaderCell(new Cell().add("YTD\nReturns /\nWithd. /\nExch.").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Share Price\n" + mostRecentSharePrice.format(DATE_FORMATTER))
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
		for (PortfolioFund fund : this.getFundsByCategory(portfolio, FundCategory.CASH)) {
			addFundsToTable(portfolio, fund, table, FundCategory.CASH);
		}
		addCategoryTotalsToTable(portfolio, table, FundCategory.CASH);

		// Bond Funds
		table.addCell(new Cell().add("Bond Funds").setTextAlignment(TextAlignment.LEFT).setItalic());
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell());
		for (PortfolioFund fund : this.getFundsByCategory(portfolio, FundCategory.BOND)) {
			addFundsToTable(portfolio, fund, table, FundCategory.BOND);
		}
		addCategoryTotalsToTable(portfolio, table, FundCategory.BOND);

		// Stock Funds
		table.addCell(
				new Cell().add("Stock Funds").setTextAlignment(TextAlignment.LEFT).setKeepWithNext(true).setItalic());
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell());
		for (PortfolioFund fund : getFundsByCategory(portfolio, FundCategory.STOCK)) {
			addFundsToTable(portfolio, fund, table, FundCategory.STOCK);
		}
		addCategoryTotalsToTable(portfolio, table, FundCategory.STOCK);

		// Intl Funds
		table.addCell(new Cell().add("Intl Funds").setTextAlignment(TextAlignment.LEFT).setItalic());
		table.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell()).addCell(new Cell())
				.addCell(new Cell()).addCell(new Cell());
		for (PortfolioFund fund : this.getFundsByCategory(portfolio, FundCategory.INTL)) {
			addFundsToTable(portfolio, fund, table, FundCategory.INTL);
		}
		addCategoryTotalsToTable(portfolio, table, FundCategory.INTL);

		addTotalsToTable(portfolio, table);

		document.add(table);
		document.add(new AreaBreak());

	}

	public void printWithdrawalSpreadsheet(Portfolio portfolio, BigDecimal netWithdrawalAmount,
			Map<String, BigDecimal> withdrawals, Document document) {
		// Creating a table object
		float[] pointColumnWidths = { 8F, 5F, 5F, 5F, 5F, 5F, 10F, 5F, 5F, 5F, 5F, 5F, 5F };
		Table table = new Table(pointColumnWidths);

		table.setFontSize(14);
		table.setTextAlignment(TextAlignment.RIGHT);

		// Print table headings
		table.addHeaderCell(new Cell().add("Fund"));
		table.addHeaderCell(new Cell().add("Current Value\nCategory\n Total").setTextAlignment(TextAlignment.CENTER));
		table.addHeaderCell(new Cell().add("Current\n%").setTextAlignment(TextAlignment.CENTER));
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
		for (PortfolioFund fund : this.getFundsByCategory(portfolio, FundCategory.CASH)) {
			addFundsToWithdrawalTable(portfolio, fund, table, FundCategory.CASH, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(portfolio, table, FundCategory.CASH, withdrawals);

		table.addCell(new Cell().add("Bond Funds").setBold());
		table.startNewRow();
		for (PortfolioFund fund : this.getFundsByCategory(portfolio, FundCategory.BOND)) {
			addFundsToWithdrawalTable(portfolio, fund, table, FundCategory.BOND, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(portfolio, table, FundCategory.BOND, withdrawals);

		table.startNewRow();
		table.addCell(new Cell().add("Stock Funds").setKeepWithNext(true).setBold());
		table.startNewRow();
		for (PortfolioFund fund : getFundsByCategory(portfolio, FundCategory.STOCK)) {
			addFundsToWithdrawalTable(portfolio, fund, table, FundCategory.STOCK, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(portfolio, table, FundCategory.STOCK, withdrawals);

		table.startNewRow();
		table.addCell(new Cell().add("Intl Funds").setBold());
		table.startNewRow();
		for (PortfolioFund fund : this.getFundsByCategory(portfolio, FundCategory.INTL)) {
			addFundsToWithdrawalTable(portfolio, fund, table, FundCategory.INTL, netWithdrawalAmount, withdrawals);
		}
		addCategoryTotalsToWithdrawalTable(portfolio, table, FundCategory.INTL, withdrawals);

		addTotalsToWithdrawalTable(portfolio, table, withdrawals);

		document.add(table);
		document.add(new AreaBreak());

	}

	private void addFundsToWithdrawalTable(Portfolio portfolio, PortfolioFund fund, Table table, FundCategory category,
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
		BigDecimal deviation = currentPercentage.subtract(fundTotalTargetPercentage);

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

		// Deviation / Adjusted Deviation for Minimums
		table.addCell(createDeviationCell(fund.isFixedExpensesAccount(), deviationByCategory, targetValueByCategory,
				targetValue, deviation, minimumAdjustedDeviation));

		// Surplus / Deficit
		table.addCell(
				createSurplusDeficitCell(fund.isFixedExpensesAccount(), surplusDeficitByCategory, deviationByCategory,
						surpusDeficit, deviation, minimumAdjustedSurplusDeficit, minimumAdjustedDeviation));

		if (withdrawalPerCategoryAmount.compareTo(withdrawalAmount) == 0) {
			table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(withdrawalPerCategoryAmount)));
		} else {
			table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(withdrawalPerCategoryAmount) + "\n"
					+ CurrencyHelper.formatAsCurrencyString(withdrawalAmount)));
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

	private void addCategoryTotalsToWithdrawalTable(Portfolio portfolio, Table table, FundCategory category,
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

		for (PortfolioFund fund : getFundsByCategory(portfolio, category)) {
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
					.add(fund.getValue().subtract(getHistoricalValue(fund, getYtdDays())));

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
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalWithdrawalsByCategory)));

		// Post Withdrwal Value
		table.addCell(new Cell()
				.add(CurrencyHelper.formatAsCurrencyString(totalCurrentValue.subtract(totalWithdrawalsByCategory))));

		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
	}

	private void addTotalsToTable(Portfolio portfolio, Table table) {

		BigDecimal totalDividends = BigDecimal.ZERO;
		BigDecimal totalLastYearDividends = BigDecimal.ZERO;
		BigDecimal totalCurrentPercentage = BigDecimal.ZERO;
		BigDecimal totalTargetPercentage = BigDecimal.ZERO;
		BigDecimal total = portfolio.getTotalValue();
		BigDecimal totalYtdValueChange = BigDecimal.ZERO;
		BigDecimal totalYtdWithdrawals = BigDecimal.ZERO;

		for (PortfolioFund fund : portfolio.getFundMap().values()) {
			totalYtdWithdrawals = totalYtdWithdrawals.add(fund.getWithdrawalsUpToDate(getFirstOfYearDate()));
			totalDividends = totalDividends.add(fund.getDistributionsAfterDate(getFirstOfYearDate()));
			totalLastYearDividends = totalLastYearDividends.add(fund.getDistributionsBetweenDates(
					getFirstOfYearDate().minus(1, ChronoUnit.YEARS), getFirstOfYearDate().minus(1, ChronoUnit.DAYS)));
			totalYtdValueChange = totalYtdValueChange
					.add(fund.getValue().subtract(getHistoricalValue(fund, getYtdDays())));
			totalCurrentPercentage = totalCurrentPercentage
					.add(CurrencyHelper.calculatePercentage(fund.getValue(), portfolio.getTotalValue()));
			totalTargetPercentage = totalTargetPercentage.add(fund.getPercentageByCategory(FundCategory.TOTAL));

		}
		BigDecimal totalDeviation = totalTargetPercentage.subtract(totalCurrentPercentage);
		BigDecimal totalTargetValue = total.multiply(totalTargetPercentage);
		BigDecimal totalSurplusDeficit = totalTargetValue.subtract(total);

		table.addCell(new Cell().add("Grand Total").setBold());
		table.addCell(new Cell().add(" ")); // TBD
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalDividends)).setFontSize(12f));
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalLastYearDividends)).setFontSize(12f));

		// YTD Returns (withdrawals)
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(totalYtdValueChange))
						.setFontColor(calculateCurrencyFontColor(totalYtdValueChange)))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(totalYtdWithdrawals))
//						.setFontColor(ytdWithdrawalsFontColor)
				));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(total)));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
		table.addCell(new Cell().add(""));
	}

	private void addTotalsToWithdrawalTable(Portfolio portfolio, Table table, Map<String, BigDecimal> withdrawals) {

		BigDecimal totalCurrentValue = BigDecimal.ZERO;
		BigDecimal total = portfolio.getTotalValue();
		BigDecimal totalYtdValueChange = BigDecimal.ZERO;
		BigDecimal totalWithdrawals = BigDecimal.ZERO;

		for (PortfolioFund fund : portfolio.getFundMap().values()) {
			totalCurrentValue = totalCurrentValue.add(fund.getValue());
			totalYtdValueChange = totalYtdValueChange
					.add(fund.getValue().subtract(getHistoricalValue(fund, getYtdDays())));

		}

		for (BigDecimal withdrawal : withdrawals.values()) {
			totalWithdrawals = totalWithdrawals.add(withdrawal);
		}

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
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(totalWithdrawals)));

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

	private void addFundsToTable(Portfolio portfolio2, PortfolioFund fund, Table table, FundCategory category) {

		PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
		MutualFundPerformance performance = new MutualFundPerformance(portfolio, fund);
		LocalDate fiftyTwoWeeksAgo = LocalDate.now().minus(1, ChronoUnit.YEARS);
		LocalDate threeYearsAgo = LocalDate.now().minus(3, ChronoUnit.YEARS);
		LocalDate fiveYearsAgo = LocalDate.now().minus(5, ChronoUnit.YEARS);

		BigDecimal currentPrice = fund.getCurrentPrice();
		BigDecimal dayPriceChange = performance.getDayPriceChange();

		Float fiftyTwoWeekPriceChange = performance.getPerformanceRateByDate(fiftyTwoWeeksAgo);
		Float threeYearPriceChange = performance.getPerformanceRateByDate(threeYearsAgo);
		Float annualizedThreeYearPriceChange = 0f;
		if (threeYearPriceChange != null) {
			annualizedThreeYearPriceChange = threeYearPriceChange / 3;
		}
		Float fiveYearPriceChange = performance.getPerformanceRateByDate(fiveYearsAgo);
		Float annualizedFiveYearPriceChange = 0f;
		if (threeYearPriceChange != null) {
			annualizedFiveYearPriceChange = fiveYearPriceChange / 10;
		}
		BigDecimal ytdPriceChange = performance.getYtdPriceChange();
		// Does this include exchanges and withdrawals?
		BigDecimal ytdValueChange = performance.getYtdValueChange();
		BigDecimal ytdWithdrawals = fund.getWithdrawalsUpToDate(getFirstOfYearDate());
		Color ytdWithdrawalsFontColor = ytdWithdrawals.compareTo(BigDecimal.ZERO) > 0 ? Color.RED : Color.BLACK;

		BigDecimal ytdExchanges = BigDecimal.ZERO.subtract(fund.getExchangeTotalFromDate(getFirstOfYearDate()));
		Color ytdExchangesFontColor = ytdExchanges.compareTo(BigDecimal.ZERO) < 0 ? Color.RED : Color.GREEN;

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
		BigDecimal ytdDividends = fund.getDistributionsAfterDate(getFirstOfYearDate());
		BigDecimal lastYearDividends = fund.getDistributionsBetweenDates(
				getFirstOfYearDate().minus(1, ChronoUnit.YEARS), getFirstOfYearDate().minus(1, ChronoUnit.DAYS));

		// Min/max prices
		LocalDate oldestDate = LocalDate.now().minus(oldestDay, ChronoUnit.DAYS);
		Pair<LocalDate, BigDecimal> maxPricePair = priceHistory.getMaxPriceFromDate(fund, oldestDate);
		Pair<LocalDate, BigDecimal> minPricePair = priceHistory.getMinPriceFromDate(fund, oldestDate);
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
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(maxPricePair.getRight()) + "\n"
								+ maxPricePair.getLeft().format(DATE_FORMATTER))
						.setFontColor(maxPriceFontColor))
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(maxPrice1YRPair.getRight()) + "\n"
								+ maxPrice1YRPair.getLeft().format(DATE_FORMATTER))
						.setFontColor(maxPrice1YRFontColor).setFontSize(12)));

		// 1YR Min Price
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(minPricePair.getRight()) + "\n"
								+ minPricePair.getLeft().format(DATE_FORMATTER))
						.setFontColor(minPriceFontColor))
				.add(new Cell().setMargin(0f)
						.add(CurrencyHelper.formatAsCurrencyString(minPrice1YRPair.getRight()) + "\n"
								+ minPrice1YRPair.getLeft().format(DATE_FORMATTER))
						.setFontColor(minPrice1YRFontColor).setFontSize(12)));

		// Begin Year Price
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(performance.getFirstOfYearPrice())));

		// Day Price Change
		table.addCell(new Cell().add(CurrencyHelper.formatPercentageString(dayPriceChange)).setBackgroundColor(
				calculatePercentageFontColor(dayPriceChange),
				dayPriceChange.multiply(new BigDecimal(100)).abs().floatValue()));

		// YTD / 1 year / 3 yr annualized Price Change
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().add(CurrencyHelper.formatPercentageString(ytdPriceChange)).setBackgroundColor(
						calculatePercentageFontColor(ytdPriceChange),
						ytdPriceChange.multiply(new BigDecimal(10)).abs().floatValue()))
				.add(new Cell().add(CurrencyHelper.formatPercentageString(fiftyTwoWeekPriceChange)).setBackgroundColor(
						calculatePercentageFontColor(new BigDecimal(fiftyTwoWeekPriceChange)),
						new BigDecimal(fiftyTwoWeekPriceChange * 10f).abs().floatValue()))
				.add(new Cell().add(CurrencyHelper.formatPercentageString(annualizedThreeYearPriceChange))
						.setBackgroundColor(
								calculatePercentageFontColor(new BigDecimal(annualizedThreeYearPriceChange)),
								new BigDecimal(annualizedThreeYearPriceChange * 10f).abs().floatValue()))
				.add(new Cell().add(CurrencyHelper.formatPercentageString(annualizedFiveYearPriceChange))
						.setBackgroundColor(calculatePercentageFontColor(new BigDecimal(annualizedFiveYearPriceChange)),
								new BigDecimal(annualizedFiveYearPriceChange * 10f).abs().floatValue())));

		// YTD Dividends
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(ytdDividends)).setFontSize(12f));

		// Last Year Dividends
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(lastYearDividends)).setFontSize(12f));

		// YTD Returns (withdrawals and exchanges)
		table.addCell(new Cell().setMargin(0f)
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdValueChange))
						.setFontColor(calculateCurrencyFontColor(ytdValueChange)))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdWithdrawals))
						.setFontColor(ytdWithdrawalsFontColor))
				.add(new Cell().setMargin(0f).add(CurrencyHelper.formatAsCurrencyString(ytdExchanges))
						.setFontColor(ytdExchangesFontColor)))
				.setFontSize(12);

		// Current share price
		table.addCell(new Cell().add(CurrencyHelper.formatAsCurrencyString(currentPrice))
				.setFontColor(currentPriceFontColor));

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
				.add(isFixedExpensesAccount ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(surpusDeficitByCategory))
								.setBackgroundColor(calculateCurrencyFontColor(deviationByCategory),
										deviationByCategory.abs().multiply(new BigDecimal(100))
												.multiply(new BigDecimal(2)).floatValue()))
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
				.add(isFixedExpensesAccount ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString3(deviationByCategory))
								.setBackgroundColor(calculateCurrencyFontColor(deviationByCategory),
										deviationByCategory.abs().multiply(new BigDecimal(100))
												.multiply(new BigDecimal(2)).floatValue()))
				.add(targetValue.compareTo(targetValueByCategory) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString3(deviation)).setBackgroundColor(
								calculateCurrencyFontColor(deviation),
								deviation.abs().multiply(new BigDecimal(100)).multiply(new BigDecimal(2)).floatValue()))
				.add(adjustedMinimumDeviation == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString3(adjustedMinimumDeviation))
								.setBackgroundColor(calculateCurrencyFontColor(adjustedMinimumDeviation),
										adjustedMinimumDeviation.abs().multiply(new BigDecimal(100))
												.multiply(new BigDecimal(2)).floatValue()));
		return cell;
	}

	private Cell createTargetValueCell(Boolean isFixedExpensesAccount, BigDecimal targetValueByCategory,
			BigDecimal targetValue, BigDecimal adjustedMinimumTargetValue) {
		Cell cell = new Cell()
				.add(isFixedExpensesAccount ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(targetValueByCategory)))
				.add(targetValue.compareTo(targetValueByCategory) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(targetValue)))
				.add(adjustedMinimumTargetValue == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(adjustedMinimumTargetValue)));
		return cell;
	}

	private Cell createTargetPercentageCell(Boolean isFixedExpensesAccount, BigDecimal targetCategoryPercentage,
			BigDecimal fundTotalPercentage, BigDecimal adjustedMinimumTargetPerentage) {
		Cell cell = new Cell()
				.add(isFixedExpensesAccount ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(targetCategoryPercentage)))
				.add(fundTotalPercentage.compareTo(targetCategoryPercentage) == 0 ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(fundTotalPercentage)))
				.add(adjustedMinimumTargetPerentage == null ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(adjustedMinimumTargetPerentage)));
		return cell;
	}

	private Cell createCurrentPercentageCell(Boolean isFixedExpensesAccount, BigDecimal currentPercentageByCategory,
			BigDecimal targetCategoryPercentage, BigDecimal fundTotalPercentage,
			BigDecimal adjustedMinimumTargetPerentage, BigDecimal currentPercentage) {

		Cell cell = new Cell()
				.add(isFixedExpensesAccount ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatPercentageString(currentPercentageByCategory)))
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
				.add(isFixedExpensesAccount ? new Cell().add("n/a")
						: new Cell().add(CurrencyHelper.formatAsCurrencyString(currentValueByCategory))
								.setBackgroundColor(calculateCurrencyFontColor(deviationByCategory),
										deviationByCategory.abs().multiply(new BigDecimal(100))
												.multiply(new BigDecimal(2)).floatValue()))
				.add(currentValue.subtract(currentValueByCategory).abs().compareTo(new BigDecimal(.01)) <= 0
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

	private int getYtdDays() {
		Period period = getFirstOfYearDate().until(LocalDate.now());
		Integer ytdDays = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
		return ytdDays;
	}

	private Color calculatePercentageFontColor(BigDecimal value) {
		Color fontColor = Color.BLACK;
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			fontColor = Color.RED;
		} else {
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

	public void loadPortfolioFile(Portfolio portfolio, LocalDate date, String filename) {

		try {
			portfolio.getPriceHistory().loadPortfolioFile(portfolio, date, filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void addCategoryTotalsToTable(Portfolio portfolio, Table table, FundCategory category) {

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
		for (PortfolioFund fund : getFundsByCategory(portfolio, category)) {
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

		BigDecimal totalDeviation = totalCurrentPercentage.subtract(totalTargetPercentage);
		BigDecimal totalAdjustedMinimumDeviation = totalCurrentPercentage
				.subtract(totalAdjustedMinimumTargetPercentage);
		BigDecimal totalDeviationByCategory = totalCurrentPercentageByCategory
				.subtract(totalTargetPercentageByCategory);
		BigDecimal totalSurplusDeficit = totalCurrentValue.subtract(totalTargetValue);
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

	}

}
