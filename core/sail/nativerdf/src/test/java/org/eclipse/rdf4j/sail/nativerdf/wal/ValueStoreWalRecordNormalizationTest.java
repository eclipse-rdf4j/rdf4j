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
package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValueStoreWalRecordNormalizationTest {

	@Test
	void nullStringsAreNormalizedToEmpty() {
		ValueStoreWalRecord r = new ValueStoreWalRecord(1L, 123, ValueStoreWalValueKind.IRI, null, null, null, 0);
		assertThat(r.lexical()).isEqualTo("");
		assertThat(r.datatype()).isEqualTo("");
		assertThat(r.language()).isEqualTo("");
	}
}
