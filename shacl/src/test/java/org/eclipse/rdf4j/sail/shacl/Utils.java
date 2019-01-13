/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * @author Håvard Ottestad
 */
public class Utils {

	public static void loadShapeData(ShaclSail sail, String resourceName)
		throws RDF4JException, UnsupportedRDFormatException, IOException
	{
		InputStream shapesData = Utils.class.getResourceAsStream("/" + resourceName);
		Model shapes = Rio.parse(shapesData, "", RDFFormat.TURTLE, ShaclSail.SHAPE_GRAPH);
		try (SailConnection conn = sail.getConnection()) {
			conn.begin();
			for (Statement st : shapes) {
				conn.addStatement(st.getSubject(), st.getPredicate(), st.getContext(), ShaclSail.SHAPE_GRAPH);
			}
			conn.commit();
		}

	}

	public static void loadShapeData(SailRepository repo, String resourceName)
		throws RDF4JException, UnsupportedRDFormatException, IOException
	{
		InputStream shapesData = Utils.class.getResourceAsStream("/" + resourceName);
		Model shapes = Rio.parse(shapesData, "", RDFFormat.TURTLE, ShaclSail.SHAPE_GRAPH);
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			for (Statement st : shapes) {
				conn.add(st.getSubject(), st.getPredicate(), st.getContext(), ShaclSail.SHAPE_GRAPH);
			}
			conn.commit();
		}

	}

	public static SailRepository getInitializedShaclRepository(String shapeData) throws Exception {
		SailRepository repo = new SailRepository(new ShaclSail(new MemoryStore()));
		repo.initialize();
		Utils.loadShapeData(repo, shapeData);
		return repo;
	}
	
	public static ShaclSail getInitializedShaclSail(String shapeData) throws Exception {
		ShaclSail sail = new ShaclSail(new MemoryStore());
		sail.initialize();
		Utils.loadShapeData(sail, shapeData);
		return sail;
	}

	static class Ex {

		public final static String ns = "http://example.com/ns#";

		public final static IRI Person = createIri("Person");

		public final static IRI ssn = createIri("ssn");

		public final static IRI name = createIri("name");

		public static IRI createIri(String name) {
			return SimpleValueFactory.getInstance().createIRI(ns + name);
		}

		public static IRI createIri() {
			return SimpleValueFactory.getInstance().createIRI(ns + UUID.randomUUID().toString());
		}
	}
}
