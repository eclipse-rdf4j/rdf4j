/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Utility methods for {@link TupleExpr} objects.
 * 
 * @author Jeen Broekstra
 */
public class TupleExprs {

	/**
	 * Verifies if the supplied {@link TupleExpr} contains a {@link Projection}.
	 * If the supplied TupleExpr is a {@link Join} or contains a {@link Join},
	 * projections inside that Join's arguments will not be taken into
	 * account.
	 * 
	 * @param t
	 *        a tuple expression.
	 * @return <code>true</code> if the TupleExpr contains a projection (outside
	 *         of a Join), <code>false</code> otherwise.
	 */
	public static boolean containsProjection(TupleExpr t) {
		@SuppressWarnings("serial")
		class VisitException extends Exception {
			VisitException() {
				super(null, null, false, false);
			}
		}
		final boolean[] result = new boolean[1];
		try {
			t.visit(new AbstractQueryModelVisitor<VisitException>() {

				@Override
				public void meet(Projection node)
					throws VisitException
				{
					result[0] = true;
					throw new VisitException();
				}

				@Override
				public void meet(Join node)
					throws VisitException
				{
					// projections already inside a Join need not be
					// taken into account
					result[0] = false;
					throw new VisitException();
				}
			});
		}
		catch (VisitException ex) {
			// Do nothing. We have thrown this exception on the first
			// meeting of Projection or Join.
		}
		return result[0];
	}

	/**
	 * Creates an (anonymous) Var representing a constant value. The variable
	 * name will be derived from the actual value to guarantee uniqueness.
	 * 
	 * @param value
	 * @return an (anonymous) Var representing a constant value.
	 */
	public static Var createConstVar(Value value) {
		String varName = getConstVarName(value);
		Var var = new Var(varName);
		var.setConstant(true);
		var.setAnonymous(true);
		var.setValue(value);
		return var;
	}

	public static String getConstVarName(Value value) {
		if (value == null) {
			throw new IllegalArgumentException("value can not be null");
		}

		// We use toHexString to get a more compact stringrep.
		String uniqueStringForValue = Integer.toHexString(value.stringValue().hashCode());

		if (value instanceof Literal) {
			uniqueStringForValue += "_lit";

			// we need to append datatype and/or language tag to ensure a unique
			// var name (see SES-1927)
			Literal lit = (Literal)value;
			if (lit.getDatatype() != null) {
				uniqueStringForValue += "_" + Integer.toHexString(lit.getDatatype().hashCode());
			}
			if (lit.getLanguage() != null) {
				uniqueStringForValue += "_" + Integer.toHexString(lit.getLanguage().hashCode());
			}
		}
		else if (value instanceof BNode) {
			uniqueStringForValue += "_node";
		}
		else {
			uniqueStringForValue += "_uri";
		}

		return "_const_" + uniqueStringForValue;
	}
}
