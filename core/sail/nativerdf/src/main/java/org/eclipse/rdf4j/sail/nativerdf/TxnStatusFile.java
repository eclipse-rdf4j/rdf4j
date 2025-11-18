/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

/**
 * Writes transaction statuses to a file.
 */
class TxnStatusFile {

	boolean disabled = false;

	public void disable() {
		this.disabled = true;
	}

	public enum TxnStatus {

		/**
		 * No active transaction. This occurs if no transaction has been started yet, or if all transactions have been
		 * committed or rolled back. An empty TxnStatus file also represents the NONE status.
		 */
		NONE(TxnStatus.NONE_BYTE),

		/**
		 * A transaction has been started, but was not yet committed or rolled back.
		 */
		ACTIVE(TxnStatus.ACTIVE_BYTE),

		/**
		 * A transaction is being committed.
		 */
		COMMITTING(TxnStatus.COMMITTING_BYTE),

		/**
		 * A transaction is being rolled back.
		 */
		ROLLING_BACK(TxnStatus.ROLLING_BACK_BYTE),

		/**
		 * The transaction status is unknown.
		 */
		UNKNOWN(TxnStatus.UNKNOWN_BYTE);

		private final byte[] onDisk;

		TxnStatus(byte onDisk) {
			this.onDisk = new byte[1];
			this.onDisk[0] = onDisk;
		}

		byte[] getOnDisk() {
			return onDisk;
		}

		private static final byte NONE_BYTE = (byte) 0b00000000;
		private static final byte OLD_NONE_BYTE = (byte) 0b00000001;

		private static final byte ACTIVE_BYTE = (byte) 0b00000010;
		private static final byte COMMITTING_BYTE = (byte) 0b00000100;
		private static final byte ROLLING_BACK_BYTE = (byte) 0b00001000;
		private static final byte UNKNOWN_BYTE = (byte) 0b00010000;

	}

	/**
	 * The name of the transaction status file.
	 */
	public static final String FILE_NAME = "txn-status";

	/**
	 * We currently store a single status byte, but this constant makes it trivial to extend the layout later if needed.
	 */
	private static final int MAPPED_SIZE = 1;

	private static final String DISABLE_DSYNC_PROPERTY = "org.eclipse.rdf4j.sail.nativerdf.disableTxnStatusDsync";

	static boolean DISABLE_DSYNC = Boolean.getBoolean(DISABLE_DSYNC_PROPERTY);

	private final File statusFile;
	private final FileChannel channel;
	private final MappedByteBuffer mapped;

	/**
	 * Creates a new transaction status file. New files are initialized with {@link TxnStatus#NONE}.
	 *
	 * @param dataDir The directory for the transaction status file.
	 * @throws IOException If the file could not be opened or created.
	 */
	public TxnStatusFile(File dataDir) throws IOException {
		this.statusFile = new File(dataDir, FILE_NAME);

		DISABLE_DSYNC = !Boolean.getBoolean(DISABLE_DSYNC_PROPERTY);

		EnumSet<StandardOpenOption> openOptions = EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE);
		if (!DISABLE_DSYNC) {
			openOptions.add(StandardOpenOption.DSYNC);
		}

		this.channel = FileChannel.open(statusFile.toPath(), openOptions.toArray(new StandardOpenOption[0]));

		long size = channel.size();

		// Ensure the file is at least MAPPED_SIZE bytes so we can map it safely.
		// If it was previously empty, we treat that as NONE (which is also byte 0).
		if (size < MAPPED_SIZE) {
			channel.position(MAPPED_SIZE - 1);
			int write = channel.write(ByteBuffer.wrap(TxnStatus.NONE.getOnDisk()));
			if (write != 1) {
				throw new IOException("Failed to initialize transaction status file");
			}
			channel.force(true);
		}

		this.mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, MAPPED_SIZE);
	}

	public void close() throws IOException {
		// We rely on the GC to eventually unmap the MappedByteBuffer; explicitly
		// closing the channel is enough for our purposes here.
		channel.close();
	}

	/**
	 * Writes the specified transaction status to file.
	 *
	 * @param txnStatus The transaction status to write.
	 * @param forceSync
	 */
	public void setTxnStatus(TxnStatus txnStatus, boolean forceSync) {
		if (disabled) {
			return;
		}

		mapped.put(0, txnStatus.getOnDisk()[0]);
		if (forceSync) {
			mapped.force();
		}
	}

	/**
	 * Reads the transaction status from file.
	 *
	 * @return The read transaction status, or {@link TxnStatus#UNKNOWN} when the file contains an unrecognized status
	 *         string.
	 * @throws IOException If the transaction status file could not be read.
	 */
	public TxnStatus getTxnStatus() throws IOException {
		if (disabled) {
			return TxnStatus.NONE;
		}

		byte b = mapped.get(0);
		try {
			return statusMapping[b];
		} catch (IndexOutOfBoundsException e) {
			return getTxnStatusDeprecated();
		}
	}

	private final static TxnStatus[] statusMapping = new TxnStatus[17];
	static {
		statusMapping[TxnStatus.NONE_BYTE] = TxnStatus.NONE;
		statusMapping[TxnStatus.OLD_NONE_BYTE] = TxnStatus.NONE;
		statusMapping[TxnStatus.ACTIVE_BYTE] = TxnStatus.ACTIVE;
		statusMapping[TxnStatus.COMMITTING_BYTE] = TxnStatus.COMMITTING;
		statusMapping[TxnStatus.ROLLING_BACK_BYTE] = TxnStatus.ROLLING_BACK;
		statusMapping[TxnStatus.UNKNOWN_BYTE] = TxnStatus.UNKNOWN;
	}

	private TxnStatus getTxnStatusDeprecated() throws IOException {
		if (disabled) {
			return TxnStatus.NONE;
		}

		// Read the full file contents as a string, for compatibility with very old
		// versions that stored the enum name instead of a bitfield.
		byte[] bytes = Files.readAllBytes(statusFile.toPath());

		if (bytes.length == 0) {
			return TxnStatus.NONE;
		}

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
