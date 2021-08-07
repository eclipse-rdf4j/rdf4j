/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.*;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.EmptyPropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class ExtendedExpressions {

	public static Bind BIND(Assignable exp, Variable var) {
		return new Bind(exp, var);
	}

	public static Expression<?> NOT_IN(Variable var, RdfValue... options) {
		return new NotIn(var, options);
	}

	public static Expression<?> IN(Variable var, RdfValue... options) {
		return new In(var, options);
	}

	public static Expression<?> STR(Operand var) {
		return function(SparqlFunction.STR, var);
	}

	public static Expression<?> STRDT(Operand lexicalForm, Operand datatype) {
		return function(SparqlFunction.STRDT, lexicalForm, datatype);
	}

	public static Expression<?> IS_BLANK(Variable var) {
		return function(SparqlFunction.IS_BLANK, var);
	}

	public static Expression<?> DATATYPE(Variable var) {
		return function(SparqlFunction.DATATYPE, var);
	}

	public static Expression<?> IF(Operand testExp, Operand thenExp, Operand elseExp) {
		return function(SparqlFunction.IF, testExp, thenExp, elseExp);
	}

	public static RdfPredicate PathZeroOrMoreAlt(IRI... properties) {
		return new PZeroOrMore(properties);
	}

	public static RdfPredicate PathSequence(IRI... properties) {
		return new PSequence(properties);
	}

	public static SequencePath SEQ(PropertyPath left, PropertyPath right) {
		return new SequencePath(left, right);
	}

	public static PredicatePath P(Iri predicate) {
		return new PredicatePath(predicate);
	}

	public static InversePredicatePath INV(Iri predicate) {
		return new InversePredicatePath(predicate);
	}

	public static InversePath INV(PropertyPath path) {
		return new InversePath(path);
	}

	public static AlternativePath ALT(PropertyPath left, PropertyPath right) {
		return new AlternativePath(left, right);
	}

	public static ZeroOrMorePath ZOM(PropertyPath path) {
		return new ZeroOrMorePath(path);
	}

	public static OneOrMorePath OOM(PropertyPath path) {
		return new OneOrMorePath(path);
	}

	public static ZeroOrOnePath ZOO(PropertyPath path) {
		return new ZeroOrOnePath(path);
	}

	public static GroupedPath GROUP(PropertyPath path) {
		return new GroupedPath(path);
	}

	public static NegatedPropertySet NEG(PredicatePathOrInversePredicatePath... predicates) {
		return new NegatedPropertySet(predicates);
	}

	public static PropertyPathBuilder path(Iri property) {
		return new EmptyPropertyPathBuilder().pred(property);
	}

	public static EmptyPropertyPathBuilder path() {
		return new EmptyPropertyPathBuilder();
	}
}
