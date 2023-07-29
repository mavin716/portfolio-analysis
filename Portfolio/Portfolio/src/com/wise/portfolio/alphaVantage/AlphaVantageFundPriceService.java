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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wise.portfolio.portfolio.Portfolio;
import com.wise.portfolio.service.PortfolioPriceHistory;

public class AlphaVantageFundPriceService {

	private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
	private static final String ALPHA_VANTAGE_APIKEY = "85MODZ3M0IN6CT0R";

	public static boolean loadFundHistoryFromAlphaVantage(Portfolio portfolio, String symbol, boolean retry)
			throws IOException {

		LocalDate earliestAlphaVantageDate = LocalDate.now();

		BigDecimal closingPrice = BigDecimal.ZERO;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String url = ALPHA_VANTAGE_URL + "?function=TIME_SERIES_MONTHLY&outputsize=compact&symbol=" + symbol
				+ "&apikey=" + ALPHA_VANTAGE_APIKEY + "&outputsize=full";
		HttpGet httpget = new HttpGet(url);
		try {
			HttpResponse httpresponse = httpclient.execute(httpget);
			System.out.println(httpresponse.getStatusLine());
			if (httpresponse.getStatusLine().getStatusCode() != 200) {
				System.out.println("Sleep for 10 seconds and try again");
				Thread.sleep(10000);
				boolean success = loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
				return success;
			}

			StringBuffer response = new StringBuffer();
			Scanner sc = new Scanner(httpresponse.getEntity().getContent());
			while (sc.hasNext()) {
				response.append(sc.nextLine());
			}
			sc.close();

			// json object mapper
			ObjectMapper mapper = new ObjectMapper();
			SimpleModule module = new SimpleModule();
			module.addDeserializer(Series.class, new SeriesDeserializer());
			mapper.registerModule(module);

			Series series = mapper.readValue(response.toString(), Series.class);
			if (series.getMetadata() == null) {
				boolean success = false;
				System.out.println(response.toString());
				if (retry) {
					int tries = 7;
					while (tries-- > 0 && !success) {
						System.out.println("Sleep for 10 seconds and try again");
						Thread.sleep(10000);
						System.out.println("retries left:  " + tries);
						success = loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
					}
					if (tries <= 0 && !success) {
						System.out.println("Exhausted retries");
						return false;
					}
				}
				return success;
			}
			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			LocalDate oldestDate = priceHistory.getOldestDate();
			for (Entry<String, TimeSeries> entry : series.getTimeSeries().entrySet()) {

				LocalDate date = LocalDate.parse(entry.getKey());
				
				// Only use dates BEFORE download files (they are inconsistent with downloads and ruin the integrity
				if (date.isAfter(oldestDate)) {
					continue;
				}
				
				if (date.isBefore(earliestAlphaVantageDate)) {
					earliestAlphaVantageDate = date;
				}

				TimeSeries timeSeries = entry.getValue();
				closingPrice = new BigDecimal(timeSeries.getClose());
//				System.out.println("date:  " + date.toString() + " closing price:  "
//						+ CurrencyHelper.formatAsCurrencyString(closingPrice));

				priceHistory.addFundPrice(symbol, date, closingPrice, "AlphaVantage Service");

			}

			httpclient.close();
//			if (earliestAlphaVantageDate.isBefore(priceHistory.getOldestDate())) {
//				priceHistory.setOldestDate(earliestAlphaVantageDate);
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

}
