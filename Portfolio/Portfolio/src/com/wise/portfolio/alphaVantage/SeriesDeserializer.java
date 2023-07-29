package com.wise.portfolio.alphaVantage;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.wise.portfolio.alphaVantage.Metadata;
import com.wise.portfolio.alphaVantage.Series;
import com.wise.portfolio.alphaVantage.TimeSeries;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SeriesDeserializer extends JsonDeserializer<Series> {

    @Override
    public Series deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Series series = new Series();
        node.fields().forEachRemaining(field -> {
            JsonNode childNode = field.getValue();
            if (field.getKey().contains("Meta Data")) {
                series.setMetadata(setMetadata(childNode));
            } else if (field.getKey().contains("Time Series")) {
                Map<String, TimeSeries> timeSeries = new LinkedHashMap<>();
                childNode.fields().forEachRemaining(entry -> timeSeries.put(entry.getKey(), setTimeSeries(entry.getValue())));
                series.setTimeSeries(timeSeries);
            }
        });
        return series;
    }

    private Metadata setMetadata(JsonNode metadataNode) {
        Metadata metadata = new Metadata();
        metadataNode.fieldNames().forEachRemaining(fieldName -> {
            if (fieldName.contains("Information")) {
                metadata.setInformation(metadataNode.get(fieldName).asText());
            } else if (fieldName.contains("Symbol")) {
                metadata.setSymbol(metadataNode.get(fieldName).asText());
            } else if (fieldName.contains("Last Refreshed")) {
                metadata.setLastRefreshed(metadataNode.get(fieldName).asText());
            } else if (fieldName.contains("Interval")) {
                metadata.setInterval(metadataNode.get(fieldName).asText());
            } else if (fieldName.contains("Output Size")) {
                metadata.setOutputSize(metadataNode.get(fieldName).asText());
            } else if (fieldName.contains("Time Zone")) {
                metadata.setTimeZone(metadataNode.get(fieldName).asText());
            }
        });
        return metadata;
    }

    private TimeSeries setTimeSeries(JsonNode seriesNode) {
        TimeSeries timeSeries = new TimeSeries();
        seriesNode.fieldNames().forEachRemaining(fieldName -> {
            if (fieldName.contains("open")) {
                timeSeries.setOpen(seriesNode.get(fieldName).asText());
            } else if (fieldName.contains("high")) {
                timeSeries.setHigh(seriesNode.get(fieldName).asText());
            } else if (fieldName.contains("low")) {
                timeSeries.setLow(seriesNode.get(fieldName).asText());
            } else if (fieldName.contains("close")) {
                timeSeries.setClose(seriesNode.get(fieldName).asText());
            } else if (fieldName.contains("volume")) {
                timeSeries.setVolume(seriesNode.get(fieldName).asText());
            }
        });
        return timeSeries;
    }
}