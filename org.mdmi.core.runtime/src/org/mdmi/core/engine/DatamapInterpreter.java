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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.ConversionRule;
import org.mdmi.DatatypeMap;
import org.mdmi.MessageGroup;
import org.mdmi.MessageModel;
import org.mdmi.SemanticElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.StaticSourceFile.SourceKind;

/**
 * @author seanmuir
 *
 */
public class DatamapInterpreter {

	private static String checkFilter = "function checkFilter(target,properties) { " + "print('checkFilter'); " +
			"if (properties.containsKey('VALUESET')) { " +
			"    if (properties.get('VALUESET').containsKey(target.value())) { " +
			"         print('checkFilter true'); " + "        return true; " + "    } " + "} " +
			"print('checkFilter false'); " + "return false " + "} ";

	private static Logger logger = LoggerFactory.getLogger(DatamapInterpreter.class);

	ScriptEngineManager manager;

	ScriptEngine engine;

	Invocable inv;

	public HashMap<String, Exception> exceptions = new HashMap<>();

	/**
	 *
	 */
	public DatamapInterpreter(MessageGroup messgaeGroup) {
		super();
		// date.js
		manager = new ScriptEngineManager();
		engine = manager.getEngineByName("JavaScript");

		StringBuffer sb = new StringBuffer();
		sb.append("importPackage(org.mdmi.core.engine.javascript);\n");
		for (DatatypeMap dm : messgaeGroup.getDatatypeMaps()) {

			if (dm.getToMDMI() != null && !dm.getToMDMI().isEmpty()) {
				sb.append(dm.getToMDMI());
			}

			if (dm.getFromMDMI() != null && !dm.getFromMDMI().isEmpty()) {
				sb.append(dm.getFromMDMI());
			}

		}

		try {
			// && logger.isTraceEnabled()
			if (logger.isTraceEnabled()) {

				logger.trace(sb.toString());
				try {
					Files.write(Paths.get("./logs/datatypemaps.js"), sb.toString().getBytes());
				} catch (IOException e) {

				}

			}

			engine.eval(sb.toString());
		} catch (ScriptException e) {
			logger.error(e.getLocalizedMessage());
		}

		inv = (Invocable) engine;
	}

