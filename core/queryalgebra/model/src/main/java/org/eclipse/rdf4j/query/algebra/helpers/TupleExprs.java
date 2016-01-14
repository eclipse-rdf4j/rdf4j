/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Utility methods for {@link TupleExpr} objects.
 * 
 * @author Jeen Broekstra
 * @since 2.7.3
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
	 * @since 2.7.3
	 */
	public static boolean containsProjection(TupleExpr t) {
		@SuppressWarnings("serial")
		class VisitException extends Exception {
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
}
