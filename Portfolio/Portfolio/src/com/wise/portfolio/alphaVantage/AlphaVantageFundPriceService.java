package com.wise.portfolio.alphaVantage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wise.portfolio.fund.FundPriceHistory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.service.AppProperties;
import com.wise.portfolio.service.PortfolioPriceHistory;

public class AlphaVantageFundPriceService {


	protected static final Logger logger = LogManager.getLogger(AlphaVantageFundPriceService.class);

	public static boolean retrieveFundPriceHistoryFromAlphaVantage(Portfolio portfolio, String symbol, boolean retry)
			throws IOException {

		logger.debug("Retrieve prices from AlphaVantage API for:  " + portfolio.getFundName(symbol));
		LocalDate earliestAlphaVantageDate = LocalDate.now();

		BigDecimal closingPrice = BigDecimal.ZERO;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String url = AppProperties.getProperty("alphaVantageUrl") + "?function="
				+ AppProperties.getProperty("alphaVantageFunction") + "&outputsize=compact&symbol=" + symbol
				+ "&apikey=" + AppProperties.getProperty("alphaVantageApiKey") + "&outputsize=full";
		try {
			HttpGet httpget = new HttpGet(url);
			HttpResponse httpresponse = httpclient.execute(httpget);
			if (httpresponse.getStatusLine().getStatusCode() != 200) {
				logger.error("Error retrieving prices for fund:  " + portfolio.getFundName(symbol) + " http response:  "
						+ httpresponse.getStatusLine());
				return false;
			}

			StringBuffer response = new StringBuffer();
			Scanner sc = new Scanner(httpresponse.getEntity().getContent());
			while (sc.hasNext()) {
				response.append(sc.nextLine());
			}
			logger.debug("response " + response.substring(0, response.length() > 80 ? 80 : response.length()));
			sc.close();
			httpclient.close();

			// Convert data into series of objects
			Series series = convertResponseIntoSeries(response);
			if (series == null || series.getMetadata() == null) {
				boolean success = false;
				if (retry) {
					int tries = 2;
					while (tries-- > 0 && !success) {
						System.out.println("Sleep for 10 seconds and try again");
						Thread.sleep(10000);
						logger.debug("retries left:  " + tries);
						success = retrieveFundPriceHistoryFromAlphaVantage(portfolio, symbol, false);
					}
					if (tries <= 0 && !success) {
						logger.warn("Exhausted retries");
						return false;
					}
				}
				logger.warn("Unsuccessful "  + portfolio.getFund(symbol).getShortName() + " response:  " + response.toString());
				return success;
			}
			LocalDate mostRecentDate = null;
			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			PortfolioFund fund = portfolio.getFund(symbol);
			for (Entry<String, TimeSeries> entry : series.getTimeSeries().entrySet()) {

				LocalDate date = LocalDate.parse(entry.getKey());

				if (mostRecentDate == null) {
					mostRecentDate = date;

					fund.setCurrentPrice(closingPrice, mostRecentDate);
					logger.debug(fund.getShortName() + "most recent date:  " + mostRecentDate);
				}

				if (date.isBefore(earliestAlphaVantageDate)) {
					earliestAlphaVantageDate = date;
				}

				TimeSeries timeSeries = entry.getValue();
				closingPrice = new BigDecimal(timeSeries.getClose());

				FundPriceHistory fundPriceHistory = priceHistory.getAlphaVantagePriceHistory().get(symbol);
				if (fundPriceHistory == null) {
					fundPriceHistory = new FundPriceHistory(symbol);
					priceHistory.getAlphaVantagePriceHistory().put(symbol, fundPriceHistory);
				}
				priceHistory.addAlphaVantagePrice(symbol, date, closingPrice);

			}
			logger.debug(fund.getShortName() + "oldest date:  " + earliestAlphaVantageDate);

//			if (earliestAlphaVantageDate.isBefore(priceHistory.getOldestDate())) {
//				priceHistory.setOldestDate(earliestAlphaVantageDate);
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	private static Series convertResponseIntoSeries(StringBuffer response) {
		Series series = null;

		// json object mapper
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Series.class, new SeriesDeserializer());
		mapper.registerModule(module);

		try {
			series = mapper.readValue(response.toString(), Series.class);
		} catch (JsonProcessingException e) {
			logger.error("Exception processing http response:  " + e.getLocalizedMessage(), e);
		}
		return series;
	}

}
