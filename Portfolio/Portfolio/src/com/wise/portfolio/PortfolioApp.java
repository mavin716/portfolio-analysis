package com.wise.portfolio;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.wise.portfolio.alphaVantage.AlphaVantageFundPriceService;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.pdf.FooterHandler;
import com.wise.portfolio.pdf.HeaderHandler;
import com.wise.portfolio.portfolio.ManagedPortfolio;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.portfolio.PortfolioTransaction;
import com.wise.portfolio.service.CurrencyHelper;
import com.wise.portfolio.service.MailService;
import com.wise.portfolio.service.PortfolioService;

public class PortfolioApp {

	// TODO need configuration file
	private static final String DOWNLOAD_PATH = "C:\\Users\\mavin\\Downloads\\";
//	private static final String CURRENT_DOWNLOAD_FILE_PATH = "C:\\Users\\mavin\\My Drive\\";
	private static final String CURRENT_DOWNLOAD_FILE_PATH = "C:\\Users\\mavin\\Downloads\\";

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");

	private static final String DOWNLOAD_FILENAME_PREFIX = "ofxdownload";
	private static final String CURRENT_DOWNLOAD_FILE = CURRENT_DOWNLOAD_FILE_PATH + DOWNLOAD_FILENAME_PREFIX + ".csv";
	private static final String ALLOCATION_FILE = DOWNLOAD_PATH + "allocation.csv";
	private static final String SCHEDULE_FILE = DOWNLOAD_PATH + "Schedule.csv";
	private static final String ALPHA_VANTAGE_PRICE_HISTORY_FILENAME = "alphaVantagePriceHistory.csv";

	private static final String FUND_SYMBOLS_MAP_FILE = "allocation.csv";
	private static final String PORTFOLIO_PDF_FILE = "C:\\Users\\mavin\\Documents\\portfolio.pdf";

	private static final BigDecimal FEDERAL_WITHOLD_TAXES_PERCENT = new BigDecimal(.12);
	private static final BigDecimal STATE_WITHOLD_TAXES_PERCENT = new BigDecimal(.03);
	private static final BigDecimal WITHOLD_TAXES_PERCENT = FEDERAL_WITHOLD_TAXES_PERCENT
			.add(STATE_WITHOLD_TAXES_PERCENT);
	private static final BigDecimal AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE = BigDecimal.ONE
			.subtract(WITHOLD_TAXES_PERCENT);

	private static final int RECENT_TRANSACTIONS_DAYS = 30;

	private static final long PORTFOLIO_TRANSACTION_REPORT_WINDOW = 5;

	public static void main(String[] args) {

		PortfolioApp app = new PortfolioApp();
		app.run();

	}

	public PortfolioApp() {
		super();
	}

	private PortfolioService portfolioService;
	private ManagedPortfolio portfolio;

