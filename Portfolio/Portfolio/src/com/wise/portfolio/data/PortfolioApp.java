package com.wise.portfolio.data;

import java.io.File;
import java.math.BigDecimal;
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

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.HorizontalAlignment;

public class PortfolioApp {

	private static final String DOWNLOAD_PATH = "C:\\Users\\Margaret\\Downloads\\";

	private static final String DOWNLOAD_FILENAME_PREFIX = "ofxdownload";
	private static final String CURRENT_DOWNLOAD_FILE = DOWNLOAD_PATH + DOWNLOAD_FILENAME_PREFIX + ".csv";
	private static final String ALLOCATION_FILE = DOWNLOAD_PATH + "allocation.csv";

	private static final String FUND_SYMBOLS_MAP_FILE = "allocation.csv";
	private static final String PORTFOLIO_PDF_FILE = "C:\\Users\\Margaret\\Documents\\portfolio.pdf";

	private static final BigDecimal WITHOLD_TAXES_PERCENT = new BigDecimal(.15);
	private static final BigDecimal MONTHLY_WITHDRAWAL_AMOUNT = new BigDecimal(1000);
	private static final BigDecimal CONDO_MONTHLY_SHARE_AMOUNT = new BigDecimal(650);
	private static final BigDecimal POST_CONDO_MONTHLY_WITHDRAWAL_AMOUNT = MONTHLY_WITHDRAWAL_AMOUNT
			.add(CONDO_MONTHLY_SHARE_AMOUNT);
	private static final BigDecimal PROPERTY_TAX_WITHDRAWAL_AMOUNT = new BigDecimal(3500);
	private static final BigDecimal SCHOOL_TAX_WITHDRAWAL_AMOUNT = new BigDecimal(4500);

	public static String formatAsCurrencyString(BigDecimal n) {
		return NumberFormat.getCurrencyInstance().format(n);
	}

	public static void main(String[] args) {

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
			if (LocalTime.now().isBefore(LocalTime.of(15, 0))) {
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
//				String symbol = entry.getKey();
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, true);
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
			}

			// Load fund allocation file
			portfolioService.loadFundAllocation(portfolio, ALLOCATION_FILE);

			// save all prices in spreadsheet
			portfolioService.savePortfolioData(portfolio);

			// Overwrite PDF output file
			File portfolioPdfFile = new File(PORTFOLIO_PDF_FILE);
			portfolioPdfFile.delete();

			PdfWriter writer = new PdfWriter(PORTFOLIO_PDF_FILE);
			PdfDocument pdfDoc = new PdfDocument(writer);

			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(10f, 10f, 10f, 10f);

//			document.setFont(PDFFont.COURIER);
//			document.setFontSize(10);
			document.add(
					new Paragraph("Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy"))
							+ " " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))).setFontSize(14)
									.setHorizontalAlignment(HorizontalAlignment.CENTER));

