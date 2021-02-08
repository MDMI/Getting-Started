/**
 *
 * $Id$
 */
package org.mdmi.validation;

import org.mdmi.MessageGroup;
import org.mdmi.MessageSyntaxModel;
import org.mdmi.SemanticElementSet;

/**
 * A sample validator interface for {@link org.mdmi.MessageModel}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface MessageModelValidator {
	boolean validate();

	boolean validateMessageModelName(String value);

	boolean validateSyntaxModel(MessageSyntaxModel value);

	boolean validateElementSet(SemanticElementSet value);

	boolean validateDescription(String value);

	boolean validateSource(String value);

	boolean validateGroup(MessageGroup value);
}
