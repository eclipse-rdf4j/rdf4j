/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;

/**
 * @author Håvard Ottestad
 */
public class LiteralComparatorFilter extends FilterPlanNode {

	private final Literal compareTo;
	private final Function<Integer, Boolean> function;
	private final boolean numericDatatype;
	private final boolean calendarDatatype;
	private final boolean durationDatatype;
	private final boolean booleanDatatype;
	private final boolean timeDatatype;
	private final boolean dateDatatype;

	public LiteralComparatorFilter(PlanNode parent, Literal compareTo, Function<Integer, Boolean> function) {
		super(parent);
		this.function = function;
		this.compareTo = compareTo;
		IRI datatype = compareTo.getDatatype();
		numericDatatype = XMLDatatypeUtil.isNumericDatatype(datatype);
		calendarDatatype = XMLDatatypeUtil.isCalendarDatatype(datatype);
		durationDatatype = XMLDatatypeUtil.isDurationDatatype(datatype);
		booleanDatatype = XSD.BOOLEAN.equals(datatype);
		timeDatatype = XSD.TIME.equals(datatype);
		dateDatatype = XSD.DATE.equals(datatype);

	}

	@Override
	boolean checkTuple(Tuple t) {
		Value literal = t.getLine().get(1);

		if (literal instanceof Literal) {

			IRI datatype = ((Literal) literal).getDatatype();

			if (datatypesMatch(datatype)) {

				if (dateDatatype && XSD.DATETIME.equals(datatype)) {
					literal = SimpleValueFactory.getInstance()
							.createLiteral(literal.stringValue().split("T")[0], XSD.DATE);
				}

				int compare = new ValueComparator().compare(compareTo, literal);

				return function.apply(compare);
			}

		}

		return false;
	}

	private boolean datatypesMatch(IRI datatype) {
		return (numericDatatype && XMLDatatypeUtil.isNumericDatatype(datatype))
				|| (calendarDatatype && XMLDatatypeUtil.isCalendarDatatype(datatype)
						&& (timeDatatype || !XSD.TIME.equals(datatype)))
				|| (durationDatatype && XMLDatatypeUtil.isDurationDatatype(datatype))
				|| (booleanDatatype && XSD.BOOLEAN.equals(datatype));

	}

	@Override
	public String toString() {
		return "LiteralComparatorFilter{" + "function=" + function + '}';
	}
}
