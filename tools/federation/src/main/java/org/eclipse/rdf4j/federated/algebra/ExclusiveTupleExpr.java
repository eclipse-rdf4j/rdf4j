/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

/**
 * Interface representing nodes that can exclusively be evaluated at a single {@link StatementSource}.
 * <p>
 * Implementations are recommended to additionally implement {@link ExclusiveTupleExprRenderer}
 * </p>
 *
 * @author Andreas Schwarte
 * @see ExclusiveStatement
 */
public interface ExclusiveTupleExpr extends FedXTupleExpr {

	/**
	 *
	 * @return the owner for this expression
	 */
	StatementSource getOwner();

}
