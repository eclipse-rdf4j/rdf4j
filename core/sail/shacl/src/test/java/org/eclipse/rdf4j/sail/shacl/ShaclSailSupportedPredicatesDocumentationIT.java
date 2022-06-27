/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Håvard Ottestad
 */
public class ShaclSailSupportedPredicatesDocumentationIT extends AbstractShaclTest {

	private static final Set<IRI> STATIC_SHACL_PREDICATES = new CopyOnWriteArraySet<>(
			ShaclSail.getSupportedShaclPredicates());

	@AfterAll
	public static void afterClass() {

		Assertions.assertTrue(STATIC_SHACL_PREDICATES.isEmpty(),
				"No test uses the following predicate that the ShaclSail announces as supported: "
						+ Arrays.toString(STATIC_SHACL_PREDICATES.toArray()));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	public void testShaclSailSupportedPredicatesDocumentation(TestCase testCase) throws IOException {

		HashSet<IRI> shaclPredicates = new HashSet<>(ShaclSail.getSupportedShaclPredicates());

		Model parse = testCase.getShacl();

		Set<IRI> predicatesInUseInTest = parse.predicates()
				.stream()
				.filter(p -> (p.getNamespace().equals(SHACL.NAMESPACE) ||
						p.getNamespace().equals(RSX.NAMESPACE) ||
						p.getNamespace().equals(DASH.NAMESPACE)))
				.collect(Collectors.toSet());

		for (IRI predicate : predicatesInUseInTest) {
			Assertions.assertTrue(shaclPredicates.contains(predicate),
					"Predicate used in test but not listed in ShaclSail: " + predicate);
			STATIC_SHACL_PREDICATES.remove(predicate);
		}

	}

	private Model getShacl(String shacl) throws IOException {
		return Rio.parse(
				new StringReader(shacl), "",
				RDFFormat.TRIG);
	}

}
