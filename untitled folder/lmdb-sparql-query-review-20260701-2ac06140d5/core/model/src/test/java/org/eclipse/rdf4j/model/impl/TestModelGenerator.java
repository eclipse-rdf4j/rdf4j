/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;

import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestSetGenerator;

/**
 * Creates Models, containing sample {@link Statement}s, for use in Java Collection Framework conformance testing.
 *
 * @author Jeen Broekstra
 * @see ModelCollectionTest
 */
public class TestModelGenerator implements TestSetGenerator<Statement> {

	private final ModelFactory mf;
	private final ValueFactory vf;
	private final Statement st0, st1, st2, st3, st4;

	public TestModelGenerator(ModelFactory factory) {
		super();
		this.mf = factory;
		this.vf = SimpleValueFactory.getInstance();
		this.st0 = vf.createStatement(vf.createIRI("foo:s0"), vf.createIRI("foo:p0"), vf.createIRI("foo:o0"));
		this.st1 = vf.createStatement(vf.createIRI("foo:s1"), vf.createIRI("foo:p1"), vf.createIRI("foo:o1"));
		this.st2 = vf.createStatement(vf.createIRI("foo:s2"), vf.createIRI("foo:p2"), vf.createIRI("foo:o2"));
		this.st3 = vf.createStatement(vf.createIRI("foo:s3"), vf.createIRI("foo:p3"), vf.createIRI("foo:o3"));
		this.st4 = vf.createStatement(vf.createIRI("foo:s4"), vf.createIRI("foo:p4"), vf.createIRI("foo:o4"));
	}

	@Override
	public SampleElements<Statement> samples() {
		return new SampleElements<>(st0, st1, st2, st3, st4);
	}

	@Override
	public Statement[] createArray(int length) {
		return new Statement[length];
	}

	@Override
	public Iterable<Statement> order(List<Statement> insertionOrder) {
		return insertionOrder;
	}

	@Override
	public Set<Statement> create(Object... elements) {
		Model m = mf.createEmptyModel();
		for (Object e : elements) {
			m.add((Statement) e);
		}

		return m;
	}

}
