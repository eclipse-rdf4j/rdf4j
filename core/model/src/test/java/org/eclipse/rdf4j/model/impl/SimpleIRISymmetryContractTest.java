/**
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

/**
 * Reproduces equals() symmetry issue for SimpleIRI with third-party IRI implementations that rely on Object.equals.
 */
public class SimpleIRISymmetryContractTest {

	private static final class ThirdPartyIri implements IRI, Serializable {
		private static final long serialVersionUID = 1L;
		private final String ns;
		private final String ln;

		ThirdPartyIri(String ns, String ln) {
			this.ns = ns;
			this.ln = ln;
		}

		@Override
		public String getNamespace() {
			return ns;
		}

		@Override
		public String getLocalName() {
			return ln;
		}

		@Override
		public String stringValue() {
			return ns + ln;
		}
	}

	@Test
	public void equalsMustBeSymmetric() {
		IRI simple = new SimpleIRI("http://example.org/x");
		IRI third = new ThirdPartyIri("http://example.org/", "x");

		boolean a = simple.equals(third);
		boolean b = third.equals(simple);

		// Contract: equals must be symmetric
		assertThat(a).as("SimpleIRI.equals(third) must equal third.equals(SimpleIRI)").isEqualTo(b);
	}
}
