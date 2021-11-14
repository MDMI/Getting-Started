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
package org.mdmi.core;

import java.util.ArrayList;

import org.mdmi.core.engine.postprocessors.IPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of post processors registered with this runtime.
 * Will be loaded from the config file at the start, and may be updated later.
 */
public final class MdmiPostProcessors {

	private static Logger logger = LoggerFactory.getLogger(MdmiPostProcessors.class);

	private final ArrayList<IPostProcessor> m_postProcessors = new ArrayList<IPostProcessor>();

	public void addPostProcessor(IPostProcessor postProcessor) {
		if (postProcessor != null) {
			boolean add = true;
			// fail safe to make sure post process is onyl added once
			for (IPostProcessor registered : m_postProcessors) {
				if (registered.getName().equals(postProcessor.getName())) {
					add = false;
				}
			}
			if (add) {
				logger.trace("Adding post process " + postProcessor.getName());
				m_postProcessors.add(postProcessor);
			}
		}

	}

	/**
	 * From the list of post-processors call all the ones that are registered to handle the source or target messages,
	 * based on the message group name and message model name.
	 *
	 * @param transferInfo
	 *            The transfer info which is just about to being processed by the runtime.
	 */
	public void postProcess(MdmiTransferInfo transferInfo) {
		if (transferInfo == null) {
			throw new IllegalArgumentException("transferInfo is null");
		}
		for (int i = 0; i < m_postProcessors.size(); i++) {
			IPostProcessor postProcessor = m_postProcessors.get(i);
			logger.trace("Checking " + postProcessor.getName());
			logger.trace("messageModel.getGroup().getName() " + transferInfo.targetModel.getGroupName());
			if (postProcessor.canProcess(transferInfo.targetModel.getModel())) {
				try {
					logger.info("Executing " + postProcessor.getName());
					postProcessor.processMessage(transferInfo.targetModel.getModel(), transferInfo.targetMessage);
				} catch (Exception ex) {

					logger.error(
						"MdmiPostProcessor {0} throws an unexpected exception while processing source of transfer request",
						ex);
				}
			}
		}
	}

} // MdmiPostProcessors
