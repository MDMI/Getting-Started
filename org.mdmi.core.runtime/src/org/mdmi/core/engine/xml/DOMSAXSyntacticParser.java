/*******************************************************************************
 * Copyright (c) 2016 MDMIX Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MDMIX Software, Inc - initial API and implementation
 *
 * Author:
 *     Sean Muir
 *
 *******************************************************************************/
package org.mdmi.core.engine.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;
import org.mdmi.Bag;
import org.mdmi.Choice;
import org.mdmi.LeafSyntaxTranslator;
import org.mdmi.MessageModel;
import org.mdmi.MessageSyntaxModel;
import org.mdmi.Node;
import org.mdmi.core.ISyntacticParser;
import org.mdmi.core.ISyntaxNode;
import org.mdmi.core.MdmiException;
import org.mdmi.core.MdmiMessage;
import org.mdmi.core.engine.YBag;
import org.mdmi.core.engine.YChoice;
import org.mdmi.core.engine.YLeaf;
import org.mdmi.core.engine.YNode;
import org.mdmi.core.engine.xml.XPathParser.AbbreviatedStepContext;
import org.mdmi.core.engine.xml.XPathParser.AxisSpecifierContext;
import org.mdmi.core.engine.xml.XPathParser.NodeTestContext;
import org.mdmi.core.engine.xml.XPathParser.PredicateContext;
import org.mdmi.core.util.XmlParser;
import org.mdmi.core.util.XmlUtil;
import org.mdmi.core.util.XslUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class DOMSAXSyntacticParser implements ISyntacticParser {

	private static Logger logger = LoggerFactory.getLogger(DOMSAXSyntacticParser.class);

	private class XPathExtractor extends XPathBaseListener {

		private static final String DOTDOT = "..";

		boolean inPredicate = false;

		boolean isAttribute = false;

		boolean isContainer = false;

		org.w3c.dom.Node node;

		Document document;

		public XPathExtractor(Element element, boolean isContainer) {
			this.node = element;
			this.isContainer = isContainer;
			document = this.node.getOwnerDocument();
		}

		@Override
		public void exitAbbreviatedStep(AbbreviatedStepContext ctx) {
			// If xpath has .. move node upto parent
			if (DOTDOT.equals(ctx.getText())) {
				node = node.getParentNode();
			}
			super.exitAbbreviatedStep(ctx);
		}

		@Override
		public void exitNodeTest(NodeTestContext ctx) {
			if (!inPredicate) {
				if (isAttribute) {
					// sb.append(ctx.getText());
					Attr attribute = document.createAttribute(ctx.getText());
					((Element) node).setAttributeNode(attribute);
					node = attribute;
				} else {
					Element childElement = null;
					if (isContainer) {
						childElement = document.createElement(ctx.getText());
						node.appendChild(childElement);
					} else {
						if (childElement == null) {
							childElement = document.createElement(ctx.getText());
							node.appendChild(childElement);
						}
					}

					node = childElement;
				}
				isAttribute = false;
			}
			super.exitNodeTest(ctx);
		}

		@Override
		public void enterPredicate(PredicateContext ctx) {
			logger.trace("enterPredicate " + ctx.getText());
			inPredicate = true;
			super.enterPredicate(ctx);
		}

		@Override
		public void exitPredicate(PredicateContext ctx) {
			logger.trace("exitPredicate " + ctx.getText());
			inPredicate = false;
			super.exitPredicate(ctx);
		}

		@Override
		public void exitAxisSpecifier(AxisSpecifierContext ctx) {
			logger.trace("exitAxisSpecifier " + ctx.getText());
			if (!inPredicate && "@".equals(ctx.getText())) {
				isAttribute = true;
			}
			super.exitAxisSpecifier(ctx);
		}

	}

	public static class SL4JBaseErrorListener extends BaseErrorListener {

		public static final SL4JBaseErrorListener INSTANCE = new SL4JBaseErrorListener();

		/*
		 * (non-Javadoc)
		 *
		 * @see org.antlr.v4.runtime.BaseErrorListener#syntaxError(org.antlr.v4.runtime.Recognizer, java.lang.Object, int, int, java.lang.String,
		 * org.antlr.v4.runtime.RecognitionException)
		 */
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			if (logger.isTraceEnabled()) {
				logger.trace("line " + line + ":" + charPositionInLine + " " + msg);
			}
		}

	}

	String messageGroupName;

	/**
	 * @TODO Temporary fix for XML namespace
	 *       Supporting such configurations on model is needed
	 *       or access to the properties files
	 * @param name
	 */
	public DOMSAXSyntacticParser(String messageGroupName) {
		this.messageGroupName = messageGroupName;
	}

	private org.w3c.dom.Node createElement(Element elemement, String xPath, boolean container) {

		logger.trace("ELEMEMENT IS :" + elemement.getTagName());

		logger.trace("XPATH IS :" + xPath);

		if (xPath.startsWith("@")) {
			Attr attribute = elemement.getOwnerDocument().createAttribute(xPath.substring(1));
			elemement.setAttributeNode(attribute);
			return attribute;
		}

		if (!(xPath.contains(".") || xPath.contains("[") || xPath.contains("]"))) {
			if (xPath.contains("/")) {
				String[] elements = xPath.split("/");
				Element current = elemement;
				for (String element : elements) {
					if (element.startsWith("@")) {
						Attr attribute = elemement.getOwnerDocument().createAttribute(element.substring(1));
						current.setAttributeNode(attribute);
					} else {
						Element childElement = elemement.getOwnerDocument().createElement(element);
						current.appendChild(childElement);
						current = childElement;
					}
				}
				return current;
			} else {
				Element childElement = elemement.getOwnerDocument().createElement(xPath);
				elemement.appendChild(childElement);
				return childElement;
			}
		}

		logger.trace(elemement.getNodeName() + " creating " + xPath + " container " + container);
		XPathLexer lexer = new XPathLexer(new ANTLRInputStream(xPath));

		logger.trace("Creating CommonTokenStream");
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		logger.trace("Creating XPathParser" + tokens);
		XPathParser parser = new XPathParser(tokens);

		parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
		parser.addErrorListener(SL4JBaseErrorListener.INSTANCE);

		logger.trace("Creating ParseTreeWalker");
		ParseTreeWalker walker = new ParseTreeWalker();

		logger.trace("Creating XPathExtractor");
		XPathExtractor extractor = new XPathExtractor(elemement, container);
		logger.trace("walk");

		walker.walk(extractor, parser.main());

		logger.trace("created " + extractor.node.getLocalName());
		return extractor.node;
	}

	protected NamespaceContext context;

	/*
	 * Extend DOM/SAX Tree Walker which tracks the current dom node so we can
	 * perform XPATH queries
	 */
	private class MDMITreeWalker extends InternalTreeWalker {

		public MDMITreeWalker(ContentHandler contentHandler) {
			super(contentHandler);
		}

		@Override
		protected void startNode(org.w3c.dom.Node node) throws SAXException {
			domNodes.push(node);
			super.startNode(node);
		}

		@Override
		protected void endNode(org.w3c.dom.Node node) throws SAXException {
			domNodes.pop();
			super.endNode(node);
		}

	}

	// static int expressionCounter = 0;

	HashMap<String, XPathExpression> xPathExpressions = new HashMap<String, XPathExpression>();

	private Stack<org.w3c.dom.Node> domNodes = new Stack<org.w3c.dom.Node>();

	private void saxParse(final YBag yroot, final byte[] data) throws ParserConfigurationException, SAXException {

		DefaultHandler2 mdmiHandler = new DefaultHandler2() {

			private Stack<EndTagProcessor> endTags;

			private Stack<Node> syntaxNodes;

			private Stack<YNode> yBags;

			boolean isParsing = false;

			boolean isActive = false;

			StringBuilder builder = null;

			YLeaf currentYLeaf = null;

			XPath xPath = null;

			@Override
			public void startCDATA() throws SAXException {
				if (isParsing && builder != null) {
					builder.append("<![CDATA[");
				} else {
					isParsing = true;
					builder = new StringBuilder();
					builder.append("<![CDATA[");
				}
			}

			@Override
			public void endCDATA() throws SAXException {
				if (isParsing && builder != null) {
					builder.append("]]>");
				}
			}

			@Override
			public void startDocument() throws SAXException {

				endTags = new Stack<EndTagProcessor>();
				syntaxNodes = new Stack<Node>();
				syntaxNodes.push(yroot.getNode());
				yBags = new Stack<YNode>();
				yBags.push(yroot);
			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				if (isParsing && builder != null) {
					builder.append(new String(ch, start, length));
				} else {
				}
			}

			/**
			 * Construct the XPath expression
			 */
			private String getCurrentXPath() {
				String str = "//";
				boolean first = true;
				for (Node node : syntaxNodes) {
					if (first) {
						str = node.getLocation();
					} else {
						str = str + "/" + node.getLocation();
					}
					first = false;
				}
				return str;
			}

			/**
			 * Construct the XPath expression
			 */
			private String getCurrentRelativePath(Node bag) {
				Stack<String> currentPath = getRelativePath(bag);

				if (currentPath != null && !currentPath.isEmpty()) {

					String str = "";

					boolean first = true;
					for (String node : currentPath) {

						if (first) {
							str = node;
							first = false;
						} else {
							str = str + "/" + node;
						}

					}
					// // System.out.println("getCurrentRelativePath " + str + "/");
					return str = str + "/";
				} else {
					return "";
				}
			}

			HashMap<Node, Stack<String>> relativePaths = new HashMap<Node, Stack<String>>();

			void pushXPath(Node bag, String path) {
				if (!relativePaths.containsKey(bag)) {
					relativePaths.put(bag, new Stack<String>());
				}
				relativePaths.get(bag).push(path);
			}

			void popXPath(Node bag) {
				if (relativePaths.containsKey(bag)) {
					relativePaths.get(bag).pop();
				}
			}

			Stack<String> getRelativePath(Node bag) {
				if (relativePaths.containsKey(bag)) {
					return relativePaths.get(bag);
				} else {
					return null;
				}
			}

			private boolean isMatch(String currentRelativeXPath, String nodeXPathLocation) {

				logger.trace(currentRelativeXPath);
				logger.trace(nodeXPathLocation);

				if (currentRelativeXPath.equals(nodeXPathLocation)) {

					return true;
				}

				if (nodeXPathLocation.equals("@" + currentRelativeXPath)) {

					return true;
				}

				String[] locationSegments = nodeXPathLocation.split("/@");

				if (currentRelativeXPath.equals(locationSegments[0])) {

					return true;
				}

				String[] path = nodeXPathLocation.split("\\[");

				if (currentRelativeXPath.equals(path[0])) {

					return true;
				}
				return false;

			}

			BagEndTagProcessor bagEndTagProcessor = new BagEndTagProcessor();

			LeafEndTagProcessor leafEndTagProcessor = new LeafEndTagProcessor();

			NotFoundEndTagProcessor notFoundEndTagProcessor = new NotFoundEndTagProcessor();

			class NodePredicate implements Predicate<Node> {

				public NodePredicate(Node bag, final String qName) {
					sb.append(getCurrentRelativePath(bag));
					sb.append(qName);
				}

				StringBuilder sb = new StringBuilder();

				@Override
				public boolean apply(Node node) {

					/**
					 * Skip an computedIn nodes as they are local and not matched as part of the semantics
					 */
					if (node.getSemanticElement() != null && node.getSemanticElement().isComputedIn()) {
						return false;
					}

					return isMatch(sb.toString(), node.getLocation());
				}

				void pushNode(Node node) {
					sb.insert(0, node.getLocation() + "/");
				}
			}

			private List<Node> lookForMatch(final String qName) {

				NodePredicate matches = null;
				List<Node> results = new ArrayList<Node>();

				int depthCtr = 0;
				for (Node currentBag : Lists.reverse(syntaxNodes)) {
					depthCtr++;

					logger.trace(currentBag.getName() + "lookForMatch " + qName);

					if (results.size() > 0) {
						logger.info("Look for match using after found " + currentBag.getName());
					}

					if (matches == null) {
						matches = new NodePredicate(currentBag, qName);
					}
					// logger.trace("Try node " + matches);

					Iterable<Node> matchingNodes = null;

					matchingNodes = Iterables.filter(currentBag.getNodesByLocation(qName), matches);

					// Iterable<Node>
					Iterator<Node> iterator = matchingNodes.iterator();

					while (iterator.hasNext()) {

						if (!results.isEmpty()) {
							break;
						}

						Node node = iterator.next();

						String expression = "";
						try {
							String path = node.getLocation();

							logger.trace("path " + path);

							int start = path.indexOf("[");
							int end = node.getLocation().lastIndexOf("]");
							if (start > -1 && end > -1) {

								expression = path.substring(start + 1, end);

								if (xPath == null) {
									xPath = XPathFactory.newInstance().newXPath();

									logger.debug("XPATH VERSION IS " + xPath.getClass().getCanonicalName());
									URL location = xPath.getClass().getResource(
										'/' + xPath.getClass().getName().replace('.', '/') + ".class");
									logger.debug("XPATH JAR IS " + location.getFile());

									NamespaceContext ctx = new NamespaceContext() {
										public String getNamespaceURI(String prefix) {
											if ("xsi".equals(prefix)) {
												return "http://www.w3.org/2001/XMLSchema-instance";
											}
											return null;
										}

										public String getPrefix(String uri) {
											throw new UnsupportedOperationException();
										}

										public Iterator getPrefixes(String uri) {
											throw new UnsupportedOperationException();
										}
									};

									xPath.setNamespaceContext(ctx);
								}
								try {

									if (!xPathExpressions.containsKey(expression)) {
										logger.trace("cache expression " + expression);
										XPathExpression xxxx = xPath.compile(expression);
										xPathExpressions.put(expression, xxxx);
									}
									org.w3c.dom.Node p = null;

									/*
									 * for performance reasons - use clone node wiht children
									 * unless the XPATH is relative to sibling node
									 *
									 */
									if (expression.contains("..")) {
										p = domNodes.peek();
									} else {
										p = domNodes.peek().cloneNode(false);
										if (!StringUtils.isEmpty(domNodes.peek().getTextContent())) {
											p.setTextContent(domNodes.peek().getTextContent());
										}

										for (int childNodeCtr = 0; childNodeCtr < domNodes.peek().getChildNodes().getLength(); childNodeCtr++) {
											org.w3c.dom.Node achildnode = domNodes.peek().getChildNodes().item(
												childNodeCtr);
											if (expression.contains(achildnode.getNodeName())) {
												org.w3c.dom.Node clonedChildNode = p.appendChild(
													achildnode.cloneNode(false));
												if (!StringUtils.isEmpty(achildnode.getTextContent())) {
													clonedChildNode.setTextContent(achildnode.getTextContent());
												}
											}
										}
									}

									/*
									 * Attempt to limit evaluations because they are expensive
									 *
									 * do not skip if not @
									 * if @ set skip to true until you find an attribute value that is in the exxpression
									 */
									boolean skip = false;
									boolean foundSimple = false;
									if (expression.startsWith("@")) {
										skip = true;
										for (int attributeCtr = 0; attributeCtr < p.getAttributes().getLength(); attributeCtr++) {
											String theValue = p.getAttributes().item(attributeCtr).getNodeValue();
											String nodeName = p.getAttributes().item(attributeCtr).getNodeName();
											if (expression.contains(theValue) && expression.contains(nodeName)) {
												skip = false;
												// check for simple
												String[] xxx = expression.split("'");
												if (xxx.length == 2 && theValue.equals(xxx[1])) {
													foundSimple = true;
												}
												break;
											}
										}
									}
									if (skip) {
										// // System.out.println("skipping " + expression);
										continue;
									} else {
										// // System.out.println("not skipping " + expression);
									}

									Boolean isMatch = (foundSimple
											? Boolean.TRUE
											: (Boolean) xPathExpressions.get(expression).evaluate(
												p, XPathConstants.BOOLEAN));

									/**
									 * @TODO Fix bug with xsi type
									 *       XPath boolean expressions using xsi:type are not returning true
									 */
									if (!isMatch) {
										if (expression.startsWith("@xsi:type")) {
											String[] expressionArray = expression.split("=");
											if (expressionArray.length == 2) {
												String value = expressionArray[1].replaceAll("'", "");
												for (int aactr = 0; aactr < p.getAttributes().getLength(); aactr++) {
													if (value.equals(p.getAttributes().item(aactr).getNodeValue())) {
														isMatch = true;
														break;
													}
												}

											}

										}
									}

									if (isMatch) {

										results.add(node);
										logger.trace(
											depthCtr + " loop count  " + path + " Matched using " +
													currentBag.getName());
									}
								} catch (Exception e) {
									NodeList nodes = (NodeList) xPath.evaluate(
										expression, domNodes.peek(), XPathConstants.NODESET);
									if (nodes.getLength() > 0) {
										results.add(node);
									}
								}
							} else {
								results.add(node);
								logger.trace(
									depthCtr + " loop count  " + path + " Matched using " + currentBag.getName());
								// logger.trace("Matched using " + currentBag.getName());
							}
						} catch (XPathExpressionException ex) {
							logger.error("Expression Error " + expression + ex.getMessage());
						}

					}
					logger.trace("pushing bag" + currentBag.getName());
					matches.pushNode(currentBag);

					if (results.size() > 0) {
						break;
					}

					/**
					 * @TODO - Set depthctr at model level
					 */
					if (depthCtr > 0) {
						break;
					}

					// logger.info("results " + results.size());
				}
				if (logger.isWarnEnabled() && !results.isEmpty() && results.size() > 1) {
					for (Node n : results) {
						logger.warn(
							"Warning multiple matched elements " + getFullPathForNode(n) + " to " +
									(n.getSemanticElement() != null
											? n.getSemanticElement().getName()
											: "NO SEMANTIC ELEMENT"));
					}
				}
				return results;
			}

			private String getAttributeValue(Attributes attributes, String attribute) {

				for (int i = 0; i < attributes.getLength(); i++) {
					if (attribute.equals(attributes.getQName(i))) {
						return attributes.getValue(i);
					}
				}

				return null;

			}

			private YNode getYBagForParentNode(Node matchingSyntaxNode) {

				Node currentParent = matchingSyntaxNode.getParentNode();
				boolean foundSyntaxNode = false;
				int index = 0;
				while (!foundSyntaxNode && currentParent != null) {
					index = 0;
					for (Node syntaxNode : syntaxNodes) {
						if (currentParent.equals(syntaxNode)) {
							foundSyntaxNode = true;
							break;
						}
						index++;
					}
					if (!foundSyntaxNode) {
						currentParent = currentParent.getParentNode();
					}
				}

				if (!foundSyntaxNode) {
					index = 0;
				}

				return yBags.get(index);
			}

			/*
			 * Processing Steps Syntax node should always be a bag (or a choice) If
			 * current syntax node is a bag and matching node is a bag and semantic
			 * element is a container - push corresponding node If current syntax
			 * node is a bag and matching node is a LST, create YNode and parse
			 */

			boolean prune = false;

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes)
					throws SAXException {

				if ("table".equals(qName)) {
					prune = true;
				}

				if (prune) {
					return;
				}
				if (yBags.peek().getParent() == null && qName.equals(yBags.peek().getNode().getLocation())) {
					return;
				}

				List<Node> matchingSyntaxNodes = lookForMatch(qName);

				Node matchingSyntaxNode = null;

				if (!matchingSyntaxNodes.isEmpty()) {
					int longestPath = 0;
					for (Node n : matchingSyntaxNodes) {
						if (n.getLocation() != null && n.getLocation().length() >= longestPath) {
							longestPath = n.getLocation().length();
							matchingSyntaxNode = n;
						}

					}
					// matchingSyntaxNode = matchingSyntaxNodes.get(0);
					if (isParsing && builder != null) {
						builder = null;
					}
				} else {

					if (isParsing && builder != null) {
						builder.append("<" + localName);

						for (int i = 0; i < attributes.getLength(); i++) {

							builder.append(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\" ");

						}

						builder.append(">");
					}
				}

				if (matchingSyntaxNode != null) {

					logger.trace(
						"Processing Matched Element " + qName + " to " + matchingSyntaxNode.getName() +
								" At location " + matchingSyntaxNode.getLocation());

					YNode parentYBag = getYBagForParentNode(matchingSyntaxNode);

					if (matchingSyntaxNode instanceof Bag) {
						YBag y = new YBag((Bag) matchingSyntaxNode, parentYBag);
						parentYBag.addYNode(y);
						syntaxNodes.push(matchingSyntaxNode);
						endTags.push(bagEndTagProcessor);
						for (Node n : ((Bag) matchingSyntaxNode).getNodes()) {
							if (n.getLocation().startsWith("@")) {
								String attributeValue = getAttributeValue(attributes, n.getLocation().substring(1));

								if (n instanceof LeafSyntaxTranslator && attributeValue != null) {
									YLeaf aLeaf = new YLeaf((LeafSyntaxTranslator) n, y);
									aLeaf.setValue(attributeValue);
									y.addYNode(aLeaf);
								}

							} else if (n.getLocation().equals(".")) {
								// YLeaf aLeaf = new YLeaf((LeafSyntaxTranslator) n, y);

								bagEndTagProcessor.currentMixedLeaf = new YLeaf((LeafSyntaxTranslator) n, y);
								// currentYLeaf = new YLeaf((LeafSyntaxTranslator) n, y);
								y.addYNode(bagEndTagProcessor.currentMixedLeaf);

								isParsing = true;

								if (builder == null) {
									builder = new StringBuilder();
								}

								// endTags.push(textEndProcessor);

							}

						}
						yBags.push(y);
					}

					if (matchingSyntaxNode instanceof Choice) {

						Choice aChoice = (Choice) matchingSyntaxNode;

						YChoice y = new YChoice((Choice) matchingSyntaxNode, parentYBag);
						parentYBag.addYNode(y);

						syntaxNodes.push(aChoice);
						endTags.push(bagEndTagProcessor);
						yBags.push(y);

						String choiceType = getAttributeValue(attributes, "xsi:type");

						for (Node choiceNode : aChoice.getNodes()) {
							if (choiceNode.getLocation() != null && choiceNode.getLocation().contains(choiceType)) {
								if (choiceNode instanceof Bag) {
									// YBag y2 = new YBag((Bag) choiceNode, y);
									// y.addYNode(y2);

									syntaxNodes.pop();
									syntaxNodes.push(choiceNode);
									YBag y2 = new YBag((Bag) choiceNode, y);
									y.addYNode(y2);

									for (Node attributeNode : ((Bag) choiceNode).getNodes()) {
										if (attributeNode.getLocation().startsWith("@")) {
											String attributeValue = getAttributeValue(
												attributes, attributeNode.getLocation().substring(1));
											if (attributeNode instanceof LeafSyntaxTranslator &&
													attributeValue != null) {
												YLeaf aLeaf = new YLeaf((LeafSyntaxTranslator) attributeNode, y2);
												aLeaf.setValue(attributeValue);
												y2.addYNode(aLeaf);
											}
										}
									}
								}

								if (choiceNode instanceof LeafSyntaxTranslator) {

									// syntaxNodes.push(aChoice);

									bagEndTagProcessor.currentMixedLeaf = new YLeaf(
										(LeafSyntaxTranslator) choiceNode, y);
									// currentYLeaf = new YLeaf((LeafSyntaxTranslator) n, y);
									y.addYNode(bagEndTagProcessor.currentMixedLeaf);

									isParsing = true;

									if (builder == null) {
										builder = new StringBuilder();
									}

								}

							}
						}

					}

					if (matchingSyntaxNode instanceof LeafSyntaxTranslator) {

						if (matchingSyntaxNode.getLocation().startsWith("@")) {

							/*
							 * Loop over all the matches - this supports maps with multiple syntax nodes with
							 */
							for (Node attributeMatches : matchingSyntaxNodes) {
								String[] locationSegments = attributeMatches.getLocation().split("/@");
								if (locationSegments.length > 1) {
									String attributeValue = getAttributeValue(attributes, locationSegments[1]);
									logger.trace(attributeValue);
									if (attributeValue != null) {
										YLeaf aLeaf = new YLeaf((LeafSyntaxTranslator) attributeMatches, parentYBag);
										// // System.out.println(attributeValue);
										aLeaf.setValue(attributeValue);
										parentYBag.addYNode(aLeaf);
									}
								}
							}

							pushXPath(syntaxNodes.peek(), qName);
							endTags.push(notFoundEndTagProcessor);

						} else {
							currentYLeaf = new YLeaf((LeafSyntaxTranslator) matchingSyntaxNode, parentYBag);
							parentYBag.addYNode(currentYLeaf);

							isParsing = true;
							if (builder == null) {
								builder = new StringBuilder();
							}

							endTags.push(leafEndTagProcessor);
						}

					}

				} else {
					pushXPath(syntaxNodes.peek(), qName);
					endTags.push(notFoundEndTagProcessor);
				}

				logger.trace("End Element " + qName);

			}

			/**
			 * EndTagProcessor is function object definition to support end tag functionality
			 *
			 *
			 *
			 */
			abstract class EndTagProcessor {
				abstract boolean process();
			}

			class BagEndTagProcessor extends EndTagProcessor {

				public YLeaf currentMixedLeaf = null;

				@Override
				boolean process() {
					syntaxNodes.pop();
					yBags.pop();
					isActive = false;
					if (currentMixedLeaf != null && builder != null) {
						// // System.out.println(builder.toString());
						currentMixedLeaf.setValue(builder.toString());
						builder = null;
						isParsing = false;
					}

					currentMixedLeaf = null;
					return true;

				}

			}

			class NotFoundEndTagProcessor extends EndTagProcessor {

				@Override
				boolean process() {
					popXPath(syntaxNodes.peek());
					isActive = false;
					return true;

				}

			}

			class LeafEndTagProcessor extends EndTagProcessor {

				@Override
				boolean process() {
					if (isParsing && currentYLeaf != null) {
						if (builder != null && builder.length() > 0) {
							// // System.out.println(builder.toString());
							currentYLeaf.setValue(builder.toString());
						} else {
							// YBag parent = (YBag) currentYLeaf.getParent();
							// parent.getChildren().remove(currentYLeaf);

						}
						builder = null;
						isParsing = false;
						return true;
					}
					isActive = false;
					return false;

				}

			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {

				if (!prune) {
					if (!endTags.isEmpty()) {
						EndTagProcessor etp = endTags.pop();
						etp.process();
					}
					if (isParsing && builder != null) {
						builder.append("</" + localName + ">");
					}
				}

				if ("table".equals(qName)) {
					prune = false;
				}

			}

		};

		DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
		// df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		// df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		DocumentBuilder b = df.newDocumentBuilder();
		org.w3c.dom.Document doc;
		try {
			doc = b.parse(new ByteArrayInputStream(data));
			InternalTreeWalker treeWalker = new MDMITreeWalker(mdmiHandler);
			treeWalker.traverse(doc.getDocumentElement());
		} catch (IOException ex) {

		}

	}

	public String getXPath(org.w3c.dom.Node node) {
		return getXPath(node, "");
	}

	public String getXPath(org.w3c.dom.Node node, String xpath) {
		if (node == null) {
			return "";
		}
		String elementName = "";
		if (node instanceof Element) {
			elementName = ((Element) node).getLocalName();
		}
		org.w3c.dom.Node parent = node.getParentNode();
		if (parent == null) {
			return xpath;
		}
		return getXPath(parent, "/" + elementName + xpath);
	}

	String getFullPathForNode(Node n) {
		if (n.getParentNode() == null) {
			return n.getLocation();
		} else {
			return getFullPathForNode(n.getParentNode()) + "/" + n.getLocation();
		}
	}

	@Override
	public ISyntaxNode parse(MessageModel mdl, MdmiMessage msg) {
		if (mdl == null || msg == null) {
			throw new IllegalArgumentException("Null argument!");
		}
		byte[] data = msg.getData();
		if (data == null) {
			return null; // <---- NOTE message can be empty
		}

		YNode yroot = null;
		try {
			MessageSyntaxModel syn = mdl.getSyntaxModel();
			Node node = syn.getRoot();
			yroot = new YBag((Bag) node, null);
			saxParse((YBag) yroot, data);
		} catch (MdmiException ex) {
			throw ex;
		} catch (Exception ex) {
			logger.error("Syntax.parse(): unexpected exception", ex);

			throw new MdmiException(ex, "Syntax.parse(): unexpected exception");
		}

		return yroot;
	}

	@Override
	public void serialize(MessageModel mdl, MdmiMessage msg, ISyntaxNode yroot, String transformComment) {
		if (mdl == null || msg == null || yroot == null) {
			throw new IllegalArgumentException("Null argument!");
		}
		try {
			MessageSyntaxModel syn = mdl.getSyntaxModel();
			Node node = syn.getRoot();
			if (node != yroot.getNode()) {
				throw new MdmiException(
					"Invalid serialization attempt, expected node {0} forund node {1}", node.getName(),
					yroot.getNode().getName());
			}
			String nodeName = location(node); // for the root node it is its name
			msg.getData();
			XmlParser p = new XmlParser();
			Document doc = null;
			Element root = null;

			// if (data == null) {
			doc = p.newDocument();

			logger.trace("create node " + nodeName);
			root = doc.createElement(nodeName);

			if ("CDAR2".equals(messageGroupName)) {
				namespace(root, null, "urn:hl7-org:v3");
				namespace(root, "xsi", "http://www.w3.org/2001/XMLSchema-instance");
				namespace(root, "cda", "urn:hl7-org:v3");
				namespace(root, "sdtc", "urn:hl7-org:sdtc");
			}

			doc.appendChild(root);

			if (!StringUtils.isEmpty(transformComment)) {
				String[] comments = transformComment.split(System.getProperty("line.separator"));
				for (String comment : comments) {
					// org.w3c.dom.Comment comment = ;
					root.getParentNode().insertBefore(doc.createComment(comment), root);
				}

			}

			if (node instanceof Bag) {
				serializeBag((YBag) yroot, root);
			} else if (node instanceof Choice) {
				serializeChoice((YChoice) yroot, root);
			} else {
				serializeLeaf((YLeaf) yroot, root);
			}
			msg.setData(format(doc).getBytes());
		} catch (MdmiException ex) {
			throw ex;
		} catch (Exception ex) {

			throw new MdmiException(ex, "Syntax.serialize(): unexpected exception");
		}
	}

	public String format(Document document) {

		DOMImplementationRegistry registry;
		try {
			registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
			LSSerializer writer = impl.createLSSerializer();

			LSOutput lsOutput = impl.createLSOutput();
			lsOutput.setEncoding("UTF-8");
			Writer stringWriter = new StringWriter();
			lsOutput.setCharacterStream(stringWriter);
			writer.getDomConfig().setParameter("format-pretty-print", true);
			writer.write(document, lsOutput);
			String str = stringWriter.toString();
			return str;
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (ClassCastException e) {
		}

		return "";

		// try {
		// TransformerFactory tf = TransformerFactory.newInstance();
		// Transformer trans = tf.newTransformer();
		// StringWriter sw = new StringWriter();
		// trans.transform(new DOMSource(document), new StreamResult(sw));
		// return sw.toString();
		// } catch (TransformerException e) {
		// // TODO Auto-generated catch block
		//
		// }
		//
		// // DOMImplementationRegistry registry;
		// // try {
		// // registry = DOMImplementationRegistry.newInstance();
		// // DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
		// // LSSerializer writer = impl.createLSSerializer();
		// //
		// // LSOutput lsOutput = impl.createLSOutput();
		// // lsOutput.setEncoding("UTF-8");
		// // Writer stringWriter = new StringWriter();
		// // lsOutput.setCharacterStream(stringWriter);
		// // writer.getDomConfig().setParameter("format-pretty-print", true);
		// // writer.write(document, lsOutput);
		// // String str = stringWriter.toString();
		// // return str;
		// // } catch (ClassNotFoundException e) {
		// // } catch (InstantiationException e) {
		// // } catch (IllegalAccessException e) {
		// // } catch (ClassCastException e) {
		// // }
		// //
		// return "";
	}

	/**
	 * Serialize a YBag to the given root element.
	 *
	 * @param yroot
	 *            The bag node to serialize.
	 * @param root
	 *            The node to store it into.
	 */
	int indent = 0;

	private void serializeBag(YBag yroot, Element root) {
		serialize(yroot, root);
	}

	private void serialize(YBag bag, Element element) {

		LinkedList<YNode> ynodes = bag.getYNodes();

		Comparator<YNode> compare = new Comparator<YNode>() {

			@Override
			public int compare(YNode o1, YNode o2) {
				Integer i1;
				Integer i2;
				try {
					if (!StringUtils.isEmpty(o1.getNode().getDescription()) &&
							!StringUtils.isNumeric(o1.getNode().getDescription())) {
						i1 = Integer.valueOf(o1.getNode().getDescription());
					} else {
						i1 = Integer.valueOf(999);
					}
				} catch (Throwable t) {
					i1 = Integer.valueOf(999);
				}
				try {
					if (!StringUtils.isEmpty(o2.getNode().getDescription()) &&
							!StringUtils.isNumeric(o2.getNode().getDescription())) {
						i2 = Integer.valueOf(o2.getNode().getDescription());
					} else {
						i2 = Integer.valueOf(999);
					}
				} catch (Throwable t) {
					i2 = Integer.valueOf(999);
				}
				return i1.compareTo(i2);
			}
		};

		try {
			Collections.sort(ynodes, compare);
		} catch (IllegalArgumentException iae) {
			logger.error(iae.getMessage() + " Invalid Descriptions used for sort");
		}

		// TODO - This loop maintains the physical order of serialization of the syntax mode
		// Replace with more effective loop
		logger.trace("serialize " + element.toString());
		// for (Node node : bag.getBag().getNodes()) {
		for (YNode ynode : ynodes) {
			// // System.out.println("serialize " + ynode.getNode().getName());
			Node node = ynode.getNode();

			// if (ynode.getNode().equals(node)) {
			if (ynode.isBag()) {
				boolean isContainer = false;
				if (node.getSemanticElement() != null && node.getSemanticElement().getDatatype() != null) {
					if ("Container".equals(node.getSemanticElement().getDatatype().getName())) {
						isContainer = true;
					}
				}

				if (node.getMaxOccurs() != 1) {
					isContainer = true;
				}
				try {
					logger.trace(
						"IsContainer " + ynode.getNode().getLocation() + " == " + isContainer + "  " +
								node.getMaxOccurs());
					Element childElement = (Element) createElement(element, ynode.getNode().getLocation(), isContainer);
					// // System.out.println("Created element " + childElement.getNodeName());
					serialize((YBag) ynode, childElement);
				} catch (java.lang.ClassCastException cce) {

				}
			} else if (ynode.isChoice()) {

				try {
					Element childElement = (Element) createElement(element, ynode.getNode().getLocation(), false);
					YChoice yc = (YChoice) ynode;

					for (YNode fgh : yc.getChildren()) {
						serialize((YBag) fgh, childElement);
					}

					// serialize((YBag) ynode, childElement);
				} catch (java.lang.ClassCastException cce) {

				}

			} else if (ynode.isLeaf()) {

				YLeaf yleaf = (YLeaf) ynode;
				String value = yleaf.getValue();
				logger.trace("LEAF Value" + value);

				/*
				 * TODO FIX!!! Passing in a default of true - ynode.getNode().getMaxOccurs() != 1 is not working
				 * All data type attributes are returning 0..1 regardless of the model
				 * The createElement handles attributes but could have other unforseen issues
				 *
				 */

				/*
				 * treatAsContainer - If the leaf node has a "/" in its definition, combine all "/" of similiar types with a element
				 * node.getMaxOccurs != 1
				 * we set treat as container to false - this will cause the createElement process to search for existing elements before
				 * creating new ones
				 * SWM
				 *
				 */
				boolean treatAsContainer = true;

				if (ynode.getNode().getLocation().contains("/") && ynode.getNode().getMaxOccurs() == 1) {
					treatAsContainer = false;
				}

				org.w3c.dom.Node xmlNode = createElement(
					element, (ynode.getNode().getOriginalLocation() != null
							? ynode.getNode().getOriginalLocation()
							: ynode.getNode().getLocation()),
					treatAsContainer);

				if (xmlNode.getNodeType() == org.w3c.dom.Node.ATTRIBUTE_NODE) {
					Attr o = (Attr) xmlNode;
					o.setTextContent(value);
				} else if (xmlNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
					Element o = (Element) xmlNode;
					if (value != null) {
						XmlUtil.setText(o, value);
					}
				} else if (xmlNode.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
					Text o = (Text) xmlNode;
					o.setTextContent(value);
				}
			}
			// }
			// }

		}

	}

	/**
	 * Serialize a YChoice to the given root element.
	 *
	 * @param yroot
	 *            The choice node to serialize.
	 * @param root
	 *            The node to store it into.
	 */
	private void serializeChoice(YChoice yroot, Element root) {
		Node node = yroot.getChosenNode();
		String xpath = location(node);
		ArrayList<YNode> ynodes = yroot.getYNodes();
		for (int j = 0; j < ynodes.size(); j++) {
			if (node instanceof LeafSyntaxTranslator) {
				YLeaf y = (YLeaf) ynodes.get(j);
				setValue(root, xpath, y, j);
			} else if (node instanceof Bag) {
				YBag y = (YBag) ynodes.get(j);
				setBag(root, xpath, y, j);
			} else if (node instanceof Choice) {
				YChoice y = (YChoice) ynodes.get(j);
				setChoice(root, xpath, y, j);
			}
		}
	}

	/**
	 * Serialize a YLeaf to the given root element. Only called for the root leaf
	 * (boundary case).
	 *
	 * @param yroot
	 *            The leaf node to serialize.
	 * @param root
	 *            The node to store it into.
	 */
	private void serializeLeaf(YLeaf yroot, Element root) {
		LeafSyntaxTranslator rootLeaf = yroot.getLeaf();
		String xpath = location(rootLeaf);
		setValue(root, xpath, yroot, 0);
	}

	/**
	 * Set the values from the given YBag into the specified xpath relative to
	 * the given node.
	 *
	 * @param root
	 *            The root XML element relative to which we serialize.
	 * @param xpath
	 *            The XPath to store it at, relative root the root element.
	 * @param ybag
	 *            The YBag containing the values to store.
	 * @param yindex
	 *            The index of the ynode to store, used when having multiple
	 *            instances of ybag.
	 */
	private void setBag(Element root, String xpath, YBag ybag, int yindex) {
		if (xpath == null || xpath.length() <= 0) {
			serializeBag(ybag, root); // mapped to content
		} else {
			org.w3c.dom.Node xmlNode = XslUtil.createNodeForPath(root, xpath, yindex);
			if (xmlNode == null || xmlNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
				throw new MdmiException(
					"Invalid xpath expression {0} for element {1}, ybag {2}", xpath, root.getNodeName(),
					ybag.toString());
			}
			Element e = (Element) xmlNode;
			serializeBag(ybag, e);
		}
	}

	/**
	 * Set the values from the given YChoice into the specified xpath relative to
	 * the given node.
	 *
	 * @param root
	 *            The root XML element relative to which we serialize.
	 * @param xpath
	 *            The XPath to store it at, relative root the root element.
	 * @param ychoice
	 *            The YChoice containing the values to store.
	 * @param yindex
	 *            The index of the ynode to store
	 */
	private void setChoice(Element root, String xpath, YChoice ychoice, int yindex) {
		if (xpath == null || xpath.length() <= 0) {
			serializeChoice(ychoice, root); // mapped to content
		} else {
			org.w3c.dom.Node xmlNode = XslUtil.createNodeForPath(root, xpath, yindex);
			if (xmlNode == null || xmlNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
				throw new MdmiException(
					"Invalid xpath expression {0} for element {1}, ychoice {2}", xpath, root.getNodeName(),
					ychoice.toString());
			}
			Element e = (Element) xmlNode;
			serializeChoice(ychoice, e);
		}
	}

	/**
	 * Set one value (from the given YLeaf) into the specified xpath relative to
	 * the given node. The xpath can be: text()
	 *
	 * @attrName elementName elementName/text() elementName[2]
	 *           elementName[2]/text() elementName@attrName elementName[3]@attrName
	 *           element1/element2...
	 *
	 * @param root
	 *            The root XML element relative to which we serialize.
	 * @param xpath
	 *            The XPath to store it at, relative root the root element.
	 * @param yleaf
	 *            The YLeaf containing the value to store.
	 * @param yindex
	 *            The index of the ynode to store
	 */
	private void setValue(Element root, String xpath, YLeaf yleaf, int yindex) {
		String value = yleaf.getValue();
		logger.trace("Create for " + root.getNodeName() + "  --- " + xpath);
		org.w3c.dom.Node xmlNode = XslUtil.createNodeForPath(root, xpath, yindex);

		if (xmlNode.getNodeType() == org.w3c.dom.Node.ATTRIBUTE_NODE) {
			Attr o = (Attr) xmlNode;
			o.setTextContent(value);
		} else if (xmlNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
			Element o = (Element) xmlNode;
			XmlUtil.setText(o, value);
		} else if (xmlNode.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
			Text o = (Text) xmlNode;
			o.setTextContent(value);
		} else {
			throw new MdmiException(
				"Invalid XML node type for xpath expression {0} for element {1}, yleaf {2}: {3}", xpath,
				root.getNodeName(), yleaf.toString(), xmlNode.getNodeType());
		}
	}

	/**
	 * Get the node location (XPath) name based on the Node.location in the
	 * model. This field can be empty, and it is normally an XPath expression,
	 * such as "elem@attr", etc.
	 *
	 * @param node
	 *            The node.
	 * @return The node location as an XPath expression.
	 */
	static String location(Node node) {
		String location = node.getLocation();
		if (location == null || location.trim().length() <= 0) {
			return null;
		}
		return location.trim();
	}

	/**
	 * Get the model path to the given node.
	 *
	 * @param node
	 *            The node.
	 * @return The path (XPath) to this node.
	 */
	static String getNodePath(Node node) {
		Node parent = node.getParentNode();
		String name = node.getLocation();
		if (parent == null) {
			return name;
		}
		String p = getNodePath(parent);
		if (name.startsWith("@")) {
			return p + name;
		}
		return p + '/' + name;
	}

	protected void namespace(Element element, String prefix, String uri) {
		String qname;
		if ("".equals(prefix) || prefix == null) {
			qname = "xmlns";
		} else {
			qname = "xmlns:" + prefix;
		}

		// older version of Xerces (I confirmed that the bug is gone with Xerces 2.4.0)
		// have a problem of re-setting the same namespace attribute twice.
		// work around this bug removing it first.
		if (element.hasAttributeNS("http://www.w3.org/2000/xmlns/", qname)) {
			// further workaround for an old Crimson bug where the removeAttribtueNS
			// method throws NPE when the element doesn't have any attribute.
			// to be on the safe side, check the existence of attributes before
			// attempting to remove it.
			// for details about this bug, see org.apache.crimson.tree.ElementNode2
			// line 540 or the following message:
			// https://jaxb.dev.java.net/servlets/ReadMsg?list=users&msgNo=2767
			element.removeAttributeNS("http://www.w3.org/2000/xmlns/", qname);
		}
		// workaround until here

		element.setAttributeNS("http://www.w3.org/2000/xmlns/", qname, uri);
	}
} // DefaultMdmiSyntaxParser
