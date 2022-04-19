/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog.log;

import org.eclipse.rdf4j.query.Operation;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class OperationExecutionStats {
	private final int bindingsHashCode;
	private final String operation;
	private final long start;
	private Long end = null;
	private boolean failed = false;

	public OperationExecutionStats(String operation, int bindingsHashCode) {
		this.bindingsHashCode = bindingsHashCode;
		this.operation = operation;
		this.start = System.currentTimeMillis();
	}

	public static OperationExecutionStats of(Operation operation) {
		return new OperationExecutionStats(
				operation.toString(), operation.getBindings().hashCode());
	}

	public static OperationExecutionStats of(PseudoOperation operation) {
		return new OperationExecutionStats(operation.getOperation(), operation.getValuesHash());
	}

	public void operationSuccessful() {
		this.end = System.currentTimeMillis();
	}

	public void operationFailed() {
		this.end = System.currentTimeMillis();
		this.failed = true;
	}

	public String getOperation() {
		return operation;
	}

	public int getBindingsHashCode() {
		return bindingsHashCode;
	}

	public long getQueryDuration() {
		if (this.end == null) {
			throw new IllegalStateException("Cannot calculate duration - end is null");
		}
		return end - start;
	}

	public boolean isFailed() {
		return failed;
	}
}
