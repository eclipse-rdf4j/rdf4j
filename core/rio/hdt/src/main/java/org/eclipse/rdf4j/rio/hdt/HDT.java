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
	 * hdt:formatInformation
	 */
	public static final IRI FORMAT_INFORMATION;

	/**
	 * hdt:publicationInformation
	 */
	public static final IRI PUBLICATION_INFORMATION;

	/**
	 * hdt:statisticalInformation
	 */
	public static final IRI STATISTICAL_INFORMATION;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		DATASET = factory.createIRI(NAMESPACE, "Dataset");

		FORMAT_INFORMATION = factory.createIRI(NAMESPACE, "formatInformation");
		PUBLICATION_INFORMATION = factory.createIRI(NAMESPACE, "publicationInformation");
		STATISTICAL_INFORMATION = factory.createIRI(NAMESPACE, "statisticalInformation");
	}
}
