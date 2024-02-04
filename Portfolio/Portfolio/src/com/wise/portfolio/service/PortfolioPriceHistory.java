package com.wise.portfolio.service;

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
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.wise.portfolio.fund.Fund;
import com.wise.portfolio.fund.FundPriceHistory;
import com.wise.portfolio.fund.MutualFund.FundCategory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;

public class PortfolioPriceHistory {

	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");
	public static final int CURRENCY_SCALE = 6;

	public LocalDate getFundStart(String symbol) {
		return Collections.min(vanguardPriceHistory.get(symbol).getFundPricesMap().keySet());
	}

	private TreeMap<String, FundPriceHistory> vanguardPriceHistory = new TreeMap<String, FundPriceHistory>();

	public TreeMap<String, FundPriceHistory> getVanguardPriceHistory() {
		return vanguardPriceHistory;
	}

	public void setVanguardPriceHistory(TreeMap<String, FundPriceHistory> vanguardPriceHistory) {
		this.vanguardPriceHistory = vanguardPriceHistory;
	}

	private TreeMap<String, FundPriceHistory> alphaVantagePriceHistory = new TreeMap<String, FundPriceHistory>();

	public TreeMap<String, FundPriceHistory> getAlphaVantagePriceHistory() {
		return alphaVantagePriceHistory;
	}

	public void setAlphaVantagePriceHistory(TreeMap<String, FundPriceHistory> alphaVantagePriceHistory) {
		this.alphaVantagePriceHistory = alphaVantagePriceHistory;
	}

	private Map<String, Map<LocalDate, Double>> fundShares = new TreeMap<>();
//	private Map<String, Map<LocalDate, BigDecimal>> fundPrices = new TreeMap<>();
	private Map<String, Map<LocalDate, String>> fundPricesSource = new TreeMap<>();
	private Map<String, Map<LocalDate, BigDecimal>> fundReturnsByDateMap = new TreeMap<>();

	public Map<String, Map<LocalDate, BigDecimal>> getFundReturnsMap() {
		return fundReturnsByDateMap;
	}

	private Map<String, Pair<LocalDate, BigDecimal>> fundsMaxPriceMap = new HashMap<>();
	private Map<String, Pair<LocalDate, BigDecimal>> fundsMinPriceMap = new HashMap<>();

	private Map<FundCategory, BigDecimal> desiredCategoryAllocation = new HashMap<>();

	private Map<String, Map<FundCategory, BigDecimal>> desiredFundAllocation = new HashMap<>();

	private LocalDate oldestDate = LocalDate.now();

	public LocalDate getOldestDate() {
		return oldestDate;
	}

	public long getDaysSinceOldestDate() {
		return oldestDate.until(LocalDate.now(), ChronoUnit.DAYS);
	}

	public void setOldestDate(LocalDate date) {
		this.oldestDate = date;
	}

	public LocalDate getMostRecentDay() {
		return mostRecentDay;
	}

	private LocalDate mostRecentDay = LocalDate.of(2000, 1, 1);

	public Map<String, Map<LocalDate, Double>> getFundShares() {
		return fundShares;
	}

	public void setFundShares(Map<String, Map<LocalDate, Double>> fundShares) {
		this.fundShares = fundShares;
	}

	public Map<String, Pair<LocalDate, BigDecimal>> getFundsMaxPrice() {
		return fundsMaxPriceMap;
	}

	public void setFundsMaxPrice(Map<String, Pair<LocalDate, BigDecimal>> fundsMaxPrice) {
		this.fundsMaxPriceMap = fundsMaxPrice;
	}

	public Map<String, Pair<LocalDate, BigDecimal>> getFundsMinPrice() {
		return fundsMinPriceMap;
	}

