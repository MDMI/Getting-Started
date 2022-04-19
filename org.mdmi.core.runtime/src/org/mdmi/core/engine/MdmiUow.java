/*******************************************************************************

 * Copyright (c) 2012 Firestar Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Firestar Software, Inc. - initial API and implementation
 *
 * Author:
 *     Gabriel Oancea
 *
 *******************************************************************************/
package org.mdmi.core.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.emf.common.util.EList;
import org.mdmi.Bag;
import org.mdmi.ConversionRule;
import org.mdmi.DTCStructured;
import org.mdmi.Field;
import org.mdmi.MDMIBusinessElementReference;
import org.mdmi.MDMIDatatype;
import org.mdmi.MessageGroup;
import org.mdmi.MessageModel;
import org.mdmi.Node;
import org.mdmi.SemanticElement;
import org.mdmi.SemanticElementBusinessRule;
import org.mdmi.SemanticElementRelationship;
import org.mdmi.SemanticElementType;
import org.mdmi.core.ElementValueSet;
import org.mdmi.core.IElementValue;
import org.mdmi.core.ISemanticParser;
import org.mdmi.core.ISyntacticParser;
import org.mdmi.core.ISyntaxNode;
import org.mdmi.core.Mdmi;
import org.mdmi.core.MdmiMessage;
import org.mdmi.core.MdmiModelRef;
import org.mdmi.core.MdmiResolver;
import org.mdmi.core.MdmiTransferInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * MDMI Unit of Work. Will execute a transfer from the source to the target
 * messages, based on the given maps.
 *
 * @author goancea
 */
public class MdmiUow implements Runnable {

	public static Logger logger = LoggerFactory.getLogger(MdmiUow.class);

	MdmiEngine owner;

	MdmiTransferInfo transferInfo;

	ISyntaxNode srcSyntaxModel;

	/**
	 * @return the srcSyntaxModel
	 */
	public ISyntaxNode getSrcSyntaxModel() {
		return srcSyntaxModel;
	}

	/**
	 * @param srcSyntaxModel
	 *            the srcSyntaxModel to set
	 */
	public void setSrcSyntaxModel(ISyntaxNode srcSyntaxModel) {
		this.srcSyntaxModel = srcSyntaxModel;
	}

	/**
	 * @return the trgSyntaxModel
	 */
	public ISyntaxNode getTrgSyntaxModel() {
		return trgSyntaxModel;
	}

	/**
	 * @param trgSyntaxModel
	 *            the trgSyntaxModel to set
	 */
	public void setTrgSyntaxModel(ISyntaxNode trgSyntaxModel) {
		this.trgSyntaxModel = trgSyntaxModel;
	}

	ElementValueSet srcSemanticModel;

	ISyntaxNode trgSyntaxModel;

	ElementValueSet trgSemanticModel;

	/**
	 * serializeSemanticModel is temporary approach to suppress serialize semantic model within web container
	 *
	 */
	static boolean serializeSemanticModel = false;

	public static boolean isSerializeSemanticModel() {
		return serializeSemanticModel;
	}

	public static void setSerializeSemanticModel(boolean serializeSemanticModel) {
		MdmiUow.serializeSemanticModel = serializeSemanticModel;
	}

	/**
	 * Construct a new unit of work instance with the given owner and transfer
	 * info.
	 *
	 * @param owner
	 *            The actual owner.
	 * @param transferInfo
	 *            The transfer info - data used to execute a transfer.
	 */
	MdmiUow(MdmiEngine owner, MdmiTransferInfo transferInfo) {
		this.owner = owner;
		this.transferInfo = transferInfo;

	}

	/**
	 * @param mdmiEngine
	 */
	public MdmiUow(MdmiEngine owner) {
		this.owner = owner;
	}

	public static boolean compareSourceAndTarget = false;

	@Override
	public void run() {

		try {

			StopWatch watch = new StopWatch();
			watch.start();
			logger.info("Execute preProcess " + Thread.currentThread().getName());
			preProcess();
			watch.split();
			logger.info("Split preProcess: " + watch.toSplitString());
			// watch.split();
			// logger.trace("Split Time Elapsed in MILLISECONDS: " + watch.getSplitNanoTime());
			logger.info("Execute processInboundSourceMessage " + Thread.currentThread().getName());
			processInboundSourceMessage();
			watch.split();
			logger.info("Split processInboundSourceMessage: " + watch.toSplitString());

			logger.info("Execute processInboundTargetMessage " + Thread.currentThread().getName());
			processInboundTargetMessage();
			watch.split();
			logger.info("Split processInboundTargetMessage: " + watch.toSplitString());
			// watch.split();
			// logger.trace("Split Time Elapsed in MILLISECONDS: " + watch.getSplitNanoTime());
			logger.info("Done processInboundTargetMessage " + Thread.currentThread().getName());
			logger.info("Execute processConversions " + Thread.currentThread().getName());

			processSourceSemanticModel();
			processConversions();
			processTargetSemanticModel();

			watch.split();
			logger.info("Split processConversions: " + watch.toSplitString());
			logger.info("Done processConversions " + Thread.currentThread().getName());

			if (false && logger.isTraceEnabled()) {

				Multimap<String, IElementValue> sourceMap = ArrayListMultimap.create();
				Multimap<String, IElementValue> targetMap = ArrayListMultimap.create();

				// HashMap<String,String>
				for (IElementValue elementValue : srcSemanticModel.getAllElementValues()) {
					for (ConversionRule conversionRule : elementValue.getSemanticElement().getMapToMdmi()) {
						sourceMap.put(conversionRule.getBusinessElement().getName(), elementValue);
					}
				}

				for (IElementValue elementValue : this.trgSemanticModel.getAllElementValues()) {
					for (ConversionRule conversionRule : elementValue.getSemanticElement().getMapFromMdmi()) {
						targetMap.put(conversionRule.getBusinessElement().getName(), elementValue);
					}
				}

				for (MDMIBusinessElementReference aaa : transferInfo.targetElements) {
					String key = aaa.getName();
					Collection<IElementValue> sources = sourceMap.get(key);
					if (targetMap.containsKey(key)) {
						Collection<IElementValue> targets = targetMap.get(key);
						if (sources.size() != targets.size()) {
							logger.error(
								key + " Number of instances in sources and data are not the same,  " + sources.size() +
										" to " + targets.size());
						} else {
							/**
							 * @TODO Add smart value comparison
							 */
						}
					} else {
						logger.error(key + " missing element in target");
					}

				}

			}
			logger.info("Execute processOutboundTargetMessage " + Thread.currentThread().getName());
			processOutboundTargetMessage();
			watch.split();
			logger.info("Split processOutboundTargetMessage: " + watch.toSplitString());
			// watch.split();
			// logger.trace("Split Time Elapsed in MILLISECONDS: " + watch.getSplitNanoTime());
			logger.info("Execute postProcess " + Thread.currentThread().getName());
			postProcess();
			logger.info("Done Processing " + Thread.currentThread().getName());
			watch.split();
			logger.info("Split postProcess: " + watch.toSplitString());
			watch.stop();

			logger.info(Thread.currentThread().getName() + " Process Message in MILLISECONDS: " + watch.getTime());

		} catch (

		RuntimeException ex) {
			logger.error("Exception during transformation", ex);
		}
	}

