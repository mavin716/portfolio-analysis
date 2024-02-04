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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wise.portfolio.fund.FundPriceHistory;
import com.wise.portfolio.fund.PortfolioFund;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.service.PortfolioPriceHistory;

public class AlphaVantageFundPriceService {

	private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
//	private static final String ALPHA_VANTAGE_FUNCTION = "TIME_SERIES_MONTHLY";
	private static final String ALPHA_VANTAGE_FUNCTION = "TIME_SERIES_DAILY";
	private static final String ALPHA_VANTAGE_APIKEY = "85MODZ3M0IN6CT0R";

	public static boolean retrieveFundHistoryFromAlphaVantage(Portfolio portfolio, String symbol, boolean retry)
			throws IOException {

		LocalDate earliestAlphaVantageDate = LocalDate.now();

		BigDecimal closingPrice = BigDecimal.ZERO;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String url = ALPHA_VANTAGE_URL + "?function=" + ALPHA_VANTAGE_FUNCTION + "&outputsize=compact&symbol=" + symbol
				+ "&apikey=" + ALPHA_VANTAGE_APIKEY + "&outputsize=full";
		try {
			HttpGet httpget = new HttpGet(url);
			HttpResponse httpresponse = httpclient.execute(httpget);
			System.out.println(httpresponse.getStatusLine());
			if (httpresponse.getStatusLine().getStatusCode() != 200) {
				return false;
			}

			StringBuffer response = new StringBuffer();
			Scanner sc = new Scanner(httpresponse.getEntity().getContent());
			while (sc.hasNext()) {
				response.append(sc.nextLine());
			}
			System.out.println("response " + response.substring(0, 50));
			sc.close();
			httpclient.close();

			// Convert data into series of objects
			Series series = convertResponseIntoSeries(response);
			if (series == null || series.getMetadata() == null) {
				boolean success = false;
				if (retry) {
					int tries = 3;
					while (tries-- > 0 && !success) {
						System.out.println("Sleep for 10 seconds and try again");
						Thread.sleep(10000);
						System.out.println("retries left:  " + tries);
						success = retrieveFundHistoryFromAlphaVantage(portfolio, symbol, false);
					}
					if (tries <= 0 && !success) {
						System.out.println("Exhausted retries");
						return false;
					}
				}
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
					System.out.println(fund.getShortName() + "most recent date:  " + mostRecentDate);
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
			System.out.println(fund.getShortName() + "oldest date:  " + earliestAlphaVantageDate);

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
			System.out.println("Exception processing http response:  " + e.getLocalizedMessage());
		}
		return series;
	}

}
