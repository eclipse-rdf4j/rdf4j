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
package org.eclipse.rdf4j.http.client;

import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.CleanerGraphQueryResult;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.CleanerTupleQueryResult;
import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.ConcurrentCleaner;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResult;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.BackgroundGraphResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.helpers.BackgroundTupleResult;
import org.eclipse.rdf4j.rio.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundResultExecutor implements AutoCloseable {

	private final static ConcurrentCleaner cleaner = new ConcurrentCleaner();

	private final Logger logger = LoggerFactory.getLogger(BackgroundResultExecutor.class);

	private final ExecutorService executor;

	private final HashSet<QueryResult<?>> executing = new HashSet<>();

	public BackgroundResultExecutor(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor, "Executor service was null");
	}

	public TupleQueryResult parse(TupleQueryResultParser parser, InputStream in, WeakReference<?> callerReference) {
		BackgroundTupleResult result = new BackgroundTupleResult(parser, in, callerReference);
		autoCloseRunnable(result, result);
		return new CleanerTupleQueryResult(result, cleaner);
	}

	public GraphQueryResult parse(RDFParser parser, InputStream in, Charset charset, String baseURI,
			WeakReference<?> callerReference) {
		BackgroundGraphResult result = new BackgroundGraphResult(parser, in, charset, baseURI, callerReference);
		autoCloseRunnable(result, result);
		return new CleanerGraphQueryResult(result, cleaner);
	}

	/**
	 * Force close any executing background result parsers
	 */
	@Override
	public void close() {
		synchronized (executing) {
			for (AutoCloseable onclose : executing) {
				try {
					onclose.close();
				} catch (Exception e) {
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					logger.error(e.toString(), e);
				}
			}
		}
	}

	private void autoCloseRunnable(QueryResult<?> result, Runnable runner) {
		synchronized (executing) {
			executing.add(result);
		}
		executor.execute(() -> {
			try {
				runner.run();
			} finally {
				synchronized (executing) {
					executing.remove(result);
				}
			}
		});
	}

}
