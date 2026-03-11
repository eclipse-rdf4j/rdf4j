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
package org.eclipse.rdf4j.workbench.commands;

import static org.eclipse.rdf4j.model.util.Values.bnode;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryServlet extends TransformationServlet {

	private static final String EFFECTIVE_CONFIG_TURTLE = "effective-config-turtle";

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	private static final Logger LOGGER = LoggerFactory.getLogger(SummaryServlet.class);

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryEvaluationException, MalformedQueryException, QueryResultHandlerException {
		builder.transform(xslPath, "summary.xsl");
		builder.metadata(EFFECTIVE_CONFIG_TURTLE, getEffectiveConfigTurtle());
		builder.start("id", "description", "location", "server", "size", "contexts");
		builder.link(List.of(INFO));
		try (RepositoryConnection con = repository.getConnection()) {
			String size = null;
			String numContexts = null;
			try {
				List<Future<String>> futures = getRepositoryStatistics(con);
				size = getResult("repository size.", futures.get(0));
				numContexts = getResult("labeled contexts.", futures.get(1));
			} catch (InterruptedException e) {
				LOGGER.warn("Interrupted while requesting repository statistics.", e);
			}
			builder.result(info.getId(), info.getDescription(), info.getLocation(), getServer(), size, numContexts);
			builder.end();
		}
	}

	private String getResult(String itemRequested, Future<String> future) {
		String result = "Unexpected interruption while requesting " + itemRequested;
		try {
			if (future.isCancelled()) {
				result = "Timed out while requesting " + itemRequested;
			} else {
				try {
					result = future.get();
				} catch (ExecutionException e) {
					LOGGER.warn("Exception occured during async request.", e);
					result = "Exception occured while requesting " + itemRequested;
				}
			}
		} catch (InterruptedException e) {
			LOGGER.error("Unexpected exception", e);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<Future<String>> getRepositoryStatistics(final RepositoryConnection con) throws InterruptedException {
		List<Future<String>> futures;
		futures = executorService.invokeAll(Arrays.asList(new Callable<>() {

			@Override
			public String call() throws RepositoryException {
				return Long.toString(con.size());
			}

		}, new Callable<>() {

			@Override
			public String call() throws RepositoryException {
				return Integer.toString(Iterations.asList(con.getContextIDs()).size());
			}

		}), 2000, TimeUnit.MILLISECONDS);
		return futures;
	}

	private String getServer() {
		String result = null; // gracefully ignored by builder.result(...)
		if (manager instanceof LocalRepositoryManager) {
			result = ((LocalRepositoryManager) manager).getBaseDir().toString();
		} else if (manager instanceof RemoteRepositoryManager) {
			result = ((RemoteRepositoryManager) manager).getServerURL();
		}
		return result;
	}

	private String getEffectiveConfigTurtle() {
		if (manager == null || info == null || info.getId() == null) {
			return null;
		}

		try {
			RepositoryConfig repositoryConfig = manager.getRepositoryConfig(info.getId());
			if (repositoryConfig == null) {
				return null;
			}

			Model model = new LinkedHashModel();
			repositoryConfig.export(model, bnode());

			WriterConfig writerConfig = new WriterConfig();
			writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
			writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

			StringWriter writer = new StringWriter();
			Rio.write(model, writer, RDFFormat.TURTLE, writerConfig);
			return writer.toString();
		} catch (RepositoryConfigException | RepositoryException e) {
			LOGGER.warn("Unable to render effective repository config.", e);
			return null;
		}
	}
}
