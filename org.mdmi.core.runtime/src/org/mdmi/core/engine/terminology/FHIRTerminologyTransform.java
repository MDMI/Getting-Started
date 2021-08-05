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
package org.mdmi.core.engine.terminology;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.core.engine.ITerminologyTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seanmuir
 *
 */
public class FHIRTerminologyTransform implements ITerminologyTransform {

	public static class CodeHashMap extends LinkedHashMap<String, TransformCode> {

		private static final int MAX_ENTRIES = 100000;

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
		 */
		@Override
		protected boolean removeEldestEntry(Entry eldest) {
			return size() > MAX_ENTRIES;
		}
	}

	public static TransformCode BLANK = new TransformCode("", "", "");

	public static CodeHashMap codeValues = new CodeHashMap();

	public static String DEFALUTURL = "http://ec2-18-188-214-103.us-east-2.compute.amazonaws.com:8080/fhir";

	private static String fhirTerminologyURL = "http://ec2-18-188-214-103.us-east-2.compute.amazonaws.com:8080/fhir";

	private static Logger logger = LoggerFactory.getLogger(FHIRTerminologyTransform.class);

	private static String password = "";

	public static boolean processTerminology = false;

	private static String userName = "";;

	public static String createKey(String source, String code, String target) {
		return source + "_X_" + code + "_Y_" + target + "_Z_";
	}

	public static void setFHIRTerminologyURL(String url) {
		fhirTerminologyURL = url;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public static void setPassword(String password) {
		FHIRTerminologyTransform.password = password;
	}

	/**
	 * @param userName
	 *            the userName to set
	 */
	public static void setUserName(String userName) {
		FHIRTerminologyTransform.userName = userName;
	}

	/**
	*
	*/
	public FHIRTerminologyTransform() {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.engine.ITerminologyTransform#transform(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public TransformCode transform(String source, String code, String target) {
		logger.debug("Transform from " + code + " from " + source + " to " + target);

		if (processTerminology && !StringUtils.isEmpty(target) && !StringUtils.isEmpty(code) &&
				!StringUtils.isEmpty(source) && !StringUtils.isEmpty(fhirTerminologyURL)) {
			try {
				return translate(fhirTerminologyURL, source, code, target);
			} catch (Exception e) {
				// TODO Auto-generated catch block

			}
		}

		/**
		 * Unfortunate hack to support terminology without a terminology server
		 * if not processTerminology but cache has been populated - we are not using server
		 */
		if (!processTerminology && !codeValues.isEmpty()) {
			String key = createKey(source, code, target);
			if (codeValues.containsKey(key)) {
				return codeValues.get(key);
			} else {
				return BLANK;
			}

		}
		return new TransformCode(code, "", "");

	}

	private TransformCode translate(String url, String source, String code, String target) throws Exception {

		// JSONObject jsonObject = (JSONObject) parser.parse(json);

		// logger.debug("translate ");
		// try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
		// if (StringUtils.isEmpty(source)) {
		// throw new Exception("SOURCE IS NULL");
		// }
		//
		// if (StringUtils.isEmpty(target)) {
		// throw new Exception("TARGET IS NULL");
		// }
		//
		// if (StringUtils.isEmpty(code)) {
		// throw new Exception("CODE IS NULL");
		// }
		//
		// String jsonInputString = "{\r\n" + "\"resourceType\": \"Parameters\",\r\n" + "\"id\": \"example\",\r\n" +
		// "\"parameter\": [\r\n" + " {\r\n" + " \"name\": \"source\",\r\n" + " \"valueUri\": \"" +
		// source + "\"\r\n" + " },\r\n" + " {\r\n" + " \"name\": \"target\",\r\n" +
		// " \"valueUri\": \"" + target + "\"\r\n" + " },\r\n" + " {\r\n" +
		// " \"name\": \"code\",\r\n" + " \"valueString\": \"" + code + "\"\r\n" + " }\r\n" +
		// "]\r\n" + "}";
		//
		// StringEntity entity = new StringEntity(jsonInputString, ContentType.APPLICATION_JSON);
		//
		// HttpPost request = new HttpPost(fhirTerminologyURL + "/ConceptMap/$translate");
		//
		// if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(password)) {
		// String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
		// request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
		// }
		//
		// request.setEntity(entity);
		//
		// CloseableHttpResponse response = httpclient.execute(request);
		//
		// String json = EntityUtils.toString(response.getEntity(), "UTF-8");
		//
		// JSONParser parser = new JSONParser();
		// String key = createKey(source, code, target);
		// try {
		//
		// JSONArray msg = (JSONArray) jsonObject.get("parameter");
		//
		// if (msg == null) {
		// return BLANK;
		// }
		//
		// Iterator<JSONObject> iterator = msg.iterator();
		// while (iterator.hasNext()) {
		// JSONObject p = iterator.next();
		// String name = (String) p.get("name");
		//
		// switch (name) {
		// case "result":
		// Boolean result = (Boolean) p.get("valueBoolean");
		//
		// if (!result) {
		// logger.trace("Unable to transform from " + source + "::" + code + " to " + target);
		// }
		// break;
		// case "match":
		// JSONArray partArray = (JSONArray) p.get("part");
		// Iterator<JSONObject> partIterator = partArray.iterator();
		// while (partIterator.hasNext()) {
		// JSONObject part = partIterator.next();
		// String partName = (String) part.get("name");
		// if ("concept".equals(partName)) {
		// JSONObject valueCoding = (JSONObject) part.get("valueCoding");
		// String translatedCode = (String) valueCoding.get("code");
		// if (!StringUtils.isEmpty(translatedCode)) {
		// String translatedSystem = (String) valueCoding.get("system");
		// String translatedDisplay = (String) valueCoding.get("display");
		// TransformCode tc = new TransformCode(
		// translatedCode, translatedSystem, translatedDisplay);
		// codeValues.put(key, tc);
		// return tc;
		// }
		// }
		// }
		// break;
		// }
		//
		// }
		//
		// } catch (ParseException e) {
		// logger.error(e.getMessage());
		// }
		return BLANK;
		// }

	}

}
