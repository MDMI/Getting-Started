/*******************************************************************************
 * Copyright (c) 2016 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package org.mdmi.core.engine;

/**
 * @author seanmuir
 *
 */
public interface YNodeVisitor {

	public void visit(YBag ybag);

	public void visit(YLeaf yleaf);

	public void visit(YChoice ychoice);

}
