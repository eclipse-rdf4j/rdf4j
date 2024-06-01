/*
 * *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * *****************************************************************************
 */

package org.eclipse.rdf4j.spring.dao.support.join;

import static org.eclipse.rdf4j.spring.util.QueryResultUtils.getIRI;
import static org.eclipse.rdf4j.spring.util.QueryResultUtils.getIRIOptional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.BindingsBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.TupleQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.operation.TupleQueryResultConverter;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * Encapsulates all the state required for one execution of the query and provides methods for obtaining the result in
 * different forms
 *
 * <p>
 * Obtained via {@link JoinQuery#evaluationBuilder(RDF4JTemplate)}. To use a {@link JoinQueryEvaluationBuilder}:
 *
 * <ol>
 * <li>set its bindings
 * <ul>
 * <li>use {@link #withSourceEntityId(IRI)} and {@link #withTargetEntityId(IRI)} to set either side
 * <li>use any `withBinding` method to set other variables you may have used
 * </ul>
 * <li>obtain the results by using any of the `as` methods, such as {@link #asOneToMany()}, {@link #asOneToOne()}
 * {@link #asIsPresent()}, etc.
 * </ol>
 */
public class JoinQueryEvaluationBuilder {
	private TupleQueryEvaluationBuilder tupleQueryEvaluationBuilder;
	private final BindingsBuilder bindingsBuilder = new BindingsBuilder();

	JoinQueryEvaluationBuilder(TupleQueryEvaluationBuilder tupleQueryEvaluationBuilder) {
		this.tupleQueryEvaluationBuilder = tupleQueryEvaluationBuilder;
	}

	/**
	 * Builds a List of 2-component arrays, with the source entity Id in position 0 and the target entity id in position
	 * 1. Depending on the configuration of the {@link JoinQuery}, either position may be null
	 *
	 * @return a list of (source, target) id pairs.
	 */
	public List<IRI[]> asIdPairList() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toList(
						b -> new IRI[] {
								getIRIOptional(b, JoinQuery._sourceEntity).orElse(null),
								getIRIOptional(b, JoinQuery._targetEntity).orElse(null)
						});
	}

	/** Builds a One-to-One Map using the configuration of this JoinQuery. */
	public Map<IRI, Optional<IRI>> asOneToOne() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toMap(
						b -> getIRI(b, JoinQuery._sourceEntity),
						b -> getIRIOptional(b, JoinQuery._targetEntity));
	}

	/** Builds a One-to-Many Map using the configuration of this JoinQuery. */
	public Map<IRI, Set<IRI>> asOneToMany() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.mapAndCollect(
						Function.identity(),
						Collectors.toMap(
								b -> getIRI(b, JoinQuery._sourceEntity),
								b -> getIRIOptional(b, JoinQuery._targetEntity)
										.map(Set::of)
										.orElseGet(Set::of),
								JoinQueryEvaluationBuilder::mergeSets));
	}

	/**
	 * Returns only the left column of the join, i.e. all source entity ids, as a {@link Set}
	 *
	 * @return
	 */
	public Set<IRI> asSourceEntityIdSet() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toStream(b -> getIRIOptional(b, JoinQuery._sourceEntity))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns only the right column of the join, i.e. all target entity ids, as a {@link Set}
	 *
	 * @return
	 */
	public Set<IRI> asTargetEntityIdSet() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toStream(b -> getIRIOptional(b, JoinQuery._targetEntity))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
	}

	/**
	 * Returns true if the join has no results, false otherwise.
	 *
	 * @return
	 */
	public boolean asIsEmpty() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toSingletonOfWholeResult(result -> !result.hasNext());
	}

	/**
	 * Returns true if the join has one or more results, fals otherwise.
	 *
	 * @return
	 */
	public boolean asIsPresent() {
		return makeTupleQueryBuilder()
				.evaluateAndConvert()
				.toSingletonOfWholeResult(result -> result.hasNext());
	}

	/**
	 * Returns a {@link TupleQueryResultConverter} so the client can convert the result as needed.
	 *
	 * @return
	 */
	public TupleQueryResultConverter evaluateAndConvert() {
		return makeTupleQueryBuilder().evaluateAndConvert();
	}

	private static <T> Set<T> mergeSets(Set<T> left, Set<T> right) {
		Set<T> merged = new HashSet<>(left);
		merged.addAll(right);
		return merged;
	}

	public JoinQueryEvaluationBuilder withSourceEntityId(IRI value) {
		return withBinding(JoinQuery._sourceEntity, value);
	}

	public JoinQueryEvaluationBuilder withTargetEntityId(IRI value) {
		return withBinding(JoinQuery._targetEntity, value);
	}

	public JoinQueryEvaluationBuilder withObjectBinding(IRI value) {
		return withBinding(JoinQuery._sourceEntity, value);
	}

	public JoinQueryEvaluationBuilder withSubjectBindingMaybe(IRI value) {
		return withBindingMaybe(JoinQuery._sourceEntity, value);
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, Value value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, Value value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable key, Value value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, Value value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, IRI value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, IRI value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable key, IRI value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable key, String value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, IRI value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, String value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, String value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, String value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, Integer value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, Integer value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable key, Integer value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, Integer value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, Boolean value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, Boolean value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable key, Boolean value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, Boolean value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, Float value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, Float value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable key, Float value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, Float value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(Variable key, Double value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBinding(String key, Double value) {
		bindingsBuilder.add(key, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(Variable var, Double value) {
		bindingsBuilder.addMaybe(var, value);
		return this;
	}

	public JoinQueryEvaluationBuilder withBindingMaybe(String key, Double value) {
		bindingsBuilder.addMaybe(key, value);
		return this;
	}

	private TupleQueryEvaluationBuilder makeTupleQueryBuilder() {
		return this.tupleQueryEvaluationBuilder.withBindings(bindingsBuilder.build());
	}
}
