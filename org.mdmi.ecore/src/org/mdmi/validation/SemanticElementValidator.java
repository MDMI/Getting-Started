/**
 *
 * $Id$
 */
package org.mdmi.validation;

import org.eclipse.emf.common.util.EList;
import org.mdmi.DataRule;
import org.mdmi.Keyword;
import org.mdmi.MDMIDatatype;
import org.mdmi.MDMIExpression;
import org.mdmi.Node;
import org.mdmi.SemanticElement;
import org.mdmi.SemanticElementBusinessRule;
import org.mdmi.SemanticElementRelationship;
import org.mdmi.SemanticElementSet;
import org.mdmi.SimpleMessageComposite;

/**
 * A sample validator interface for {@link org.mdmi.SemanticElement}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface SemanticElementValidator {
	boolean validate();

	boolean validateName(String value);

	boolean validateDescription(String value);

	boolean validateElementType(String value);

	boolean validateDatatype(MDMIDatatype value);

	boolean validatePropertyQualifier(EList<String> value);

	boolean validateComposite(SimpleMessageComposite value);

	boolean validateElementSet(SemanticElementSet value);

	boolean validateBusinessRules(EList<SemanticElementBusinessRule> value);

	boolean validateDataRules(EList<DataRule> value);

	boolean validateRelationships(EList<SemanticElementRelationship> value);

	boolean validateMultipleInstances(boolean value);

	boolean validateOrdering(String value);

	boolean validateOrderingLanguage(String value);

	boolean validateComputedValue(MDMIExpression value);

	boolean validateComputedInValue(MDMIExpression value);

	boolean validateParent(SemanticElement value);

	boolean validateChildren(EList<SemanticElement> value);

	boolean validateSyntaxNode(Node value);

	boolean validateComputedOutValue(MDMIExpression value);

	boolean validateKeywords(EList<Keyword> value);
}
