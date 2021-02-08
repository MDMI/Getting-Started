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

import java.math.BigInteger;

public class IntegerBooleanConverter implements IConvertFromString {
	private IConvertFromString s_convertFrom = null;

	public IntegerBooleanConverter(IConvertFromString convert) {
		s_convertFrom = convert;
	}

	@Override
	public Object convertFromString(String value, String format) {
		BigInteger intVal = new BigInteger(value);
		return s_convertFrom.convertFromString(
			intVal.equals(BigInteger.ZERO)
					? "false"
					: "true",
			format);
	}
}
