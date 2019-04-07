/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DC;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Bart Hanssens
 */
public class VocabulariesTest {

	@Test
	public void testVocabAllIRI() throws Exception {
		Set<IRI> dcIRIs = new HashSet<>(Arrays.asList(DC.CONTRIBUTOR, DC.COVERAGE, DC.CREATOR, DC.DATE, DC.DESCRIPTION,
				DC.FORMAT, DC.IDENTIFIER, DC.LANGUAGE, DC.PUBLISHER, DC.RELATION, DC.RIGHTS, DC.SOURCE, DC.SUBJECT,
				DC.TITLE, DC.TYPE));

		Set<IRI> allIRIs = Vocabularies.getIRIs(DC.class);

		assertEquals(dcIRIs, allIRIs);
	}
}
