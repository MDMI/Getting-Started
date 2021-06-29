/*******************************************************************************
 * Copyright (c) 2017,2018 MDIX.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir(MDIX) - initial API and implementation
 *     seanmuir(MDIX) - Updated Date Formating
 *
 *******************************************************************************/
package org.mdmi.core.engine.javascript;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.ConversionRule;
import org.mdmi.core.engine.XDataStruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seanmuir
 *
 */
public class Utils {

	private static Logger logger = LoggerFactory.getLogger(Utils.class);

	public static String Hello() {
		return "hello from java";
	}

	/**
	 * RuntimeDateFormat pairs the format for parsing and the expected significant digits in the date for the target format
	 * We are assuming all two digit dates are in this century
	 *
	 * @author seanmuir
	 *
	 */
	private static class RuntimeDateFormat {

		DateTimeFormatter input;

		DateTimeFormatter output;

		int significant;

		String pattern;

		public RuntimeDateFormat(String pattern, int significant) {
			super();

			this.input = DateTimeFormatter.ofPattern(pattern.replace("yyyy", "uuuu")).withLocale(Locale.US).withZone(
				ZoneId.systemDefault());
			this.output = DateTimeFormatter.ofPattern(pattern.replace("yyyy", "uuuu")).withLocale(Locale.US).withZone(
				ZoneId.systemDefault());
			this.significant = significant;
			this.pattern = pattern;
		}

		public RuntimeDateFormat(String inpattern, String outpattern, int significant) {
			super();

			this.input = DateTimeFormatter.ofPattern(inpattern.replace("yyyy", "uuuu")).withLocale(Locale.US).withZone(
				ZoneId.systemDefault());
			this.output = DateTimeFormatter.ofPattern(outpattern.replace("yyyy", "uuuu")).withLocale(
				Locale.US).withZone(ZoneId.systemDefault());
			this.significant = significant;
			this.pattern = inpattern;
		}
	}

	private static final String dash = "MM-dd-yyyy";

	private static final RuntimeDateFormat dashes[] = {
			new RuntimeDateFormat("M-d-yyyy", "MM-dd-yyyy", 8), new RuntimeDateFormat("M-d-yy", 8),
			new RuntimeDateFormat("M-yy", 6), new RuntimeDateFormat("yy", 4), new RuntimeDateFormat("M-yyyy", 6),
			new RuntimeDateFormat("yyyy", 4) };

	private static final String dash2 = "yyyy-MM-dd";

	private static final RuntimeDateFormat dashes2[] = {
			new RuntimeDateFormat("yyyy-M-d", "yyyy-MM-dd", 8), new RuntimeDateFormat("yyyy-MM", 6),
			new RuntimeDateFormat("yyyy", 4) };

	private static final String dash3 = "yyyy-MM-dd HH:mm:ss";

	private static final RuntimeDateFormat dashes3[] = {
			new RuntimeDateFormat("yyyy-M-d HH:mm:ss", 14), new RuntimeDateFormat("yyyy-M-d", 8),
			new RuntimeDateFormat("yyyy-MM", 6), new RuntimeDateFormat("yyyy", 4) };

	private static final String slash = "MM/dd/yyyy";

	private static final RuntimeDateFormat slashes[] = {
			new RuntimeDateFormat("M/d/yy", 8), new RuntimeDateFormat("M/yy", 6), new RuntimeDateFormat("M/yyyy", 6),
			new RuntimeDateFormat("yy", 4), new RuntimeDateFormat("M/d/yyyy", 8), new RuntimeDateFormat("M/yyyy", 6),
			new RuntimeDateFormat("yyyy", 4) };

	private static final String slash2 = "MM/dd/yyyy HH:mm:ss";

	private static final RuntimeDateFormat slashes2[] = {
			new RuntimeDateFormat("MM/dd/yyyy HH:mm:ss", 14), new RuntimeDateFormat("M/d/yy", 8),
			new RuntimeDateFormat("M/yy", 6), new RuntimeDateFormat("M/yyyy", 6), new RuntimeDateFormat("yy", 4),
			new RuntimeDateFormat("M/d/yyyy", 8), new RuntimeDateFormat("M/yyyy", 6),
			new RuntimeDateFormat("yyyy", 4) };

