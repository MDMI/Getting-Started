/*******************************************************************************
* Copyright (c) 2014 SemantX, Inc.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     SemantX, Inc. - initial API and implementation
*
* Author:
*     Sean Muir
*
*******************************************************************************/
package org.mdmi.core;

/**
 * An interface implemented by a provider that wishes to process the target semantic model.
 *
 * @author smuir
 */
public interface ITargetSemanticModelPostProcessor {
	/**
	 * Get the unique name of this processor.
	 *
	 * @return The unique name of this processor.
	 */
	public String getName();

	/**
	 * Execute the processing of the specified message, do what is necessary to the message and set the
	 * values back as expected by the runtime. Note that the message is always the target message in its
	 * final form.
	 *
	 *
	 * @param model
	 *            The semanticModel.
	 */
	public void processModel(ElementValueSet semanticModel);
} // IPostProcessor
