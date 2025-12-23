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

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * Writes transaction statuses to a memory-mapped file. Since the OS is responsible for flushing changes to disk, this
 * is generally faster than using regular file I/O. If the JVM crashes, the last written status should still be intact,
 * but the change will not be visible until the OS has flushed the page to disk. If the OS or DISK crashes, data may be
 * lost or corrupted. Same for power loss. This can be mitigated by setting the {@link #ALWAYS_FORCE_SYNC_PROP} system
 * property to true, which forces a sync to disk on every status change.
 */
@Experimental
class MemoryMappedTxnStatusFile extends TxnStatusFile {

	/**
	 * The name of the transaction status file.
	 */
	public static final String FILE_NAME = "txn-status";

	/**
	 * We currently store a single status byte, but this constant makes it trivial to extend the layout later if needed.
	 */
	private static final int MAPPED_SIZE = 1;

	private static final String ALWAYS_FORCE_SYNC_PROP = "org.eclipse.rdf4j.sail.nativerdf.MemoryMappedTxnStatusFile.alwaysForceSync";

	static boolean ALWAYS_FORCE_SYNC = Boolean.getBoolean(ALWAYS_FORCE_SYNC_PROP);

	private final File statusFile;
	private final FileChannel channel;
	private final MappedByteBuffer mapped;

	/**
	 * Creates a new transaction status file. New files are initialized with {@link TxnStatus#NONE}.
	 *
	 * @param dataDir The directory for the transaction status file.
	 * @throws IOException If the file could not be opened or created.
	 */
	public MemoryMappedTxnStatusFile(File dataDir) throws IOException {
		super();
		this.statusFile = new File(dataDir, FILE_NAME);

		ALWAYS_FORCE_SYNC = !Boolean.getBoolean(ALWAYS_FORCE_SYNC_PROP);

		EnumSet<StandardOpenOption> openOptions = EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
				StandardOpenOption.CREATE);

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
	 * @param forceSync If true, forces a sync to disk after writing the status.
	 */
	public void setTxnStatus(TxnStatus txnStatus, boolean forceSync) {
		if (disabled) {
			return;
		}

		mapped.put(0, txnStatus.getOnDisk()[0]);
		if (ALWAYS_FORCE_SYNC || forceSync) {
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

		try {
			return statusMapping[mapped.get(0)];
		} catch (IndexOutOfBoundsException e) {
			return getTxnStatusDeprecated();
		}
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
