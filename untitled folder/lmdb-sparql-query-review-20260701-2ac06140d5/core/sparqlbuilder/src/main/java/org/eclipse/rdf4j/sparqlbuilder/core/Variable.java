/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphName;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfSubject;

/**
 * A SPARQL query variable
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynVariables"> SPARQL Variable Syntax</a>
 */
public class Variable implements Projectable, RdfSubject, RdfPredicate, RdfObject, Operand, Orderable, Groupable,
		GraphName, Assignable {
	private final String varName;

	Variable(String varName) {
		this.varName = varName;
	}

	public String getVarName() {
		return varName;
	}

	@Override
	public String getQueryString() {
		return "?" + varName;
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
		if (varName == null) {
			if (other.varName != null) {
				return false;
			}
		} else if (!varName.equals(other.varName)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((varName == null) ? 0 : varName.hashCode());
		return result;
	}
}
