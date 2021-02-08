/*******************************************************************************
* Copyright (c) 2012, 2017, 2018 MDIX Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     MDIX Inc - initial API and implementation
*
* Author:
*     Gabriel Oancea
*
*******************************************************************************/
package org.mdmi.core.engine.converter.from;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GYearConverter implements IConvertFromString {
	private static final String PATTERN_STRING = "(-?[0-9]{4})((Z|[\\+-][0-9]{2}:[0-9]{2})?)";

	private static final Pattern s_pattern = Pattern.compile(PATTERN_STRING);

	private static final String s_defaultMonth = "01";

	private static final String s_defaultTimezone = "Z";

	private static final IConvertFromString s_converter = new GYearMonthConverter();

	@Override
	public Object convertFromString(String value, String format) {
		StringBuilder builder = new StringBuilder();
		Matcher match = s_pattern.matcher(value);
		if (match.matches()) {
			String timezone = match.group(2);
			if (timezone == null || timezone.length() == 0) {
				timezone = s_defaultTimezone;
			}
			builder.append(match.group(1)).append("-").append(s_defaultMonth).append(timezone);
			return s_converter.convertFromString(builder.toString(), format);
		} else {
			// Did not match pattern => not valid.
			throw new IllegalArgumentException("Unable to parse '" + value + "' as a gYear.");
		}
	}
}
