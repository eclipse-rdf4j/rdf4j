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
package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

/**
 * An inferencer may infer the same statement from two different statements. This leads to that single inferred
 * statement undergoing inferencing multiple times, and all the statements inferred from it too, etc. This is mainly due
 * to the use of SailConnectionListeners which don't distinguish between adding a new statement and one that already
 * exists. Adding this inferencer to a Sail stack prevents this problem and gives a significant performance increase.
 */
public class DedupingInferencer extends NotifyingSailWrapper {

	public DedupingInferencer() {
	}

	public DedupingInferencer(NotifyingSail baseSail) {
		super(baseSail);
	}

	@Override
	public InferencerConnection getConnection() throws SailException {
		try {
			InferencerConnection con = (InferencerConnection) super.getConnection();
			return new DedupingInferencerConnection(con, getValueFactory());
		} catch (ClassCastException e) {
			throw new SailException(e.getMessage(), e);
		}
	}
}
