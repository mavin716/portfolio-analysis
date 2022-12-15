package com.wise.portfolio.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.wise.portfolio.data.MutualFund.FundCategory;

public class PortfolioPriceHistory {

	public LocalDate getFundStart(String symbol) {
		return Collections.min(fundPrices.get(symbol).keySet());
	}

	private Map<String, Map<LocalDate, Float>> fundShares = new HashMap<>();
	private Map<String, Map<LocalDate, BigDecimal>> fundPrices = new HashMap<>();
	private Map<String, Map<LocalDate, BigDecimal>> fundReturns = new HashMap<>();
	private Map<String, Map<LocalDate, BigDecimal>> fundBalanceByDate = new HashMap<>();

	public Map<String, Map<LocalDate, BigDecimal>> getFundReturns() {
		return fundReturns;
	}

	private Map<String, Pair<LocalDate, BigDecimal>> fundsMaxPriceMap = new HashMap<>();
	private Map<String, Pair<LocalDate, BigDecimal>> fundsMinPrice = new HashMap<>();

	private Map<FundCategory, BigDecimal> desiredCategoryAllocation = new HashMap<>();

	private Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocation = new HashMap<>();

	private LocalDate oldestDay = LocalDate.now();

	public LocalDate getOldestDay() {
		return oldestDay;
	}

	public void setOldestDay(LocalDate oldestDay) {
		this.oldestDay = oldestDay;
	}

	public LocalDate getMostRecentDay() {
		return mostRecentDay;
	}

	public void setMostRecentDay(LocalDate mostRecentDay) {
		this.mostRecentDay = mostRecentDay;
	}

	private LocalDate mostRecentDay = LocalDate.of(2000, 1, 1);

	public Map<String, Map<LocalDate, BigDecimal>> getFundPrices() {
		return fundPrices;
	}

	public Map<String, Map<LocalDate, Float>> getFundShares() {
		return fundShares;
	}

	public void setFundShares(Map<String, Map<LocalDate, Float>> fundShares) {
		this.fundShares = fundShares;
	}

	public Map<String, Pair<LocalDate, BigDecimal>> getFundsMaxPrice() {
		return fundsMaxPriceMap;
	}

	public void setFundsMaxPrice(Map<String, Pair<LocalDate, BigDecimal>> fundsMaxPrice) {
		this.fundsMaxPriceMap = fundsMaxPrice;
	}

	public Map<String, Pair<LocalDate, BigDecimal>> getFundsMinPrice() {
		return fundsMinPrice;
	}

	public void setFundsMinPrice(Map<String, Pair<LocalDate, BigDecimal>> fundsMinPrice) {
		this.fundsMinPrice = fundsMinPrice;
	}

	public Map<FundCategory, BigDecimal> getDesiredCategoryAllocation() {
		return desiredCategoryAllocation;
	}

	public void setDesiredCategoryAllocation(Map<FundCategory, BigDecimal> desiredCategoryAllocation) {
		this.desiredCategoryAllocation = desiredCategoryAllocation;
	}

	public Map<String, Map<FundCategory, BigDecimal>> getDesiredFundAllocation() {
		return desiredFundAllocation;
	}

	public void setDesiredFundAllocation(Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocation) {
		this.desiredFundAllocation = desiredFundAllocation;
	}

