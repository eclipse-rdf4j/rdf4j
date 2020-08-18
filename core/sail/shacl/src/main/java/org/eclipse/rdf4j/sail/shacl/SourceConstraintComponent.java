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
	MaxCountConstraintComponent(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT, ConstraintType.Cardinality, false),
	MinCountConstraintComponent(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT, ConstraintType.Cardinality, false),

	DatatypeConstraintComponent(SHACL.DATATYPE_CONSTRAINT_COMPONENT, ConstraintType.ValueType, true),
	LanguageInConstraintComponent(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT, ConstraintType.StringBased, true),

	NodeKindConstraintComponent(SHACL.NODE_KIND_CONSTRAINT_COMPONENT, ConstraintType.ValueType, true),
	PatternConstraintComponent(SHACL.PATTERN_CONSTRAINT_COMPONENT, ConstraintType.StringBased, true),

	ClassConstraintComponent(SHACL.CLASS_CONSTRAINT_COMPONENT, ConstraintType.ValueType, true),

	InConstraintComponent(SHACL.IN_CONSTRAINT_COMPONENT, ConstraintType.Other, true),
	HasValueConstraintComponent(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT, ConstraintType.Other, false),
	HasValueInConstraintComponent(DASH.HasValueInConstraintComponent, ConstraintType.Other, false),
	UniqueLangConstraintComponent(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT, ConstraintType.StringBased, false),

	MinExclusiveConstraintComponent(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange, true),
	MaxExclusiveConstraintComponent(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange, true),
	MaxInclusiveConstraintComponent(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange, true),
	MinInclusiveConstraintComponent(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT, ConstraintType.ValueRange, true),

	MaxLengthConstraintComponent(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT, ConstraintType.StringBased, true),
	MinLengthConstraintComponent(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT, ConstraintType.StringBased, true),

	AndConstraintComponent(SHACL.AND_CONSTRAINT_COMPONENT, ConstraintType.Logical, true),
	OrConstraintComponent(SHACL.OR_CONSTRAINT_COMPONENT, ConstraintType.Logical, true),
	NotConstraintComponent(SHACL.NOT_CONSTRAINT_COMPONENT, ConstraintType.Logical, true);

	private final IRI iri;
	private final ConstraintType constraintType;
	private final boolean producesValidationResultValue;

	SourceConstraintComponent(IRI iri, ConstraintType constraintType, boolean producesValidationResultValue) {
		this.iri = iri;
		this.constraintType = constraintType;
		this.producesValidationResultValue = producesValidationResultValue;
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
		Other;

	}

	public boolean producesValidationResultValue() {
		return producesValidationResultValue;
	}
}
