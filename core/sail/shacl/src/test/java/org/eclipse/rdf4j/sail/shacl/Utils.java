/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
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
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.testimp.TestNotifyingSail;

/**
 * @author Håvard Ottestad
 */
public class Utils {

	public static void loadShapeData(ShaclSail sail, String resourceName) throws IOException {
		sail.init();
		sail.disableValidation();
		Model shapes;
		try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream(resourceName)) {
			shapes = Rio.parse(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
		}
		try (SailConnection conn = sail.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (Statement st : shapes) {
				conn.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), RDF4J.SHACL_SHAPE_GRAPH);
			}
			conn.commit();
		}
		sail.enableValidation();

	}

	public static void loadShapeData(SailRepository repo, String resourceName) throws IOException {

		try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream(resourceName)) {

			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
				conn.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
				conn.commit();
			}
		}
	}

	public static void loadShapeData(SailRepository repo, URL resourceName)
			throws RDF4JException, UnsupportedRDFormatException, IOException {

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			conn.add(resourceName, resourceName.toString(), RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			conn.commit();
		}
	}

	public static SailRepository getInitializedShaclRepository(String shapeData) throws IOException {
		ShaclSail sail = new ShaclSail(new MemoryStore());
		SailRepository repo = new SailRepository(sail);
		Utils.loadShapeData(repo, shapeData);
		return repo;
	}

	public static ShaclSail getInitializedShaclSail(String shapeData) throws IOException {
		ShaclSail sail = new ShaclSail(new MemoryStore());
		Utils.loadShapeData(sail, shapeData);
		return sail;
	}

	public static ShaclSail getInitializedShaclSailNativeStore(TemporaryFolder file, String shapeData)
			throws IOException {
		NativeStore baseSail = getNativeStore(file);

		ShaclSail sail = new ShaclSail(baseSail);
		Utils.loadShapeData(sail, shapeData);
		return sail;
	}

	public static Sail getTestNotifyingSailNativeStore(TemporaryFolder file) {
		NativeStore baseSail = getNativeStore(file);

		return new TestNotifyingSail(baseSail);
	}

	private static NativeStore getNativeStore(TemporaryFolder file) {
		NativeStore baseSail = new NativeStore(file.getFile());
		baseSail.setValueCacheSize(100);
		baseSail.setNamespaceCacheSize(100);
		baseSail.setValueIDCacheSize(100);
		baseSail.setNamespaceIDCacheSize(100);
		return baseSail;
	}

	public static Sail getInitializedShaclSail(NotifyingSail baseSail, String shaclFileName) throws IOException {
		ShaclSail sail = new ShaclSail(baseSail);
		Utils.loadShapeData(sail, shaclFileName);
		return sail;
	}

	public static SailRepository getInitializedShaclRepository(URL resourceName) {
		SailRepository repo = new SailRepository(new ShaclSail(new MemoryStore()));
		repo.initialize();
		try {
			Utils.loadShapeData(repo, resourceName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return repo;
	}

	public static SailRepository getSailRepository(URL resourceName) {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		sailRepository.initialize();
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(resourceName, resourceName.toString(), RDFFormat.TURTLE);
		} catch (IOException | NullPointerException e) {
			System.out.println("Error reading: " + resourceName);
			throw new RuntimeException(e);
		}
		return sailRepository;
	}

	public static void loadInitialData(SailRepository repo, String resourceName) throws IOException {

		try (InputStream initialData = Utils.class.getClassLoader().getResourceAsStream(resourceName)) {
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

	public static TemporaryFolder newTemporaryFolder() {
		return new TemporaryFolder();
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

	public static class TemporaryFolder implements Closeable {

		File file;

		public TemporaryFolder() {
			this.file = Files.newTemporaryFolder();
		}

		@Override
		public void close() {
			try {
				if (file != null) {
					FileUtils.deleteDirectory(file);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public File getFile() {
			return file;
		}
	}

}
