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
package org.mdmi.core;

import java.util.Properties;

import org.mdmi.MessageModel;

/**
 * Implemented by classes that can construct a semantic MDMI model from the syntax tree, and vice versa.
 */
public interface ISemanticParser {

	/**
	 * Construct a semantic model, from the given syntax tree and the message model. The semantic model is represented by
	 * the elements set (eset), and may be partially populated.
	 *
	 * @param mdl
	 *            The MessageModel that applies.
	 * @param root
	 *            The syntax tree input.
	 * @param eset
	 *            The element set as defined by the message model (IN-OUT parameter).
	 */
	public void buildSemanticModel(MessageModel mdl, ISyntaxNode root, ElementValueSet eset, Properties properties);

	/**
	 * Create a new syntax tree represented from the given semantic content (element set) and
	 * based on the specified message model.
	 *
	 * @param mdl
	 *            The MessageModel that applies.
	 * @param eset
	 *            The semantic content (element set).
	 * @return The syntax tree corresponding to the existing syntactic model.
	 */
	public ISyntaxNode createNewSyntacticModel(MessageModel mdl, ElementValueSet eset, Properties properties);

	/**
	 * Update the syntax tree represented by the syntax node given from the given semantic content (element set) and
	 * based on the specified message model.
	 *
	 * @param mdl
	 *            The MessageModel that applies.
	 * @param eset
	 *            The semantic content (element set).
	 * @param yroot
	 *            The syntax tree corresponding to the existing syntactic model.
	 */
	public void updateSyntacticModel(MessageModel mdl, ElementValueSet eset, ISyntaxNode yroot, Properties properties);
} // ISemanticParser
