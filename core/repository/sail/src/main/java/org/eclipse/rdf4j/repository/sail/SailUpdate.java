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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.impl.AbstractParserUpdate;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.helpers.SailUpdateExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeen Broekstra
 * @author James Leigh
 */
public class SailUpdate extends AbstractParserUpdate {

	private static final Logger logger = LoggerFactory.getLogger(SailUpdate.class);

	private final SailRepositoryConnection con;

	protected SailUpdate(ParsedUpdate parsedUpdate, SailRepositoryConnection con) {
		super(parsedUpdate);
		this.con = con;
	}

	protected SailRepositoryConnection getConnection() {
		return con;
	}

	@Override
	public void execute() throws UpdateExecutionException {
		ParsedUpdate parsedUpdate = getParsedUpdate();
		List<UpdateExpr> updateExprs = parsedUpdate.getUpdateExprs();
		Map<UpdateExpr, Dataset> datasetMapping = parsedUpdate.getDatasetMapping();

		SailUpdateExecutor executor = new SailUpdateExecutor(con.getSailConnection(), con.getValueFactory(),
				con.getParserConfig());

		boolean localTransaction = false;
		try {
			if (!getConnection().isActive()) {
				localTransaction = true;
				beginLocalTransaction();
			}
			for (UpdateExpr updateExpr : updateExprs) {

				Dataset activeDataset = getMergedDataset(datasetMapping.get(updateExpr));

				try {
					executor.executeUpdate(updateExpr, activeDataset, getBindings(), getIncludeInferred(),
							getMaxExecutionTime());
				} catch (RDF4JException | IOException e) {
					logger.warn("exception during update execution: ", e);
					if (!updateExpr.isSilent()) {
						throw new UpdateExecutionException(e);
					}
				}
			}

			if (localTransaction) {
				commitLocalTransaction();
				localTransaction = false;
			}
		} finally {
			if (localTransaction) {
				rollbackLocalTransaction();
			}
		}
	}

	private void beginLocalTransaction() throws RepositoryException {
		getConnection().begin();
	}

	private void commitLocalTransaction() throws RepositoryException {
		getConnection().commit();

	}

	private void rollbackLocalTransaction() throws RepositoryException {
		getConnection().rollback();

	}
}
