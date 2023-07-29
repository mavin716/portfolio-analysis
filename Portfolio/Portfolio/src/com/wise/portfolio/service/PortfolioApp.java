package com.wise.portfolio.service;

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
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.pdf.FooterHandler;
import com.wise.portfolio.pdf.HeaderHandler;
import com.wise.portfolio.portfolio.ManagedPortfolio;

public class PortfolioApp {

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

			// Get price history via alphaVantage
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
//				String symbol = entry.getKey();
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, true);
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
			}

			// Load fund allocation file
			portfolioService.loadFundAllocation(ALLOCATION_FILE);

			BigDecimal total2021 = portfolio.getTotalValueByDate(LocalDate.of(2021, 1, 5));
			System.out.println(CurrencyHelper.formatAsCurrencyString(total2021));
			// save all prices in spreadsheet
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
			FooterHandler footerHandler = new FooterHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);
			pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);

			//
			LocalDate today = LocalDate.now();

			// Property Tax due Feb 1st
			if (today.getMonthValue() == 1 && today.getDayOfMonth() > 10) {
				headerHandler.setHeader("propety tax withdrawal");
				pdfDoc.addNewPage();
				printPropertyTaxWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}

			// School tax due Sept 1st
			if (today.getMonthValue() == 8 && today.getDayOfMonth() > 10) {
				headerHandler.setHeader("school tax withdrawal");
				pdfDoc.addNewPage();
				printSchoolTaxWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}

			// Monthly withdrawal, if before 24th include auto mortgage payment
			if (today.getDayOfMonth() > 19 || today.getDayOfMonth() < 4) {
				headerHandler.setHeader("monthly withdrawal");
				pdfDoc.addNewPage();
				if (today.getDayOfMonth() < 26 && today.getDayOfMonth() >= 4) {
					// Withdrawal including auto withdrawal $650 mortgage from MM
					printPostCondoMonthlyWithdrawalSpreadsheet(portfolio, document, portfolioService);
				} else {
					// Withdrawal excluding auto withdrawal $650 mortgage from MM
					printPostCondoMonthlyWithdrawalSpreadsheet26(portfolio, document, portfolioService);
				}
			}
			if (today.getDayOfMonth() < 19 && today.getDayOfMonth() > 7) {
				headerHandler.setHeader("fixed expenses transfer include transfer $650 into Fed MM");
				pdfDoc.addNewPage();
				printFixedExpensesTransferSpreadsheet(portfolio, document, portfolioService);
			}

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
				if (fund.isFixedExpensesAccount())
					continue;
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
			// Print fund Trends
			// portfolioService.printTrends(portfolio);

			// Print ranking
			headerHandler.setHeader("Ranking");
			pdfDoc.addNewPage();
			portfolioService.printRanking(document);

//            Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments(portfolio);
//           System.out.println("Rebalance funds");
//           portfolioService.rebalanceFunds(portfolio, EXCHANGE_INCREMENT, adjustments);

			Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments();
//			portfolioService.rebalanceFunds(portfolio, EXCHANGE_INCREMENT,
//					portfolioService.calculateAdjustments(portfolio));
			pdfDoc.addNewPage();
			String title = "Adjust portfolio";
			portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, adjustments, document);

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss.SSS");
			// move latest file to dated file to make way for next download
			String newFileName = CURRENT_DOWNLOAD_FILE.substring(0, CURRENT_DOWNLOAD_FILE.lastIndexOf(".")) + " - "
					+ formatter.format(new Date(System.currentTimeMillis())) + ".csv";
			if (Files.exists(Paths.get(CURRENT_DOWNLOAD_FILE)) && !Files.exists(Paths.get(newFileName))) {
				Files.move(Paths.get(CURRENT_DOWNLOAD_FILE), Paths.get(newFileName));
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
			String textBody = "Change:  " + formatAsCurrencyString(difference) + " Total:  "
					+ formatAsCurrencyString(portfolio.getTotalValue());
			MailService.sendMail(subject, textBody, portfolioPdfFile);

		} catch (Exception e) {
			System.out.println("Exception e: " + e.getMessage());
			e.printStackTrace();
		}
		return portfolioPdfFile;

	}

	private static final String DOWNLOAD_PATH = "C:\\Users\\mavin\\Downloads\\";

	private static final String DOWNLOAD_FILENAME_PREFIX = "ofxdownload";
	private static final String CURRENT_DOWNLOAD_FILE = DOWNLOAD_PATH + DOWNLOAD_FILENAME_PREFIX + ".csv";
	private static final String ALLOCATION_FILE = DOWNLOAD_PATH + "allocation.csv";

	private static final String FUND_SYMBOLS_MAP_FILE = "allocation.csv";
	private static final String PORTFOLIO_PDF_FILE = "C:\\Users\\mavin\\Documents\\portfolio.pdf";

	private static final BigDecimal FEDERAL_WITHOLD_TAXES_PERCENT = new BigDecimal(.12);
	private static final BigDecimal STATE_WITHOLD_TAXES_PERCENT = new BigDecimal(.03);
	private static final BigDecimal WITHOLD_TAXES_PERCENT = FEDERAL_WITHOLD_TAXES_PERCENT
			.add(STATE_WITHOLD_TAXES_PERCENT);
	private static final BigDecimal AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE = BigDecimal.ONE
			.subtract(WITHOLD_TAXES_PERCENT);

	private static final BigDecimal CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT = new BigDecimal(600);
	private static final BigDecimal MONTHLY_EXPENSES_AMOUNT = new BigDecimal(1000);

	private static final BigDecimal PROPERTY_TAX_WITHDRAWAL_AMOUNT = new BigDecimal(3000);
	private static final BigDecimal SCHOOL_TAX_WITHDRAWAL_AMOUNT = new BigDecimal(4000);

	public static String formatAsCurrencyString(BigDecimal n) {
		return NumberFormat.getCurrencyInstance().format(n);
	}

	public static void main(String[] args) {

		PortfolioApp app = new PortfolioApp();
		app.run();

	}

	private void printFixedExpensesTransferSpreadsheet(ManagedPortfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal totalCondoMortgageAmountIncludingTaxes = CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT
				.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE, 0, RoundingMode.UP);

		String title = "Monthly Fixed Expenses Transfer (included "
				+ CurrencyHelper.formatAsCurrencyString(totalCondoMortgageAmountIncludingTaxes)
				+ " into Fed MM for Condo Mortgage)";
		System.out.println(title);

		Map<String, BigDecimal> withdrawals = portfolioService.calculateFixedExpensesTransfer(new BigDecimal(1000),
				new BigDecimal(1000).add(totalCondoMortgageAmountIncludingTaxes));
