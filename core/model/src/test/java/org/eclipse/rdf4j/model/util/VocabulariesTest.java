/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.HYDRA;
import org.junit.Test;

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

	@Test
	public void testVocabAllIRIHYDRA() throws Exception {
		Set<IRI> hydraIRIs = new HashSet<>(Arrays.asList(HYDRA.API_DOCUMENTATION, HYDRA.CLASS, HYDRA.COLLECTION,
				HYDRA.ERROR,
				HYDRA.IRI_TEMPLATE, HYDRA.IRI_TEMPLATE_MAPPING, HYDRA.LINK, HYDRA.OPERATION,
				HYDRA.PARTIAL_COLLECTION_VIEW,
				HYDRA.RESOURCE, HYDRA.STATUS, HYDRA.SUPPORTED_PROPERTY, HYDRA.TEMPLATED_LINK,
				HYDRA.VARIABLE_REPRESENTATION,
				HYDRA.API_DOCUMENTATION_PROP, HYDRA.COLLECTION_PROP, HYDRA.DESCRIPTION, HYDRA.ENTRYPOINT, HYDRA.EXPECTS,
				HYDRA.EXPECTS_HEADER, HYDRA.FIRST, HYDRA.FREETEXT_QUERY, HYDRA.LAST, HYDRA.LIMIT, HYDRA.MAPPING,
				HYDRA.MEMBER,
				HYDRA.METHOD, HYDRA.NEXT, HYDRA.OFFSET, HYDRA.OPERATION_PROP, HYDRA.PAGE_INDEX, HYDRA.PAGE_REFERENCE,
				HYDRA.POSSIBLE_STATUS, HYDRA.PREVIOUS, HYDRA.PROPERTY, HYDRA.READABLE, HYDRA.REQUIRED, HYDRA.RETURNS,
				HYDRA.RETURNS_HEADER, HYDRA.SEARCH, HYDRA.STATUS_CODE, HYDRA.SUPPORTED_CLASS, HYDRA.SUPPORTED_OPERATION,
				HYDRA.SUPPORTED_PROPERTY_PROP, HYDRA.TEMPLATE, HYDRA.TITLE, HYDRA.TOTAL_ITEMS, HYDRA.VARIABLE,
				HYDRA.VARIABLE_REPRESENTATION_PROP, HYDRA.VIEW, HYDRA.WRITABLE));

		Set<IRI> allIRIs = Vocabularies.getIRIs(HYDRA.class);

		assertEquals(hydraIRIs, allIRIs);
	}
}
