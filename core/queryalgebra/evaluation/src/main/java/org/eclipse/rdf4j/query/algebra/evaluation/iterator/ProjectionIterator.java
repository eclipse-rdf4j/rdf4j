/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;

public class ProjectionIterator extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException>
{

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Projection projection;

	private final BindingSet parentBindings;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ProjectionIterator(Projection projection,
			CloseableIteration<BindingSet, QueryEvaluationException> iter, BindingSet parentBindings)
		throws QueryEvaluationException
	{
		super(iter);
		this.projection = projection;
		this.parentBindings = parentBindings;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet convert(BindingSet sourceBindings)
		throws QueryEvaluationException
	{

		return project(projection.getProjectionElemList(), sourceBindings, parentBindings);
	}

	public static BindingSet project(ProjectionElemList projElemList, BindingSet sourceBindings,
			BindingSet parentBindings)
	{
		QueryBindingSet resultBindings = new QueryBindingSet();

		for (ProjectionElem pe : projElemList.getElements()) {
			Value targetValue = sourceBindings.getValue(pe.getSourceName());
			if (targetValue == null) {
				targetValue = parentBindings.getValue(pe.getSourceName());
			}
			if (targetValue != null) {
				resultBindings.setBinding(pe.getTargetName(), targetValue);
			}
		}

		return resultBindings;
	}
}
