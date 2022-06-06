/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.helpers;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.QueueCursor;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

/**
 * Provides concurrent access to tuple results as they are being parsed.
 *
 * @author James Leigh
 */
@Deprecated(since = "4.1.0")
public class BackgroundTupleResult extends IteratingTupleQueryResult implements Runnable, TupleQueryResultHandler {

	private final TupleQueryResultParser parser;

	private final InputStream in;

	private final QueueCursor<BindingSet> queue;

	private final List<String> bindingNames = new ArrayList<>();

	private final CountDownLatch bindingNamesReady = new CountDownLatch(1);

	private final CountDownLatch finishedParsing = new CountDownLatch(1);

	public BackgroundTupleResult(TupleQueryResultParser parser, InputStream in, WeakReference<?> callerRef) {
		this(new QueueCursor<>(10, callerRef), parser, in);
	}

	public BackgroundTupleResult(QueueCursor<BindingSet> queue, TupleQueryResultParser parser, InputStream in) {
		super(Collections.emptyList(), queue);
		this.queue = queue;
		this.parser = parser;
		this.in = in;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			queue.done();
		}
		try {
			finishedParsing.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			queue.checkException();
		}
	}

	@Override
	public List<String> getBindingNames() {
		try {
			bindingNamesReady.await();
			return bindingNames;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		} finally {
			queue.checkException();
		}
	}

	@Override
	public void run() {
		try {
			try {
				parser.setQueryResultHandler(this);
				parser.parseQueryResult(in);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			queue.toss(e);
		} finally {
			queue.done();
			bindingNamesReady.countDown();
			finishedParsing.countDown();
		}
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		this.bindingNames.addAll(bindingNames);
		bindingNamesReady.countDown();
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		try {
			queue.put(bindingSet);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			queue.toss(e);
			queue.done();
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		// no-op
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		// ignore
	}
}
