/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.compliance;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.SparqlRegexTest;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStoreImplForTests;

import java.io.IOException;

public class ExtensibleStoreSparqlRegexTest extends SparqlRegexTest {

	@Override
	protected Repository newRepository() throws IOException {
		return new SailRepository(new ExtensibleStoreImplForTests());
	}

}
