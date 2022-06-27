package com.wise.portfolio.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceService
{

    // < <Fund symbol>, <Date, Price> >
    private Map<String, Map<LocalDate, BigDecimal>> fundPrices = new HashMap<>();
    private Portfolio portfolio = new Portfolio();

    public PerformanceService(Map<String, Map<LocalDate, BigDecimal>> fundPrices, Portfolio portfolio)
    {
        super();
        this.fundPrices = fundPrices;
        this.portfolio = portfolio;
    }



    public Float getTrendByDays(String symbol, int trendDays)
    {

        LocalDate today = LocalDate.now();

        // Find the nearest date
        LocalDate date = today.minusDays(trendDays);
        BigDecimal historicalPrice = getPriceByDate(symbol, date);
        if (historicalPrice == null)
        {
            historicalPrice = getClosestHistoricalPrice(symbol, date, 30);
            if (historicalPrice == null)
            {
                return null;
            }
        }
        BigDecimal currentPrice = getPriceByDate(symbol, today);

        return currentPrice.subtract(historicalPrice).divide(historicalPrice, 4, RoundingMode.HALF_UP).floatValue();
    }

    public Float getTrendByYear(String symbol, int trendYears)
    {

        LocalDate today = LocalDate.now();

        // Find the nearest date
        LocalDate date = today.minusYears(trendYears);
        BigDecimal historicalPrice = getPriceByDate(symbol, date);
        if (historicalPrice == null)
        {
            historicalPrice = getClosestHistoricalPrice(symbol, date, 90);
            if (historicalPrice == null)
            {
                return null;
            }
        }
        BigDecimal currentPrice = getPriceByDate(symbol, today);
        return currentPrice.subtract(historicalPrice).divide(historicalPrice, 4, RoundingMode.HALF_UP).floatValue();
    }
    
    public BigDecimal getPriceByDate(String symbol, LocalDate date)
    {
        BigDecimal value = null;

        Map<LocalDate, BigDecimal> fundPriceMap = fundPrices.get(symbol);
        if (fundPriceMap != null)
        {
            value = fundPriceMap.get(date);
        }

        return value;
    }

    public BigDecimal getValueByDate(String symbol, LocalDate date)
    {

        BigDecimal value = new BigDecimal(0);

        BigDecimal price = getPriceByDate(symbol, date);
        double shares = portfolio.getFund(symbol).getShares();
        if (price != null && shares > 0)
        {
            value = price.multiply(new BigDecimal(shares));
        }

        return value;
    }
    
    private BigDecimal getClosestHistoricalPrice(String symbol, LocalDate date, int days)
    {
        LocalDate closestHistoricalDate = null;

        int tries = 0;
        while (closestHistoricalDate == null && tries++ < days)
        {
            BigDecimal historicalValue = getPriceByDate(symbol, date.plusDays(tries));
            if (historicalValue != null)
            {
                return historicalValue;
            }
            historicalValue = getPriceByDate(symbol, date.minusDays(tries));
            if (historicalValue != null)
            {
                return historicalValue;
            }
        }
        return null;
    }

}
