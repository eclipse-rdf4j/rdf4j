package org.eclipse.rdf4j.model;

/**
 * Generic property path.
 *
 * <p>
 * Represents a directed property path of unknown length between two {@linkplain Value values} in an RDF graph.
 * </p>
 *
 * @author Alessandro Bollini
 * @see <a href="https://www.w3.org/TR/sparql11-query/#propertypaths">SPARQL 1.1 Query Language – 9.1 Property Path
 *      Syntax</a>
 * @since 3.7.0
 */
public interface Link {

	/**
	 * Checks if this link is inverse.
	 *
	 * @return {@code true}, if this link is inverse; {@code false}, otherwise
	 */
	public boolean isInverse();

}
