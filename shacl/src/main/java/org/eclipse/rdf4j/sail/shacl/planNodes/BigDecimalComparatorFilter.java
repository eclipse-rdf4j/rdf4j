/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * @author Håvard Ottestad
 */
public class BigDecimalComparatorFilter extends FilterPlanNode {

	Function<BigDecimal, Boolean> function;

	public BigDecimalComparatorFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, Function<BigDecimal, Boolean> function) {
		super(parent, trueNode, falseNode);
		this.function = function;
	}

	@Override
	boolean checkTuple(Tuple t) {
		Value literal = t.line.get(1);

		if(literal instanceof Literal){


			IRI datatype = ((Literal) literal).getDatatype();


			BigDecimal bigDecimal = ((Literal) literal).decimalValue();
			return function.apply(bigDecimal);
		}


		return false;
	}


	@Override
	public String toString() {
		return "BigDecimalComparatorFilter{" +
			"function=" + function +
			'}';
	}
}
