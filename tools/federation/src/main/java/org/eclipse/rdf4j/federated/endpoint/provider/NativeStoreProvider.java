/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint.provider;

import java.io.File;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.endpoint.ManagedRepositoryEndpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for an Endpoint that uses a RDF4J {@link NativeStore} as underlying repository. For optimization purposes
 * the {@link SailSourceEvaluationStrategyFactory} is used to allow for evaluation of prepared queries without prior
 * optimization. Note that NativeStores are always classified as 'Local'.
 *
 * <p>
 * If the repository location denotes an absolute path, the native store directory must already exist. If a relative
 * path is used, the repository is created on the fly (if necessary).
 * </p>
 *
 * @author Andreas Schwarte
 */
public class NativeStoreProvider implements EndpointProvider<NativeRepositoryInformation> {

	private static final Logger log = LoggerFactory.getLogger(NativeStoreProvider.class);

	private final File baseDir;

	public NativeStoreProvider(File baseDir) {
		super();
		this.baseDir = baseDir;
	}

	@Override
	public Endpoint loadEndpoint(NativeRepositoryInformation repoInfo) throws FedXException {

		File store = new File(repoInfo.getLocation());
		if (store.isAbsolute()) {
			// if the referenced location is absolute, we make sure that the store needs to
			// exists
			if (!store.isDirectory()) {
				throw new FedXRuntimeException(
						"Store does not exist at '" + repoInfo.getLocation() + ": " + store.getAbsolutePath() + "'.");
			}

			log.debug("Loading Native store from " + store.getAbsolutePath());
		} else {

			if (baseDir == null) {
				throw new FedXException(
						"Base directory not defined. Use FedXFactory for base directory initialization.");
			}

			store = new File(baseDir, "repositories/" + repoInfo.getLocation());
			if (store.isDirectory()) {
				log.debug("Loading existing native store from " + store.getAbsolutePath());
			} else {
				log.info("Creating and loading native store from " + store.getAbsolutePath());
				store.mkdirs();
			}
		}

		try {
			NativeStore ns = createNativeStore(store);
			SailRepository repo = new SailRepository(ns);

			try {
				repo.init();
			} finally {
				repo.shutDown();
			}

			ManagedRepositoryEndpoint res = new ManagedRepositoryEndpoint(repoInfo, repoInfo.getLocation(),
					EndpointClassification.Local, repo);
			res.setEndpointConfiguration(repoInfo.getEndpointConfiguration());

			return res;
		} catch (RepositoryException e) {
			throw new FedXException("Repository " + repoInfo.getId() + " could not be initialized: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Create a {@link NativeStore} and apply the {@link SailSourceEvaluationStrategyFactory}.
	 *
	 * @param store
	 * @return the store
	 */
	protected NativeStore createNativeStore(File store) {
		NativeStore ns = new NativeStore(store);
		EvaluationStrategyFactory factory = new SailSourceEvaluationStrategyFactory(
				ns.getEvaluationStrategyFactory());
		ns.setEvaluationStrategyFactory(factory);
		return ns;
	}

}
