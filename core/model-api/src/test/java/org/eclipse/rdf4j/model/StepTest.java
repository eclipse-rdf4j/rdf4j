package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.Step.format;
import static org.eclipse.rdf4j.model.Step.inverse;

import org.eclipse.rdf4j.model.base.AbstractIRI;
import org.junit.Test;

public class StepTest {

	private static final IRI iri = new AbstractIRI() {

		@Override
		public String getNamespace() {
			return "test";
		}

		@Override
		public String getLocalName() {
			return "x";
		}

	};

	@Test
	public void testCreation() {
		assertThat(iri.getIRI()).isEqualTo(iri);
		assertThat(iri.isInverse()).isFalse();
	}

	@Test
	public void testInverse() {
		assertThat(inverse(iri).isInverse()).isTrue();
		assertThat(inverse(inverse(iri)).isInverse()).isTrue();
		assertThat(inverse(iri).getIRI()).isEqualTo(iri);
	}

	@Test
	public void testFormat() {
		assertThat(format(iri)).isEqualTo(String.format("<%s>", iri.stringValue()));
		assertThat(format(inverse(iri))).isEqualTo(String.format("^<%s>", iri.stringValue()));
	}

}