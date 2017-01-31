/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;

/**
 * Provides concurrent access to statements as they are being parsed when instances of this class are run as
 * Threads.
 * 
 * @author James Leigh
 */
public class BackgroundGraphResult extends IterationWrapper<Statement, QueryEvaluationException>
		implements GraphQueryResult, Runnable, RDFHandler
{

	private final RDFParser parser;

	private final Charset charset;

	private final InputStream in;

	private final String baseURI;

	private final CountDownLatch namespacesReady = new CountDownLatch(1);

	private final Map<String, String> namespaces = new ConcurrentHashMap<String, String>();

	private final QueueCursor<Statement> queue;

	public BackgroundGraphResult(RDFParser parser, InputStream in, Charset charset, String baseURI) {
		this(new QueueCursor<Statement>(10), parser, in, charset, baseURI);
	}

	public BackgroundGraphResult(QueueCursor<Statement> queue, RDFParser parser, InputStream in,
			Charset charset, String baseURI)
	{
		super(queue);
		this.queue = queue;
		this.parser = parser;
		this.in = in;
		this.charset = charset;
		this.baseURI = baseURI;
	}

	@Override
	public boolean hasNext()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			return false;
		}
		if (Thread.currentThread().isInterrupted()) {
			close();
			return false;
		}

		boolean result = queue.hasNext();
		if (!result) {
			close();
		}
		return result;
	}

	@Override
	public Statement next()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		if (Thread.currentThread().isInterrupted()) {
			close();
			throw new NoSuchElementException("The iteration has been closed.");
		}

		try {
			return queue.next();
		}
		catch (NoSuchElementException e) {
			close();
			throw e;
		}
	}

	@Override
	public void remove()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		if (Thread.currentThread().isInterrupted()) {
			close();
			throw new IllegalStateException("The iteration has been closed.");
		}

		try {
			queue.remove();
		}
		catch (IllegalStateException e) {
			close();
			throw e;
		}
	}

	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		try {
			try {
				super.handleClose();
			}
			finally {
				try {
					// After checking that we ourselves cannot possibly be generating an NPE ourselves, 
					// attempt to close the input stream we were given
					InputStream toClose = in;
					if (toClose != null) {
						toClose.close();
					}
				}
				catch (NullPointerException e) {
					// Swallow NullPointerException that Apache HTTPClient is hiding behind a NotThreadSafe annotation
				}
			}
		}
		catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
		finally {
			queue.close();
		}
	}

	@Override
	public void run() {
		try {
			parser.setRDFHandler(this);
			if (charset == null) {
				parser.parse(in, baseURI);
			}
			else {
				parser.parse(new InputStreamReader(in, charset), baseURI);
			}
		}
		catch (RDFHandlerException e) {
			// parsing was cancelled or interrupted
			close();
		}
		catch (Exception e) {
			queue.toss(e);
			close();
		}
		finally {
			queue.done();
			namespacesReady.countDown();
		}
	}

	@Override
	public void startRDF()
		throws RDFHandlerException
	{
		// no-op
	}

	@Override
	public Map<String, String> getNamespaces() {
		try {
			namespacesReady.await();
			// Show the user an unmodifiable view on the map but we can still change it here
			return Collections.unmodifiableMap(namespaces);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			close();
			throw new UndeclaredThrowableException(e);
		}
	}

	@Override
	public void handleComment(String comment)
		throws RDFHandlerException
	{
		// ignore
	}

	@Override
	public void handleNamespace(String prefix, String uri)
		throws RDFHandlerException
	{
		namespaces.put(prefix, uri);
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		namespacesReady.countDown();
		try {
			queue.put(st);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			close();
			throw new RDFHandlerException(e);
		}
		if (isClosed()) {
			throw new RDFHandlerException("Result closed");
		}
	}

	@Override
	public void endRDF()
		throws RDFHandlerException
	{
		namespacesReady.countDown();
	}

}
