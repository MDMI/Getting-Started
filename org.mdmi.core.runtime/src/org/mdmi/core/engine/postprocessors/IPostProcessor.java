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
package org.mdmi.core.engine.postprocessors;

import org.mdmi.MessageModel;
import org.mdmi.core.MdmiMessage;

/**
 * An interface implemented by a provider that wishes to process the source or target messages after
 * the runtime is done with them. Typically in involves parsing the XML message and adding or removing elements
 * or attributes.
 *
 * @author goancea
 */
public interface IPostProcessor {
	/**
	 * Get the unique name of this processor.
	 *
	 * @return The unique name of this processor.
	 */
	public String getName();

	public boolean canProcess(org.mdmi.MessageModel messageModel);

	public void processMessage(MessageModel messageModel, MdmiMessage message);
} // IPostProcessor
