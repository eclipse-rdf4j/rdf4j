/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

public class AnnotationTripleRef extends ReifiedTripleRef {

	public AnnotationTripleRef() {
	}

	public AnnotationTripleRef(Var subjectVar, Var predicateVar, Var objectVar, Var exprVar, Var reifVar) {
		super(subjectVar, predicateVar, objectVar, exprVar, reifVar);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AnnotationTripleRef) {
			AnnotationTripleRef o = (AnnotationTripleRef) other;
			return getSubjectVar().equals(o.getSubjectVar()) && getPredicateVar().equals(o.getPredicateVar())
					&& getObjectVar().equals(o.getObjectVar()) && getReifVar().equals(o.getReifVar());
		}
		return false;
	}

	@Override
	public TripleRef clone() {
		return super.clone();
	}
}
