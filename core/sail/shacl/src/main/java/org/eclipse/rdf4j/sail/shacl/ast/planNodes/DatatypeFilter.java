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
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class DatatypeFilter extends FilterPlanNode {

	private static final Logger logger = LoggerFactory.getLogger(DatatypeFilter.class);

	private final IRI datatype;
	private final CoreDatatype.XSD xsdDatatype;
	private StackTraceElement[] stackTrace;

	public DatatypeFilter(PlanNode parent, IRI datatype, ConnectionsGroup connectionsGroup) {
		super(parent, connectionsGroup);
		this.datatype = datatype;
		this.xsdDatatype = CoreDatatype.from(datatype).asXSDDatatype().orElse(null);
	}

	@Override
	boolean checkTuple(Reference t) {
		if (!(t.get().getValue().isLiteral())) {
			logger.debug("Tuple rejected because it's not a literal. Tuple: {}", t);
			return false;
		}

		Literal literal = (Literal) t.get().getValue();
		if (xsdDatatype != null) {
			if (literal.getCoreDatatype() == xsdDatatype) {
				boolean isValid = XMLDatatypeUtil.isValidValue(literal.stringValue(), xsdDatatype);
				if (isValid) {
					logger.trace(
							"Tuple accepted because its literal value is valid according to the rules for the datatype in the XSD spec. Actual datatype: {}, Expected datatype: {}, Tuple: {}",
							literal.getDatatype(), xsdDatatype, t);
				} else {
					logger.debug(
							"Tuple rejected because its literal value is invalid according to the rules for the datatype in the XSD spec. Actual datatype: {}, Expected datatype: {}, Tuple: {}",
							literal.getDatatype(), xsdDatatype, t);
				}
				return isValid;
			}
			logger.debug(
					"Tuple rejected because literal's core datatype is not the expected datatype. Actual datatype: {}, Expected datatype: {}, Tuple: {}",
					literal.getDatatype(), xsdDatatype, t);
			return false;
		} else {
			boolean isEqual = literal.getDatatype() == datatype || literal.getDatatype().equals(datatype);
			if (isEqual) {
				logger.trace(
						"Tuple accepted because literal's datatype is equal to the expected datatype. Actual datatype: {}, Expected datatype: {}, Tuple: {}",
						literal.getDatatype(), datatype, t);
			} else {
				logger.debug(
						"Tuple rejected because literal's datatype is not equal to the expected datatype. Actual datatype: {}, Expected datatype: {}, Tuple: {}",
						literal.getDatatype(), datatype, t);
			}
			return isEqual;
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
