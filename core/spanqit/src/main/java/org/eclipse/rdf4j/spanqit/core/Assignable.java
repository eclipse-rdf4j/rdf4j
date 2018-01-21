package org.eclipse.rdf4j.spanqit.core;

/**
 * A marker interface to denote objects which are bind-able in
 * a SPARQL assignment expression. 
 * 
 * @see <a
 * 		 href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#assignment">
 * 			SPARQL Assignments
 * 		</a>
 *
 */
public interface Assignable extends QueryElement {
	/**
	 * Create a SPARQL assignment from this object
	 * 
	 * @param var
	 *            the variable to bind the expression value to
	 * @return an Assignment object
	 */
	default public Assignment as(Variable var) {
		return Spanqit.as(this, var);
	}
}