/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Opens a real RDF4J 5.3.2 LMDB store and checks that RDF4J 6.0 reads it, queries it, and writes to it without
 * converting it away from the 5.3.x on-disk format.
 * <p>
 * The fixture in {@code lmdb-5.3.2-complex-store.zip} was produced by running
 * {@code lmdb-5.3.2-complex-store-generator.java.txt} against the released {@code rdf4j-sail-lmdb} 5.3.2 jar built from
 * the {@code 5.3.2} tag. It deliberately covers the format-sensitive areas: legacy {@code (ordinal << 2) | type} IDs,
 * legacy value-record prefixes, the 5.3.x literal layout, language-tagged and typed literals, values that RDF4J 6.0
 * would otherwise inline into the ID, long values resolved through the hash index, blank nodes, several named graphs
 * including a blank-node graph, inferred statements in {@code -inf} databases, a non-default index configuration
 * ({@code spoc,posc,ospc,cspo}), and freed value IDs left behind by garbage collection.
 */
class LegacyLmdbStoreFixtureTest {

	private static final String FIXTURE = "/lmdb-5.3.2-complex-store.zip";

	private static final String EX = "http://example.org/legacy/";
	private static final String LONG_NS = "http://example.org/a-deliberately-long-namespace-that-exceeds-any-short-"
			+ "value-inlining-threshold-and-forces-the-hash-based-lookup-path-to-be-used-for-resolution/";

	/** Counts observed by RDF4J 5.3.2 itself when reading the same fixture. */
	private static final long EXPECTED_EXPLICIT = 2507;
	private static final long EXPECTED_INFERRED = 451;
	private static final long EXPECTED_INFERRED_AGENTS = 300;
	private static final int EXPECTED_BNODE_SUBJECTS = 20;
	private static final int EXPECTED_HUGE_LITERAL_LENGTH = 8208;

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	private File dataDir;

	@BeforeEach
	void unpackFixture() throws IOException {
		dataDir = tempDir.resolve("legacy-store").toFile();
		extract(FIXTURE, dataDir.toPath());
		assertTrue(new File(dataDir, "lmdbrdf.ver").isFile(), "fixture must carry the legacy version marker");
		assertEquals("5.3.2",
				Files.readString(dataDir.toPath().resolve("lmdbrdf.ver"), StandardCharsets.UTF_8).trim());
		assertFalse(new File(dataDir, StoreProperties.FILE_NAME).exists(),
				"fixture must not contain 6.0 metadata");
	}

	@Test
	void readsEveryStatementWrittenBy532() throws Exception {
		LmdbStore store = open();
		try {
			try (NotifyingSailConnection conn = store.getConnection()) {
				assertEquals(EXPECTED_EXPLICIT, count(conn, false));
				assertEquals(EXPECTED_EXPLICIT + EXPECTED_INFERRED, count(conn, true));
			}
		} finally {
			store.shutDown();
		}
	}

	@Test
	void resolvesLegacyLiteralsOfEveryShape() throws Exception {
		LmdbStore store = open();
		try {
			try (NotifyingSailConnection conn = store.getConnection()) {
				// Values that RDF4J 6.0 would inline into the ID must still resolve from the legacy dictionary.
				assertObjectPresent(conn, VF.createIRI(EX, "inline/3"), VF.createIRI(EX, "numeric"),
						VF.createLiteral(42));
				assertObjectPresent(conn, VF.createIRI(EX, "inline/9"), VF.createIRI(EX, "numeric"),
						VF.createLiteral(3.141592653589793d));
				assertObjectPresent(conn, VF.createIRI(EX, "inline/13"), VF.createIRI(EX, "numeric"),
						VF.createLiteral(true));
				assertObjectPresent(conn, VF.createIRI(EX, "inline/6"), VF.createIRI(EX, "numeric"),
						VF.createLiteral(Long.MAX_VALUE));

				// Typed literals, including a custom datatype in a long namespace.
				assertObjectPresent(conn, VF.createIRI(EX, "typed/2"), VF.createIRI(EX, "typedValue"),
						VF.createLiteral("2026-08-07T12:34:56.789Z", XSD.DATETIME));
				assertObjectPresent(conn, VF.createIRI(EX, "typed/8"), VF.createIRI(EX, "typedValue"),
						VF.createLiteral("custom", VF.createIRI(EX, "customDatatype")));
				assertObjectPresent(conn, VF.createIRI(EX, "typed/9"), VF.createIRI(EX, "typedValue"),
						VF.createLiteral("custom-long", VF.createIRI(LONG_NS, "anotherCustomDatatype")));

				// Language-tagged literals, including non-ASCII labels and a long language tag.
				assertObjectPresent(conn, VF.createIRI(EX, "lang/4"), RDFS.LABEL, VF.createLiteral("こんにちは", "ja"));
				assertObjectPresent(conn, VF.createIRI(EX, "lang/7"), RDFS.LABEL,
						VF.createLiteral("emoji 😀 label", "en-US"));
				assertObjectPresent(conn, VF.createIRI(EX, "lang/8"), RDFS.LABEL,
						VF.createLiteral("variant", "en-US-x-lvariant-abc-def-ghi"));

				// A long literal is stored behind the hash index rather than a direct value record.
				List<Value> descriptions = objects(conn, VF.createIRI(EX, "huge"),
						VF.createIRI("http://purl.org/dc/elements/1.1/description"));
				assertEquals(3, descriptions.size());
				for (Value description : descriptions) {
					assertEquals(EXPECTED_HUGE_LITERAL_LENGTH, ((Literal) description).getLabel().length());
				}
			}
		} finally {
			store.shutDown();
		}
	}

