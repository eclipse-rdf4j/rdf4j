/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

public class ProjectionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final BiConsumer<QueryBindingSet, BindingSet> projector;

	private final Supplier<QueryBindingSet> maker;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ProjectionIterator(Projection projection, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			BindingSet parentBindings) throws QueryEvaluationException {
		super(iter);
		ProjectionElemList projectionElemList = projection.getProjectionElemList();
		boolean isOuterProjection = determineOuterProjection(projection);
		boolean includeAllParentBindings = !isOuterProjection;

		BiConsumer<QueryBindingSet, BindingSet> consumer = null;
		for (ProjectionElem pe : projectionElemList.getElements()) {
			String sourceName = pe.getSourceName();
			String targetName = pe.getTargetName();
			BiConsumer<QueryBindingSet, BindingSet> next = (resultBindings, sourceBindings) -> {
				Value targetValue = sourceBindings.getValue(sourceName);
				if (!includeAllParentBindings && targetValue == null) {
					targetValue = parentBindings.getValue(sourceName);
				}
				if (targetValue != null) {
					resultBindings.setBinding(targetName, targetValue);
				}
			};
			consumer = andThen(consumer, next);
		}
		if (projectionElemList.getElements().isEmpty()) {
			consumer = (resultBindings, sourceBindings) -> {
				// If there are no projection elements we do nothing.
			};
		}

		if (includeAllParentBindings) {
			this.maker = () -> new QueryBindingSet(parentBindings);
		} else
			this.maker = () -> new QueryBindingSet();
		this.projector = consumer;
	}

	private BiConsumer<QueryBindingSet, BindingSet> andThen(BiConsumer<QueryBindingSet, BindingSet> consumer,
			BiConsumer<QueryBindingSet, BindingSet> next) {
		if (consumer == null)
			return next;
		else
			return consumer.andThen(next);
	}

	private final boolean determineOuterProjection(QueryModelNode ancestor) {
		while (ancestor.getParentNode() != null) {
			ancestor = ancestor.getParentNode();
			if (ancestor instanceof Projection || ancestor instanceof MultiProjection) {
				return false;
			}
		}
		return true;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet convert(BindingSet sourceBindings) throws QueryEvaluationException {
		QueryBindingSet qbs = maker.get();
		projector.accept(qbs, sourceBindings);
		return qbs;
	}

	public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
			BindingSet parentBindings) {
		return project(projElemList, sourceBindings, parentBindings, false);
	}

	public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
			BindingSet parentBindings, boolean includeAllParentBindings) {
		final QueryBindingSet resultBindings = makeNewQueryBindings(parentBindings, includeAllParentBindings);

		for (ProjectionElem pe : projElemList.getElements()) {
			Value targetValue = sourceBindings.getValue(pe.getSourceName());
			if (!includeAllParentBindings && targetValue == null) {
				targetValue = parentBindings.getValue(pe.getSourceName());
			}
			if (targetValue != null) {
				resultBindings.setBinding(pe.getTargetName(), targetValue);
			}
		}

		return resultBindings;
	}

	private static QueryBindingSet makeNewQueryBindings(BindingSet parentBindings, boolean includeAllParentBindings) {
		final QueryBindingSet resultBindings = new QueryBindingSet();
		if (includeAllParentBindings) {
			resultBindings.addAll(parentBindings);
		}
		return resultBindings;
	}
}