	// 0. Call the pre-processors, if any
	void preProcess() {
		Mdmi.INSTANCE().getPreProcessors().preProcess(transferInfo);
	}

	private void serializeSemanticModel(String name, String location, ElementValueSet semanticModel, boolean b) {
		// if logger is trace and serializeSemanticModel is true save the semantic model

		// if (serializeSemanticModel && logger.isTraceEnabled()) {
		//
		// try {
		// ObjectMapper mapper = new ObjectMapper();
		//
		// SimpleModule dateTimeSerializerModule = new SimpleModule(
		// "DateTimeSerializerModule", new Version(1, 0, 0, null));
		// dateTimeSerializerModule.addSerializer(ElementValueSet.class, new ElementValueSetSerializer());
		// mapper.registerModule(dateTimeSerializerModule);
		//
		// SimpleModule xElementValueSerializerModule = new SimpleModule(
		// "XElementValueSerializer", new Version(1, 0, 0, null));
		// xElementValueSerializerModule.addSerializer(XElementValue.class, new XElementValueSerializer());
		// mapper.registerModule(xElementValueSerializerModule);
		//
		// SimpleModule semanticElementSerializerModule = new SimpleModule(
		// "SemanticElementSerializer", new Version(1, 0, 0, null));
		// xElementValueSerializerModule.addSerializer(SemanticElement.class, new SemanticElementSerializer());
		// mapper.registerModule(semanticElementSerializerModule);
		//
		// SimpleModule xDataStructSerializerrModule = new SimpleModule(
		// "XDataStructSerializer", new Version(1, 0, 0, null));
		// xDataStructSerializerrModule.addSerializer(XDataStruct.class, new XDataStructSerializer());
		// mapper.registerModule(xDataStructSerializerrModule);
		//
		// if (location == null) {
		// File directory = new File(String.valueOf("logs"));
		// if (!directory.exists()) {
		// directory.mkdir();
		// }
		// }
		// File jsonFile = new File(
		// String.format(
		// "%s/%s.json", (location != null
		// ? location
		// : "./logs"),
		// name));
		//
		// ArrayList<IElementValue> roots = new ArrayList<IElementValue>();
		// for (IElementValue f : semanticModel.getAllElementValues()) {
		// if (f.getParent() == null) {
		// roots.add(f);
		// } else {
		// }
		// }
		//
		// logger.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(roots));
		//
		// mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, roots);
		//
		// } catch (Exception ex) {
		// logger.error(ex.getLocalizedMessage());
		//
		// }
		// }
	}

	// 1. Build the source syntax tree and semantic model
	void processInboundSourceMessage() {

		StopWatch watch = new StopWatch();
		watch.start();

		ISyntacticParser srcSynProv = getSyntaxProvider(transferInfo.getSourceMessageGroup());

		ISemanticParser srcSemProv = getSemanticProvider(transferInfo.getSourceMessageGroup());
		if (logger.isTraceEnabled()) {
			logger.trace(transferInfo.sourceMessage.getDataAsString());
		}

		watch.split();
		logger.trace("getSyntaxProvider and getSemanticProvider : " + watch.toSplitString());

		srcSyntaxModel = srcSynProv.parse(transferInfo.sourceModel.getModel(), transferInfo.sourceMessage);

		watch.split();
		logger.trace("parse : " + watch.toSplitString());

		logger.debug("Source Syntax Model : \n" + srcSyntaxModel.toString());

		srcSemanticModel = new ElementValueSet();

		srcSemProv.buildSemanticModel(
			transferInfo.sourceModel.getModel(), srcSyntaxModel, srcSemanticModel, transferInfo.sourceProperties);

		watch.split();
		logger.trace("buildSemanticModel : " + watch.toSplitString());

		if (logger.isTraceEnabled()) {
			logElementSet(srcSemanticModel);
		}
		// logger.debug("Source Semantic Model : \n" + srcSemanticModel.toString());

	}

	void logElementSet(ElementValueSet srcSemanticModel2) {

		logger.trace("Source Semantic Model ");
		for (IElementValue elementValue : srcSemanticModel2.getAllElementValues()) {
			XElementValue aaa = (org.mdmi.core.engine.XElementValue) elementValue;
			logXElementValue(aaa);
			if (aaa.getSemanticElement() != null && aaa.getSemanticElement().getSyntaxNode() != null) {
				if (aaa.getSemanticElement().getSyntaxNode().getParentNode() != null) {
					if (aaa.getSemanticElement().getSyntaxNode().getParentNode().getParentNode() == null) {
						logXElementValue(aaa);
					}
				}
				// logger.trace(aaa.getSemanticElement().getName());
				// if (aaa.getSemanticElement().getParent() != null) {
				// if (aaa.getSemanticElement().getParent().getParent() != null) {
				// logger.trace("FOUND AN ELEMENT " + aaa.getSemanticElement().getName());
				// }
				// }
			}
		}
	}

