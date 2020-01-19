/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.eclipse.rdf4j.common.io.NioFile;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Writes transaction statuses to a file.
 */
class TxnStatusFile {

	public static enum TxnStatus {

		/**
		 * No active transaction. This occurs if no transaction has been started yet, or if all transactions have been
		 * committed or rolled back.
		 */
		NONE((byte) 1),

		/**
		 * A transaction has been started, but was not yet committed or rolled back.
		 */
		ACTIVE((byte) 2),

		/**
		 * A transaction is being committed.
		 */
		COMMITTING((byte) 3),

		/**
		 * A transaction is being rolled back.
		 */
		ROLLING_BACK((byte) 4),

		/**
		 * The transaction status is unknown.
		 */
		UNKNOWN((byte) 5);

		byte[] onDisk;

		TxnStatus(byte onDisk) {
			this.onDisk = new byte[1];
			this.onDisk[0] = onDisk;
		}
	}

	/**
	 * The name of the transaction status file.
	 */
	public static final String FILE_NAME = "txn-status";

	private final NioFile nioFile;

	/**
	 * Creates a new transaction status file. New files are initialized with {@link TxnStatus#NONE}.
	 *
	 * @param dataDir The directory for the transaction status file.
	 * @throws IOException If the file did not yet exist and could not be written to.
	 */
	public TxnStatusFile(File dataDir) throws IOException {
		File statusFile = new File(dataDir, FILE_NAME);
		nioFile = new NioFile(statusFile, "rwd");

		if (nioFile.size() == 0) {
			setTxnStatus(TxnStatus.NONE);
		}
	}

	public void close() throws IOException {
		nioFile.close();
	}

	/**
	 * Writes the specified transaction status to file.
	 *
	 * @param txnStatus The transaction status to write.
	 * @throws IOException If the transaction status could not be written to file.
	 */
	public void setTxnStatus(TxnStatus txnStatus) throws IOException {
		byte[] bytes = txnStatus.onDisk;
		nioFile.truncate(bytes.length);
		nioFile.writeBytes(bytes, 0);
	}

	/**
	 * Reads the transaction status from file.
	 *
	 * @return The read transaction status, or {@link TxnStatus#UNKNOWN} when the file contains an unrecognized status
	 *         string.
	 * @throws IOException If the transaction status file could not be read.
	 */
	public TxnStatus getTxnStatus() throws IOException {
		byte[] bytes = nioFile.readBytes(0, 1);

		TxnStatus status;

		switch (bytes[0]) {
		case 1:
			status = TxnStatus.NONE;
			break;
		case 2:
			status = TxnStatus.ACTIVE;
			break;
		case 3:
			status = TxnStatus.COMMITTING;
			break;
		case 4:
			status = TxnStatus.ROLLING_BACK;
			break;
		case 5:
			status = TxnStatus.UNKNOWN;
			break;
		default:
			status = getTxnStatusDeprecated();
		}

		return status;

	}

	private TxnStatus getTxnStatusDeprecated() throws IOException {
		byte[] bytes = nioFile.readBytes(0, (int) nioFile.size());

		String s = new String(bytes, US_ASCII);
		try {
			return TxnStatus.valueOf(s);
		} catch (IllegalArgumentException e) {
			// use platform encoding for backwards compatibility with versions
			// older than 2.6.6:
			s = new String(bytes);
			try {
				return TxnStatus.valueOf(s);
			} catch (IllegalArgumentException e2) {
				return TxnStatus.UNKNOWN;
			}
		}
	}
}
