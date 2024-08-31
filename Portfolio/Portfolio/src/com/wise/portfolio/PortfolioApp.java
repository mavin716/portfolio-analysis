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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.data.time.TimeSeriesCollection;

import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.wise.portfolio.alphaVantage.AlphaVantageFundPriceService;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.pdf.FooterHandler;
import com.wise.portfolio.pdf.HeaderHandler;
import com.wise.portfolio.portfolio.ManagedPortfolio;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.portfolio.PortfolioTransaction;
import com.wise.portfolio.service.AppProperties;
import com.wise.portfolio.service.CurrencyHelper;
import com.wise.portfolio.service.MailService;
import com.wise.portfolio.service.PortfolioService;

public class PortfolioApp {

	protected static final Logger logger = LogManager.getLogger(PortfolioService.class);

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");
	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm");

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

			String downloadPath = AppProperties.getProperty("downloadPath");
			String currentDownloadFilename = AppProperties.getProperty("currentDownloadFilename");
			String downloadFilenamePrefix = AppProperties.getProperty("downloadFilenamePrefix");
			// Create the portfolio service
			portfolioService = new PortfolioService(downloadPath);

			// Create the portfolio
			portfolio = portfolioService.createPortfolio(AppProperties.getProperty("fundAllocationFileName"));

			// Load history download files
			logger.info("Load download files");
			portfolioService.loadPortfolioDownloadFiles(portfolio, downloadFilenamePrefix, currentDownloadFilename);
			// Used to change naming policy
			// portfolioService.updateDownloadFilenames(portfolio,
			// DOWNLOAD_FILENAME_PREFIX);

			SimpleDateFormat fileNameFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSS");
			// move latest file to dated file to make way for next download
			String newFileName = downloadFilenamePrefix + " - "
					+ fileNameFormatter.format(new Date(System.currentTimeMillis())) + ".csv";
			if (Files.exists(Paths.get(downloadPath, currentDownloadFilename))
					&& !Files.exists(Paths.get(newFileName))) {
				Files.move(Paths.get(downloadPath, currentDownloadFilename), Paths.get(downloadPath, newFileName));
			}

			logger.info("Load fund allocations");
			String filePath = Paths.get(downloadPath, AppProperties.getProperty("fundAllocationFileName")).toString();
			portfolioService.loadFundAllocation(filePath);

			// Load price history via alphaVantage
			logger.info("Retrieve price history from AlphaVantage");
			List<PortfolioFund> fundList = portfolio.getFunds();
			// randomize order of funds to compensate for daily quota 25
			Collections.shuffle(fundList);
			fundList.parallelStream().filter(f -> !f.isMMFund() && !f.isClosed()).forEach(f -> {
				try {
					AlphaVantageFundPriceService.retrieveFundPriceHistoryFromAlphaVantage(portfolio, f.getSymbol(),
							false);
				} catch (IOException e) {
					logger.error("Exception retrieving prices from Alpha Vantage:  " + e.getMessage(), e);
				}
			});

			logger.info("Load alpha vantage history files");
			portfolio.getPriceHistory().loadAlphaPriceHistoryFile(portfolio, downloadPath,
					AppProperties.getProperty("alphaVantagePriceHistoryFilename"));

			logger.info("Load scheduled transactions");
			filePath = Paths.get(downloadPath, AppProperties.getProperty("scheduleFilename")).toString();
			portfolioService.loadPortfolioScheduleFile(filePath);

			logger.info("Load scenarios");
			String[] scenarios = null;
			String property = AppProperties.getProperty("projectedScenario");
			if (property != null) {
				scenarios = property.split(",");
			}
			for (String scenario : scenarios) {
				logger.debug("Load scenario:  " + scenario);
				filePath = Paths.get(downloadPath, scenario+".csv").toString();
				portfolioService.loadPortfolioScheduleScenarioFile(scenario, filePath);				
			}

			logger.info("Save portfolio data");
			portfolioService.savePortfolioData();

