/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.SameTerm;

/**
 * A query optimizer that replaces {@link Compare} operators with {@link SameTerm}s, if possible.
 *
 * @author Arjohn Kampman
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.CompareOptimizer} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class CompareOptimizer extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.CompareOptimizer {

}
