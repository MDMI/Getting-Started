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

import org.mdmi.Node;

/**
 * Syntax node abstraction - a node in a syntax tree.
 *
 * @author goancea
 */
public interface ISyntaxNode {
	/**
	 * Get the model Node class for this syntax node instance.
	 *
	 * @return The model Node class for this syntax node instance.
	 */
	public Node getNode();

	/**
	 * Get the parent syntax node of this syntax node. Only the root node will return null.
	 *
	 * @return The parent syntax node of this syntax node.
	 */
	public ISyntaxNode getParent();

	/**
	 * Return true if this instance is a leaf. Means the getNode will return a LeafSyntaxTranslator.
	 *
	 * @return True if this instance is a leaf.
	 */
	public boolean isLeaf();

	/**
	 * Return true if this instance is a bag. Means the getNode will return a Bag.
	 *
	 * @return True if this instance is a bag.
	 */
	public boolean isBag();

	/**
	 * Return true if this instance is a choice. Means the getNode will return a Choice.
	 *
	 * @return True if this instance is a choice.
	 */
	public boolean isChoice();
} // ISynNode
