/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.common.iteration.IterationWrapper;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;

/**
 * Provides concurrent access to statements as they are being parsed.
 * 
 * @author James Leigh
 */
public class BackgroundGraphResult extends IterationWrapper<Statement, QueryEvaluationException> implements
		GraphQueryResult, Runnable, RDFHandler
{

	private volatile boolean closed;

	private RDFParser parser;

	private Charset charset;

	private InputStream in;

	private String baseURI;

	private CountDownLatch namespacesReady = new CountDownLatch(1);

	private Map<String, String> namespaces = new ConcurrentHashMap<String, String>();

	private QueueCursor<Statement> queue;

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

	public boolean hasNext()
		throws QueryEvaluationException
	{
		return queue.hasNext();
	}

	public Statement next()
		throws QueryEvaluationException
	{
		return queue.next();
	}

	public void remove()
		throws QueryEvaluationException
	{
		queue.remove();
	}

	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		try {
			super.handleClose();
		}
		finally {
			closed = true;
			try {
				in.close();
			}
			catch (IOException e) {
				throw new QueryEvaluationException(e);
			}
			finally {
				queue.close();
			}
		}
	}

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
		}
		catch (RDFParseException e) {
			queue.toss(e);
		}
		catch (IOException e) {
			queue.toss(e);
		}
		finally {
			queue.done();
			namespacesReady.countDown();
		}
	}

	public void startRDF()
		throws RDFHandlerException
	{
		// no-op
	}

	public Map<String, String> getNamespaces() {
		try {
			namespacesReady.await();
			return namespaces;
		}
		catch (InterruptedException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public void handleComment(String comment)
		throws RDFHandlerException
	{
		// ignore
	}

	public void handleNamespace(String prefix, String uri)
		throws RDFHandlerException
	{
		namespaces.put(prefix, uri);
	}

	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		namespacesReady.countDown();
		try {
			queue.put(st);
		}
		catch (InterruptedException e) {
			throw new RDFHandlerException(e);
		}
		if (closed)
			throw new RDFHandlerException("Result closed");
	}

	public void endRDF()
		throws RDFHandlerException
	{
		namespacesReady.countDown();
	}

}
