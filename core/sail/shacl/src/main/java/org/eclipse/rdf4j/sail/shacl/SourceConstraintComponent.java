/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public enum SourceConstraintComponent {
	MaxCountConstraintComponent(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT, false),
	MinCountConstraintComponent(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT, false),

	DatatypeConstraintComponent(SHACL.DATATYPE_CONSTRAINT_COMPONENT, true),
	NodeKindConstraintComponent(SHACL.NODE_KIND_CONSTRAINT_COMPONENT, true),
	ClassConstraintComponent(SHACL.CLASS_CONSTRAINT_COMPONENT, true),

	PatternConstraintComponent(SHACL.PATTERN_CONSTRAINT_COMPONENT, true),
	UniqueLangConstraintComponent(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT, false),
	LanguageInConstraintComponent(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT, true),
	MaxLengthConstraintComponent(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT, true),
	MinLengthConstraintComponent(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT, true),

	InConstraintComponent(SHACL.IN_CONSTRAINT_COMPONENT, true),
	HasValueConstraintComponent(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT, false),
	HasValueInConstraintComponent(DASH.HasValueInConstraintComponent, false),
	ClosedConstraintComponent(SHACL.CLOSED_CONSTRAINT_COMPONENT, true),

	MinExclusiveConstraintComponent(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT, true),
	MaxExclusiveConstraintComponent(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT, true),
	MaxInclusiveConstraintComponent(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT, true),
	MinInclusiveConstraintComponent(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT, true),

	AndConstraintComponent(SHACL.AND_CONSTRAINT_COMPONENT, true),
	OrConstraintComponent(SHACL.OR_CONSTRAINT_COMPONENT, true),
	NotConstraintComponent(SHACL.NOT_CONSTRAINT_COMPONENT, true),
	XoneConstraintComponent(SHACL.XONE_CONSTRAINT_COMPONENT, true),

	DisjointConstraintComponent(SHACL.DISJOINT_CONSTRAINT_COMPONENT, true),
	EqualsConstraintComponent(SHACL.EQUALS_CONSTRAINT_COMPONENT, true),
	LessThanConstraintComponent(SHACL.LESS_THAN_CONSTRAINT_COMPONENT, true),
	LessThanOrEqualsConstraintComponent(SHACL.LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT,
			true),

	QualifiedMaxCountConstraintComponent(SHACL.QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT,
			false),
	QualifiedMinCountConstraintComponent(SHACL.QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT,
			false),
	NodeConstraintComponent(SHACL.NODE_CONSTRAINT_COMPONENT, true),
	PropertyConstraintComponent(SHACL.PROPERTY_CONSTRAINT_COMPONENT, false),

	SPARQLConstraintComponent(SHACL.SPARQL_CONSTRAINT_COMPONENT, true);

	private final IRI iri;
	private final boolean producesValidationResultValue;

	SourceConstraintComponent(IRI iri, boolean producesValidationResultValue) {
		this.iri = iri;
		this.producesValidationResultValue = producesValidationResultValue;
	}

	public IRI getIri() {
		return iri;
	}

	public boolean producesValidationResultValue() {
		return producesValidationResultValue;
	}
}
