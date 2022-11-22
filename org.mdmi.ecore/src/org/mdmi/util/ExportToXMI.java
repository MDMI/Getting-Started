package org.mdmi.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.mdmi.MDMIPackage;
import org.mdmi.MessageGroup;

public class ExportToXMI {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 1) {
			return;
		}

		MDMIPackage.eINSTANCE.getMessageGroup();
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(MDMIPackage.eNS_URI, MDMIPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("mdmi", new MDMIResourceFactoryImpl());

		MDMIResourceImpl mdmiResourceImp = (MDMIResourceImpl) resourceSet.getResource(URI.createURI(args[0]), true);

		MessageGroup messageGroup = (MessageGroup) mdmiResourceImp.getContents().get(0);

		if (mdmiResourceImp.getContents().size() > 1 && mdmiResourceImp.getContents().get(1) instanceof MessageGroup) {
			MessageGroup messageGroup1 = ((MessageGroup) mdmiResourceImp.getContents().get(1));
			if (messageGroup1.getModels() != null && messageGroup1.getModels().size() > 0) {
				messageGroup = messageGroup1;
			}
		}

		MDMIResourceImpl resource = (MDMIResourceImpl) resourceSet.createResource(URI.createURI("Out_" + args[0]));
		resource.setXMIVersion("2.1");
		resource.getContents().add(messageGroup);

		Map options = new HashMap();
		options.put(XMIResource.OPTION_USE_XMI_TYPE, Boolean.TRUE);
		options.put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.FALSE);
		options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);

		try {
			resource.save(options);

		} catch (IOException e1) {

		}

	}

}