	private static final String slash3 = "MM/dd/yyyy HH:mm:ss.SSSSSS";

	private static final RuntimeDateFormat slashes3[] = {
			new RuntimeDateFormat("MM/dd/yyyy HH:mm:ss.SSSSSS", 20), new RuntimeDateFormat("MM/dd/yyyy HH:mm:ss", 14),
			new RuntimeDateFormat("M/d/yy", 8), new RuntimeDateFormat("M/yy", 6), new RuntimeDateFormat("M/yyyy", 6),
			new RuntimeDateFormat("yy", 4), new RuntimeDateFormat("M/d/yyyy", 8), new RuntimeDateFormat("M/yyyy", 6),
			new RuntimeDateFormat("yyyy", 4) };

	private static final String fmtHL7 = "yyyyMMddHHmmssZ";

	private static final RuntimeDateFormat fmtHL7s[] = {
			new RuntimeDateFormat("yyyyMMddHHmmssZ", 16), new RuntimeDateFormat("yyyyMMddHHmmss.SSSZ", 16),
			new RuntimeDateFormat("yyyyMMddHHmmZ", 12), new RuntimeDateFormat("yyyyMMddHHZ", 10),
			new RuntimeDateFormat("yyyyMMddHHmmss", 14), new RuntimeDateFormat("yyyyMMddHHmm", 12),
			new RuntimeDateFormat("yyyyMMdd", 8), new RuntimeDateFormat("yyyyMM", 6),
			new RuntimeDateFormat("yyyy", 4) };

	private static final String fhirfmt = "yyyy-MM-dd'T'hh:mm:ss";

	private static final RuntimeDateFormat fhirfmts[] = {
			new RuntimeDateFormat("yyyy-MM-dd'T'HH:mm:ss", 14), new RuntimeDateFormat("yyyy-MM-dd'T'HH:mm", 12),
			new RuntimeDateFormat("yyyy-MM-dd'T'HH", 10), new RuntimeDateFormat("yyyy-MM-dd", 8),
			new RuntimeDateFormat("yyyy-MM", 6), new RuntimeDateFormat("yyyy", 4) };

	private static final String fhirInstancefmt = "yyyy-MM-dd'T'HH:mm:ss+zzzz";

	private static final RuntimeDateFormat fhirInstancefmts[] = {
			new RuntimeDateFormat("yyyy-MM-dd'T'HH:mm:ss+zzzz", "yyyy-MM-dd'T'HH:mm:ssxxx", 16),
			new RuntimeDateFormat("yyyy-MM-dd'T'HH:mm:ss", 14), new RuntimeDateFormat("yyyy-MM-dd'T'HH:mm", 12),
			new RuntimeDateFormat("yyyy-MM-dd'T'HH", 10), new RuntimeDateFormat("yyyy-MM-dd", 8),
			new RuntimeDateFormat("yyyy-MM", 6), new RuntimeDateFormat("yyyy", 4) };

	public static HashMap<String, RuntimeDateFormat[]> formats = new HashMap<String, RuntimeDateFormat[]>();

	/*
	 * Ignore case when matching formats
	 */
	static {
		formats.put("yyyyMMdd".toUpperCase(), fhirfmts);
		formats.put("yyyy-MM-dd".toUpperCase(), fmtHL7s);
		formats.put(fmtHL7.toUpperCase(), fmtHL7s);
		formats.put(fhirfmt.toUpperCase(), fhirfmts);
		formats.put(slash.toUpperCase(), slashes);
		formats.put(slash2.toUpperCase(), slashes2);
		formats.put(slash3.toUpperCase(), slashes3);
		formats.put(dash.toUpperCase(), dashes);
		formats.put(dash2.toUpperCase(), dashes2);
		formats.put(dash3.toUpperCase(), dashes3);
		formats.put(fhirInstancefmt.toUpperCase(), fhirInstancefmts);
	}

