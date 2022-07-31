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
package org.eclipse.rdf4j.sail.model;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.testsuite.model.ModelNamespacesTest;

/**
 * @author Mark
 */
public class SailModelNamespacesTest extends ModelNamespacesTest {

	private Sail sail;

	private SailConnection conn;

	@Override
	protected Model getModelImplementation() {
		sail = new MemoryStore();
		try {
			sail.init();
			conn = sail.getConnection();
			conn.begin();
			return new SailModel(conn, false);
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	@Override
	public void tearDown() throws Exception {
		if (conn != null) {
			conn.commit();
			conn.close();
			conn = null;
		}
		if (sail != null) {
			sail.shutDown();
			sail = null;
		}
		super.tearDown();
	}
}
