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

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class DatatypeConstraintComponent extends AbstractSimpleConstraintComponent {

	private final CoreDatatype coreDatatype;
	private final IRI datatype;

	public DatatypeConstraintComponent(IRI datatype) {
		super();
		this.datatype = datatype;
		this.coreDatatype = CoreDatatype.from(datatype);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.DATATYPE, datatype);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.DatatypeConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new DatatypeConstraintComponent(datatype);
	}

	@Override
	Function<PlanNode, FilterPlanNode> getFilterAttacher(ConnectionsGroup connectionsGroup) {
		return (parent) -> new DatatypeFilter(parent, datatype, connectionsGroup);
	}

	@Override
	String getSparqlFilterExpression(Variable<Value> variable, boolean negated) {
		String checkDatatypeConformance = "<" + RSX.valueConformsToXsdDatatypeFunction + ">("
				+ variable.asSparqlVariable() + ", <"
				+ datatype + ">)";
		if (negated) {
			return "isLiteral(" + variable.asSparqlVariable() + ") && datatype(" + variable.asSparqlVariable() + ") = <"
					+ datatype + ">"
					+ (coreDatatype.isXSDDatatype() ? " && " + checkDatatypeConformance : "");
		} else {
			return "!isLiteral(" + variable.asSparqlVariable() + ") || datatype(" + variable.asSparqlVariable()
					+ ") != <" + datatype + ">"
					+ (coreDatatype.isXSDDatatype() ? " || !" + checkDatatypeConformance : "");
		}
	}

	// @formatter:off
	/*
	// This an attempt at an alternate approach using casting instead of a custom function. One issue with this is that we don't have an xsd:date constructor function which would be required to have parity with the reference SHACL validator.
	if (negated) {
		return "isLiteral(?" + varName + ") && datatype(?" + varName + ") = <" + datatype + ">" + (coreDatatype != null && coreDatatype.isPrimitiveDatatype() ? " && xsd:"+coreDatatype.getIri().getLocalName()+"(?"+varName+")":"");
	} else {
		return "!isLiteral(?" + varName + ") || datatype(?" + varName + ") != <" + datatype + "> "+ (coreDatatype != null && coreDatatype.isPrimitiveDatatype() ? " || !sameTerm(xsd:"+coreDatatype.getIri().getLocalName()+"(?"+varName+"), ?"+varName+")":"");
	}
	*/
	// @formatter:on

	@Override
	public List<Literal> getDefaultMessage() {
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DatatypeConstraintComponent that = (DatatypeConstraintComponent) o;

		return datatype.equals(that.datatype);
	}

	@Override
	public int hashCode() {
		return datatype.hashCode() + "DatatypeConstraintComponent".hashCode();
	}
}