	void logXElementValue(XElementValue xElementValue) {

		logger.trace("XElementValue " + xElementValue.getName());

		if ("Value918".equals(xElementValue.getName())) {
			logger.trace("xElementValue.getName()");
		}
		// xElementValue.get

		if (xElementValue.getSemanticElement() != null && xElementValue.getSemanticElement().getSyntaxNode() != null) {
			logger.trace("LOCATION " + xElementValue.getSemanticElement().getSyntaxNode().getLocation());
		}

		logXValue(xElementValue.getXValue());
		for (IElementValue children : xElementValue.getChildren()) {
			logXElementValue((XElementValue) children);
		}
	}

	void logXDataStruct(XDataStruct xDataStruct) {
		for (XValue value : xDataStruct.getXValues()) {
			logXValue(value);
		}
	}

	void logXValue(XValue xValue) {

		for (Object value : xValue.getValues()) {
			if (value instanceof XDataStruct) {
				logXDataStruct((XDataStruct) value);
			} else {
				logger.trace("VALUE IS " + value);
			}
		}

	}

	// 2. Build the target syntax tree and semantic model (if any)
	void processInboundTargetMessage() {
		trgSemanticModel = new ElementValueSet();
		transferInfo.targetMessage.getData();
		// if (data == null || data.length <= 0) {
		// return;
		// }
		ISemanticParser trgSemProv = getSemanticProvider(transferInfo.getTargetMessageGroup());
		ISyntacticParser trgSynProv = getSyntaxProvider(transferInfo.getTargetMessageGroup());

		transferInfo.targetModel.getModel().getSyntaxModel().getRoot();

		if (transferInfo.targetMessage.getData() != null) {
			trgSyntaxModel = trgSynProv.parse(transferInfo.targetModel.getModel(), transferInfo.targetMessage);
		} else {
			trgSyntaxModel = new YBag((Bag) transferInfo.targetModel.getModel().getSyntaxModel().getRoot(), null);
		}

		trgSemProv.buildSemanticModel(
			transferInfo.targetModel.getModel(), trgSyntaxModel, trgSemanticModel, transferInfo.sourceProperties);

	}

	void getMappedStack(SemanticElement se, Stack<SemanticElement> mappedParentStack) {
		if (se != null) {
			if (se.getSemanticElementType().equals(SemanticElementType.NORMAL) && !(se.getMapFromMdmi().isEmpty())) {
				mappedParentStack.push(se);
			}
			getMappedStack(se.getParent(), mappedParentStack);
		}
		return;
	}

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

	boolean hasTarget(MDMIBusinessElementReference e) {

		for (MDMIBusinessElementReference mber : transferInfo.targetElements) {
			if (mber.getUniqueIdentifier().equals(e.getUniqueIdentifier())) {
				return true;
			}
		}

		return false;
	}

	void findTargetSemanticElements() {

	}

	static ConversionRule getToBE(SemanticElement src, MDMIBusinessElementReference ber) {
		Collection<ConversionRule> toBEs = src.getMapFromMdmi();
		for (Iterator<ConversionRule> it = toBEs.iterator(); it.hasNext();) {
			ConversionRule t = it.next();
			if (t.getBusinessElement() == ber) {
				return t;
			}
		}
		return null;
	}

	static ConversionRule getToSE(SemanticElement trg, MDMIBusinessElementReference ber) {
		Collection<ConversionRule> toSEs = trg.getMapToMdmi();
		for (Iterator<ConversionRule> it = toSEs.iterator(); it.hasNext();) {
			ConversionRule t = it.next();
			if (t.getBusinessElement() == ber) {
				return t;
			}
		}
		return null;
	}

	boolean manyToOneContainers = true;

	HashMap<String, HashSet<String>> manyToOneMatches = new HashMap<String, HashSet<String>>();

