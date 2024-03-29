/**
 *
 * $Id$
 */
package org.mdmi.validation;

import org.mdmi.SemanticElement;

/**
 * A sample validator interface for {@link org.mdmi.Keyword}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface KeywordValidator {
	boolean validate();

	boolean validateDescription(String value);

	boolean validateKeyword(String value);

	boolean validateKeywordValue(String value);

	boolean validateReference(String value);

	boolean validateOwner(SemanticElement value);
}
