/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import org.eclipse.rdf4j.sparqlbuilder.core.GraphTemplate;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.core.Projection;
import org.eclipse.rdf4j.sparqlbuilder.core.TriplesTemplate;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

/**
 * A class with static methods to create SPARQL queries
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/"> SPARQL Query Language</a>
 */
public class Queries {
	// prevent instantiation of this class
	private Queries() {
	}

	/**
	 * Create a SPARQL Select query
	 *
	 * @param projectables the initial set of {@link Projectable}(s), if any, to select
	 *
	 * @return a new {@link SelectQuery}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SPARQL Select Query</a>
	 */
	public static SelectQuery SELECT(Projectable... projectables) {
		return new SelectQuery().select(projectables);
	}

	/**
	 * Create a SPARQL Select query
	 *
	 * @param select the {@link Projection} to set initially
	 * @return a new {@link SelectQuery}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#select">SPARQL Select Query</a>
	 */
	public static SelectQuery SELECT(Projection select) {
		return new SelectQuery().select(select);
	}

	/**
	 * Create a SPARQL Construct query
	 *
	 * @param patterns the initial set of {@link TriplePattern}(s), if any, to construct
	 * @return a new {@link ConstructQuery}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">SPARQL Construct Query</a>
	 */
	public static ConstructQuery CONSTRUCT(TriplePattern... patterns) {
		return new ConstructQuery().construct(patterns);
	}

	/**
	 * Create a SPARQL Construct query
	 *
	 * @param construct the {@link GraphTemplate} to set initially
	 * @return a new {@link ConstructQuery}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">SPARQL Construct Query</a>
	 */
	public static ConstructQuery CONSTRUCT(GraphTemplate construct) {
		return new ConstructQuery().construct(construct);
	}

	/**
	 * Create a SPARQL INSERT DATA query
	 *
	 * @param triples the initial set of {@link TriplePattern}(s), if any, to use
	 *
	 * @return a new {@link InsertDataQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#insertData"> SPARQL INSERT DATA Query</a>
	 */
	public static InsertDataQuery INSERT_DATA(TriplePattern... triples) {
		return new InsertDataQuery().insertData(triples);
	}

	/**
	 * Create a SPARQL INSERT DATA query
	 *
	 * @param triplesTemplate the {@link TriplesTemplate} to set initially
	 *
	 * @return a new {@link InsertDataQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#insertData"> SPARQL INSERT DATA Query</a>
	 */
	public static InsertDataQuery INSERT_DATA(TriplesTemplate triplesTemplate) {
		return new InsertDataQuery().insertData(triplesTemplate);
	}

	/**
	 * Create a SPARQL DELETE DATA query
	 *
	 * @param triples the initial set of {@link TriplePattern}(s), if any, to use
	 *
	 * @return a new {@link DeleteDataQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteData"> SPARQL DELETE DATA Query</a>
	 */
	public static DeleteDataQuery DELETE_DATA(TriplePattern... triples) {
		return new DeleteDataQuery().deleteData(triples);
	}

	/**
	 * Create a SPARQL DELETE DATA query
	 *
	 * @param triplesTemplate the {@link TriplesTemplate} to set initially
	 *
	 * @return a new {@link DeleteDataQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteData"> SPARQL DELETE DATA Query</a>
	 */
	public static DeleteDataQuery DELETE_DATA(TriplesTemplate triplesTemplate) {
		return new DeleteDataQuery().deleteData(triplesTemplate);
	}

	/**
	 * Creates a SPARQL Modify query
	 *
	 * @return a new {@link ModifyQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteInsert"> SPARQL Modify Query</a>
	 */
	public static ModifyQuery MODIFY() {
		return new ModifyQuery();
	}

	/**
	 * Convenience method, creates a SPARQL DELETE query using ModifyQuery.
	 *
	 * @param triples the initial set of {@link TriplePattern}(s), if any, to use
	 * @return a new {@link ModifyQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#delete"> SPARQL DELETE Query</a>
	 */
	public static ModifyQuery DELETE(TriplePattern... triples) {
		return new ModifyQuery().delete(triples);
	}

	/**
	 * Convenience method, creates a SPARQL INSERT query using ModifyQuery.
	 *
	 * @param triples the initial set of {@link TriplePattern}(s), if any, to use
	 * @return a new {@link ModifyQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#insert"> SPARQL INSERT Query</a>
	 */
	public static ModifyQuery INSERT(TriplePattern... triples) {
		return new ModifyQuery().insert(triples);
	}

	/**
	 * Creates a SPARQL LOAD query
	 *
	 * @return a new {@link LoadQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#load"> SPARQL LOAD Query</a>
	 */
	public static LoadQuery LOAD() {
		return new LoadQuery();
	}

	/**
	 * Creates a SPARQL CLEAR Query
	 *
	 * @return a new {@link ClearQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#clear"> SPARQL CLEAR Query</a>
	 */
	public static ClearQuery CLEAR() {
		return new ClearQuery();
	}

	/**
	 * Creates a SPARQL CREATE Query
	 *
	 * @return a new {@link CreateQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#create"> SPARQL CREATE Query</a>
	 */
	public static CreateQuery CREATE() {
		return new CreateQuery();
	}

	/**
	 * Creates a SPARQL DROP Query
	 *
	 * @return a new {@link DropQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#drop"> SPARQL DROP Query</a>
	 */
	public static DropQuery DROP() {
		return new DropQuery();
	}

	/**
	 * Creates a SPARQL COPY Query
	 *
	 * @return a new {@link CopyQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#copy"> SPARQL COPY Query</a>
	 */
	public static CopyQuery COPY() {
		return new CopyQuery();
	}

	/**
	 * Creates a SPARQL MOVE Query
	 *
	 * @return a new {@link MoveQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#move"> SPARQL MOVE Query</a>
	 */
	public static MoveQuery MOVE() {
		return new MoveQuery();
	}

	/**
	 * Creates a new SPARQL ADD Query
	 *
	 * @return a new {@link AddQuery}
	 *
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#add"> SPARQL ADD Query</a>
	 */
	public static AddQuery ADD() {
		return new AddQuery();
	}
}
