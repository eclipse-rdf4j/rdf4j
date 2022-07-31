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

import static org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils.findWrapper;
import static org.eclipse.rdf4j.spring.util.RepositoryConnectionWrappingUtils.wrapOnce;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationReportHelper;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.tx.exception.CommitException;
import org.eclipse.rdf4j.spring.tx.exception.ConnectionClosedException;
import org.eclipse.rdf4j.spring.tx.exception.NoTransactionException;
import org.eclipse.rdf4j.spring.tx.exception.RDF4JTransactionException;
import org.eclipse.rdf4j.spring.tx.exception.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author ameingast@gmail.com
 * @author Florian Kleedorfer
 */
public class TransactionalRepositoryConnectionFactory implements RepositoryConnectionFactory {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final RepositoryConnectionFactory delegateFactory;

	private final ThreadLocal<TransactionObject> transactionData = new ThreadLocal<>();

	public TransactionalRepositoryConnectionFactory(RepositoryConnectionFactory delegateFactory) {
		this.delegateFactory = delegateFactory;
	}

	public TransactionObject getTransactionData() {
		return transactionData.get();
	}

	public RepositoryConnection getConnection() {
		logger.debug("Trying to obtain connection");
		TransactionObject data = getTransactionData();
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
			TransactionObject data = getTransactionData();
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

	public TransactionObject createTransaction() {
		logger.debug("Trying to create new transaction");
		RepositoryConnection delegate = delegateFactory.getConnection();
		RepositoryConnection wrappedCon = wrapOnce(
				delegate,
				con -> new TransactionalRepositoryConnection(con.getRepository(), con),
				TransactionalRepositoryConnection.class);
		TransactionObject txObj = new TransactionObject(wrappedCon);
		transactionData.set(txObj);
		TransactionalRepositoryConnection txCon = findWrapper(wrappedCon, TransactionalRepositoryConnection.class)
				.get();
		txCon.setTransactionObject(txObj);
		logger.debug("Transaction created");
		return txObj;
	}

	public void endTransaction(boolean rollback) {
		logger.debug("Trying to end transaction");
		TransactionObject data = getTransactionData();
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
			logger.debug("transaction is readonly");
			if (con.isActive()) {
				logger.debug("however, the connection is active - rolling back");
				try {
					con.rollback();
				} catch (Exception e) {
					throw new RollbackException(
							"Cannot rollback changes in readonly transaction: an error occurred",
							e);
				}
			} else {
				logger.debug("The connection is inactive, no updates have been attempted.");
			}
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
						ShaclSailValidationReportHelper
								.getValidationReportAsString(t)
								.ifPresent(report -> logger.error(
										"SHACL validation failed, cannot commit. Validation report:\n{}", report));
						throw new CommitException(
								"Cannot commit transaction: an error occurred", t);
					}
				}
			}
		}
	}
}
