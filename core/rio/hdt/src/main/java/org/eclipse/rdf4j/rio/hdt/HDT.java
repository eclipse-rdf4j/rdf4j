/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * HDT Vocabulary helper class. 
 * 
 * Kept to the bare minimum, since it is not an "official" vocabulary, just to be compatible with HDT-It
 *  
 * @author Bart Hanssens
 */
class HDT {
	/**
	 * The HDT namespace: http://purl.org/HDT/hdt#
	 */
	public static final String NAMESPACE = "http://purl.org/HDT/hdt#";

	/**
	 * Recommended prefix for HDT: "hdt"
	 */
	public static final String PREFIX = "hdt";

	/**
	 * An immutable {@link Namespace} constant that represents the HDT namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Class
	/**
	 * hdt:Dataset
	 */
	public static final IRI DATASET;

	// Properties
	/**
	 * hdt:dictionary
	 */
	public static final IRI DICTIONARY;
	
	/**
	 * hdt:dictionaryblockSize
	 */
	public static final IRI DICTIONARY_BLOCK_SIZE;

	/**
	 * hdt:dictionaryFour
	 */
	public static final IRI DICTIONARY_FOUR;

	/**
	 * hdt:dictionarymapping
	 */
	public static final IRI DICTIONARY_MAPPING;

	/**
	 * hdt:dictionarynumSharedSubjectObject
	 */
	public static final IRI DICTIONARY_NUMSHARED;

	/**
	 * hdt:dictionarysizeStrings
	 */
	public static final IRI DICTIONARY_SIZE_STRINGS;

	/**
	 * hdt:formatInformation
	 */
	public static final IRI FORMAT_INFORMATION;

	/**
	 * hdt:HDTv1
	 */
	public static final IRI HDT_V1;

	/**
	 * hdt:publicationInformation
	 */
	public static final IRI PUBLICATION_INFORMATION;

	/**
	 * hdt:statisticalInformation
	 */
	public static final IRI STATISTICAL_INFORMATION;

	/**
	 * hdt:triples
	 */
	public static final IRI TRIPLES;

	
	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		DATASET = factory.createIRI(NAMESPACE, "Dataset");

		DICTIONARY = factory.createIRI(NAMESPACE, "dictionary");
		DICTIONARY_BLOCK_SIZE = factory.createIRI(NAMESPACE, "dictionaryblockSize");
		DICTIONARY_FOUR = factory.createIRI(NAMESPACE, "dictionaryFour");
		DICTIONARY_MAPPING = factory.createIRI(NAMESPACE, "dictionarymapping");
		DICTIONARY_NUMSHARED = factory.createIRI(NAMESPACE, "dictionarynumSharedSubjectObject");
		DICTIONARY_SIZE_STRINGS = factory.createIRI(NAMESPACE, "dictionarysizeStrings");
		FORMAT_INFORMATION = factory.createIRI(NAMESPACE, "formatInformation");
		HDT_V1 = factory.createIRI(NAMESPACE, "HDTv1");
		PUBLICATION_INFORMATION = factory.createIRI(NAMESPACE, "publicationInformation");
		STATISTICAL_INFORMATION = factory.createIRI(NAMESPACE, "statisticalInformation");
		TRIPLES = factory.createIRI(NAMESPACE, "triples");
	}
}
