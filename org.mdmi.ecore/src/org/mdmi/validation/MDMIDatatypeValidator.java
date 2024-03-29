/**
 *
 * $Id$
 */
package org.mdmi.validation;

/**
 * A sample validator interface for {@link org.mdmi.MDMIDatatype}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface MDMIDatatypeValidator {
	boolean validate();

	boolean validateTypeName(String value);

	boolean validateDescription(String value);

	boolean validateReference(String value);

	boolean validateIsReadonly(boolean value);

	boolean validateTypeSpec(String value);

	boolean validateRestriction(String value);
}
