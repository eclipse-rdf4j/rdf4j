/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.util.concurrent.atomic.AtomicBoolean;

final class NodeListenerHandle {

	final NodeListener listener;
	final Node node;
	NodeListenerHandle prev;
	NodeListenerHandle next;
	private final AtomicBoolean removed = new AtomicBoolean(false);

	NodeListenerHandle(Node node, NodeListener listener) {
		this.node = node;
		this.listener = listener;
	}

	boolean isRemoved() {
		return removed.get();
	}

	void remove() {
		if (removed.compareAndSet(false, true)) {
			node.removeListenerHandle(this);
		}
	}
}
