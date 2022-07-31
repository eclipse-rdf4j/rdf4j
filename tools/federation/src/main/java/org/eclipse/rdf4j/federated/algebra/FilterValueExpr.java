/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.query.algebra.ValueExpr;

/**
 * Interface to indicate filter expressions. Does not provide methods since implementing classes have different
 * purposes, this interface is just a marker.
 *
 * @author Andreas Schwarte
 *
 * @see FilterExpr
 * @see ConjunctiveFilterExpr
 */
public interface FilterValueExpr extends ValueExpr {

	// TODO add methods for both use cases
}
