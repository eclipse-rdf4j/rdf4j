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
package org.eclipse.rdf4j.common.iteration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SilentIterationTest {

	@Mock
	private CloseableIteration<Object, Exception> delegate;

	@InjectMocks
	private SilentIteration<Object, Exception> subject;

	@Test
	public void hasNextSwallowsException() throws Exception {
		when(delegate.hasNext()).thenThrow(new RuntimeException());
		assertThat(subject.hasNext()).isFalse();
	}

	@Test
	public void nextConvertsException() throws Exception {
		when(delegate.next()).thenThrow(new RuntimeException());
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(subject::next);
	}

}
