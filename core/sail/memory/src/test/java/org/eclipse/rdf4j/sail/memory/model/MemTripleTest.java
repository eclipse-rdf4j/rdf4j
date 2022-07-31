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
package org.eclipse.rdf4j.sail.memory.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.impl.SimpleTriple;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the memory-specific implementation of the {@link Triple} interface.
 *
 * @author Jeen Broekstra
 *
 */
public class MemTripleTest {

	private MemTriple triple;
	private MemIRI subject, predicate, object;

	private IRI s1, p1, o1;

	@BeforeEach
	public void setUp() throws Exception {

		SimpleValueFactory svf = SimpleValueFactory.getInstance();

		s1 = svf.createIRI("foo:s1");
		p1 = svf.createIRI("foo:p1");
		o1 = svf.createIRI("foo:o1");

		subject = new MemIRI(this, s1.getNamespace(), s1.getLocalName());
		predicate = new MemIRI(this, p1.getNamespace(), p1.getLocalName());
		object = new MemIRI(this, o1.getNamespace(), o1.getLocalName());
		triple = new MemTriple(this, subject, predicate, object);
	}

	@Test
	public void testStringValue() {
		assertThat(triple.stringValue())
				.startsWith("<<")
				.contains(s1.stringValue())
				.contains(p1.stringValue())
				.contains(o1.stringValue())
				.endsWith(">>");
	}

	@Test
	public void testEquals() {
		SimpleTriple equalTriple = (SimpleTriple) SimpleValueFactory.getInstance().createTriple(s1, p1, o1);
		assertThat(triple).isEqualTo(equalTriple);

		SimpleTriple unequalTriple = (SimpleTriple) SimpleValueFactory.getInstance().createTriple(s1, o1, p1);
		assertThat(triple).isNotEqualTo(unequalTriple);
	}

}
