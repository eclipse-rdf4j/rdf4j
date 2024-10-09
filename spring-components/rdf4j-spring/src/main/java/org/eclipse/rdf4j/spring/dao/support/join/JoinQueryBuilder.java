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

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.EmptyPropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * Builder for the {@link JoinQuery}. Allows for building the JoinQuery object directly via
 * {@link #build(RDF4JTemplate)}, and for building a lazy initizalizer via {@link #buildLazyInitializer()}.
 *
 * <p>
 * You would use the lazy initializer like so:
 *
 * <pre>
 * public class MyDao extends RDF4JDAO {
 * 	// ...
 *
 * 	private static final LazyJoinQueryInitizalizer lazyJoinQuery = JoinQueryBuilder.of(SKOS.broader)
 * 			// .. configure your join
 * 			.buildLazyInitializer();
 *
 * 	public Map<IRI, Set<IRI>> getJoinedData(IRI sourceEntityId) {
 * 		return lazyJoinQuery.get(getRdf4JTemplate())
 * 				.withSourceEntityIdBinding(sourceEntityId)
 * 				.buildOneToMany();
 * 	}
 *
 * }
 *
 * </pre>
 */
public class JoinQueryBuilder {

	private final RdfPredicate predicate;
	private GraphPattern subjectConstraints = null;
	private GraphPattern objectConstraints = null;
	private JoinType joinType = JoinType.INNER;

	private JoinQueryBuilder(Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer) {
		EmptyPropertyPathBuilder propertyPathBuilder = new EmptyPropertyPathBuilder();
		propertyPathConfigurer.accept(propertyPathBuilder);
		this.predicate = propertyPathBuilder.build();
	}

	private JoinQueryBuilder(RdfPredicate predicate) {
		this.predicate = predicate;
	}

	private JoinQueryBuilder(IRI predicate) {
		this.predicate = iri(predicate);
	}

	public static JoinQueryBuilder of(RdfPredicate rdfPredicate) {
		return new JoinQueryBuilder(rdfPredicate);
	}

	public static JoinQueryBuilder of(IRI predicate) {
		return new JoinQueryBuilder(predicate);
	}

	public static JoinQueryBuilder of(Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer) {
		return new JoinQueryBuilder(propertyPathConfigurer);
	}

	public static JoinQueryBuilder of(
			RDF4JTemplate rdf4JTemplate, PropertyPathBuilder propertyPathBuilder) {
		return new JoinQueryBuilder(() -> propertyPathBuilder.build().getQueryString());
	}

	public JoinQueryBuilder sourceEntityConstraints(
			Function<Variable, GraphPattern> constraintBuilder) {
		this.subjectConstraints = constraintBuilder.apply(JoinQuery._sourceEntity);
		return this;
	}

	public JoinQueryBuilder targetEntityConstraints(
			Function<Variable, GraphPattern> constraintBuilder) {
		this.objectConstraints = constraintBuilder.apply(JoinQuery._targetEntity);
		return this;
	}

	/**
	 * Return only results where the relation is present and subjectConstraints and objectConstraints are satisfied.
	 *
	 * @return
	 */
	public JoinQueryBuilder innerJoin() {
		this.joinType = JoinType.INNER;
		return this;
	}

	/**
	 * Return results where subjectConstraints are satisfied. The existence of the relation is optional, but
	 * objectConstraints must be satisfied where the relation exists.
	 *
	 * @return
	 */
	public JoinQueryBuilder leftOuterJoin() {
		this.joinType = JoinType.LEFT_OUTER;
		return this;
	}

	/**
	 * Return results where objectConstraints are satisfied, The existence of the relation is optional, and
	 * subjectConstraints are satisfied where the relation exists.
	 *
	 * @return
	 */
	public JoinQueryBuilder rightOuterJoin() {
		this.joinType = JoinType.RIGHT_OUTER;
		return this;
	}

	public JoinQuery build() {
		return new JoinQuery(this);
	}

	public LazyJoinQueryInitizalizer buildLazyInitializer() {
		return new LazyJoinQueryInitizalizer(this);
	}

	String makeQueryString() {
		return Queries.SELECT(getProjection()).where(getWhereClause()).distinct().getQueryString();
	}

	private Projectable[] getProjection() {
		return new Projectable[] { JoinQuery._sourceEntity, JoinQuery._targetEntity };
	}

	private GraphPattern andIfPresent(GraphPattern leftOrNull, GraphPattern rightOrNull) {
		if (rightOrNull == null) {
			return leftOrNull;
		}
		if (leftOrNull == null) {
			if (rightOrNull == null) {
				throw new UnsupportedOperationException("left or right parameter must be non-null");
			}
			return rightOrNull;
		}
		return leftOrNull.and(rightOrNull);
	}

	private GraphPattern optionalIfPresent(GraphPattern patternOrNull) {
		if (patternOrNull == null) {
			return null;
		}
		return patternOrNull.optional();
	}

	private GraphPattern getWhereClause() {
		GraphPattern relation = JoinQuery._sourceEntity.has(predicate, JoinQuery._targetEntity);
		switch (this.joinType) {
		case INNER:
			return andIfPresent(
					andIfPresent(relation, this.subjectConstraints), this.objectConstraints);
		case LEFT_OUTER:
			return andIfPresent(
					this.subjectConstraints,
					andIfPresent(relation, this.objectConstraints).optional());
		case RIGHT_OUTER:
			return andIfPresent(
					this.objectConstraints,
					andIfPresent(relation, this.subjectConstraints).optional());
		}
		throw new UnsupportedOperationException("Join type Not supported: " + this.joinType);
	}
}
