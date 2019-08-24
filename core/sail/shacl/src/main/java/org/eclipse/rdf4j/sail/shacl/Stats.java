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

	public void added(Statement statement) {
		hasAdded = true;
	}

	public void removed(Statement statement) {
		hasRemoved = true;

	}

	public boolean hasAdded() {
		return hasAdded;
	}

	public boolean hasRemoved() {
		return hasRemoved;
	}

	public boolean isBaseSailEmpty() {
		return baseSailEmpty;
	}

	void setBaseSailEmpty(boolean baseSailEmpty) {
		this.baseSailEmpty = baseSailEmpty;
	}
}