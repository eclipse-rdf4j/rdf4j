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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValueStoreWalValueKindTest {

	@Test
	void mapsKnownCodes() {
		assertThat(ValueStoreWalValueKind.fromCode("I")).isEqualTo(ValueStoreWalValueKind.IRI);
		assertThat(ValueStoreWalValueKind.fromCode("B")).isEqualTo(ValueStoreWalValueKind.BNODE);
		assertThat(ValueStoreWalValueKind.fromCode("L")).isEqualTo(ValueStoreWalValueKind.LITERAL);
		assertThat(ValueStoreWalValueKind.fromCode("N")).isEqualTo(ValueStoreWalValueKind.NAMESPACE);
	}

	@Test
	void rejectsUnknownOrEmptyCodes() {
		assertThatThrownBy(() -> ValueStoreWalValueKind.fromCode("?"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown value kind code");
		assertThatThrownBy(() -> ValueStoreWalValueKind.fromCode(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Missing value kind code");
		assertThatThrownBy(() -> ValueStoreWalValueKind.fromCode(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Missing value kind code");
	}
}
