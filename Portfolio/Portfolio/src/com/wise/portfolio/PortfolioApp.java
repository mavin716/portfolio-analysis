package com.wise.portfolio;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private String SCHEDULE_FILE = DOWNLOAD_PATH + "Schedule.csv";

	private static final String FUND_SYMBOLS_MAP_FILE = "allocation.csv";
	private static final String PORTFOLIO_PDF_FILE = "C:\\Users\\mavin\\Documents\\portfolio.pdf";

	private static final BigDecimal FEDERAL_WITHOLD_TAXES_PERCENT = new BigDecimal(.12);
	private static final BigDecimal STATE_WITHOLD_TAXES_PERCENT = new BigDecimal(.03);
	private static final BigDecimal WITHOLD_TAXES_PERCENT = FEDERAL_WITHOLD_TAXES_PERCENT
			.add(STATE_WITHOLD_TAXES_PERCENT);
	private static final BigDecimal AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE = BigDecimal.ONE
			.subtract(WITHOLD_TAXES_PERCENT);
	
	private static final int RECENT_TRANSACTIONS_DAYS = 90;
	private static final long PORTFOLIO_TRANSACTION_REPORT_WINDOW = 10;

	public static void main(String[] args) {

		PortfolioApp app = new PortfolioApp();
		app.run();

	}

	public PortfolioApp() {
		super();
	}

	public File run() {

		File portfolioPdfFile = null;
		try {
			// Create the portfolio service
			PortfolioService portfolioService = new PortfolioService(DOWNLOAD_PATH);

			// Create the portfolio
			ManagedPortfolio portfolio = portfolioService.createPortfolio(FUND_SYMBOLS_MAP_FILE);

			// Load history download files
			portfolioService.loadPortfolioDownloadFiles(portfolio, DOWNLOAD_FILENAME_PREFIX, CURRENT_DOWNLOAD_FILE);
			// Used to change naming policy
			// portfolioService.updateDownloadFilenames(portfolio,
			// DOWNLOAD_FILENAME_PREFIX);

			// Load price history via alphaVantage
			for (Entry<String, PortfolioFund> entry : portfolio.getFundMap().entrySet()) {
				if (entry.getValue().getShares() == 0) {
					continue;
				}
//				boolean success = AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, entry.getKey(), true);
//				if (!success) {
//					break;
//				}
				// not working, returning values which differ greatly from vanguard download
				// files
				String symbol = entry.getKey();
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, true);
				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
			}

			portfolioService.loadFundAllocation(ALLOCATION_FILE);

			portfolioService.loadPortfolioScheduleFile(SCHEDULE_FILE);

			portfolioService.savePortfolioData();

			portfolioService.setFundColors();

			// Overwrite PDF output file
			portfolioPdfFile = new File(PORTFOLIO_PDF_FILE);
			portfolioPdfFile.delete();

			PdfWriter writer = new PdfWriter(PORTFOLIO_PDF_FILE);
			PdfDocument pdfDoc = new PdfDocument(writer);

			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(30f, 10f, 30f, 10f);

			// Document Title
			document.add(
					new Paragraph("Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy"))
							+ " " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))).setFontSize(14)
							.setHorizontalAlignment(HorizontalAlignment.CENTER));
			HeaderHandler headerHandler = new HeaderHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
			FooterHandler footerHandler = new FooterHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

			portfolioService.printRecentTransactionsSpreadsheet(
					"Recent Transactions (" + RECENT_TRANSACTIONS_DAYS + " days)", RECENT_TRANSACTIONS_DAYS, portfolio,
					document);

			// Process scheduled portfolio transactions
			boolean updateScedulerFile = false;
			// Aggregate transfers to allow multiple transactions in one day
			List<PortfolioTransaction> transferTransactions = new ArrayList<>();
			LocalDate today = LocalDate.now();
			List<PortfolioTransaction> transactions = portfolio.getPortfolioTransactions();
					for (PortfolioTransaction transaction : transactions) {
				LocalDate transactionDate = transaction.getDate();
				if (today.isAfter(transactionDate.minusDays(PORTFOLIO_TRANSACTION_REPORT_WINDOW))) {
					if (transaction.getType().equalsIgnoreCase("Withdraw")) {
						processPortfolioTransactionWithdraw(transaction, portfolioService, document);
					} else {
						transferTransactions.add(transaction);
					}
					if (transaction.isRecurring() && today.isEqual(transactionDate)) {
						updateScedulerFile = true;
						switch (transaction.getRecurringPeriod()) {
						case "Month":
							transaction.setDate(transactionDate.plusMonths(1));
							break;
						case "Year":
							transaction.setDate(transactionDate.plusYears(1));
							break;
						}
					}
				}
			}
			if (transferTransactions.size() > 0) {
				processPortfolioTransactionTransfer(transferTransactions, portfolioService, document, portfolio);
			}
			// update next run date
			if (updateScedulerFile) {
				portfolioService.updatePortfolioSchedule(SCHEDULE_FILE, portfolio.getPortfolioTransactions());
			}

			// use scheduler
