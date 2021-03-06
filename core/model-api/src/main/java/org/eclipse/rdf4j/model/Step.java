package org.eclipse.rdf4j.model;

/**
 * Single step property path.
 *
 * <p>
 * Represents a directed single step property path between two {@linkplain Value values} in an RDF graph.
 * </p>
 *
 * @implNote {@link #equals(Object)}/{@link #hashCode()} contracts conflicts with those specified by {@link IRI}, which
 *           extends this interface: in order to preserve equality consistency, plain direct steps must be represented
 *           only with IRIs; concrete classes implementing this interface must ensure that no instance is created with a
 *           {@code false} {@link #isInverse()} value.
 * 
 * @author Alessandro Bollini
 * @see <a href="https://www.w3.org/TR/sparql11-query/#propertypaths">SPARQL 1.1 Query Language – 9.1 Property Path
 *      Syntax</a>
 * @since 3.7.0
 */
public interface Step extends Link {

	/**
	 * Creates an inverse step.
	 *
	 * @param step the step to be inverted
	 *
	 * @return a new inverse step derived from {@code step}
	 *
	 * @throws NullPointerException if {@code step} is null
	 */
	public static Step inverse(Step step) {

		if (step == null) {
			throw new NullPointerException("null step");
		}

		return new StepDefault(step.getIRI(), true);
	}

	/**
	 * Converts a step to a textual representation.
	 *
	 * @param step the step to be formatted
	 *
	 * @return a SPARQL expression representing {@code step} with a full IRI
	 *
	 * @throws NullPointerException if {@code step} is null
	 */
	public static String format(Step step) {

		if (step == null) {
			throw new NullPointerException("null step");
		}

		return (step.isInverse() ? "^<" : "<") + step.getIRI().stringValue() + ">";
	}

	/**
	 * Retrieves the IRI of this predicate path step.
	 *
	 * @return the IRI of this step
	 */
	public IRI getIRI();

	/**
	 * Checks if this step is equal to a reference object.
	 *
	 * @param object the reference object
	 *
	 * @return {@code true}, if the reference object is an instance of {@code Step} and the {@link #getIRI()} value of
	 *         this step and of the reference object are equal to each other; {@code false}, otherwise
	 *
	 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
	 *           {@code equals(Object)} method must be implemented exactly as described in this specs.
	 */
	public boolean equals(Object object);

	/**
	 * Computes the hash code of this step.
	 *
	 * @return a hash code for this step computed as:<br>
	 *
	 *         {@link #getIRI()}{@code .hashCode()}<br>
	 *         {@code ^Boolean.hashCode(}{@link #isInverse()}{@code )}
	 *
	 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
	 *           {@code hashCode()} method must be implemented exactly as described in this specs.
	 */
	public int hashCode();

}
