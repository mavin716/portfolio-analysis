package com.wise.portfolio.alphaVantage.data;

import java.util.Map;

public class Series {
    private Metadata metadata;
    public Metadata getMetadata() {
		return metadata;
	}
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	public Map<String, TimeSeries> getTimeSeries() {
		return timeSeries;
	}
	public void setTimeSeries(Map<String, TimeSeries> timeSeries) {
		this.timeSeries = timeSeries;
	}
	private Map<String, TimeSeries> timeSeries;
}