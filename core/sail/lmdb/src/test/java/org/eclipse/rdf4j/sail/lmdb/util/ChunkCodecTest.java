package org.eclipse.rdf4j.sail.lmdb.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

class ChunkCodecTest {

	@Test
	void roundTripPrefersPackedEncodingWhenSmaller() {
		long[][] tuples = {
				{ 1L, 2L, 1_000L, 300L },
				{ 1L, 2L, 1_001L, 600L },
				{ 1L, 2L, 1_002L, 300L },
				{ 1L, 2L, 1_003L, 900L }
		};

		ChunkOutput output = new ChunkOutput(tuples.length, 4, 2);
		for (long[] tuple : tuples) {
			assertTrue(output.addTuple(tuple));
		}

		ByteBuffer keyBuffer = ByteBuffer.allocate(64).order(ByteOrder.nativeOrder());
		ByteBuffer valueBuffer = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder());
		output.write(keyBuffer, valueBuffer);

		keyBuffer.flip();
		valueBuffer.flip();

		ChunkInput input = new ChunkInput(keyBuffer.duplicate(), valueBuffer.duplicate(), 4, 2);
		for (long[] tuple : tuples) {
			assertArrayEquals(tuple, input.next());
		}
		assertNull(input.next());
	}
}
