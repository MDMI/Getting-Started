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
import java.util.Map;

import org.mdmi.Field;
import org.mdmi.SemanticElement;
import org.mdmi.core.ElementValueSet;
import org.mdmi.core.IElementValue;
import org.mdmi.core.engine.XElementValue;
import org.mdmi.core.engine.XValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seanmuir
 *
 */
public class CascadingSemantic extends ConfigurableSemanticProcessor {

	public CascadingSemantic() {
		super();
	}

	private static Logger logger = LoggerFactory.getLogger(CascadingSemantic.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#getName()
	 */
	@Override
	public String getName() {
		return "CascadingSemantic";
	}

	@Override
	public void setArguments(Object arguments) {
		Map map = (Map) arguments;
		// this.direction = DIRECTION.valueOf(map.get("direction").toString());

	}

	// void serializeXDataStruct(XDataStruct v, int indent) {
	// for (String field : v.getFields()) {
	// if (v.getValue(field) != null) {
	// if (v.getValue(field) instanceof String) {
	// logger.trace(StringUtils.repeat(".", indent + 2) + " " + field + " = " + v.getValue(field));
	// } else if (v.getValue(field) instanceof XDataStruct) {
	// logger.trace(StringUtils.repeat("-", indent + 2) + "> " + field);
	// serializeXDataStruct((XDataStruct) v.getValue(field), indent + 4);
	// } else {
	// logger.trace(v.getValue(field).getClass() + " " + v.getValue(field));
	// }
	// }
	// }
	// }

	void collectMapTarget(String target) {

	}

	void cascade(IElementValue semanticElement, IElementValue cascadingValue, String target) {

		String businessElementName = "NONE";

		// if (semanticElement.getXValue().getValue() instanceof XDataStruct) {
		// logger.trace(
		// StringUtils.repeat("-", indent) + "> " + semanticElement.getName() + " (" + businessElementName + ")");
		// XDataStruct v = (XDataStruct) semanticElement.getXValue().getValue();
		// serializeXDataStruct(v, indent);
		// } else {
		// logger.trace(
		// StringUtils.repeat("-", indent) + "> " + semanticElement.getName() + " (" + businessElementName + ")" +
		// " = " + semanticElement.getXValue().getValue());
		// }

		for (IElementValue childSemanticElement : semanticElement.getChildren()) {
			cascade(childSemanticElement, cascadingValue, target);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.semanticprocessors.ISemanticProcessor#processSemanticModel(org.mdmi.core.ElementValueSet)
	 */
	@Override
	public void processSemanticModel(ElementValueSet elementValueSet) {

		XElementValue cascadeSource = null;
		// XElementValue cascadetarget = null;
		ArrayList<XElementValue> cascadetargets = new ArrayList<>();
		XElementValue cascadeParent = null;
		XElementValue idvalue = null;
		XElementValue typevalue = null;
		SemanticElement targetsemantic = null;
		for (IElementValue elementValue : elementValueSet.getAllElementValues()) {
			if ("AuthorCascading".equals(elementValue.getSemanticElement().getName())) {
				cascadeParent = (XElementValue) elementValue;
			}

			if ("AssignedAuthorCascading".equals(elementValue.getSemanticElement().getName())) {
				cascadeSource = (XElementValue) elementValue;
			}

			if ("AllergyConcernAct2".equals(elementValue.getSemanticElement().getName())) {
				cascadetargets.add((XElementValue) elementValue);
			}

			if ("Id9Cascading".equals(elementValue.getSemanticElement().getName())) {
				idvalue = (XElementValue) elementValue;
			}

			if ("Type2345Cascading".equals(elementValue.getSemanticElement().getName())) {
				typevalue = (XElementValue) elementValue;
			}

		}

		for (IElementValue cascadetarget : cascadetargets) {

			boolean process = true;
			for (IElementValue checkFor : cascadetarget.getChildren()) {

				if ("AuthorCascadingTarget".equals(checkFor.getSemanticElement().getName())) {
					process = false;
				}

			}
			if (process && cascadeSource != null && cascadetarget != null) {

				for (SemanticElement foo : cascadeSource.getSemanticElement().getElementSet().getSemanticElements()) {
					if ("AuthorCascadingTarget".equals(foo.getName())) {
						targetsemantic = foo;
						XElementValue t = new XElementValue(targetsemantic, elementValueSet);
						cascadetarget.addChild(t);
						cascadetarget = t;
					}
				}

				for (SemanticElement foo : cascadeSource.getSemanticElement().getElementSet().getSemanticElements()) {
					if ("Id46CascadingTarget".equals(foo.getName())) {
						targetsemantic = foo;
						XElementValue t = new XElementValue(targetsemantic, elementValueSet);
						if (idvalue != null) {

							XValue whatisthis = idvalue.getXValue();

							for (Field f : whatisthis.getDatatype().getFields()) {
								// System.out.println(f.getName());
								t.getXValue().addValue(f.getName(), idvalue.getXValue().getValueByName(f.getName()));

							}

						}

						cascadetarget.addChild(t);
					}
				}

				for (SemanticElement foo : cascadeSource.getSemanticElement().getElementSet().getSemanticElements()) {
					if ("Type2345128401CascadingTarget".equals(foo.getName())) {
						targetsemantic = foo;
						XElementValue t = new XElementValue(targetsemantic, elementValueSet);
						if (typevalue != null) {
							XValue whatisthis = typevalue.getXValue();
							for (Field f : whatisthis.getDatatype().getFields()) {
								t.getXValue().addValue(f.getName(), typevalue.getXValue().getValueByName(f.getName()));
							}
						}
						cascadetarget.addChild(t);
					}
				}

			}
		}
		if (cascadeParent != null)

		{

			elementValueSet.removeElementValue(cascadeParent);

			cascadeParent.getParent().removeChild(cascadeParent);
		}

	}

}