	public void setFundsMinPrice(Map<String, Pair<LocalDate, BigDecimal>> fundsMinPrice) {
		this.fundsMinPriceMap = fundsMinPrice;
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

		if (date.isBefore(oldestDate)) {
			setOldestDate(date);
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

		for (List<String> fundValues : fundsValues) {
			if (fundValues.size() < 6) {
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
			String priceString = fundValues.get(4);
			if (priceString == null) {
				System.out.println("price is null");
				continue;
			}
			BigDecimal price = new BigDecimal(0);
			try {
				price = new BigDecimal(priceString);
			} catch (Exception e) {
				System.out.println("Exception converting price to decimal:  " + e.getMessage());
				continue;
			}

			PortfolioFund fund = funds.get(symbol);
			if (fund == null) {
				fund = new PortfolioFund();
				fund.setSymbol(symbol);
				fund.setName(portfolio.getFundName(symbol));
				funds.put(symbol, fund);
			}
			Double shares = Float.valueOf(fundValues.get(3)) + fund.getShares();
			fund.setShares(shares);
			fund.setCurrentPrice(price, date);
			addFundPrice(symbol, date, price, downloadFile);
			FundPriceHistory fundPriceHistory = vanguardPriceHistory.get(symbol);
			if (fundPriceHistory == null) {
				fundPriceHistory = new FundPriceHistory(symbol);
				vanguardPriceHistory.put(symbol, fundPriceHistory);
			}
			fundPriceHistory.addFundPrice(date, price);
			addFundShares(symbol, date, shares, downloadFile);

		}

		try {
			// Read transactions into list of transactions per fund (lines with 10
			// entries)
			List<List<String>> currentFundsTransactions = null;
			try (BufferedReader br = Files.newBufferedReader(Paths.get(downloadFile), PortfolioService.INPUT_CHARSET)) {
				currentFundsTransactions = br.lines().map(line -> Arrays.asList(line.split(",")))
						.filter(line -> line.size() == 14).collect(Collectors.toList());

			} catch (Exception e) {
				System.out.println("Invalid file:  " + downloadFile + "Exception: " + e.getMessage());
			}
			if (currentFundsTransactions != null && currentFundsTransactions.size() > 0) {

				// Remove headings line
				currentFundsTransactions.remove(0);

				for (List<String> fundTransaction : currentFundsTransactions) {
					PortfolioFund fund = null;
					String transactionType = fundTransaction.get(3);
					String fundSymbol = fundTransaction.get(6);
//					System.out.println("Transaction type:  " + transactionType);
					if (!transactionType.contains("Withholding")) {

						if (fundSymbol == null || fundSymbol.length() == 0) {
							continue;
						}
						fund = funds.get(fundSymbol);
						if (fund == null) {
							continue;
						}
					}

					LocalDate tradeDate;
					try {
						tradeDate = LocalDate.parse(fundTransaction.get(1), DateTimeFormatter.ofPattern("M/d/yyyy"));
					} catch (Exception e) {
						try {
							tradeDate = LocalDate.parse(fundTransaction.get(1),
									DateTimeFormatter.ofPattern("yyyy-MM-dd"));
						} catch (Exception e1) {
							System.out.println("Exception processing transactions:  " + e);
							continue;
						}
					}
					Float transactionShares = Float.valueOf(fundTransaction.get(7));
					BigDecimal transactionSharePrice = new BigDecimal(fundTransaction.get(8));
					BigDecimal principalAmount = new BigDecimal(fundTransaction.get(9));

					if (transactionType.startsWith("Reinvestment") && fund != null) {
						fund.addDistribution(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}

					if ((transactionType.contains("Dividend") || transactionType.contains("Capital gain"))
							&& fund != null) {
						fund.addIncome(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}

					// There can be two Sell transactions on consecutive days, maybe because price
					// was adjusted?
					// no way to distinguish (except maybe amount is equal) and doesn't always
					// happen
					if (transactionType.equals("Sell") && fund != null) {
						fund.addWithdrawal(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}
					if (transactionType.contains("exchange") && fund != null) {
						fund.addExchange(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);
					}
					if (transactionType.contains("Share Conversion") && fund != null) {
						fund.addShareConversion(tradeDate, transactionType, transactionShares, transactionSharePrice,
								principalAmount, downloadFile);

					}
					if (transactionType.contains("Withholding (Federal)")) {
						portfolio.addFederalWithholdingTax(tradeDate, transactionType, transactionShares,
								transactionSharePrice, principalAmount, downloadFile);
					}
					if (transactionType.contains("Withholding (State)")) {
						portfolio.addStateWithholdingTax(tradeDate, transactionType, transactionShares,
								transactionSharePrice, principalAmount, downloadFile);
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
					addFundPrice(symbol, date, price, historyFile);
				}
			}

		}

		portfolio.setFundMap(funds);

	}

	public void loadAlphaPriceHistoryFile(Portfolio portfolio, String basePath, String historyFile) throws IOException {

		Path historyFilePath = Paths.get(basePath, historyFile);
		if (!Files.exists(historyFilePath, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}

		Map<String, PortfolioFund> funds = portfolio.getFundMap();
		if (funds == null) {
			return;
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
					addAlphaVantagePrice(symbol, date, price);
				}
			}

		}

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

				Double shares = (double) 0;
				if (priceString.length() > 0) {
					shares = Double.valueOf(priceString);
				}

				if (shares > 0) {
					addFundShares(symbol, date, shares, sharesFile);
				}
			}

		}

		portfolio.setFundMap(funds);

	}

	private void addFundPriceSource(String symbol, LocalDate date, String source) {
		Map<LocalDate, String> fundPriceSourceMap = fundPricesSource.get(symbol);
		if (fundPriceSourceMap == null) {
			fundPriceSourceMap = new HashMap<>();
			fundPricesSource.put(symbol, fundPriceSourceMap);
		}

		fundPriceSourceMap.put(date, source);

	}

	public void addFundPrice(String symbol, LocalDate date, BigDecimal price, String source) {

		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		price = price.setScale(CURRENCY_SCALE, RoundingMode.HALF_DOWN);

		if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
			date = date.minusDays(1);
		}
		if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			date = date.minusDays(2);
		}
		FundPriceHistory fundPriceHistory = vanguardPriceHistory.get(symbol);
		if (fundPriceHistory == null) {
			fundPriceHistory = new FundPriceHistory(symbol);
			vanguardPriceHistory.put(symbol, fundPriceHistory);
		}
		Map<LocalDate, BigDecimal> fundPriceMap = fundPriceHistory.getFundPricesMap();

		// Check if price exists for date (alternate source)
		BigDecimal existingPrice = fundPriceMap.get(date);
		if (existingPrice != null) {
			// Don't add second price for same date
			return;
		}

		fundPriceMap.put(date, price);
		addFundPriceSource(symbol, date, source);

		updatePriceMinMaxPrices(symbol, date, price);

	}

	public void addFundShares(String symbol, LocalDate date, Double shares, String source) {

		Map<LocalDate, Double> fundShareMap = fundShares.get(symbol);
		if (fundShareMap == null) {
			fundShareMap = new HashMap<>();
			fundShares.put(symbol, fundShareMap);
		}
		fundShareMap.put(date, shares);
		addFundPriceSource(symbol, date, source);

	}

	public Pair<LocalDate, BigDecimal> getMaxPrice(PortfolioFund fund) {

		Pair<LocalDate, BigDecimal> maxPricePair = fundsMaxPriceMap.get(fund.getSymbol());
		if (maxPricePair == null) {
			maxPricePair = Pair.of(LocalDate.now(), BigDecimal.ZERO);
		}
		return maxPricePair;
	}

	public Pair<LocalDate, BigDecimal> getMinPrice(PortfolioFund fund) {

		Pair<LocalDate, BigDecimal> minPricePair = fundsMinPriceMap.get(fund.getSymbol());
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

			// saved max price is older than requested date
			maxPrice = Pair.of(LocalDate.now(), BigDecimal.ZERO);

			Map<LocalDate, BigDecimal> prices = vanguardPriceHistory.get(fund.getSymbol()).getFundPricesMap();
			for (Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {

				if (entry.getKey().isAfter(date) && entry.getValue().compareTo(maxPrice.getRight()) > 0) {
					maxPrice = Pair.of(entry.getKey(), entry.getValue());
				}
			}
			FundPriceHistory fundPriceHistory = alphaVantagePriceHistory.get(fund.getSymbol());
			if (fundPriceHistory != null) {

				prices = fundPriceHistory.getFundPricesMap();
				for (Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {
					if (entry.getKey().isAfter(date) && entry.getValue().compareTo(maxPrice.getRight()) > 0)
						maxPrice = Pair.of(entry.getKey(), entry.getValue());
				}
			}
		}
		return maxPrice;
	}

	public Pair<LocalDate, BigDecimal> getMinPriceFromDate(PortfolioFund fund, LocalDate date) {

		Pair<LocalDate, BigDecimal> minPrice = fundsMinPriceMap.get(fund.getSymbol());
		if (minPrice.getLeft().isBefore(date)) {
			// saved min price is older than datemust iterate to find
			minPrice = Pair.of(LocalDate.now(), new BigDecimal(10000));

			Map<LocalDate, BigDecimal> prices = vanguardPriceHistory.get(fund.getSymbol()).getFundPricesMap();
			for (Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {
				if (entry.getKey().isAfter(date) && entry.getValue().compareTo(minPrice.getRight()) < 0)
					minPrice = Pair.of(entry.getKey(), entry.getValue());
			}
			FundPriceHistory fundPriceHistory = alphaVantagePriceHistory.get(fund.getSymbol());
			if (fundPriceHistory != null) {
				prices = fundPriceHistory.getFundPricesMap();
				for (Entry<LocalDate, BigDecimal> entry : prices.entrySet()) {
					if (entry.getKey().isAfter(date) && entry.getValue().compareTo(minPrice.getRight()) < 0)
						minPrice = Pair.of(entry.getKey(), entry.getValue());
				}
			}
		}
		return minPrice;
	}

	public BigDecimal getPriceByDate(Fund fund, LocalDate date, boolean isExactDate) {

		Map<LocalDate, BigDecimal> fundPriceMap = vanguardPriceHistory.get(fund.getSymbol()).getFundPricesMap();

		BigDecimal value = fundPriceMap.get(date);
		if (value == null && !isExactDate) {
			int tries = 5;
			while (tries-- > 0) {
				date = date.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(date);
				if (value != null) {
					return value;
				}
			}
		}
		if (value == null) {
			FundPriceHistory fundPriceHistory = alphaVantagePriceHistory.get(fund.getSymbol());
			if (fundPriceHistory != null) {
				fundPriceMap = fundPriceHistory.getFundPricesMap();
				value = fundPriceMap.get(date);
			}

		}

		return value;
	}

	public Double getSharesByDate(Fund fund, LocalDate date, boolean isExactDate) {
		Double value = (double) 0;

		Map<LocalDate, Double> fundPriceMap = fundShares.get(fund.getSymbol());
		value = fundPriceMap.get(date);
		if (value == null && !isExactDate) {
			int tries = 5;
			while (tries-- > 0) {
				date = date.minus(1, ChronoUnit.DAYS);
				value = fundPriceMap.get(date);
				if (value != null) {
					return value;
				}
			}
			return (double) 0;
		}

		return value;
	}

	public BigDecimal getFundValueByDate(Fund fund, LocalDate date, boolean isExactDate) {

		BigDecimal value = null;

		BigDecimal price = getPriceByDate(fund, date, isExactDate);
		Double shares = getSharesByDate(fund, date, isExactDate);

		if (price != null && shares != null && shares.compareTo((double) 0) > 0) {
			value = price.multiply(new BigDecimal(shares));
		}

		return value;
	}

	public BigDecimal getPortfolioValueByDate(Portfolio portfolio, LocalDate date, boolean isExactDate) {

		BigDecimal portfolioValue = BigDecimal.ZERO;
		for (PortfolioFund fund : portfolio.getFundMap().values()) {

			BigDecimal fundValue = BigDecimal.ZERO;

			BigDecimal fundPrice = getPriceByDate(fund, date, isExactDate);
			Double fundShares = getSharesByDate(fund, date, isExactDate);

			if (fundPrice != null && fundShares != null) {
				fundValue = fundPrice.multiply(new BigDecimal(fundShares));
			}
			if (fundValue.compareTo(BigDecimal.ZERO) == 0 && !isExactDate) {
				int tries = 5;
				while (tries-- > 0) {
					date = date.minus(1, ChronoUnit.DAYS);
					fundPrice = getPriceByDate(fund, date, isExactDate);
					fundShares = getSharesByDate(fund, date, isExactDate);
					if (fundPrice != null && fundShares != null) {
						fundValue = fundPrice.multiply(new BigDecimal(fundShares));
						if (fundValue.compareTo(BigDecimal.ZERO) > 0) {
							break;
						}
					}
				}
			}
			portfolioValue = portfolioValue.add(fundValue);
		}
		return portfolioValue;
	}

	public BigDecimal getPriceByDate(String symbol, LocalDate date) {
		BigDecimal fundPrice = vanguardPriceHistory.get(symbol).getPriceByDate(date);
		if (fundPrice == null) {
			FundPriceHistory fundPriceHistory = alphaVantagePriceHistory.get(symbol);
			if (fundPriceHistory != null) {
				fundPrice = fundPriceHistory.getPriceByDate(date);
			}
		}
		return fundPrice;

	}

	public Set<LocalDate> getAllDates() {
		TreeSet<LocalDate> set = new TreeSet<>();
		for (FundPriceHistory history : vanguardPriceHistory.values()) {
			set.addAll(history.getFundPricesMap().keySet());
		}
		for (FundPriceHistory history : alphaVantagePriceHistory.values()) {
			set.addAll(history.getFundPricesMap().keySet());
		}
		return set;
	}

	public void addAlphaVantagePrice(String symbol, LocalDate date, BigDecimal closingPrice) {

		FundPriceHistory fundPriceHistory = alphaVantagePriceHistory.get(symbol);
		if (fundPriceHistory == null) {
			fundPriceHistory = new FundPriceHistory(symbol);
			alphaVantagePriceHistory.put(symbol, fundPriceHistory);
		}
		fundPriceHistory.addFundPrice(date, closingPrice);
		updatePriceMinMaxPrices(symbol, date, closingPrice);
		if (date.isBefore(oldestDate)) {
			setOldestDate(date);
		}
		if (date.isAfter(mostRecentDay)) {
			mostRecentDay = date;
		}

	}

	private void updatePriceMinMaxPrices(String symbol, LocalDate date, BigDecimal price) {
		// Update price maximum
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

		// Update price minimum
		Pair<LocalDate, BigDecimal> fundMinPriceMap = fundsMinPriceMap.get(symbol);
		if (fundMinPriceMap == null) {
			// Initialize price map for fund
			fundMinPriceMap = Pair.of(LocalDate.now(), new BigDecimal(1000));
			fundsMinPriceMap.put(symbol, fundMinPriceMap);
		}
		Pair<LocalDate, BigDecimal> lastMinPrice = fundsMinPriceMap.get(symbol);
		if (price.compareTo(lastMinPrice.getRight()) < 0) {
			fundsMinPriceMap.put(symbol, Pair.of(date, price));
		}

	}

}
