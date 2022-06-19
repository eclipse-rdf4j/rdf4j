/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;

@InternalUseOnly
class CoreDatatypeHelper {

	private static Map<IRI, CoreDatatype> reverseLookup;

	static Map<IRI, CoreDatatype> getReverseLookup() {

		if (reverseLookup == null) {
			HashMap<IRI, CoreDatatype> map = new HashMap<>();

			for (CoreDatatype value : CoreDatatype.RDF.values()) {
				map.put(value.getIri(), value);
			}

			for (CoreDatatype value : CoreDatatype.GEO.values()) {
				map.put(value.getIri(), value);
			}

			for (CoreDatatype value : CoreDatatype.XSD.values()) {
				map.put(value.getIri(), value);
			}

			reverseLookup = Collections.unmodifiableMap(map);
		}

		return reverseLookup;
	}

}
