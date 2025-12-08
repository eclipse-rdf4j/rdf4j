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
package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.rdf4j.queryrender.sparql.ir.util.transform.PathTextUtils;
import org.junit.jupiter.api.Test;

public class PathTextUtilsTest {

	@Test
	void testIsWrappedAndTrim() {
		assertThat(PathTextUtils.isWrapped("(a)")).isTrue();
		assertThat(PathTextUtils.isWrapped("((a))")).isTrue();
		assertThat(PathTextUtils.isWrapped("a")).isFalse();

		assertThat(PathTextUtils.trimSingleOuterParens("(a)")).isEqualTo("a");
		assertThat(PathTextUtils.trimSingleOuterParens("((a))")).isEqualTo("(a)");
		assertThat(PathTextUtils.trimSingleOuterParens("a")).isEqualTo("a");
	}

	@Test
	void testSplitTopLevel() {
		List<String> parts = PathTextUtils.splitTopLevel("a|b|(c|d)", '|');
		assertThat(parts).containsExactly("a", "b", "(c|d)");

		List<String> seq = PathTextUtils.splitTopLevel("(a|b)/c", '/');
		assertThat(seq).containsExactly("(a|b)", "c");
	}

	@Test
	void testAtomicAndWrapping() {
		assertThat(PathTextUtils.isAtomicPathText("a|b")).isFalse();
		assertThat(PathTextUtils.isAtomicPathText("^(a|b)")).isTrue();
		assertThat(PathTextUtils.isAtomicPathText("!(a|b)"))
				.as("NPS is atomic")
				.isTrue();

		assertThat(PathTextUtils.wrapForSequence("a|b")).isEqualTo("(a|b)");
		assertThat(PathTextUtils.wrapForSequence("(a|b)")).isEqualTo("(a|b)");

		assertThat(PathTextUtils.wrapForInverse("a/b")).isEqualTo("^(a/b)");
		assertThat(PathTextUtils.wrapForInverse("a")).isEqualTo("^a");
	}

	@Test
	void testQuantifierWrapping() {
		assertThat(PathTextUtils.applyQuantifier("a|b", '?')).isEqualTo("(a|b)?");
		assertThat(PathTextUtils.applyQuantifier("a", '+')).isEqualTo("a+");
	}
}
