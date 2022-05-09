/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * @author Håvard Ottestad
 */
public class Utils {

	public static void loadShapeData(ShaclSail sail, String resourceName) throws IOException {
		assert resourceName.endsWith(".trig") : "Not a RDF Trig file: " + resourceName;
		sail.init();
		sail.disableValidation();
		Model shapes;
		try (InputStream shapesData = getResourceAsStream(resourceName)) {
			assert shapesData != null : "Could not find: " + resourceName;
			shapes = Rio.parse(shapesData, "", RDFFormat.TRIG);
		}
		try (SailConnection conn = sail.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (Statement st : shapes) {
				conn.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
			conn.commit();
		}
		sail.enableValidation();

	}

	public static void loadShapeData(SailRepository repo, String resourceName) throws IOException {
		assert resourceName.endsWith(".trig") : "Not a RDF Trig file: " + resourceName;

		try (InputStream shapesData = getResourceAsStream(resourceName)) {
			assert shapesData != null : "Could not find: " + resourceName;
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				conn.add(shapesData, "", RDFFormat.TRIG);
				conn.commit();
			}
		}
	}

	public static void loadShapeData(SailRepository repo, Model shapes) {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			conn.add(shapes);
			conn.commit();
		}
	}

	private static InputStream getResourceAsStream(String resourceName) {
		InputStream resourceAsStream = Utils.class.getClassLoader().getResourceAsStream(resourceName);
		if (resourceAsStream != null) {
			return new BufferedInputStream(resourceAsStream);
		}
		return null;
	}

	public static void loadShapeData(SailRepository repo, URL resourceName)
			throws RDF4JException, UnsupportedRDFormatException, IOException {
		assert resourceName.toString().endsWith(".trig") : "Not a RDF Trig file: " + resourceName;

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			conn.add(resourceName, resourceName.toString(), RDFFormat.TRIG);
			conn.commit();
		}
	}

	public static SailRepository getInitializedShaclRepository(String shapeData) throws IOException {
		SailRepository repo = new SailRepository(new ShaclSail(new MemoryStore()));
		Utils.loadShapeData(repo, shapeData);
		return repo;
	}

	public static ShaclSail getInitializedShaclSail(String shapeData) throws IOException {
		ShaclSail sail = new ShaclSail(new MemoryStore());
		Utils.loadShapeData(sail, shapeData);
		return sail;
	}

	public static Sail getInitializedShaclSail(NotifyingSail baseSail, String shaclFileName) throws IOException {
		ShaclSail sail = new ShaclSail(baseSail);
		Utils.loadShapeData(sail, shaclFileName);
		return sail;
	}

	public static SailRepository getInitializedShaclRepository(URL resourceName) {
		assert resourceName.toString().endsWith(".trig") : "Not a RDF Trig file: " + resourceName;
		SailRepository repo = new SailRepository(new ShaclSail(new MemoryStore()));
		try {
			Utils.loadShapeData(repo, resourceName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return repo;
	}

	public static SailRepository getSailRepository(URL resourceName, RDFFormat format) {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(resourceName, resourceName.toString(), format);
		} catch (IOException | NullPointerException e) {
			System.out.println("Error reading: " + resourceName);
			throw new RuntimeException(e);
		}
		return sailRepository;
	}

	public static void loadInitialData(SailRepository repo, String resourceName) throws IOException {

		try (InputStream initialData = getResourceAsStream(resourceName)) {
			if (initialData == null) {
				return;
			}

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				conn.add(initialData, "", RDFFormat.TURTLE);
				conn.commit();
			}
		}

	}

	public static void loadShapeData(SailRepository repo, URL resourceName, RDFFormat format, IRI shaclShapeGraph)
			throws IOException {

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			conn.add(resourceName, resourceName.toString(), format, shaclShapeGraph);
			conn.commit();
		}
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
			return SimpleValueFactory.getInstance().createIRI(ns + UUID.randomUUID());
		}
	}
}
