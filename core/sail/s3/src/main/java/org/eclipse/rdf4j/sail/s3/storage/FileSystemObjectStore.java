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
package org.eclipse.rdf4j.sail.s3.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link ObjectStore} implementation backed by the local filesystem. Stores each key as a file under the configured
 * root directory, creating subdirectories as needed.
 */
public class FileSystemObjectStore implements ObjectStore {

	private final Path root;

	public FileSystemObjectStore(Path root) {
		this.root = root;
	}

	private Path resolve(String key) {
		return root.resolve(key);
	}

	@Override
	public void put(String key, byte[] data) {
		try {
			Path target = resolve(key);
			Files.createDirectories(target.getParent());
			Files.write(target, data);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public byte[] get(String key) {
		try {
			Path target = resolve(key);
			if (!Files.exists(target)) {
				return null;
			}
			return Files.readAllBytes(target);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public byte[] getRange(String key, long offset, long length) {
		Path target = resolve(key);
		if (!Files.exists(target)) {
			return null;
		}
		try (RandomAccessFile raf = new RandomAccessFile(target.toFile(), "r")) {
			long fileLen = raf.length();
			int start = (int) Math.min(offset, fileLen);
			int readLen = (int) Math.min(length, fileLen - start);
			if (readLen <= 0) {
				return new byte[0];
			}
			raf.seek(start);
			byte[] buf = new byte[readLen];
			raf.readFully(buf);
			return buf;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Path target = resolve(key);
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public List<String> list(String subPrefix) {
		List<String> result = new ArrayList<>();
		Path prefixPath = resolve(subPrefix);
		Path searchDir = Files.isDirectory(prefixPath) ? prefixPath : prefixPath.getParent();
		if (searchDir == null || !Files.exists(searchDir)) {
			return result;
		}
		try (Stream<Path> walk = Files.walk(searchDir)) {
			walk.filter(Files::isRegularFile)
					.forEach(p -> {
						String relative = root.relativize(p).toString();
						if (relative.startsWith(subPrefix)) {
							result.add(relative);
						}
					});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return result;
	}

	@Override
	public void close() {
		// no-op
	}
}
