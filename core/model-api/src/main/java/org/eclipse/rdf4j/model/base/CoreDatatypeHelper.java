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

	static Map<IRI, Optional<CoreDatatype>> reverseLookup;

	static Map<IRI, Optional<CoreDatatype>> getReverseLookup() {

		if (reverseLookup == null) {
			HashMap<IRI, Optional<CoreDatatype>> map = new HashMap<>();

			for (CoreDatatype value : CoreDatatype.RDF.values()) {
				map.put(value.getIri(), value.asOptional());
			}

			for (CoreDatatype value : CoreDatatype.GEO.values()) {
				map.put(value.getIri(), value.asOptional());
			}

			for (CoreDatatype value : CoreDatatype.XSD.values()) {
				map.put(value.getIri(), value.asOptional());
			}

			reverseLookup = Collections.unmodifiableMap(map);
		}

		return reverseLookup;
	}

	static class DatatypeIRI extends AbstractIRI {

		private static final long serialVersionUID = 169243624049169159L;

		private final String namespace;
		private final String localName;
		private final String stringValue;

		public DatatypeIRI(String namespace, String localName) {
			this.namespace = namespace;
			this.localName = localName;
			this.stringValue = namespace.concat(localName);
		}

		@Override
		public String stringValue() {
			return stringValue;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public String getLocalName() {
			return localName;
		}

	}
}
