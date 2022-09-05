/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;

/**
 * @author Håvard Ottestad
 */
public class DatatypeFilter extends FilterPlanNode {

	private final IRI datatype;
	private final CoreDatatype.XSD xsdDatatype;
	private StackTraceElement[] stackTrace;

	public DatatypeFilter(PlanNode parent, IRI datatype) {
		super(parent);
		this.datatype = datatype;
		this.xsdDatatype = CoreDatatype.from(datatype).asXSDDatatype().orElse(null);
//		stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		if (!(t.getValue().isLiteral())) {
			return false;
		}

		Literal literal = (Literal) t.getValue();
		if (xsdDatatype != null) {
			if (literal.getCoreDatatype() == xsdDatatype) {
				boolean validValue = XMLDatatypeUtil.isValidValue(literal.stringValue(), xsdDatatype);
				return validValue;
			}
			return false;
		} else {
			return literal.getDatatype() == datatype || literal.getDatatype().equals(datatype);
		}
	}

	@Override
	public String toString() {
		return "DatatypeFilter{" + "datatype=" + Formatter.prefix(datatype) + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		DatatypeFilter that = (DatatypeFilter) o;
		return datatype.equals(that.datatype);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), datatype);
	}
}
