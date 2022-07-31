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
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.rdf4j.common.io.NioFile;

/**
 * Class supplying access to a hash file.
 *
 * @author Arjohn Kampman
 */
public class HashFile implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	// The size of an item (32-bit hash + 32-bit ID), in bytes
	private static final int ITEM_SIZE = 8;

	/**
	 * Magic number "Native Hash File" to detect whether the file is actually a hash file. The first three bytes of the
	 * file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'h', 'f' };

	/**
	 * File format version, stored as the fourth byte in hash files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	/**
	 * The size of the file header in bytes. The file header contains the following data: magic number (3 bytes) file
	 * format version (1 byte), number of buckets (4 bytes), bucket size (4 bytes) and number of stored items (4 bytes).
	 */
	private static final long HEADER_LENGTH = 16;

	private static final int INIT_BUCKET_SIZE = 8;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final NioFile nioFile;

	private final boolean forceSync;

	// The number of (non-overflow) buckets in the hash file
	private volatile int bucketCount;

	// The number of items that can be stored in a bucket
	private final int bucketSize;

	// The number of items in the hash file
	private volatile int itemCount;

	// Load factor (fixed, for now)
	private final float loadFactor;

	// recordSize = ITEM_SIZE * bucketSize + 4
	private final int recordSize;

	// first prime > 5MB
	private final BitSet poorMansBloomFilter;

	boolean loadedHashFileFromDisk = false;

	/**
	 * A read/write lock that is used to prevent structural changes to the hash file while readers are active in order
	 * to prevent concurrency issues.
	 */
	private final ReentrantReadWriteLock structureLock = new ReentrantReadWriteLock();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public HashFile(File file) throws IOException {
		this(file, false);
	}

	public HashFile(File file, boolean forceSync) throws IOException {
		this(file, forceSync, 512); // 512 is default initial size
	}

	public HashFile(File file, boolean forceSync, int initialSize) throws IOException {
		this.nioFile = new NioFile(file);
		this.forceSync = forceSync;
		loadFactor = 0.75f;

		try {
			if (nioFile.size() == 0L) {
				// Empty file, insert bucket count, bucket size
				// and item count at the start of the file

				// the bucket count handles sizes not divisible by INIT_BUCKET_SIZE
				bucketCount = (int) Math.ceil(initialSize * 1.0 / INIT_BUCKET_SIZE);
				bucketSize = INIT_BUCKET_SIZE;
				itemCount = 0;
				recordSize = ITEM_SIZE * bucketSize + 4;

				// Initialize the file by writing <_bucketCount> empty buckets
				writeEmptyBuckets(HEADER_LENGTH, bucketCount);

				sync();
			} else {
				// Read bucket count, bucket size and item count from the file
				ByteBuffer buf = ByteBuffer.allocate((int) HEADER_LENGTH);
				nioFile.read(buf, 0L);
				buf.rewind();

				if (buf.remaining() < HEADER_LENGTH) {
					throw new IOException("File too short to be a compatible hash file");
				}

				byte[] magicNumber = new byte[MAGIC_NUMBER.length];
				buf.get(magicNumber);
				byte version = buf.get();
				bucketCount = buf.getInt();
				bucketSize = buf.getInt();
				itemCount = buf.getInt();

				if (!Arrays.equals(MAGIC_NUMBER, magicNumber)) {
					throw new IOException("File doesn't contain compatible hash file data");
				}

				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read hash file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read hash file; invalid file format version: " + version);
				}

				recordSize = ITEM_SIZE * bucketSize + 4;
				loadedHashFileFromDisk = itemCount > 0;
			}

			if (!loadedHashFileFromDisk) {
				// 41943049 is ~5MB, and a prime
				if (initialSize > 41943049) {
					// initialSize < Integer.MAX_VALUE and Integer.MAX_VALUE = ~250 MB
					poorMansBloomFilter = new BitSet(initialSize);
				} else {
					poorMansBloomFilter = new BitSet(41943049);
				}
			} else {
				poorMansBloomFilter = null;
			}
		} catch (IOException e) {
			this.nioFile.close();
			throw e;
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getFile() {
		return nioFile.getFile();
	}

	public int getItemCount() {
		return itemCount;
	}

	/**
	 * Gets an iterator that iterates over the IDs with hash codes that match the specified hash code.
	 */
	public IDIterator getIDIterator(int hash) throws IOException {
		if (!loadedHashFileFromDisk && !poorMansBloomFilter.get(getBloomFilterIndex(hash))) {
			return emptyIDIterator;
		} else {
			return new IDIterator(hash);
		}
	}

	private int getBloomFilterIndex(int hash) {
		return Math.abs(hash) % poorMansBloomFilter.size();
	}

	/**
	 * Stores ID under the specified hash code in this hash file.
	 */
	public void storeID(int hash, int id) throws IOException {
		structureLock.readLock().lock();
		if (!loadedHashFileFromDisk) {
			poorMansBloomFilter.set(getBloomFilterIndex(hash), true);
		}
		try {
			// Calculate bucket offset for initial bucket
			long bucketOffset = getBucketOffset(hash);
			storeID(bucketOffset, hash, id);
		} finally {
			structureLock.readLock().unlock();
		}

		if (++itemCount >= loadFactor * bucketCount * bucketSize) {
			structureLock.writeLock().lock();
			try {
				increaseHashTable();
			} finally {
				structureLock.writeLock().unlock();
			}
		}
	}

	private void storeID(long bucketOffset, int hash, int id) throws IOException {
		ByteBuffer bucket = ByteBuffer.allocate(recordSize);

		while (true) {
			nioFile.read(bucket, bucketOffset);

			// Find first empty slot in bucket
			int slotID = findEmptySlotInBucket(bucket);

			if (slotID >= 0) {
				// Empty slot found, store dataOffset in it

				ByteBuffer diff = ByteBuffer.allocate(8);
				diff.putInt(hash);
				diff.putInt(id);
				diff.rewind();

				nioFile.write(diff, bucketOffset + ITEM_SIZE * slotID);
				break;
			} else {
				// No empty slot found, check if bucket has an overflow bucket
				int overflowID = bucket.getInt(ITEM_SIZE * bucketSize);

				if (overflowID == 0) {
					// No overflow bucket yet, create one
					overflowID = createOverflowBucket();

					// Link overflow bucket to current bucket
					bucket.putInt(ITEM_SIZE * bucketSize, overflowID);
					bucket.rewind();
					nioFile.write(bucket, bucketOffset);
				}

				// Continue searching for an empty slot in the overflow bucket
				bucketOffset = getOverflowBucketOffset(overflowID);
				bucket.clear();
			}
		}
	}

	public void clear() throws IOException {
		structureLock.writeLock().lock();
		poorMansBloomFilter.clear();
		try {
			// Truncate the file to remove any overflow buffers
			nioFile.truncate(HEADER_LENGTH + (long) bucketCount * recordSize);

			// Overwrite normal buckets with empty ones
			writeEmptyBuckets(HEADER_LENGTH, bucketCount);

			itemCount = 0;
		} finally {
			structureLock.writeLock().unlock();
		}
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	public void sync() throws IOException {
		structureLock.readLock().lock();
		try {
			// Update the file header
			writeFileHeader();
		} finally {
			structureLock.readLock().unlock();
		}

		if (forceSync) {
			nioFile.force(false);
		}
	}

	public void sync(boolean force) throws IOException {
		sync();
		nioFile.force(force);
	}

	@Override
	public void close() throws IOException {
		nioFile.close();
	}

	/*-----------------*
	 * Utility methods *
	 *-----------------*/

	private RandomAccessFile createEmptyFile(File file) throws IOException {
		// Make sure the file exists
		if (!file.exists()) {
			boolean created = file.createNewFile();
			if (!created) {
				throw new IOException("Failed to create file " + file);
			}
		}

		// Open the file in read-write mode and make sure the file is empty
		RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.setLength(0L);

		return raf;
	}

	/**
	 * Writes the bucket count, bucket size and item count to the file header.
	 */
	private void writeFileHeader() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate((int) HEADER_LENGTH);
		buf.put(MAGIC_NUMBER);
		buf.put(FILE_FORMAT_VERSION);
		buf.putInt(bucketCount);
		buf.putInt(bucketSize);
		buf.putInt(itemCount);
		buf.rewind();

		nioFile.write(buf, 0L);
	}

	/**
	 * Returns the offset of the bucket for the specified hash code.
	 */
	private long getBucketOffset(int hash) {
		int bucketNo = hash % bucketCount;
		if (bucketNo < 0) {
			bucketNo += bucketCount;
		}
		return HEADER_LENGTH + (long) bucketNo * recordSize;
	}

	/**
	 * Returns the offset of the overflow bucket with the specified ID.
	 */
	private long getOverflowBucketOffset(int bucketID) {
		return HEADER_LENGTH + ((long) bucketCount + (long) bucketID - 1L) * recordSize;
	}

	/**
	 * Creates a new overflow bucket and returns its ID.
	 */
	private int createOverflowBucket() throws IOException {
		long offset = nioFile.size();
		writeEmptyBuckets(offset, 1);
		return (int) ((offset - HEADER_LENGTH) / recordSize) - bucketCount + 1;
	}

	private void writeEmptyBuckets(long fileOffset, int bucketCount) throws IOException {
		ByteBuffer emptyBucket = ByteBuffer.allocate(recordSize);

		for (int i = 0; i < bucketCount; i++) {
			nioFile.write(emptyBucket, fileOffset + i * (long) recordSize);
			emptyBucket.rewind();
		}
	}

	private int findEmptySlotInBucket(ByteBuffer bucket) {
		for (int slotNo = 0; slotNo < bucketSize; slotNo++) {
			// Check for offsets that are equal to 0
			if (bucket.getInt(ITEM_SIZE * slotNo + 4) == 0) {
				return slotNo;
			}
		}

		return -1;
	}

	/**
	 * Double the number of buckets in the hash file and rehashes the stored items.
	 */
	private void increaseHashTable() throws IOException {
		long oldTableSize = HEADER_LENGTH + (long) bucketCount * recordSize;
		long newTableSize = HEADER_LENGTH + (long) bucketCount * recordSize * 2;
		long oldFileSize = nioFile.size(); // includes overflow buckets

		// Move any overflow buckets out of the way to a temporary file
		File tmpFile = new File(getFile().getParentFile(), "rehash_" + getFile().getName());
		try (RandomAccessFile tmpRaf = createEmptyFile(tmpFile)) {
			FileChannel tmpChannel = tmpRaf.getChannel();
			// Transfer the overflow buckets to the temp file
			// FIXME: work around java bug 6431344:
			// "FileChannel.transferTo() doesn't work if address space runs out"
			nioFile.transferTo(oldTableSize, oldFileSize - oldTableSize, tmpChannel);
			// Increase hash table by factor 2
			writeEmptyBuckets(oldTableSize, bucketCount);
			bucketCount *= 2;
			// Discard any remaining overflow buffers
			nioFile.truncate(newTableSize);
			ByteBuffer bucket = ByteBuffer.allocate(recordSize);
			ByteBuffer newBucket = ByteBuffer.allocate(recordSize);
			// Rehash items in non-overflow buckets, half of these will move to a
			// new location, but none of them will trigger the creation of new overflow
			// buckets. Any (now deprecated) references to overflow buckets are
			// removed too.

			// All items that are moved to a new location end up in one and the same
			// new and empty bucket. All items are divided between the old and the
			// new bucket and the changes to the buckets are written to disk only once.
			for (long bucketOffset = HEADER_LENGTH; bucketOffset < oldTableSize; bucketOffset += recordSize) {
				nioFile.read(bucket, bucketOffset);

				boolean bucketChanged = false;
				long newBucketOffset = 0L;

				for (int slotNo = 0; slotNo < bucketSize; slotNo++) {
					int id = bucket.getInt(ITEM_SIZE * slotNo + 4);

					if (id != 0) {
						// Slot is not empty
						int hash = bucket.getInt(ITEM_SIZE * slotNo);
						long newOffset = getBucketOffset(hash);

						if (newOffset != bucketOffset) {
							// Move this item to new bucket...
							newBucket.putInt(hash);
							newBucket.putInt(id);

							// ...and remove it from the current bucket
							bucket.putInt(ITEM_SIZE * slotNo, 0);
							bucket.putInt(ITEM_SIZE * slotNo + 4, 0);

							bucketChanged = true;
							newBucketOffset = newOffset;
						}
					}
				}

				if (bucketChanged) {
					// Some of the items were moved to the new bucket, write it to
					// the file
					newBucket.flip();
					nioFile.write(newBucket, newBucketOffset);
					newBucket.clear();
				}

				// Reset overflow ID in the old bucket to 0 if necessary
				if (bucket.getInt(ITEM_SIZE * bucketSize) != 0) {
					bucket.putInt(ITEM_SIZE * bucketSize, 0);
					bucketChanged = true;
				}

				if (bucketChanged) {
					// Some of the items were moved to the new bucket or the
					// overflow ID has been reset; write the bucket back to the file
					bucket.rewind();
					nioFile.write(bucket, bucketOffset);
				}

				bucket.clear();
			} // Rehash items in overflow buckets. This might trigger the creation of
				// new overflow buckets so we can't optimize this in the same way as we
				// rehash the normal buckets.
			long tmpFileSize = tmpChannel.size();
			for (long bucketOffset = 0L; bucketOffset < tmpFileSize; bucketOffset += recordSize) {
				tmpChannel.read(bucket, bucketOffset);

				for (int slotNo = 0; slotNo < bucketSize; slotNo++) {
					int id = bucket.getInt(ITEM_SIZE * slotNo + 4);

					if (id != 0) {
						// Slot is not empty
						int hash = bucket.getInt(ITEM_SIZE * slotNo);
						long newBucketOffset = getBucketOffset(hash);

						// Copy this item to its new location
						storeID(newBucketOffset, hash, id);
					}
				}

				bucket.clear();
			}
			// Discard the temp file
		}
		tmpFile.delete();
	}

	/*------------------------*
	 * Inner class IDIterator *
	 *------------------------*/

	private final IDIterator emptyIDIterator = new IDIterator() {
		@Override
		public void close() {

		}

		@Override
		public int next() throws IOException {
			return -1;
		}
	};

	public class IDIterator {

		private final int queryHash;

		private ByteBuffer bucketBuffer;

		private int slotNo;

		private IDIterator(int hash) throws IOException {
			queryHash = hash;
			bucketBuffer = ByteBuffer.allocate(recordSize);

			structureLock.readLock().lock();
			try {
				// Read initial bucket
				long bucketOffset = getBucketOffset(hash);
				nioFile.read(bucketBuffer, bucketOffset);

				slotNo = -1;
			} catch (IOException | RuntimeException e) {
				structureLock.readLock().unlock();
				throw e;
			}
		}

		IDIterator() {
			queryHash = 0;
		}

		public void close() {
			bucketBuffer = null;
			structureLock.readLock().unlock();
		}

		/**
		 * Returns the next ID that has been mapped to the specified hash code, or <var>-1</var> if no more IDs were
		 * found.
		 */
		public int next() throws IOException {
			while (bucketBuffer != null) {
				// Search in current bucket
				while (++slotNo < bucketSize) {
					if (bucketBuffer.getInt(ITEM_SIZE * slotNo) == queryHash) {
						return bucketBuffer.getInt(ITEM_SIZE * slotNo + 4);
					}
				}

				// No matching hash code in current bucket, check overflow
				// bucket
				int overflowID = bucketBuffer.getInt(ITEM_SIZE * bucketSize);
				if (overflowID == 0) {
					// No overflow bucket, end the search
					bucketBuffer = null;
					break;
				} else {
					// Continue with overflow bucket
					bucketBuffer.clear();
					long bucketOffset = getOverflowBucketOffset(overflowID);
					nioFile.read(bucketBuffer, bucketOffset);
					slotNo = -1;
				}
			}

			return -1;
		}

	} // End inner class IDIterator
} // End class HashFile