//			// Property Tax
//			LocalDate withdrawalDate = PROPERTY_TAX_DUE_DATE.minusDays(15);
//			if (today.isBefore(PROPERTY_TAX_DUE_DATE.minusDays(5)) && today.isAfter(withdrawalDate)) {
//				headerHandler.setHeader("propety tax withdrawal");
//				pdfDoc.addNewPage();
//				printOctInsuranceWithdrawalSpreadsheet(document, portfolioService);
//			}
//
//			// School tax
//			withdrawalDate = SCHOOL_TAX_DUE_DATE.minusDays(15);
//			if (today.isBefore(SCHOOL_TAX_DUE_DATE.minusDays(5)) && today.isAfter(withdrawalDate)) {
//				headerHandler.setHeader("school tax withdrawal");
//				pdfDoc.addNewPage();
//				printSchoolTaxWithdrawalSpreadsheet(document, portfolioService);
//			}
//
//			// Homeowners insurance and half auto insurance
//			withdrawalDate = HOMEOWNERS_INSURANCE_DUE_DATE.minusDays(15);
//			if (today.isBefore(HOMEOWNERS_INSURANCE_DUE_DATE) && today.isAfter(withdrawalDate)) {
//				BigDecimal insuranceWithdrawalAmount = HOMEOWNERS_INSURANCE_AMOUNT
//						.add(AUTO_INSURANCE_SEMI_YEARLY_AMOUNT);
//				BigDecimal recentWithdrawalAmount = portfolio.getRecentWithdrawalAmount(10);
//				if (recentWithdrawalAmount.compareTo(insuranceWithdrawalAmount) < 0) {
//					headerHandler.setHeader("homeowners insurance withdrawal");
//					pdfDoc.addNewPage();
//					printOctInsuranceWithdrawalSpreadsheet(document, portfolioService);
//				}
//			}
//
//			// Monthly withdrawal,
//			if (today.getDayOfMonth() > 20) {
//				pdfDoc.addNewPage();
//				// if before 24th include mortgage payment (automatically withdrawn)
//				if (today.getDayOfMonth() <= CONDO_MORTGAGE_WITHDRAW_DAY_OF_MONTH && today.getDayOfMonth() > 19) {
//					// Withdrawal including auto withdrawal $600 mortgage from MM
//					headerHandler.setHeader("monthly withdrawal plus $600 MM Mortage");
//					printMonthlyWithdrawalWithCondoMortgageSpreadsheet(document, portfolioService);
//				} else {
//					// Withdrawal excluding automatic withdrawal for mortgage
//					headerHandler.setHeader("monthly withdrawal");
//					printMonthlyWithdrawalSpreadsheet(document, portfolioService);
//				}
//			}
//			if (today.getDayOfMonth() < 16 && today.getDayOfMonth() > 10) {
//				headerHandler.setHeader("fixed expenses transfer include transfer $600 into Fed MM");
//				pdfDoc.addNewPage();
//				printFixedExpensesTransferSpreadsheet(document, portfolioService);
//			}

			headerHandler.setHeader("performance table");
			pdfDoc.addNewPage();
			portfolioService.printPerformanceTable(document);

			headerHandler.setHeader("portfolio performance table");
			pdfDoc.addNewPage();
			portfolioService.printPortfolioPerformanceTable(document);

			// Add price performance graphs,
			headerHandler.setHeader("balance line graph");
			pdfDoc.addNewPage();
			portfolioService.printBalanceLineGraphs(document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			portfolioService.printBalanceLineGraphs(document, pdfDoc, LocalDate.now().minusYears(1), LocalDate.now());

			pdfDoc.addNewPage();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				// if (fund.isClosed())
				// continue;
				portfolioService.printFundPerformanceLineGraph(fund.getSymbol(), document, pdfDoc,
						LocalDate.now().minusYears(5), today);
			}
