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
import java.util.Optional;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;

@InternalUseOnly
class CoreDatatypeHelper {

	static Map<IRI, Optional<? extends CoreDatatype>> getReverseLookup() {

		HashMap<IRI, Optional<? extends CoreDatatype>> reverseLookup = new HashMap<>();

		for (CoreDatatype value : CoreDatatype.RDF.values()) {
			reverseLookup.put(value.getIri(), value.asOptional());
		}

		for (CoreDatatype value : CoreDatatype.GEO.values()) {
			reverseLookup.put(value.getIri(), value.asOptional());
		}

		for (CoreDatatype value : CoreDatatype.XSD.values()) {
			reverseLookup.put(value.getIri(), value.asOptional());
		}

		return reverseLookup;
	}
}
