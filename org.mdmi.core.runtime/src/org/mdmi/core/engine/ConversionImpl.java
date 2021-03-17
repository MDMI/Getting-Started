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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.ConversionRule;
import org.mdmi.DTCStructured;
import org.mdmi.DTExternal;
import org.mdmi.DTSEnumerated;
import org.mdmi.EnumerationLiteral;
import org.mdmi.Field;
import org.mdmi.MDMIBusinessElementReference;
import org.mdmi.MDMIDatatype;
import org.mdmi.MessageGroup;
import org.mdmi.Node;
import org.mdmi.SemanticElement;
import org.mdmi.core.Mdmi;
import org.mdmi.core.MdmiValueSet;
import org.mdmi.core.MdmiValueSetMap;
import org.mdmi.core.MdmiValueSetsHandler;
import org.mdmi.core.engine.Conversion.ConversionInfo;
import org.mdmi.core.engine.ITerminologyTransform.TransformCode;
import org.mdmi.core.engine.terminology.FHIRTerminologyTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation class for rule execution.
 */
@SuppressWarnings("deprecation")
class ConversionImpl {

	private static Logger logger = LoggerFactory.getLogger(ConversionImpl.class);

	boolean logging;

	FileOutputStream jsonFop;

	File jsonFile;

	boolean first = true;

	// ObjectNode conversionNode;

	private static String parseFunctionName(String rule) {

		String[] content = rule.split(":");

		if (content.length == 2) {
			return content[1];
		} else {
			return "invalidfunctionmapping";
		}
	}

	public DatamapInterpreter datamapInterpreter;

	// public SemanticRollupInterpreter semanticRollupInterpreter;

	// public static ConversionImpl Instance = new ConversionImpl();

	@SuppressWarnings("unused")
	private void logToJson() throws Exception {
		// if (logging) {
		// if (first) {
		// first = false;
		// } else {
		// jsonFop.write(",".getBytes());
		// }
		//
		// mapper.writeValue(jsonFop, conversionNode);
		//
		// conversionNode = null;
		// }
	}

	@SuppressWarnings("unused")
	private void logObject(String name, Object object) {

		// if (logging) {
		// if (conversionNode == null) {
		// conversionNode = mapper.createObjectNode().putObject("conversion");
		// }
		// conversionNode.putPOJO(name, object);
		// }
	}

	FHIRTerminologyTransform terminologyService;

	/**
	 * @return the terminologyService
	 */
	public FHIRTerminologyTransform getTerminologyService() {
		if (terminologyService == null) {
			terminologyService = new FHIRTerminologyTransform();
		}
		return terminologyService;
	}

	public static class SEERvalues {
		/**
		 * @param mber
		 * @param xvalue
		 */
		public SEERvalues(String mber, XValue xvalue) {
			super();
			this.mber = mber;
			this.xvalue = xvalue;
		}

		public String mber;

		public XValue xvalue;;

	}

	ArrayList<SEERvalues> theSeerValues = new ArrayList<SEERvalues>();

	boolean convert(XElementValue sourceSemanticElement, ConversionInfo ci, XElementValue targetSemanticElement)
			throws Exception {

		logger.trace("convert business element " + ci.srcBER.getName());

		ConversionRule mapToMDMI = Conversion.getToBE(sourceSemanticElement.getSemanticElement(), ci.srcBER);

		ConversionRule mapFromMDMI = Conversion.getToSE(targetSemanticElement.getSemanticElement(), ci.trgBER);

		XValue xv = new XValue(
			mapToMDMI.getBusinessElement().getName(), mapToMDMI.getBusinessElement().getReferenceDatatype());

		execMapToMDMI(sourceSemanticElement, mapToMDMI, xv);
		if (!xv.isNullOrEmpty()) {
			theSeerValues.add(new SEERvalues(mapToMDMI.getBusinessElement().getName(), xv));

			execMapFromMDMI(xv, targetSemanticElement, mapFromMDMI);
		}

		return !xv.isNullOrEmpty();
	}

