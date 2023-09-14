/*******************************************************************************
 * Copyright (c) 2016 MDIX Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MDIX Software, Inc.. - Refactored from Default Semantic Parser
 *
 * Author:
 *    Sean Muir
 *
 * SimplifiedSemanticParser - as it names suggests - has simplified and streamlined some of the
 * MDMI functions to support the CDA mapping requirements
 *
 *  In particular the use of Semantic Containers and limiting the use of Local Semantic Containers to being
 *  a cardinality of 1
 *
 *******************************************************************************/
package org.mdmi.core.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.mdmi.Bag;
import org.mdmi.Choice;
import org.mdmi.DTCChoice;
import org.mdmi.DTCStructured;
import org.mdmi.DTExternal;
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
import org.mdmi.SemanticElementType;
import org.mdmi.core.ElementValueSet;
import org.mdmi.core.IElementValue;
import org.mdmi.core.ISyntaxNode;
import org.mdmi.core.MdmiException;
import org.mdmi.core.MdmiUtil;
import org.mdmi.impl.MDMIPackageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplifiedSemanticParser extends DefaultSemanticParser {

	/**
	 * @param messageGroup
	 */
	public SimplifiedSemanticParser(MessageGroup messageGroup) {
		super(messageGroup);
	}

	private static Logger logger = LoggerFactory.getLogger(SimplifiedSemanticParser.class);

	/**
	 * @param sourceMessageGroup
	 */

	@Override
	public ISyntaxNode createNewSyntacticModel(MessageModel mdl, ElementValueSet eset, Properties properties) {
		Node root = mdl.getSyntaxModel().getRoot();
		if (root.getMinOccurs() != 1 || root.getMaxOccurs() != 1) {
			logger.error("Invalid mapping for root node  " + MdmiUtil.getNodePath(root));
		}
		YNode yroot = createYNode(root);
		updateSyntacticModel(mdl, eset, yroot, properties);
		return yroot;
	}

	/**
	 * normalizeSemanticContainers - makes sure the appropriate semantic container chain is established
	 * during creation some semantic elements will be owned not by the direct sementic parent container but the grand parent element
	 * because the parent container is not part of the source message mapping
	 *
	 * So walk the children and if the semantic parent does not match the current parent add a new instance and connect the dots
	 *
	 *
	 * @param elementValueSet
	 * @param parent
	 * @param iterator
	 */
	void normalizeSemanticContainers(ElementValueSet elementValueSet, IElementValue parent,
			ListIterator<IElementValue> iterator, HashMap<String, IElementValue> containers) {
		// = new HashMap<>();
		ArrayList<IElementValue> remove = new ArrayList<>();
		for (IElementValue child : parent.getChildren()) {
			if (!child.getSemanticElement().getParent().getName().equals(parent.getSemanticElement().getName())) {
				if (!containers.containsKey(child.getSemanticElement().getParent().getName())) {
					XElementValue x = new XElementValue(
						child.getSemanticElement().getParent(), elementValueSet, iterator);
					containers.put(child.getSemanticElement().getParent().getName(), x);
					parent.addChild(x);
					x.setParent(parent);
				}

				IElementValue targetElementValue = containers.get(child.getSemanticElement().getParent().getName());

				targetElementValue.addChild(child);
				child.setParent(targetElementValue);
				remove.add(child);
			}

		}

		for (IElementValue r : remove) {
			parent.removeChild(r);
		}

	}

	void walkComputedIn(MessageModel mdl, ElementValueSet elementValueSet, Properties properties,
			HashMap<String, IElementValue> containers) {

		StopWatch watch = new StopWatch();
		watch.start();

		ArrayList<SemanticElement> computedElementsContainers = new ArrayList<>();
		ArrayList<SemanticElement> rootComputedElements = new ArrayList<>();

		for (SemanticElement semanticElement : mdl.getElementSet().getSemanticElements()) {
			if (semanticElement.isComputedIn()) {

				if (semanticElement.getParent() != null && semanticElement.getParent().getParent() != null) {
					computedElementsContainers.add(semanticElement);
				}
				if (semanticElement.getParent() != null && semanticElement.getParent().getParent() == null) {
					rootComputedElements.add(semanticElement);
				}
			}
		}

		ListIterator<IElementValue> iterator = elementValueSet.getAllElementValues().listIterator();

		while (iterator.hasNext()) {
			IElementValue elementValue = iterator.next();
			if (elementValue.getSemanticElement() != null) {
				;
				normalizeSemanticContainers(elementValueSet, elementValue, iterator, containers);
			}
		}

		for (SemanticElement computedElementOwner : computedElementsContainers) {

			if (computedElementOwner.getParent() != null &&
					elementValueSet.hasElementValuesByName(computedElementOwner.getParent())) {

				List<IElementValue> foundElements = elementValueSet.getElementValuesByName(
					computedElementOwner.getParent());

				for (IElementValue elementValue : foundElements) {

					if (elementValue.getSemanticElement() != null) {

						for (SemanticElement child : elementValue.getSemanticElement().getChildren()) {

							if (child.isComputedIn()) {

								boolean ran = false;
								for (IElementValue ce : elementValue.getChildren()) {
									if (ce.getSemanticElement().getName().equals(child.getName())) {
										ran = true;
									}
								}

								if (ran) {
									continue;
								}

								logger.trace("Process computedin " + child.getName());
								String rule = child.getComputedInValue().getExpression();
								String lang = child.getComputedInValue().getLanguage();
								XElementValue computedInElement = new XElementValue(child, elementValueSet);
								if ("Value".equals(lang)) {
									logger.trace("Process computedin value " + rule);
									computedInElement.getXValue().setValue(rule);
								} else {
									logger.trace("Process computedin rule " + rule);

									this.getSemanticInterpreter().execute(
										child.getName() + "_COMPUTEDIN", computedInElement, properties);

									// evalRule(lang, rule, computedInElement, properties);
								}
								elementValue.addChild(computedInElement);
							}

						}
					}

				}
			}

		}

		for (SemanticElement child : rootComputedElements) {
			logger.trace("Walk for computedin " + child.getName());
			if (child.isComputedIn()) {
				String rule = child.getComputedInValue().getExpression();
				String lang = child.getComputedInValue().getLanguage();
				XElementValue computedInElement = new XElementValue(child, elementValueSet);

				if ("Value".equals(lang)) {
					computedInElement.getXValue().setValue(rule);
				} else {
					logger.trace("Process computedin " + child.getName());
					logger.trace("Process computedin rule " + rule);

					logger.trace("Process computedin rule " + rule);

					this.getSemanticInterpreter().execute(
						child.getName() + "_COMPUTEDIN", computedInElement, properties);

					// evalRule(lang, rule, computedInElement, properties);
				}

				for (IElementValue parentValue : elementValueSet.getElementValuesByName(child.getParent())) {
					parentValue.addChild(computedInElement);
					break;
				}
			}

		}

		watch.split();
		logger.trace("call computed in: " + watch.toSplitString());

		watch.split();
		logger.trace("walk computedin for children computed in: " + watch.toSplitString());

		/**
		 * @TODO - This is required to fire any compute ins when the path between the value has a local container
		 *       This only supports on local container need to support multiple local contianers
		 */

		watch.split();
		logger.trace("locals : " + watch.toSplitString());

		watch.stop();

		logger.trace("walk computed in time : " + watch.getTime());

	}

	void walkNullFlavor(ElementValueSet elementValueSet, Properties properties) {

		ArrayList<Pair<IElementValue, SemanticElement>> toRun = new ArrayList<>();

		for (IElementValue elementValue : elementValueSet.getAllElementValues()) {

			if (elementValue.getSemanticElement() != null) {
				for (SemanticElement child : elementValue.getSemanticElement().getChildren()) {

					if (child.isNullFlavor()) {

						boolean runForElement = true;

						for (SemanticElementRelationship ser : child.getRelationships()) {
							for (IElementValue childElement : elementValue.getChildren()) {
								if (ser.getRelatedSemanticElement() != null) {
									if (childElement.getSemanticElement().getName().equals(
										ser.getRelatedSemanticElement().getName())) {
										runForElement = false;
									}
									if (ser.getRelatedSemanticElement().isComputedIn()) {
										runForElement = false;
									}
								} else {
									logger.error(
										"Error! processing Null Flavor " + child.getName() +
												", NULLFLAVOR relationship missing semantic element");
									runForElement = false;
								}
							}

						}

						if (runForElement) {
							toRun.add(Pair.of(elementValue, child));
						}
					}
				}
			}
		}

		for (Pair<IElementValue, SemanticElement> nullFlavorToRun : toRun) {
			try {
				SemanticElementRelationship ser = nullFlavorToRun.getRight().getRelationshipByName("NULLFLAVOR");
				logger.trace("Running Null Flavor " + ser.getRelatedSemanticElement().getName());
				XElementValue nullFlavor = new XElementValue(ser.getRelatedSemanticElement(), elementValueSet);
				this.getSemanticInterpreter().execute(
					"setNullFlavor" + ser.getContext().getName(), nullFlavor, properties);
				nullFlavorToRun.getLeft().addChild(nullFlavor);
				nullFlavor.setParent(nullFlavorToRun.getLeft());
				System.err.println(nullFlavor.value());
			} catch (Exception e) {
			}
		}
	}

	SemanticElement getRoot(SemanticElement child) {
		if (child.getParent() == null) {
			return child;
		} else {
			return getRoot(child.getParent());
		}
	}

	@Override
	public void updateSyntacticModel(MessageModel mdl, ElementValueSet elementValueSet, ISyntaxNode yr,
			Properties properties) {

		StopWatch watch = new StopWatch();
		watch.start();
		logger.trace("Start updateSyntacticModel ");

		if (mdl == null || elementValueSet == null || yr == null) {
			throw new IllegalArgumentException("Null argument!");
		}
		YNode yroot = (YNode) yr;

		// Locate the root semantic element
		SemanticElement rootSemantic = null;
		for (IElementValue elementValue : elementValueSet.getAllElementValues()) {
			if (elementValue.getSemanticElement() != null) {
				rootSemantic = getRoot(elementValue.getSemanticElement());
			}
			if (rootSemantic != null) {
				break;
			}
		}

		if (rootSemantic == null) {
			logger.error("NO RootElement Found ");
			return;
		}

		HashMap<String, XElementValue> fillInTheBlanks = new HashMap<>();

		logger.trace("Root Semantic Element " + rootSemantic.getName());

		watch.split();
		logger.trace("Set up: " + watch.toSplitString());

		for (IElementValue rootElementValue : elementValueSet.getElementValuesByName(rootSemantic)) {
			fillInTheBlanks.put(rootSemantic.getName(), (XElementValue) rootElementValue);
		}

		watch.split();
		logger.trace("fillInTheBlanks: " + watch.toSplitString());

		// update all semantic containment

		ListIterator<IElementValue> iterator = elementValueSet.getAllElementValues().listIterator();

		while (iterator.hasNext()) {
			IElementValue value = iterator.next();
			if (value.getParent() == null) {
				logger.trace("ORPHAN ELEMENT " + value.getSemanticElement().getName());
				Stack<SemanticElement> path = new Stack<>();
				SemanticElement currentParent = value.getSemanticElement().getParent();
				while (currentParent != null) {
					path.push(currentParent);
					currentParent = currentParent.getParent();
				}
				IElementValue currentValueParent = null;
				while (!path.isEmpty()) {
					SemanticElement current = path.pop();
					logger.trace("PATH " + current.getName());
					if (!fillInTheBlanks.containsKey(current.getName())) {
						XElementValue localElement = new XElementValue(current, elementValueSet, iterator);
						if (currentValueParent != null) {
							currentValueParent.addChild(localElement);
						}
						fillInTheBlanks.put(current.getName(), localElement);
					}

					currentValueParent = fillInTheBlanks.get(current.getName());
				}
				if (currentValueParent != null) {
					currentValueParent.addChild(value);
				}
			}
		}

		watch.split();
		logger.trace("update all semantic containment: " + watch.toSplitString());

		HashMap<String, IElementValue> containers = new HashMap<String, IElementValue>();

		for (IElementValue rootElementValue : elementValueSet.getElementValuesByName(rootSemantic)) {
			containers.put(rootElementValue.getSemanticElement().getName(), rootElementValue);
		}

		walkComputedIn(mdl, elementValueSet, properties, containers);
		watch.split();
		logger.trace("walkComputedIn: " + watch.toSplitString());

		walkNullFlavor(elementValueSet, properties);

		watch.split();
		logger.trace("walkNullFlavor: " + watch.toSplitString());

		for (

		IElementValue elementValue : elementValueSet.getAllElementValues()) {
			if (elementValue.getSemanticElement() != null && elementValue.getSemanticElement().getParent() == null) {
				if (elementValue.value() instanceof XDataStruct) {
					setYNodeValuesForBag((YBag) yroot, (XDataStruct) elementValue.value());
				}
				walkModel(yroot, elementValue);
			}
		}

		watch.split();
		logger.trace("walkModel: " + watch.toSplitString());

		logger.debug("compled update syntax model");

		watch.stop();

		logger.trace("updateSyntacticModel time : " + watch.getTime());

	}

	void getPath(Stack<Node> bags, Node container, Node child) {

		if (child == null || container == null) {
			return;
		}
		bags.push(child);
		if (child.getParentNode() != null && container != null && !container.equals(child.getParentNode())) {
			getPath(bags, container, child.getParentNode());
		}
	}

	/**
	 * walkModel is a recursive method used to populate the YBag
	 *
	 * @param currentYBag
	 * @param elementValue
	 */

	void walkModel(YNode currentYBag, IElementValue elementValue) {

		logger.trace("walkModel : " + elementValue.getName());

		Node node = elementValue.getSemanticElement().getSyntaxNode();

		HashMap<Node, YNode> createdNodes = new HashMap<>();

		for (IElementValue childElementValue : elementValue.getChildren()) {
			Stack<Node> bags = new Stack<>();

			getPath(bags, currentYBag.getNode(), childElementValue.getSemanticElement().getSyntaxNode());

			logger.trace("walkModel child : " + childElementValue.getName());

			YNode ynode = currentYBag;
			while (!bags.isEmpty()) {
				Node newNode = bags.pop();

				YNode child = null;
				if (newNode instanceof Choice) {
					if (!createdNodes.containsKey(newNode)) {
						logger.trace("Create YChoice: " + newNode.getName());
						createdNodes.put(newNode, new YChoice((Choice) newNode, ynode));
						child = createdNodes.get(newNode);
						ynode.addYNode(child);
					}
					child = createdNodes.get(newNode);
				}
				if (newNode instanceof Bag) {

					if (newNode.getMaxOccurs() != 1 || !createdNodes.containsKey(newNode)) {

						if ("representedCustodianOrganization".equals(newNode.getName())) {
							System.err.println(newNode.getName());
						}
						logger.trace("Create YBag: " + newNode.getName());
						createdNodes.put(newNode, new YBag((Bag) newNode, ynode));

						child = createdNodes.get(newNode);
						if (childElementValue.value() != null) {
							if (childElementValue.value() instanceof XDataStruct) {
								setYNodeValuesForBag((YBag) child, (XDataStruct) childElementValue.value());
							} else {

								logger.error(
									" Syntax Model Populating Error/Mismatch - Possible missing proper semantic containment/container " +
											elementValue.getSemanticElement().getName() + " " +
											getFullPathForNode(child.getNode()) + " " + childElementValue.value());

							}
						}
						ynode.addYNode(child);
					}
					child = createdNodes.get(newNode);

				}
				if (newNode instanceof LeafSyntaxTranslator) {
					if (!createdNodes.containsKey(newNode)) {

						logger.trace("Create YLeaf: " + newNode.getName());

						createdNodes.put(newNode, new YLeaf((LeafSyntaxTranslator) newNode, ynode));
						child = createdNodes.get(newNode);
						setLeafValue((YLeaf) child, childElementValue.value());
						ynode.addYNode(child);
					}
					child = createdNodes.get(newNode);

				}

				ynode = child;
			}

			if (ynode != null) {

				logger.trace("ynode setvalue " + ynode.getNode().getName() + " : " + childElementValue.getXValue());
				ynode.setElementValue(childElementValue);
				walkModel(ynode, childElementValue);
			}
		}

	}

	String getFullPathForNode(Node n) {
		if (n.getParentNode() == null) {
			return n.getLocation();
		} else {
			return getFullPathForNode(n.getParentNode()) + "/" + n.getLocation();
		}
	}

	private void getMappedElement(YNode yroot, XElementValue owner, ElementValueSet eset) {
		if (yroot.isLeaf()) {
			getSimpleElement((YLeaf) yroot, owner, eset);
		} else if (yroot.isChoice()) {
			getChoiceElement((YChoice) yroot, owner, eset);
		} else if (yroot.isBag()) { // Bag
			getStructElement((YBag) yroot, owner, eset);
		} else {
			throw new IllegalArgumentException("Invalid YNode!");
		}
	}

	// a leaf mapped to a simple type (must be simple)
	private void getSimpleElement(YLeaf yleaf, XElementValue owner, ElementValueSet eset) {
		SemanticElement me = yleaf.getLeaf().getSemanticElement();
		if (me == null) {
			throw new MdmiException("Null SE for node {0}", MdmiUtil.getNodePath(yleaf.getNode()));
		}
		XElementValue xe = new XElementValue(me, eset);
		if (owner != null) {
			// set parent-child relationship
			owner.addChild(xe);
		}

		// set the value
		MDMIDatatype dt = me.getDatatype();
		if (!(dt.isSimple() || dt.isExternal())) {
			throw new MdmiException("Invalid mapping for node bbbb " + MdmiUtil.getNodePath(yleaf.getNode()));
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
				DTExternal dte = (DTExternal) dt;
				String uri = dte.getTypeSpec();
				if (uri != null) {
					// Object o = Mdmi.INSTANCE().getExternalResolvers().getDictionaryValue(dte, value);
					// xe.getXValue().addValue(o);
				} else {
					DTSPrimitive pdt = MDMIPackageImpl.STRING;
					if (xdt == null) {
						xdt = XDT.fromPDT(pdt);
					}
					Object o = XDT.convertFromString(xdt, value, format, pdt);
					xe.getXValue().addValue(o);
				}
			} else {
				DTSEnumerated edt = (DTSEnumerated) dt;
				EnumerationLiteral el = edt.getLiteralByCode(value);
				xe.getXValue().addValue(el);
			}
		} catch (Throwable throwable) {
			throw new MdmiException("Error proccessing node " + MdmiUtil.getNodePath(yleaf.getNode()), throwable);
		}
	}

	private void getChoiceElement(YChoice ychoice, XElementValue owner, ElementValueSet eset) {
		SemanticElement me = ychoice.getChoice().getSemanticElement();
		if (me == null) {
			throw new IllegalArgumentException(
				"Missing Semantic Elements for " + MdmiUtil.getNodePath(ychoice.getNode()));
		}
		XElementValue xe = new XElementValue(me, eset);
		if (owner != null) {
			// set parent-child relationship
			owner.addChild(xe);
		}

		// set the value
		MDMIDatatype dt = me.getDatatype();
		if (!dt.isChoice()) {
			throw new MdmiException(
				"Invalid mapping for node cccc {0}, expected datatype choice, found {1}.",
				MdmiUtil.getNodePath(ychoice.getNode()), dt.toString());
		}

		try {
			XDataChoice xc = new XDataChoice(xe.getXValue());
			xe.getXValue().addValue(xc);

			ArrayList<YNode> yns = ychoice.getYNodes();
			for (YNode yn : yns) {
				Node n = yn.getNode();
				if (n.getSemanticElement() != null) {
					// the choice has only child elements
					for (YNode yyn : yns) {
						getMappedElement(yyn, xe, eset);
					}
					break; // we found the choice
				} else {
					// the choice has only fields
					String fieldName = n.getFieldName();
					if (null != fieldName) {
						// if fieldName is null it means it is
						XValue xv = xc.setXValue(fieldName);
						for (YNode yyn : yns) {
							getValue(yyn, xv, xe, eset);
						}
						break; // we found the choice
					}
					// try the next node if this was no match
				}
			}
		} catch (Throwable throwable) {
			throw new MdmiException(throwable, "Error proccessing node " + MdmiUtil.getNodePath(ychoice.getNode()));
		}
	}

	private void getStructElement(YBag ybag, XElementValue owner, ElementValueSet eset) {
		SemanticElement me = ybag.getBag().getSemanticElement();
		if (me == null) {
			throw new IllegalArgumentException("Missing Semantic Elements for " + MdmiUtil.getNodePath(ybag.getNode()));
		}
		XElementValue xe = new XElementValue(me, eset);
		if (owner != null) {
			// set parent-child relationship
			owner.addChild(xe);
		}

		// set the value
		MDMIDatatype dt = me.getDatatype();
		if (dt == null || !dt.isStruct()) {
			throw new MdmiException("Invalid mapping for node dddd " + MdmiUtil.getNodePath(ybag.getNode()));
		}

		try {
			XDataStruct xs = new XDataStruct(xe.getXValue());
			xe.getXValue().addValue(xs);

			LinkedList<YNode> yns = ybag.getYNodes();
			for (YNode yn : yns) {
				Node n = yn.getNode();
				if (n.getSemanticElement() != null) {
					// child element
					getMappedElement(yn, xe, eset);
				} else {
					// field
					String fieldName = n.getFieldName();
					if (fieldName == null) {
						continue; // will ignore it for now, assuming unmapped syntax node
					}
					// throw new MdmiException("Field Name is Null " + MdmiUtil.getNodePath(n));
					XValue xv = xs.getXValue(fieldName);
					if (xv == null) {
						throw new MdmiException(
							"Invalid mapping for node eee " + fieldName + " --- for type " +
									xs.getDatatype().getName() + MdmiUtil.getNodePath(ybag.getNode()));
					}
					getValue(yn, xv, xe, eset);
				}
			}
		} catch (Throwable throwable) {
			throw new MdmiException(throwable, "Error proccessing node " + MdmiUtil.getNodePath(ybag.getNode()));
		}
	}

	private void getValue(YNode yroot, XValue xv, XElementValue owner, ElementValueSet eset) {
		if (yroot.isLeaf()) {
			getValueLeaf((YLeaf) yroot, xv);
		} else if (yroot.isChoice()) {
			getValueChoice((YChoice) yroot, xv, owner, eset);
		} else if (yroot.isBag()) { // Bag
			getValueStruct((YBag) yroot, xv, owner, eset);
		} else {
			throw new IllegalArgumentException("Invalid YNode!");
		}
	}

	private void getValueLeaf(YLeaf yleaf, XValue xv) {
		MDMIDatatype dt = xv.getDatatype();
		if (!(dt.isSimple() || dt.isExternal())) {
			throw new MdmiException("Invalid mapping for node ffff " + MdmiUtil.getNodePath(yleaf.getNode()));
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
				// if (uri != null) {
				// Object o = Mdmi.INSTANCE().getExternalResolvers().getDictionaryValue(dte, value);
				// xv.addValue(o);
				// } else {
				// DTSPrimitive pdt = MDMIPackageImpl.STRING;
				// if (xdt == null) {
				// xdt = XDT.fromPDT(pdt);
				// }
				// Object o = XDT.convertFromString(xdt, value, format, pdt);
				// xv.addValue(o);
				// }
			} else {
				DTSEnumerated edt = (DTSEnumerated) dt;
				EnumerationLiteral el = edt.getLiteralByCode(value);
				xv.addValue(el);
			}
		} catch (Throwable throwable) {
			throw new MdmiException("Error proccessing node " + MdmiUtil.getNodePath(yleaf.getNode()), throwable);
		}
	}

	private void getValueChoice(YChoice ychoice, XValue xv, XElementValue owner, ElementValueSet eset) {
		MDMIDatatype dt = xv.getDatatype();
		if (!dt.isChoice()) {
			throw new MdmiException("Invalid mapping for node hhh " + MdmiUtil.getNodePath(ychoice.getNode()));
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
					getMappedElement(yn, owner, eset);
				}
			} else {
				// the choice has only fields
				String fieldName = n.getFieldName();
				XValue xvalue = xc.setXValue(fieldName);
				for (YNode yn : yns) {
					getValue(yn, xvalue, owner, eset);
				}
			}
		}
	}

	private void getValueStruct(YBag ybag, XValue xv, XElementValue owner, ElementValueSet eset) {
		MDMIDatatype dt = xv.getDatatype();
		if (!dt.isStruct()) {
			throw new MdmiException("Invalid mapping for node iiii " + MdmiUtil.getNodePath(ybag.getNode()));
		}
		XDataStruct xs = new XDataStruct(xv);
		xv.addValue(xs);

		LinkedList<YNode> yns = ybag.getYNodes();
		for (YNode yn : yns) {
			Node n = yn.getNode();
			if (n.getSemanticElement() != null) {
				// child element
				getMappedElement(yn, owner, eset);
			} else {
				// field
				String fieldName = n.getFieldName();
				XValue xvalue = xs.getXValue(fieldName);
				if (xvalue == null) {
					throw new MdmiException("Invalid mapping for node jjj " + MdmiUtil.getNodePath(ybag.getNode()));
				}
				getValue(yn, xvalue, owner, eset);
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

	// set the node value(s) for all fields
	private void setYNodeValues(YNode ynode, Object value) {
		if (value == null) {
			return;
		}
		Node node = ynode.getNode();
		if (node instanceof Bag) {
			if (!(value instanceof XDataStruct)) {
				/**
				 * @TODO
				 *       The XValue,etc do not support an array of structures - so pop the value from the value
				 */
				if (value instanceof XValue) {
					XValue xValue = (XValue) value;
					setYNodeValuesForBag((YBag) ynode, xValue.getXDataStruct());
				} else {
					logger.error(
						"Invalid map: expected XDataStruct for node " + ynode.getNode().getName() + " : " +
								MdmiUtil.getNodePath(ynode.getNode()) + value.toString());
				}
			} else {
				setYNodeValuesForBag((YBag) ynode, (XDataStruct) value);
			}

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

			// if (n instanceof Bag) {
			//
			// }
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
			// throw new MdmiException("Invalid mapping for node " + MdmiUtil.getNodePath(yleaf.getNode()));
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
				if (value instanceof EnumerationLiteral) {
					v = ((EnumerationLiteral) value).getCode();
				} else if (value instanceof String) {
					v = (String) value;
				} else {
					// throw new MdmiException("Invalid enum conversion for type {0} value {1}", edt.getTypeName(), value);
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

	@Override
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

	// private void evalRule(String lang, String rule, XElementValue xe, Properties properties) {
	// // IExpressionInterpreter adapter = Mdmi.getInterpreter(lang, xe, "", null);
	// // logger.trace(rule);
	// // logger.trace(xe.getName());
	// //
	// // logger.trace(xe.getXValue().toString());
	// // // xe.getXValue().addValueHack(field);
	// // adapter.evalAction(xe, rule, properties);
	// }
} // DefaultMdmiSemanticParser
