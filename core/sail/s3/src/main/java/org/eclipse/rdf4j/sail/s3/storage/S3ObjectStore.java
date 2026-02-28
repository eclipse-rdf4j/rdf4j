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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;

/**
 * {@link ObjectStore} implementation backed by an S3-compatible service via MinIO client.
 */
public class S3ObjectStore implements ObjectStore {

	private final MinioClient client;
	private final String bucket;
	private final String prefix;

	public S3ObjectStore(String bucket, String endpoint, String region, String prefix,
			String accessKey, String secretKey, boolean forcePathStyle) {
		this.bucket = bucket;
		if (prefix == null || prefix.isEmpty()) {
			this.prefix = "";
		} else if (prefix.endsWith("/")) {
			this.prefix = prefix;
		} else {
			this.prefix = prefix + "/";
		}

		this.client = MinioClient.builder()
				.endpoint(endpoint)
				.credentials(accessKey, secretKey)
				.region(region)
				.build();
	}

	private String resolve(String key) {
		return prefix + key;
	}

	@Override
	public void put(String key, byte[] data) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			client.putObject(PutObjectArgs.builder()
					.bucket(bucket)
					.object(resolve(key))
					.stream(bais, data.length, -1)
					.build());
		} catch (Exception e) {
			throw new UncheckedIOException(new IOException("Failed to put " + key, e));
		}
	}

	@Override
	public byte[] get(String key) {
		return executeGet(GetObjectArgs.builder()
				.bucket(bucket)
				.object(resolve(key))
				.build(), key);
	}

	@Override
	public byte[] getRange(String key, long offset, long length) {
		return executeGet(GetObjectArgs.builder()
				.bucket(bucket)
				.object(resolve(key))
				.offset(offset)
				.length(length)
				.build(), key);
	}

	private byte[] executeGet(GetObjectArgs args, String key) {
		try (InputStream is = client.getObject(args)) {
			return is.readAllBytes();
		} catch (ErrorResponseException e) {
			if ("NoSuchKey".equals(e.errorResponse().code())) {
				return null;
			}
			throw new UncheckedIOException(new IOException("Failed to get " + key, e));
		} catch (Exception e) {
			throw new UncheckedIOException(new IOException("Failed to get " + key, e));
		}
	}

	@Override
	public void delete(String key) {
		try {
			client.removeObject(RemoveObjectArgs.builder()
					.bucket(bucket)
					.object(resolve(key))
					.build());
		} catch (Exception e) {
			throw new UncheckedIOException(new IOException("Failed to delete " + key, e));
		}
	}

	@Override
	public List<String> list(String subPrefix) {
		List<String> keys = new ArrayList<>();
		String fullPrefix = resolve(subPrefix);
		Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder()
				.bucket(bucket)
				.prefix(fullPrefix)
				.recursive(true)
				.build());
		try {
			for (Result<Item> result : results) {
				String objectKey = result.get().objectName();
				// Strip the store prefix to return relative keys
				if (objectKey.startsWith(prefix)) {
					keys.add(objectKey.substring(prefix.length()));
				} else {
					keys.add(objectKey);
				}
			}
		} catch (Exception e) {
			throw new UncheckedIOException(new IOException("Failed to list " + subPrefix, e));
		}
		return keys;
	}

	@Override
	public void close() {
		// MinioClient doesn't need explicit close
	}
}
