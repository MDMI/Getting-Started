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

import org.mdmi.core.ISyntaxNode;
import org.mdmi.core.Mdmi;
import org.mdmi.core.MdmiTransferInfo;

/**
 * The entry point for the transfer engine.
 * This class wraps the pool of threads.
 *
 * @TODO Combine wiht Mdmi and MdmiUow
 * @deprecated
 */

@Deprecated
public final class MdmiEngine {
	private Mdmi m_owner;

	public MdmiEngine(Mdmi owner) {
		m_owner = owner;
	}

	public void start() {
	}

	public void stop() {

	}

	Mdmi getOwner() {
		return m_owner;
	}

	ISyntaxNode sourceSyntaxModel;

	/**
	 * @return the sourceSyntaxModel
	 */
	public ISyntaxNode getSourceSyntaxModel() {
		return sourceSyntaxModel;
	}

	/**
	 * @return the targetSyntaxModel
	 */
	public ISyntaxNode getTargetSyntaxModel() {
		return targetSyntaxModel;
	}

	ISyntaxNode targetSyntaxModel;

	public void executeTransfer(MdmiTransferInfo transferInfo) {
		MdmiUow uow = new MdmiUow(this, transferInfo);
		uow.run();
		uow.clean();
		sourceSyntaxModel = uow.getSrcSyntaxModel();
		targetSyntaxModel = uow.getTrgSyntaxModel();

	}

	/**
	 * @param sMod
	 * @param tMsg
	 * @param bers
	 * @param semanticContainer
	 * @param location
	 */
	// public void executeTransfer(MdmiModelRef sMod, MdmiMessage tMsg, ArrayList<MDMIBusinessElementReference> bers,
	// SemanticElement semanticContainer, List<SemanticElement> semanticElements, String location) {
	// MdmiUow uow = new MdmiUow(this);
	// uow.run(sMod, tMsg, bers, semanticContainer, semanticElements, location);
	// sourceSyntaxModel = uow.getSrcSyntaxModel();
	// targetSyntaxModel = uow.getTrgSyntaxModel();
	//
	// }

	// private ISyntacticParser getSyntaxProvider(MessageGroup messageGroup) {
	// for (MessageModel s : messageGroup.getModels()) {
	// return m_owner.getResolver().getSyntacticParser(messageGroup.getName(), s.getMessageModelName());
	// }
	// return null;
	//
	// }
	//
	// private ISemanticParser getSemanticProvider(MessageGroup messageGroup) {
	// for (MessageModel s : messageGroup.getModels()) {
	// return m_owner.getResolver().getSemanticParser(messageGroup.getName(), s.getMessageModelName());
	// }
	// return null;
	//
	// }

} // MdmiEngine
