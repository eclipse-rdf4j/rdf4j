/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.query.parser.sparql.ComplexSPARQLQueryTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;

/**
 * @author jeen
 */
public class MemoryComplexSPARQLQueryTest extends ComplexSPARQLQueryTest {

	@Override
	protected Repository newRepository() throws Exception {
		return new SailRepository(new MemoryStore());
	}

}
