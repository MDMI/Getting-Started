/*******************************************************************************
 * Copyright (c) 2019 seanmuir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     seanmuir - initial API and implementation
 *
 *******************************************************************************/
package org.mdmi.core.engine.postprocessors;

import org.mdmi.MessageModel;
import org.mdmi.core.MdmiMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seanmuir
 *
 */
public class SQLInsertPostProcessor extends XML2Deliminated {

	@Override
	protected String getXSLT() {
		return XSLT;
	};

	static final String XSLT = "<?xml version=\"1.0\"?>" +
			"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:fo=\"http://www.w3.org/1999/XSL/Format\" >" +
			"<xsl:output method=\"text\" omit-xml-declaration=\"yes\" indent=\"no\"/>" + "<xsl:template match=\"/\">" +
			"Insert into TABLENAME (HEADERS) values ('" + "<xsl:for-each select=\"//ROOT\">" +
			"<xsl:value-of select=\"CONCAT\"/>" + "</xsl:for-each>" + "')</xsl:template>" + "</xsl:stylesheet>";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.postprocessors.XML2Deliminated#processMessage(org.mdmi.MessageModel, org.mdmi.core.MdmiMessage)
	 */
	@Override
	public void processMessage(MessageModel messageModel, MdmiMessage message) {
		super.processMessage(messageModel, message);
		message.setData(message.getDataAsString().replace("TABLENAME", messageModel.getMessageModelName()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.core.engine.postprocessors.XML2Deliminated#canProcess(org.mdmi.MessageModel)
	 */
	@Override
	public boolean canProcess(MessageModel messageModel) {
		if (messageModel != null && "sqlinsert".equals(messageModel.getGroup().getName())) {
			return true;
		}
		return false;
	}

	/**
	 * @param name
	 * @param delim
	 */
	public SQLInsertPostProcessor() {
		super("SQLINSERT", ",");
	}

	private static Logger logger = LoggerFactory.getLogger(SQLInsertPostProcessor.class);

}
