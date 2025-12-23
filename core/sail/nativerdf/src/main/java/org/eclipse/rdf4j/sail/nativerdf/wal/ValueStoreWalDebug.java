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
package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Package-private debug hook that allows tests to observe when WAL files are forced to disk.
 */
final class ValueStoreWalDebug {

	private static volatile Consumer<Path> forceListener;

	private ValueStoreWalDebug() {
	}

	static void setForceListener(Consumer<Path> listener) {
		forceListener = listener;
	}

	static void clearForceListener() {
		forceListener = null;
	}

	static void fireForceEvent(Path path) {
		Consumer<Path> listener = forceListener;
		if (listener != null) {
			listener.accept(Objects.requireNonNull(path, "path"));
		}
	}
}
