/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

/**
 * Option to describe how the {@link org.eclipse.rdf4j.sail.lucene.LuceneSail} should handle the add of a new type
 * statement in a connection if the {@link LuceneSail#INDEXEDTYPES} property is defined.
 *
 * <h2 id="backtrace_example">Backtrace example</h2> For example if we have the predicate my:text indexed by Lucene and
 * (my:oftype my:type1) as an {@link LuceneSail#INDEXEDTYPES}, the previous store state is
 *
 * <pre>
 * # Store triples:
 * my:subj1 my:text   "demo 1" .
 * my:subj2 my:oftype my:type1 .
 * my:subj2 my:text   "demo 2" .
 * # Lucene Indexed literals:
 * my:subj2 "demo 2"
 * </pre>
 *
 * The option will define how the Sail will handle the update:
 *
 * <pre>
 * INSERT my:subj1 my:oftype my:type1 .
 * DELETE my:subj2 my:oftype my:type1 .
 * </pre>
 */
public enum TypeBacktraceMode {
	/**
	 * The sail will get all previous triples of the subject and add them (if required) in the Lucene index, this mode
	 * is enabled by default.
	 *
	 * the future state of the Lucene index in the <a href="#backtrace_example">above example</a> would be:
	 *
	 * <pre>
	 * my:subj1 "demo 1"
	 * </pre>
	 */
	COMPLETE(true, true),
	/**
	 * The sail won't get any previous triples of the subject in the Lucene index, this mode is useful if you won't
	 * change the type and values of your subjects in multiple queries.
	 *
	 * the future state of the Lucene index in the <a href="#backtrace_example">above example</a> would be:
	 *
	 * <pre>
	 * my:subj2 "demo 2"
	 * </pre>
	 */
	PARTIAL(false, false);

	private final boolean shouldBackTraceInsert;
	private final boolean shouldBackTraceDelete;

	TypeBacktraceMode(boolean shouldBackTraceInsert, boolean shouldBackTraceDelete) {
		this.shouldBackTraceInsert = shouldBackTraceInsert;
		this.shouldBackTraceDelete = shouldBackTraceDelete;
	}

	/**
	 * @return if the index should backtrace over the old properties on an add
	 */
	public boolean shouldBackTraceInsert() {
		return shouldBackTraceInsert;
	}

	/**
	 * @return if the index should backtrace over the old properties on a delete
	 */
	public boolean shouldBackTraceDelete() {
		return shouldBackTraceDelete;
	}

	/**
	 * Default backtrace mode
	 */
	public static final TypeBacktraceMode DEFAULT_TYPE_BACKTRACE_MODE = COMPLETE;
}
