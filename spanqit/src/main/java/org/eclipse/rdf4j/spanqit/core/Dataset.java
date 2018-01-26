package org.eclipse.rdf4j.spanqit.core;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

/**
 * A SPARQL dataset specification
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#rdfDataset">
 *      RDF Datasets</a>
 */
public class Dataset extends StandardQueryElementCollection<From> {
	/**
	 * Add graph references to this dataset
	 * 
	 * @param graphs
	 *            the datasets to add
	 * @return this object
	 */
	public Dataset from(From... graphs) {
		addElements(graphs);

		return this;
	}

	/**
	 * Add unnamed graph references to this dataset
	 * 
	 * @param iris the IRI's of the graphs to add
	 * @return this
	 */
	public Dataset from(Iri... iris) {
		addElements(Spanqit::from, iris);

		return this;
	}
}