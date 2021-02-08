/*******************************************************************************
* Copyright (c) 2012, 2017, 2018 MDIX Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     MDIX Inc - initial API and implementation
*     Jeff Klann, PhD - revision for variable precision
*
* Authors:
*     Gabriel Oancea
*     Jeff Klann
*
*******************************************************************************/
package org.mdmi.core.engine.converter.to;

import java.util.Date;

import org.mdmi.core.engine.converter.DateWrapper;
import org.mdmi.core.util.DateUtil;

public class ToStringConverter implements IConvertToString {
	private String originalFormat = null;

	@Override
	public String convertToString(Object obj, String format) {
		if (format != null && format.length() > 0) {
			if (obj instanceof Date) {
				return convertStringSplit(obj, format);
			}
			if (obj instanceof DateWrapper) {
				originalFormat = ((DateWrapper) obj).getOriginalFormat();
				return convertStringSplit(((DateWrapper) obj).getDate(), format);
			}
		}
		return obj.toString();
	}

	public String convertStringSplit(Object obj, String format) {
		String bestFormat = DateUtil.getLongestWithoutSemiColons(format);
		String convert = convert(obj, bestFormat);
		if (convert != null) {
			return convert;
		}
		return null;
	}

	private String convert(Object obj, String format) {
		if (obj == null) {
			return "ERROR";
		}
		try {
			return DateUtil.formatDate(format, (Date) obj, originalFormat);
		} catch (IllegalArgumentException ignored) {
		}
		return null;
	}
}
