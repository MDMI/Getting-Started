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

public class ArrayToAnyConverter implements IConvertToString {
	private IConvertToString m_wrapped;

	public ArrayToAnyConverter(IConvertToString wrapped) {
		m_wrapped = wrapped;
	}

	@Override
	public String convertToString(Object obj, String format) {
		if (!(obj instanceof Object[])) {
			throw new IllegalArgumentException("Object is not an Object array type.");
		}
		StringBuilder builder = new StringBuilder();
		for (Object curObj : (Object[]) obj) {
			if (builder.length() > 0) {
				builder.append(" ");
			}
			builder.append(m_wrapped.convertToString(curObj, format));
		}
		return builder.toString();
	}
}
