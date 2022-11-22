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
package org.mdmi.core.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mdmi.LeafSyntaxTranslator;
import org.mdmi.Node;

/**
 * A leaf syntax node (no children, the model Node is a LeafSyntaxTranslator).
 * It has a string value - the value representation in the message.
 *
 * @author goancea
 */
public final class YLeaf extends YNode {
	private LeafSyntaxTranslator m_leaf;

	private String m_value;

	/**
	 * Construct a leaf from its model node and its parent.
	 *
	 * @param leaf
	 *            The model node (a LeafSyntaxTranslator).
	 * @param parent
	 *            The owner of this instance. Null only for the root node.
	 */
	public YLeaf(LeafSyntaxTranslator leaf, YNode parent) {
		super(leaf, parent);
		m_leaf = leaf;
	}

	/**
	 * Construct a leaf from its model node, its parent and a value.
	 *
	 * @param leaf
	 *            The model node (a LeafSyntaxTranslator).
	 * @param parent
	 *            The owner of this instance. Null only for the root node.
	 * @param value
	 *            The value of this leaf node.
	 */
	public YLeaf(LeafSyntaxTranslator leaf, YNode parent, String value) {
		this(leaf, parent);
		m_value = value;
	}

	/**
	 * Get the value of this leaf node.
	 *
	 * @return The value of this leaf node.
	 */
	public String getValue() {
		return m_value;
	}

	/**
	 * Set the value of this leaf node.
	 *
	 * @param value
	 *            The new value of this leaf node.
	 */
	public void setValue(String value) {
		m_value = value;
	}

	/**
	 * Get the model node, a LeafSyntaxTranslator;
	 *
	 * @return The model node - a LeafSyntaxTranslator.
	 */
	public LeafSyntaxTranslator getLeaf() {
		return m_leaf;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	protected String toString(String indent) {
		return indent + m_leaf.getName() + ": " + (m_value == null
				? "null"
				: m_value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.engine.YNode#getChildren()
	 */
	@Override
	public List<YNode> getChildren() {
		return Collections.EMPTY_LIST;
	}

	private static final ArrayList<YNode> empty = new ArrayList<>();

	/*
	 *
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.engine.YNode#getYNodesForNode(org.mdmi.model.Node)
	 */
	@Override
	public ArrayList<YNode> getYNodesForNode(Node newNode) {
		return empty;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.engine.YNode#addYNode(org.mdmi.engine.YNode)
	 */
	@Override
	public void addYNode(YNode child) {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.engine.YNode#accept(org.mdmi.engine.YNodeVisitor)
	 */
	@Override
	public void accept(YNodeVisitor visitor) {
		visitor.visit(this);
	}

} // YLeaf
