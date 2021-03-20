/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
public class ShaclSailSupportedPredicatesDocumentationIT extends AbstractShaclTest {

	private static HashSet<IRI> staticShaclPredicates = new HashSet<>(ShaclSail.getSupportedShaclPredicates());

	public ShaclSailSupportedPredicatesDocumentationIT(String testCasePath, String path,
			ExpectedResult expectedResult, IsolationLevel isolationLevel) {
		super(testCasePath, path, expectedResult, isolationLevel);
	}

	@AfterClass
	public static void afterClass() {

		assertTrue("No test uses the following predicate that the ShaclSail announces as supported: "
				+ Arrays.toString(staticShaclPredicates.toArray()), staticShaclPredicates.isEmpty());
	}

	@Test
	public void testShaclSailSupportedPredicatesDocumentation() throws IOException {

		HashSet<IRI> shaclPredicates = new HashSet<>(ShaclSail.getSupportedShaclPredicates());

		Model parse = getShacl();

		Set<IRI> predicatesInUseInTest = parse.predicates()
				.stream()
				.filter(p -> (p.getNamespace().equals(SHACL.NAMESPACE) ||
						p.getNamespace().equals(RSX.NAMESPACE) ||
						p.getNamespace().equals(DASH.NAMESPACE)))
				.collect(Collectors.toSet());

		for (IRI predicate : predicatesInUseInTest) {
			assertTrue("Predicate used in test but not listed in ShaclSail: " + predicate,
					shaclPredicates.contains(predicate));
			staticShaclPredicates.remove(predicate);
		}

	}

	private Model getShacl() throws IOException {
		String shaclFile = getShaclPath();

		return Rio.parse(
				ShaclSailSupportedPredicatesDocumentationIT.class.getClassLoader().getResourceAsStream(shaclFile), "",
				RDFFormat.TURTLE);
	}

}
