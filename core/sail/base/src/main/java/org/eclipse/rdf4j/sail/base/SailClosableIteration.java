/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIterationWrapper;

class SailClosableIteration<T, X extends Exception>
		extends CloseableIterationWrapper<T, X, CloseableIteration<? extends T, X>> {

	public static <T, X extends Exception> SailClosableIteration<T, X> getInstance(
			CloseableIteration<? extends T, X> iter, SailClosable closes1) {
		return new SailClosableIteration<>(iter, closes1);
	}

	public static <T, X extends Exception> SailClosableIteration<T, X> getInstance(
			CloseableIteration<? extends T, X> iter, SailClosable closes1, SailClosable closes2) {
		return new SailClosableIteration<>(iter, closes1, closes2);
	}

	private final SailClosable closes1;
	private final SailClosable closes2;

	private SailClosableIteration(CloseableIteration<? extends T, X> iter, SailClosable closes1) {
		super(iter);
		this.closes1 = closes1;
		this.closes2 = null;
	}

	private SailClosableIteration(CloseableIteration<? extends T, X> iter, SailClosable closes1, SailClosable closes2) {
		super(iter);
		this.closes1 = closes1;
		this.closes2 = closes2;
	}

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			try {
				closes1.close();
			} finally {
				if (closes2 != null) {
					closes2.close();
				}
			}

		}
	}

}
