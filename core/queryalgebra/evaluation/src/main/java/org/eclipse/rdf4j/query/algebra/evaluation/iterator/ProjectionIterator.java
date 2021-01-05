/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import org.eclipse.rdf4j.query.algebra.evaluation.ArrayBindingSet;

/**
 * A projection iterator "changes" the names of the variables from inside the engine to what is used by the query
 * result.
 */
public class ProjectionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Constants *
	 *-----------*/
	/**
	 * This function is specialized for different scenarios. At construction time the function that is most specialized
	 * is initialized and used.
	 */
	private final Function<BindingSet, BindingSet> converter;

	/*--------------*
	 * Constructors *
	 *--------------*/
	public ProjectionIterator(Projection projection, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			BindingSet parentBindings) throws QueryEvaluationException {
		super(iter);
		ProjectionElemList pel = projection.getProjectionElemList();
		this.converter = createConverter(pel, parentBindings, !determineOuterProjection(projection));
	}

	public static Function<BindingSet, BindingSet> createConverter(ProjectionElemList pel, BindingSet parentBindings,
			boolean includeAllParent) {
		String[] targetNames = getTargetNames(pel);
		if (parentBindings.size() == 0) {
			return convertWithNoParentBindings(pel, targetNames);
		} else if (includeAllParent) {
			return convertIncludingAllParentBindings(pel, parentBindings, targetNames);
		} else {
			return convertSometimesIncludingAParentBinding(pel, parentBindings, targetNames);
		}
	}

	private static String[] getTargetNames(ProjectionElemList pel) {
		return pel.getTargetNames().toArray(new String[0]);
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
		final String[] targetNames = getTargetNames(projElemList);
		if (includeAllParentBindings) {
			return convertIncludingAllParentBindings(projElemList, parentBindings, targetNames)
					.apply(sourceBindings);
		} else {
			return convertSometimesIncludingAParentBinding(projElemList, parentBindings, targetNames)
					.apply(sourceBindings);
		}
	}

	private static Function<BindingSet, BindingSet> convertSometimesIncludingAParentBinding(ProjectionElemList pel,
			BindingSet parentBindings,
			String[] targetNames) {
		final int size = pel.getElements().size();
		final String[] sourcenames = new String[size];
		@SuppressWarnings("unchecked")
		final BiConsumer<ArrayBindingSet, Value>[] setters = new BiConsumer[size];

		getSourceToTargetSetters(pel, targetNames, sourcenames, setters);
		Supplier<ArrayBindingSet> sup = () -> new ArrayBindingSet(targetNames);
		final BiFunction<BindingSet, String, Value> extractor = (sb, var) -> {
			final Value value = sb.getValue(var);
			if (value == null)
				return parentBindings.getValue(var);
			return value;
		};
		return makeConverterFunction(size, sourcenames, setters, sup, extractor);
	}

	private static Function<BindingSet, BindingSet> convertIncludingAllParentBindings(ProjectionElemList pel,
			BindingSet parentBindings,
			String[] targetNames) {

		final int size = pel.getElements().size();
		final String[] sourcenames = new String[size];
		@SuppressWarnings("unchecked")
		final BiConsumer<ArrayBindingSet, Value>[] setters = new BiConsumer[size];

		getSourceToTargetSetters(pel, targetNames, sourcenames, setters);
		Supplier<ArrayBindingSet> sup = () -> {
			return new ArrayBindingSet(parentBindings, targetNames);
		};
		return makeConverterFunction(size, sourcenames, setters, sup, (sb, var) -> sb.getValue(var));
	}

	private static Function<BindingSet, BindingSet> convertWithNoParentBindings(ProjectionElemList pel,
			String[] targetNames) {

		final int size = pel.getElements().size();
		final String[] sourcenames = new String[size];
		@SuppressWarnings("unchecked")
		final BiConsumer<ArrayBindingSet, Value>[] setters = new BiConsumer[size];

		getSourceToTargetSetters(pel, targetNames, sourcenames, setters);
		Supplier<ArrayBindingSet> sup = () -> new ArrayBindingSet(targetNames);
		return makeConverterFunction(size, sourcenames, setters, sup, (sb, var) -> sb.getValue(var));
	}

	private static Function<BindingSet, BindingSet> makeConverterFunction(final int size, final String[] sourcenames,
			final BiConsumer<ArrayBindingSet, Value>[] setters, Supplier<ArrayBindingSet> sup,
			BiFunction<BindingSet, String, Value> extractor) {
		return sourceBindings -> {
			ArrayBindingSet abs2 = sup.get();

			for (int j = 0; j < size; j++) {
				Value targetValue = sourceBindings.getValue(sourcenames[j]);
				if (targetValue != null) {
					setters[j].accept(abs2, targetValue);
				}
			}
			return abs2;
		};
	}

	private static void getSourceToTargetSetters(ProjectionElemList pel, String[] targetNames,
			final String[] sourcenames,
			final BiConsumer<ArrayBindingSet, Value>[] setters) {
		ArrayBindingSet abs = new ArrayBindingSet(targetNames);
		List<ProjectionElem> elements = pel.getElements();
		for (int i = 0; i < elements.size(); i++) {
			ProjectionElem el = elements.get(i);
			sourcenames[i] = el.getSourceName();
			setters[i] = abs.getDirectSetterForVariable(el.getTargetName());
		}
	}
}
