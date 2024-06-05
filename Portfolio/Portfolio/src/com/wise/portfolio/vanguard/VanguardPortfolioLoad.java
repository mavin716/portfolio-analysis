package com.wise.portfolio.vanguard;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.service.PerformanceService;
import com.wise.portfolio.service.PortfolioPriceHistory;

public class VanguardPortfolioLoad {
	
	public VanguardPortfolioLoad(Portfolio portfolio, String basePath, String downloadFilenamePrefix, String currentDownloadFile) {
		super();
		this.portfolio = portfolio;
		this.downloadFilenamePrefix = downloadFilenamePrefix;
		this.currentDownloadFile = currentDownloadFile;
		this.basePath = basePath;
	}
	private Portfolio portfolio;
	private String downloadFilenamePrefix;
	private String currentDownloadFile;
	private String basePath;
	
	/**
	 * @param currentDownloadFile
	 * @param path                to find price history files
	 * 
	 * @throws IOException
	 */
	public void loadPortfolioDownloadFiles() throws IOException {

		PerformanceService.setPortfolio(portfolio);
		PerformanceService.setPriceHistory(portfolio.getPriceHistory());

		// Load all download files
		LocalDate earliestDate = portfolio.getPriceHistory().getOldestDate();
		try (Stream<Path> stream = Files.list(Paths.get(basePath))) {

			List<String> filenames = stream.filter(p -> p.getFileName().toString().startsWith(downloadFilenamePrefix))
					.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
			BigDecimal yesterdayWithdrawals = BigDecimal.ZERO;
			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			for (String filename : filenames) {
				LocalDate date = getDownloadFileDate(filename, true);

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
				priceHistory.loadPortfolioDownloadFile(portfolio, date, basePath + "\\" + filename);

				BigDecimal withdrawals = BigDecimal.ZERO;
				for (PortfolioFund fund : portfolio.getFunds()) {
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
				for (PortfolioFund fund : portfolio.getFunds()) {
					if (fund != null) {
						income = income.add(fund.getDistributionsForDate(date));
					}
				}

			}
//			mostRecentSharePriceDay = earliestDate;
		}

//		if (portfolio.getPriceHistory().getMostRecentDay().isAfter(mostRecentSharePriceDay)) {
//			mostRecentSharePriceDay = portfolio.getPriceHistory().getMostRecentDay();
//		}

		// TODO read saved values to fill in
		// portfolio.getPriceHistory().loadFundSharesHistoryFile(portfolio, basePath,
		// "historical.csv");
		// portfolio.getPriceHistory().loadPortfolioSharesFile(portfolio, basePath,
		// "historicalshares.csv");

		// Use earliest date for cost, not true cost but don't have enough history to
		// get actual cost
//		Set<LocalDate> allDates = portfolio.getPriceHistory().getAllDates();
//		LocalDate oldestDate = allDates.iterator().next();
//		for (Entry<String, Map<LocalDate, BigDecimal>> entry : portfolio.getPriceHistory().getFundPrices().entrySet()) {
//			String symbol = entry.getKey();
//			BigDecimal oldestPrice = entry.getValue().values().iterator().next();
//			if (oldestPrice.compareTo(BigDecimal.ZERO) == 0) {
//				continue;
//			}
//			PortfolioFund fund = portfolio.getFund(symbol);
//			if (fund != null) {
//				fund.setCost(oldestPrice);
//			}
//		}

		// Load current download file
		// If current download file timestamp before 6pm, then the fund prices are from
		// the previous day.
		LocalDate currentDownloadFilePriceDate = LocalDate.now();
		if (LocalTime.now().isBefore(LocalTime.of(18, 0))) {
			currentDownloadFilePriceDate = currentDownloadFilePriceDate.minusDays(1);
		}
		loadPortfolioDownloadFile(currentDownloadFilePriceDate, currentDownloadFile);

	}
	public void loadPortfolioDownloadFile(LocalDate date, String filename) {

		try {
			portfolio.getPriceHistory().loadPortfolioDownloadFile(portfolio, date, filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private LocalDate getDownloadFileDate(String filename, boolean adjust) {

		LocalDateTime fileDateTime = null;
		LocalDate date = null;
		File file = new File(basePath + "\\" + filename);
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

			fileDateTime = LocalDateTime.ofEpochSecond(attr.creationTime().to(TimeUnit.SECONDS), 0,
					ZoneOffset.of("-5"));

			// time = LocalTime.ofNanoOfDay(attr.creationTime().to(TimeUnit.NANOSECONDS));
//			System.out.println("File creation date:   " + date.format(DATE_FORMATTER));
//			System.out.println("File creation time:   " + date.format(TIME_FORMATTER));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return LocalDate.now();
		}

		date = fileDateTime.toLocalDate();
		if (adjust) {
			LocalTime cutoffTime = LocalTime.of(18, 0);
			if (fileDateTime.toLocalTime().isBefore(cutoffTime)) {
				date = date.minusDays(1);
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

	public void updateDownloadFilenames(String downloadFilenamePrefix) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss.SSS");

		try (Stream<Path> stream = Files.list(Paths.get(basePath))) {

			List<String> filenames = stream.filter(p -> p.getFileName().toString().startsWith(downloadFilenamePrefix))
					.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
			for (String filename : filenames) {
				LocalDate date = getDownloadFileDate(filename, false);

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



}
