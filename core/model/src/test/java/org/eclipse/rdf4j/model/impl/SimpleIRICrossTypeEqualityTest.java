/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

/**
 * Verifies cross-type equality between {@link SimpleIRI} and a third-party {@link IRI} implementation that does not
 * override {@link Object#equals(Object)} or {@link Object#hashCode()}.
 */
public class SimpleIRICrossTypeEqualityTest {

	@Test
	public void simpleIriEqualsThirdPartyIriByStringValue() {
		IRI simple = new SimpleIRI("http://example.org/x");
		IRI thirdParty = new ThirdPartyIri("http://example.org/", "x");

		// Historical behavior: SimpleIRI compares by string value against any IRI implementation.
		assertThat(simple).isEqualTo(thirdParty);

		// Note: The reverse direction is deliberately not asserted here. A third-party
		// implementation may rely on Object.equals (identity) and thus not be symmetric.
	}

	/**
	 * Minimal third-party IRI implementation: relies on Object.equals/hashCode (identity), only implements the methods
	 * required by the {@link IRI} contract for namespace/localname.
	 */
	private static final class ThirdPartyIri implements IRI, Serializable {
		private static final long serialVersionUID = 1L;

		private final String namespace;
		private final String localname;

		ThirdPartyIri(String namespace, String localname) {
			this.namespace = namespace;
			this.localname = localname;
		}

		@Override
		public String getNamespace() {
			return namespace;
		}

		@Override
		public String getLocalName() {
			return localname;
		}

		@Override
		public String stringValue() {
			return namespace + localname;
		}
	}
}
