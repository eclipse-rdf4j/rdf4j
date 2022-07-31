/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.base.InternedIRI;

/**
 * Utility methods related to RDF vocabularies.
 *
 * @author Alessandro Bollini
 * @implNote To be eventually removed or merged with {@code org.eclipse.rdf4j.model.util.Vocabularies}.
 * @since 3.5.0
 */
class Vocabularies {

	private Vocabularies() {
	}

	static Namespace createNamespace(String prefix, String namespace) {
		return new VocabularyNamespace(prefix, namespace);
	}

	private static class VocabularyNamespace extends AbstractNamespace {

		private final String prefix;
		private final String namespace;

		public VocabularyNamespace(String prefix, String namespace) {

			this.prefix = prefix;
			this.namespace = namespace;
		}

		@Override
		public String getPrefix() {
			return prefix;
		}

		@Override
		public String getName() {
			return namespace;
		}
	}

	static IRI createIRI(String namespace, String localName) {
		return new InternedIRI(namespace, localName);
	}

}
