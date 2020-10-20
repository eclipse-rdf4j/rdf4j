/**
 * Copyright (c) 2018 Eclipse RDF4J contributors, and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Distribution License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the ISA Programme Location Core Vocabulary.
 *
 * @see <a href="https://www.w3.org/ns/locn">ISA Programme Location Core Vocabulary</a>
 *
 * @author Bart Hanssens
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
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

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

		ADDRESS = createIRI(NAMESPACE, "Address");
		GEOMETRY = createIRI(NAMESPACE, "Geometry");

		ADDRESS_PROP = createIRI(NAMESPACE, "address");
		ADDRESS_AREA = createIRI(NAMESPACE, "addressArea");
		ADDRESS_ID = createIRI(NAMESPACE, "addressId");
		ADMIN_UNIT_L1 = createIRI(NAMESPACE, "adminUnitL1");
		ADMIN_UNIT_L2 = createIRI(NAMESPACE, "adminUnitL2");
		FULL_ADDRESS = createIRI(NAMESPACE, "fullAddress");
		GEOGRAPHIC_NAME = createIRI(NAMESPACE, "geographicName");
		GEOMETRY_PROP = createIRI(NAMESPACE, "geometry");
		LOCATION = createIRI(NAMESPACE, "location");
		LOCATOR_DESIGNATOR = createIRI(NAMESPACE, "locatorDesignator");
		LOCATOR_NAME = createIRI(NAMESPACE, "locatorName");
		PO_BOX = createIRI(NAMESPACE, "poBox");
		POST_CODE = createIRI(NAMESPACE, "postCode");
		POST_NAME = createIRI(NAMESPACE, "postName");
		THOROUGHFARE = createIRI(NAMESPACE, "thoroughfare");
	}
}
