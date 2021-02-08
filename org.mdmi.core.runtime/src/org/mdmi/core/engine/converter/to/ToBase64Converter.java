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

import org.mdmi.core.util.StringUtil;

public class ToBase64Converter implements IConvertToString {
	private IConvertToString s_wrapped;

	public ToBase64Converter() {
		this(new ToStringConverter());
	}

	public ToBase64Converter(IConvertToString wrapped) {
		s_wrapped = wrapped;
	}

	@Override
	public String convertToString(Object obj, String format) {
		return StringUtil.encode(s_wrapped.convertToString(obj, format));
	}
}
