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

package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.AbstractMemoryOverflowModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model implementation that stores in a {@link LinkedHashModel} until more than 10KB statements are added and the
 * estimated memory usage is more than the amount of free memory available. Once the threshold is cross this
 * implementation seamlessly changes to a disk based {@link SailSourceModel}.
 */
abstract class MemoryOverflowModel extends AbstractMemoryOverflowModel<SailSourceModel> {

	final Logger logger = LoggerFactory.getLogger(MemoryOverflowModel.class);
	private final boolean verifyAdditions;

	private transient File dataDir;

	private transient LmdbSailStore store;

	public MemoryOverflowModel(boolean verifyAdditions) {
		super();
		this.verifyAdditions = verifyAdditions;
	}

	protected abstract LmdbSailStore createSailStore(File dataDir) throws IOException, SailException;

	@Override
	protected void overflowToDiskInner(Model memory) {
		try {
			assert disk == null;
			dataDir = Files.createTempDirectory("model").toFile();
			logger.debug("memory overflow using temp directory {}", dataDir);
			store = createSailStore(dataDir);
			disk = new SailSourceModel(store, memory) {

				@Override
				protected void finalize() throws Throwable {
					logger.debug("finalizing {}", dataDir);
					if (disk == this) {
						try {
							store.close();
						} catch (SailException e) {
							logger.error(e.toString(), e);
						} finally {
							FileUtil.deleteDir(dataDir);
							dataDir = null;
							store = null;
							disk = null;
						}
					}
					super.finalize();
				}
			};
		} catch (IOException | SailException e) {
			String path = dataDir != null ? dataDir.getAbsolutePath() : "(unknown)";
			logger.error("Error while writing to overflow directory " + path, e);
		}
	}

}
