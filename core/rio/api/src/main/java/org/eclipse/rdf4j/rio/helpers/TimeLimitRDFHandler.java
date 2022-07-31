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
package org.eclipse.rdf4j.rio.helpers;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeLimitRDFHandler extends RDFHandlerWrapper {

	private static final Timer timer = new Timer("TimeLimitRDFHandler", true);

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final InterruptTask interruptTask;

	private volatile boolean isInterrupted = false;

	private final AtomicBoolean ended = new AtomicBoolean(false);

	public TimeLimitRDFHandler(RDFHandler rdfHandler, long timeLimit) {
		super(rdfHandler);

		assert timeLimit > 0 : "time limit must be a positive number, is: " + timeLimit;

		interruptTask = new InterruptTask(this);

		timer.schedule(interruptTask, timeLimit);
	}

	private boolean isEnded() {
		return ended.get();
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		checkInterrupted();
		super.startRDF();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkInterrupted();
		if (ended.compareAndSet(false, true)) {
			super.endRDF();
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		checkInterrupted();
		super.handleNamespace(prefix, uri);
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		checkInterrupted();
		super.handleStatement(st);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		checkInterrupted();
		super.handleComment(comment);
	}

	private void checkInterrupted() throws RDFHandlerException {
		if (isInterrupted) {
			throw new RDFHandlerException("RDFHandler took too long");
		}
	}

	private void interrupt() {
		isInterrupted = true;
		if (!isEnded()) {
			try {
				// we call endRDF() in case impls have resources to close
				endRDF();
			} catch (RDFHandlerException e) {
				logger.warn("Failed to end RDF", e);
			}
		}
	}

	private static class InterruptTask extends TimerTask {

		private final WeakReference<TimeLimitRDFHandler> handlerRef;

		InterruptTask(TimeLimitRDFHandler handler) {
			this.handlerRef = new WeakReference<>(handler);
		}

		@Override
		public void run() {
			TimeLimitRDFHandler handler = handlerRef.get();
			if (handler != null) {
				handler.interrupt();
			}
		}
	}
}