//			pdfDoc.addNewPage();
//			PortfolioPriceHistory portfolioPriceHistory = portfolio.getPriceHistory();
//			List<String> fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(100000)) > 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (balance > 100K)", fundSynbols,  document, pdfDoc, null, null);
//			portfolioService.printFundBalanceLineGraphs(portfolio, "1 yr Funds (balance > 100K)", fundSynbols,  document, pdfDoc, oneYrAgo, LocalDate.now());
//			
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount() || fund.isClosed()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(100000)) <= 0  && maxPriceFundValue.compareTo(new BigDecimal(50000)) > 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (100K > balance > 50K)", fundSynbols,  document, pdfDoc, null, null);
//			portfolioService.printFundBalanceLineGraphs(portfolio, "1 yr Funds (100K > balance > 50K)", fundSynbols,  document, pdfDoc, oneYrAgo, LocalDate.now());
//
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount() || fund.isClosed()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(50000)) <= 0  && maxPriceFundValue.compareTo(new BigDecimal(40000)) > 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (50K > balance > 40K)", fundSynbols,  document, pdfDoc, null, null);
//			portfolioService.printFundBalanceLineGraphs(portfolio, "YTD Funds (50K > balance > 40K)", fundSynbols,  document, pdfDoc,oneYrAgo, LocalDate.now());
//
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount() || fund.isClosed()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(40000)) <= 0  && maxPriceFundValue.compareTo(new BigDecimal(30000)) > 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (40K > balance > 30K)", fundSynbols,  document, pdfDoc, null, null);
//
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount() || fund.isClosed()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(30000)) <= 0  && maxPriceFundValue.compareTo(new BigDecimal(25000)) > 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (30K > balance > 25K)", fundSynbols,  document, pdfDoc, null, null);
//
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount() || fund.isClosed()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(25000)) <= 0  && maxPriceFundValue.compareTo(new BigDecimal(20000)) > 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (25K > balance > 20K)", fundSynbols,  document, pdfDoc, null, null);
//
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount() || fund.isClosed()) continue;
//				LocalDate maxDate = portfolioPriceHistory.getMaxPrice(fund).getKey();
//				BigDecimal maxPriceFundValue = portfolioPriceHistory.getFundValueByDate(fund, maxDate, true);
//				if (maxPriceFundValue != null && maxPriceFundValue.compareTo(new BigDecimal(20000)) <= 0) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Funds (20K > balance)", fundSynbols,  document, pdfDoc, null, null);
//
//			fundSynbols = new ArrayList<String>();
//			for (PortfolioFund fund : portfolio.getFundMap().values()) {
//				if (fund.isFixedExpensesAccount()) {
//					fundSynbols.add(fund.getSymbol());
//				}
//			}
//			portfolioService.printFundBalanceLineGraphs(portfolio, "Fixed Expenses Funds", fundSynbols,  document, pdfDoc, null, null);
//
			// Separate by current price (e.g., > 200, 100 - 200, 50 = 100, under 50
			pdfDoc.addNewPage();
			List<String> fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(200)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(200)) < 0 && maxPrice.compareTo(new BigDecimal(150)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(150)) < 0 && maxPrice.compareTo(new BigDecimal(100)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(100)) < 0 && maxPrice.compareTo(new BigDecimal(50)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(50)) < 0 && maxPrice.compareTo(new BigDecimal(30)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);
			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(30)) < 0 && maxPrice.compareTo(new BigDecimal(20)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(20)) < 0 && maxPrice.compareTo(new BigDecimal(1)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.printPerformanceLineGraphs(fundSynbols, document, pdfDoc, null, null);

			// portfolioService.printTrends(portfolio);

			// Print ranking
			headerHandler.setHeader("Ranking");
			pdfDoc.addNewPage();
			portfolioService.printRanking(document);

			Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments();
			pdfDoc.addNewPage();
			String title = "Adjust portfolio";
			portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, adjustments, document);

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

			LocalDate lastBusinessDay = LocalDate.now();
			if (LocalTime.now().getHour() < 18) {
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
			System.out.println("Exception e: " + e.getMessage());
			e.printStackTrace();
		}
		return portfolioPdfFile;

	}

	private void processPortfolioTransactionWithdraw(PortfolioTransaction transaction,
			PortfolioService portfolioService, Document document) {
		ManagedPortfolio portfolio = portfolioService.getPortfolio();

		BigDecimal withdrawAmount = transaction.getAmount();
		BigDecimal totalWithdrawalAmountIncludingTaxes = withdrawAmount.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE,
				0, RoundingMode.UP);

		String title = transaction.getDescription() + " " + DATE_FORMATTER.format(transaction.getDate()) + " "
				+ CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmountIncludingTaxes) + " net: "
				+ CurrencyHelper.formatAsCurrencyString(withdrawAmount);
		System.out.println(title);

		// Calculate withdrawals
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalAmountIncludingTaxes,
				transaction.getFundSymbol());

		// print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, totalWithdrawalAmountIncludingTaxes, withdrawals,
				document);

	}

	private void processPortfolioTransactionTransfer(List<PortfolioTransaction> transactions,
			PortfolioService portfolioService, Document document, ManagedPortfolio portfolio) {

		Map<String, BigDecimal> withdrawalMap = new LinkedHashMap<>();

		String title = "";
		for (PortfolioTransaction transaction : transactions) {
			withdrawalMap.put(transaction.getFundSymbol(), BigDecimal.ZERO.subtract(transaction.getAmount()));
			title = transaction.getDescription();
		}
		System.out.println(title);

		Map<String, BigDecimal> transfers = portfolioService.calculateFixedExpensesTransfer(withdrawalMap);

		portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, transfers, document);

	}

}
