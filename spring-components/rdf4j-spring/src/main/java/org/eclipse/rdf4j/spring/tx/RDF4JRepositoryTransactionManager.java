/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.tx;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @since 4.0.0
 * @author ameingast@gmail.com
 * @author Florian Kleedorfer
 */
public class RDF4JRepositoryTransactionManager extends AbstractPlatformTransactionManager {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final TransactionalRepositoryConnectionFactory repositoryConnectionFactory;

	public RDF4JRepositoryTransactionManager(
			TransactionalRepositoryConnectionFactory repositoryConnectionFactory) {
		this.repositoryConnectionFactory = repositoryConnectionFactory;
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		TransactionObject transactionData = this.repositoryConnectionFactory.getTransactionData();
		logger.debug("obtaining transaction data");
		if (transactionData == null) {
			logger.debug("creating new transaction");
			transactionData = this.repositoryConnectionFactory.createTransaction();
		} else {
			logger.debug("using existing transaction");
			transactionData.setExisting(true);
		}

		return transactionData;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return ((TransactionObject) transaction).isExisting();
	}

	@Override
	protected void doBegin(Object o, TransactionDefinition transactionDefinition)
			throws TransactionException {
		logger.debug("beginning transaction");
		TransactionObject data = (TransactionObject) o;
		data.setTimeout(transactionDefinition.getTimeout());
		data.setIsolationLevel(transactionDefinition.getIsolationLevel());
		data.setPropagationBehavior(transactionDefinition.getPropagationBehavior());
		data.setReadOnly(transactionDefinition.isReadOnly());
		data.setName(Thread.currentThread().getName() + " " + transactionDefinition.getName());
		setIsolationLevel(data, transactionDefinition);
	}

	private void setIsolationLevel(
			TransactionObject transactionData, TransactionDefinition transactionDefinition) {
		RepositoryConnection repositoryConnection = transactionData.getConnection();
		Repository repository = repositoryConnection.getRepository();

		if (repository instanceof SailRepository) {
			Sail sail = ((SailRepository) repository).getSail();
			IsolationLevel isolationLevel = IsolationLevelAdapter.adaptToRdfIsolation(
					sail, transactionDefinition.getIsolationLevel());
			repositoryConnection.setIsolationLevel(isolationLevel);
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		logger.debug("committting transaction");
		TransactionObject data = (TransactionObject) defaultTransactionStatus.getTransaction();
		try {
			this.repositoryConnectionFactory.endTransaction(data.isRollbackOnly());
		} catch (Exception e) {
			throw new TransactionSystemException("Error during commit", e);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		logger.debug("rolling back transaction");
		TransactionObject data = (TransactionObject) defaultTransactionStatus.getTransaction();
		try {
			this.repositoryConnectionFactory.endTransaction(true);
		} catch (Exception e) {
			throw new TransactionSystemException("Error during rollback", e);
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
		logger.debug("marking transaction for rollback");
		TransactionObject data = (TransactionObject) status.getTransaction();
		data.setRollbackOnly(true);
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		this.repositoryConnectionFactory.closeConnection();
	}
}
