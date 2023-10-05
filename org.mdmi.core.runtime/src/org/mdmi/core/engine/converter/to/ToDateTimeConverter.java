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
 * Author:
 *     Gabriel Oancea
 *     Jeff Klann
 *
 *******************************************************************************/
package org.mdmi.core.engine.converter.to;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.mdmi.core.engine.converter.DateWrapper;
import org.mdmi.core.util.DateUtil;

public class ToDateTimeConverter implements IConvertToString {

	HashMap<String, SimpleDateFormat> formats = new HashMap<>();

	@Override
	public String convertToString(Object obj, String format) {
		String originalFormat = null;
		// if (!((obj instanceof Date) || obj instanceof DateWrapper)) {
		// throw new IllegalArgumentException("Object is not a java.util.Date type.");
		// }
		if (obj instanceof DateWrapper) {
			DateWrapper dateWrapper = (DateWrapper) obj;
			obj = dateWrapper.getDate();
			originalFormat = dateWrapper.getOriginalFormat();
		}
		try {
			if (obj instanceof Date) {
				return DateUtil.formatDate(
					format == null
							? null
							: DateUtil.getLongestWithoutSemiColons(format),
					(Date) obj, originalFormat);
			}
		} catch (Exception ex) {
			// throw new MdmiException(ex, "ToDateTimeConverter.convertToString({0}, {1}) failed.", obj, format);
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		return obj.toString();
	}
}