	/**
	 * @param source
	 * @param target
	 */
	public DatamapInterpreter(MessageGroup source, MessageGroup target) {
		manager = new ScriptEngineManager();
		engine = manager.getEngineByName("JavaScript");

		if (engine == null) {
			throw new RuntimeException("Unable to Load Script");
		}

		/**
		 * Make sure no overlap of datatype maps - relying on name might have to rely on function name
		 *
		 */
		HashSet<String> added = new HashSet<>();

		StringBuffer sb = new StringBuffer();

		sb.append("try {\n load(\"nashorn:mozilla_compat.js\");\n} \ncatch (e) \n{ \n}\n");

		sb.append("importPackage(org.mdmi.core.engine.javascript);\n");

		for (DatatypeMap dm : source.getDatatypeMaps()) {

			added.add(dm.getName());

			if (dm.getToMDMI() != null && !dm.getToMDMI().isEmpty()) {
				sb.append(dm.getToMDMI());
			}

			if (dm.getFromMDMI() != null && !dm.getFromMDMI().isEmpty()) {
				sb.append(dm.getFromMDMI());
			}

		}

		if (!source.getName().equals(target.getName())) {

			for (DatatypeMap dm : target.getDatatypeMaps()) {

				if (!added.contains(dm.getName())) {
					if (dm.getToMDMI() != null && !dm.getToMDMI().isEmpty()) {
						sb.append(dm.getToMDMI());
					}

					if (dm.getFromMDMI() != null && !dm.getFromMDMI().isEmpty()) {
						sb.append(dm.getFromMDMI());
					}
				}

			}
		}

		for (MessageModel mm : target.getModels()) {
			for (SemanticElement se : mm.getElementSet().getSemanticElements()) {
				if (se.getRelationshipByName("QUALIFIER") != null) {
					if (!StringUtils.isEmpty(se.getRelationshipByName("QUALIFIER").getRule())) {
						sb.append(se.getRelationshipByName("QUALIFIER").getRule());
					}
				}
			}
			sb.append(checkFilter);
		}

		try {
			if (logger.isTraceEnabled()) {
				try {
					Files.write(Paths.get("./logs/datatypemaps.js"), sb.toString().getBytes());
				} catch (IOException e) {
					logger.error(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
			engine.eval(compile(sb.toString()));
		} catch (ScriptException e) {
			logger.error("INVALID JAVA SCRIPT", e.getMessage());
		}

		inv = (Invocable) engine;
	}

	// static int count = 0;

	void trace(String context, Object object) {
		logger.trace(context);
		if (object instanceof XDataStruct) {
			XDataStruct struct = (XDataStruct) object;

			logger.trace(struct.getDatatype().getName());
			for (String field : struct.getFields()) {
				logger.trace(field + " " + struct.getValue(field));
			}
		} else {

			logger.trace(object.toString());
		}

	}

	/**
	 * @param code
	 *            JavaScript source code to compile.
	 * @return The compiled version of the code.
	 */
	private static String compile(String code) {
		Compiler compiler = new Compiler();

		CompilerOptions options = new CompilerOptions();
		// Advanced mode is used here, but additional options could be set, too.
		CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

		SourceFile sf;
		// try {

		// String code2;
		// SourceKind SourceKind.STRONG;
		// SourceFile.fromCode(code, code2);
		sf = SourceFile.fromCode("mappings.js", code, SourceKind.STRONG);
		ArrayList<SourceFile> inputFiles = new ArrayList<>();
		inputFiles.add(sf);

		Result result = compiler.compile(new ArrayList<SourceFile>(), inputFiles, options);

		return compiler.toSource();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		//
		// }

		// To get the complete set of externs, the logic in
		// CompilerRunner.getDefaultExterns() should be used here.
		// JSSourceFile extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}");

		// The dummy input name "input.js" is used here so that any warnings or
		// errors will cite line numbers in terms of input.js.
		// JSSourceFile input = JSSourceFile.fromCode("input.js", code);

		// compile() returns a Result, but it is not needed here.

		// compiler.compile(extern, sf, options);

		// The compiler is responsible for generating the compiled code; it is not
		// accessible via the Result.
		// return code;
	}

	void compare(String function, Object source, Object target) {

		if (source instanceof XDataStruct) {
			if (target instanceof XDataStruct) {

				/**
				 * @TODO Add better comparison
				 */
				// int sourceValueCounter = 0;
				// int targetValueCounter = 0;
				// for (String sourceField : sourceStruct.getFields()) {
				// Object sourceValue = sourceStruct.getValue(sourceField);
				// if (sourceValue != null) {
				// sourceValueCounter++;
				// }
				// }
				//
				// for (String targetField : targetStruct.getFields()) {
				// Object targetValue = targetStruct.getValue(targetField);
				// if (targetValue != null) {
				// targetValueCounter++;
				// }
				// }
				//
				// if (sourceValueCounter != targetValueCounter) {
				// logger.trace(function + " NOT EQUAL ");
				// logger.trace(" Source Had " + sourceValueCounter);
				// logger.trace(" Target Had " + targetValueCounter);
				//
				// for (String sourceField : sourceStruct.getFields()) {
				// Object sourceValue = sourceStruct.getValue(sourceField);
				// if (sourceValue != null) {
				// logger.trace(" Source Value " + sourceField + " == " + sourceValue);
				// }
				// }
				//
				// for (String targetField : targetStruct.getFields()) {
				// Object targetValue = targetStruct.getValue(targetField);
				// if (targetValue != null) {
				// logger.trace(" Target Value " + targetField + " == " + targetValue);
				// }
				// }
				//
				// }

			}
		}

	}

	public boolean execute(String function, Object source, Object target, Properties properties,
			ConversionRule conversionRule) {

		// synchronized (inv) {
		try {
			/*
			 * @TODO Fix Editor Whitespace
			 * Editor is adding on white space chars causing issues with method invocation
			 */
			logger.trace("Executing Method " + function);
			// if (conversionRule.getOwner() != null && !conversionRule.getOwner().getPropertyQualifier().isEmpty()) {
			inv.invokeFunction(function.replaceAll("\\s+", ""), source, target, properties, conversionRule);
			// } else {
			// inv.invokeFunction(function.replaceAll("\\s+", ""), source, target, properties);
			// }

			return true;
			// compare(function, source, target);
		} catch (Exception e) {
			exceptions.put(function, e);
			logger.error("Failed executing function " + function + " := " + e.getMessage());
			// logger.error(e.getMessage(), e);
			return false;
		}
		// }
	}

	Boolean execute(String function, Object target, Properties properties) {

		// synchronized (inv) {
		try {
			logger.trace("Executing Method " + function);
			return (Boolean) inv.invokeFunction(function.replaceAll("\\s+", ""), target, properties);
		} catch (Exception e) {
			exceptions.put(function, e);
			logger.error("Failed executing Executing " + function + " := " + e.getMessage());
			logger.error(e.getMessage(), e);
			return false;
		}
		// }
	}

}
