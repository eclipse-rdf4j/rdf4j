/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.common.order;

import java.util.Set;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * An interface for {@link StatementOrder} implementations that can report which orders they support for a given
 * subject, predicate, object and contexts.
 */
@Experimental
public interface AvailableStatementOrder {

	/**
	 * Returns the supported orders for the given subject, predicate, object and contexts.
	 *
	 * @param subj
	 * @param pred
	 * @param obj
	 * @param contexts
	 * @return the supported orders for the given subject, predicate, object and contexts.
	 */
	@Experimental
	Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts);
}