//			 HeaderHandler headerHandler = new HeaderHandler();
//			 headerHandler.setHeader("Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy"))
//							+ " " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
//		    pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);

			//
			LocalDate today = LocalDate.now();
			if (today.getMonthValue() == 1 && today.getDayOfMonth() > 15) {
				printPropertyTaxWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}
			if (today.getMonthValue() == 8 && today.getDayOfMonth() > 15) {
				printSchoolTaxWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}
			if (today.getDayOfMonth() > 9 || today.getDayOfMonth() < 3) {
				printMonthlyWithdrawalSpreadsheet(portfolio, document, portfolioService);
				printPostCondoMonthlyWithdrawalSpreadsheet(portfolio, document, portfolioService);
			}

			pdfDoc.addNewPage();
			portfolioService.printPerformanceTable(portfolio, document);

			portfolioService.printPortfolioPerformanceTable(portfolio, document);

			// Add price performance graphs,
			pdfDoc.addNewPage();
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
				if (maxPrice.compareTo(new BigDecimal(200)) < 0 && maxPrice.compareTo(new BigDecimal(100)) > 0) {
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
				if (maxPrice.compareTo(new BigDecimal(50)) < 0 && maxPrice.compareTo(new BigDecimal(25)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);
			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(25)) < 0 && maxPrice.compareTo(new BigDecimal(1)) > 0) {
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

//			System.out.println(
//					"\n\nCalculate condo Withdrawal " + CurrencyHelper.formatAsCurrencyString(new BigDecimal(40000)));
//			withdrawals = portfolioService.calculateWithdrawal(portfolio, new BigDecimal(40000), BigDecimal.ZERO,
//					BigDecimal.ZERO);
//			netWithdrawalAmount = new BigDecimal(40000);
//			portfolioService.printWithdrawalSpreadsheet(portfolio, netWithdrawalAmount, withdrawals, document);
//
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
			portfolioService.sendMail(subject, textBody, portfolioPdfFile);

		} catch (Exception e) {
			System.out.println("Exception e: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void printSchoolTaxWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		String title = "School Tax Withdrawal " + CurrencyHelper.formatAsCurrencyString(SCHOOL_TAX_WITHDRAWAL_AMOUNT);
		System.out.println(title);

		// Net withdrawal adjusts for transfer to money market fixed accounts
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio,
				SCHOOL_TAX_WITHDRAWAL_AMOUNT, BigDecimal.ZERO, new BigDecimal(2000), BigDecimal.ZERO, BigDecimal.ZERO);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, SCHOOL_TAX_WITHDRAWAL_AMOUNT, withdrawals,
				document);

	}

	private static void printMonthlyWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		String title = "Monthly Expenses Withdrawal "
				+ CurrencyHelper.formatAsCurrencyString(MONTHLY_WITHDRAWAL_AMOUNT);
		System.out.println(title);

		// Net withdrawal adjusts for transfer to money market fixed accounts
		// Transfer 500 into money market fixed accounts (included in withdrawal amount)
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio, MONTHLY_WITHDRAWAL_AMOUNT,
				new BigDecimal(1000), BigDecimal.ZERO, new BigDecimal(1000), BigDecimal.ZERO);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, MONTHLY_WITHDRAWAL_AMOUNT, withdrawals, document);

	}

	private static void printPostCondoMonthlyWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		BigDecimal taxesWitheld = POST_CONDO_MONTHLY_WITHDRAWAL_AMOUNT.multiply(WITHOLD_TAXES_PERCENT);
		BigDecimal netWithdrawalAmount = POST_CONDO_MONTHLY_WITHDRAWAL_AMOUNT.subtract(taxesWitheld);
		String title = "Post Condo Monthly Expenses Withdrawal "
				+ CurrencyHelper.formatAsCurrencyString(POST_CONDO_MONTHLY_WITHDRAWAL_AMOUNT) + " Net:  "
				+ CurrencyHelper.formatAsCurrencyString(netWithdrawalAmount);
		System.out.println(title);

		// Net withdrawal adjusts for transfer to money market fixed accounts
		// Transfer 500 into money market fixed accounts (included in withdrawal amount)
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio,
				POST_CONDO_MONTHLY_WITHDRAWAL_AMOUNT, new BigDecimal(1000), BigDecimal.ZERO, new BigDecimal(1000),
				BigDecimal.ZERO);

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, POST_CONDO_MONTHLY_WITHDRAWAL_AMOUNT, withdrawals,
				document);

	}

	private static void printPropertyTaxWithdrawalSpreadsheet(Portfolio portfolio, Document document,
			PortfolioService portfolioService) {

		String title = "Property Tax Withdrawal "
				+ CurrencyHelper.formatAsCurrencyString(PROPERTY_TAX_WITHDRAWAL_AMOUNT);
		System.out.println(title);

		// Net withdrawal adjusts for transfer to money market fixed accounts
		Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio,
				PROPERTY_TAX_WITHDRAWAL_AMOUNT, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				new BigDecimal(1750));

		// Calculate withdrawals and print spreadsheet
		portfolioService.printWithdrawalSpreadsheet(title, portfolio, PROPERTY_TAX_WITHDRAWAL_AMOUNT, withdrawals,
				document);
	}

}
