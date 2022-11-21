package com.wise.portfolio.data;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.wise.portfolio.alphaVantage.data.Series;
import com.wise.portfolio.alphaVantage.data.SeriesDeserializer;
import com.wise.portfolio.alphaVantage.data.TimeSeries;

public class AlphaVantageFundPriceService {

	private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
	private static final String ALPHA_VANTAGE_APIKEY = "85MODZ3M0IN6CT0R";

	public static boolean loadFundHistoryFromAlphaVantage(Portfolio portfolio, String symbol, boolean retry) throws IOException {

		LocalDate earliestDate = LocalDate.now();
		
		BigDecimal closingPrice = BigDecimal.ZERO;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String url = ALPHA_VANTAGE_URL + "?function=TIME_SERIES_DAILY_ADJUSTED&outputsize=compact&symbol=" + symbol + "&apikey="
				+ ALPHA_VANTAGE_APIKEY + "&outputsize=full";
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
				System.out.println(response.toString());
				if (retry) {
					int tries = 6;
					boolean success = false;
					while (tries-- > 0 && !success) {
						System.out.println("Sleep for 10 seconds and try again");
						Thread.sleep(10000);
						success = loadFundHistoryFromAlphaVantage(portfolio, symbol, false);
					}
				}
				return false;
			}
			PortfolioPriceHistory priceHistory = portfolio.getPriceHistory();
			for (Entry<String, TimeSeries> entry : series.getTimeSeries().entrySet()) {

				LocalDate date = LocalDate.parse(entry.getKey()); 
				if (date.isBefore(earliestDate)) {
					earliestDate = date;
				}
				
				TimeSeries timeSeries = entry.getValue();
				closingPrice = new BigDecimal(timeSeries.getClose());
				System.out.println("date:  " + date.toString() + " closing price:  " + closingPrice);

				priceHistory.addFundPrice(symbol, date, closingPrice);
				
			}

			httpclient.close();
			if (earliestDate.isBefore(priceHistory.getOldestDay())) 
			{
				priceHistory.setOldestDay(earliestDate);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}


		return true;
	}

}
