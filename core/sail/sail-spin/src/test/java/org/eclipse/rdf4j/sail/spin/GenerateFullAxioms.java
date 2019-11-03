/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import java.io.FileWriter;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class GenerateFullAxioms {

	public static void main(String[] args) throws Exception {
		MemoryStore baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		SchemaCachingRDFSInferencer rdfsInferencer = new SchemaCachingRDFSInferencer(deduper);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		Repository repo = new SailRepository(spinSail);
		repo.initialize();
		try (FileWriter writer = new FileWriter("spin-full.ttl")) {
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.exportStatements(null, null, null, true, Rio.createWriter(RDFFormat.TURTLE, writer));
			}
		}
		repo.shutDown();
	}
}