	// public static void serializeSEER() {
	// // if logger is trace and serializeSemanticModel is true save the semantic model
	//
	// if (logger.isTraceEnabled()) {
	//
	// try {
	// ObjectMapper mapper = new ObjectMapper();
	//
	// // SimpleModule dateTimeSerializerModule = new SimpleModule(
	// // "DateTimeSerializerModule", new Version(1, 0, 0, null));
	// // dateTimeSerializerModule.addSerializer(ElementValueSet.class, new ElementValueSetSerializer());
	// // mapper.registerModule(dateTimeSerializerModule);
	// //
	// // SimpleModule xElementValueSerializerModule = new SimpleModule(
	// // "XElementValueSerializer", new Version(1, 0, 0, null));
	// // xElementValueSerializerModule.addSerializer(XElementValue.class, new XElementValueSerializer());
	// // mapper.registerModule(xElementValueSerializerModule);
	// //
	// // SimpleModule semanticElementSerializerModule = new SimpleModule(
	// // "SemanticElementSerializer", new Version(1, 0, 0, null));
	// // xElementValueSerializerModule.addSerializer(SemanticElement.class, new SemanticElementSerializer());
	// // mapper.registerModule(semanticElementSerializerModule);
	// //
	// SimpleModule xValueSerializerrModule = new SimpleModule("XValueSerializer", new Version(1, 0, 0, null));
	// xValueSerializerrModule.addSerializer(XValue.class, new XValueSerializer());
	// mapper.registerModule(xValueSerializerrModule);
	//
	// SimpleModule xDataStructSerializerrModule = new SimpleModule(
	// "XDataStructSerializer", new Version(1, 0, 0, null));
	// xDataStructSerializerrModule.addSerializer(XDataStruct.class, new XDataStructSerializer());
	// mapper.registerModule(xDataStructSerializerrModule);
	//
	// // if (location == null) {
	// // File directory = new File(String.valueOf("logs"));
	// // if (!directory.exists()) {
	// // directory.mkdir();
	// // }
	// // }
	// // File jsonFile = new File(
	// // String.format(
	// // "%s/%s.json", (location != null
	// // ? location
	// // : "./logs"),
	// // name));
	// //
	// // ArrayList<IElementValue> roots = new ArrayList<IElementValue>();
	// // for (IElementValue f : semanticModel.getAllElementValues()) {
	// // if (f.getParent() == null) {
	// // roots.add(f);
	// // } else {
	// // }
	// // }
	//
	// logger.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(theSeerValues));
	//
	// // mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, roots);
	//
	// } catch (Exception ex) {
	// logger.error(ex.getLocalizedMessage());
	//
	//
	// }
	// }
	// }

	boolean hasSrcRule(ConversionRule toBE) {
		return toBE != null && toBE.getRule() != null && 0 < toBE.getRule().length();
	}

	boolean hasTrgRule(ConversionRule toSE) {
		return toSE != null && toSE.getRule() != null && 0 < toSE.getRule().length();
	}

	private static Map<String, DatamapInterpreter> datamapInterpreters = new HashMap<String, DatamapInterpreter>();

	public synchronized void initializeDI(MessageGroup source, MessageGroup target, Properties sp, Properties tp) {
		String key = source.getName() + "_" + source.getModels().get(0).getMessageModelName() + "__DATATYPEMAPS___" +
				target.getName() + "_" + target.getModels().get(0).getMessageModelName();

		if (!datamapInterpreters.containsKey(key)) {
			datamapInterpreters.put(key, new DatamapInterpreter(source, target));
		}
		datamapInterpreter = datamapInterpreters.get(key);

		sourceProperties = sp;
		targetProperties = tp;
		theSeerValues.clear();

	}

	Properties sourceProperties;

	Properties targetProperties;

