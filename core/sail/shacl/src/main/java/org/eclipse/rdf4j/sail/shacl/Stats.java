/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Statement;

/**
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 */
@InternalUseOnly
public class Stats {

	private boolean baseSailEmpty;
	private boolean hasAdded;
	private boolean hasRemoved;
	private boolean empty;

	public void added(Statement statement) {
		hasAdded = true;
	}

	public void removed(Statement statement) {
		hasRemoved = true;
	}

	/**
	 *
	 * @return true if statements were effectively added in this transaction
	 */
	public boolean hasAdded() {
		return hasAdded;
	}

	/**
	 *
	 * @return true if statements were effectively removed in this transaction
	 */
	public boolean hasRemoved() {
		return hasRemoved;
	}

	/**
	 *
	 * @return true if the sail was empty before this transaction started
	 */
	public boolean isBaseSailEmpty() {
		return baseSailEmpty;
	}

	void setBaseSailEmpty(boolean baseSailEmpty) {
		this.baseSailEmpty = baseSailEmpty;
	}

	/**
	 *
	 * @return true if the entire sail is empty
	 */
	public boolean isEmpty() {
		return empty;
	}

	void setEmpty(boolean empty) {
		this.empty = empty;
	}
}
