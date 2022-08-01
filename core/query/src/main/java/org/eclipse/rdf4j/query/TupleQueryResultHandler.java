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
package org.eclipse.rdf4j.query;

import java.util.List;

/**
 * An interface defining methods related to handling sequences of Solutions.
 * <p>
 * Instances of this interface are capable of handling tuple results using the {@link #startQueryResult(List)} ,
 * {@link #handleSolution(BindingSet)} and {@link #endQueryResult()} methods.
 */
public interface TupleQueryResultHandler extends QueryResultHandler {

}