	@Test
	void readsLegacyBlankNodesContextsAndInferredStatements() throws Exception {
		LmdbStore store = open();
		try {
			try (NotifyingSailConnection conn = store.getConnection()) {
				Set<String> contexts = new TreeSet<>();
				boolean bnodeContext = false;
				try (CloseableIteration<? extends Resource> it = conn.getContextIDs()) {
					while (it.hasNext()) {
						Resource ctx = it.next();
						if (ctx.isBNode()) {
							bnodeContext = true;
						} else {
							contexts.add(ctx.stringValue());
						}
					}
				}
				assertTrue(bnodeContext, "the blank-node named graph must survive");
				assertEquals(Set.of(EX + "graph/a", EX + "graph/b", EX + "graph/inferred"), contexts);

				int bnodeSubjects = 0;
				try (CloseableIteration<? extends Statement> it = conn.getStatements(null, RDF.TYPE, FOAF.PERSON,
						false)) {
					while (it.hasNext()) {
						if (it.next().getSubject().isBNode()) {
							bnodeSubjects++;
						}
					}
				}
				assertEquals(EXPECTED_BNODE_SUBJECTS, bnodeSubjects);

				// Inferred statements live in the legacy "-inf" databases and must not appear as explicit.
				assertEquals(0, countStatements(conn, null, RDF.TYPE, FOAF.AGENT, false));
				assertEquals(EXPECTED_INFERRED_AGENTS, countStatements(conn, null, RDF.TYPE, FOAF.AGENT, true));
			}
		} finally {
			store.shutDown();
		}
	}

	@Test
	void reusesLegacyIdsFreedByGarbageCollectionInTheOriginalStore() throws Exception {
		LmdbStore store = open();
		try {
			try (NotifyingSailConnection conn = store.getConnection()) {
				// The generator deleted this subject before shutting down, so its value IDs are on the free list.
				assertEquals(0, countStatements(conn, VF.createIRI(EX, "doomed-subject"), null, null, true));
				// ...and the statement written afterwards is still readable.
				assertObjectPresent(conn, VF.createIRI(EX, "post-gc"), RDFS.LABEL,
						VF.createLiteral("written after garbage collection"));
			}
		} finally {
			store.shutDown();
		}
	}

	@Test
	void writesNewStatementsWithoutLeavingLegacyMode() throws Exception {
		IRI subject = VF.createIRI(EX, "written-by-60");
		Literal label = VF.createLiteral("added by RDF4J 6.0", "en");
		Literal number = VF.createLiteral(123456789);

		LmdbStore store = open();
		try {
			try (NotifyingSailConnection conn = store.getConnection()) {
				conn.begin();
				conn.addStatement(subject, RDFS.LABEL, label);
				conn.addStatement(subject, VF.createIRI(EX, "count"), number);
				conn.addStatement(subject, RDF.TYPE, FOAF.PERSON, VF.createIRI(EX, "graph/a"));
				conn.commit();
			}
		} finally {
			store.shutDown();
		}

		// The metadata that decides how a 5.3.x process interprets the store must be untouched.
		assertEquals("5.3.2",
				Files.readString(dataDir.toPath().resolve("lmdbrdf.ver"), StandardCharsets.UTF_8).trim());
		assertFalse(new File(dataDir, StoreProperties.FILE_NAME).exists(),
				"writing through 6.0 must not create 6.0 metadata");
		assertTrue(new File(new File(dataDir, "triples"), "triples.prop").isFile(),
				"the legacy triple-store properties must be preserved");

		LmdbStore reopened = open();
		try {
			try (NotifyingSailConnection conn = reopened.getConnection()) {
				assertEquals(EXPECTED_EXPLICIT + 3, count(conn, false));
				assertObjectPresent(conn, subject, RDFS.LABEL, label);
				assertObjectPresent(conn, subject, VF.createIRI(EX, "count"), number);
				assertEquals(1, countStatements(conn, subject, RDF.TYPE, FOAF.PERSON, false));
				// Everything written by 5.3.2 is still there alongside the new statements.
				assertObjectPresent(conn, VF.createIRI(EX, "lang/4"), RDFS.LABEL, VF.createLiteral("こんにちは", "ja"));
			}
		} finally {
			reopened.shutDown();
		}
	}

