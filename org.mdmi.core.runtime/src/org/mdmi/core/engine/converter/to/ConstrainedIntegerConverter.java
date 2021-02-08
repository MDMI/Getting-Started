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

import org.mdmi.core.MdmiException;

public class ConstrainedIntegerConverter implements IConvertToString {
	private BigInteger m_minValue = null;

	private BigInteger m_maxValue = null;

	public ConstrainedIntegerConverter(Integer min, Integer max) {
		this(
			min == null
					? null
					: BigInteger.valueOf(min),
			max == null
					? null
					: BigInteger.valueOf(max));
	}

	public ConstrainedIntegerConverter(BigInteger min, BigInteger max) {
		m_minValue = min;
		m_maxValue = max;
	}

	@Override
	public String convertToString(Object obj, String format) {
		if (!(obj instanceof Number)) {
			throw new IllegalArgumentException("Object is not a Number type.");
		}

		BigInteger value = new BigInteger(obj.toString());
		if (m_minValue != null && value.compareTo(m_minValue) < 0) {
			throw new MdmiException("'" + obj.toString() + "' is not >= to '" + m_minValue + "'");
		}
		if (m_maxValue != null && value.compareTo(m_maxValue) > 0) {
			throw new IllegalArgumentException("'" + obj.toString() + "' is not <= to '" + m_minValue + "'");
		}
		return value.toString();
	}
}
