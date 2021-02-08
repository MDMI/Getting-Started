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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class BinaryConverter implements IConvertFromString {
	@Override
	public Object convertFromString(String value, String format) {
		Charset cs = Charset.forName("UTF-8");
		ByteBuffer bb = cs.encode(value);
		return bb;
	}
}
