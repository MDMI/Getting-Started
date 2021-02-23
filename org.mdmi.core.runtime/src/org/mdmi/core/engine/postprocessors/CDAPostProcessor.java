package org.mdmi.core.engine.postprocessors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.mdmi.MessageModel;
import org.mdmi.core.MdmiMessage;
import org.mdmi.core.util.XmlParser;
import org.mdmi.core.util.XmlUtil;
import org.mdmi.core.util.XmlWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.mdmi.IPostProcessor;
//import org.openhealthtools.mdht.mdmi.MdmiMessage;
//import org.openhealthtools.mdht.mdmi.MdmiModelRef;
//import org.openhealthtools.mdht.mdmi.util.XmlParser;
//import org.openhealthtools.mdht.mdmi.util.XmlUtil;
//import org.openhealthtools.mdht.mdmi.util.XmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

public class CDAPostProcessor implements IPostProcessor {

	private static Logger logger = LoggerFactory.getLogger(CDAPostProcessor.class);

	@Override
	public String getName() {
		return "CcdaPostProcessor";
	}

	static String stylesheet;

	/**
	 * @return the stylesheet
	 */
	public static String getStylesheet() {
		if (StringUtils.isEmpty(stylesheet)) {
			stylesheet = "section.xsl";
		}
		return stylesheet;
	}

	/**
	 * @param stylesheet
	 *            the stylesheet to set
	 */
	public static void setStylesheet(String resource) {
		stylesheet = resource;
	}

	private void addSectionNarrative(Element section, Document doc) {

		try {

			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			DOMImplementationLS lsImpl = (DOMImplementationLS) registry.getDOMImplementation("LS");

			LSSerializer serializer = lsImpl.createLSSerializer();

			LSOutput lsOutput = lsImpl.createLSOutput();
			lsOutput.setEncoding("UTF-8");
			Writer stringWriter = new StringWriter();
			lsOutput.setCharacterStream(stringWriter);
			serializer.write(section, lsOutput);

			String result = stringWriter.toString();

			// System.out.println(result);

			javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();

			// tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			// tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

			ByteArrayInputStream bis = new ByteArrayInputStream(result.getBytes());

			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			final ClassLoader resourceLoader = Thread.currentThread().getContextClassLoader();

			InputStream is = resourceLoader.getResourceAsStream(getStylesheet());
			if (is == null) {
				logger.error("Unable to open section.xsl stylesheet");
				return;
			}

			javax.xml.transform.Transformer transformer = tFactory.newTransformer(
				new javax.xml.transform.stream.StreamSource(is));

			transformer.transform(
				new javax.xml.transform.stream.StreamSource(bis), new javax.xml.transform.stream.StreamResult(bos));

			Element narrativeText = XmlUtil.getElement(section, "text");

			if (narrativeText == null) {
				narrativeText = doc.createElement("text");
				NodeList nl = section.getElementsByTagNameNS("*", "entry");

				if (nl.getLength() == 0) {
					narrativeText = (Element) section.insertBefore(narrativeText, null);
				} else {
					narrativeText = (Element) section.insertBefore(narrativeText, nl.item(0));
				}
			}

			// Add the result of the style sheet to the document as raw html

			DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
			// df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			// df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			DocumentBuilder documentBuilder = df.newDocumentBuilder();

			// System.out.println(bos.toString());

			// Files.write(Paths.get("resources/cda/examples/test.html"), bos.toString().getBytes());

			Document narrativeDocument = documentBuilder.parse(new InputSource(new StringReader(bos.toString())));
			Element narrativeContent = narrativeDocument.getDocumentElement();
			narrativeContent = (Element) doc.importNode(narrativeContent, true);
			narrativeText.appendChild(narrativeContent);
		} catch (Exception exception) {
			logger.error("Error Processing CDA Post Processer", exception);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.postprocessors.IPostProcessor#canProcess(org.mdmi.MessageModel)
	 */
	@Override
	public boolean canProcess(MessageModel messageModel) {

		if ("CDAR2".equals(messageModel.getGroup().getName())) {
			logger.trace("CDA Post Processing is Enabled");
			return true;
		}
		logger.trace("CDA Post Processing is not Enabled");
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.postprocessors.IPostProcessor#processMessage(org.mdmi.MessageModel, org.mdmi.core.MdmiMessage)
	 */
	@Override
	public void processMessage(MessageModel messageModel, MdmiMessage message) {

		if (message.getData() == null) {
			return;
		}

		// Parse the target message
		XmlParser p = new XmlParser();
		Document doc = p.parse(new ByteArrayInputStream(message.getDataAsString().getBytes(Charset.forName("UTF-8"))));

		if (doc != null) {
			NodeList sections = doc.getElementsByTagNameNS("*", "section");
			for (int sectionCtr = 0; sectionCtr < sections.getLength(); sectionCtr++) {
				addSectionNarrative((Element) sections.item(sectionCtr), doc);
			}
			// Serialize it to a string and put it back
			StringWriter sw = new StringWriter();
			XmlWriter w = new XmlWriter(sw);
			w.write(doc);
			logger.trace("Processing CDA Post Processer Results " + sw.toString());
			message.setData(sw.toString());
		}
	}
}
