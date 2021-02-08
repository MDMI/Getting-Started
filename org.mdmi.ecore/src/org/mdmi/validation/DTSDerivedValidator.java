/**
 *
 * $Id$
 */
package org.mdmi.validation;

import org.mdmi.MDMIDatatype;

/**
 * A sample validator interface for {@link org.mdmi.DTSDerived}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface DTSDerivedValidator {
	boolean validate();

	boolean validateBaseType(MDMIDatatype value);
}
