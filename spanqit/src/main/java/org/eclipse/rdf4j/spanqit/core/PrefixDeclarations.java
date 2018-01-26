package org.eclipse.rdf4j.spanqit.core;

/**
 * A collection of SPARQL Prefix declarations
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#prefNames">
 *      SPARQL Prefix</a>
 */
public class PrefixDeclarations extends StandardQueryElementCollection<Prefix> {
	/**
	 * Add prefix declarations to this collection
	 * 
	 * @param prefixes
	 * @return this
	 */
	public PrefixDeclarations addPrefix(Prefix... prefixes) {
		addElements(prefixes);
		
		return this;
	}
}