			BigDecimal difference = getBalanceDifference();
			String changeText = "Change:  " + NumberFormat.getCurrencyInstance().format(difference);
			logger.info("Portfolio value as of " + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + ": "
					+ CurrencyHelper.formatAsCurrencyString(portfolio.getTotalValue()) + " change: " + changeText);
			// Overwrite PDF output file
			portfolioPdfFile = new File(AppProperties.getProperty("reportFilePath"));
			portfolioPdfFile.delete();

			PdfWriter writer = new PdfWriter(portfolioPdfFile);
			PdfDocument pdfDoc = new PdfDocument(writer);

			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(30f, 10f, 30f, 10f);

			document.add(new Paragraph(generateReportTitle()).setFontSize(20).setBold()
					.setHorizontalAlignment(HorizontalAlignment.CENTER));

			HeaderHandler headerHandler = new HeaderHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
			FooterHandler footerHandler = new FooterHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

			// Process scheduled portfolio transactions
			logger.info("Process scheduled transactions");
			processScheduledPortfolioTransactions(document);

			logger.info("Print performance tables");
			headerHandler.setHeader("performance table");
			footerHandler.setFooter("* 10 yr return doesn't include income");
			pdfDoc.addNewPage();
			portfolioService.printPerformanceTable(document);

			headerHandler.setHeader("portfolio performance table");
			footerHandler.setFooter("");
			pdfDoc.addNewPage();
			portfolioService.printPortfolioPerformanceTable(document);

			logger.info("Print portfolio transaction tables");
			headerHandler.setHeader("Scheduled Transactions");
			pdfDoc.addNewPage();
			portfolioService.printScheduledTransactionsSpreadsheet("Scheduled Transactions",
					portfolio.getPortfolioTransactions(), document);

			Integer recentTransactionsDays = AppProperties.getPropertyAsInteger("recentTransactionsReportDays");
			portfolioService.printRecentTransactionsSpreadsheet(
					"Recent Transactions (" + recentTransactionsDays + " days)", recentTransactionsDays, portfolio,
					document);

			portfolioService.printYTDDistributionsSpreadsheet("YTD Withdrawals", portfolio, document,
					PortfolioService.getYtdDays());
			portfolioService.printFutureWithdrawalsSpreadsheet("Future Withdrawals", portfolio, document,
					getFederalTaxWithholdingPercentage(), getStateTaxWithholdingPercentage());

			// Add price performance graphs,
			logger.info("Print graphs");
			headerHandler.setHeader("balance line graph");
			pdfDoc.addNewPage();
			LocalDate endDate = LocalDate.now();
			if (LocalTime.now().isBefore(LocalTime.of(18, 0))) {
				endDate = endDate.minusDays(1);
			}
			LocalDate startDate = LocalDate.now().minusYears(5);
			startDate = startDate.withDayOfMonth(1);
			Period period = Period.ofMonths(1);
			portfolioService.printBalanceLineAndBarGraphs(document, pdfDoc, startDate, endDate, period);

			headerHandler.setHeader("balance line graph by Category");
			period = Period.ofDays(1);
			portfolioService.printBalanceLinebyCategory(document, pdfDoc, startDate, endDate, period);

			pdfDoc.addNewPage();
			startDate = LocalDate.now().minusYears(1).withDayOfMonth(1);
			period = Period.ofDays(7);
			portfolioService.printBalanceLineAndBarGraphs(document, pdfDoc, startDate, endDate, period);

			pdfDoc.addNewPage();
			startDate = LocalDate.now().minusMonths(1);
			period = Period.ofDays(1);
			portfolioService.printBalanceLineAndBarGraphs(document, pdfDoc, startDate, endDate, period);

			// Projected Balance per schedule
			pdfDoc.addNewPage();
			portfolioService.printProjectedBalanceLineGraphs(document, pdfDoc, null, Period.ofMonths(1));

			// Projected Balance for schedule against scenarios
			pdfDoc.addNewPage();
			for (String scenario : scenarios) {
				portfolioService.printProjectedBalanceLineGraphs(document, pdfDoc, scenario, Period.ofMonths(1));
			}

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
			logger.info("Print ranking tables");
			headerHandler.setHeader("Ranking");
			pdfDoc.addNewPage();
			portfolioService.printRanking(document);

//			logger.info("Print adjustment tables");
//			Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments();
//			pdfDoc.addNewPage();
//			String title = "Adjust portfolio";
//			portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, BigDecimal.ZERO, adjustments,
//					document);

