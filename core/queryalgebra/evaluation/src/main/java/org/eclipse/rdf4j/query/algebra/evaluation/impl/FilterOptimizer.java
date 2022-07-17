/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Filter;

/**
 * Optimizes a query model by pushing {@link Filter}s as far down in the model tree as possible.
 *
 * To make the first optimization succeed more often it splits filters which contains {@link And} conditions.
 *
 * <code>
 * SELECT * WHERE {
 * ?s ?p ?o .
 * ?s ?p ?o2  .
 * FILTER(?o > '2'^^xsd:int && ?o2 < '4'^^xsd:int)
 * }
 * </code> May be more efficient when decomposed into <code>
 * SELECT * WHERE {
 * ?s ?p ?o .
 * FILTER(?o > '2'^^xsd:int)
 * ?s ?p ?o2  .
 * FILTER(?o2 < '4'^^xsd:int)
 * }
 * </code>
 *
 * Then it optimizes a query model by merging adjacent {@link Filter}s. e.g. <code>
 * SELECT * WHERE {
 *  ?s ?p ?o .
 *  FILTER(?o > 2) .
 *  FILTER(?o < 4) .
 *  }
 * </code> may be merged into <code>
 * SELECT * WHERE {
 *   ?s ?p ?o .
 *   FILTER(?o > 2 && ?o < 4) . }
 *  </code>
 *
 * This optimization allows for sharing evaluation costs in the future and removes an iterator. This is done as a second
 * step to not break the first optimization. In the case that the splitting was done but did not help it is now undone.
 *
 * @author Arjohn Kampman
 * @author Jerven Bolleman
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.FilterOptimizer} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class FilterOptimizer extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.FilterOptimizer {

}
