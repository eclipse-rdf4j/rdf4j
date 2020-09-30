/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.Test;

/**
 * Abstract {@link IRI} test suite.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class IRITest {

	/**
	 * Creates a test IRI instance.
	 *
	 * @param namespace the namespace of the IRI
	 * @param localname the localname of the IRI
	 *
	 * @return a new instance of the concrete IRI class under test
	 */
	protected abstract IRI iri(String namespace, String localname);

	@Test
	public final void testConstructor() {

		final String namespace = "http://example.org/";
		final String localname = "iri";

		final IRI iri = iri(namespace, localname);

		assertThat(iri.getNamespace()).isEqualTo(namespace);
		assertThat(iri.getLocalName()).isEqualTo(localname);

		assertThatNullPointerException().isThrownBy(() -> iri(null, null));
		assertThatNullPointerException().isThrownBy(() -> iri(null, localname));
		assertThatNullPointerException().isThrownBy(() -> iri(namespace, null));
	}

	@Test
	public void testStringValue() {

		final String namespace = "http://example.org/";
		final String localname = "x";

		final IRI iri = iri(namespace, localname);

		assertThat(iri.stringValue()).isEqualTo(namespace + localname);
	}

	@Test
	public void testEquals() {

		final IRI x = iri("http://example.org/", "x");
		final IRI y = iri("http://example.org/", "Y");

		assertThat(x).isEqualTo(x);
		assertThat(x).isEqualTo(iri(x.getNamespace(), x.getLocalName()));

		assertThat(x).isNotEqualTo(null);
		assertThat(x).isNotEqualTo(new Object());
		assertThat(x).isNotEqualTo(y);

	}

	@Test
	public void testHashCode() {

		final IRI iri = iri("http://example.org/", "iri");

		assertThat(iri.hashCode()).isEqualTo(iri.stringValue().hashCode());
	}

}
