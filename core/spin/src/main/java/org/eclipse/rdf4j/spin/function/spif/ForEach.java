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
package org.eclipse.rdf4j.spin.function.spif;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.spin.function.InverseMagicProperty;

public class ForEach implements InverseMagicProperty {

	@Override
	public String getURI() {
		return SPIF.FOR_EACH_PROPERTY.toString();
	}

	@Override
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			ValueFactory valueFactory, Value... args) throws QueryEvaluationException {
		return new CloseableIteratorIteration<>(
				SingleValueToListTransformer.transform(Arrays.<Value>asList(args).iterator()));
	}
}
