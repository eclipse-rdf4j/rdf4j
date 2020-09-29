/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.LiteralTest;
import org.eclipse.rdf4j.model.base.AbstractIRITest.TestIRI;

public class AbstractLiteralTest extends LiteralTest {

	@Override
	protected Literal literal(String label) {
		return new TestLiteral(label);
	}

	@Override
	protected Literal literal(String label, String language) {
		return new TestLiteral(label, language);
	}

	@Override
	protected Literal literal(String label, IRI datatype) {
		return new TestLiteral(label, datatype);
	}

	@Override
	protected IRI datatype(String iri) {
		return new TestIRI(iri);
	}

	private static final class TestLiteral extends AbstractLiteral {

		private static final long serialVersionUID = -19640527584237291L;

		private final String label;
		private final String language;
		private final IRI datatype;

		TestLiteral(String label) {

			if (label == null) {
				throw new NullPointerException("null label");
			}

			this.label = label;
			this.language = null;
			this.datatype = new TestIRI(XSD_STRING);
		}

		TestLiteral(String label, String language) {

			if (label == null) {
				throw new NullPointerException("null label");
			}

			if (language == null) {
				throw new NullPointerException("null language");
			}

			if (label.isEmpty()) {
				throw new IllegalArgumentException("empty language tag");
			}

			this.label = label;
			this.language = language;
			this.datatype = new TestIRI(RDF_LANG_STRING);
		}

		TestLiteral(String label, IRI datatype) {

			if (label == null) {
				throw new NullPointerException("null label");
			}

			if (datatype != null && datatype.stringValue().equals(RDF_LANG_STRING)) {
				throw new IllegalArgumentException("reserved rdf:langString datatype");
			}

			this.label = label;
			this.language = null;
			this.datatype = datatype != null ? datatype : new TestIRI(XSD_STRING);
		}

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public Optional<String> getLanguage() {
			return Optional.ofNullable(language);
		}

		@Override
		public IRI getDatatype() {
			return datatype;
		}

	}

}