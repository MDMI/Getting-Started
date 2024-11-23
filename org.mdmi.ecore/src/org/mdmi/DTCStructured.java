/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mdmi;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>DTC Structured</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 * <li>{@link org.mdmi.DTCStructured#getFields <em>Fields</em>}</li>
 * </ul>
 *
 * @see org.mdmi.MDMIPackage#getDTCStructured()
 * @model
 * @generated
 */
public interface DTCStructured extends MDMIDatatype {
	/**
	 * Returns the value of the '<em><b>Fields</b></em>' containment reference list.
	 * The list contents are of type {@link org.mdmi.Field}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Fields</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Fields</em>' containment reference list.
	 * @see org.mdmi.MDMIPackage#getDTCStructured_Fields()
	 * @model containment="true"
	 * @generated
	 */
	EList<Field> getFields();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @model required="true" nameRequired="true"
	 * @generated
	 */
	Field getField(String name);

} // DTCStructured
