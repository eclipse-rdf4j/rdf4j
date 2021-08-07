/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.tx;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.tx.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author ameingast@gmail.com
 * @author Florian Kleedorfer
 */
public class TransactionalRepositoryConnectionFactory implements RepositoryConnectionFactory {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private RepositoryConnectionFactory delegateFactory;

	private final ThreadLocal<TransactionData> transactionData = new ThreadLocal<>();

	public TransactionalRepositoryConnectionFactory(RepositoryConnectionFactory delegateFactory) {
		this.delegateFactory = delegateFactory;
	}

	public TransactionData getTransactionData() {
		return transactionData.get();
	}

	public RepositoryConnection getConnection() {
		logger.debug("Trying to obtain connection");
		TransactionData data = getTransactionData();
		if (data == null) {
			throw new NoTransactionException("Cannot obtain connection: no transaction");
		}
		RepositoryConnection con = data.getConnection();
		if (con == null) {
			throw new RDF4JTransactionException(
					"Cannot obtain connection: transaction started but no connection found");
		}
		if (!con.isOpen()) {
			throw new ConnectionClosedException("Cannot obtain connection: connection closed");
		}
		if (data.isReadOnly()) {
			logger.debug("transaction is readonly, not starting a database transaction");
		} else {
			if (!con.isActive()) {
				logger.debug(
						"connection does not have an active transaction yet, starting transaction");
				con.begin();
				logger.debug("con.begin() called");
			}
		}
		logger.debug("returning  connection");
		return con;
	}

	public void closeConnection() {
		logger.debug("Trying to close connection");
		RepositoryConnection con = null;
		try {
			TransactionData data = getTransactionData();
			if (data == null) {
				throw new NoTransactionException("Cannot close connection: no transaction");
			}
			con = data.getConnection();
			if (con == null) {
				throw new RDF4JTransactionException(
						"Cannot close connection: transaction started but no connection found");
			}
			if (!con.isOpen()) {
				throw new ConnectionClosedException("Cannot close connection: connection closed");
			}
		} finally {
			try {
				if (con != null && con.isActive()) {
					logger.warn(
							"Encountered active transaction when closing connection - rolling back!");
					con.rollback();
					logger.debug("con.rollback() called");
				}
			} catch (Throwable t) {
				logger.error("Error rolling back transaction", t);
			}
			try {
				if (con != null) {
					con.close();
					logger.debug("con.close() called");
				}
			} catch (Throwable t) {
				logger.error("Error closing connection", t);
			}
			this.transactionData.remove();
			logger.debug("Thread-local transaction data removed");
		}
	}

	public TransactionData createTransaction() {
		logger.debug("Trying to create new transaction");
		RepositoryConnection con = delegateFactory.getConnection();
		TransactionData data = new TransactionData(con);
		transactionData.set(data);
		logger.debug("Transaction created");
		return data;
	}

	public void endTransaction(boolean rollback) {
		logger.debug("Trying to end transaction");
		TransactionData data = getTransactionData();
		if (data == null) {
			throw new NoTransactionException("Cannot obtain connection: no transaction");
		}
		RepositoryConnection con = data.getConnection();
		if (con == null) {
			throw new RDF4JTransactionException(
					"Cannot obtain connection: transaction started but no connection found");
		}
		if (!con.isOpen()) {
			throw new ConnectionClosedException("Cannot obtain connection: connection closed");
		}
		if (data.isReadOnly()) {
			logger.debug("transaction is readonly - neither rolling back nor committing");
		} else {
			if (con.isActive()) {
				if (rollback) {
					try {
						logger.debug("rolling back transaction...");
						con.rollback();
						logger.debug("con.rollback() called");
					} catch (Throwable t) {
						throw new RollbackException(
								"Cannot rollback transaction: an error occurred", t);
					}
				} else {
					try {
						logger.debug("committing transaction...");
						con.commit();
						logger.debug("con.commit() called");
					} catch (Throwable t) {
						throw new CommitException(
								"Cannot commit transaction: an error occurred", t);
					}
				}
			}
		}
	}
}
