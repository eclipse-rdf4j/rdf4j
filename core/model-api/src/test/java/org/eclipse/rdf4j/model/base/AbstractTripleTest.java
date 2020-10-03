/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.TripleTest;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractIRITest.TestIRI;

/**
 * Unit tests for {@link AbstractTriple}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractTripleTest extends TripleTest {

	@Override
	protected Triple triple(Resource subject, IRI predicate, Value object) {
		return new TestTriple(subject, predicate, object);
	}

	@Override
	protected IRI iri(String iri) {
		return new TestIRI(iri);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class TestTriple extends AbstractTriple {

		private static final long serialVersionUID = 7822116805598041700L;

		private final Resource subject;
		private final IRI predicate;
		private final Value object;

		TestTriple(Resource subject, IRI predicate, Value object) {

			if (subject == null) {
				throw new NullPointerException("null subject");
			}

			if (predicate == null) {
				throw new NullPointerException("null predicate");
			}

			if (object == null) {
				throw new NullPointerException("null object");
			}

			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
		}

		@Override
		public Resource getSubject() {
			return subject;
		}

		@Override
		public IRI getPredicate() {
			return predicate;
		}

		@Override
		public Value getObject() {
			return object;
		}
	}
}