//		Map<String, BigDecimal> withdrawals = portfolioService.calculateFixedExpensesTransfer(new BigDecimal(0),
//				new BigDecimal(0).add(totalCondoMortgageAmountIncludingTaxes));

		portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, withdrawals, document);

	}

	private void printSchoolTaxWithdrawalSpreadsheet(ManagedPortfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = SCHOOL_TAX_WITHDRAWAL_AMOUNT;
		BigDecimal totalWithdrawalAmountIncludingTaxes = withdrawAmount.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE,
				0, RoundingMode.UP);

		String title = "School Tax Withdrawal: "
				+ CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmountIncludingTaxes) + " net: "
				+ CurrencyHelper.formatAsCurrencyString(withdrawAmount);
		System.out.println(title);

		// Calculate withdrawals
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalAmountIncludingTaxes,
				BigDecimal.ZERO, new BigDecimal(4000));

		// print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, totalWithdrawalAmountIncludingTaxes, withdrawals,
				document);

	}

	private void printPostCondoMonthlyWithdrawalSpreadsheet(ManagedPortfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = MONTHLY_EXPENSES_AMOUNT.add(CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT);
		BigDecimal totalWithdrawalAmountIncludingTaxes = withdrawAmount.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE,
				0, RoundingMode.UP);

		String title = "Monthly Expenses (Including Automatic Monthly Condo Mortgage Share) Withdrawal "
				+ CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmountIncludingTaxes) + " Net:  "
				+ CurrencyHelper.formatAsCurrencyString(withdrawAmount);
		System.out.println(title);

		BigDecimal fedMMWithdrawal = CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE,
				0, RoundingMode.UP);

		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalAmountIncludingTaxes,
				BigDecimal.ZERO, fedMMWithdrawal);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, withdrawAmount, withdrawals, document);

	}

	private void printPostCondoMonthlyWithdrawalSpreadsheet26(ManagedPortfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = MONTHLY_EXPENSES_AMOUNT;
		// extra $700 to reimburse shiori for japan resort
		withdrawAmount = withdrawAmount.add(new BigDecimal(500));
		BigDecimal totalWithdrawalAmount = withdrawAmount.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE, 4,
				RoundingMode.UP);
		// round up to nearest $5
		totalWithdrawalAmount = totalWithdrawalAmount.divide(new BigDecimal(5), 0, RoundingMode.HALF_DOWN)
				.setScale(2, RoundingMode.HALF_DOWN).multiply(new BigDecimal(5));

		String title = "Monthly Expenses Withdrawal (extra $500 to reimburse Shiori for resort) " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmount)
				+ " Net:  " + CurrencyHelper.formatAsCurrencyString(withdrawAmount);
		System.out.println(title);

		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalAmount,
				BigDecimal.ZERO, BigDecimal.ZERO);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, withdrawAmount, withdrawals, document);

	}

	private void printPropertyTaxWithdrawalSpreadsheet(ManagedPortfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = PROPERTY_TAX_WITHDRAWAL_AMOUNT;
		BigDecimal totalWithdrawalAmount = withdrawAmount.divide(AFTER_TAXES_WITHDRAW_AMOUNT_PERCENTAGE, 0,
				RoundingMode.UP);

		String title = "Property Tax Withdrawal " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmount);
		System.out.println(title);

		// Net withdrawal adjusts for transfer to money market fixed accounts
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(totalWithdrawalAmount,
				BigDecimal.ZERO, PROPERTY_TAX_WITHDRAWAL_AMOUNT);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, PROPERTY_TAX_WITHDRAWAL_AMOUNT, withdrawals,
				document);
	}

}
