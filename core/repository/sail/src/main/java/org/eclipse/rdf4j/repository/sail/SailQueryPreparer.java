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
package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.repository.evaluation.RepositoryTripleSource;

/**
 * QueryPreparer for use with SailRepository.
 */
public class SailQueryPreparer implements QueryPreparer {

	private final SailRepositoryConnection con;

	private final boolean includeInferred;

	private final TripleSource source;

	public SailQueryPreparer(SailRepositoryConnection con, boolean includeInferred) {
		this.con = con;
		this.includeInferred = includeInferred;
		this.source = new RepositoryTripleSource(con, includeInferred);
	}

	@Override
	public BooleanQuery prepare(ParsedBooleanQuery askQuery) {
		BooleanQuery query = new SailBooleanQuery(askQuery, con);
		query.setIncludeInferred(includeInferred);
		return query;
	}

	@Override
	public TupleQuery prepare(ParsedTupleQuery tupleQuery) {
		TupleQuery query = new SailTupleQuery(tupleQuery, con);
		query.setIncludeInferred(includeInferred);
		return query;
	}

	@Override
	public GraphQuery prepare(ParsedGraphQuery graphQuery) {
		GraphQuery query = new SailGraphQuery(graphQuery, con);
		query.setIncludeInferred(includeInferred);
		return query;
	}

	@Override
	public Update prepare(ParsedUpdate graphUpdate) {
		Update update = new SailUpdate(graphUpdate, con);
		update.setIncludeInferred(includeInferred);
		return update;
	}

	@Override
	public TripleSource getTripleSource() {
		return source;
	}
}
