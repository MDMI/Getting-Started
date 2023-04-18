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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.mdmi.Bag;
import org.mdmi.Choice;
import org.mdmi.DTCChoice;
import org.mdmi.DTCStructured;
import org.mdmi.DTSDerived;
import org.mdmi.DTSEnumerated;
import org.mdmi.DTSPrimitive;
import org.mdmi.EnumerationLiteral;
import org.mdmi.Field;
import org.mdmi.LeafSyntaxTranslator;
import org.mdmi.MDMIDatatype;
import org.mdmi.MessageGroup;
import org.mdmi.MessageModel;
import org.mdmi.Node;
import org.mdmi.SemanticElement;
import org.mdmi.SemanticElementRelationship;
import org.mdmi.SemanticElementSet;
import org.mdmi.SemanticElementType;
import org.mdmi.core.ElementValueSet;
import org.mdmi.core.IElementValue;
import org.mdmi.core.ISemanticParser;
import org.mdmi.core.ISyntaxNode;
import org.mdmi.core.MdmiException;
import org.mdmi.core.MdmiUtil;
import org.mdmi.impl.MDMIPackageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSemanticParser implements ISemanticParser {

	private static Logger logger = LoggerFactory.getLogger(DefaultSemanticParser.class);

	private ElementValueSet valueSet;

	protected static Map<String, SemanticInterpreter> semanticRollupInterpreters = new HashMap<>();

	MessageGroup sourceMessageGroup = null;

	// MessageGroup targetMessageGroup = null;

	public DefaultSemanticParser(MessageGroup sourceMessageGroup) {
		this.sourceMessageGroup = sourceMessageGroup;
	}

	protected SemanticInterpreter getSemanticInterpreter() {
		String key = sourceMessageGroup.getName() + "__SEMANTICINTERPRETER__" +
				sourceMessageGroup.getModels().get(0).getMessageModelName();
		if (!semanticRollupInterpreters.containsKey(key)) {
			semanticRollupInterpreters.put(key, new SemanticInterpreter(sourceMessageGroup));
		}

		return semanticRollupInterpreters.get(key);
	}

	@Override
	public void buildSemanticModel(MessageModel mdl, ISyntaxNode yroot, ElementValueSet eset, Properties properties) {

		if (mdl == null || yroot == null || eset == null) {
			throw new IllegalArgumentException("Null argument!");
		}
		valueSet = eset;
		Node root = mdl.getSyntaxModel().getRoot();
		if (root != yroot.getNode()) {
			throw new MdmiException(
				"Invalid root node, expected {0}, found {1}.", root.getName(), (yroot.getNode() == null
						? "null"
						: yroot.getNode().getName()));
		}

		setInitialValues(mdl, properties);

		// 1. create all XElementValues
		getElements((YNode) yroot);

		// 2. set relationships
		List<IElementValue> xes = valueSet.getAllElementValues();
		for (IElementValue xe : xes) {
			setRelations((XElementValue) xe);
		}

		// 3. set computed SEs
		SemanticElementSet set = mdl.getElementSet();
		Collection<SemanticElement> ses = set.getSemanticElements();
		for (SemanticElement se : ses) {

			if (se.isComputed() && !se.isNullFlavor()) {
				setComputedValue(se, eset, properties);
			} else if (se.isComputedOut()) {
				if (se.getParent() != null) {
					List<IElementValue> elements = eset.getElementValuesByType(se.getParent());
					for (IElementValue element : elements) {
						setComputedOutValue(se, properties, element);
					}
				} else {
					setComputedOutValue(se, properties, null);
				}
			}
		}
	}

	void searchForParent(IElementValue element, SemanticElement searchfor, List<IElementValue> theList) {
		if (element.getSemanticElement().getName().equals(searchfor.getName())) {
			theList.add(element);
		}
		for (IElementValue child : element.getChildren()) {
			searchForParent(child, searchfor, theList);
		}

	}

	@Override
	public ISyntaxNode createNewSyntacticModel(MessageModel mdl, ElementValueSet eset, Properties properties) {
		Node root = mdl.getSyntaxModel().getRoot();
		if (root.getMinOccurs() != 1 || root.getMaxOccurs() != 1) {
			throw new MdmiException("Invalid mapping for node " + MdmiUtil.getNodePath(root));
		}
		YNode yroot = createYNode(root);
		updateSyntacticModel(mdl, eset, yroot, properties);
		return yroot;
	}

	@Override
	public void updateSyntacticModel(MessageModel mdl, ElementValueSet eset, ISyntaxNode yr, Properties properties) {
		if (mdl == null || eset == null || yr == null) {
			throw new IllegalArgumentException("Null argument!");
		}
		YNode yroot = (YNode) yr;

		// 1. set computed SEs
		SemanticElementSet set = mdl.getElementSet();
		Collection<SemanticElement> ses = set.getSemanticElements();
		for (SemanticElement se : ses) {
			if (se.isComputedIn()) {
				setComputedInValue(se, properties);
			}
		}

		// 2. Update the syntax model
		Node root = mdl.getSyntaxModel().getRoot();
		if (root.getMinOccurs() != 1 || root.getMaxOccurs() != 1) {
			throw new MdmiException("Invalid mapping for root node " + MdmiUtil.getNodePath(root));
		}
		XElementValues values = new XElementValues(eset);
		// top level XElementValues
		for (XElementValues.XES xes : values.elementValues) {
			YNode ynode = ensureAbsolutePath(yroot, xes.semanticElement);
			List<YNode> ynodes = new ArrayList<>();
			int n = xes.elementValues.size();
			if (n == 1) {
				ynodes.add(ynode);
			} else {
				YNode parent = (YNode) ynode.getParent();
				if (parent != null) {
					ynodes = ensureParentHasChildren(parent, ynode.getNode(), n);
				}
			}
			for (int i = 0; i < n; i++) {
				XElementValues.XE xe = xes.elementValues.get(i);
				ynode = ynodes.get(i);
				setYNodeValuesAndChildren(ynode, xe);
			}
		}
	}

	private void getElements(YNode yroot) {
		Node node = yroot.getNode();

		SemanticElement me = node.getSemanticElement();
		if (me == null) {
			// node is not mapped to an element or field (one of the top nodes)
			if (yroot instanceof YBag) {
				YBag y = (YBag) yroot;
				LinkedList<YNode> yns = y.getYNodes();
				for (YNode yn : yns) {
					getElements(yn);
				}
			} else if (yroot instanceof YChoice) {
				YChoice y = (YChoice) yroot;
				ArrayList<YNode> yns = y.getYNodes();
				for (YNode yn : yns) {
					getElements(yn);
				}
			}
			// else we have a not-mapped Leaf, we ignore it
		} else {
			// top node is mapped to an element, no owner
			getMappedElement(yroot, null);
		}
	}

	private void getMappedElement(YNode yroot, XElementValue owner) {
		if (yroot.isLeaf()) {
			getSimpleElement((YLeaf) yroot, owner);
		} else if (yroot.isChoice()) {
			getChoiceElement((YChoice) yroot, owner);
		} else if (yroot.isBag()) { // Bag
			getStructElement((YBag) yroot, owner);
		} else {
			throw new IllegalArgumentException("Invalid YNode!");
		}
	}

	// a leaf mapped to a simple type (must be simple)
	private void getSimpleElement(YLeaf yleaf, XElementValue owner) {
		SemanticElement me = yleaf.getLeaf().getSemanticElement();
		if (me == null) {
			throw new MdmiException("Null SE for node {0}", MdmiUtil.getNodePath(yleaf.getNode()));
		}
		XElementValue xe = new XElementValue(me, valueSet);
		if (owner != null) {
			if (!xe.getSemanticElement().getParent().getName().equals(owner.getSemanticElement().getName())) {
				XElementValue inject = new XElementValue(me.getParent(), valueSet);
				inject.addChild(xe);
				owner.addChild(inject);
			} else {
				owner.addChild(xe);
			}
		}

		// set the value
		MDMIDatatype dt = me.getDatatype();
		if (!(dt.isSimple() || dt.isExternal())) {
			// dt.isSimple();
			if ((!"InstanceIdentifier".equals(dt.getName())) && (!"Telecom".equals(dt.getName()))) {
				throw new MdmiException("Invalid mapping for node " + MdmiUtil.getNodePath(yleaf.getNode()));
			}
		}

		try {
			String value = yleaf.getValue();
			String format = yleaf.getLeaf().getFormat();
			XDT xdt = XDT.fromString(format);
			if (dt.isPrimitive()) {
				DTSPrimitive pdt = (DTSPrimitive) dt;
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				Object o = XDT.convertFromString(xdt, value, format, pdt);
				xe.getXValue().addValue(o);
			} else if (dt.isDerived()) {
				DTSDerived ddt = (DTSDerived) dt;
				DTSPrimitive pdt = ddt.getPrimitiveBaseType();
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				Object o = XDT.convertFromString(xdt, value, format, pdt);
				xe.getXValue().addValue(o);
			} else if (dt.isExternal()) {
				DTSPrimitive pdt = MDMIPackageImpl.STRING;
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				Object o = XDT.convertFromString(xdt, value, format, pdt);
				xe.getXValue().addValue(o);
				// }
			} else if ("InstanceIdentifier".equals(dt.getName())) {
				XDataStruct xs = new XDataStruct(xe.getXValue(), true);
				xs.setValueSafely("extension", value);
				xe.getXValue().addValue(xs);
			} else if ("Telecom".equals(dt.getName())) {
				XDataStruct xs = new XDataStruct(xe.getXValue(), true);
				xs.setValueSafely("value", value);
				xe.getXValue().addValue(xs);

			} else {
				DTSEnumerated edt = (DTSEnumerated) dt;
				EnumerationLiteral el = edt.getLiteralByCode(value);
				xe.getXValue().addValue(el);
			}
		} catch (Throwable throwable) {
			throw new MdmiException("Error proccessing node " + MdmiUtil.getNodePath(yleaf.getNode()), throwable);
		}
	}

	private void setInitialValues(MessageModel mdl, Properties properties) {
		// JSONParser parser = new JSONParser();
		// Object obj;
		// try {
		// String values = properties.getProperty("InitialValues");
		// if (StringUtils.isEmpty(values)) {
		// return;
		// }
		//
		// logger.trace("Initial Values " + values);
		// obj = parser.parse(values);
		// JSONObject initialValues = (JSONObject) obj;
		// Consumer<String> action = new Consumer<>() {
		// @Override
		// public void accept(String semanticElementName) {
		// MDMIBusinessElementReference be = null;
		// // @TODO Fix look up BER by name
		// SemanticElement me = mdl.getElementSet().getSemanticElement(be);
		// if (me != null && me.getDatatype() != null) {
		// XElementValue xe = new XElementValue(me, valueSet);
		// XDataStruct xs = new XDataStruct(xe.getXValue(), true);
		// me.getDatatype();
		// JSONObject semanticElement = (JSONObject) initialValues.get(semanticElementName);
		// Consumer<String> action2 = new Consumer<>() {
		// @Override
		// public void accept(String property) {
		// logger.trace(property + " : " + semanticElement.get(property));
		// xs.setValueSafely(property, semanticElement.get(property));
		// xe.getXValue().addValue(xs);
		// }
		// };
		// semanticElement.keySet().forEach(action2);
		// }
		// }
		// };
		// initialValues.keySet().forEach(action);
		// } catch (Exception e) {
		// logger.error("setInitialValues", e);
		// }

	}

	private void getChoiceElement(YChoice ychoice, XElementValue owner) {

		ychoice.getChoice();

		for (YNode y : ychoice.getYNodes()) {

			if (y.getNode().getSemanticElement() != null) {
				getMappedElement(y, owner);
			}

		}

	}

	private void getStructElement(YBag ybag, XElementValue owner) {
		SemanticElement me = ybag.getBag().getSemanticElement();
		if (me == null) {
			throw new IllegalArgumentException("Missing Semantic Elements for " + MdmiUtil.getNodePath(ybag.getNode()));
		}
		XElementValue xe = new XElementValue(me, valueSet);
		if (owner != null) {
			// set parent-child relationship
			owner.addChild(xe);
		}

		// set the value
		MDMIDatatype dt = me.getDatatype();
		if (dt == null || !dt.isStruct()) {
			return;
		}

		try {
			XDataStruct xs = new XDataStruct(xe.getXValue(), true);
			xe.getXValue().addValue(xs);

			LinkedList<YNode> yns = ybag.getYNodes();
			for (YNode yn : yns) {

				Node n = yn.getNode();
				if (n.getSemanticElement() != null) {
					// child element
					getMappedElement(yn, xe);
				} else if (yn instanceof YChoice) {
					getMappedElement(yn, xe);
				} else {
					// field
					Stack<Node> nodes = new Stack<>();
					Node currentNode = n;

					while (currentNode != null && currentNode.getSemanticElement() == null) {
						nodes.push(currentNode);
						currentNode = currentNode.getParentNode();
					}

					String fieldName = n.getFieldName();
					if (fieldName == null) {
						continue; // will ignore it for now, assuming unmapped syntax node
					}
					XValue xv = null;
					while (!nodes.isEmpty()) {
						Node nn = nodes.pop();
						if (xv != null && xv.getValue() != null) {
							try {
								xv = ((XDataStruct) xv.getValue()).getXValue(nn.getFieldName());
							} catch (ClassCastException cce) {

							}

						} else {
							if (nn.getFieldName() != null) {
								xv = xs.getXValue(nn.getFieldName());
							}
						}

					}

					if (xv != null) {
						getValue(yn, xv, xe);
					}
				}
			}
		} catch (

		Throwable throwable) {

			throw new MdmiException(
				throwable,
				"Error proccessing node " + me.getName() + " at location " + MdmiUtil.getNodePath(ybag.getNode()));
		}
	}

	private void getValue(YNode yroot, XValue xv, XElementValue owner) {
		if (yroot.isLeaf()) {
			getValueLeaf((YLeaf) yroot, xv);
		} else if (yroot.isChoice()) {
			getValueChoice((YChoice) yroot, xv, owner);
		} else if (yroot.isBag()) { // Bag
			getValueStruct((YBag) yroot, xv, owner);
		} else {
			throw new IllegalArgumentException("Invalid YNode!");
		}
	}

	private void getValueLeaf(YLeaf yleaf, XValue xv) {
		MDMIDatatype dt = xv.getDatatype();
		if (dt == null || !(dt.isSimple() || dt.isExternal())) {
			return;
		}

		try {
			String value = yleaf.getValue();
			String format = yleaf.getLeaf().getFormat();
			XDT xdt = XDT.fromString(format);
			if (xdt != null) {
				format = null; // null the format so it does not get into the
				// conversions
			}
			if (dt.isPrimitive()) {
				DTSPrimitive pdt = (DTSPrimitive) dt;
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				Object o = XDT.convertFromString(xdt, value, format, pdt);
				xv.addValue(o);
			} else if (dt.isDerived()) {
				DTSDerived ddt = (DTSDerived) dt;
				DTSPrimitive pdt = ddt.getPrimitiveBaseType();
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				Object o = XDT.convertFromString(xdt, value, format, pdt);
				xv.addValue(o);
			} else if (dt.isExternal()) {
				// DTExternal dte = (DTExternal) dt;
				// String uri = dte.getTypeSpec();
				// // if (uri != null) {
				// // Object o = Mdmi.INSTANCE().getExternalResolvers().getDictionaryValue(dte, value);
				// // xv.addValue(o);
				// // } else {
				// DTSPrimitive pdt = MDMIPackageImpl.STRING;
				// if (xdt == null) {
				// xdt = XDT.fromPDT(pdt);
				// }
				// Object o = XDT.convertFromString(xdt, value, format, pdt);
				// xv.addValue(o);
				// // }
			} else {
				DTSEnumerated edt = (DTSEnumerated) dt;
				EnumerationLiteral el = edt.getLiteralByCode(value);
				xv.addValue(el);
			}
		} catch (Throwable throwable) {
			throw new MdmiException("Error proccessing node " + MdmiUtil.getNodePath(yleaf.getNode()), throwable);
		}
	}

	private void getValueChoice(YChoice ychoice, XValue xv, XElementValue owner) {
		MDMIDatatype dt = xv.getDatatype();
		if (!dt.isChoice()) {
			throw new MdmiException("Invalid mapping for node " + MdmiUtil.getNodePath(ychoice.getNode()));
		}
		XDataChoice xc = new XDataChoice(xv);
		xv.addValue(xc);

		ArrayList<YNode> yns = ychoice.getYNodes();
		if (yns.size() > 0) { // otherwise its an empty choice
			YNode yn0 = yns.get(0);
			Node n = yn0.getNode();
			if (n.getSemanticElement() != null) {
				// the choice has only child elements
				for (YNode yn : yns) {
					getMappedElement(yn, owner);
				}
			} else {
				// the choice has only fields
				String fieldName = n.getFieldName();
				XValue xvalue = xc.setXValue(fieldName);
				for (YNode yn : yns) {
					getValue(yn, xvalue, owner);
				}
			}
		}
	}

	private void getValueStruct(YBag ybag, XValue xv, XElementValue owner) {
		MDMIDatatype dt = xv.getDatatype();
		if (!dt.isStruct()) {
			// throw new MdmiException("Invalid mapping for node " + DefaultSyntacticParser.getNodePath(ybag.getNode()));
			return;
		}
		XDataStruct xs = new XDataStruct(xv);
		xv.addValue(xs);

		LinkedList<YNode> yns = ybag.getYNodes();
		for (YNode yn : yns) {
			Node n = yn.getNode();
			if (n.getSemanticElement() != null) {
				// child element
				getMappedElement(yn, owner);
			} else {
				// field
				String fieldName = n.getFieldName();
				XValue xvalue = xs.getXValue(fieldName);
				if (xvalue == null) {
					throw new MdmiException("Invalid mapping for node " + MdmiUtil.getNodePath(ybag.getNode()));
				}
				getValue(yn, xvalue, owner);
			}
		}
	}

	private void setRelations(XElementValue xe) {
		SemanticElement me = xe.getSemanticElement();
		Collection<SemanticElementRelationship> rels = me.getRelationships();
		for (SemanticElementRelationship rel : rels) {
			SemanticElement trg = rel.getRelatedSemanticElement();
			// 1. Check child elements
			if (trg == null) {
				continue;
			}
			List<IElementValue> xevs = valueSet.getChildElementValuesByType(trg, xe);
			if (xevs != null && 0 < xevs.size()) {
				for (IElementValue xev : xevs) {
					xe.addRelation(rel.getName(), xev);
				}
				return;
			}
			// 2. Check child elements of the owner
			xevs = valueSet.getOwnerElementValuesByType(trg, xe);
			if (xevs != null && 0 < xevs.size()) {
				for (IElementValue xev : xevs) {
					xe.addRelation(rel.getName(), xev);
				}
				return;
			}
			// 3. Last resort: get all
			xevs = valueSet.getElementValuesByType(trg);
			for (IElementValue xev : xevs) {
				xe.addRelation(rel.getName(), xev);
			}
		}
	}

	// ==================================================================================================================
	// create top-level root node
	private YNode createYNode(Node node) {
		if (node instanceof Bag) {
			return new YBag((Bag) node, null);
		}
		if (node instanceof Choice) {
			return new YChoice((Choice) node, null);
		}
		return new YLeaf((LeafSyntaxTranslator) node, null);
	}

	/**
	 * Set the value(s) and then the children recursively for the specified YNode
	 * from the given element value wrapper.
	 *
	 * @param ynode
	 * @param xe
	 */
	private void setYNodeValuesAndChildren(YNode ynode, XElementValues.XE xe) {
		// 1. set the value of the ynode first
		Object value = xe.elementValue.getXValue().getValue();
		setYNodeValues(ynode, value);

		// 2. recursively go through its child nodes and set the values
		for (XElementValues.XES xes : xe.children) {
			YNode ychild = ensureRelativePath(ynode, xes.semanticElement);
			List<YNode> ynodes = new ArrayList<>();
			int n = xes.elementValues.size();
			if (n == 1) {
				ynodes.add(ychild);
			} else {
				ynodes = ensureParentHasChildren(ynode, ychild.getNode(), n);
			}
			for (int i = 0; i < n; i++) {
				XElementValues.XE xeChild = xes.elementValues.get(i);
				ychild = ynodes.get(i);
				setYNodeValuesAndChildren(ychild, xeChild);
			}
		}
	}

	// set the node value(s) for all fields
	private void setYNodeValues(YNode ynode, Object value) {
		if (value == null) {
			return;
		}
		Node node = ynode.getNode();
		if (node instanceof Bag) {
			if (!(value instanceof XDataStruct)) {
				throw new IllegalArgumentException(
					"Invalid map: expected XDataStruct for node " + ynode.getNode().getName() + " : " +
							MdmiUtil.getNodePath(ynode.getNode()));
			}
			setYNodeValuesForBag((YBag) ynode, (XDataStruct) value);
		} else if (node instanceof Choice) {
			if (!(value instanceof XDataChoice)) {
				throw new IllegalArgumentException(
					"Invalid map: expected XDataChoice for node " + ynode.getNode().getName() + " : " +
							MdmiUtil.getNodePath(ynode.getNode()));
			}
			setYNodeValuesForChoice((YChoice) ynode, (XDataChoice) value);
		} else { // Leaf
			setLeafValue((YLeaf) ynode, value);
		}
	}

	private void setYNodeValuesForBag(YBag ybag, XDataStruct xds) {
		if (xds.getXValues().size() <= 0) {
			return; // nothing to set
		}
		Node node = ybag.getNode();
		Bag bag = (Bag) node;
		Collection<Node> nodes = bag.getNodes();
		for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
			Node n = it.next();
			String fieldName = n.getFieldName();
			if (fieldName == null) {
				continue;
			}
			XValue xvalue = xds.getXValue(fieldName);
			if (xvalue == null || xvalue.size() <= 0) {
				continue;
			}
			for (int i = 0; i < xvalue.size(); i++) {
				Object xv = xvalue.getValue(i);
				YNode yn = ensureYNodeExists(ybag, n, i);
				setYNodeValues(yn, xv);
			}
		}
	}

	private void setYNodeValuesForChoice(YChoice ychoice, XDataChoice xdc) {
		if (xdc.getXValue() == null) {
			return;
		}
		XValue xvalue = xdc.getXValue();
		if (xvalue != null && xvalue.size() > 0) {
			Node sel = ychoice.getChoice().getNode(xvalue.getName());
			for (int i = 0; i < xvalue.size(); i++) {
				Object xv = xvalue.getValue(i);
				YNode yn = ensureYNodeExists(ychoice, sel, i);
				setYNodeValues(yn, xv);
			}
		}
	}

	private void setLeafValue(YLeaf yleaf, Object value) {
		if (value == null) {
			return;
		}
		Node node = yleaf.getNode();
		MDMIDatatype dt = getDatatype(node);
		if (!(dt.isSimple() || dt.isExternal())) {
			throw new MdmiException("Invalid mapping for node " + MdmiUtil.getNodePath(yleaf.getNode()));
		}
		try {
			String format = yleaf.getLeaf().getFormat();
			XDT xdt = XDT.fromString(format);
			String v = null;
			if (dt.isPrimitive()) {
				DTSPrimitive pdt = (DTSPrimitive) dt;
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				v = XDT.convertToString(pdt, value, format, xdt);
			} else if (dt.isDerived()) {
				DTSDerived ddt = (DTSDerived) dt;
				DTSPrimitive pdt = ddt.getPrimitiveBaseType();
				if (xdt == null) {
					xdt = XDT.fromPDT(pdt);
				}
				v = XDT.convertToString(pdt, value, format, xdt);
			} else if (dt.isExternal()) {
				// DTExternal dte = (DTExternal) dt;
				// String uri = dte.getTypeSpec();
				// if (uri != null) {
				// v = Mdmi.INSTANCE().getExternalResolvers().getModelValue(dte, value);
				// } else {
				// DTSPrimitive pdt = MDMIPackageImpl.STRING;
				// if (xdt == null) {
				// xdt = XDT.fromPDT(pdt);
				// }
				// v = XDT.convertToString(pdt, value, format, xdt);
				// }
			} else {
				DTSEnumerated edt = (DTSEnumerated) dt;
				if (value instanceof EnumerationLiteral) {
					v = ((EnumerationLiteral) value).getCode();
				} else if (value instanceof String) {
					v = (String) value;
				} else {
					throw new MdmiException("Invalid enum conversion for type {0} value {1}", edt.getTypeName(), value);
				}
			}
			yleaf.setValue(v);
		} catch (Throwable throwable) {
			throw new MdmiException("Error proccessing node " + MdmiUtil.getNodePath(yleaf.getNode()), throwable);
		}
	}

	private MDMIDatatype getDatatype(Node node) {
		ArrayList<String> fns = new ArrayList<>();
		Node n = node;
		while (n.getSemanticElement() == null) {
			fns.add(0, n.getFieldName());
			n = n.getParentNode();
		}

		MDMIDatatype dt = n.getSemanticElement().getDatatype();
		for (int i = 0; i < fns.size(); i++) {
			String fieldName = fns.get(i);
			Field f = null;
			if (dt.isStruct()) {
				DTCStructured dts = (DTCStructured) dt;
				f = dts.getField(fieldName);
			} else if (dt.isChoice()) {
				DTCChoice dtc = (DTCChoice) dt;
				f = dtc.getField(fieldName);
			}
			if (f == null) {
				throw new MdmiException("Invalid construct for node " + MdmiUtil.getNodePath(node));
			}
			dt = f.getDatatype();
		}
		return dt;
	}

	/**
	 * Ensure the child of the specified parent exists, at the given index.
	 * Return the ynode at the given index. Will create it if necessary.
	 *
	 * @param parent
	 *            The parent ynode.
	 * @param node
	 *            The child type.
	 * @param index
	 *            The index of the child in the parent children of the same type.
	 * @return The ynode at the requested index (may be creating it if need be).
	 */
	private YNode ensureYNodeExists(YNode parent, Node node, int index) {
		YNode ynode = null;
		if (parent instanceof YBag) {
			YBag ybag = (YBag) parent;
			if (node instanceof Bag) {
				Bag bag = (Bag) node;
				List<YNode> ynodes = ybag.getYNodesForNode(bag);
				while (ynodes.size() <= index) {
					ynode = new YBag(bag, ybag);
					ybag.addYNode(ynode);
					ynodes = ybag.getYNodesForNode(bag);
				}
				return ynodes.get(index);
			} else if (node instanceof Choice) {
				Choice choice = (Choice) node;
				List<YNode> ynodes = ybag.getYNodesForNode(choice);
				while (ynodes.size() <= index) {
					ynode = new YChoice(choice, ybag);
					ybag.addYNode(ynode);
					ynodes = ybag.getYNodesForNode(choice);
				}
				return ynodes.get(index);
			} else {
				LeafSyntaxTranslator leaf = (LeafSyntaxTranslator) node;
				List<YNode> ynodes = ybag.getYNodesForNode(leaf);
				while (ynodes.size() <= index) {
					ynode = new YLeaf(leaf, ybag);
					ybag.addYNode(ynode);
					ynodes = ybag.getYNodesForNode(leaf);
				}
				return ynodes.get(index);
			}

		} else if (parent instanceof YChoice) {
			YChoice ychoice = (YChoice) parent;
			if (node instanceof Bag) {
				Bag bag = (Bag) node;
				if (ychoice.getChosenNode() != bag) {
					ychoice.forceChoice(null);
				}
				ArrayList<YNode> ynodes = ychoice.getYNodes();
				while (ynodes.size() <= index) {
					ynode = new YBag(bag, ychoice);
					ychoice.addYNode(ynode);
					ynodes = ychoice.getYNodes();
				}
				return ynodes.get(index);
			} else if (node instanceof Choice) {
				Choice choice = (Choice) node;
				if (ychoice.getChosenNode() != choice) {
					ychoice.forceChoice(null);
				}
				ArrayList<YNode> ynodes = ychoice.getYNodes();
				while (ynodes.size() <= index) {
					ynode = new YChoice(choice, ychoice);
					ychoice.addYNode(ynode);
					ynodes = ychoice.getYNodes();
				}
				return ynodes.get(index);
			} else {
				LeafSyntaxTranslator leaf = (LeafSyntaxTranslator) node;
				if (ychoice.getChosenNode() != leaf) {
					ychoice.forceChoice(null);
				}
				ArrayList<YNode> ynodes = ychoice.getYNodes();
				while (ynodes.size() <= index) {
					ynode = new YLeaf(leaf, ychoice);
					ychoice.addYNode(ynode);
					ynodes = ychoice.getYNodes();
				}
				return ynodes.get(index);
			}

		} else {
			throw new IllegalArgumentException("Invalid state: parent is a leaf!");
		}
	}

	/**
	 * Get the relative path from the node the se given is mapped to to the
	 * specified node. If the given node is null, it will return the absolute
	 * path.
	 *
	 * <pre>
	 * NodeA
	 *   NodeB
	 *     NodeC
	 *
	 * getPath(NodeA, null) returns {NodeA}
	 * getPath(NodeC, null) returns {NodeA, NodeB, NodeC}
	 * getPath(NodeC, NodeA) return {NodeB, NodeC}
	 * </pre>
	 *
	 * @param se
	 *            The semantic element mapped to the node we want an absolute path.
	 * @param node
	 *            The node relative to which we want the path (excluding the node given).
	 * @return The relative path from the node the se given is mapped to to the specified node.
	 *         If the given node is null, it will return the absolute path.
	 */
	private ArrayList<Node> getPath(SemanticElement se, Node node) {
		ArrayList<Node> path = new ArrayList<>();
		Node n = se.getSyntaxNode();
		while (n != null && n != node) {
			path.add(0, n);
			n = n.getParentNode();
		}
		return path;
	}

	/**
	 * Get the ynode to which the semantic element passed in is mapped to.
	 * Assumes the path is absolute, i.e. relative to the root of the syntax
	 * tree. Will create the ynodes on the path, if required.
	 *
	 * @param yroot
	 *            The root of the syntax tree.
	 * @param se
	 *            The semantic element to which the syntax node to look for is mapped.
	 * @return The ynode corresponding to the SE passed in.
	 */
	private YNode ensureAbsolutePath(YNode yroot, SemanticElement se) {
		try {
			ArrayList<Node> path = getPath(se, null);
			if (path.size() == 1) {
				return yroot;
			}
			int index = 1;
			YNode parent = yroot;
			do {
				Node current = path.get(index++);
				parent = ensureParentHasChild(parent, current);
			} while (index < path.size());
			return parent;
		} catch (IndexOutOfBoundsException ioobe) {
			throw new MdmiException(
				"Invalid Model - No Path Found  " + se.getName() + " :: " + MdmiUtil.getNodePath(yroot.getNode()));
		}
	}

	/**
	 * Get the ynode to which the semantic element passed in is mapped to.
	 * Assumes the path is relative to the given ynode. Will create the ynodes on
	 * the path, if required.
	 *
	 * @param ynode
	 *            The node relative to which we ensure we have a path
	 * @param se
	 *            The semantic element to which the syntax node to look for is mapped.
	 * @return The ynode corresponding to the SE passed in.
	 */
	private YNode ensureRelativePath(YNode ynode, SemanticElement se) {
		try {
			ArrayList<Node> path = getPath(se, ynode.getNode());
			int index = 0;
			YNode parent = ynode;
			do {
				Node current = path.get(index++);
				parent = ensureParentHasChild(parent, current);
			} while (index < path.size());
			return parent;
		} catch (IndexOutOfBoundsException ioobe) {
			throw new MdmiException(
				"Invalid Model - No Path Found  " + se.getName() + " :: " + MdmiUtil.getNodePath(ynode.getNode()));
		}
	}

	/**
	 * Ensure parent has at least one child of the given type.
	 *
	 * @param parent
	 *            The parent.
	 * @param childType
	 *            The child type.
	 * @return The existing node, either existing of newly created.
	 */
	private YNode ensureParentHasChild(YNode parent, Node childType) {
		if (parent.getNode() instanceof LeafSyntaxTranslator) {
			throw new MdmiException("Invalid parent, found leaf, expected a bag or a choice!");
		}
		if (parent.getNode() instanceof Bag) {
			YBag ybag = (YBag) parent;
			List<YNode> nodes = ybag.getYNodesForNode(childType);
			if (0 < nodes.size()) {
				return nodes.get(0); // get the first if more than one
			}
			// need to add a node of that type
			if (childType instanceof Bag) {
				YBag child = new YBag((Bag) childType, ybag);
				ybag.addYNode(child);
				return child;
			} else if (childType instanceof Choice) {
				YChoice child = new YChoice((Choice) childType, ybag);
				ybag.addYNode(child);
				return child;
			} else {
				YLeaf child = new YLeaf((LeafSyntaxTranslator) childType, ybag);
				ybag.addYNode(child);
				return child;
			}
		} else {
			YChoice ychoice = (YChoice) parent;
			List<YNode> nodes = ychoice.getYNodes();
			if (0 < nodes.size()) {
				return nodes.get(0); // get the first if more than one
			}
			// need to add a node of that type
			if (childType instanceof Bag) {
				YBag child = new YBag((Bag) childType, ychoice);
				ychoice.addYNode(child);
				return child;
			} else if (childType instanceof Choice) {
				YChoice child = new YChoice((Choice) childType, ychoice);
				ychoice.addYNode(child);
				return child;
			} else {
				YLeaf child = new YLeaf((LeafSyntaxTranslator) childType, ychoice);
				ychoice.addYNode(child);
				return child;
			}
		}
	}

	private List<YNode> ensureParentHasChildren(YNode parent, Node childType, int n) {
		if (parent == null || parent.getNode() instanceof LeafSyntaxTranslator) {
			throw new MdmiException("Invalid parent, found leaf, expected a bag or a choice!");
		}
		if (parent.getNode() instanceof Bag) {
			YBag ybag = (YBag) parent;
			List<YNode> nodes = ybag.getYNodesForNode(childType);
			if (n <= nodes.size()) {
				while (n < nodes.size()) {
					nodes.remove(nodes.size() - 1); // return exact number n of
					// references
				}
				return nodes;
			}
			// need to add nodes of that type
			for (int i = nodes.size(); i < n; i++) {
				if (childType instanceof Bag) {
					YBag child = new YBag((Bag) childType, ybag);
					ybag.addYNode(child);
					nodes.add(child);
				} else if (childType instanceof Choice) {
					YChoice child = new YChoice((Choice) childType, ybag);
					ybag.addYNode(child);
					nodes.add(child);
				} else {
					YLeaf child = new YLeaf((LeafSyntaxTranslator) childType, ybag);
					ybag.addYNode(child);
					nodes.add(child);
				}
			}
			return nodes;
		} else {
			YChoice ychoice = (YChoice) parent;
			ArrayList<YNode> nodes = ychoice.getYNodes();
			if (n <= nodes.size()) {
				while (n < nodes.size()) {
					nodes.remove(nodes.size() - 1); // return exact number n of
					// references
				}
				return nodes;
			}
			// need to add nodes of that type
			for (int i = nodes.size(); i < n; i++) {
				if (childType instanceof Bag) {
					YBag child = new YBag((Bag) childType, ychoice);
					ychoice.addYNode(child);
					nodes.add(child);
				} else if (childType instanceof Choice) {
					YChoice child = new YChoice((Choice) childType, ychoice);
					ychoice.addYNode(child);
					nodes.add(child);
				} else {
					YLeaf child = new YLeaf((LeafSyntaxTranslator) childType, ychoice);
					ychoice.addYNode(child);
					nodes.add(child);
				}
			}
			return nodes;
		}
	}

	@SuppressWarnings("unused")
	private String pathToString(ArrayList<Node> path, boolean isAbsolute) {
		StringBuilder sb = new StringBuilder();
		if (isAbsolute) {
			sb.append('/');
		}
		for (int i = 0; i < path.size(); i++) {
			if (0 < i) {
				sb.append('/');
			}
			sb.append(path.get(i).getName());
		}
		return sb.toString();
	}

	/**
	 *
	 * @implNote Need to revisit the computed value
	 *
	 *           SEMANTICROLLUP
	 *           Simplify option - use a rule such a set field
	 *           Optimize by putting map roll ups into script library
	 *           Support Structure to Structure for roll ups
	 *
	 *
	 * @param se
	 * @param elementValueSet
	 * @param properties
	 */
	private void setComputedValue(SemanticElement se, ElementValueSet elementValueSet, Properties properties) {

		String rule = se.getComputedValue().getExpression();
		se.getComputedValue().getLanguage();
		HashMap<IElementValue, ArrayList<IElementValue>> valuesByParent = new HashMap<>();
		HashMap<SemanticElement, String> rulesBySemanticElement = new HashMap<>();

		if (rule.equals("SEMANTICROLLUP")) {
			/*
			 * SEMANTICROLLUP provides a mechanism for multiple local semantic elements be "rolled up" into one normal semantic
			 * element based on semantic relationships
			 * This is currently required for when content is related but syntactically mixed in with other elements
			 * for instance religionCode and religionName are not isolated and the syntax processors do not currently
			 * provide overlapped matching - perhaps supporting more sophisticated matching is required but now this is
			 * an isolated requirement
			 *
			 * <pid>2939;19</pid>
			 * <religionCode>urn:va:pat-religion:99</religionCode>
			 * <religionName>ROMAN CATHOLIC CHURCH</religionName>
			 * <scPercent>0</scPercent>
			 * <sensitive>false</sensitive>
			 * <serviceConnected>true</serviceConnected>
			 *
			 * using StringEscapeUtils.escapeEcmaScript because current apache in eclipse does not have StringEscapeUtils.escapeJavaScript
			 * StringEscapeUtils.escapeEcmaScript is the same as StringEscapeUtils.escapeJava except it encodes single quotes ' to \'
			 *
			 */

			logger.trace("Processing SEMANTICROLLUP " + se.getName());
			// organize data by parent and rules by semantic element
			for (SemanticElementRelationship relationship : se.getRelationships()) {

				logger.trace(
					"Processing SEMANTICROLLUP Relationship " + relationship.getRelatedSemanticElement().getName());
				rulesBySemanticElement.put(relationship.getRelatedSemanticElement(), relationship.getRule());

				if (logger.isTraceEnabled()) {
					if (elementValueSet.getElementValuesByType(relationship.getRelatedSemanticElement()).isEmpty()) {

						logger.trace(
							"Processing SEMANTICROLLUP Relationship No value found for  " +
									relationship.getRelatedSemanticElement().getName());

					}
				}
				if (elementValueSet.hasElementValuesByName(relationship.getRelatedSemanticElement())) {

					for (IElementValue iev2 : elementValueSet.getElementValuesByType(
						relationship.getRelatedSemanticElement())) {

						logger.trace("Walk the elements " + iev2.getName());
						IElementValue theParentForRollup = iev2.getParent();

						logger.trace("theParentForRollup  " + theParentForRollup);
						/*
						 * Loop for the IElementValue parent - this allows for roll ups where the content is not at the same level
						 */
						while ((theParentForRollup != null) && (theParentForRollup.getSemanticElement() != null) &&
								!theParentForRollup.getSemanticElement().getName().equals(se.getParent().getName())) {
							theParentForRollup = theParentForRollup.getParent();
						}

						if (theParentForRollup != null) {
							if (!valuesByParent.containsKey(theParentForRollup)) {
								valuesByParent.put(theParentForRollup, new ArrayList<IElementValue>());
							}

							valuesByParent.get(theParentForRollup).add(iev2);
						} else {

							// check for root owned
							logger.error("Invalid Semantic Rollup Relationship, no apparent parent " + se.getName());
						}

					}
				}
			}

			logger.trace("Root level parent = null" + valuesByParent.isEmpty());
			if (valuesByParent.isEmpty()) {
				if (se.getParent() == null) {
					logger.trace("Root level parent = null");
				} else {
					logger.trace("Root level parent" + se.getParent());
				}
			}

			/**
			 * @TODO Need to properly define roll up functionality and relationships
			 *
			 *       SCRIPT BASED - Use java script to set values leveraging LOCALSEMANTICVALUE
			 *
			 *       FUNCTION BASED - Use a java function - was added for performance - could be moot if we can compile computed scripts
			 *
			 *       COMPLEX TYPE BASED - Uses field to field
			 *
			 *       CONTAINER BASED - sets the parent container - no manipulation of value
			 *
			 *
			 *
			 */
			// for each parent - create a new element and roll up all the values leveraging the relationship rule
			for (IElementValue parentValue : valuesByParent.keySet()) {
				XElementValue computedInElement = new XElementValue(se, elementValueSet);
				parentValue.addChild(computedInElement);
				computedInElement.setParent(parentValue);
				for (IElementValue rollupValue : valuesByParent.get(parentValue)) {
					// Need a value to process
					if (rollupValue.getXValue().getValue() != null) {
						if (rollupValue.getXValue().getValue() instanceof String) {
							String rollupRule = rulesBySemanticElement.get(rollupValue.getSemanticElement());
							if (!StringUtils.isEmpty(rollupRule)) {
								logger.trace("Setting value to rollup " + (String) rollupValue.getXValue().getValue());
								if (rollupRule.startsWith("SEMANTICROLLUPFUNCTION:")) {
									switch (parseFunctionName(rollupRule)) {
										case "setDocumentHomeId":
											setDocumentHomeId(
												computedInElement, StringEscapeUtils.escapeEcmaScript(
													(String) rollupValue.getXValue().getValue()));
											break;
										case "setRepositoryId":
											setRepositoryId(
												computedInElement, StringEscapeUtils.escapeEcmaScript(
													(String) rollupValue.getXValue().getValue()));
											break;
										case "setDocumentId":
											setDocumentId(
												computedInElement, StringEscapeUtils.escapeEcmaScript(
													(String) rollupValue.getXValue().getValue()));
											break;

										case "setDocumentCode":
											setDocumentCode(
												computedInElement, StringEscapeUtils.escapeEcmaScript(
													(String) rollupValue.getXValue().getValue()));
											break;

									}

								} else {

									getSemanticInterpreter().execute(
										SemanticInterpreter.getFunctionName(rollupValue.getSemanticElement(), se),
										computedInElement, rollupValue.getXValue().getValue());

								}
							}
						} else {
							String rollupRule = rulesBySemanticElement.get(rollupValue.getSemanticElement());
							if (!StringUtils.isEmpty(rollupRule)) {
								/**
								 * The rule currently is a format of fromfield,tofield or field
								 */
								String[] fromto = rollupRule.split(",");
								XValue foo = (XValue) rollupValue.getXValue();
								computedInElement.getXValue().addValue(
									(fromto.length > 1
											? fromto[1]
											: fromto[0]),
									foo.getValueByName(fromto[0]));
							} else {
								logger.trace("Container Rollup");
								rollupValue.setParent(computedInElement);
								logger.trace("Container Rollup" + rollupValue.getSemanticElement().getName());

							}
						}
					}
				}
			}

		} else {

			if (rule.startsWith("UPDATEVALUE:")) {
				if (elementValueSet.hasElementValuesByName(se)) {
					for (IElementValue value : elementValueSet.getElementValuesByName(se)) {
						getSemanticInterpreter().update(se.getName() + "_UPDATEVALUE", value);
					}
				}
			} else {
				if (se.getParent() != null) {

					if (elementValueSet.hasElementValuesByName(se.getParent())) {

						List<IElementValue> elements = elementValueSet.getElementValuesByType(se.getParent());
						for (IElementValue element : elements) {
							XElementValue computedInElement = new XElementValue(se, elementValueSet);
							computedInElement.setParent(element);
							element.addChild(computedInElement);
							getSemanticInterpreter().update(se.getName() + "_COMPUTED", computedInElement);
						}
					}
				}

			}
		}

	}

	/**
	 * @param computedInElement
	 * @param escapeEcmaScript
	 */
	private void setDocumentId(XElementValue computedInElement, String escapeEcmaScript) {
		if (computedInElement.getXValue().getValue() == null) {
			computedInElement.setValue("homeID_##_repositoryUniqueID_##_documentID_##_documentCode");
		}
		String str = (String) computedInElement.getXValue().getValue();
		str = str.replace("documentID", escapeEcmaScript);
		computedInElement.getXValue().setValue(str);

	}

	/**
	 * @param computedInElement
	 * @param escapeEcmaScript
	 */
	private void setDocumentCode(XElementValue computedInElement, String escapeEcmaScript) {
		if (computedInElement.getXValue().getValue() == null) {
			computedInElement.setValue("homeID_##_repositoryUniqueID_##_documentID_##_documentCode");
		}
		String str = (String) computedInElement.getXValue().getValue();
		str = str.replace("documentCode", escapeEcmaScript);
		computedInElement.getXValue().setValue(str);

	}

	/**
	 * @param computedInElement
	 * @param escapeEcmaScript
	 */
	private void setRepositoryId(XElementValue computedInElement, String escapeEcmaScript) {
		if (computedInElement.getXValue().getValue() == null) {
			computedInElement.setValue("homeID_##_repositoryUniqueID_##_documentID_##_documentCode");
		}
		String str = (String) computedInElement.getXValue().getValue();
		str = str.replace("repositoryUniqueID", escapeEcmaScript);
		computedInElement.getXValue().setValue(str);

	}

	/**
	 * @param computedInElement
	 * @param escapeEcmaScript
	 */
	private void setDocumentHomeId(XElementValue computedInElement, String escapeEcmaScript) {
		if (computedInElement.getXValue().getValue() == null) {
			computedInElement.setValue("homeID_##_repositoryUniqueID_##_documentID_##_documentCode");
		}
		String str = (String) computedInElement.getXValue().getValue();
		str = str.replace("homeID", escapeEcmaScript);
		computedInElement.getXValue().setValue(str);
	}

	private static String parseFunctionName(String rule) {

		String[] content = rule.split(":");

		if (content.length == 2) {
			return content[1];
		} else {
			return "invalidfunctionmapping";
		}
	}

	private void setComputedOutValue(SemanticElement se, Properties properties, IElementValue parent) {
		se.getComputedOutValue().getExpression();
		se.getComputedOutValue().getLanguage();
		XElementValue xe = new XElementValue(se, valueSet);
		if (parent != null) {
			xe.setParent(parent);
			parent.addChild(xe);
		}

		getSemanticInterpreter().update(se.getName() + "_COMPUTEDOUT", xe);

	}

	/**
	 * setComputedInValue Given a semantic element - determine if the computedin should be fired
	 * The first check is to see if the parent not null - Search for instance of the parent and add child element and run computed in
	 * The second is if the parent is null and the type is not normal
	 * The last if if is the semantic element is owned by the root semantic element and the type is local
	 *
	 * @TODO Document and perhaps simplify the handling of computed in
	 * @param se
	 */

	SemanticElement getNormalContainer(SemanticElement se) {

		if (se.getSemanticElementType().equals(SemanticElementType.NORMAL)) {
			return se;
		} else {
			if (se.getParent() != null) {
				if (se.getParent() != null &&
						se.getParent().getSemanticElementType().equals(SemanticElementType.NORMAL)) {
					return se.getParent();
				} else {
					return getNormalContainer(se.getParent());
				}
			}
		}
		return null;

	}

	boolean hasOffSpring(SemanticElement se) {
		boolean result = false;
		for (SemanticElement child : se.getChildren()) {
			List<IElementValue> siblingValues = valueSet.getElementValuesByType(child);
			if (!siblingValues.isEmpty()) {
				result = true;
				return result;
			}
		}

		if (!result) {
			for (SemanticElement child : se.getChildren()) {
				result = hasOffSpring(child);
				if (result) {
					return result;
				}
			}
		}

		return false;

	}

	boolean exists(SemanticElement se) {
		return !valueSet.getElementValuesByType(se).isEmpty();
	}

	private void setComputedInValue(SemanticElement se, Properties properties) {
		se.getComputedInValue().getExpression();
		se.getComputedInValue().getLanguage();
		SemanticElement normalContainer = getNormalContainer(se);

		// If my normal container exists
		if (normalContainer != null && exists(normalContainer)) {
			boolean fireComputedIn = false;
			// Check for existing siblings
			for (SemanticElement sibling : se.getParent().getChildren()) {
				List<IElementValue> siblingValues = valueSet.getElementValuesByType(sibling);
				if (!siblingValues.isEmpty()) {
					fireComputedIn = true;
					break;
				}
			}

			// if no existing siblings, check for offspring
			if (!fireComputedIn) {
				fireComputedIn = hasOffSpring(se.getParent());
			}

			if (fireComputedIn) {
				new XElementValue(se, valueSet);
			}

			// ArrayList<IElementValue> a = valueSet.getElementValuesByType(se);
			// if (a.size() <= 0) {
			// new XElementValue(se, valueSet);
			// }
		}

		if (normalContainer.getParent() == null) {
			new XElementValue(se, valueSet);
		}

		List<IElementValue> axs = valueSet.getElementValuesByType(se);
		for (int i = 0; i < axs.size(); i++) {

			this.getSemanticInterpreter().execute(se.getName() + "_COMPUTEDIN", axs.get(i), properties);
		}
	}

	private List<IElementValue> getElementValuesWithoutChild(SemanticElement semanticElement) {
		List<IElementValue> parentElementValues = valueSet.getElementValuesByType(semanticElement.getParent());
		List<IElementValue> result = new ArrayList<>(parentElementValues);

		for (IElementValue parentElementValue : parentElementValues) {
			for (IElementValue childElementValue : valueSet.getElementValuesByType(semanticElement)) {
				if (parentElementValue.getChildren().contains(childElementValue)) {
					result.remove(parentElementValue);
					break;
				}
			}
		}
		return result;
	}

}
