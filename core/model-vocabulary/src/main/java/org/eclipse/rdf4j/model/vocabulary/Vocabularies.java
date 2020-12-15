/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.AbstractIRI;
import org.eclipse.rdf4j.model.base.AbstractNamespace;

/**
 * Utility methods related to RDF vocabularies.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 *
 * @implNote To be eventually removed or merged with {@code org.eclipse.rdf4j.model.util.Vocabularies}.
 */
class Vocabularies {

	static Namespace createNamespace(String prefix, String namespace) {
		return new AbstractNamespace() {

			private static final long serialVersionUID = 3913851292983866831L;

			@Override
			public String getPrefix() {
				return prefix;
			}

			@Override
			public String getName() {
				return namespace;
			}

		};
	}

	static IRI createIRI(String namespace, String localName) {
		return new AbstractIRI() {

			private static final long serialVersionUID = 1692436252019169159L;

			// ;( removing .toString() causes a 2x penalty in .equals() performance on Oracle JDK 1.8/11…

			private final String stringValue = (namespace + localName).toString();

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

		};
	}

	private Vocabularies() {
	}

}
