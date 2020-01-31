/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.builder;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * <p>
 * Interface for something that supports the ability to turn itself into a Sesame TupleExpr.
 * </p>
 * 
 * @author Michael Grove
 * @deprecated use {@link org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder} instead.
 */
@Deprecated
public interface SupportsExpr {

	TupleExpr expr();
}
