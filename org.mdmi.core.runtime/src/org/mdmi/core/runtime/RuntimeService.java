package org.mdmi.core.runtime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.ConversionRule;
import org.mdmi.DatatypeMap;
import org.mdmi.Field;
import org.mdmi.MDMIBusinessElementReference;
import org.mdmi.SemanticElement;
import org.mdmi.core.Mdmi;
import org.mdmi.core.MdmiMessage;
import org.mdmi.core.MdmiModelRef;
import org.mdmi.core.MdmiTransferInfo;
import org.mdmi.core.MdmiUtil;
import org.mdmi.core.engine.DatamapInterpreter;
import org.mdmi.core.engine.XDataStruct;
import org.mdmi.core.engine.XValue;
import org.mdmi.core.engine.semanticprocessors.LogSemantic;
import org.mdmi.core.engine.semanticprocessors.LogSemantic.DIRECTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeService {

	private static Logger logger = LoggerFactory.getLogger(RuntimeService.class);

	//
	public static String runTransformation(String srcMdl, byte[] srcData, String trgMdl, String location,
			Properties sourcePropertyValues, Properties targetPropertyValues) throws Exception {

		String retVal = null;

		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		a = trgMdl.split("\\.");
		String trgMapName = a[0];
		String trgMsgMdl = a[1];
		final ArrayList<String> filter = new ArrayList<String>();

		logger.trace("Start Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE()) " + Thread.currentThread().getName());

		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());

		logger.trace("End Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE()) " + Thread.currentThread().getName());
		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		MdmiMessage sMsg = new MdmiMessage(srcData);
		MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
		MdmiMessage tMsg = new MdmiMessage();

		logger.trace(
			"Start  MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter) " +
					Thread.currentThread().getName());
		ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter);
		logger.trace(
			"End  MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter) " + Thread.currentThread().getName());

		MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);

		ti.location = location;
		ti.useDictionary = true;

		ti.sourceProperties = sourcePropertyValues;
		ti.targetProperties = targetPropertyValues;

		long start = System.nanoTime();

		logger.trace("Start Mdmi.INSTANCE().executeTransfer(ti) " + Thread.currentThread().getName());

		Mdmi.INSTANCE().executeTransfer(ti); // do something

		logger.trace("End Mdmi.INSTANCE().executeTransfer(ti) " + Thread.currentThread().getName());

		long elapsedTime = System.nanoTime() - start;

		logger.trace("Elapsed time for transformaiton" + elapsedTime);
		retVal = tMsg.getDataAsString();

		// ti.
		ti.targetMessage = null;
		ti.sourceMessage = null;
		// ti.

		return retVal; // return the target message transformed
	}

	/**
	 *
	 * @deprecated Use the properties interface
	 * @param srcMdl
	 * @param srcData
	 * @param trgMdl
	 * @param location
	 * @param sourcePropertyValues
	 * @param targetPropertyValues
	 * @return
	 * @throws Exception
	 */
	@Deprecated
	public static String runTransformation(String srcMdl, byte[] srcData, String trgMdl, String location,
			HashMap<String, String> sourcePropertyValues, HashMap<String, String> targetPropertyValues)
			throws Exception {

		String retVal = null;

		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		a = trgMdl.split("\\.");
		String trgMapName = a[0];
		String trgMsgMdl = a[1];
		final ArrayList<String> filter = new ArrayList<String>();

		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());
		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		MdmiMessage sMsg = new MdmiMessage(srcData);
		MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
		MdmiMessage tMsg = new MdmiMessage();

		ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter);

		MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);

		ti.location = location;
		ti.useDictionary = true;

		ti.sourceProperties.putAll(sourcePropertyValues);
		ti.targetProperties.putAll(targetPropertyValues);

		long start = System.nanoTime();

		Mdmi.INSTANCE().executeTransfer(ti); // do something

		long elapsedTime = System.nanoTime() - start;

		logger.trace("Elapsed time for transformaiton" + elapsedTime);
		retVal = tMsg.getDataAsString();

		return retVal; // return the target message transformed
	}

	public static void loadMap(String srcMap, String srcMdl) throws Exception {
		Mdmi mdmi = Mdmi.INSTANCE();
		mdmi.start();

		// String retVal = null;
		// String[] a = srcMdl.split("\\.");
		// String srcMapName = a[0];
		// String srcMsgMdl = a[1];
		// a = trgMdl.split("\\.");
		// String trgMapName = a[0];
		// String trgMsgMdl = a[1];
		// final ArrayList<String> filter = new ArrayList<String>();

		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(srcMdl, srcMap));
		// Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(trgMdl, trgMap));
		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());

	}

	public static String runTransformationWithPreloadedMaps(String srcMap, String srcMdl, String srcMsg, String trgMap,
			String trgMdl, String location, HashMap<String, String> sourcePropertyValues,
			HashMap<String, String> targetPropertyValues) throws Exception {
		Mdmi mdmi = Mdmi.INSTANCE();
		mdmi.start();

		String retVal = null;
		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		a = trgMdl.split("\\.");
		String trgMapName = a[0];
		String trgMsgMdl = a[1];
		final ArrayList<String> filter = new ArrayList<String>();

		// Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(srcMdl, srcMap));
		// Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(trgMdl, trgMap));
		// Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());
		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		MdmiMessage sMsg = new MdmiMessage(new File(srcMsg));
		MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
		MdmiMessage tMsg = new MdmiMessage();

		ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter);

		MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);

		ti.location = location;
		ti.useDictionary = true;

		ti.sourceProperties.putAll(sourcePropertyValues);
		ti.targetProperties.putAll(targetPropertyValues);

		long start = System.nanoTime();
		Mdmi.INSTANCE().executeTransfer(ti);
		long elapsedTime = System.nanoTime() - start;

		logger.trace("Elapsed time for transformaiton " + TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS));

		retVal = tMsg.getDataAsString();

		// ti.
		ti.targetMessage = null;
		ti.sourceMessage = null;
		return retVal; // return the target message transformed
	}

	public static String runTransformation(String srcMap, String srcMdl, String srcMsg, String trgMap, String trgMdl,
			String location, HashMap<String, String> sourcePropertyValues, HashMap<String, String> targetPropertyValues)
			throws Exception {
		Mdmi mdmi = Mdmi.INSTANCE();
		mdmi.start();

		String retVal = null;
		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		a = trgMdl.split("\\.");
		String trgMapName = a[0];
		String trgMsgMdl = a[1];
		final ArrayList<String> filter = new ArrayList<String>();

		DIRECTION asdf;
		Mdmi.INSTANCE().getSourceSemanticModelProcessors().addSourceSemanticProcessor(new LogSemantic(DIRECTION.TO));
		Mdmi.INSTANCE().getTargetSemanticModelProcessors().addTargetSemanticProcessor(new LogSemantic(DIRECTION.FROM));

		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(srcMdl, srcMap));
		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(trgMdl, trgMap));
		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());
		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		MdmiMessage sMsg = new MdmiMessage(new File(srcMsg));
		MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
		MdmiMessage tMsg = new MdmiMessage();

		ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter);

		MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);

		ti.location = location;
		ti.useDictionary = true;

		ti.sourceProperties.putAll(sourcePropertyValues);
		ti.targetProperties.putAll(targetPropertyValues);

		long start = System.nanoTime();
		Mdmi.INSTANCE().executeTransfer(ti);
		long elapsedTime = System.nanoTime() - start;

		logger.trace("Elapsed time for transformaiton " + TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS));

		retVal = tMsg.getDataAsString();

		// ti.
		ti.targetMessage = null;
		ti.sourceMessage = null;
		return retVal; // return the target message transformed
	}

	public static void runDatatypeMappings(String srcMap, String srcMdl) throws Exception {
		Mdmi.INSTANCE().start();

		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		// final List<String> filter = new ArrayList<String>();

		// boolean isAll = true;
		// for (String element : elements) {
		// logger.trace("ELEMENT FILTER " + element);
		// if ("ALL".equalsIgnoreCase(element)) {
		// isAll = true;
		// }
		// }

		// if (!isAll) {
		// filter.addAll(Arrays.asList(elements));
		// }

		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(srcMapName, srcMap));
		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());
		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);

		DatamapInterpreter datamapInterpreter = new DatamapInterpreter(sMod.getModel().getGroup());

		for (DatatypeMap datatypemap : sMod.getModel().getGroup().getDatatypeMaps()) {

			if (datatypemap.getMessageDatatype() != null && datatypemap.getMdmiDatatype() != null &&
					datatypemap.getMessageDatatype().isStruct() && datatypemap.getMdmiDatatype().isStruct()

			) {
				// // System.out.println(datatypemap.getMessageDatatype().getName());
				// // System.out.println(datatypemap.getMdmiDatatype().getName());

				if (StringUtils.isEmpty(datatypemap.getFromMDMI())) {

					XDataStruct to = new XDataStruct(new XValue(datatypemap.getMessageDatatype()));
					XDataStruct from = new XDataStruct(new XValue(datatypemap.getMessageDatatype()));
					populateFrom(from);
					String function = "map" + datatypemap.getMdmiDatatype().getName() + "To" +
							datatypemap.getMessageDatatype().getName();
					datamapInterpreter.execute(function, from, to, null, null);
				}

				if (!StringUtils.isEmpty(datatypemap.getToMDMI())) {
					XDataStruct from = new XDataStruct(new XValue(datatypemap.getMessageDatatype()));
					populateFrom(from);
					log(from);
					XDataStruct to = new XDataStruct(new XValue(datatypemap.getMdmiDatatype()));
					String function = "map" + datatypemap.getMessageDatatype().getName() + "To" +
							datatypemap.getMdmiDatatype().getName();
					datamapInterpreter.execute(function, from, to, null, null);
					// System.out.println("FROM IS");
					log(from);
					// System.out.println("TO IS");
					log(to);
					// for (Field field : to.getDatatype().getFields()) {
					// // System.out.println(field.getName() + " : " + to.getValue(field.getName()));
					// }

				}

			}

		}

		// MdmiMessage tMsg = new MdmiMessage();

	}

	static void populateFrom(XDataStruct from) {

		if ("SAD".equals(from.getDatatype().getName())) {
			// System.out.println("aaaa");
		}

		for (Field field : from.getDatatype().getFields()) {
			if (field.getDatatype().isSimple()) {
				// System.out.println("isSimple " + field.getName() + " " + field.getDatatype().getName());
				from.setValue(field.getName(), field.getName());
				// System.out.println("populateFrom " + field.getName() + " " + from.getValue(field.getName()));
			} else {
				// System.out.println("SUB TYPE " + field.getDatatype().getName());

				XDataStruct sub = new XDataStruct(new XValue(field.getDatatype()));
				populateFrom(sub);
				from.setValue(field.getName(), sub);
			}
		}
	}

	static void log(XDataStruct xDataStruct) {
		// System.out.println(xDataStruct.getDatatype().getName());
		if ("SAD".equals(xDataStruct.getDatatype().getName())) {
			// System.out.println("aaaa");
		}
		for (Field field : xDataStruct.getDatatype().getFields()) {
			// System.out.println("field.getName() " + field.getName());

			if (field.getDatatype() == null) {
				// System.out.println(field.getName() + " NULL DATATYPE !!!");
			} else {
				if (field.getDatatype().isSimple()) {
					if (field.getMaxOccurs() == 1) {
						// System.out.println(field.getName() + " : " + xDataStruct.getValue(field.getName()));
					} else {
						//

						if (xDataStruct.getValue(field.getName()) instanceof XDataStruct) {
							XDataStruct values = (XDataStruct) xDataStruct.getValue(field.getName());

							for (XValue value : values.getXValues()) {
								// System.out.println(field.getName() + " : " + value.getValue());
							}

						} else {
							// System.out.println(xDataStruct.getValue(field.getName()));
						}

					}
				} else {
					if (field.getDatatype().isStruct()) {
						XDataStruct sub = new XDataStruct(new XValue(field.getDatatype()));
						log(sub);
					}
				}
			}
		}
	}

	public static String runTransformationISO(String srcMap, String srcMdl, String[] elements, String location)
			throws Exception {
		Mdmi.INSTANCE().start();

		String retVal = null;
		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		new ArrayList<String>();

		boolean isAll = false;
		for (String element : elements) {
			logger.trace("ELEMENT FILTER " + element);
			if ("ALL".equalsIgnoreCase(element)) {
				isAll = true;
			}
		}

		// if (!isAll) {
		// filter.addAll(Arrays.asList(elements));
		// }

		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(srcMapName, srcMap));
		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());
		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		MdmiMessage tMsg = new MdmiMessage();

		if (isAll) {

			for (SemanticElement sem : sMod.getModel().getElementSet().getSemanticElements()) {

				if (sem.getParent() == null) {

					final List<String> allfilter = new ArrayList<String>();

					walkSyntax(sem, allfilter);

					if (!allfilter.isEmpty()) {
						ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(
							sMod.getModel(), sMod.getModel(), allfilter);

						List<SemanticElement> semanticElements = new ArrayList<SemanticElement>();
						long start = System.nanoTime();
						Mdmi.INSTANCE().executeTransfer(sMod, tMsg, sem, bers, semanticElements, location);
						logger.trace("Elapsed Time FILTER " + (System.nanoTime() - start));
						retVal = tMsg.getDataAsString();

						Files.write(Paths.get(location + "/" + sem.getName() + ".xml"), retVal.getBytes());
						allfilter.clear();
						semanticElements.clear();
					}

				}
			}
		} else {
			for (SemanticElement sem : sMod.getModel().getElementSet().getSemanticElements()) {

				if (sem.getParent() == null) {
					for (SemanticElement semchild : sem.getChildren()) {
						final List<String> allfilter = new ArrayList<String>();

						// if (!isAll) {
						if (semchild.getSyntaxNode() == null ||
								(!Arrays.asList(elements).contains(semchild.getSyntaxNode().getName()))) {
							continue;
						}
						// }

						walkSyntax(semchild, allfilter);

						if (!allfilter.isEmpty()) {
							ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(
								sMod.getModel(), sMod.getModel(), allfilter);

							List<SemanticElement> semanticElements = new ArrayList<SemanticElement>();
							long start = System.nanoTime();
							Mdmi.INSTANCE().executeTransfer(sMod, tMsg, semchild, bers, semanticElements, location);
							logger.trace("Elapsed Time FILTER " + (System.nanoTime() - start));
							retVal = tMsg.getDataAsString();

							Files.write(Paths.get(location + "/" + semchild.getName() + ".xml"), retVal.getBytes());
							allfilter.clear();
							semanticElements.clear();
						}
					}
				}
				// return retVal;

			}
		}

		return retVal;

	}

	/**
	 * @param model
	 */
	// private static void getFilter(MessageModel model) {
	//
	// final List<String> filter = new ArrayList<String>();
	//
	// if (node instanceof Bag) {
	//
	// }
	//
	// }
	//
	// }

	private static void walkSyntax(SemanticElement semchild, List<String> filter) {
		for (SemanticElement child : semchild.getChildren()) {
			for (ConversionRule mapping : child.getMapToMdmi()) {
				filter.add(mapping.getBusinessElement().getUniqueIdentifier());
			}
			walkSyntax(child, filter);
		}
	}

	public static String runBatchTransformations(String srcMap, String srcMdl, String sourceFolder, String trgMap,
			String trgMdl, String location, HashMap<String, String> sourcePropertyValues,
			HashMap<String, String> targetPropertyValues) throws Exception {
		Mdmi.INSTANCE().start();

		String retVal = null;
		String[] a = srcMdl.split("\\.");
		String srcMapName = a[0];
		String srcMsgMdl = a[1];
		a = trgMdl.split("\\.");
		String trgMapName = a[0];
		String trgMsgMdl = a[1];
		final ArrayList<String> filter = new ArrayList<String>();

		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(srcMapName, srcMap));
		Mdmi.INSTANCE().putMapInfo(new Mdmi.MapInfo(trgMapName, trgMap));
		Mdmi.INSTANCE().getResolver().load(Mdmi.INSTANCE());

		File folder = new File(sourceFolder);

		MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);

		ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter);

		for (final File fileEntry : folder.listFiles()) {

			if (!fileEntry.isFile()) {
				continue;
			}

			if (!fileEntry.getName().endsWith("xml")) {
				continue;
			}

			MdmiMessage sMsg = new MdmiMessage(fileEntry);
			MdmiMessage tMsg = new MdmiMessage();

			MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);

			ti.location = location;
			ti.useDictionary = true;

			ti.sourceProperties.putAll(sourcePropertyValues);
			ti.targetProperties.putAll(targetPropertyValues);

			long start = System.nanoTime();
			Mdmi.INSTANCE().executeTransfer(ti);
			long elapsedTime = System.nanoTime() - start;
			logger.trace("FILE " + fileEntry.getName() + " : " + elapsedTime);
			// retVal = tMsg.getDataAsString();

			// if (fileEntry.isDirectory()) {
			// if (!runTransformations(
			// fileEntry, extension, sourceMap, sourceModel, targetMap, targetModel, targetMessage, validate)) {
			// return false;
			// }
			// } else if (fileEntry.getName().endsWith(extension)) {

			// logger.debug("Running " + fileEntry.getAbsolutePath());

			// HashMap<String, String> sourceProperties = new HashMap<String, String>();
			// HashMap<String, String> targetProperties = new HashMap<String, String>();
			//
			// String result = transform(
			// sourceMap, sourceModel, fileEntry.getAbsolutePath(), targetMap, targetModel, targetMessage, "",
			// sourceProperties, targetProperties);

			// if (!validate.checkResults(fileEntry, result)) {
			// return false;
			// }

			// }
		}

		// MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
		// MdmiMessage sMsg = new MdmiMessage(new File(srcMsg));
		// MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
		// MdmiMessage tMsg = new MdmiMessage();
		//
		// ArrayList<MDMIBusinessElementReference> bers = MdmiUtil.getElements(sMod.getModel(), tMod.getModel(), filter);
		//
		// MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);
		//
		// ti.location = location;
		// ti.useDictionary = true;
		//
		// ti.sourceProperties.putAll(sourcePropertyValues);
		// ti.targetProperties.putAll(targetPropertyValues);
		//
		// long start = System.nanoTime();
		// Mdmi.INSTANCE().executeTransfer(ti);
		// long elapsedTime = System.nanoTime() - start;
		// retVal = tMsg.getDataAsString();
		return retVal; // return the target message transformed
	}

}
