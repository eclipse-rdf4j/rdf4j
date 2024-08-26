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
	MaxCountConstraintComponent(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT, ProducesValidationResultValue.NEVER),
	MinCountConstraintComponent(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT, ProducesValidationResultValue.NEVER),

	DatatypeConstraintComponent(SHACL.DATATYPE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	NodeKindConstraintComponent(SHACL.NODE_KIND_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	ClassConstraintComponent(SHACL.CLASS_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),

	PatternConstraintComponent(SHACL.PATTERN_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	UniqueLangConstraintComponent(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT, ProducesValidationResultValue.NEVER),
	LanguageInConstraintComponent(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	MaxLengthConstraintComponent(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	MinLengthConstraintComponent(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),

	InConstraintComponent(SHACL.IN_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	HasValueConstraintComponent(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.NEVER),
	HasValueInConstraintComponent(DASH.HasValueInConstraintComponent, ProducesValidationResultValue.NEVER),
	ClosedConstraintComponent(SHACL.CLOSED_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),

	MinExclusiveConstraintComponent(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	MaxExclusiveConstraintComponent(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	MaxInclusiveConstraintComponent(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	MinInclusiveConstraintComponent(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),

	AndConstraintComponent(SHACL.AND_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	OrConstraintComponent(SHACL.OR_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	NotConstraintComponent(SHACL.NOT_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	XoneConstraintComponent(SHACL.XONE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),

	DisjointConstraintComponent(SHACL.DISJOINT_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	EqualsConstraintComponent(SHACL.EQUALS_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	LessThanConstraintComponent(SHACL.LESS_THAN_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	LessThanOrEqualsConstraintComponent(SHACL.LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT,
			ProducesValidationResultValue.ALWAYS),

	QualifiedMaxCountConstraintComponent(SHACL.QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT,
			ProducesValidationResultValue.NEVER),
	QualifiedMinCountConstraintComponent(SHACL.QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT,
			ProducesValidationResultValue.NEVER),
	NodeConstraintComponent(SHACL.NODE_CONSTRAINT_COMPONENT, ProducesValidationResultValue.ALWAYS),
	PropertyConstraintComponent(SHACL.PROPERTY_CONSTRAINT_COMPONENT, ProducesValidationResultValue.NEVER),

	SPARQLConstraintComponent(SHACL.SPARQL_CONSTRAINT_COMPONENT, ProducesValidationResultValue.SOMETIMES);

	private final IRI iri;
	private final ProducesValidationResultValue producesValidationResultValue;

	SourceConstraintComponent(IRI iri, ProducesValidationResultValue producesValidationResultValue) {
		this.iri = iri;
		this.producesValidationResultValue = producesValidationResultValue;
	}

	public IRI getIri() {
		return iri;
	}

	public boolean producesValidationResultValue() {
		return producesValidationResultValue != ProducesValidationResultValue.NEVER;
	}

	public boolean alwaysProducesValidationResultValue() {
		return producesValidationResultValue == ProducesValidationResultValue.ALWAYS;
	}

	private enum ProducesValidationResultValue {
		ALWAYS,
		NEVER,
		SOMETIMES
	}
}