	/*
	 *
	 * 92ad5c46-0554-4919-95ad-ed267e4ec007 Observation
	 * 04efdee4-2132-490a-99c4-0f635300879a - Result Observation
	 *
	 */
	void processConversions() {

		StopWatch watch = new StopWatch();
		watch.start();

		// 3. execute the data conversions

		Set<IElementValue> whattotransfer = new LinkedHashSet<IElementValue>();
		HashMap<String, MDMIBusinessElementReference> matches = new HashMap<String, MDMIBusinessElementReference>();
		for (IElementValue iev : srcSemanticModel.getAllElementValues()) {
			logger.trace("Source Semantic Element " + iev.getSemanticElement().getName());

			boolean isMatched = false;
			for (ConversionRule tme : iev.getSemanticElement().getMapToMdmi()) {
				if (hasTarget(tme.getBusinessElement())) {
					logger.debug("Matching Busines Element " + tme.getBusinessElement().getName());
					whattotransfer.add(iev);
					matches.put(tme.getBusinessElement().getUniqueIdentifier(), tme.getBusinessElement());
					isMatched = true;
				} else {

					if (false && manyToOneContainers) {

						if (manyToOneMatches.isEmpty()) {
							manyToOneMatches.put("92ad5c46-0554-4919-95ad-ed267e4ec007", new HashSet<String>());

							manyToOneMatches.get("92ad5c46-0554-4919-95ad-ed267e4ec007").add(
								"04efdee4-2132-490a-99c4-0f635300879a");
						}

						for (String key : manyToOneMatches.keySet()) {
							if (manyToOneMatches.get(key).contains(tme.getBusinessElement().getUniqueIdentifier())) {
								whattotransfer.add(iev);
								tme.getBusinessElement().setUniqueIdentifier(key);
								matches.put(tme.getBusinessElement().getUniqueIdentifier(), tme.getBusinessElement());
								isMatched = true;
							}
							if (tme.getBusinessElement().getUniqueIdentifier().equals(key)) {
								whattotransfer.add(iev);
								tme.getBusinessElement().setUniqueIdentifier(key);
								matches.put(tme.getBusinessElement().getUniqueIdentifier(), tme.getBusinessElement());
								isMatched = true;
							}
						}

					}

				}
			}

			if (!isMatched && !iev.getSemanticElement().isMultipleInstances()) {
				logger.trace("LOOKING FOR PARENT FOR " + iev.getSemanticElement().getName());
				SemanticElementRelationship targetSemanticRelationship = iev.getSemanticElement().getRelationshipByName(
					"PARENTNODE");
				if (targetSemanticRelationship != null) {
					SemanticElement targetSemanticParent = targetSemanticRelationship.getRelatedSemanticElement();
					if (targetSemanticParent != null) {
						for (ConversionRule tme : targetSemanticParent.getMapToMdmi()) {
							if (hasTarget(tme.getBusinessElement())) {
								logger.trace("Matching SINGLE Busines Element " + tme.getBusinessElement().getName());
								whattotransfer.add(iev);
								matches.put(tme.getBusinessElement().getUniqueIdentifier(), tme.getBusinessElement());
								isMatched = true;
							}
						}
					}
				}
			}

		}

		watch.split();
		logger.trace("match : " + watch.toSplitString());

		ConversionImpl impl = new ConversionImpl();

		impl.initializeDI(
			transferInfo.sourceModel.getGroup(), transferInfo.targetModel.getGroup(), transferInfo.sourceProperties,
			transferInfo.targetProperties);

		watch.split();
		logger.trace("impl.initializeDI " + watch.getTime());

		HashMap<IElementValue, ArrayList<IElementValue>> sourcetotarget = new HashMap<IElementValue, ArrayList<IElementValue>>();
		HashMap<IElementValue, IElementValue> targettosource = new HashMap<IElementValue, IElementValue>();

		boolean skipContainmentCheck = "SKIPCONTAINMENT".equals(transferInfo.targetModel.getGroup().getDescription());

		EList<SemanticElement> l = transferInfo.targetModel.getModel().getElementSet().getSemanticElements();

		HashMap<String, ArrayList<SemanticElement>> targetSementicElementsByBER = new HashMap<String, ArrayList<SemanticElement>>();

		for (SemanticElement targetSementicElement : l) {
			if (!targetSementicElement.isMultipleInstances()) {
				continue;
			}
			for (ConversionRule tme : targetSementicElement.getMapFromMdmi()) {
				if (tme.getBusinessElement() != null) {
					if (!targetSementicElementsByBER.containsKey(tme.getBusinessElement().getUniqueIdentifier())) {
						targetSementicElementsByBER.put(
							tme.getBusinessElement().getUniqueIdentifier(), new ArrayList<SemanticElement>());

					}
					targetSementicElementsByBER.get(tme.getBusinessElement().getUniqueIdentifier()).add(
						targetSementicElement);
				}
			}

		}

		for (IElementValue sourceElementValue : whattotransfer) {

			// Create list of RI for the element
			// ArrayList<MDMIBusinessElementReference> businessReference = new ArrayList<MDMIBusinessElementReference>();

			for (ConversionRule tme : sourceElementValue.getSemanticElement().getMapToMdmi()) {

				if (tme.getBusinessElement() == null) {
					continue;
				}

				if (!targetSementicElementsByBER.containsKey(tme.getBusinessElement().getUniqueIdentifier())) {
					continue;
				}
				// businessReference.add(tme.getBusinessElement());
				// }

				// Create list of target semantic elements based on the source RI
				for (SemanticElement targetSementicElement : targetSementicElementsByBER.get(
					tme.getBusinessElement().getUniqueIdentifier())) {
					// if (!targetSementicElement.isMultipleInstances()) {
					// continue;
					// }
					for (ConversionRule tmo : targetSementicElement.getMapFromMdmi()) {
						// for (MDMIBusinessElementReference sourceRI : businessReference) {

						// if (sourceRI.getUniqueIdentifier() == null || tmo.getBusinessElement() == null) {
						// logger.trace("sourceRI.getUniqueIdentifier() == null || tmo.getBusinessElement() == null");
						// continue;
						// }
						// if (sourceRI.getUniqueIdentifier().equals(tmo.getBusinessElement().getUniqueIdentifier())) {
						logger.trace("CREATE CORRESPONDNG ELEMENT " + targetSementicElement.getName());
						Stack<SemanticElement> mappedParentStack = new Stack<SemanticElement>();
						getMappedStack(targetSementicElement, mappedParentStack);

						boolean wholeStackMapped = true;

						if (!skipContainmentCheck) {
							while (!mappedParentStack.isEmpty()) {
								SemanticElement mp1 = mappedParentStack.pop();
								boolean isMP1Mapped = false;
								for (ConversionRule lll : mp1.getMapFromMdmi()) {
									if (matches.containsKey(lll.getBusinessElement().getUniqueIdentifier())) {
										isMP1Mapped = true;
										break;
									}
								}
								wholeStackMapped = isMP1Mapped;
								if (!wholeStackMapped) {
									break;
								}
							}
						}

						// wholeStackMapped = true;

						if (wholeStackMapped) {
							logger.trace("CREATING TARGET ELEMENT " + targetSementicElement.getName());
							XElementValue targetElementValue = new XElementValue(
								targetSementicElement, trgSemanticModel);

							try {
								@SuppressWarnings("deprecation")
								org.mdmi.core.engine.Conversion.ConversionInfo ci = new org.mdmi.core.engine.Conversion.ConversionInfo(
									targetSementicElement, tme.getBusinessElement(), tmo.getBusinessElement());
								impl.convert((XElementValue) sourceElementValue, ci, targetElementValue);

								targettosource.put(targetElementValue, sourceElementValue);

								if (!sourcetotarget.containsKey(sourceElementValue)) {
									sourcetotarget.put(sourceElementValue, new ArrayList<IElementValue>());
								}
								sourcetotarget.get(sourceElementValue).add(targetElementValue);

							} catch (Exception e) {
								logger.error("ERROR IN CONVERSION", e);
							}
						} else {
							logger.debug(
								sourceElementValue.getName() +
										" NOT TRANSFERRED, Semantic containment not established");
							if (logger.isDebugEnabled()) {
								Stack<SemanticElement> missingSemanticStack = new Stack<SemanticElement>();
								getMappedStack(targetSementicElement, missingSemanticStack);

								while (!missingSemanticStack.isEmpty()) {
									SemanticElement mp1 = missingSemanticStack.pop();
									logger.debug("Semantic Path " + mp1.getName());
									boolean isMP1Mapped = false;
									for (ConversionRule lll : mp1.getMapFromMdmi()) {
										if (matches.containsKey(lll.getBusinessElement().getUniqueIdentifier())) {
											isMP1Mapped = true;
											break;
										}
									}
									if (!isMP1Mapped) {
										logger.debug("MISSING ELEMENT " + mp1.getName());
										break;
									}
								}
							}

						}

					}
				}

			}

		}

		watch.split();
		logger.trace("what to transfer : " + watch.toSplitString());

		ArrayList<SemanticElement> singles = new ArrayList<SemanticElement>();

		for (SemanticElement semanticElement : transferInfo.targetModel.getModel().getElementSet().getSemanticElements()) {
			if (!semanticElement.isMultipleInstances()) {
				singles.add(semanticElement);
			}
		}

		for (SemanticElement s : singles) {
			logger.trace("SINGLE ELEMENTS : " + s.getName());
		}

		/*
		 * Loop over semantic elements defined as single instance
		 * Check for semantic parent - currently no support or need to have root singleton
		 * If the single parent has content - populate the appropriate single instances per container
		 *
		 */
		for (SemanticElement single : singles) {

			if (single.getParent() != null) {

				if (!trgSemanticModel.hasElementValuesByName(single.getParent().getName())) {
					continue;
				}

				List<IElementValue> values = trgSemanticModel.getElementValuesByName(single.getParent().getName());

				// if (values.isEmpty()) {
				// continue;
				// }

				logger.trace("GETTING SINGLE ELEMENT PARENTS : " + single.getParent().getName());
				logger.trace("GETTING SINGLE ELEMENT PARENTS : " + values);

				for (IElementValue v : values) {
					logger.trace("GETTING SINGLE ELEMENT PARENTS : " + v.getSemanticElement().getName());
				}

				for (IElementValue foo : sourcetotarget.keySet()) {

					logger.trace("xxxGETTING SINGLE ELEMENT PARENTS : " + foo.getSemanticElement().getName());
				}

				for (ConversionRule x : single.getMapToMdmi()) {
					for (IElementValue bbbb : whattotransfer) {

						ArrayList<MDMIBusinessElementReference> businessReference = new ArrayList<MDMIBusinessElementReference>();

						for (ConversionRule tme : bbbb.getSemanticElement().getMapToMdmi()) {
							businessReference.add(tme.getBusinessElement());
						}

						for (MDMIBusinessElementReference sourceRI : businessReference) {
							if (sourceRI.getUniqueIdentifier().equals(x.getBusinessElement().getUniqueIdentifier())) {
								logger.trace("SINGLE ELEMENT MATCHED : " + sourceRI.getName());
								logger.trace("SINGLE ELEMENT PARENT : " + single.getName());
								logger.trace("SOURCE ELEMENT CONTAINER : " + bbbb.getSemanticElement().getName());
								// bbbb

								XElementValue singleElementValue = new XElementValue(single, trgSemanticModel);
								try {
									@SuppressWarnings("deprecation")
									org.mdmi.core.engine.Conversion.ConversionInfo ci = new org.mdmi.core.engine.Conversion.ConversionInfo(
										single, sourceRI, x.getBusinessElement());
									impl.convert((XElementValue) bbbb, ci, singleElementValue);

									List<IElementValue> foundElements = trgSemanticModel.getElementValuesByName(
										single.getParent().getName());

									for (IElementValue parentElementValue : foundElements) {
										parentElementValue.addChild(singleElementValue);
										singleElementValue.setParent(parentElementValue);
									}

								} catch (Exception e) {
									logger.error("ERROR IN CONVERSION", e);
								}

							}
						}

					}
				}
			}
		}

		watch.split();
		logger.trace("singles : " + watch.toSplitString());
		/*
		 * Loop over and set semantic containers
		 */

		// for (

		// IElementValue targetElementValue : targettosource.keySet()) {

		for (IElementValue targetElementValue : this.trgSemanticModel.getAllElementValues()) {
			if (targettosource.containsKey(targetElementValue)) {
				if (targetElementValue.getParent() == null) {
					if (targetElementValue.getSemanticElement().isMultipleInstances()) {
						IElementValue sourceElementValue = targettosource.get(targetElementValue);
						IElementValue sourceParent = sourceElementValue.getParent();

						while (sourceParent != null && !sourcetotarget.containsKey(sourceParent)) {
							sourceParent = sourceParent.getParent();
						}
						if (sourceParent != null && sourcetotarget.containsKey(sourceParent)) {
							for (IElementValue targetParent : sourcetotarget.get(sourceParent)) {
								targetParent.addChild(targetElementValue);
							}

							// if (!sourcetotarget2.isEmpty()) {

							// while (sourceParent != null && !sourcetotarget.containsKey(sourceParent)) {
							// sourceParent = sourceParent.getParent();
							// }
							// if (sourceParent != null && sourcetotarget2.containsKey(sourceParent)) {
							// sourcetotarget2.get(sourceParent).addChild(targetElementValue);
						}

						// }

					}
				}
			}

		}
		// for (IElementValue elementValueToTransfer : this.trgSemanticModel.getAllElementValues()) {''
		// for (IElementValue targetElementValue : this.trgSemanticModel.getAllElementValues()) {

		ListIterator<IElementValue> iterator = trgSemanticModel.getAllElementValues().listIterator();

		while (iterator.hasNext()) {
			IElementValue targetElementValue = iterator.next();

			if (isBreakContainment(targetElementValue.getSemanticElement())) {
				if (targetElementValue.getParent() != null) {
					for (IElementValue child : targetElementValue.getChildren()) {
						if (isBreadCrumb(child.getSemanticElement())) {
							// logger.trace("isBreadCrumb 1 " + child.getSemanticElement().getName());
							for (SemanticElement child2 : targetElementValue.getParent().getSemanticElement().getChildren()) {
								if (isBreadCrumb(child2)) {
									// logger.trace("isBreadCrumb 2 " + child2.getName());
									XElementValue idElementValue = new XElementValue(
										child2, trgSemanticModel, iterator);
									idElementValue.setValue(child.value());
									targetElementValue.getParent().addChild(idElementValue);
								}
							}
						}
					}
					targetElementValue.getParent().removeChild(targetElementValue);
					targetElementValue.setParent(null);
				}
			}
		}

		watch.split();
		logger.trace("containers : " + watch.toSplitString());

		ArrayList<IElementValue> tobedeleted = new ArrayList<IElementValue>();
		for (IElementValue targetElementValue : targettosource.keySet()) {
			SemanticElement se = targetElementValue.getSemanticElement();
			SemanticElementRelationship ser = se.getRelationshipByName("QUALIFIER");
			if (ser != null) {
				for (IElementValue child : targetElementValue.getParent().getChildren()) {
					if (child.getSemanticElement().getName().equals(ser.getRelatedSemanticElement().getName())) {
						if (!impl.datamapInterpreter.execute("is" + se.getName(), child, impl.targetProperties)) {
							tobedeleted.add(targetElementValue);
						}

					}
				}
			}

		}

		watch.split();
		logger.trace("QUALIFIER : " + watch.toSplitString());

		for (

		IElementValue remove : tobedeleted) {
			trgSemanticModel.removeElementValue(remove);
			if (remove.getParent() != null) {
				remove.getParent().removeChild(remove);
			}
		}

		for (

		String key : impl.datamapInterpreter.exceptions.keySet()) {
			logger.error(key);
			logger.error(impl.datamapInterpreter.exceptions.get(key).getMessage());
		}

		// logger.debug("Target Semantic Model : \n" + trgSemanticModel.toString());

		watch.split();
		logger.trace("Done : " + watch.toSplitString());

	}

