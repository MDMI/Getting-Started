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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.mdmi.core.MdmiException;
import org.mdmi.core.util.XmlUtil;

public class DateConverter implements IConvertFromString {
	@Override
	public Object convertFromString(String value, String format) {
		if (format != null && 0 < format.length()) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				return sdf.parse(value);
			} catch (Exception ex) {
				throw new MdmiException(ex, "DateConverter.convertFromString({0}, {1}) failed.", value, format);
			}
		}

		try {
			return XmlUtil.parseDateYMDZ(value);
		} catch (ParseException ex) {
			throw new MdmiException(ex, "DateConverter.convertFromString({0}, {1}) failed.", value, format);
		}
	}
}
