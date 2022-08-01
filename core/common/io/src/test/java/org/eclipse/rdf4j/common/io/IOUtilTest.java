/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.Test;

class IOUtilTest {

	@Test
	void shouldWriteVarInt() throws IOException {
		// test some basic numbers
		shouldWriteVarInt(0);
		shouldWriteVarInt(1);
		shouldWriteVarInt(2);
		shouldWriteVarInt(Integer.MAX_VALUE);

		// test all numbers up to 1048576
		for (int i = 0; i < 1024 * 1024; i++) {
			shouldWriteVarInt(i);
		}

		// test random positive integers
		Random rng = new Random(328982033);
		for (int i = 2; i < 10_000; i++) {
			shouldWriteVarInt(rng.nextInt(Integer.MAX_VALUE));
		}
	}

	private void shouldWriteVarInt(int value) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtil.writeVarInt(out, value);
		byte[] bytes = out.toByteArray();

		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		int read = IOUtil.readVarInt(in);

		assertThat(value).isEqualTo(read);
	}

	@Test
	void shouldRejectWritingNegativeVarInt() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertThatThrownBy(() -> IOUtil.writeVarInt(out, -1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> IOUtil.writeVarInt(out, Integer.MIN_VALUE))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowEOFException() {
		// the single byte in this array indicates that more bytes should follow in the varint format, but none will
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { -2 });

		assertThatThrownBy(() -> IOUtil.readVarInt(in))
				.isInstanceOf(EOFException.class);
	}

}