	public static String FormatDate(String from, String to, String date) {

		String fromKey = from.toUpperCase();
		String toKey = to.toUpperCase();

		if (logger.isTraceEnabled()) {
			logger.trace("from : " + from);
			logger.trace("to : " + to);
			logger.trace("date : " + date);

		}
		if (date == null || StringUtils.isEmpty(date)) {
			return "";
		}

		int significant = 0;

		if (formats.containsKey(toKey) && formats.containsKey(fromKey)) {
			TemporalAccessor fromDate = null;
			for (RuntimeDateFormat fromFormat : formats.get(fromKey)) {
				try {
					fromDate = fromFormat.input.parse(date);
				} catch (DateTimeParseException ne) {

				}
				if (fromDate != null) {
					significant = fromFormat.significant;
					break;
				}
			}

			/*
			 * This is specific fix for known data issue for a particular transformation
			 */
			if (fromDate == null && (date.endsWith("-"))) {

				// date = date.replace("--", "-");
				date = StringUtils.stripEnd(date, "-");
				for (RuntimeDateFormat fromFormat : formats.get(fromKey)) {
					try {
						fromDate = fromFormat.input.parse(date);
					} catch (DateTimeParseException ne) {
					}
					if (fromDate != null) {
						significant = fromFormat.significant;
						break;
					}
				}
			}

			if (fromDate == null && (date.contains("--"))) {
				String[] splitresult = date.split("--");
				date = splitresult[0];
				for (RuntimeDateFormat fromFormat : formats.get(fromKey)) {
					try {
						fromDate = fromFormat.input.parse(date);
					} catch (DateTimeParseException ne) {
					}
					if (fromDate != null) {
						significant = fromFormat.significant;
						break;
					}
				}
			}

			if (fromDate != null) {
				// if FHIR target set significant to 14 pads 00 to seconds
				// No time zone in fhir
				if (to.equals("yyyy-MM-dd'T'hh:mm:ss") &&
						(significant == 12 || significant == 16 || significant == 20)) {
					significant = 14;
				}

				if (significant > 8 && to.length() <= 10) {
					significant = 8;
				}

				for (RuntimeDateFormat toFormat : formats.get(toKey)) {
					if (toFormat.significant == significant) {
						try {
							return toFormat.output.format(fromDate);
						} catch (UnsupportedTemporalTypeException unsupportedTemporalTypeException) {
							logger.error("From " + from + " To " + to + " Date " + date);
							logger.error(unsupportedTemporalTypeException.getLocalizedMessage());
							return date;
						}
					}
				}
				// try best fit here
				// String key
				for (RuntimeDateFormat toFormat : formats.get(toKey)) {
					if (toFormat.pattern.equals(toKey.replace("yyyy", "uuuu"))) {
						try {
							return toFormat.output.format(fromDate);
						} catch (UnsupportedTemporalTypeException unsupportedTemporalTypeException) {
							logger.error("From " + from + " To " + to + " Date " + date);
							logger.error(unsupportedTemporalTypeException.getLocalizedMessage());
							return date;
						}
					}

				}
			}
		} else {
			if (!formats.containsKey(toKey)) {
				logger.error("to date format is missing " + toKey);
				for (String key : formats.keySet()) {
					logger.trace(key);
				}

			}
			if (!formats.containsKey(fromKey)) {
				logger.error("from date format is missing " + fromKey);
				for (String key : formats.keySet()) {
					logger.trace(key);
				}
			}
		}
		return date;
	}