	// 4. Build the target syntax tree from the target semantic model
	void processOutboundTargetMessage() {
		ISemanticParser trgSemProv = getSemanticProvider(transferInfo.getTargetMessageGroup());
		ISyntacticParser trgSynProv = getSyntaxProvider(transferInfo.getTargetMessageGroup());
		long ts = System.currentTimeMillis();
		if (trgSyntaxModel != null) {
			trgSemProv.updateSyntacticModel(
				transferInfo.targetModel.getModel(), trgSemanticModel, trgSyntaxModel, transferInfo.targetProperties);
		} else {
			trgSyntaxModel = trgSemProv.createNewSyntacticModel(
				transferInfo.targetModel.getModel(), trgSemanticModel, transferInfo.targetProperties);
		}

		logger.debug("Target Syntax Model : \n" + trgSyntaxModel.toString());

		logger.debug("updateSyntacticModel  took " + (System.currentTimeMillis() - ts) + " milliseconds.");
		ts = System.currentTimeMillis();

		StringBuffer runtimeComment = new StringBuffer();

		/**
		 * @TODO
		 *       Temporary update to not add comments to FHIR targets
		 *
		 */
		if (System.getenv("RUNTIMECOMMENT") != null) {
			runtimeComment.append("Generated using MDMI " + Mdmi.RUNTIMEVERSION + "  " + Mdmi.RUNTIMEBUILD).append(
				System.getProperty("line.separator"));

			for (MessageModel messageModel : transferInfo.sourceModel.getModel().getGroup().getModels()) {
				runtimeComment.append(
					"Source Group : " + transferInfo.sourceModel.getModel().getGroup().getName()).append(
						System.getProperty("line.separator"));
				;
				runtimeComment.append("Source Message : " + messageModel.getMessageModelName()).append(
					System.getProperty("line.separator"));
				;
				runtimeComment.append("Source Version : " + messageModel.getDescription()).append(
					System.getProperty("line.separator"));
				;
			}

			for (MessageModel messageModel : transferInfo.targetModel.getModel().getGroup().getModels()) {
				runtimeComment.append(
					"Target Group : " + transferInfo.targetModel.getModel().getGroup().getName()).append(
						System.getProperty("line.separator"));
				;
				runtimeComment.append("Target Message : " + messageModel.getMessageModelName()).append(
					System.getProperty("line.separator"));
				;
				runtimeComment.append("Target Version : " + messageModel.getDescription()).append(
					System.getProperty("line.separator"));
				;
			}
		}

		trgSynProv.serialize(
			transferInfo.targetModel.getModel(), transferInfo.targetMessage, trgSyntaxModel, runtimeComment.toString());

		//
		logger.debug(
			"Serializing the target syntax model took " + (System.currentTimeMillis() - ts) + " milliseconds.");
	}