	void execMapToMDMI(XElementValue src, ConversionRule toBE, XValue v) {

		if (!hasSrcRule(toBE)) {
			logger.trace("cloneValue");
			cloneValue(src.getXValue(), v, true, toBE, null);
		} else if (toBE.getRule().startsWith("DATATYPEMAP:")) {

			if (datamapInterpreter == null) {
				throw new RuntimeException("execMapToMDMI datamapInterpreter is null");
				// datamapInterpreter = new DatamapInterpreter(toBE.getOwner().getElementSet().getModel().getGroup());
			}

			// If getValue == null - we have a primitive pass int the XValue to method
			boolean executed = true;
			if (v.getValue() == null) {
				executed = datamapInterpreter.execute(
					parseFunctionName(toBE.getRule()), src.value(), v, sourceProperties);

			} else {
				executed = datamapInterpreter.execute(
					parseFunctionName(toBE.getRule()), src.value(), v.getValue(), sourceProperties);
			}
			if (!executed) {
				logger.error(
					"Unable To Execute " + parseFunctionName(toBE.getRule()) + " at " +
							getFullPathForNode(toBE.getOwner().getSyntaxNode()) + " for " +
							toBE.getBusinessElement().getName());
			}

		} else {
			// System.err.println("EXECUTE EXECMAPTOMDMI");
			// IExpressionInterpreter adapter = Mdmi.getInterpreter(toBE, src, toBE.getName(), v);
			//
			// logger.trace("Source Rule " + toBE.getRule());
			// adapter.evalAction(src, toBE.getRule(), sourceProperties);
		}

		// If we have a terminology conversion
		if (v != null && hasTerminologyTransformation(toBE, toBE.getBusinessElement())) {

			XDataStruct xds = (XDataStruct) v.getValue();
			if (xds.hasfield("code")) {
				TransformCode transformCode = getTerminologyService().transform(
					toBE.getEnumExtResolverUri(), xds.getValue("code").toString(),
					toBE.getBusinessElement().getEnumValueSet());

				if (!StringUtils.isEmpty(transformCode.code)) {
					xds.replaceValue("code", transformCode.code);
					if (xds.hasfield("codeSystem")) {
						xds.replaceValue("codeSystem", transformCode.system);
					}
					if (xds.hasfield("displayName")) {

						if (!StringUtils.isEmpty((String) xds.getValue("displayName")) &&
								xds.hasfield("originalText")) {
							xds.replaceValue("originalText", (String) xds.getValue("displayName"));
						}
						xds.replaceValue("displayName", transformCode.displayName);
					}
				}
			}

		}

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

	private boolean hasTerminologyTransformation(ConversionRule conversionRule,
			MDMIBusinessElementReference businessElement) {

		if (conversionRule != null && businessElement != null) {
			return !StringUtils.isEmpty(conversionRule.getEnumExtResolverUri()) &&
					!StringUtils.isEmpty(businessElement.getEnumValueSet());
		}

		return false;
	}

	void execMapFromMDMI(XValue v, XElementValue trg, ConversionRule toSE) {

		if (v != null) {

			if (hasTerminologyTransformation(toSE, toSE.getBusinessElement())) {
				logger.trace("Looking to transform " + v.getValue());
				XDataStruct xds = (XDataStruct) v.getValue();
				if (xds.hasfield("code") && xds.getValue("code") != null &&
						!StringUtils.isEmpty(xds.getValue("code").toString())) {

					TransformCode transformCode = getTerminologyService().transform(
						toSE.getBusinessElement().getEnumValueSet(), xds.getValue("code").toString(),
						toSE.getEnumExtResolverUri());

					if (!StringUtils.isEmpty(transformCode.code)) {
						xds.replaceValue("code", transformCode.code);
						if (xds.hasfield("codeSystem")) {
							xds.replaceValue("codeSystem", transformCode.system);
						}
						if (xds.hasfield("displayName")) {

							if (!StringUtils.isEmpty((String) xds.getValue("displayName")) &&
									xds.hasfield("originalText")) {
								xds.replaceValue("originalText", (String) xds.getValue("displayName"));
							}
							xds.replaceValue("displayName", transformCode.displayName);
						}
						// xds.replaceValue("system", transformCode.system);
						// xds.replaceValue("display", transformCode.displayName);
					}
				}

			}

		}

		if (!hasTrgRule(toSE)) {
			cloneValue(v, trg.getXValue(), false, null, toSE);
		} else if (toSE.getRule().startsWith("DATATYPEMAP:")) {

			if (datamapInterpreter == null) {
				throw new RuntimeException("execMapFromMDMI datamapInterpreter is null");
				// datamapInterpreter = new DatamapInterpreter(toSE.getOwner().getElementSet().getModel().getGroup());
			}

			if (trg.getXValue().getValues().size() == 0) {
				if ((trg.getXValue().getDatatype() instanceof DTCStructured)) {
					XDataStruct xs = new XDataStruct(trg.getXValue());
					trg.getXValue().addValue(xs);
				}

			}

			Object source = null;

			if (v.getDatatype().isSimple()) {
				source = v;
			} else {
				source = v.getValue();
			}

			Object target = null;
			if (trg.getXValue().getDatatype().isSimple()) {
				target = trg;
			} else {
				target = trg.value();
			}
			boolean executed = datamapInterpreter.execute(
				parseFunctionName(toSE.getRule()), source, target, targetProperties);

			if (!executed) {

				logger.error(
					"Unable To Execute " + parseFunctionName(toSE.getRule()) + " at " +
							getFullPathForNode(toSE.getOwner().getSyntaxNode()) + " for " +
							toSE.getBusinessElement().getName());

				// logger.error("DATATYPE MISMATCH FOR BUSINESS ELEMENT " + toSE.getBusinessElement().getName());
			}

		} else {
			if (trg.getXValue().getValues().size() == 0 && (trg.getXValue().getDatatype() instanceof DTCStructured)) {
				XDataStruct xs = new XDataStruct(trg.getXValue());
				trg.getXValue().addValue(xs);
			}
			// System.err.println("EXECUTE EXECMAPFROMMDMI" + toSE.getRule());
			// IExpressionInterpreter adapter = Mdmi.getInterpreter(toSE, trg, toSE.getName(), v);
			// adapter.evalAction(trg, toSE.getRule(), targetProperties);
		}
	}

	private void cloneStruct(XDataStruct src, XDataStruct trg, boolean fromSrc) {
		if (src == null || trg == null) {
			throw new IllegalArgumentException("Null argument!");
		}
		Collection<XValue> values = trg.getXValues();
		for (XValue t : values) {
			XValue s = src.getXValue(t.getName());
			if (s != null) {
				cloneValue(s, t, fromSrc);
			}
		}
	}

	private void cloneChoice(XDataChoice src, XDataChoice trg, boolean fromSrc) {
		XValue s = src.getXValue();
		String fieldName = s.getName();
		XValue t = trg.setXValue(fieldName);
		cloneValue(s, t, fromSrc);
	}

	private void cloneValue(XValue src, XValue trg, boolean fromSrc) {
		cloneValue(src, trg, fromSrc, null, null);
	}

	private String getpath(Node n) {

		if (n == null) {
			return "null";
		}
		Stack<String> path = new Stack<String>();

		if (n != null) {
			path.push(n.getLocation());
		}
		Node next = n.getParentNode();
		while (next != null) {
			path.push(next.getLocation());
			next = next.getParentNode();
		}

		StringBuilder sb = new StringBuilder();
		while (!path.isEmpty()) {
			sb.append(path.pop()).append("/");
		}

		return sb.toString();

	}

	private void cloneValue(XValue src, XValue trg, boolean fromSrc, ConversionRule toMDMI, ConversionRule fromMDMI) {

		if (src.getDatatype() == null) {
			logger.error(src.getName() + "  Datatype is NULL!");
			return;
		}

		if (trg.getDatatype() == null) {
			logger.error(trg.getName() + "  Datatype is NULL!");
			return;
		}

		// fromMDMI.getOwner().getSyntaxNode()
		if (src.getDatatype() == null || src.getDatatype().getName() == null || trg.getDatatype() == null) {
			// // System.out.println(src.getDatatype());
			// // System.out.println(src.getDatatype().getName());
			// // System.out.println(trg.getDatatype());
			// // System.out.println("asdfasdf");
		}

		if (!(src.getDatatype().getName().equals(trg.getDatatype().getName()))) {
			logger.error(
				"No Conversion rule found and the datatypes need to be the same to clone, \"" +
						src.getDatatype().getName() + " to " + trg.getDatatype().getName() + "\" at Semantic " +
						(toMDMI != null
								? toMDMI.getOwner().getName()
								: (fromMDMI != null
										? fromMDMI.getOwner().getName()
										: "UNKNOWN")) +
						" located " + getpath(
							(toMDMI != null
									? toMDMI.getOwner().getSyntaxNode()
									: (fromMDMI != null
											? fromMDMI.getOwner().getSyntaxNode()
											: null))));
			return;
		}

		List<Object> values = src.getValues();
		if (values.size() <= 0) {
			return;
		}

		if (usesVSR(fromSrc, toMDMI, fromMDMI)) {
			vsConvertWithResolver(src, trg, fromSrc, toMDMI, fromMDMI);
			return;
		} else if (usesVS(fromSrc, toMDMI, fromMDMI)) {
			vsConvert(src, trg, fromSrc, toMDMI, fromMDMI);
			return;
		} else if (src.getDatatype().isStruct()) {
			for (int i = 0; i < values.size(); i++) {
				XDataStruct srcXD = (XDataStruct) values.get(i);
				try {
					XDataStruct trgXD = new XDataStruct(trg);
					trg.setValue(trgXD);
					cloneStruct(srcXD, trgXD, fromSrc);
				} catch (java.lang.ClassCastException cce) {

					logger.debug(
						"NEED CONVERSION FOR " + src.getDatatype().getName() + " -----to----- " +
								trg.getDatatype().getName());
					logger.error(cce.getMessage());
				}

			}
		} else if (src.getDatatype().isChoice()) {
			for (int i = 0; i < values.size(); i++) {
				XDataChoice srcXD = (XDataChoice) values.get(i);
				XDataChoice trgXD = new XDataChoice(trg);
				trg.setValue(trgXD);
				cloneChoice(srcXD, trgXD, fromSrc);
			}
		} else { // simple
			if (src.getDatatype().isPrimitive() || src.getDatatype().isDerived() || src.getDatatype().isExternal()) {
				trg.cloneValues(src);
			} else {
				DTSEnumerated edt = (DTSEnumerated) trg.getDatatype();
				trg.clear();
				for (int i = 0; i < values.size(); i++) {
					EnumerationLiteral srcEL = (EnumerationLiteral) values.get(i);
					if (srcEL != null) {
						EnumerationLiteral trgEL = edt.getLiteralByCode(srcEL.getCode());
						trg.setValue(trgEL, -1);
					}
				}
			}
		}
	}

	private void vsConvertWithResolver(XValue src, XValue trg, boolean fromSrc, ConversionRule toBE,
			ConversionRule toSE) {
		// MdmiExternalResolvers rs = Mdmi.INSTANCE().getExternalResolvers();
		DTExternal dt = null;
		Field srcField = null;
		Field trgField = null;
		if (fromSrc) {
			SemanticElement se = toBE.getOwner(); // source
			MDMIBusinessElementReference ber = toBE.getBusinessElement(); // target
			DTCStructured srcDT = (DTCStructured) se.getDatatype();
			srcField = srcDT.getField(se.getEnumValueField());
			dt = (DTExternal) srcField.getDatatype();
			DTCStructured trgDT = (DTCStructured) ber.getReferenceDatatype();
			trgField = trgDT.getField(ber.getEnumValueField());
		} else {
			SemanticElement se = toSE.getOwner(); // target
			MDMIBusinessElementReference ber = toSE.getBusinessElement(); // source
			DTCStructured srcDT = (DTCStructured) ber.getReferenceDatatype();
			srcField = srcDT.getField(ber.getEnumValueField());
			dt = (DTExternal) srcField.getDatatype();
			DTCStructured trgDT = (DTCStructured) se.getDatatype();
			trgField = trgDT.getField(se.getEnumValueField());
		}
		List<Object> values = src.getValues();
		for (int i = 0; i < values.size(); i++) {
			XDataStruct srcXD = (XDataStruct) values.get(i);
			XDataStruct trgXD = new XDataStruct(trg);
			trg.setValue(trgXD);
			XValue sfv = srcXD.getXValue(srcField);
			XValue tfv = trgXD.getXValue(trgField);
			// if (fromSrc) {
			// Object o = rs.getDictionaryValue(dt, sfv.getValue().toString());
			// tfv.addValue(o);
			// } else {
			// Object o = rs.getModelValue(dt, sfv.getValue().toString());
			// tfv.addValue(o);
			// }
		}
	}

	private boolean vsConvert(XValue src, XValue trg, boolean fromSrc, ConversionRule toBE, ConversionRule toSE) {
		MdmiValueSetsHandler handler = null;
		String vsMapName = null;
		MDMIDatatype srcDT = null;
		MDMIDatatype trgDT = null;
		Field srcField = null;
		Field trgField = null;
		Field trgFieldDescr = null;
		if (fromSrc) {
			SemanticElement se = toBE.getOwner(); // source
			MDMIBusinessElementReference ber = toBE.getBusinessElement(); // target
			handler = Mdmi.INSTANCE().getResolver().getValueSetsHandler(
				se.getElementSet().getModel().getGroup().getName(), "");
			vsMapName = MdmiValueSetMap.getMapName(se.getEnumValueSet(), ber.getEnumValueSet());
			srcDT = se.getDatatype();
			trgDT = ber.getReferenceDatatype();
			if (srcDT instanceof DTCStructured) {
				DTCStructured xdt = (DTCStructured) se.getDatatype();
				srcField = xdt.getField(se.getEnumValueField());
			}
			if (trgDT instanceof DTCStructured) {
				DTCStructured xdt = (DTCStructured) ber.getReferenceDatatype();
				trgField = xdt.getField(ber.getEnumValueField());
				if (null != ber.getEnumValueDescrField()) {
					trgFieldDescr = xdt.getField(ber.getEnumValueDescrField());
				}
			}
		} else {
			SemanticElement se = toSE.getOwner(); // target
			MDMIBusinessElementReference ber = toSE.getBusinessElement(); // source
			handler = Mdmi.INSTANCE().getResolver().getValueSetsHandler(
				se.getElementSet().getModel().getGroup().getName(), "");
			vsMapName = MdmiValueSetMap.getMapName(ber.getEnumValueSet(), se.getEnumValueSet());
			srcDT = ber.getReferenceDatatype();
			trgDT = se.getDatatype();
			if (srcDT instanceof DTCStructured) {
				DTCStructured xdt = (DTCStructured) ber.getReferenceDatatype();
				srcField = xdt.getField(ber.getEnumValueField());
			}
			if (trgDT instanceof DTCStructured) {
				DTCStructured xdt = (DTCStructured) se.getDatatype();
				trgField = xdt.getField(se.getEnumValueField());
				if (null != se.getEnumValueDescrField()) {
					trgFieldDescr = xdt.getField(se.getEnumValueDescrField());
				}
			}
		}
		if (null == handler) {
			return false;
		}
		MdmiValueSetMap vsMap = handler.getValueSetMap(vsMapName);
		if (null == vsMap) {
			return false;
		}

		List<Object> values = src.getValues();
		for (int i = 0; i < values.size(); i++) {
			Object source = values.get(i); // XDataStruct or EnumerationLiteral

			if (null == srcField && null == trgField) {
				// 1. Enum - > Enum
				EnumerationLiteral srcEL = (EnumerationLiteral) source;
				MdmiValueSetMap.Mapping map = vsMap.getMappingBySource(srcEL.getName());
				if (null != map) {
					MdmiValueSet.Value target = map.getTarget();
					DTSEnumerated srcDTSE = (DTSEnumerated) srcDT;
					trg.setValue(srcDTSE.getLiteralByName(target.getCode()));
				}
			} else if (null != srcField && null == trgField) {
				// 2. VS - > Enum
				XDataStruct srcXD = (XDataStruct) values.get(i);
				XValue sfv = srcXD.getXValue(srcField);
				MdmiValueSetMap.Mapping map = vsMap.getMappingBySource((String) sfv.getValue());
				if (null != map) {
					MdmiValueSet.Value target = map.getTarget();
					DTSEnumerated srcDTSE = (DTSEnumerated) srcDT;
					trg.setValue(srcDTSE.getLiteralByName(target.getCode()));
				}
			} else if (null == srcField && null != trgField) {
				// 3. Enum -> VS
				EnumerationLiteral srcEL = (EnumerationLiteral) source;
				XDataStruct trgXD = new XDataStruct(trg);
				trg.setValue(trgXD);
				XValue tfv = trgXD.getXValue(trgField);
				XValue tfvd = null;
				if (null != trgFieldDescr) {
					tfvd = trgXD.getXValue(trgFieldDescr);
				}
				MdmiValueSetMap.Mapping map = vsMap.getMappingBySource(srcEL.getName());
				if (null != map) {
					MdmiValueSet.Value target = map.getTarget();
					tfv.setValue(target.getCode());
					if (null != tfvd) {
						tfvd.setValue(target.getDescription());
					}
				}
			} else {
				// 4. VS -> VS
				XDataStruct srcXD = (XDataStruct) values.get(i);
				XDataStruct trgXD = new XDataStruct(trg);
				trg.setValue(trgXD);
				XValue sfv = srcXD.getXValue(srcField);
				XValue tfv = trgXD.getXValue(trgField);
				XValue tfvd = null;
				if (null != trgFieldDescr) {
					tfvd = trgXD.getXValue(trgFieldDescr);
				}
				MdmiValueSetMap.Mapping map = vsMap.getMappingBySource((String) sfv.getValue());
				if (null != map) {
					MdmiValueSet.Value target = map.getTarget();
					tfv.setValue(target.getCode());
					if (null != tfvd) {
						tfvd.setValue(target.getDescription());
					}
				}
			}
		}
		return true;
	}

	public void start(boolean logging) {
		this.logging = logging;
		if (this.logging) {
			try (FileOutputStream jsonFop = new FileOutputStream(new File("./logs/Conversion.json"))) {
				jsonFop.write("[".getBytes());
			} catch (FileNotFoundException e) {

			} catch (IOException e) {

			}
			first = true;
		}
	}

	public void end() {
		if (this.logging) {
			try {
				jsonFop.write("]".getBytes());
				jsonFop.flush();
				jsonFop.close();
				jsonFop = null;
			} catch (IOException e) {

			}
		}
	}

	boolean usesVS(boolean fromSrc, ConversionRule toBE, ConversionRule toSE) {
		// if (null == toBE && null == toSE) {
		// return false;
		// }
		// if (fromSrc) {
		// // toBE is not null, toSE is null, executing conversion from SE -> BER
		// String erUri = toBE.getEnumExtResolverUri();
		// if (null != erUri && 0 < erUri.length()) {
		// return true;
		// }
		// MDMIBusinessElementReference ber = toBE.getBusinessElement();
		// SemanticElement se = toBE.getOwner();
		// if (ber.usesValueSet() && se.usesValueSet()) {
		// return true;
		// }
		// } else {
		// // toBE is null, toSE is not null, executing conversion from BER -> SE
		// String erUri = toSE.getEnumExtResolverUri();
		// if (null != erUri && 0 < erUri.length()) {
		// return true;
		// }
		// MDMIBusinessElementReference ber = toSE.getBusinessElement();
		// SemanticElement se = toSE.getOwner();
		// if (ber.usesValueSet() && se.usesValueSet()) {
		// return true;
		// }
		// }
		return false;
	}

	boolean usesVSR(boolean fromSrc, ConversionRule toBE, ConversionRule toSE) {
		// if (null == toBE && null == toSE) {
		// return false;
		// }
		// if (fromSrc) {
		// // toBE is not null, toSE is null, executing conversion from SE -> BER
		// String erUri = toBE.getEnumExtResolverUri();
		// if (null != erUri && 0 < erUri.length()) {
		// return true;
		// }
		// } else {
		// // toBE is null, toSE is not null, executing conversion from BER -> SE
		// String erUri = toSE.getEnumExtResolverUri();
		// if (null != erUri && 0 < erUri.length()) {
		// return true;
		// }
		// }
		return false;
	}
} // ConversionImpl
