/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Combines two sources to act as a single {@link SailSource}. This is useful to provide a combined view of both
 * explicit and inferred statements.
 *
 * @author James Leigh
 */
class UnionSailSource implements SailSource {

	/**
	 * The branch that will be used in calls to {@link #sink(IsolationLevel)}.
	 */
	private final SailSource primary;

	/**
	 * Additional statements that should be included in {@link SailDataset}s.
	 */
	private final SailSource additional;

	/**
	 * An {@link SailSource} that combines two other {@link SailSource}es.
	 *
	 * @param primary    delegates all calls to the given {@link SailSource}.
	 * @param additional delegate all call except {@link #sink(IsolationLevel)}.
	 */
	public UnionSailSource(SailSource primary, SailSource additional) {
		super();
		this.primary = primary;
		this.additional = additional;
	}

	@Override
	public String toString() {
		return primary.toString() + "\n" + additional.toString();
	}

	@Override
	public void close() throws SailException {
		primary.close();
		additional.close();
	}

	@Override
	public SailSource fork() {
		return new UnionSailSource(primary.fork(), additional.fork());
	}

	@Override
	public void prepare() throws SailException {
		primary.prepare();
		additional.prepare();
	}

	@Override
	public void flush() throws SailException {
		primary.flush();
		additional.flush();
	}

	@Override
	public SailSink sink(IsolationLevel level) throws SailException {
		return primary.sink(level);
	}

	@Override
	public SailDataset dataset(IsolationLevel level) throws SailException {
		SailDataset dataset1;
		SailDataset dataset2;

		// do {
		dataset1 = primary.dataset(level);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dataset2 = additional.dataset(level);
		// }while(!dataset1.sameTransactionAs(dataset2));

		return new UnionSailDataset(dataset1, dataset2);
	}

}
