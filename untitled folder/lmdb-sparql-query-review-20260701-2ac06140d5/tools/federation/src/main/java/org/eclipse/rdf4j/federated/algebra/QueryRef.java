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

import org.eclipse.rdf4j.federated.structures.QueryInfo;

/**
 * Interface to access the {@link QueryInfo} from all FedX Algebra nodes. All FedX Algebra nodes should implement this
 * interface.
 *
 * @author Andreas Schwarte
 *
 */
public interface QueryRef {

	/**
	 * Retrieve the attached query information of the tuple expression
	 *
	 * @return the {@link QueryInfo}
	 */
	QueryInfo getQueryInfo();
}
