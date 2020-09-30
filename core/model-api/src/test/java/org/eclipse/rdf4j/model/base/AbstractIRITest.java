/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.IRITest;

/**
 * Unit tests for {@link AbstractIRI}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractIRITest extends IRITest {

	@Override
	protected IRI iri(final String namespace, final String localname) {
		return new TestIRI(namespace, localname);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static final class TestIRI extends AbstractIRI {

		private static final long serialVersionUID=5909565726259853948L;

		private static int split(String iri) {

			final int hash=iri.indexOf('#');

			if ( hash >= 0 ) {
				return hash;
			} else {

				final int slash=iri.lastIndexOf('/');

				if ( slash >= 0 ) {
					return slash;
				} else {

					final int colon=iri.lastIndexOf(':');

					if ( colon >= 0 ) {
						return colon;
					} else {

						throw new IllegalArgumentException("missing colon in absolute IRI");
					}

				}

			}

		}

		private final String namespace;
		private final String localname;

		TestIRI(String iri) {

			if ( iri == null ) {
				throw new NullPointerException("null iri");
			}

			final int split=split(iri);

			this.namespace=iri.substring(0, split+1);
			this.localname=iri.substring(split+1);
		}

		TestIRI(String namespace, String localname) {

			if ( namespace == null ) {
				throw new NullPointerException("null namespace");
			}

			if ( localname == null ) {
				throw new NullPointerException("null localname");
			}

			this.namespace=namespace;
			this.localname=localname;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public String getLocalName() {
			return localname;
		}

	}

}