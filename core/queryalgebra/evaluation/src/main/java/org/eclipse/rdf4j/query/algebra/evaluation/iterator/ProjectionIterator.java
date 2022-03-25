/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

public class ProjectionIterator extends
		ConvertingIteration<CloseableIteration<BindingSet, QueryEvaluationException>, BindingSet, BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Constants *
	 *-----------*/
	private final static BiConsumer<MutableBindingSet, BindingSet> NO_OP = (a, b) -> {
	};

	private final BiConsumer<MutableBindingSet, BindingSet> projector;

	private final Supplier<MutableBindingSet> maker;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ProjectionIterator(Projection projection,
			CloseableIteration<BindingSet, QueryEvaluationException> iter,
			BindingSet parentBindings, QueryEvaluationContext context) throws QueryEvaluationException {
		super(iter);
		ProjectionElemList projectionElemList = projection.getProjectionElemList();
		boolean isOuterProjection = determineOuterProjection(projection);
		boolean includeAllParentBindings = !isOuterProjection;

		this.projector = projectionElemList.getElements()
				.stream()
				.map(pe -> {
					String sourceName = pe.getSourceName();
					String targetName = pe.getTargetName();

					Function<BindingSet, Value> getSourceValue = context.getValue(sourceName);
					BiConsumer<Value, MutableBindingSet> setTargetValue = context.setBinding(targetName);

					return (BiConsumer<MutableBindingSet, BindingSet>) (resultBindings, sourceBindings) -> {
						Value targetValue = getSourceValue.apply(sourceBindings);
						if (!includeAllParentBindings && targetValue == null) {
							targetValue = getSourceValue.apply(parentBindings);
						}
						if (targetValue != null) {
							setTargetValue.accept(targetValue, resultBindings);
						}
					};
				})
				.reduce(BiConsumer::andThen)
				.orElse(NO_OP);

		if (includeAllParentBindings) {
			this.maker = () -> context.createBindingSet(parentBindings);
		} else {
			this.maker = context::createBindingSet;
		}
	}

	private boolean determineOuterProjection(QueryModelNode ancestor) {
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
	protected MutableBindingSet convert(BindingSet sourceBindings) throws QueryEvaluationException {
		MutableBindingSet qbs = maker.get();
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