	public File run() {

		File portfolioPdfFile = null;
		try {
			// Create the portfolio service
			portfolioService = new PortfolioService(DOWNLOAD_PATH);

			// Create the portfolio
			portfolio = portfolioService.createPortfolio(FUND_SYMBOLS_MAP_FILE);

			// Load history download files
			portfolioService.loadPortfolioDownloadFiles(portfolio, DOWNLOAD_FILENAME_PREFIX, CURRENT_DOWNLOAD_FILE);
			// Used to change naming policy
			// portfolioService.updateDownloadFilenames(portfolio,
			// DOWNLOAD_FILENAME_PREFIX);

			System.out.println("Load portfolio allocation");
			portfolioService.loadFundAllocation(ALLOCATION_FILE);

			// Load price history via alphaVantage
			// randomize order of funds because the number exceeds daily quota
			portfolio.getFunds().parallelStream().filter(f -> !f.isClosed() && !f.isMMFund()).forEach(f -> {
				try {
					AlphaVantageFundPriceService.retrieveFundHistoryFromAlphaVantage(portfolio, f.getSymbol(), false);
				} catch (IOException e) {
					System.out.println("Exception:  " + e.getMessage());
					e.printStackTrace();
				}
			});

			System.out.println("Loading Alpha Vantage Price History File");
			portfolio.getPriceHistory().loadAlphaPriceHistoryFile(portfolio, DOWNLOAD_PATH,
					ALPHA_VANTAGE_PRICE_HISTORY_FILENAME);

			System.out.println("Load portfolio schedule");
			portfolioService.loadPortfolioScheduleFile(SCHEDULE_FILE);

			System.out.println("Save Portfolio Data");
			portfolioService.savePortfolioData();

			portfolioService.setFundColors();

			// Overwrite PDF output file
			portfolioPdfFile = new File(PORTFOLIO_PDF_FILE);
			portfolioPdfFile.delete();

			PdfWriter writer = new PdfWriter(portfolioPdfFile);
			PdfDocument pdfDoc = new PdfDocument(writer);

			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(30f, 10f, 30f, 10f);

			document.add(new Paragraph(generateReportTitle()).setFontSize(14)
					.setHorizontalAlignment(HorizontalAlignment.CENTER));
			
			HeaderHandler headerHandler = new HeaderHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
			FooterHandler footerHandler = new FooterHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

			// Process scheduled portfolio transactions
			processScheduledPortfolioTransactions(document);

			System.out.println("Print performance table");
			headerHandler.setHeader("performance table");
			footerHandler.setFooter("* 10 yr return doesn't include income");
			pdfDoc.addNewPage();
			portfolioService.printPerformanceTable(document);

			headerHandler.setHeader("portfolio performance table");
			footerHandler.setFooter("");
			pdfDoc.addNewPage();
			portfolioService.printPortfolioPerformanceTable(document);

			headerHandler.setHeader("Scheduled Transactions");
			pdfDoc.addNewPage();
			portfolioService.printScheduledTransactionsSpreadsheet("Scheduled Transactions",
					portfolio.getPortfolioTransactions(), document);

			portfolioService.printRecentTransactionsSpreadsheet(
					"Recent Transactions (" + RECENT_TRANSACTIONS_DAYS + " days)", RECENT_TRANSACTIONS_DAYS, portfolio,
					document);

			portfolioService.printYTDDistributionsSpreadsheet("YTD Withdrawals", portfolio, document,
					PortfolioService.getYtdDays());
			portfolioService.printFutureWithdrawalsSpreadsheet("Future Withdrawals", portfolio, document,
					FEDERAL_WITHOLD_TAXES_PERCENT, STATE_WITHOLD_TAXES_PERCENT);

			// Add price performance graphs,
			System.out.println("Print graphs");
			headerHandler.setHeader("balance line graph");
			pdfDoc.addNewPage();
			LocalDate startDate = LocalDate.now().minusYears(5);
			startDate = startDate.withDayOfMonth(1);
			portfolioService.printBalanceLineGraphs(document, pdfDoc, startDate, LocalDate.now(), Period.ofMonths(3));

			pdfDoc.addNewPage();
			startDate = LocalDate.now().minusYears(1);
			startDate = startDate.withDayOfMonth(1);
			portfolioService.printBalanceLineGraphs(document, pdfDoc, startDate, LocalDate.now(), Period.ofMonths(1));

			pdfDoc.addNewPage();
			startDate = LocalDate.now().minusWeeks(2);
			portfolioService.printBalanceLineGraphs(document, pdfDoc, startDate, LocalDate.now(), Period.ofDays(1));

			pdfDoc.addNewPage();
			portfolioService.printProjectedBalanceLineGraphs(document, pdfDoc, Period.ofMonths(1));

			pdfDoc.addNewPage();
			for (PortfolioFund fund : portfolio.getFunds()) {
				if (fund.isClosed() || fund.getShares() == 0)
					continue;
				portfolioService.printFundPerformanceLineGraph(fund.getSymbol(), document, pdfDoc,
						LocalDate.now().minusYears(5), LocalDate.now());
			}
			// Separate by current price (e.g., > 200, 100 - 200, 50 = 100, under 50
			pdfDoc.addNewPage();

			startDate = LocalDate.now().minusYears(10);
			LocalDate endDate = LocalDate.now();
			List<String> fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(200)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $200", fundSynbols, document, pdfDoc, startDate,
					endDate, false);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(200)) < 0 && maxPrice.compareTo(new BigDecimal(150)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $150 and < $200", fundSynbols, document, pdfDoc,
					startDate, endDate, false);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(150)) < 0 && maxPrice.compareTo(new BigDecimal(100)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $100 and < $150", fundSynbols, document, pdfDoc,
					startDate, endDate, false);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(100)) < 0 && maxPrice.compareTo(new BigDecimal(50)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $50 and < $100", fundSynbols, document, pdfDoc,
					startDate, endDate, false);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(50)) < 0 && maxPrice.compareTo(new BigDecimal(30)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $30 and < $50", fundSynbols, document, pdfDoc,
					startDate, endDate, false);
			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(30)) < 0 && maxPrice.compareTo(new BigDecimal(15)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $15 and < $30", fundSynbols, document, pdfDoc,
					startDate, endDate, false);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFunds()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(15)) < 0 && maxPrice.compareTo(new BigDecimal(1)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs("fund price > $1 and < $15", fundSynbols, document, pdfDoc,
					startDate, endDate, false);

			// portfolioService.printTrends(portfolio);

			// Print ranking
			System.out.println("Print ranking tables");
			headerHandler.setHeader("Ranking");
			pdfDoc.addNewPage();
			portfolioService.printRanking(document);

			Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments();
			pdfDoc.addNewPage();
			String title = "Adjust portfolio";
			portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, BigDecimal.ZERO, adjustments,
					document);

			SimpleDateFormat fileNameFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSS");
			// move latest file to dated file to make way for next download
			String newFileName = DOWNLOAD_FILENAME_PREFIX + " - "
					+ fileNameFormatter.format(new Date(System.currentTimeMillis())) + ".csv";
			if (Files.exists(Paths.get(CURRENT_DOWNLOAD_FILE)) && !Files.exists(Paths.get(newFileName))) {
				Files.move(Paths.get(CURRENT_DOWNLOAD_FILE), Paths.get(DOWNLOAD_PATH, newFileName));
			}

			// Closing the document
			document.close();
			System.out.println("PDF Created");

			String subject = "YEAH";
			BigDecimal difference = getBalanceDifference();
			if (difference.compareTo(BigDecimal.ZERO) < 0) {
				subject = "NOOO";
			} else if (difference.compareTo(BigDecimal.ZERO) == 0) {
				subject = "SAME";
			}
			String textBody = "Change:  " + NumberFormat.getCurrencyInstance().format(difference) + " Total:  "
					+ NumberFormat.getCurrencyInstance().format(portfolio.getTotalValue());
			MailService.sendMail(subject, textBody, portfolioPdfFile);

		} catch (Exception e) {
			System.out.println("Exception e: " + e.getMessage());
			e.printStackTrace();
		}
		return portfolioPdfFile;

	}

	private String generateReportTitle() {

		BigDecimal difference = getBalanceDifference();
		String changeText = "Change:  " + NumberFormat.getCurrencyInstance().format(difference) + " Total:  "
				+ NumberFormat.getCurrencyInstance().format(portfolio.getTotalValue());

		String title = "Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy")) + " "
				+ LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) + " " + changeText;
		return title;
	}

	private BigDecimal getBalanceDifference() {
		LocalDate lastBusinessDay = LocalDate.now();
		if (LocalTime.now().getHour() < 18) {
			lastBusinessDay = lastBusinessDay.minusDays(1);
		}
		if (lastBusinessDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
			lastBusinessDay = lastBusinessDay.minusDays(1);
		}
		if (lastBusinessDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
			lastBusinessDay = lastBusinessDay.minusDays(1);
		}
		LocalDate previousBusinessDay = lastBusinessDay.minusDays(1);
		if (previousBusinessDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
			previousBusinessDay = previousBusinessDay.minusDays(1);
		}
		if (previousBusinessDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
			previousBusinessDay = previousBusinessDay.minusDays(1);
		}

		BigDecimal currentTotalValue = portfolio.getTotalValueByDate(lastBusinessDay);
		BigDecimal previousTotalValue = portfolio.getTotalValueByDate(previousBusinessDay);
		BigDecimal difference = currentTotalValue.subtract(previousTotalValue);
		return difference;
	}

	private void processScheduledPortfolioTransactions(Document document) {

		// Aggregate transfers to allow multiple transactions in one day
		List<PortfolioTransaction> transferTransactions = new ArrayList<>();
		List<PortfolioTransaction> withdrawTransactions = new ArrayList<>();
		LocalDate today = LocalDate.now();
		LocalDate withdrawDate = today;
		List<PortfolioTransaction> transactions = portfolio.getPortfolioTransactions();
		List<PortfolioTransaction> updatedTransactions = new ArrayList<>();
		for (PortfolioTransaction transaction : transactions) {
			LocalDate transactionDate = transaction.getDate();
			if (transactionDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
				transactionDate.plusDays(1);
			}
			if (transactionDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
				transactionDate.plusDays(1);
			}
			if (today.isAfter(transactionDate.minusDays(PORTFOLIO_TRANSACTION_REPORT_WINDOW))) {
				if (transaction.getType().equalsIgnoreCase("Withdraw")) {
					// TODO bundle 'like' withdrawals for same date
					withdrawDate = transaction.getDate();
					withdrawTransactions.add(transaction);
				} else {
					transferTransactions.add(transaction);
				}
			}
			while (transaction.isRecurring() && !today.isBefore(transactionDate)) {
				switch (transaction.getRecurringPeriod()) {
				case "Month":
					transactionDate = transactionDate.plusMonths(1);
					break;
				case "Year":
					transactionDate = transactionDate.plusYears(1);
					break;
				default:
					System.out.print("Unknown recurrng period:  " + transaction.getRecurringPeriod());
					transactionDate = transactionDate.plusYears(99);
					break;
				}
				transaction.setDate(transactionDate);
			}
			if (transaction.getDate().isAfter(today)) {
				updatedTransactions.add(transaction);
			}
		}
		if (withdrawTransactions.size() > 0) {
			processPortfolioTransactionWithdraw(withdrawDate, withdrawTransactions, portfolioService, document,
					portfolio);
		}
		if (transferTransactions.size() > 0) {
			processPortfolioTransactionTransfer(transferTransactions, portfolioService, document, portfolio);
		}
		// update next run date
		portfolioService.updatePortfolioSchedule(SCHEDULE_FILE, updatedTransactions);

	}

	private void processPortfolioTransactionWithdraw(LocalDate withdrawDate,
			List<PortfolioTransaction> withdrawTransactions, PortfolioService portfolioService, Document document,
			Portfolio portfolio2) {

		Portfolio portfolio = portfolioService.getPortfolio();

		String title = "Withdrawal scheduled for " + DATE_FORMATTER.format(withdrawDate);
		BigDecimal withdrawAmount = BigDecimal.ZERO;
		BigDecimal totalAddlTaxes = BigDecimal.ZERO;
		BigDecimal totalWithdrawalIncludingTaxes = withdrawAmount;
		List<Pair<String, BigDecimal>> fundWithdrawals = new ArrayList<>();

		withdrawTransactions.sort(Comparator.comparing(PortfolioTransaction::getAmount).reversed());

		BigDecimal netWithdrawalAmount = BigDecimal.ZERO;

		for (PortfolioTransaction transaction : withdrawTransactions) {
			withdrawAmount = withdrawAmount.add(transaction.getAmount());

			if (transaction.getFundSymbol() != null && transaction.getFundSymbol().length() > 0) {
				fundWithdrawals.add(Pair.of(transaction.getFundSymbol(), transaction.getAmount()));
			}
			if (transaction.isNetAmount()) {
				// add in taxes which will be distributed across portfolio
				// to include taxes in selected fund, add to amount and set Is Net Amount to
				// true
				netWithdrawalAmount = netWithdrawalAmount.add(transaction.getAmount());
				totalAddlTaxes = totalAddlTaxes
						.add(transaction.getAmount().multiply(WITHOLD_TAXES_PERCENT).setScale(2, RoundingMode.HALF_UP));
			} else {
				netWithdrawalAmount = netWithdrawalAmount
						.add(transaction.getAmount().multiply(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE));
			}
			title += "; " + transaction.getDescription();
		}

		totalWithdrawalIncludingTaxes = withdrawAmount.add(totalAddlTaxes);
		title += " " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalIncludingTaxes) + " Net: "
				+ CurrencyHelper.formatAsCurrencyString(netWithdrawalAmount);
		System.out.println(title);

		// Calculate withdrawals
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalIncludingTaxes,
				fundWithdrawals);

		// print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, withdrawAmount, totalWithdrawalIncludingTaxes,
				withdrawals, document);

	}

	private void processPortfolioTransactionTransfer(List<PortfolioTransaction> transactions,
			PortfolioService portfolioService, Document document, Portfolio portfolio) {

		Map<String, BigDecimal> withdrawalMap = new LinkedHashMap<>();

		String title = "";
		LocalDate date = LocalDate.now();
		transactions.sort(Comparator.comparing(PortfolioTransaction::getAmount).reversed());

		for (PortfolioTransaction transaction : transactions) {
			withdrawalMap.put(transaction.getFundSymbol(), BigDecimal.ZERO.subtract(transaction.getAmount()));
			title = transaction.getDescription();
			date = transaction.getDate();
		}
		title = title + " Scheduled for " + DATE_FORMATTER.format(date);
		System.out.println(title);

		Map<String, BigDecimal> transfers = portfolioService.calculateFixedExpensesTransfer(withdrawalMap);

		portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, BigDecimal.ZERO, transfers,
				document);

	}

}
