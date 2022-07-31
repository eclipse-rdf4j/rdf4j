/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.eclipse.rdf4j.sail.SailException;

abstract class SearchQueryEvaluatorTest {
	protected ParsedQuery parseQuery(String query) {
		return new SPARQLParser().parseQuery(query, "urn:base:");
	}

	protected Collection<BindingSet> createBindingSet(String name, String iri) {
		MapBindingSet bindingSet = new MapBindingSet();
		bindingSet.addBinding(name, SimpleValueFactory.getInstance().createIRI(iri));
		return Collections.singletonList(bindingSet);
	}

	protected class SearchIndexImpl implements SearchIndex {
		protected Set<String> wktFields = Collections.singleton(SearchFields.getPropertyField(GEO.AS_WKT));

		@Override
		public void initialize(Properties parameters) throws Exception {
		}

		@Override
		public Collection<BindingSet> evaluate(SearchQueryEvaluator query) throws SailException {
			return null;
		}

		@Override
		public void shutDown() throws IOException {
		}

		@Override
		public boolean accept(Literal literal) {
			return false;
		}

		@Override
		public boolean isGeoField(String propertyName) {
			return (wktFields != null) && wktFields.contains(propertyName);
		}

		@Override
		public boolean isTypeStatement(Statement statement) {
			return false;
		}

		@Override
		public boolean isTypeFilteringEnabled() {
			return false;
		}

		@Override
		public boolean isIndexedTypeStatement(Statement statement) {
			return false;
		}

		@Override
		public Map<IRI, Set<IRI>> getIndexedTypeMapping() {
			return null;
		}

		@Override
		public void begin() throws IOException {
		}

		@Override
		public void commit() throws IOException {
		}

		@Override
		public void rollback() throws IOException {
		}

		@Override
		public void addStatement(Statement statement) throws IOException {
		}

		@Override
		public void removeStatement(Statement statement) throws IOException {
		}

		@Override
		public void addRemoveStatements(Collection<Statement> added, Collection<Statement> removed) throws IOException {
		}

		@Override
		public void clearContexts(Resource... contexts) throws IOException {
		}

		@Override
		public void addDocuments(Resource subject, List<Statement> statements) throws IOException {
		}

		@Override
		public void clear() throws IOException {
		}
	}
}
