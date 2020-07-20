/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

/**
 * Default implementation of {@link TaskWrapper} which returns the unmodified original task
 * 
 * @author Andreas Schwarte
 *
 */
public class DefaultTaskWrapper implements TaskWrapper {

	public static DefaultTaskWrapper INSTANCE = new DefaultTaskWrapper();

	private DefaultTaskWrapper() {
	}

	@Override
	public Runnable wrap(Runnable runnable) {
		return runnable;
	}

}
