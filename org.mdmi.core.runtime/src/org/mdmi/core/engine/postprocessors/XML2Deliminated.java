package org.mdmi.core.engine.postprocessors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.mdmi.Bag;
import org.mdmi.MessageModel;
import org.mdmi.Node;
import org.mdmi.core.MdmiMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

//java.lang.NoClassDefFoundError: org/apache/xml/serializer/ExtendedContentHandler

public class XML2Deliminated implements IPostProcessor {

	private static Logger logger = LoggerFactory.getLogger(XML2Deliminated.class);

	private String name;

	private String delim;

	private boolean useQuotes = true;

	/**
	 * @param name
	 * @param delim
	 */
	public XML2Deliminated(String name, String delim) {
		super();
		this.name = name;
		this.delim = delim;
		if (useQuotes) {
			this.delim = ",_tick__tick_,";
		}
	}

	@Override
	public String getName() {
		return name;
	}

	protected String getXSLT() {
		return XSLT;
	};

	static final String XSLT = "<?xml version=\"1.0\"?>" +
			"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:fo=\"http://www.w3.org/1999/XSL/Format\" >" +
			"<xsl:output method=\"text\" omit-xml-declaration=\"yes\" indent=\"no\"/>" + "<xsl:template match=\"/\">" +
			"HEADERS" + "<xsl:for-each select=\"//ROOT\">" + "<xsl:value-of select=\"CONCAT\"/>" + "</xsl:for-each>" +
			"</xsl:template>" + "</xsl:stylesheet>";

	// concat(Host_Name,',',IP_address,',',OS,Load_avg_1min,',',Load_avg_5min,',',Load_avg_15min,'&#xA;')

	private static String encodeSpaces(String source) {
		if (source != null) {
			return source.replaceAll(" ", "_");
		} else {
			return "";
		}

	}

	@Override
	public void processMessage(MessageModel messageModel, MdmiMessage message) {

		String root = "";
		StringBuffer headers = new StringBuffer();
		StringBuffer concat = new StringBuffer();

		try {

			if (messageModel.getSyntaxModel() != null && messageModel.getSyntaxModel().getRoot() instanceof Bag) {
				Bag rootBag = (Bag) messageModel.getSyntaxModel().getRoot();

				if (!rootBag.getNodes().isEmpty()) {
					root = encodeSpaces(rootBag.getLocation()) + "/" +
							encodeSpaces(rootBag.getNodes().get(0).getLocation());
					boolean isFirst = true;
					boolean isLast = false;
					Bag bucket = (Bag) rootBag.getNodes().get(0);
					int i = 0;

					// ;
					ArrayList<String> columns = new ArrayList<>();
					for (Node node : bucket.getNodes()) {
						columns.add(encodeSpaces(node.getLocation()));
					}

					headers.append(columns.stream().collect(Collectors.joining(",")));

					concat.append("concat(").append(
						columns.stream().collect(Collectors.joining(",'__tick_comma_tick__',"))).append(")");
					;
					// String result = ;

					// // System.out.println(result);
					// for (Node node : bucket.getNodes()) {
					// // System.out.println(i + "==" + bucket.getNodes().size());
					// isFirst = (i == 0);
					// if (i++ == bucket.getNodes().size() - 1) {
					// isLast = true;
					// }
					//
					// if (isFirst) {
					// if (useQuotes) {
					// // concat.append("'_tick_,',");
					// }
					// }
					//
					// if (isLast) {
					// if (useQuotes) {
					// // concat.append(",',_tick_'");
					// // concat.append(",'" + delim + "',");
					// // concat.append("_tick_");
					// }
					// }
					//
					// if (!isFirst && !isLast) {
					// headers.append(",");
					// // concat.append(",'" + delim + "',");
					// }
					//
					// // if (!isFirst && !isLast) {
					// // headers.append(",");
					// // concat.append(",'" + delim + "',");
					// // } else {
					// // if (useQuotes) {
					// // concat.append("_tick_");
					// // }
					// //
					// // }
					//
					// headers.append(encodeSpaces(node.getLocation()));
					// // concat.append(encodeSpaces(node.getLocation()));
					// }
				}

			}

			// concat.append(",'&#xA;')");
			// headers.append("&#xA;");

			/**
			 * @TODO Fix this hack
			 *       Need to replace all the whitespace - but this mucks up version and encoding
			 *       Also replacing delims from message to prevent issues with the parsing
			 *
			 */
			message.setData(
				message.getDataAsString().replaceAll("\\s+", "").replace("xmlversion", "xml version").replace(
					"encoding", " encoding").replaceAll("\\|", " ").replaceAll(",", " "));

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			// factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			// System.out.println(message.getDataAsString());
			ByteArrayInputStream xml = new ByteArrayInputStream(message.getData());
			Document document = builder.parse(xml);
			String xslt = getXSLT().replace("ROOT", root).replace("HEADERS", headers).replace("CONCAT", concat);
			logger.trace(xslt);
			// System.out.println(xslt);
			ByteArrayInputStream bais = new ByteArrayInputStream(xslt.getBytes());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamSource stylesource = new StreamSource(bais);
			Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
			Source source = new DOMSource(document);
			Result outputTarget = new StreamResult(baos);
			transformer.transform(source, outputTarget);
			message.setData(baos.toByteArray());

			if (useQuotes) {
				message.setData(message.getDataAsString().replaceAll("__tick_comma_tick__", "','"));
			}
			// message.setData(message.getDataAsString());

			// System.out.println(message.getDataAsString());

		} catch (Throwable e) {

		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.IPostProcessor#canProcess(org.mdmi.MessageModel)
	 */
	@Override
	public boolean canProcess(MessageModel messageModel) {
		if (messageModel != null && messageModel.getGroup() != null) {
			if (name.equals(messageModel.getGroup().getDefaultFormatExpressionLanguage())) {
				return true;
			}
		}
		return false;
	}
}
