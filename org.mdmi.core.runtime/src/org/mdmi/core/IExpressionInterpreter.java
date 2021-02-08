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

import java.util.List;
import java.util.Properties;

import org.mdmi.SemanticElement;
import org.mdmi.core.engine.XElementValue;
import org.mdmi.core.engine.XValue;

/**
 * @TODO - single expressions are to expensive - replace with functions
 *
 *       An interface implemented by the expression processors.
 * @author goancea
 * @deprecated
 */

@Deprecated
public interface IExpressionInterpreter {
	/**
	 * Perform initialization. This was done by the constructor but we need to make
	 * it separate since we're externalizing the actual adapter.
	 */
	public void initialize(ElementValueSet eset, XElementValue context, String name, XValue value);

	/**
	 * Evaluate the given constraint in the given context.
	 *
	 * @param context
	 *            The context, which is an element value.
	 * @param rule
	 *            The rule to evaluate (as a string).
	 * @return True if the constraint is valid (evaluates to true).
	 */
	// public boolean evalConstraint(IElementValue context, String rule);

	/**
	 * Evaluate an action language rule in the given context.
	 *
	 * @param context
	 *            The context, which is an element value.
	 * @param rule
	 *            The rule to evaluate (as a string).
	 */
	public void evalAction(IElementValue context, String rule, Properties properties);

	/**
	 * Evaluate an action language rule in the given context BUT do not execute.
	 *
	 * @param context
	 *            The context, which is an element value.
	 * @param rule
	 *            The rule to evaluate (as a string).
	 * @return errors The list of compilation errors.
	 */
	public List<String> compileAction(SemanticElement se, String rule);

	/**
	 * Evaluate an constraint language rule in the given context BUT do not execute.
	 *
	 * @param context
	 *            The context, which is an element value.
	 * @param rule
	 *            The rule to evaluate (as a string).
	 * @return errors The list of compilation errors.
	 */
	// public List<String> compileConstraint(SemanticElement se, String rule);
} // IExpressionInterpreter
