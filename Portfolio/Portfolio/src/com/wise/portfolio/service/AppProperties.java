package com.wise.portfolio.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class AppProperties {

	private static final String PROPERTIES_FLE = "C:\\Users\\mavin\\Downloads\\appproperties.txt";

	private static final Properties appProps = new Properties();

	private static  AppProperties instance = null;

	protected static final Logger logger = LogManager.getLogger(AppProperties.class);
	
	private AppProperties() {
		try {
			appProps.load(new FileInputStream(PROPERTIES_FLE));
		} catch (FileNotFoundException e) {
			logger.error("Properties file not found:  " + PROPERTIES_FLE, e);
		} catch (IOException e) {
			logger.error("Error loading properties file:  " + PROPERTIES_FLE, e);
		}
	}
	
	public static void load() {
		
	}
	public static String getProperty(String key) {
		if (instance == null) {
			instance = new AppProperties();
		}
		return appProps.getProperty(key);
	}
	
	public static BigDecimal getPropertyAsBigDecimal(String key) {
		BigDecimal property = BigDecimal.ZERO;
		String stringProperty = getProperty(key);
		if (stringProperty != null) {
			try {
				property = new BigDecimal(stringProperty);
			} catch (NumberFormatException e) {
				logger.error("Invalid decimal property:  " + key + " value:  " + key, e);
			}
		} else {
			logger.error("Missing property:  " + key);
		}
		return property;
	}

	public static Integer getPropertyAsInteger(String key) {
		Integer property = 0;
		String stringProperty = getProperty(key);
		if (stringProperty != null) {
			try {
				property = new Integer(stringProperty);
			} catch (NumberFormatException e) {
				logger.error("Invalid integer property:  " + key + " value:  " + key, e);
			}
		} else {
			logger.error("Missing property:  " + key);
		}
		return property;
	}

	public static Float getPropertyAsFloat(String key) {
		Float property = 0f;
		String stringProperty = getProperty(key);
		if (stringProperty != null) {
			try {
				property = new Float(stringProperty);
			} catch (NumberFormatException e) {
				logger.error("Invalid float property:  " + key + " value:  " + key, e);
			}
		} else {
			logger.error("Missing property:  " + key);
		}
		return property;
	}


}
