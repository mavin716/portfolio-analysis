package com.wise.portfolio.data;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.UnitValue;

public class PortfolioApp {
	private static BigDecimal EXCHANGE_INCREMENT = new BigDecimal(500);

	private static final String DOWNLOAD_PATH = "C:\\Users\\Margaret\\Downloads\\";

	private static final String DOWNLOAD_FILENAME_PREFIX = "ofxdownload";
	private static final String CURRENT_DOWNLOAD_FILE = DOWNLOAD_PATH + DOWNLOAD_FILENAME_PREFIX + ".csv";
	private static final String ALLOCATION_FILE = DOWNLOAD_PATH + "allocation.csv";

	private static final String FUND_SYMBOLS_MAP_FILE = "allocation.csv";
	private static final String HISTORICAL_PRICES_FILE = "historical.csv";
	private static final String HISTORICAL_VALUES_FILE = "historicalvalues.csv";

	private static final BigDecimal MONTHLY_WITHDRAWAL_AMOUNT = new BigDecimal(2200);

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
			portfolioService.loadPriceHistory(portfolio, DOWNLOAD_FILENAME_PREFIX);

			// Load current download file
			// If current download file timestamp before 6pm, then the fund prices are from
			// the previous day.
			LocalDate currentDownloadFilePriceDate = LocalDate.now();
			if (LocalTime.now().isBefore(LocalTime.of(18, 0))) {
				currentDownloadFilePriceDate = currentDownloadFilePriceDate.minusDays(1);
			}
			portfolioService.loadPortfolioFile(portfolio, currentDownloadFilePriceDate, CURRENT_DOWNLOAD_FILE);

			// Load fund allocation file
			portfolioService.loadFundAllocation(portfolio, ALLOCATION_FILE);

			// System.out.println("Total portfolio value: " +
			// formatAsCurrencyString(portfolio.getTotalValue()));

			// save all prices in spreadsheet
			// TODO use historical prices to load price history since not getting
			// download file everyday and api only contains 90 days
			portfolioService.saveHistoricalPrices(portfolio, HISTORICAL_PRICES_FILE);
			portfolioService.saveHistoricalValue(portfolio, HISTORICAL_VALUES_FILE);

			String dest = "C:\\Users\\Margaret\\Documents\\portfolio.pdf";
			File portfolioPdfFile = new File(dest);
			portfolioPdfFile.delete();
			PdfWriter writer = new PdfWriter(dest);
			PdfDocument pdfDoc = new PdfDocument(writer);
			Document document = new Document(pdfDoc, PageSize.LEDGER);
			document.setMargins(30f, 30f, 30f, 30f);
			pdfDoc.addNewPage();
			portfolioService.printSpreadsheet(portfolio, document);

			// Print fund Trends
			// portfolioService.printTrends(portfolio);

			// Print ranking
			portfolioService.printRanking(portfolio, document);

//            Map<String, BigDecimal> adjustments = portfolioService.calculateAdjustments(portfolio);
//           System.out.println("Rebalance funds");
//           portfolioService.rebalanceFunds(portfolio, EXCHANGE_INCREMENT, adjustments);

			System.out.println("\n\nCalculate Withdrawal " + CurrencyHelper.formatAsCurrencyString(MONTHLY_WITHDRAWAL_AMOUNT));
			Map<String, BigDecimal> withdrawals = portfolioService.calculateWithdrawal(portfolio, MONTHLY_WITHDRAWAL_AMOUNT,
					BigDecimal.ZERO, BigDecimal.ZERO);
			// Transfer 500 into money market (included in withdrawal amount)
			withdrawals.put("VMRXX", new BigDecimal(-500));
			withdrawals.put("VMFXX", new BigDecimal(-500));
			BigDecimal netWithdrawalAmount = MONTHLY_WITHDRAWAL_AMOUNT.subtract(new BigDecimal(1200));
			portfolioService.printWithdrawalSpreadsheet(portfolio, netWithdrawalAmount, withdrawals, document);

			System.out.println("\n\nCalculate school tax  " + CurrencyHelper.formatAsCurrencyString(new BigDecimal(5300)));
			withdrawals = portfolioService.calculateWithdrawal(portfolio, new BigDecimal(1200),
					BigDecimal.ZERO, BigDecimal.ZERO);
			withdrawals.put("VMFXX", new BigDecimal(4100));
			netWithdrawalAmount = new BigDecimal(5300);
			portfolioService.printWithdrawalSpreadsheet(portfolio, netWithdrawalAmount, withdrawals, document);

			System.out.println("\n\nCalculate condo Withdrawal " + CurrencyHelper.formatAsCurrencyString(new BigDecimal(40000)));
			withdrawals = portfolioService.calculateWithdrawal(portfolio, new BigDecimal(40000),
					BigDecimal.ZERO, BigDecimal.ZERO);
			netWithdrawalAmount = new BigDecimal(40000);
			portfolioService.printWithdrawalSpreadsheet(portfolio, netWithdrawalAmount, withdrawals, document);

			portfolioService.rebalanceFunds(portfolio, EXCHANGE_INCREMENT,
					portfolioService.calculateAdjustments(portfolio));
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
			portfolioService.sendMail(portfolioPdfFile);

		} catch (Exception e) {
			System.out.println("Exception e: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
