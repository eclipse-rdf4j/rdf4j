/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
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

public class ProjectionIterator extends ConvertingIteration<BindingSet, BindingSet> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final BiConsumer<MutableBindingSet, BindingSet> projector;

	private final Supplier<MutableBindingSet> maker;

	/*--------------*
	 * Constructors *
	 *--------------*/

	private static class BindingSetMapper {
		Function<BindingSet, Value> valueWithSourceName;
		BiConsumer<Value, MutableBindingSet> setTarget;

		public BindingSetMapper(Function<BindingSet, Value> valueWithSourceName,
				BiConsumer<Value, MutableBindingSet> setTarget) {
			this.valueWithSourceName = valueWithSourceName;
			this.setTarget = setTarget;
		}

		public Function<BindingSet, Value> getValueWithSourceName() {
			return valueWithSourceName;
		}

		public BiConsumer<Value, MutableBindingSet> getSetTarget() {
			return setTarget;
		}
	}

	public ProjectionIterator(Projection projection, CloseableIteration<BindingSet> iter,
			BindingSet parentBindings, QueryEvaluationContext context) throws QueryEvaluationException {
		super(iter);
		ProjectionElemList projectionElemList = projection.getProjectionElemList();
		boolean isOuterProjection = determineOuterProjection(projection);
		boolean includeAllParentBindings = !isOuterProjection;

		BindingSetMapper[] array = projectionElemList.getElements()
				.stream()
				.map(pe -> {
					String projectionName = pe.getProjectionAlias().orElse(pe.getName());
					return new BindingSetMapper(context.getValue(pe.getName()), context.setBinding(projectionName));
				})
				.toArray(BindingSetMapper[]::new);

		BiConsumer<MutableBindingSet, BindingSet> consumer;

		if (includeAllParentBindings) {
			consumer = (resultBindings, sourceBindings) -> {
				for (BindingSetMapper bindingSetMapper : array) {
					Value targetValue = bindingSetMapper.valueWithSourceName.apply(sourceBindings);
					if (targetValue != null) {
						bindingSetMapper.setTarget.accept(targetValue, resultBindings);
					}
				}
			};
		} else {
			consumer = (resultBindings, sourceBindings) -> {
				for (BindingSetMapper bindingSetMapper : array) {
					Value targetValue = bindingSetMapper.valueWithSourceName.apply(sourceBindings);
					if (targetValue == null) {
						targetValue = bindingSetMapper.valueWithSourceName.apply(parentBindings);
					}
					if (targetValue != null) {
						bindingSetMapper.setTarget.accept(targetValue, resultBindings);
					}
				}
			};
		}

		if (projectionElemList.getElements().isEmpty()) {
			consumer = (resultBindings, sourceBindings) -> {
				// If there are no projection elements we do nothing.
			};
		}

		if (includeAllParentBindings) {
			this.maker = () -> context.createBindingSet(parentBindings);
		} else {
			this.maker = context::createBindingSet;
		}
		this.projector = consumer;
	}

	private BiConsumer<MutableBindingSet, BindingSet> andThen(BiConsumer<MutableBindingSet, BindingSet> consumer,
			BiConsumer<MutableBindingSet, BindingSet> next) {
		if (consumer == null) {
			return next;
		} else {
			return consumer.andThen(next);
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
	protected BindingSet convert(BindingSet sourceBindings) throws QueryEvaluationException {
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
			Value targetValue = sourceBindings.getValue(pe.getName());
			if (!includeAllParentBindings && targetValue == null) {
				targetValue = parentBindings.getValue(pe.getName());
			}
			if (targetValue != null) {
				resultBindings.setBinding(pe.getProjectionAlias().orElse(pe.getName()), targetValue);
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
