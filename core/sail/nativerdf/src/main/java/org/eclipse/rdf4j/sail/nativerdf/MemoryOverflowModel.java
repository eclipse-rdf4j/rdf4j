/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.AbstractMemoryOverflowModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model implementation that stores in a {@link LinkedHashModel} until more than 10KB statements are added and the
 * estimated memory usage is more than the amount of free memory available. Once the threshold is cross this
 * implementation seamlessly changes to a disk based {@link SailSourceModel}.
 *
 * @author James Leigh
 */
abstract class MemoryOverflowModel extends AbstractMemoryOverflowModel<SailSourceModel> {

	private static final long serialVersionUID = 4119844228099208169L;

	final Logger logger = LoggerFactory.getLogger(MemoryOverflowModel.class);

	private volatile LinkedHashModel memory;

	private transient File dataDir;

	private transient SailStore store;

	protected abstract SailStore createSailStore(File dataDir) throws IOException, SailException;

	@Override
	protected void overflowToDiskInner(Model memory) {
		try {
			assert disk == null;
			dataDir = Files.createTempDirectory("model").toFile();
			logger.debug("memory overflow using temp directory {}", dataDir);
			store = createSailStore(dataDir);
			this.disk = new SailSourceModel(store, memory);
			logger.debug("overflow synced to disk");
		} catch (IOException | SailException e) {
			String path = dataDir != null ? dataDir.getAbsolutePath() : "(unknown)";
			logger.error("Error while writing to overflow directory " + path, e);
		}
	}
}
