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

import org.mdmi.core.engine.converter.from.IConvertFromString;

public class ToAnyFromBinaryConverter implements IConvertToString {
	private IConvertToString m_binConverter = new BinaryToStringConverter();

	private IConvertFromString m_from;

	private IConvertToString m_to;

	public ToAnyFromBinaryConverter(IConvertFromString from, IConvertToString to) {
		m_from = from;
		m_to = to;
	}

	@Override
	public String convertToString(Object obj, String format) {
		String binToString = m_binConverter.convertToString(obj, format);
		Object toConvert = m_from.convertFromString(binToString, format);
		return m_to.convertToString(toConvert, format);
	}
}
