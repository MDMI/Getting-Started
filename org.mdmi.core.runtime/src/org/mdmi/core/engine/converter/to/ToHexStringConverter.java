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

import java.math.BigInteger;

import org.mdmi.core.util.StringUtil;

public class ToHexStringConverter implements IConvertToString {
	private IConvertToString m_wrapped;

	public ToHexStringConverter() {
		m_wrapped = new ToStringConverter();
	}

	public ToHexStringConverter(IConvertToString toString) {
		m_wrapped = toString;
	}

	@Override
	public String convertToString(Object obj, String format) {
		String toConvert = m_wrapped.convertToString(obj, format);
		return StringUtil.encodeBytesHex(new BigInteger(toConvert).toByteArray());
	}
}
