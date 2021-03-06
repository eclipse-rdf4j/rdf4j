package org.eclipse.rdf4j.model;

import static org.eclipse.rdf4j.model.Step.format;

/**
 * Default single step property path implementations.
 *
 * @author Alessandro Bollini
 * @since 3.7.0
 */
public final class StepDefault implements Step {

	private final IRI iri;

	private final boolean inverse;

	StepDefault(IRI iri, boolean inverse) {

		if (!inverse) {
			throw new AssertionError("plain direct steps should be represented with an IRI");
		}

		this.inverse = inverse;
		this.iri = iri;
	}

	@Override
	public boolean isInverse() {
		return inverse;
	}

	@Override
	public IRI getIRI() {
		return iri;
	}

	@Override
	public boolean equals(Object object) {
		return this == object || object instanceof Step
				&& iri.equals(((Step) object).getIRI())
				&& inverse == ((Link) object).isInverse();
	}

	@Override
	public int hashCode() {
		return iri.hashCode()
				^ Boolean.hashCode(inverse);
	}

	@Override
	public String toString() {
		return format(this);
	}

}
