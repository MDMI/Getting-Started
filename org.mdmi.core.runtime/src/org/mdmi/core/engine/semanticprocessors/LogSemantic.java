/*******************************************************************************
 * Copyright (c) 2021 seanmuir.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.ConversionRule;
import org.mdmi.MessageModel;
import org.mdmi.core.ElementValueSet;
import org.mdmi.core.IElementValue;
import org.mdmi.core.engine.XDataStruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seanmuir
 *
 */
public class LogSemantic implements ISemanticProcessor {

	public enum DIRECTION {
		FROM, TO
	};

	DIRECTION direction;

	/**
	 * @param direction
	 */
	public LogSemantic(DIRECTION direction) {
		super();
		this.direction = direction;
	}

	private static Logger logger = LoggerFactory.getLogger(LogSemantic.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#getName()
	 */
	@Override
	public String getName() {
		return "LogSemantic";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#canProcess(org.mdmi.MessageModel)
	 */
	@Override
	public boolean canProcess(MessageModel messageModel) {
		return logger.isTraceEnabled();
	}

	void serializeXDataStruct(XDataStruct v, int indent) {
		for (String field : v.getFields()) {
			if (v.getValue(field) != null) {
				if (v.getValue(field) instanceof String) {
					logger.trace(StringUtils.repeat(".", indent + 2) + " " + field + " = " + v.getValue(field));
				} else if (v.getValue(field) instanceof XDataStruct) {
					logger.trace(StringUtils.repeat("-", indent + 2) + "> " + field);
					serializeXDataStruct((XDataStruct) v.getValue(field), indent + 4);
				} else {
					logger.trace(v.getValue(field).getClass() + " " + v.getValue(field));
				}
			}
		}
	}

	void log(IElementValue semanticElement, int indent) {
		String businessElementName = "NONE";

		if (direction.equals(DIRECTION.TO)) {
			for (ConversionRule fromRule : semanticElement.getSemanticElement().getMapToMdmi()) {
				businessElementName = fromRule.getBusinessElement().getName();
			}
		} else {
			for (ConversionRule fromRule : semanticElement.getSemanticElement().getMapFromMdmi()) {
				businessElementName = fromRule.getBusinessElement().getName();
			}
		}

		if (semanticElement.getXValue().getValue() instanceof XDataStruct) {
			logger.trace(
				StringUtils.repeat("-", indent) + "> " + semanticElement.getName() + " (" + businessElementName + ")");
			XDataStruct v = (XDataStruct) semanticElement.getXValue().getValue();
			serializeXDataStruct(v, indent);
		} else {
			logger.trace(
				StringUtils.repeat("-", indent) + "> " + semanticElement.getName() + " (" + businessElementName + ")" +
						" = " + semanticElement.getXValue().getValue());
		}

		for (IElementValue childSemanticElement : semanticElement.getChildren()) {
			log(childSemanticElement, indent + 2);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#processSemanticModel(org.mdmi.core.ElementValueSet)
	 */
	@Override
	public void processSemanticModel(ElementValueSet semanticModel) {

		List<IElementValue> parents = new ArrayList<IElementValue>();

		for (IElementValue semanticElement : semanticModel.getAllElementValues()) {
			if (semanticElement.getParent() == null) {
				parents.add(semanticElement);
			}
		}

		for (IElementValue semanticElement : parents) {
			log(semanticElement, 1);
		}
		System.out.println("");
	}

}
