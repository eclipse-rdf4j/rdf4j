/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.FunctionCall;

/**
 * A query optimizer that replaces REGEX with {@link FunctionCall}s that are equivalent operators
 *
 * @author Jerven Bolleman
 * @deprecated since 4.1.0. Use
 *             {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.RegexAsStringFunctionOptimizer} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class RegexAsStringFunctionOptimizer
		extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.RegexAsStringFunctionOptimizer {

	public RegexAsStringFunctionOptimizer(ValueFactory vf) {
		super(vf);
	}

}