	private boolean isBreadCrumb(SemanticElement semanticElement) {
		return checkRules(semanticElement, "BREADCRUMB");
	}

	private boolean isBreakContainment(SemanticElement semanticElement) {
		return checkRules(semanticElement, "BREAKCONTAINMENT");
	}

	private boolean checkRules(SemanticElement semanticElement, String ruleName) {
		if (semanticElement != null) {
			for (SemanticElementBusinessRule sebr : semanticElement.getBusinessRules()) {
				if (ruleName.equals(sebr.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	// 5. Call the post-processors, if any
	void postProcess() {

		Mdmi.INSTANCE().getPostProcessors().postProcess(transferInfo);

	}

	private MdmiResolver resolver() {
		return owner.getOwner().getResolver();
	}

	private ISyntacticParser getSyntaxProvider(MessageGroup messageGroup) {

		for (MessageModel s : messageGroup.getModels()) {
			return resolver().getSyntacticParser(messageGroup.getName(), s.getMessageModelName());
		}
		return null;

	}

	private ISemanticParser getSemanticProvider(MessageGroup messageGroup) {
		for (MessageModel s : messageGroup.getModels()) {
			return resolver().getSemanticParser(messageGroup.getName(), s.getMessageModelName());
		}
		return null;

	}

	@Override
	public String toString() {
		if (transferInfo == null) {
			return "MdmiUoW: data is not set!";
		}
		return transferInfo.toString();
	}

	/**
	 * @param trgSemanticModel2
	 * @param sMod
	 * @param tMsg
	 * @param bers
	 */

	public static class FieldAndPath {
		public String path;

		public Field field;

		/**
		 * @param path
		 * @param field
		 */
		public FieldAndPath(String path, Field field) {
			super();
			this.path = path;
			this.field = field;
		}
	}

	void collectFields(MDMIDatatype mdmiDatatype, String path, ArrayList<FieldAndPath> fields) {
		// logger.trace(path);

		if (mdmiDatatype == null || mdmiDatatype.getFields() == null) {
			return;
		}
		if (path != null && path.endsWith("extension")) {
			return;
		}
		for (Field field : mdmiDatatype.getFields()) {
			if (field.getDatatype().isSimple()) {
				String fieldPath = (path != null
						? path + "."
						: "") + field.getName();
				fields.add(new FieldAndPath(fieldPath, field));
			} else {
				if (path == null || !path.contains(field.getName())) {
					collectFields(
						mdmiDatatype.getField(field.getName()).getDatatype(), (path != null
								? path + "."
								: "") + field.getName(),
						fields);
				}

			}
		}
	}

	void populateSourceSemanticModel(SemanticElement theSemanticElement, XElementValue parent,
			ArrayList<MDMIBusinessElementReference> bers, ArrayList<SemanticElement> created,
			ArrayList<String> singles) {
		XElementValue p = parent;
		MDMIBusinessElementReference theSEER = null;
		if (!theSemanticElement.getMapFromMdmi().isEmpty()) {

			boolean createSemanticElement = false;
			for (ConversionRule tbe : theSemanticElement.getMapFromMdmi()) {
				if (tbe.getBusinessElement() != null) {
					for (MDMIBusinessElementReference mber : bers) {
						if (mber.getUniqueIdentifier().equals(tbe.getBusinessElement().getUniqueIdentifier()) &&
								theSemanticElement.isMultipleInstances()) {
							createSemanticElement = true;
							theSEER = mber;
						}
					}

				}
			}

			if (!createSemanticElement && !theSemanticElement.isMultipleInstances()) {
				logger.trace("CHECKING SINGLE INSTANCE ELEMENT" + theSemanticElement.getName());
				SemanticElementRelationship targetSemanticRelationship = theSemanticElement.getRelationshipByName(
					"PARENTNODE");
				if (targetSemanticRelationship != null) {
					SemanticElement targetSemanticParent = targetSemanticRelationship.getRelatedSemanticElement();
					if (targetSemanticParent != null) {
						logger.trace("SINGLE INSTANCE PARENT IS" + targetSemanticParent.getName());
						for (ConversionRule tbe : targetSemanticParent.getMapFromMdmi()) {
							if (tbe.getBusinessElement() != null) {
								for (MDMIBusinessElementReference mber : bers) {
									if (!singles.contains(tbe.getBusinessElement().getUniqueIdentifier()) &&
											mber.getUniqueIdentifier().equals(
												tbe.getBusinessElement().getUniqueIdentifier())) {
										createSemanticElement = true;
										theSEER = mber;
										logger.trace("CREATED SINGLE INSTANCE " + theSemanticElement.getName());
										singles.add(tbe.getBusinessElement().getUniqueIdentifier());
									}
								}
							}
						}
					}
				}
			}

			if (createSemanticElement) {

				logger.trace(
					"Created Source Element " + theSemanticElement.getName() + " " +
							getFullPathForNode(theSemanticElement.getSyntaxNode()));

				created.add(theSemanticElement);
				XElementValue trg = new XElementValue(theSemanticElement, srcSemanticModel);
				if (parent != null) {
					parent.addChild(trg);
				}
				p = trg;
				if (trg.getXValue().getValues().size() == 0) {
					if ((trg.getXValue().getDatatype() instanceof DTCStructured)) {
						XDataStruct xs = new XDataStruct(trg.getXValue());

						ArrayList<FieldAndPath> values = new ArrayList<FieldAndPath>();
						collectFields(xs.getDatatype(), null, values);
						for (FieldAndPath fieldAndPath : values) {
							switch (fieldAndPath.field.getDatatype().getName()) {
								case "Decimal":
									xs.setValueSafely(fieldAndPath.path, String.valueOf(1000));
									break;
								case "Integer":
									xs.setValueSafely(fieldAndPath.path, String.valueOf(1000));
									break;
								case "Boolean":
									xs.setValueSafely(fieldAndPath.path, "false");
									break;
								default:
									xs.setValueSafely(
										fieldAndPath.path,
										theSEER.getName() + "." + fieldAndPath.path + System.currentTimeMillis());
									break;
							}

						}
						trg.getXValue().addValue(xs);
					} else {
						trg.getXValue().addValue(
							(parent != null
									? parent.getName() + "."
									: "") + theSemanticElement.getName() + "." + theSEER.getName());
						if (trg.getXValue().getDatatype() == null) {
							logger.trace("errror");
						}
						if (trg.getXValue().getDatatype() != null) {
							logger.trace(trg.getXValue().getDatatype().getName());
						} else {
							logger.trace("datatype is null");
						}

					}
				}
			}

		}
		for (SemanticElement child : theSemanticElement.getChildren()) {
			populateSourceSemanticModel(child, p, bers, created, singles);
		}
	}

	public void run(MdmiModelRef sMod, MdmiMessage tMsg, ArrayList<MDMIBusinessElementReference> bers,
			SemanticElement semanticContainer, List<SemanticElement> semanticElements, String location) {
		try {

			logger.info("Execute ISOSemantic ");

			transferInfo = new MdmiTransferInfo(sMod, tMsg, sMod, tMsg, bers);

			transferInfo.location = location;

			ConversionImpl impl = new ConversionImpl();
			impl.datamapInterpreter = null;

			trgSemanticModel = new ElementValueSet();
			srcSemanticModel = new ElementValueSet();
			//
			logger.info("Create Source Model ");
			ArrayList<SemanticElement> created = new ArrayList<SemanticElement>();
			ArrayList<String> singles = new ArrayList<String>();
			// transferInfo.sourceModel.getModel().getElementSet().getSemanticElements().clear();
			// for (SemanticElement se : transferInfo.sourceModel.getModel().getElementSet().getSemanticElements()) {
			// if (se.getParent() == null) {
			populateSourceSemanticModel(semanticContainer, null, bers, created, singles);
			// }

			// }

			serializeSemanticModel("SourceSemanticModel", transferInfo.location, srcSemanticModel, false);

			processSourceSemanticModel();

			logger.debug("Target Semantic Model : \n" + trgSemanticModel.toString());
			logger.info("Execute processConversions ");
			processConversions();
			logger.info("Execute processOutboundTargetMessage");

			processTargetSemanticModel();

			HashMap<String, IElementValue> results = new HashMap<String, IElementValue>();

			for (IElementValue ev : trgSemanticModel.getAllElementValues()) {
				results.put(ev.getSemanticElement().getName(), ev);
			}

			for (SemanticElement se : created) {
				if (!results.containsKey(se.getName())) {
					logger.error("Semantic Element not Transformed " + se.getName());
					logger.error(getFullPathForNode(se.getSyntaxNode()));

					for (ConversionRule tbe : se.getMapFromMdmi()) {
						logger.error("Semantic Element not Transformed " + tbe.getBusinessElement().getName());

					}
					for (ConversionRule tse : se.getMapToMdmi()) {
						logger.error("Semantic Element not Transformed " + tse.getBusinessElement().getName());

					}

					// for (se.getMapToMdmi()) {
					//
					// }
				}
			}

			if (impl.datamapInterpreter != null) {
				for (String function : impl.datamapInterpreter.exceptions.keySet()) {
					logger.error(
						"Datatype Interperted Errors " + function + " " +
								impl.datamapInterpreter.exceptions.get(function).getMessage());
				}
			} else {
				logger.trace("impl.datamapInterpreter  not set");
			}

			processOutboundTargetMessage();

			// MdmiMessage mdmiMessage = new MdmiMessage(transferInfo.targetMessage.getDataAsString());

			serializeSemanticModel("TargetSemanticModel", transferInfo.location, trgSemanticModel, false);

			// System.out.println(transferInfo.targetMessage.getData());

			transferInfo.sourceMessage.setData(transferInfo.targetMessage.getData());

			// ElementValueSet originalsrcSemanticModel = srcSemanticModel;
			srcSemanticModel = new ElementValueSet();
			processInboundSourceMessage();

			results.clear();
			for (IElementValue ev : srcSemanticModel.getAllElementValues()) {
				results.put(ev.getSemanticElement().getName(), ev);
			}

			for (SemanticElement se : created) {
				if (!results.containsKey(se.getName())) {

					StringBuffer message = new StringBuffer(
						"Semantic Element not Parsed " + se.getName() + " SYNTAXNODE = " +
								getFullPathForNode(se.getSyntaxNode()));

					for (ConversionRule tose : se.getMapToMdmi()) {

						message.append(" (").append(tose.getBusinessElement().getName()).append(")");
					}

					logger.error(message.toString());
				}
			}

			postProcess();

			logger.info("Completed Transformation");
		} catch (RuntimeException ex) {
			logger.error("Exception during transformation", ex);
		}
	}

	/**
	 *
	 */
	private void processTargetSemanticModel() {
		Mdmi.INSTANCE().getTargetSemanticModelProcessors().targetSemanticModelProcessing(
			transferInfo, trgSemanticModel);
	}

	/**
	 *
	 */
	private void processSourceSemanticModel() {
		Mdmi.INSTANCE().getSourceSemanticModelProcessors().sourceSemanticProcessing(transferInfo, srcSemanticModel);
	}

	String getFullPathForNode(Node n) {
		if (n == null) {
			return "NULL SYNTAX NODE!!";
		}
		if (n.getParentNode() == null) {
			return n.getLocation();
		} else {
			return getFullPathForNode(n.getParentNode()) + "/" + n.getLocation();
		}
	}

	/**
	 *
	 */
	public void clean() {
		if (this.srcSemanticModel != null) {
			this.srcSemanticModel.getAllElementValues().clear();
		}

		if (this.trgSemanticModel != null) {
			this.trgSemanticModel.getAllElementValues().clear();
		}

		this.srcSyntaxModel = null;
		this.trgSyntaxModel = null;
		//
		// this.srcSemanticModel.clearCach();
		// this.trgSemanticModel.clearCach();
		// this.trgSemanticModel = null;
		// this.srcSemanticModel = null;

	}

} // MdmiUow
