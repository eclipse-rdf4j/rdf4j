/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * Represents a SPARQL operation that takes exactly 1 argument
 */
class UnaryOperation extends Operation<UnaryOperation> {
	UnaryOperation(UnaryOperator operator) {
		super(operator, 1);
		setOperatorName(operator.getQueryString(), false);
		setWrapperMethod(SparqlBuilderUtils::getParenthesizedString);
	}
}