	public void loadPortfolioDownloadFile(Portfolio portfolio, LocalDate date, String downloadFile) throws IOException {

		final int NUM_COLUMNS = 6;
		final String HEADING = "Investment Name";

		if (!Files.exists(Paths.get(downloadFile), LinkOption.NOFOLLOW_LINKS)) {
			return;
		}

		if (date.isBefore(oldestDay)) {
			oldestDay = date;
		}
		if (date.isAfter(mostRecentDay)) {
			mostRecentDay = date;
		}
		Map<String, PortfolioFund> funds = portfolio.getFundMap();
		if (funds == null) {
			funds = new HashMap<>();
		}
		// initialize shares
		for (Entry<String, PortfolioFund> entry : funds.entrySet()) {
			PortfolioFund fund = entry.getValue();
			fund.setShares(0);
		}

		// Read fund value lines into list of values of funds (verify correct number
		// columns)
		List<List<String>> fundsValues = null;
		try (BufferedReader br = Files.newBufferedReader(Paths.get(downloadFile), PortfolioService.INPUT_CHARSET)) {
			String firstLine = br.readLine(); // first line is headings
			if (firstLine == null) {
				System.out.println("WARNING:  empty file:  " + downloadFile);
				return;
			}

			fundsValues = br.lines().map(line -> Arrays.asList(line.split(",")))
					.filter(line -> line.size() == NUM_COLUMNS).collect(Collectors.toList());
		} catch (Exception e) {
			System.out.println("Exception processing lines from downloadfile: " + e);
			return;
		}

		// Read transactions into list of transactions per fund (lines with 10
		// entries)
		List<List<String>> currentFundsTransactions = null;
		try (BufferedReader br = Files.newBufferedReader(Paths.get(downloadFile), PortfolioService.INPUT_CHARSET)) {
			currentFundsTransactions = br.lines().map(line -> Arrays.asList(line.split(",")))
					.filter(line -> line.size() == 14).collect(Collectors.toList());

		} catch (Exception e) {
			System.out.println("Invalid file:  " + downloadFile + "Exception: " + e.getMessage());
		}

		for (List<String> fundValues : fundsValues) {
			if (fundValues.size() != 6) {
				continue;
			}

			String symbol = fundValues.get(2);
			if (symbol == null) {
				System.out.println("symbol is null");
				continue;
			}
			String name = fundValues.get(1);
			if (name.contentEquals(HEADING)) {
				continue;
			}
			if (fundValues.get(4) == null) {
				continue;
			}
			BigDecimal price = new BigDecimal(0);
			try {
				price = new BigDecimal(fundValues.get(4));
			} catch (Exception e) {
				System.out.println("Exception converting price to decimal:  " + e.getMessage());
				continue;
			}

			PortfolioFund fund = funds.get(symbol);
			if (fund == null) {
				fund = new PortfolioFund();
				fund.setSymbol(symbol);
				fund.setName(PortfolioService.fundSymbolNameMap.get(symbol));
			}
			Double shares = Float.valueOf(fundValues.get(3)) + fund.getShares();
			fund.setShares(shares);
			fund.setCurrentPrice(price);
			addFundPrice(symbol, date, price);
			addFundShares(symbol, date, new Float(shares));
			funds.put(symbol, fund);

		}

		try {
			if (currentFundsTransactions != null && currentFundsTransactions.size() > 0) {

				// Remove headings line
				currentFundsTransactions.remove(0);

				for (List<String> fundTransaction : currentFundsTransactions) {
					String transactionType = fundTransaction.get(3);
					String fundSymbol = fundTransaction.get(6);
					if (fundSymbol == null || fundSymbol.length() == 0) {
						continue;
					}
					PortfolioFund fund = funds.get(fundSymbol);
					if (fund == null) {
						continue;
					}

					LocalDate tradeDate = LocalDate.parse(fundTransaction.get(1),
							DateTimeFormatter.ofPattern("M/d/yyyy"));
					Float transactionShares = Float.valueOf(fundTransaction.get(7));
					BigDecimal transactionSharePrice = new BigDecimal(fundTransaction.get(8));
					BigDecimal principalAmount = new BigDecimal(fundTransaction.get(9));

					if (transactionType.startsWith("Reinvestment")) {
						fund.addDistribution(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}

					// There can be two Sell transactions on consecutive days, maybe because price
					// was adjusted?
					// no way to distinguish (except maybe amount is equal) and doesn't always
					// happen
					if (transactionType.equals("Sell")) {
						fund.addWithdrawal(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}
					if (transactionType.contains("exchange")) {
						fund.addExchange(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}
					if (transactionType.contains("Share Conversion")) {
						fund.addShareConversion(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);

					}
				}
			}
		} catch (Exception e) {
			System.out.println("Exception processing transactions:  " + e);
		}
		portfolio.setFundMap(funds);

	}

	public void loadFundSharesHistoryFile(Portfolio portfolio, String basePath, String historyFile) throws IOException {

		Path historyFilePath = Paths.get(basePath, historyFile);
		if (!Files.exists(historyFilePath, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}

		Map<String, PortfolioFund> funds = portfolio.getFundMap();
		if (funds == null) {
			funds = new HashMap<>();
		}

		List<List<String>> fundLines = readHistoryCSVFile(historyFilePath);
		List<String> headingLine = fundLines.remove(0); // first line is headings
		if (headingLine == null) {
			System.out.println("WARNING:  empty file:  " + historyFile);
			return;
		}

		for (List<String> fundValues : fundLines) {
			int column = 0;
			String symbol = fundValues.get(column++);
			if (symbol == null) {
				System.out.println("symbol is null");
				continue;
			}
			;
			String name = fundValues.get(column++);

			PortfolioFund fund;
			if (funds.get(symbol) != null) {
				fund = funds.get(symbol);
			} else {
				fund = new PortfolioFund();
				fund.setSymbol(symbol);
				fund.setName(name);
			}

			int dateIndex = 2;
			while (column < fundValues.size()) {
				String priceString = fundValues.get(column++);
				String dateString = headingLine.get(dateIndex++);
				LocalDate date = LocalDate.parse(dateString);
				BigDecimal price = null;
				try {
					if (priceString.length() > 0) {
						price = new BigDecimal(priceString);
					}
				} catch (Exception e) {
					System.out.print("Excetpion converting price string to decimal");

				}

				if (price != null) {
					addFundPrice(symbol, date, price);
				}
			}

		}

		portfolio.setFundMap(funds);

	}

	private List<List<String>> readHistoryCSVFile(Path historyFilePath) {

		List<List<String>> fundLines = null;
		try (BufferedReader br = Files.newBufferedReader(historyFilePath, PortfolioService.INPUT_CHARSET)) {
			fundLines = br.lines().map(line -> Arrays.asList(line.split(","))).collect(Collectors.toList());
		} catch (Exception e) {
			System.out.println("Exception processing lines from downloadfile: " + e);
		}
		return fundLines;
	}

	public void loadPortfolioSharesFile(Portfolio portfolio, String basePath, String sharesFile) throws IOException {

		Path sharesFilePath = Paths.get(basePath, sharesFile);
		if (!Files.exists(sharesFilePath, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}

		Map<String, PortfolioFund> funds = portfolio.getFundMap();
		if (funds == null) {
			funds = new HashMap<>();
		}

		List<List<String>> fundLines = readHistoryCSVFile(sharesFilePath);
		List<String> headingLine = fundLines.remove(0); // first line is headings
		if (headingLine == null) {
			System.out.println("WARNING:  empty file:  " + sharesFilePath.getFileName());
			return;
		}

		for (List<String> fundValues : fundLines) {
			int column = 0;

			String symbol = fundValues.get(column++);
			if (symbol == null) {
				System.out.println("symbol is null");
				continue;
			}

			String name = fundValues.get(column++);

			PortfolioFund fund;
			if (funds.get(symbol) != null) {
				fund = funds.get(symbol);
			} else {
				fund = new PortfolioFund();
				fund.setSymbol(symbol);
				fund.setName(name);
			}

			int dateIndex = 2;
			while (column < fundValues.size()) {
				String dateString = headingLine.get(dateIndex++);
				LocalDate date = LocalDate.parse(dateString);
				String priceString = fundValues.get(column++);

				Float shares = 0f;
				if (priceString.length() > 0) {
					shares = Float.valueOf(priceString);
				}

				if (shares.compareTo(0f) > 0) {
					addFundShares(symbol, date, shares);
				}
			}

		}

		portfolio.setFundMap(funds);

	}

	public void addFundPrice(String symbol, LocalDate date, BigDecimal price) {

		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		price = price.setScale(4, RoundingMode.HALF_DOWN);
		
		if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			// filter out bad data????
			return;
		}
		Map<LocalDate, BigDecimal> fundPriceMap = fundPrices.get(symbol);
		if (fundPriceMap == null) {
			fundPriceMap = new HashMap<>();
			fundPrices.put(symbol, fundPriceMap);
		}

		BigDecimal existingPrice = fundPriceMap.get(date);
		if (existingPrice != null) {
			if (existingPrice.compareTo(price) != 0) {
				// Alpha Vantage service is returning different price than vanguard....  use vanguard download files...
				System.out.println(symbol + " " + date + " " + CurrencyHelper.formatAsCurrencyString(existingPrice) + "," + CurrencyHelper.formatAsCurrencyString(price));
			}
			// Don't add second price for same date
			return;
		}
		fundPriceMap.put(date, price);

		Pair<LocalDate, BigDecimal> fundMaxPriceMap = fundsMaxPriceMap.get(symbol);
		if (fundMaxPriceMap == null) {
			// Initialize price map for fund
			fundMaxPriceMap = Pair.of(LocalDate.now(), BigDecimal.ZERO);
			fundsMaxPriceMap.put(symbol, fundMaxPriceMap);
		}
		Pair<LocalDate, BigDecimal> lastMaxPrice = fundsMaxPriceMap.get(symbol);
		if (price.compareTo(lastMaxPrice.getRight()) > 0) {
			fundsMaxPriceMap.put(symbol, Pair.of(date, price));
		}

		Pair<LocalDate, BigDecimal> fundMinPriceMap = fundsMinPrice.get(symbol);
		if (fundMinPriceMap == null) {
			// Initialize price map for fund
			fundMinPriceMap = Pair.of(LocalDate.now(), new BigDecimal(1000));
			fundsMinPrice.put(symbol, fundMinPriceMap);
		}
		Pair<LocalDate, BigDecimal> lastMinPrice = fundsMinPrice.get(symbol);
		if (price.compareTo(lastMinPrice.getRight()) < 0) {
			fundsMinPrice.put(symbol, Pair.of(date, price));
		}

	}

	public void addFundShares(String symbol, LocalDate date, Float shares) {

		Map<LocalDate, Float> fundShareMap = fundShares.get(symbol);
		if (fundShareMap == null) {
			fundShareMap = new HashMap<>();
			fundShares.put(symbol, fundShareMap);
		}
		fundShareMap.put(date, shares);

	}

	public Pair<LocalDate, BigDecimal> getMaxPrice(PortfolioFund fund) {

		Pair<LocalDate, BigDecimal> maxPricePair = fundsMaxPriceMap.get(fund.getSymbol());
		if (maxPricePair == null) {
			maxPricePair = Pair.of(LocalDate.now(), BigDecimal.ZERO);
		}
		return maxPricePair;
	}

	public Pair<LocalDate, BigDecimal> getMinPrice(PortfolioFund fund) {

		Pair<LocalDate, BigDecimal> minPricePair = fundsMinPrice.get(fund.getSymbol());
		if (minPricePair == null) {
			minPricePair = Pair.of(LocalDate.now(), BigDecimal.ZERO);
		}
		return minPricePair;
	}

	public BigDecimal getPriceRange(PortfolioFund fund) {

		return getMaxPrice(fund).getRight().subtract(getMinPrice(fund).getRight());
	}

	public Pair<LocalDate, BigDecimal> getMaxPriceFromDate(PortfolioFund fund, LocalDate date) {

		Pair<LocalDate, BigDecimal> maxPrice = fundsMaxPriceMap.get(fund.getSymbol());
		if (maxPrice.getLeft().isBefore(date)) {
			// saved max price is older than date
			maxPrice = Pair.of(LocalDate.now(), BigDecimal.ZERO);
			// TODO Iterate through price from date finding max
			Map<LocalDate, BigDecimal> prices = fundPrices.get(fund.getSymbol());
			for (Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {
				if (entry.getKey().isAfter(date) && entry.getValue().compareTo(maxPrice.getRight()) > 0)
					maxPrice = Pair.of(entry.getKey(), entry.getValue());
			}
		}
		return maxPrice;
	}

	public Pair<LocalDate, BigDecimal> getMinPriceFromDate(PortfolioFund fund, LocalDate date) {

		Pair<LocalDate, BigDecimal> minPrice = fundsMinPrice.get(fund.getSymbol());
		if (minPrice.getLeft().isBefore(date)) {
			// saved min price is older than date
			minPrice = Pair.of(LocalDate.now(), new BigDecimal(10000));
			// TODO Iterate through price from date finding max
			Map<LocalDate, BigDecimal> prices = fundPrices.get(fund.getSymbol());
			for (Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {
				if (entry.getKey().isAfter(date) && entry.getValue().compareTo(minPrice.getRight()) < 0)
					minPrice = Pair.of(entry.getKey(), entry.getValue());
			}
		}
		return minPrice;
	}

	public BigDecimal getPriceByDate(Fund fund, LocalDate date, boolean isExactDate) {
		BigDecimal value = null;

		Map<LocalDate, BigDecimal> fundPriceMap = fundPrices.get(fund.getSymbol());
		value = fundPriceMap.get(date);
		if (value == null && !isExactDate) {
			int tries = 30;
			while (tries-- > 0) {
				date = date.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(date);
				if (value != null) {
					return value;
				}
			}
		}

		return value;
	}

	public Float getSharesByDate(Fund fund, LocalDate date, boolean isExactDate) {
		Float value = null;

		Map<LocalDate, Float> fundPriceMap = fundShares.get(fund.getSymbol());
		value = fundPriceMap.get(date);
		if (value == null && !isExactDate) {
			int tries = 30;
			while (tries-- > 0) {
				date = date.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(date);
				if (value != null) {
					return value;
				}
			}
		}

		return value;
	}

	public BigDecimal getFundValueByDate(Fund fund, LocalDate date, boolean isExactDate) {

		BigDecimal value = null;

		BigDecimal price = getPriceByDate(fund, date, isExactDate);
		Float shares = getSharesByDate(fund, date, isExactDate);

		if (price != null && shares != null && shares.compareTo(0f) > 0) {
			value = price.multiply(new BigDecimal(shares));
		}

		return value;
	}

	public BigDecimal getPortfolioValueByDate(Portfolio portfolio, LocalDate date, boolean isExactDate) {

		BigDecimal portfolioValue = BigDecimal.ZERO;
		for (PortfolioFund fund : portfolio.getFundMap().values()) {

			Map<LocalDate, BigDecimal> fundPriceMap = fundPrices.get(fund.getSymbol());
			BigDecimal value = fundPriceMap.get(date);
			if (value == null && !isExactDate) {
				int tries = 30;
				while (tries-- > 0) {
					date = date.minus(1, ChronoUnit.DAYS);
					value = fundPriceMap.get(date);
					if (value != null) {
						portfolioValue = portfolioValue.add(value);
					}
				}
			}
		}
		return portfolioValue;
	}

}
