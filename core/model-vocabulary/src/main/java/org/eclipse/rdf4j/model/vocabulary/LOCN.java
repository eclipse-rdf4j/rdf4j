/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the ISA Programme Location Core Vocabulary.
 *
 * @author Bart Hanssens
 * @see <a href="https://www.w3.org/ns/locn">ISA Programme Location Core Vocabulary</a>
 */
public class LOCN {
	/**
	 * The LOCN namespace: http://www.w3.org/ns/locn#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/locn#";

	/**
	 * Recommended prefix for the namespace: "locn"
	 */
	public static final String PREFIX = "locn";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** locn:Address */
	public static final IRI ADDRESS;

	/** locn:Geometry */
	public static final IRI GEOMETRY;

	// Properties
	/** locn:address */
	public static final IRI ADDRESS_PROP;

	/** locn:addressArea */
	public static final IRI ADDRESS_AREA;

	/** locn:addressId */
	public static final IRI ADDRESS_ID;

	/** locn:adminUnitL1 */
	public static final IRI ADMIN_UNIT_L1;

	/** locn:adminUnitL2 */
	public static final IRI ADMIN_UNIT_L2;

	/** locn:fullAddress */
	public static final IRI FULL_ADDRESS;

	/** locn:geographicName */
	public static final IRI GEOGRAPHIC_NAME;

	/** locn:geometry */
	public static final IRI GEOMETRY_PROP;

	/** locn:location */
	public static final IRI LOCATION;

	/** locn:locatorDesignator */
	public static final IRI LOCATOR_DESIGNATOR;

	/** locn:locatorName */
	public static final IRI LOCATOR_NAME;

	/** locn:poBox */
	public static final IRI PO_BOX;

	/** locn:postCode */
	public static final IRI POST_CODE;

	/** locn:postName */
	public static final IRI POST_NAME;

	/** locn:thoroughfare */
	public static final IRI THOROUGHFARE;

	static {

		ADDRESS = Vocabularies.createIRI(NAMESPACE, "Address");
		GEOMETRY = Vocabularies.createIRI(NAMESPACE, "Geometry");

		ADDRESS_PROP = Vocabularies.createIRI(NAMESPACE, "address");
		ADDRESS_AREA = Vocabularies.createIRI(NAMESPACE, "addressArea");
		ADDRESS_ID = Vocabularies.createIRI(NAMESPACE, "addressId");
		ADMIN_UNIT_L1 = Vocabularies.createIRI(NAMESPACE, "adminUnitL1");
		ADMIN_UNIT_L2 = Vocabularies.createIRI(NAMESPACE, "adminUnitL2");
		FULL_ADDRESS = Vocabularies.createIRI(NAMESPACE, "fullAddress");
		GEOGRAPHIC_NAME = Vocabularies.createIRI(NAMESPACE, "geographicName");
		GEOMETRY_PROP = Vocabularies.createIRI(NAMESPACE, "geometry");
		LOCATION = Vocabularies.createIRI(NAMESPACE, "location");
		LOCATOR_DESIGNATOR = Vocabularies.createIRI(NAMESPACE, "locatorDesignator");
		LOCATOR_NAME = Vocabularies.createIRI(NAMESPACE, "locatorName");
		PO_BOX = Vocabularies.createIRI(NAMESPACE, "poBox");
		POST_CODE = Vocabularies.createIRI(NAMESPACE, "postCode");
		POST_NAME = Vocabularies.createIRI(NAMESPACE, "postName");
		THOROUGHFARE = Vocabularies.createIRI(NAMESPACE, "thoroughfare");
	}
}