	/**
	 * @TODO There are better parses in nodejs - update runtime to support node js
	 *
	 * @param sourceAddress
	 * @param object
	 */
	public static void StringToAddress(String sourceAddress, Object object) {

		XDataStruct xds = (XDataStruct) object;

		if (!StringUtils.isEmpty(sourceAddress)) {

			Map<net.sourceforge.jgeocoder.AddressComponent, String> addressComponents = net.sourceforge.jgeocoder.us.AddressParser.parseAddress(
				sourceAddress);

			if (addressComponents != null) {

				StringBuffer streetAddresLine = new StringBuffer();

				String number = addressComponents.get(net.sourceforge.jgeocoder.AddressComponent.NUMBER);

				if (!StringUtils.isEmpty(number)) {
					streetAddresLine.append(number).append(" ");
				}

				String street1 = addressComponents.get(net.sourceforge.jgeocoder.AddressComponent.STREET);

				if (!StringUtils.isEmpty(street1)) {
					streetAddresLine.append(street1);
				}

				if (streetAddresLine.length() > 0) {
					xds.getXValue("streetAddressLine").addValue(streetAddresLine.toString());
				}
				String street2 = addressComponents.get(net.sourceforge.jgeocoder.AddressComponent.STREET2);

				if (!StringUtils.isEmpty(street2)) {
					xds.getXValue("streetAddressLine").addValue(street2);
				}

				String city = addressComponents.get(net.sourceforge.jgeocoder.AddressComponent.CITY);

				if (!StringUtils.isEmpty(city)) {
					xds.getXValue("city").setValue(city);
				}

				String state = addressComponents.get(net.sourceforge.jgeocoder.AddressComponent.STATE);

				if (!StringUtils.isEmpty(state)) {
					xds.getXValue("state").setValue(state);
				}

				String zip = addressComponents.get(net.sourceforge.jgeocoder.AddressComponent.ZIP);

				if (!StringUtils.isEmpty(zip)) {
					xds.getXValue("postalCode").setValue(zip);
				}
			} else {
				// issue parsing just set the street address line
				xds.getXValue("streetAddressLine").addValue(sourceAddress);

			}
		}

	}

	public static void StringToPatientName(String sourceName, Object object) {

		if (!StringUtils.isEmpty(sourceName)) {

			NameParser nameParser = new NameParser();
			nameParser.parse(sourceName.replaceAll("\\^", " ").replaceAll(",", " "));
			XDataStruct xds = (XDataStruct) object;
			if (!StringUtils.isEmpty(nameParser.getFirstName())) {
				xds.getXValue("given").addValue(nameParser.getFirstName());
			}

			if (!StringUtils.isEmpty(nameParser.getMiddleName())) {
				xds.getXValue("given").addValue(nameParser.getMiddleName());
			}

			if (!StringUtils.isEmpty(nameParser.getLastName())) {
				xds.getXValue("family").addValue(nameParser.getLastName());
			}

			for (String prefix : nameParser.getTitlesBefore()) {
				xds.getXValue("prefix").addValue(prefix);
			}

			for (String suffix : nameParser.getTitlesAfter()) {
				xds.getXValue("suffix").addValue(suffix);
			}

		}
	}

	static Map<String, Properties> mapOfTransforms = new HashMap<String, Properties>();

	public static void loadMap(String targetSystem) {
		if (!mapOfTransforms.containsKey(targetSystem)) {
			mapOfTransforms.put(targetSystem, new Properties());
		}
		try {
			mapOfTransforms.get(targetSystem).load(
				Utils.class.getClassLoader().getResourceAsStream(targetSystem + ".properties"));
		} catch (Exception e1) {
			logger.error(e1.getMessage());
		}
	}

	public static String transform(String sourceCode, String targetSystem) {
		loadMap(targetSystem);
		if (mapOfTransforms.containsKey(targetSystem)) {
			return (String) mapOfTransforms.get(targetSystem).get(sourceCode);
		} else {
			return sourceCode;
		}

	}

	public static String getSemanticProperyQualifier(ConversionRule conversionRule, String prefix) {
		if (conversionRule.getOwner() != null && !conversionRule.getOwner().getPropertyQualifier().isEmpty()) {
			for (String properyQualifier : conversionRule.getOwner().getPropertyQualifier()) {
				if (properyQualifier.startsWith(prefix)) {
					return properyQualifier.substring(prefix.length());
				}
			}

		}
		return "";
	}

}
