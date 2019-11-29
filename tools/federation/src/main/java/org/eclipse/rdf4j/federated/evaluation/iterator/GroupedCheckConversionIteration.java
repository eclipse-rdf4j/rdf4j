/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Retrieves the original bindings for the particular result
 * 
 * @author Andreas Schwarte
 */
public class GroupedCheckConversionIteration
		extends ConvertingIteration<BindingSet, BindingSet, QueryEvaluationException> {

	protected final List<BindingSet> bindings;

	public GroupedCheckConversionIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			List<BindingSet> bindings) {
		super(iter);
		this.bindings = bindings;
	}

	@Override
	protected BindingSet convert(BindingSet bIn) throws QueryEvaluationException {
		int bIndex = -1;
		Iterator<Binding> bIter = bIn.iterator();
		while (bIter.hasNext()) {
			Binding b = bIter.next();
			String name = b.getName();
			bIndex = Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
		}
		return bindings.get(bIndex);
	}
}
