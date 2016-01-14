/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import junit.framework.Test;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelTest;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.model.SailModel;

/**
 *
 * @author Mark
 */
public class SailModelTest extends ModelTest {
	private Sail sail;
	private SailConnection conn;

	public static Test suite() throws Exception {
		return ModelTest.suite(SailModelTest.class);
	}

	public SailModelTest(String name) {
		super(name);
	}

	@Override
	public Model makeEmptyModel() {
		sail = new MemoryStore();
		try {
			sail.initialize();
			conn = sail.getConnection();
			conn.begin();
			return new SailModel(conn, false);
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	@Override
    protected void tearDown() throws Exception {
		if(conn != null) {
			conn.commit();
			conn.close();
			conn = null;
		}
		if(sail != null) {
			sail.shutDown();
			sail = null;
		}
		super.tearDown();
	}
}
