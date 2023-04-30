/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import java.util.Comparator;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * A comparator that compares values according the SPARQL value ordering as specified in
 * <A href="http://www.w3.org/TR/rdf-sparql-query/#modOrderBy">SPARQL Query Language for RDF</a>.
 *
 * @author james
 * @author Arjohn Kampman
 */
public class ValueComparator implements Comparator<Value> {

	private boolean strict = true;

	@Override
	public int compare(Value o1, Value o2) {
		// check equality
		if (o1 == o2) {
			return 0;
		}

		// 1. (Lowest) no value assigned to the variable
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}

		if(o1.getClass() == o2.getClass()){
			return compareSameTypes(o1, o2, o1.getValueType());
		}

		Value.Type o1Type = o1.getValueType();
		Value.Type o2Type = o2.getValueType();

		if (o1Type == o2Type) {
			return compareSameTypes(o1, o2, o1Type);
		}

		return compareDifferentTypes(o1Type, o2Type);
	}

	private int compareSameTypes(Value o1, Value o2, Value.Type type) {
		switch (type) {
		case BNODE:
			return compareBNodes((BNode) o1, (BNode) o2);
		case IRI:
			return compareIRIs((IRI) o1, (IRI) o2);
		case LITERAL:
			return compareLiterals((Literal) o1, (Literal) o2);
		default:
			return compareTriples((Triple) o1, (Triple) o2);
		}
	}

	private static int compareDifferentTypes(Value.Type o1Type, Value.Type o2Type) {
		/*
		 * Using ordinal is an optimization written by ChatGPT 4 instead of the following code:
		 *
		 * if (o1Type == Value.Type.BNODE) { return -1; } if (o2Type == Value.Type.BNODE) { return 1; } if (o1Type ==
		 * Value.Type.IRI) { return -1; } if (o2Type == Value.Type.IRI) { return 1; } if (o1Type == Value.Type.LITERAL)
		 * { return -1; } return 1;
		 */
		return o1Type.ordinal() < o2Type.ordinal() ? -1 : 1;
	}

	public void setStrict(boolean flag) {
		this.strict = flag;
	}

	public boolean isStrict() {
		return this.strict;
	}

	private int compareBNodes(BNode leftBNode, BNode rightBNode) {
		return leftBNode.getID().compareTo(rightBNode.getID());
	}

	private int compareIRIs(IRI leftURI, IRI rightURI) {
		return leftURI.stringValue().compareTo(rightURI.stringValue());
	}

	private int compareLiterals(Literal leftLit, Literal rightLit) {
		// Additional constraint for ORDER BY: "A plain literal is lower
		// than an RDF literal with type CoreDatatype.XSD:string of the same lexical
		// form."

		if (!(QueryEvaluationUtility.isPlainLiteral(leftLit) || QueryEvaluationUtility.isPlainLiteral(rightLit))) {
			QueryEvaluationUtility.Order order = compareNonPlainLiterals(leftLit, rightLit);
			if (order.isValid()) {
				return order.asInt();
			}
			if (order == QueryEvaluationUtility.Order.illegalArgument) {
				throw new IllegalStateException();
			}
		}

		return comparePlainLiterals(leftLit, rightLit);
	}

	private QueryEvaluationUtility.Order compareNonPlainLiterals(Literal leftLit, Literal rightLit) {

		QueryEvaluationUtility.Order order = QueryEvaluationUtility.compareLiterals(leftLit, rightLit, strict);

		if (order == QueryEvaluationUtility.Order.notEqual) {
			return QueryEvaluationUtility.Order.smaller;
		}

		return order;
	}

	private int comparePlainLiterals(Literal leftLit, Literal rightLit) {
		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		if (leftDatatype != rightDatatype) {
			if (leftDatatype != null && rightDatatype != null) {
				// Both literals have datatypes
				int result = compareDatatypes(leftLit.getCoreDatatype(), rightLit.getCoreDatatype(), leftDatatype,
						rightDatatype);
				if (result != 0) {
					return result;
				}
			} else {
				return leftDatatype == null ? -1 : 1;
			}
		}

		boolean leftIsLang = leftLit.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
		boolean rightIsLang = rightLit.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;

		if (leftIsLang && rightIsLang) {
			int result = leftLit.getLanguage().get().compareTo(rightLit.getLanguage().get());
			if (result != 0) {
				return result;
			}
		} else if (leftIsLang || rightIsLang) {
			return leftIsLang ? 1 : -1;
		}

		// Literals are equal as far as their datatypes and language tags are concerned, compare their labels
		return leftLit.getLabel().compareTo(rightLit.getLabel());
	}

	private int compareDatatypes(CoreDatatype leftCoreDatatype, CoreDatatype rightCoreDatatype, IRI leftDatatypeIRI,
			IRI rightDatatypeIRI) {

		if (leftCoreDatatype == CoreDatatype.NONE || rightCoreDatatype == CoreDatatype.NONE) {
			return compareIRIs(leftDatatypeIRI, rightDatatypeIRI);
		}

		if (leftCoreDatatype == rightCoreDatatype) {
			return 0;
		}

		CoreDatatype.XSD leftXsdDatatype = leftCoreDatatype.asXSDDatatypeOrNull();
		CoreDatatype.XSD rightXsdDatatype = rightCoreDatatype.asXSDDatatypeOrNull();

		boolean leftNumeric = leftXsdDatatype != null && leftXsdDatatype.isNumericDatatype();
		boolean rightNumeric = rightXsdDatatype != null && rightXsdDatatype.isNumericDatatype();
		boolean leftCalendar = leftXsdDatatype != null && leftXsdDatatype.isCalendarDatatype();
		boolean rightCalendar = rightXsdDatatype != null && rightXsdDatatype.isCalendarDatatype();

		if (leftNumeric && rightNumeric || leftCalendar && rightCalendar) {
			return CoreDatatype.compare(leftCoreDatatype, rightCoreDatatype);
		}

		if (leftNumeric || leftCalendar) {
			return -1;
		}

		if (rightNumeric || rightCalendar) {
			return 1;
		}

		return CoreDatatype.compare(leftCoreDatatype, rightCoreDatatype);
	}

	private int compareTriples(Triple leftTriple, Triple rightTriple) {
		int c = compare(leftTriple.getSubject(), rightTriple.getSubject());
		if (c == 0) {
			c = compare(leftTriple.getPredicate(), rightTriple.getPredicate());
			if (c == 0) {
				c = compare(leftTriple.getObject(), rightTriple.getObject());
			}
		}
		return c;
	}
}
