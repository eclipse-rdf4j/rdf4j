/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.tx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.spring.tx.exception.WriteDeniedException;

/**
 * Connection wrapper that throws an exception if a write operation is attempted in a read-only transaction.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class TransactionalRepositoryConnection extends RepositoryConnectionWrapper {

	TransactionObject transactionObject = null;

	public TransactionalRepositoryConnection(Repository repository) {
		super(repository);
	}

	public TransactionalRepositoryConnection(Repository repository, RepositoryConnection delegate) {
		super(repository, delegate);
		this.transactionObject = transactionObject;
	}

	public void setTransactionObject(TransactionObject transactionObject) {
		this.transactionObject = transactionObject;
	}

	private void throwExceptionIfReadonly() {
		if (this.transactionObject.isReadOnly()) {
			throw new WriteDeniedException("Cannot write in a read-only transaction!");
		}
	}

	@Override
	public void add(File file, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(file, baseURI, dataFormat, contexts);
	}

	@Override
	public void add(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(in, baseURI, dataFormat, contexts);
	}

	@Override
	public void add(Iterable<? extends Statement> statements, Resource... contexts)
			throws RepositoryException {
		throwExceptionIfReadonly();
		super.add(statements, contexts);
	}

	@Override
	public <E extends Exception> void add(
			Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		throwExceptionIfReadonly();
		super.add(statementIter, contexts);
	}

	@Override
	public void add(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(reader, baseURI, dataFormat, contexts);
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		throwExceptionIfReadonly();
		super.add(subject, predicate, object, contexts);
	}

	@Override
	public void add(Statement st, Resource... contexts) throws RepositoryException {
		throwExceptionIfReadonly();
		super.add(st, contexts);
	}

	@Override
	public void add(URL url, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(url, baseURI, dataFormat, contexts);
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		throwExceptionIfReadonly();
		super.clear(contexts);
	}

	@Override
	public void remove(Iterable<? extends Statement> statements, Resource... contexts)
			throws RepositoryException {
		throwExceptionIfReadonly();
		super.remove(statements, contexts);
	}

	@Override
	public <E extends Exception> void remove(
			Iteration<? extends Statement, E> statementIter, Resource... contexts)
			throws RepositoryException, E {
		throwExceptionIfReadonly();
		super.remove(statementIter, contexts);
	}

	@Override
	public void remove(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		throwExceptionIfReadonly();
		super.remove(subject, predicate, object, contexts);
	}

	@Override
	public void remove(Statement st, Resource... contexts) throws RepositoryException {
		throwExceptionIfReadonly();
		super.remove(st, contexts);
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		throwExceptionIfReadonly();
		super.removeNamespace(prefix);
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		throwExceptionIfReadonly();
		super.clearNamespaces();
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		throwExceptionIfReadonly();
		super.setNamespace(prefix, name);
	}

	@Override
	public Update prepareUpdate(String update) throws RepositoryException, MalformedQueryException {
		throwExceptionIfReadonly();
		return super.prepareUpdate(update);
	}

	@Override
	public void add(InputStream in, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(in, dataFormat, contexts);
	}

	@Override
	public void add(Reader reader, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(reader, dataFormat, contexts);
	}

	@Override
	public void add(URL url, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(url, contexts);
	}

	@Override
	public void add(URL url, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(url, dataFormat, contexts);
	}

	@Override
	public void add(File file, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(file, contexts);
	}

	@Override
	public void add(File file, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, RepositoryException {
		throwExceptionIfReadonly();
		super.add(file, dataFormat, contexts);
	}

	@Override
	public void add(RepositoryResult<Statement> statements, Resource... contexts)
			throws RepositoryException {
		throwExceptionIfReadonly();
		super.add(statements, contexts);
	}

	@Override
	public void remove(RepositoryResult<Statement> statements, Resource... contexts)
			throws RepositoryException {
		throwExceptionIfReadonly();
		super.remove(statements, contexts);
	}
}
