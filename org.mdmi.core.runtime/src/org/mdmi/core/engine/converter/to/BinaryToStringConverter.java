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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class BinaryToStringConverter implements IConvertToString {
	@Override
	public String convertToString(Object obj, String format) {
		if (!(obj instanceof ByteBuffer)) {
			throw new IllegalArgumentException("Object is not a ByteBuffer type.");
		}
		Charset cs = Charset.forName("UTF-8");
		return cs.decode((ByteBuffer) obj).toString();
	}
}
