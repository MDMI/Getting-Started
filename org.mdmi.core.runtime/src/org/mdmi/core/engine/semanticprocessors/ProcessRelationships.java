/*******************************************************************************
 * Copyright (c) 2022 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package org.mdmi.core.engine.semanticprocessors;

import java.util.List;

import org.mdmi.MessageModel;
import org.mdmi.SemanticElementRelationship;
import org.mdmi.core.ElementValueSet;
import org.mdmi.core.IElementValue;

/**
 * @author seanmuir
 *
 */
public class ProcessRelationships implements ISemanticProcessor {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#getName()
	 */
	@Override
	public String getName() {
		return "ProcessRelationships";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#canProcess(org.mdmi.MessageModel)
	 */
	@Override
	public boolean canProcess(MessageModel messageModel) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#processSemanticModel(org.mdmi.core.ElementValueSet)
	 */
	@Override
	public void processSemanticModel(ElementValueSet semanticModel) {
		for (IElementValue element : semanticModel.getAllElementValues()) {
			if (!element.getSemanticElement().getRelationships().isEmpty()) {

				for (SemanticElementRelationship r : element.getSemanticElement().getRelationships()) {

					List<IElementValue> relatedElements = semanticModel.getElementValuesByName(
						r.getRelatedSemanticElement());

					for (IElementValue relatedElement : relatedElements) {
						for (IElementValue correctParent : relatedElement.getParent().getChildren()) {
							if (correctParent.getName().equals(element.getName())) {
								relatedElement.setParent(correctParent);
								correctParent.addChild(relatedElement);

							}

						}

					}

				}

			}
		}

	}

}
