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

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Wrap an inner iteration and suppress exceptions silently
 *
 * @author Andreas Schwarte
 *
 * @deprecated since 3.1.2. Use {@link org.eclipse.rdf4j.common.iteration.SilentIteration } instead.
 */
@Deprecated
@InternalUseOnly
public class SilentIteration
		extends org.eclipse.rdf4j.common.iteration.SilentIteration<BindingSet, QueryEvaluationException> {

	public SilentIteration(CloseableIteration<BindingSet, QueryEvaluationException> iter) {
		super(iter);
	}

}
