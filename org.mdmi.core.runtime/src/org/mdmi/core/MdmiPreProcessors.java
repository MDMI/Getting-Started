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

import org.mdmi.core.engine.preprocessors.IPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of pre processors registered with this runtime. Will be loaded from the config file at the start, and may
 * be updated later.
 */
public final class MdmiPreProcessors {

	private static Logger logger = LoggerFactory.getLogger(MdmiPreProcessors.class);

	private final ArrayList<IPreProcessor> m_preProcessors = new ArrayList<IPreProcessor>();

	public void addPreProcessor(IPreProcessor preProcessor) {
		if (preProcessor != null) {
			boolean add = true;
			// fail safe to make sure pre process is onyl added once
			for (IPreProcessor registered : m_preProcessors) {
				if (registered.getName().equals(preProcessor.getName())) {
					add = false;
				}

			}

			if (add) {
				logger.trace("Adding post process " + preProcessor.getName());
				m_preProcessors.add(preProcessor);
			}
		}

	}

	MdmiPreProcessors() {

	}

	/**
	 * From the list of pre-processors call all the ones that are registered to handle the source or target messages,
	 * based on the message group name and message model name.
	 *
	 * @param transferInfo
	 *            The transfer info which is just about to being processed by the runtime.
	 */
	public void preProcess(MdmiTransferInfo transferInfo) {
		if (transferInfo == null) {
			throw new IllegalArgumentException("transferInfo is null");
		}
		for (int i = 0; i < m_preProcessors.size(); i++) {
			IPreProcessor preprocessor = m_preProcessors.get(i);
			if (preprocessor.canProcess(transferInfo.sourceModel.getModel())) {
				try {
					preprocessor.processMessage(transferInfo.sourceModel.getModel(), transferInfo.sourceMessage);
				} catch (Exception ex) {
					throw new MdmiException(
						ex,
						"MdmiPreProcessor {0} throws an unexpected exception while processing source of transfer request {1}",
						preprocessor.getName(), transferInfo.toString());
				}
			}
		}
	}
} // MdmiPreProcessors
