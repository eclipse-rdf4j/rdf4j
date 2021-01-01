/** *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ****************************************************************************** */
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
import org.eclipse.rdf4j.query.impl.ArrayBindingSet;

public class ProjectionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Constants *
	 *-----------*/
	private final Function<BindingSet, ? extends BindingSet> converter;

	/*--------------*
	 * Constructors *
	 *--------------*/
	public ProjectionIterator(Projection projection, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			BindingSet parentBindings) throws QueryEvaluationException {
		super(iter);
		ProjectionElemList pel = projection.getProjectionElemList();
		if (parentBindings.size() == 0) {
			if (pel.getElements().size() == 1) {
				Function<BindingSet, ArrayBindingSet> oneVariableConversion = convertASingleVariable(pel);
				this.converter = oneVariableConversion;
			} else {
				Function<BindingSet, ArrayBindingSet> manyVariableConversion = convertManyVariables(pel);
				this.converter = manyVariableConversion;
			}
		} else {
			this.converter = (s) -> project(pel, s, parentBindings,
					!determineOuterProjection(projection));
		}
	}

	Function<BindingSet, ArrayBindingSet> convertASingleVariable(ProjectionElemList pel) {
		ProjectionElem el = pel
				.getElements()
				.get(0);
		String targetName = el.getTargetName();
		ArrayBindingSet abs = new ArrayBindingSet(targetName);
		String sourceName = el.getSourceName();
		BiConsumer<ArrayBindingSet, Value> setter = getSetterToTarget(abs, targetName);
		Function<BindingSet, ArrayBindingSet> oneVariableConversion = sb -> {
			ArrayBindingSet abs2 = new ArrayBindingSet(targetName);
			Value targetValue = sb.getValue(sourceName);
			if (targetValue != null) {
				setter.accept(abs2, targetValue);
			}
			return abs2;
		};
		return oneVariableConversion;
	}

	BiConsumer<ArrayBindingSet, Value> getSetterToTarget(ArrayBindingSet abs, String targetName) {
		BiConsumer<ArrayBindingSet, Value> setter = abs.getDirectSetterForVariable(targetName);
		return setter;
	}

	private static boolean determineOuterProjection(Projection projection) {
		QueryModelNode ancestor = projection;
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
		return converter.apply(sourceBindings);
	}

	public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
			BindingSet parentBindings) {
		return project(projElemList, sourceBindings, parentBindings, false);
	}

	public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
			BindingSet parentBindings, boolean includeAllParentBindings) {
		final QueryBindingSet resultBindings = new QueryBindingSet();
		if (includeAllParentBindings) {
			resultBindings.addAll(parentBindings);
		}

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

	private Function<BindingSet, ArrayBindingSet> convertManyVariables(ProjectionElemList pel) {
		String[] targetNames = pel.getTargetNames().toArray(new String[0]);
		
		final int size = pel.getElements().size();
		final String[] sourcenames = new String[size];
		@SuppressWarnings("unchecked")
		final BiConsumer<ArrayBindingSet, Value>[] setters = new BiConsumer[size];
		
		getSourceToTargetSetters(pel, targetNames, sourcenames, setters);

		return sourceBindings -> {
			ArrayBindingSet abs2 = new ArrayBindingSet(targetNames);
			
			for (int j=0;j<size;j++) {
				Value targetValue = sourceBindings.getValue(sourcenames[j]);
				if (targetValue != null) {
					setters[j].accept(abs2, targetValue);
				}
			}
			return abs2;
		};
	}

	private void getSourceToTargetSetters(ProjectionElemList pel, String[] targetNames, final String[] sourcenames,
	    final BiConsumer<ArrayBindingSet, Value>[] setters)
	{
		ArrayBindingSet abs = new ArrayBindingSet(targetNames);
		List<ProjectionElem> elements = pel.getElements();
		for (int i = 0; i < elements.size(); i++) {
			ProjectionElem el = elements.get(i);
			sourcenames[i] =el.getSourceName();
			setters[i] = getSetterToTarget(abs, el.getTargetName());
		}
	}
}
