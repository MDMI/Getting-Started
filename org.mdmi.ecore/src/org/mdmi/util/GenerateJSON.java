package org.mdmi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.mdmi.Bag;
import org.mdmi.LeafSyntaxTranslator;
import org.mdmi.MDMIPackage;
import org.mdmi.MessageGroup;
import org.mdmi.MessageModel;
import org.mdmi.MessageSyntaxModel;
import org.mdmi.Node;

public class GenerateJSON {

	public static HashMap<String, String> locations = new HashMap<>();

	public static class WalkTheModel extends MDMISwitch<Object> {

		String xPath = "";

		public WalkTheModel(String xPath) {
			super();
			this.xPath = xPath;
		}

		@Override
		public Object caseBag(Bag bag) {

			if (bag.getSemanticElement() != null) {

				locations.put(bag.getSemanticElement().getName(), xPath + "/" + bag.getLocation());

			}

			WalkTheModel wtm = new WalkTheModel(xPath + "/" + bag.getLocation());

			for (Node n : bag.getNodes()) {
				wtm.doSwitch(n);
			}
			return bag;
		}

		@Override
		public Object caseLeafSyntaxTranslator(LeafSyntaxTranslator leaf) {
			if (leaf.getSemanticElement() != null) {

				locations.put(leaf.getSemanticElement().getName(), xPath + "/" + leaf.getLocation());

			}
			return leaf;
		}

	}

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
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new MDMIResourceFactoryImpl());

		MDMIResourceImpl mdmiResourceImp = (MDMIResourceImpl) resourceSet.getResource(URI.createURI(args[0]), true);

		MessageGroup messageGroup = (MessageGroup) mdmiResourceImp.getContents().get(0);

		if (mdmiResourceImp.getContents().size() > 1 && mdmiResourceImp.getContents().get(1) instanceof MessageGroup) {
			MessageGroup messageGroup1 = ((MessageGroup) mdmiResourceImp.getContents().get(1));
			if (messageGroup1.getModels() != null && messageGroup1.getModels().size() > 0) {
				messageGroup = messageGroup1;
			}
		}

		for (MessageModel mm : messageGroup.getModels()) {
			MessageSyntaxModel msm = mm.getSyntaxModel();
			// System.out.println(msm.getRoot().toString());
			Bag rootBag = (Bag) msm.getRoot();

			WalkTheModel wtm = new WalkTheModel("");

			wtm.doSwitch(rootBag);

		}

		System.out.println(" { ");
		boolean isFirst = true;

		ArrayList<String> ss = new ArrayList<>();

		ss.addAll(locations.keySet());

		Collections.sort(ss);

		for (String key : ss) {

			if (key.contains("ALLERGY")) {

				if (!isFirst) {
					System.out.println(",");
				}
				isFirst = false;

				System.out.println("\"" + key + "\"" + ":\"" + locations.get(key) + "\"");

			}
		}
		System.out.println("  }");

		// MDMIResourceImpl resource = (MDMIResourceImpl) resourceSet.createResource(URI.createURI("Out_"+args[0]));
		// resource.setXMIVersion("2.1");
		// resource.getContents().add(messageGroup);
		//
		// Map options = new HashMap();
		// options.put(XMIResource.OPTION_USE_XMI_TYPE, Boolean.TRUE);
		// options.put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.FALSE);
		// options.put(XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
		//
		// try {
		// resource.save(options);
		// System.out.println("Model has been exported to XMI");
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// System.out.println(e1.getMessage());
		// }

	}

}
