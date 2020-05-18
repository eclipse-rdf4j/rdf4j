/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import java.lang.ref.WeakReference;
import java.util.TimerTask;

/**
 * TimerTask that keeps a weak reference to the supplied iteration and when activated, interrupts it.
 *
 * @author Jeen Broekstra
 */
class InterruptTask<E, X extends Exception> extends TimerTask {

	private WeakReference<TimeLimitIteration<E, X>> iterationRef;

	public InterruptTask(TimeLimitIteration<E, X> iteration) {
		this.iterationRef = new WeakReference<>(iteration);
	}

	@Override
	public void run() {
		TimeLimitIteration<E, X> iteration = iterationRef.get();
		if (iteration != null) {
			iteration.interrupt();
		}
	}
}
