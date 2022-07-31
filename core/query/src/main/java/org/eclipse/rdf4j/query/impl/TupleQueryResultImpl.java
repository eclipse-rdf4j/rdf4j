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
package org.eclipse.rdf4j.query.impl;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * @deprecated since 2.0. Use {@link IteratingTupleQueryResult} instead.
 * @author Jeen Broekstra
 */
@Deprecated
public class TupleQueryResultImpl extends IteratingTupleQueryResult {

	public TupleQueryResultImpl(List<String> bindingNames,
			CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingSetIter) {
		super(bindingNames, bindingSetIter);
	}

}
