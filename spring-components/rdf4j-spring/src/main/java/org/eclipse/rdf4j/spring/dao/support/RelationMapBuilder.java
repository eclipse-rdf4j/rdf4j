/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support;

import static org.eclipse.rdf4j.spring.util.QueryResultUtils.getIRI;
import static org.eclipse.rdf4j.spring.util.QueryResultUtils.getIRIOptional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.BindingsBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.TupleQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class RelationMapBuilder {
	public static final ExtendedVariable _relSubject = new ExtendedVariable("rel_subject");
	public static final ExtendedVariable _relObject = new ExtendedVariable("rel_object");
	private static final ExtendedVariable _relKey = new ExtendedVariable("rel_key");
	private static final ExtendedVariable _relValue = new ExtendedVariable("rel_value");
	private static final IRI NOTHING = SimpleValueFactory.getInstance()
			.createIRI("urn:java:relationDaoSupport:Nothing");
	private RdfPredicate predicate;
	private GraphPattern[] constraints = new GraphPattern[0];
	private RDF4JTemplate rdf4JTemplate;
	private boolean isRelationOptional = false;
	private boolean isSubjectKeyed = true;
	private BindingsBuilder bindingsBuilder = new BindingsBuilder();

	public RelationMapBuilder(RDF4JTemplate rdf4JTemplate, RdfPredicate predicate) {
		this.rdf4JTemplate = rdf4JTemplate;
		this.predicate = predicate;
	}

	/**
	 * Constrains the result iff the {@link GraphPattern} contains the variables {@link RelationMapBuilder#_relSubject}
	 * and/or {@link RelationMapBuilder#_relObject}, which are the variables in the triple with the {@link RdfPredicate}
	 * specified in the constructor.
	 */
	public RelationMapBuilder constraints(GraphPattern... constraints) {
		this.constraints = constraints;
		return this;
	}

	/**
	 * Indicates that the existence of the triple is not required, allowing to use the constraints to select certain
	 * subjects and to answer the mapping to an empty Set in the {@link RelationMapBuilder#buildOneToMany()} case and
	 * {@link RelationMapBuilder#NOTHING} in the {@link RelationMapBuilder#buildOneToOne()} case.
	 *
	 * @return the builder
	 */
	public RelationMapBuilder relationIsOptional() {
		this.isRelationOptional = true;
		return this;
	}

	/**
	 * Indicates that the builder should use the triple's object for the key in the resulting {@link Map} instead of the
	 * subject (the default).
	 *
	 */
	public RelationMapBuilder useRelationObjectAsKey() {
		this.isSubjectKeyed = false;
		return this;
	}

	/**
	 * Builds a One-to-One Map using the configuration of this builder. Throws an Exception if more than one values are
	 * found for a given key. If {@link #isRelationOptional} is <code>true
	 * </code> and no triple is found for the key, {@link #NOTHING} is set as the value.
	 *
	 */
	public Map<IRI, IRI> buildOneToOne() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toMap(b -> getIRI(b, _relKey), this::getRelationValueOrNothing);
	}

	/**
	 * Builds a One-to-Many Map using the configuration of this builder.
	 *
	 */
	public Map<IRI, Set<IRI>> buildOneToMany() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.mapAndCollect(
						Function.identity(),
						Collectors.toMap(
								b -> getIRI(b, _relKey),
								b -> getIRIOptional(b, _relValue)
										.map(Set::of)
										.orElseGet(Set::of),
								RelationMapBuilder::mergeSets));
	}

	private static <T> Set<T> mergeSets(Set<T> left, Set<T> right) {
		Set<T> merged = new HashSet<>(left);
		merged.addAll(right);
		return merged;
	}

	private IRI getRelationValue(BindingSet b) {
		if (isRelationOptional) {
			return getIRIOptional(b, _relValue).orElse(NOTHING);
		} else {
			return getIRI(b, _relValue);
		}
	}

	private IRI getRelationValueOrNothing(BindingSet b) {
		if (isRelationOptional) {
			return getIRIOptional(b, _relValue).orElse(NOTHING);
		} else {
			return getIRI(b, _relValue);
		}
	}

	private TupleQueryEvaluationBuilder makeTupleQueryBuilder() {
		return rdf4JTemplate
				.tupleQuery(
						Queries.SELECT(getProjection())
								.where(getWhereClause())
								.distinct()
								.getQueryString())
				.withBindings(bindingsBuilder.build());
	}

	private Projectable[] getProjection() {
		if (this.isSubjectKeyed) {
			return new Projectable[] {
					SparqlBuilder.as(_relSubject, _relKey), SparqlBuilder.as(_relObject, _relValue)
			};
		} else {
			return new Projectable[] {
					SparqlBuilder.as(_relSubject, _relValue), SparqlBuilder.as(_relObject, _relKey)
			};
		}
	}

	private GraphPattern[] getWhereClause() {
		TriplePattern tp = _relSubject.has(predicate, _relObject);
		if (this.isRelationOptional) {
			GraphPattern[] ret = new GraphPattern[constraints.length + 1];
			ret[0] = tp.optional();
			System.arraycopy(constraints, 0, ret, 1, constraints.length);
			return ret;
		} else {
			return new GraphPattern[] { tp.and(constraints) };
		}
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, Value value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, Value value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable key, Value value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, Value value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, IRI value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, IRI value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable key, IRI value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable key, String value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, IRI value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, String value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, String value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, String value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, Integer value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, Integer value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable key, Integer value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, Integer value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, Boolean value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, Boolean value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable key, Boolean value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, Boolean value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, Float value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, Float value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable key, Float value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, Float value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(ExtendedVariable key, Double value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBinding(String key, Double value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(ExtendedVariable var, Double value) {
		bindingsBuilder.addMaybe(var, value);
		return this;
	}

	public RelationMapBuilder withBindingMaybe(String key, Double value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}
}
