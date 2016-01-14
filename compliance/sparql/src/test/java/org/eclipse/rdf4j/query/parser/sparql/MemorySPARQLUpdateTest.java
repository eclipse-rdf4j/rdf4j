/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.query.parser.sparql.SPARQLUpdateTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;


/**
 * Test SPARQL 1.1 Update functionality on an in-memory store.
 * 
 * @author Jeen Broekstra
 */
public class MemorySPARQLUpdateTest extends SPARQLUpdateTest {

	@Override
	protected Repository newRepository()
		throws Exception
	{
		return new SailRepository(new MemoryStore());
	}

}
