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
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A SearchIndex is a one-stop-shop abstraction of a Lucene index. It takes care of proper synchronization of
 * IndexReaders, IndexWriters and IndexSearchers in a way that is suitable for a LuceneSail.
 *
 * @see LuceneSail
 */
public interface SearchIndex {

	void initialize(Properties parameters) throws Exception;

	Collection<BindingSet> evaluate(SearchQueryEvaluator query) throws SailException;

	void shutDown() throws IOException;

	/**
	 * Returns whether the provided literal is accepted by the LuceneIndex to be indexed. It for instance does not make
	 * much since to index xsd:float.
	 *
	 * @param literal the literal to be accepted
	 * @return true if the given literal will be indexed by this LuceneIndex
	 */
	boolean accept(Literal literal);

	/**
	 * Returns true if the given property contains a geometry.
	 *
	 * @param propertyName
	 * @return boolean
	 */
	boolean isGeoField(String propertyName);

	/**
	 * Returns true if the given statement is a type statement, see {@link LuceneSail#INDEXEDTYPES} to use. This method
	 * should return false if {@link #isTypeFilteringEnabled()} returns false.
	 *
	 * @param statement statement
	 * @return boolean
	 */
	boolean isTypeStatement(Statement statement);

	/**
	 * is the {@link LuceneSail#INDEXEDTYPES} parameter set for this index.
	 *
	 * @return boolean
	 */
	boolean isTypeFilteringEnabled();

	/**
	 * Returns true if the given statement is a type statement of the right type, see {@link LuceneSail#INDEXEDTYPES} to
	 * use. This method should return false if {@link #isTypeFilteringEnabled()} returns false.
	 *
	 * @param statement statement
	 * @return boolean
	 */
	boolean isIndexedTypeStatement(Statement statement);

	/**
	 * @return the accepted types for a particular predicate map (predicate -> [objects])
	 */
	Map<IRI, Set<IRI>> getIndexedTypeMapping();

	/**
	 * Begins a transaction.
	 *
	 * @throws java.io.IOException
	 */
	void begin() throws IOException;

	/**
	 * Commits any changes done to the LuceneIndex since the last commit.The semantics is synchronous to
	 * SailConnection.commit(), i.e. the LuceneIndex should be committed/rolled back whenever the LuceneSailConnection
	 * is committed/rolled back.
	 *
	 * @throws IOException
	 */
	void commit() throws IOException;

	void rollback() throws IOException;

	/**
	 * Indexes the specified Statement.This should be called from within a begin-commit-rollback block.
	 *
	 * @param statement
	 * @throws IOException
	 */
	void addStatement(Statement statement) throws IOException;

	/**
	 * Removes the specified Statement from the indexes.This should be called from within a begin-commit-rollback
	 *
	 * block.
	 *
	 * @param statement
	 * @throws java.io.IOException
	 */
	void removeStatement(Statement statement) throws IOException;

	/**
	 * Add many statements at the same time, remove many statements at the same time.Ordering by resource has to be done
	 * inside this method. The passed added/removed sets are disjunct, no statement can be in both. This should be
	 * called from within a begin-commit-rollback block.
	 *
	 * @param added   all added statements, can have multiple subjects
	 * @param removed all removed statements, can have multiple subjects
	 * @throws IOException
	 */
	void addRemoveStatements(Collection<Statement> added, Collection<Statement> removed) throws IOException;

	/**
	 * This should be called from within a begin-commit-rollback block.
	 *
	 * @param contexts
	 * @throws IOException
	 */
	void clearContexts(Resource... contexts) throws IOException;

	/**
	 * Add a complete Lucene Document based on these statements.Do not search for an existing document with the same
	 * subject id. (assume the existing document was deleted). This should be called from within a begin-commit-rollback
	 * block.
	 *
	 * @param subject
	 * @param statements the statements that make up the resource
	 * @throws IOException
	 */
	void addDocuments(Resource subject, List<Statement> statements) throws IOException;

	/**
	 * Clears the indexes.
	 *
	 * @throws java.io.IOException
	 */
	void clear() throws IOException;
}
