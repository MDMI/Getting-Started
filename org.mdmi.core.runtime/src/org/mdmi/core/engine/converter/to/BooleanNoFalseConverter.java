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

public class BooleanNoFalseConverter implements IConvertToString {
	private IConvertToString m_wrapped = null;

	public BooleanNoFalseConverter(IConvertToString wrapped) {
		m_wrapped = wrapped;
	}

	@Override
	public String convertToString(Object obj, String format) {
		if (!(obj instanceof Boolean)) {
			throw new IllegalArgumentException("Object is not a Boolean type.");
		}
		if (((Boolean) obj).booleanValue() == false) {
			throw new IllegalArgumentException("Boolean value of false cannot be converted.");
		}
		return m_wrapped.convertToString(obj, format);
	}
}
