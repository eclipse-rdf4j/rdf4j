/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import org.lwjgl.util.lmdb.LMDB;

/**
 * Reference for a LMDB transaction.
 *
 * @author Ken Wenzel
 */
public class TxnRef {
	enum Mode {
		RESET,
		ABORT,
		NONE
	};

	private long txn;
	private long refCount;
	private final Mode mode;
	private boolean mustRenew = false;

	public TxnRef(long txn, Mode mode) {
		this.txn = txn;
		this.mode = mode;
	}

	public synchronized void begin() {
		refCount++;
		if (mustRenew) {
			LMDB.mdb_txn_renew(txn);
			mustRenew = false;
		}
	}

	public synchronized void end() {
		if (--refCount <= 0) {
			switch (mode) {
			case RESET:
				LMDB.mdb_txn_reset(txn);
				mustRenew = true;
				break;
			case ABORT:
				LMDB.mdb_txn_abort(txn);
				break;
			case NONE:
				break;
			}
			refCount = 0;
		}
	}

	public long get() {
		return txn;
	}
}
