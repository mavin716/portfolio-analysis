package com.wise.portfolio.data;

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

public class PortfolioApp {

	public PortfolioApp() {
		super();
	}

	public File run() {

		File portfolioPdfFile = null;
		try {
			// Create the portfolio service
			PortfolioService portfolioService = new PortfolioService(DOWNLOAD_PATH, FUND_SYMBOLS_MAP_FILE);

			// Create the portfolio
			Portfolio portfolio = portfolioService.createPortfolio();

			// Load history download files
			portfolioService.loadPortfolio(portfolio, DOWNLOAD_FILENAME_PREFIX);
			// Used to change naming policy
			// portfolioService.updateDownloadFilenames(portfolio,
			// DOWNLOAD_FILENAME_PREFIX);

			// Load current download file
			// If current download file timestamp before 6pm, then the fund prices are from
			// the previous day.
			LocalDate currentDownloadFilePriceDate = LocalDate.now();
			if (LocalTime.now().isBefore(LocalTime.of(18, 0))) {
				currentDownloadFilePriceDate = currentDownloadFilePriceDate.minusDays(1);
			}
			portfolioService.loadPortfolioDownloadFile(portfolio, currentDownloadFilePriceDate, CURRENT_DOWNLOAD_FILE);

			// Get price history via alphaVantage

			for (Entry<String, PortfolioFund> entry : portfolio.getFundMap().entrySet()) {
				if (entry.getValue().getShares() == 0) {
					continue;
				}

				// not working, returning values which differ greatly from vanguard download
				// files
				String symbol = entry.getKey();
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, true);
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
			}

			// Load fund allocation file
			portfolioService.loadFundAllocation(portfolio, ALLOCATION_FILE);

			// save all prices in spreadsheet
			portfolioService.savePortfolioData(portfolio);

			// Overwrite PDF output file
			portfolioPdfFile = new File(PORTFOLIO_PDF_FILE);
			portfolioPdfFile.delete();

			PdfWriter writer = new PdfWriter(PORTFOLIO_PDF_FILE);
			PdfDocument pdfDoc = new PdfDocument(writer);

			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(30f, 10f, 30f, 10f);

			document.add(
					new Paragraph("Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy"))
							+ " " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))).setFontSize(14)
							.setHorizontalAlignment(HorizontalAlignment.CENTER));
			HeaderHandler headerHandler = new HeaderHandler();
			FooterHandler footerHandler = new FooterHandler();
			pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);
//			pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);

			//
			LocalDate today = LocalDate.now();
			if (today.getMonthValue() == 1 && today.getDayOfMonth() > 10) {
				headerHandler.setHeader("propety tax withdrawal");
				printPropertyTaxWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}
			if (today.getMonthValue() == 8 && today.getDayOfMonth() > 10) {
				headerHandler.setHeader("school tax withdrawal");
				printSchoolTaxWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}
			
			if (today.getDayOfMonth() > 17 || today.getDayOfMonth() < 4) {
				headerHandler.setHeader("monthly withdrawal");
				if (today.getDayOfMonth() < 27) {
					printPostCondoMonthlyWithdrawalSpreadsheet(portfolio, document, portfolioService);
				} else {
					printPostCondoMonthlyWithdrawalSpreadsheet26(portfolio, document, portfolioService);
				}
			}
			if (today.getDayOfMonth() < 16 && today.getDayOfMonth() > 7) {
				headerHandler.setHeader("fixed expenses transfer");
				printFixedExpensesTransferSpreadsheet(portfolio, document, portfolioService);
			}

			pdfDoc.addNewPage();
			headerHandler.setHeader("performance table");
			portfolioService.printPerformanceTable(portfolio, document);

			headerHandler.setHeader("portfolio performance table");
			portfolioService.printPortfolioPerformanceTable(portfolio, document);

			// Add price performance graphs,
			pdfDoc.addNewPage();
			headerHandler.setHeader("balance line graph");
			portfolioService.prinBalanceLineGraphs(portfolio, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			portfolioService.prinBalanceLineGraphs(portfolio, document, pdfDoc, LocalDate.now().minusYears(1),
					LocalDate.now());

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
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(200)) < 0 && maxPrice.compareTo(new BigDecimal(150)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(150)) < 0 && maxPrice.compareTo(new BigDecimal(100)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(100)) < 0 && maxPrice.compareTo(new BigDecimal(50)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(50)) < 0 && maxPrice.compareTo(new BigDecimal(30)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);
			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(30)) < 0 && maxPrice.compareTo(new BigDecimal(20)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);
			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(20)) < 0 && maxPrice.compareTo(new BigDecimal(1)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);
			// Print fund Trends
			// portfolioService.printTrends(portfolio);

			// Print ranking
			portfolioService.printRanking(portfolio, document);

