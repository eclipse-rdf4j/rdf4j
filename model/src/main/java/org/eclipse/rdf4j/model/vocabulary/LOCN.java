/**
 * Copyright (c) 2018 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

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
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		ValueFactory factory = SimpleValueFactory.getInstance();

		ADDRESS = factory.createIRI(NAMESPACE, "Address");
		GEOMETRY = factory.createIRI(NAMESPACE, "Geometry");

		ADDRESS_PROP = factory.createIRI(NAMESPACE, "address");
		ADDRESS_AREA = factory.createIRI(NAMESPACE, "addressArea");
		ADDRESS_ID = factory.createIRI(NAMESPACE, "addressId");
		ADMIN_UNIT_L1 = factory.createIRI(NAMESPACE, "adminUnitL1");
		ADMIN_UNIT_L2 = factory.createIRI(NAMESPACE, "adminUnitL2");
		FULL_ADDRESS = factory.createIRI(NAMESPACE, "fullAddress");
		GEOGRAPHIC_NAME = factory.createIRI(NAMESPACE, "geographicName");
		GEOMETRY_PROP = factory.createIRI(NAMESPACE, "geometry");
		LOCATION = factory.createIRI(NAMESPACE, "location");
		LOCATOR_DESIGNATOR = factory.createIRI(NAMESPACE, "locatorDesignator");
		LOCATOR_NAME = factory.createIRI(NAMESPACE, "locatorName");
		PO_BOX = factory.createIRI(NAMESPACE, "poBox");
		POST_CODE = factory.createIRI(NAMESPACE, "postCode");
		POST_NAME = factory.createIRI(NAMESPACE, "postName");
		THOROUGHFARE = factory.createIRI(NAMESPACE, "thoroughfare");
	}
}

