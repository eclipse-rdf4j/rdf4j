/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public enum SourceConstraintComponent {
	MaxCountConstraintComponent(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT, ConstraintType.Cardinality),
	MinCountConstraintComponent(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT, ConstraintType.Cardinality),

	DatatypeConstraintComponent(SHACL.DATATYPE_CONSTRAINT_COMPONENT, ConstraintType.ValueType),
	LanguageInConstraintComponent(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT, ConstraintType.StringBased),

	NodeKindConstraintComponent(SHACL.NODE_KIND_CONSTRAINT_COMPONENT, ConstraintType.ValueType),
	PatternConstraintComponent(SHACL.PATTERN_CONSTRAINT_COMPONENT, ConstraintType.StringBased),

	ClassConstraintComponent(SHACL.CLASS_CONSTRAINT_COMPONENT, ConstraintType.ValueType),

	InConstraintComponent(SHACL.IN_CONSTRAINT_COMPONENT, ConstraintType.Other),
	HasValueConstraintComponent(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT, ConstraintType.Other),
	HasValueInConstraintComponent(DASH.HasValueInConstraintComponent, ConstraintType.Other),
	UniqueLangConstraintComponent(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT, ConstraintType.StringBased),

	MinExclusiveConstraintComponent(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange),
	MaxExclusiveConstraintComponent(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange),
	MaxInclusiveConstraintComponent(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange),
	MinInclusiveConstraintComponent(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange),

	MaxLengthConstraintComponent(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT, ConstraintType.StringBased),
	MinLengthConstraintComponent(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT, ConstraintType.StringBased),

	AndConstraintComponent(SHACL.AND_CONSTRAINT_COMPONENT, ConstraintType.Logical),
	OrConstraintComponent(SHACL.OR_CONSTRAINT_COMPONENT, ConstraintType.Logical),
	NotConstraintComponent(SHACL.NOT_CONSTRAINT_COMPONENT, ConstraintType.Logical);

	private final IRI iri;
	private final ConstraintType constraintType;

	SourceConstraintComponent(IRI iri, ConstraintType constraintType) {
		this.iri = iri;
		this.constraintType = constraintType;
	}

	public IRI getIri() {
		return iri;
	}

	public ConstraintType getConstraintType() {
		return constraintType;
	}

	public enum ConstraintType {
		ValueType,
		Cardinality,
		ValueRange,
		StringBased,
		PropertyPair,
		Logical,
		ShapeBased,
		Other

	}
}