//            Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments(portfolio);
//           System.out.println("Rebalance funds");
//           portfolioService.rebalanceFunds(portfolio, EXCHANGE_INCREMENT, adjustments);

			Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments(portfolio);
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

			BigDecimal currentTotalValue = portfolioService.getTotalValueByDate(portfolio, lastBusinessDay);
			BigDecimal previousTotalValue = portfolioService.getTotalValueByDate(portfolio, previousBusinessDay);
			BigDecimal difference = currentTotalValue.subtract(previousTotalValue);
			String subject = "YEAH";
			if (difference.compareTo(BigDecimal.ZERO) < 0) {
				subject = "NOOO";
			} else if (difference.compareTo(BigDecimal.ZERO) == 0) {
				subject = "SAME";
			}
			String textBody = "Change:  " + formatAsCurrencyString(difference) + " Total:  "
					+ formatAsCurrencyString(portfolio.getTotalValue());
//			portfolioService.sendMail(subject, textBody, portfolioPdfFile);

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
	private static final BigDecimal WITHDRAW_AMOUNT_PERCENTAGE = BigDecimal.ONE.subtract(WITHOLD_TAXES_PERCENT);

	private static final BigDecimal CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT = new BigDecimal(650);
	private static final BigDecimal MONTHLY_EXPENSES_AMOUNT = new BigDecimal(1000);
	private static final BigDecimal CONDO_MONTHLY_HOA = new BigDecimal(279);
	private static final BigDecimal CONDO_MONTHLY_ENERGY_AVERAGE = new BigDecimal(40);

	private static final BigDecimal CONDO_EXPENSES_MONTHLY_SHARE_AMOUNT = (CONDO_MONTHLY_HOA
			.add(CONDO_MONTHLY_ENERGY_AVERAGE)).divide(new BigDecimal(2));
	private static final BigDecimal PROPERTY_TAX_WITHDRAWAL_AMOUNT = new BigDecimal(3000);
	private static final BigDecimal SCHOOL_TAX_WITHDRAWAL_AMOUNT = new BigDecimal(4000);

	public static String formatAsCurrencyString(BigDecimal n) {
		return NumberFormat.getCurrencyInstance().format(n);
	}

	public static void main(String[] args) {

		PortfolioApp app = new PortfolioApp();
		app.run();

	}

	private void printFixedExpensesTransferSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		String title = "Monthly Fixed Expenses Transfer (included $650 into Fed MM for Condo Mortgage)";
		System.out.println(title);

		Map<String, BigDecimal> withdrawals = portfolioService.calculateFixedExpensesTransfer(portfolio,
				new BigDecimal(1000), new BigDecimal(1650));

		portfolioService.printWithdrawalSpreadsheet(title, portfolio, BigDecimal.ZERO, withdrawals, document);

	}

	private void printSchoolTaxWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = SCHOOL_TAX_WITHDRAWAL_AMOUNT;
		BigDecimal totalWithdrawalAmount = withdrawAmount.divide(WITHDRAW_AMOUNT_PERCENTAGE, 0, RoundingMode.UP);

		String title = "School Tax Withdrawal " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmount);
		System.out.println(title);

		// Calculate withdrawals
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio, totalWithdrawalAmount,
				BigDecimal.ZERO, new BigDecimal(2000));

		// print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, totalWithdrawalAmount, withdrawals, document);

	}

	private void printPostCondoMonthlyWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = MONTHLY_EXPENSES_AMOUNT.add(CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT);
		BigDecimal totalWithdrawalAmount = withdrawAmount.divide(WITHDRAW_AMOUNT_PERCENTAGE, 0, RoundingMode.UP);

		String title = "Monthly Expenses (Including Automatic Monthly Condo Mortgage Share) Withdrawal "
				+ CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmount) + " Net:  "
				+ CurrencyHelper.formatAsCurrencyString(withdrawAmount);
		System.out.println(title);

		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio, totalWithdrawalAmount,
				BigDecimal.ZERO, CONDO_MORTGAGE_MONTHLY_SHARE_AMOUNT);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, withdrawAmount, withdrawals, document);

	}

	private void printPostCondoMonthlyWithdrawalSpreadsheet26(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = MONTHLY_EXPENSES_AMOUNT;
		BigDecimal totalWithdrawalAmount = withdrawAmount.divide(WITHDRAW_AMOUNT_PERCENTAGE, 0, RoundingMode.UP);

		String title = "Monthly Expenses Withdrawal " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmount)
				+ " Net:  " + CurrencyHelper.formatAsCurrencyString(withdrawAmount);
		System.out.println(title);

		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio, totalWithdrawalAmount,
				BigDecimal.ZERO, BigDecimal.ZERO);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, withdrawAmount, withdrawals, document);

	}

	private void printPropertyTaxWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal withdrawAmount = PROPERTY_TAX_WITHDRAWAL_AMOUNT;
		BigDecimal totalWithdrawalAmount = withdrawAmount.divide(WITHDRAW_AMOUNT_PERCENTAGE, 0, RoundingMode.UP);

		String title = "Property Tax Withdrawal " + CurrencyHelper.formatAsCurrencyString(totalWithdrawalAmount);
		System.out.println(title);

		// Net withdrawal adjusts for transfer to money market fixed accounts
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio, totalWithdrawalAmount,
				BigDecimal.ZERO, PROPERTY_TAX_WITHDRAWAL_AMOUNT);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, PROPERTY_TAX_WITHDRAWAL_AMOUNT, withdrawals,
				document);
	}

}