			// Closing the document
			document.close();
			logger.info("PDF Created");

			String subject = "YEAH";
			if (difference.compareTo(BigDecimal.ZERO) < 0) {
				subject = "NOOO";
			} else if (difference.compareTo(BigDecimal.ZERO) == 0) {
				subject = "SAME";
			}
			String textBody = "Change:  " + NumberFormat.getCurrencyInstance().format(difference) + " Total:  "
					+ NumberFormat.getCurrencyInstance().format(portfolio.getTotalValue());
			MailService.sendMail(subject, textBody, portfolioPdfFile);

		} catch (Exception e) {
			logger.error("Exception e: " + e.getMessage(), e);
		}
		return portfolioPdfFile;

	}

	private BigDecimal getStateTaxWithholdingPercentage() {
		BigDecimal percentage = AppProperties.getPropertyAsBigDecimal("stateTaxWithholdPercentage");
		return percentage;
	}

	private BigDecimal getFederalTaxWithholdingPercentage() {
		BigDecimal percentage = AppProperties.getPropertyAsBigDecimal("federalTaxWithholdPercentage");
		return percentage;
	}

	private String generateReportTitle() {

		BigDecimal difference = getBalanceDifference();
		String changeText = "Change:  " + NumberFormat.getCurrencyInstance().format(difference);

		String title = "Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy")) + " "
				+ LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) + " Balance:  "
				+ NumberFormat.getCurrencyInstance().format(portfolio.getTotalValue()) + " " + changeText;
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
		BigDecimal currentValue = portfolio.getTotalValue();
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
			Integer recentTransactionsDays = AppProperties.getPropertyAsInteger("scheduleTransactionsReportWindow");
			if (today.isAfter(transactionDate.minusDays(recentTransactionsDays))) {
				if (transaction.getType().equalsIgnoreCase("Withdraw")) {
					withdrawDate = transaction.getDate();
					withdrawTransactions.add(transaction);
				} else {
					transferTransactions.add(transaction);
				}
			}
			if (transaction.isRecurring()) {
				PortfolioTransaction updatedTransaction = new PortfolioTransaction(transaction);
				while (!today.isBefore(transactionDate)) {

					switch (transaction.getRecurringPeriod()) {
					case "Month":
						transactionDate = transactionDate.plusMonths(1);
						break;
					case "Year":
						transactionDate = transactionDate.plusYears(1);
						break;
					default:
						logger.error("Unknown recurrng period:  " + transaction.getRecurringPeriod());
						transactionDate = transactionDate.plusYears(99);
						break;
					}
				}
				updatedTransaction.setDate(transactionDate);
				updatedTransactions.add(updatedTransaction);
			}
		}
		if (withdrawTransactions.size() > 0)

		{
			printPortfolioTransactionWithdraw(withdrawDate, withdrawTransactions, portfolioService, document,
					portfolio);
		}
		if (transferTransactions.size() > 0) {
			printPortfolioTransactionTransfer(transferTransactions, portfolioService, document, portfolio);
		}
		// update next run date
		portfolioService.updatePortfolioSchedule(AppProperties.getProperty("scheduleFilename"),
				AppProperties.getProperty("downloadPath"), updatedTransactions);

	}

	private void printPortfolioTransactionWithdraw(LocalDate withdrawDate,
			List<PortfolioTransaction> withdrawTransactions, PortfolioService portfolioService, Document document,
			Portfolio portfolio) {

		String title = "";
		BigDecimal netWithdrawalAmount = BigDecimal.ZERO;
		BigDecimal totalAddlTaxes = BigDecimal.ZERO;
		BigDecimal totalWithdrawalIncludingTaxes = BigDecimal.ZERO;
		List<Pair<String, BigDecimal>> fundWithdrawals = new ArrayList<>();

		withdrawTransactions.sort(Comparator.comparing(PortfolioTransaction::getAmount).reversed());

		// Sort for heading
		withdrawTransactions.sort(Comparator.comparing(PortfolioTransaction::getDate));

		BigDecimal totalWithholdTaxesPercentage = getFederalTaxWithholdingPercentage()
				.add(getStateTaxWithholdingPercentage());
		BigDecimal afterTaxesPercentage = BigDecimal.ONE.subtract(totalWithholdTaxesPercentage);
		for (PortfolioTransaction transaction : withdrawTransactions) {
//			withdrawAmount = withdrawAmount.add(transaction.getAmount());

			if (transaction.getFundSymbol() != null && transaction.getFundSymbol().length() > 0) {
				fundWithdrawals.add(Pair.of(transaction.getFundSymbol(), transaction.getAmount()));
			}
			if (transaction.isNetAmount()) {
				// add in taxes which will be distributed across portfolio
				// to include taxes in selected fund, add to amount and set Is Net Amount to
				// true
				BigDecimal taxes = transaction.getAmount().multiply(afterTaxesPercentage).setScale(2,
						RoundingMode.HALF_UP);
				netWithdrawalAmount = netWithdrawalAmount.add(transaction.getAmount());
				totalAddlTaxes = totalAddlTaxes.add(taxes);
				logger.debug("Net transaction, net amount:  " + transaction.getAmount() + " taxes: "
						+ CurrencyHelper.formatAsCurrencyString(taxes) + " total:  "
						+ CurrencyHelper.formatAsCurrencyString(transaction.getAmount().add(taxes)));
			} else {
				BigDecimal taxes = transaction.getAmount().multiply(totalWithholdTaxesPercentage);
				BigDecimal netAmount = transaction.getAmount().subtract(taxes);
				netWithdrawalAmount = netWithdrawalAmount.add(netAmount);
				totalAddlTaxes = totalAddlTaxes.add(taxes);
				logger.debug("Net transaction, net amount:  " + netAmount + " taxes: "
						+ CurrencyHelper.formatAsCurrencyString(taxes) + " total:  "
						+ CurrencyHelper.formatAsCurrencyString(transaction.getAmount()));
			}
			title = title + "\n\t" + DATE_FORMATTER.format(transaction.getDate()) + " " + transaction.getDescription()
					+ " Amount: " + CurrencyHelper.formatAsCurrencyString(transaction.getAmount());
		}

		totalWithdrawalIncludingTaxes = netWithdrawalAmount.add(totalAddlTaxes);
		title += "\n\tTotal: " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalIncludingTaxes) + " Net: "
				+ CurrencyHelper.formatAsCurrencyString(netWithdrawalAmount);
		logger.debug(title);

		// Calculate withdrawals
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalIncludingTaxes,
				fundWithdrawals);

		// print spreadsheet
		document.add(new Paragraph("Withdrawals").setBold().setFontSize(16));
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, netWithdrawalAmount,
				totalWithdrawalIncludingTaxes, withdrawals, document);

	}

	private void printPortfolioTransactionTransfer(List<PortfolioTransaction> transactions,
			PortfolioService portfolioService, Document document, Portfolio portfolio) {

		Map<String, BigDecimal> withdrawalMap = new LinkedHashMap<>();

		String title = "";
		// Sort for heading
		transactions.sort(Comparator.comparing(PortfolioTransaction::getDate));

		for (PortfolioTransaction transaction : transactions) {
			withdrawalMap.put(transaction.getFundSymbol(), BigDecimal.ZERO.subtract(transaction.getAmount()));
			title = title + "\n\t" + DATE_FORMATTER.format(transaction.getDate()) + " " + transaction.getDescription()
					+ " Amount: " + CurrencyHelper.formatAsCurrencyString(transaction.getAmount());
		}
		logger.debug(title);

		Map<String, BigDecimal> transfers = portfolioService.calculateFixedExpensesTransfer(withdrawalMap);

		document.add(new Paragraph("Transfers").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(16));
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, BigDecimal.ZERO, transfers,
				document);

	}

}
