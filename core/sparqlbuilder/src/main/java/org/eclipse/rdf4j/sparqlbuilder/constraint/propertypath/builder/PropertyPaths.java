/*
 * *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * *****************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.*;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

abstract class PropertyPaths {
	public static SequencePath pSeq(PropertyPath left, PropertyPath right) {
		return new SequencePath(left, right);
	}

	public static PredicatePath p(Iri predicate) {
		return new PredicatePath(predicate);
	}

	public static InversePredicatePath pInv(Iri predicate) {
		return new InversePredicatePath(predicate);
	}

	public static InversePath pInv(PropertyPath path) {
		return new InversePath(path);
	}

	public static AlternativePath pAlt(PropertyPath left, PropertyPath right) {
		return new AlternativePath(left, right);
	}

	public static ZeroOrMorePath pZeroOrMore(PropertyPath path) {
		return new ZeroOrMorePath(path);
	}

	public static OneOrMorePath pOneOrMore(PropertyPath path) {
		return new OneOrMorePath(path);
	}

	public static ZeroOrOnePath pZeroOrOne(PropertyPath path) {
		return new ZeroOrOnePath(path);
	}

	public static GroupedPath pGroup(PropertyPath path) {
		return new GroupedPath(path);
	}

	public static NegatedPropertySet pNeg(PredicatePathOrInversePredicatePath... predicates) {
		return new NegatedPropertySet(predicates);
	}

	public static PropertyPathBuilder path(Iri property) {
		return new EmptyPropertyPathBuilder().pred(property);
	}

	public static EmptyPropertyPathBuilder path() {
		return new EmptyPropertyPathBuilder();
	}
}
