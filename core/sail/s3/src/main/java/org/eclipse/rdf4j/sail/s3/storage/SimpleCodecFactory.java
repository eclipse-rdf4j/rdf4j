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
import java.nio.ByteBuffer;

import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import com.github.luben.zstd.Zstd;

/**
 * A lightweight {@link CompressionCodecFactory} that handles ZSTD and UNCOMPRESSED codecs without any Hadoop
 * dependencies. Uses {@code zstd-jni} directly for ZSTD compression/decompression.
 */
final class SimpleCodecFactory implements CompressionCodecFactory {

	static final SimpleCodecFactory INSTANCE = new SimpleCodecFactory();

	private SimpleCodecFactory() {
	}

	@Override
	public BytesInputCompressor getCompressor(CompressionCodecName codec) {
		switch (codec) {
		case ZSTD:
			return ZSTD_COMPRESSOR;
		case UNCOMPRESSED:
			return NOOP_COMPRESSOR;
		default:
			throw new UnsupportedOperationException("Unsupported compression codec: " + codec);
		}
	}

	@Override
	public BytesInputDecompressor getDecompressor(CompressionCodecName codec) {
		switch (codec) {
		case ZSTD:
			return ZSTD_DECOMPRESSOR;
		case UNCOMPRESSED:
			return NOOP_DECOMPRESSOR;
		default:
			throw new UnsupportedOperationException("Unsupported compression codec: " + codec);
		}
	}

	@Override
	public void release() {
		// no resources to release
	}

	private static final BytesInputCompressor ZSTD_COMPRESSOR = new BytesInputCompressor() {
		@Override
		public BytesInput compress(BytesInput bytes) throws IOException {
			byte[] input = bytes.toByteArray();
			byte[] compressed = Zstd.compress(input);
			return BytesInput.from(compressed);
		}

		@Override
		public CompressionCodecName getCodecName() {
			return CompressionCodecName.ZSTD;
		}

		@Override
		public void release() {
		}
	};

	private static final BytesInputDecompressor ZSTD_DECOMPRESSOR = new BytesInputDecompressor() {
		@Override
		public BytesInput decompress(BytesInput bytes, int uncompressedSize) throws IOException {
			byte[] input = bytes.toByteArray();
			byte[] decompressed = new byte[uncompressedSize];
			Zstd.decompress(decompressed, input);
			return BytesInput.from(decompressed);
		}

		@Override
		public void decompress(ByteBuffer input, int compressedSize, ByteBuffer output, int uncompressedSize)
				throws IOException {
			byte[] compressedBytes = new byte[compressedSize];
			input.get(compressedBytes);
			byte[] decompressed = new byte[uncompressedSize];
			Zstd.decompress(decompressed, compressedBytes);
			output.put(decompressed);
		}

		@Override
		public void release() {
		}
	};

	private static final BytesInputCompressor NOOP_COMPRESSOR = new BytesInputCompressor() {
		@Override
		public BytesInput compress(BytesInput bytes) throws IOException {
			return bytes;
		}

		@Override
		public CompressionCodecName getCodecName() {
			return CompressionCodecName.UNCOMPRESSED;
		}

		@Override
		public void release() {
		}
	};

	private static final BytesInputDecompressor NOOP_DECOMPRESSOR = new BytesInputDecompressor() {
		@Override
		public BytesInput decompress(BytesInput bytes, int uncompressedSize) throws IOException {
			return bytes;
		}

		@Override
		public void decompress(ByteBuffer input, int compressedSize, ByteBuffer output, int uncompressedSize)
				throws IOException {
			byte[] data = new byte[compressedSize];
			input.get(data);
			output.put(data);
		}

		@Override
		public void release() {
		}
	};
}
