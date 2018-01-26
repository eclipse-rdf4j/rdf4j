package org.eclipse.rdf4j.spanqit.core;

import org.eclipse.rdf4j.spanqit.constraint.Operand;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphName;
import org.eclipse.rdf4j.spanqit.rdf.RdfObject;
import org.eclipse.rdf4j.spanqit.rdf.RdfPredicate;
import org.eclipse.rdf4j.spanqit.rdf.RdfSubject;

/**
 * A SPARQL query variable
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynVariables">
 *      SPARQL Variable Syntax</a>
 */
public class Variable implements Projectable, RdfSubject,
        RdfPredicate, RdfObject, Operand, Orderable,
		Groupable, GraphName, Assignable {
	private String alias;

	Variable(String varName) {
		this.alias = varName;
	}

	@Override
	public String getQueryString() {
		return "?" + alias;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Variable)) {
			return false;
		}

		Variable other = (Variable) obj;
		if (alias == null) {
			if (other.alias != null) {
				return false;
			}
		} else if (!alias.equals(other.alias)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		return result;
	}
}