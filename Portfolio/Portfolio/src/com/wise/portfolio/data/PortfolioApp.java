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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

public class PortfolioApp {

	private static final String DOWNLOAD_PATH = "C:\\Users\\Margaret\\Downloads\\";

	private static final String DOWNLOAD_FILENAME_PREFIX = "ofxdownload";
	private static final String CURRENT_DOWNLOAD_FILE = DOWNLOAD_PATH + DOWNLOAD_FILENAME_PREFIX + ".csv";
	private static final String ALLOCATION_FILE = DOWNLOAD_PATH + "allocation.csv";

	private static final String FUND_SYMBOLS_MAP_FILE = "allocation.csv";
	private static final String PORTFOLIO_PDF_FILE = "C:\\Users\\Margaret\\Documents\\portfolio.pdf";

	private static final BigDecimal MONTHLY_WITHDRAWAL_AMOUNT = new BigDecimal(2200);

	private static final BigDecimal EXCHANGE_INCREMENT = new BigDecimal(500);


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

			// Load current download file
			// If current download file timestamp before 6pm, then the fund prices are from
			// the previous day.
			LocalDate currentDownloadFilePriceDate = LocalDate.now();
			if (LocalTime.now().isBefore(LocalTime.of(18, 0))) {
				currentDownloadFilePriceDate = currentDownloadFilePriceDate.minusDays(1);
			}
			portfolioService.loadPortfolioDownloadFile(portfolio, currentDownloadFilePriceDate, CURRENT_DOWNLOAD_FILE);

			// Get price history via alphaVantage
			System.out.println("Retrieve prices from alphaVantage");

			int numFunds = portfolio.getFundMap().size();
			System.out.println("will take an extra " + (int) numFunds / 5 + " minutes to call alphaVantage API");
			for (Entry<String, PortfolioFund> entry : portfolio.getFundMap().entrySet()) {
				String symbol = entry.getKey();
				if (entry.getValue().getShares() == 0) {
					continue;
				}

				// not working, returning values which differ greatly from vanguard
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, true);
//				AlphaVantageFundPriceService.loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
			}

			// Load fund allocation file
			portfolioService.loadFundAllocation(portfolio, ALLOCATION_FILE);

			// save all prices in spreadsheet
			portfolioService.savePortfolioData(portfolio);

			// Create mew PDF output file
			File portfolioPdfFile = new File(PORTFOLIO_PDF_FILE);
			portfolioPdfFile.delete();
			
			PdfWriter writer = new PdfWriter(PORTFOLIO_PDF_FILE);
			PdfDocument pdfDoc = new PdfDocument(writer);
			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(10f, 10f, 10f, 10f);

			// extra $1000 for christmas
			System.out.println(
					"\n\nCalculate Withdrawal " + CurrencyHelper.formatAsCurrencyString(MONTHLY_WITHDRAWAL_AMOUNT));
			BigDecimal netWithdrawalAmount = MONTHLY_WITHDRAWAL_AMOUNT.subtract(new BigDecimal(1000));
			Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio,
					MONTHLY_WITHDRAWAL_AMOUNT, BigDecimal.ZERO, BigDecimal.ZERO, netWithdrawalAmount);
			// Transfer 500 into money market (included in withdrawal amount)
			withdrawals.put("VMRXX", new BigDecimal(-500));
			withdrawals.put("VMFXX", new BigDecimal(-500));
			portfolioService.printWithdrawalSpreadsheet(portfolio, netWithdrawalAmount, withdrawals, document);

			pdfDoc.addNewPage();
			portfolioService.printPerformanceTable(portfolio, document);


			// Add price performance graphs, 
			LocalDate oneYrAgo = LocalDate.now().minusYears(1);
			pdfDoc.addNewPage();
			portfolioService.prinBalanceLineGraphs(portfolio,  document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			portfolioService.prinBalanceLineGraphs(portfolio,  document, pdfDoc, oneYrAgo, LocalDate.now());

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
				if (maxPrice.compareTo(new BigDecimal(200)) < 0
						&& maxPrice.compareTo(new BigDecimal(100)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(100)) < 0
						&& maxPrice.compareTo(new BigDecimal(50)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);

			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(50)) < 0
						&& maxPrice.compareTo(new BigDecimal(25)) > 0) {
					fundSynbols.add(fund.getSymbol());
				}
			}
			portfolioService.prinPerformanceLineGraphs(portfolio, fundSynbols, document, pdfDoc, null, null);
			pdfDoc.addNewPage();
			fundSynbols = new ArrayList<String>();
			for (PortfolioFund fund : portfolio.getFundMap().values()) {
				BigDecimal maxPrice = portfolio.getPriceHistory().getMaxPrice(fund).getValue();
				if (maxPrice.compareTo(new BigDecimal(25)) < 0
						&& maxPrice.compareTo(new BigDecimal(1)) > 0) {
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
			portfolioService.rebalanceFunds(portfolio, EXCHANGE_INCREMENT,
					portfolioService.calculateAdjustments(portfolio));
			pdfDoc.addNewPage();
			portfolioService.printPerformanceTable(portfolio, document);

			portfolioService.calculateAdjustments(portfolio);

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
			if (LocalTime.now().getHour() < 19)
			{
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
			if (difference.compareTo(BigDecimal.ZERO) < 0)
			{
				subject = "NOOO";
			}
			String textBody = "Change:  " + formatAsCurrencyString(difference) + " Total:  " + formatAsCurrencyString(portfolio.getTotalValue());
			portfolioService.sendMail(subject, textBody, portfolioPdfFile);

		} catch (Exception e) {
			System.out.println("Exception e: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
