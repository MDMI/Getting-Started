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
package org.mdmi.core.engine.converter.to;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ToDatePatternConverter implements IConvertToString {
	private String pattern;

	public ToDatePatternConverter(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public String convertToString(Object obj, String format) {
		if (!(obj instanceof Date)) {
			throw new IllegalArgumentException("Object is not a java.util.Date type.");
		}

		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		String value = sdf.format((Date) obj);

		// We need to insert a colon to make it compatible with XML date formats.
		// NOTE: this is used with the XML datatypes
		return value.substring(0, value.length() - 2) + ":" + value.substring(value.length() - 2);
	}
}
