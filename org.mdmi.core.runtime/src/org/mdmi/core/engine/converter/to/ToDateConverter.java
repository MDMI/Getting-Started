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

import org.mdmi.core.MdmiException;
import org.mdmi.core.util.XmlUtil;

public class ToDateConverter implements IConvertToString {
	@Override
	public String convertToString(Object obj, String format) {
		if (!(obj instanceof Date)) {
			throw new IllegalArgumentException("Object is not a java.util.Date type.");
		}

		if (format != null && 0 < format.length() && !format.equals("DATE")) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				return sdf.format((Date) obj);
			} catch (Exception ex) {
				throw new MdmiException(ex, "ToDateConverter.convertToString({0}, {1}) failed.", obj, format);
			}
		}

		return XmlUtil.formatDateYMDZ((Date) obj);
	}
}
