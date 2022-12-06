/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mdmi.impl;

import org.eclipse.emf.ecore.EClass;
import org.mdmi.DTSPrimitive;
import org.mdmi.MDMIPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>DTS Primitive</b></em>'.
 * <!-- end-user-doc -->
 *
 * @generated
 */
public class DTSPrimitiveImpl extends MDMIDatatypeImpl implements DTSPrimitive {
	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.impl.MDMIDatatypeImpl#isPrimitive()
	 */
	@Override
	public boolean isPrimitive() {
		return true;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DTSPrimitiveImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return MDMIPackage.Literals.DTS_PRIMITIVE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mdmi.impl.MDMIDatatypeImpl#isSimple()
	 */
	@Override
	public boolean isSimple() {
		return true;
	}

} // DTSPrimitiveImpl