	@Test
	void rejectsRdfStarTripleTermsInLegacyMode() throws Exception {
		Value tripleTerm = VF.createTripleTerm(VF.createIRI(EX, "s"), VF.createIRI(EX, "p"), VF.createIRI(EX, "o"));
		IRI star = VF.createIRI(EX, "star");

		LmdbStore store = open();
		try {
			try (NotifyingSailConnection conn = store.getConnection()) {
				conn.begin();
				// Statements are buffered, so the 5.3.x format is enforced when the sink resolves value IDs.
				conn.addStatement(star, VF.createIRI(EX, "says"), tripleTerm);
				IllegalArgumentException e = assertThrows(IllegalArgumentException.class, conn::commit);
				assertTrue(e.getMessage().contains("5.3.x"), e::getMessage);
			}
		} finally {
			store.shutDown();
		}

		// The rejected write must not have left the store in a mixed state.
		LmdbStore reopened = open();
		try {
			try (NotifyingSailConnection conn = reopened.getConnection()) {
				assertEquals(0, countStatements(conn, star, null, null, true));
				assertEquals(EXPECTED_EXPLICIT, count(conn, false));
			}
		} finally {
			reopened.shutDown();
		}
		assertFalse(new File(dataDir, StoreProperties.FILE_NAME).exists());
	}

	@Test
	void rejectsAStoreThatCarriesBothMetadataSchemes() throws Exception {
		Files.writeString(dataDir.toPath().resolve(StoreProperties.FILE_NAME), "version=1\n", StandardCharsets.UTF_8);
		LmdbStore store = newStore();
		try {
			assertThrows(SailException.class, store::init);
		} finally {
			store.shutDown();
		}
	}

	private LmdbStore open() {
		LmdbStore store = newStore();
		store.init();
		return store;
	}

	private LmdbStore newStore() {
		LmdbStoreConfig config = new LmdbStoreConfig();
		// The fixture was created with a non-default index set; it is authoritative in triples.prop either way.
		config.setTripleIndexes("spoc,posc,ospc,cspo");
		return new LmdbStore(dataDir, config);
	}

	private static void assertObjectPresent(NotifyingSailConnection conn, Resource subj, IRI pred, Value obj) {
		assertEquals(1, countStatements(conn, subj, pred, obj, true),
				() -> "expected exactly one statement " + subj + " " + pred + " " + obj);
	}

	private static List<Value> objects(NotifyingSailConnection conn, Resource subj, IRI pred) {
		List<Value> values = new ArrayList<>();
		try (CloseableIteration<? extends Statement> it = conn.getStatements(subj, pred, null, true)) {
			while (it.hasNext()) {
				values.add(it.next().getObject());
			}
		}
		return values;
	}

	private static long count(NotifyingSailConnection conn, boolean includeInferred) {
		return countStatements(conn, null, null, null, includeInferred);
	}

	private static long countStatements(NotifyingSailConnection conn, Resource subj, IRI pred, Value obj,
			boolean includeInferred) {
		long n = 0;
		try (CloseableIteration<? extends Statement> it = conn.getStatements(subj, pred, obj, includeInferred)) {
			while (it.hasNext()) {
				it.next();
				n++;
			}
		}
		return n;
	}

	private static void extract(String resource, Path target) throws IOException {
		Files.createDirectories(target);
		try (InputStream in = LegacyLmdbStoreFixtureTest.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing test fixture " + resource);
			try (ZipInputStream zip = new ZipInputStream(in)) {
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					Path path = target.resolve(entry.getName()).normalize();
					if (!path.startsWith(target)) {
						throw new IOException("zip entry escapes the target directory: " + entry.getName());
					}
					if (entry.isDirectory()) {
						Files.createDirectories(path);
					} else {
						Files.createDirectories(path.getParent());
						Files.copy(zip, path);
					}
					zip.closeEntry();
				}
			}
		}
	}
}
