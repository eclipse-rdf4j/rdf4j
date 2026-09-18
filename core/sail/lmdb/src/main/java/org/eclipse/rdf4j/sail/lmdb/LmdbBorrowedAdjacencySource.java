/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. Eclipse Distribution License v1.0 (BSD-3-Clause).
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import org.eclipse.rdf4j.sail.lmdb.csf.ImmutablePagedQuadCsfIndex;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedFactorBatch;

/** Snapshot-owned physical factor access shared by interpreted and generated execution. */
final class LmdbBorrowedAdjacencySource extends BorrowedFactorBatch.Source {
	final LmdbAdjacencyReadView view;
	private final LmdbAdjacencyArenaCatalog[] catalogs;
	private final ContextCatalog contexts;

	LmdbBorrowedAdjacencySource(LmdbAdjacencyReadView view, LmdbAdjacencyArenaCatalog[] catalogs,
			ContextCatalog contexts) {
		super(view);
		view.retainLease();
		this.view = view;
		this.catalogs = catalogs;
		this.contexts = contexts;
	}

	@Override
	protected void release() {
		view.releaseLease();
	}

	@Override
	public BorrowedFactorBatch.Reader openReader() {
		checkOpen();
		return new BorrowedFactorBatch.Reader() {
			private final ImmutablePagedQuadCsfIndex.BorrowedPageReader page = new ImmutablePagedQuadCsfIndex.BorrowedPageReader();
			private LmdbAdjacencyRunCodec.RunCursor run;
			private long[] scratch;
			private byte kind;

			@Override
			public void bind(BorrowedFactorBatch batch, int lane) {
				checkOpen();
				if (batch.source() != LmdbBorrowedAdjacencySource.this)
					throw new IllegalArgumentException("incompatible borrowed factor source");
				kind = batch.kind(lane);
				if (kind == BorrowedFactorBatch.CSF_PAGE) {
					page.bind(batch.address(lane), batch.coordinate(lane));
				} else if (kind == BorrowedFactorBatch.ENCODED_RUN) {
					if (run == null)
						run = new LmdbAdjacencyRunCodec.RunCursor();
					long handle = batch.reference(lane);
					LmdbAdjacencyRunCodec.resolve(catalogs[LmdbDirectNativeAdjacency.sourceIndexOf(handle)],
							LmdbDirectNativeAdjacency.localOf(handle), run);
				} else
					throw new IllegalArgumentException("unsupported physical factor kind");
			}

			@Override
			public int copyFibers(long offset, int maximum, long[] values, long[] weights) {
				checkOpen();
				if (kind == BorrowedFactorBatch.CSF_PAGE)
					return page.copyFibers(offset, maximum, values, weights);
				if (scratch == null || scratch.length < maximum)
					scratch = new long[maximum];
				return run.copyBorrowedFibers(contexts, offset, maximum, values, weights, scratch);
			}
		};
	}
}
