/*******************************************************************************
 * Copyright (c) 2018 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package org.mdmi.core.runtime;

import java.util.HashMap;

public class MdmiNetTransferInfo {

	public String sourceMap;

	public String sourceModel;

	public String targetMap;

	public String targetModel;

	public String sourceMessage; // base 64 encoded bytes

	public String targetMessage; // base 64 encoded bytes, may be null

	public String elements; // may be null, comma separated BER names or IDs

	public HashMap<String, String> sourceProperties = new HashMap<String, String>();

	public HashMap<String, String> targetProperties = new HashMap<String, String>();

	public String toXml() {

		return "";
	}

}
