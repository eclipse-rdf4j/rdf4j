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
import java.util.Optional;

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

		// 2. Blank nodes
		boolean b1 = o1.isBNode();
		boolean b2 = o2.isBNode();
		if (b1 && b2) {
			return compareBNodes((BNode) o1, (BNode) o2);
		}
		if (b1) {
			return -1;
		}
		if (b2) {
			return 1;
		}

		// 3. IRIs
		boolean iri1 = o1.isIRI();
		boolean iri2 = o2.isIRI();
		if (iri1 && iri2) {
			return compareURIs((IRI) o1, (IRI) o2);
		}
		if (iri1) {
			return -1;
		}
		if (iri2) {
			return 1;
		}

		// 4. Literals
		boolean l1 = o1.isLiteral();
		boolean l2 = o2.isLiteral();
		if (l1 && l2) {
			return compareLiterals((Literal) o1, (Literal) o2);
		}
		if (l1) {
			return -1;
		}
		if (l2) {
			return 1;
		}

		// 5. RDF-star triples
		return compareTriples((Triple) o1, (Triple) o2);
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

	private int compareURIs(IRI leftURI, IRI rightURI) {
		return leftURI.toString().compareTo(rightURI.toString());
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
		int result;

		// FIXME: Confirm these rules work with RDF-1.1
		// Sort by datatype first, plain literals come before datatyped literals
		IRI leftDatatype = leftLit.getDatatype();
		IRI rightDatatype = rightLit.getDatatype();

		if (leftDatatype != null) {
			if (rightDatatype != null) {
				// Both literals have datatypes
				CoreDatatype.XSD leftXmlDatatype = leftLit.getCoreDatatype().asXSDDatatype().orElse(null);
				CoreDatatype.XSD rightXmlDatatype = rightLit.getCoreDatatype().asXSDDatatype().orElse(null);

				result = compareDatatypes(leftXmlDatatype, rightXmlDatatype, leftDatatype, rightDatatype);
				if (result != 0) {
					return result;
				}

			} else {
				return 1;
			}
		} else if (rightDatatype != null) {
			return -1;
		}

		// datatypes are equal or both literals are untyped; sort by language
		// tags, simple literals come before literals with language tags
		Optional<String> leftLanguage = leftLit.getLanguage();
		Optional<String> rightLanguage = rightLit.getLanguage();

		if (leftLanguage.isPresent()) {
			if (rightLanguage.isPresent()) {
				result = leftLanguage.get().compareTo(rightLanguage.get());
				if (result != 0) {
					return result;
				}
			} else {
				return 1;
			}
		} else if (rightLanguage.isPresent()) {
			return -1;
		}

		// Literals are equal as fas as their datatypes and language tags are
		// concerned, compare their labels
		return leftLit.getLabel().compareTo(rightLit.getLabel());
	}

	private int compareDatatypes(CoreDatatype.XSD leftDatatype, CoreDatatype.XSD rightDatatype, IRI leftDatatypeIRI,
			IRI rightDatatypeIRI) {
		if (leftDatatype != null && leftDatatype == rightDatatype) {
			return 0;
		} else if (leftDatatype != null && leftDatatype.isNumericDatatype()) {
			if (rightDatatype != null && rightDatatype.isNumericDatatype()) {
				// both are numeric datatypes
				return leftDatatype.compareTo(rightDatatype);
			} else {
				return -1;
			}
		} else if (rightDatatype != null && rightDatatype.isNumericDatatype()) {
			return 1;
		} else if (leftDatatype != null && leftDatatype.isCalendarDatatype()) {
			if (rightDatatype != null && rightDatatype.isCalendarDatatype()) {
				return leftDatatype.compareTo(rightDatatype);
			} else {
				return -1;
			}
		} else if (rightDatatype != null && rightDatatype.isCalendarDatatype()) {
			return 1;
		}

		if (leftDatatype != null && rightDatatype != null) {
			return leftDatatype.compareTo(rightDatatype);
		}

		// incompatible or unordered datatype
		return compareURIs(leftDatatypeIRI, rightDatatypeIRI);

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
