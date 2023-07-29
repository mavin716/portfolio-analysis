package com.wise.portfolio.fund;

import java.math.BigDecimal;

public interface Fund
{
    public String getName();
    public void setName(String name);
    
    public String getSymbol();
    public void setSymbol(String symbol);

    public BigDecimal getCurrentPrice();
    public void setCurrentPrice(BigDecimal currentPrice);

}
