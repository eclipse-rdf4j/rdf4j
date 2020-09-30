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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.StatementTest;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractIRITest.TestIRI;

/**
 * Unit tests for {@link AbstractStatement}.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public class AbstractStatementTest extends StatementTest {

	@Override
	protected Statement statement(Resource subject, IRI predicate, Value object, Resource context) {
		return new TestStatement(subject, predicate, object, context);
	}

	@Override
	protected IRI iri(String iri) {
		return new TestIRI(iri);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class TestStatement extends AbstractStatement {

		private static final long serialVersionUID = -4116676621136121342L;

		private final Resource subject;
		private final IRI predicate;
		private final Value object;
		private final Resource context;

		TestStatement(Resource subject, IRI predicate, Value object, Resource context) {

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
			this.context = context;
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

		@Override
		public Resource getContext() {
			return context;
		}

	}
}