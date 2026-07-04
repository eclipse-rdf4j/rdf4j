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

import org.eclipse.rdf4j.federated.util.FedXUtil;

/**
 * A factory that produces globally unique IDS which are used as node identifiers, e.g. in
 * {@link StatementSourcePattern}.
 *
 * @author Andreas Schwarte
 */
public class NodeFactory {

	public static String getNextId() {
		return "n" + FedXUtil.getIncrementalUUID();
	}
}
