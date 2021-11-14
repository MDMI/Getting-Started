/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mdmi.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.XMLSave;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

/**
 * <!-- begin-user-doc -->
 * The <b>Resource </b> associated with the package.
 * <!-- end-user-doc -->
 *
 * @see org.mdmi.util.MDMIResourceFactoryImpl
 * @generated
 */
public class MDMIResourceImpl extends XMIResourceImpl {
	/**
	 * Creates an instance of the resource.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param uri
	 *            the URI of the new resource.
	 * @generated
	 */
	public MDMIResourceImpl(URI uri) {
		super(uri);
	}

	@Override
	public boolean useUUIDs() {
		return true;
	}

	@Override
	protected boolean useIDs() {
		return true;
	}

	@Override
	public XMLSave createXMLSave() {
		return new MDMISaveImpl(createXMLHelper());
	}

	// @Override
	// protected XMLSave createXMLSave(Map<?, ?> options) {
	// if (options != null
	// && Boolean.TRUE.equals(options.get(OPTION_SUPPRESS_XMI))) {
	// return new XMLSaveImpl(new XMLHelperImpl(this));
	// } else {
	// return createXMLSave();
	// }
	// }

} // MDMIResourceImpl
