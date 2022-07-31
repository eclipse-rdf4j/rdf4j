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
package org.eclipse.rdf4j.federated.endpoint;

import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.endpoint.provider.NativeRepositoryInformation;
import org.eclipse.rdf4j.federated.endpoint.provider.NativeStoreProvider;
import org.eclipse.rdf4j.federated.endpoint.provider.RemoteRepositoryProvider;
import org.eclipse.rdf4j.federated.endpoint.provider.RemoteRepositoryRepositoryInformation;
import org.eclipse.rdf4j.federated.endpoint.provider.RepositoryEndpointProvider;
import org.eclipse.rdf4j.federated.endpoint.provider.RepositoryInformation;
import org.eclipse.rdf4j.federated.endpoint.provider.ResolvableRepositoryInformation;
import org.eclipse.rdf4j.federated.endpoint.provider.ResolvableRepositoryProvider;
import org.eclipse.rdf4j.federated.endpoint.provider.SPARQLProvider;
import org.eclipse.rdf4j.federated.endpoint.provider.SPARQLRepositoryInformation;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.Vocabulary;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing various methods to create Endpoints to be used as federation members.
 *
 * @author Andreas Schwarte
 *
 */
public class EndpointFactory {

	private static final Logger logger = LoggerFactory.getLogger(EndpointFactory.class);

	/**
	 * Construct a SPARQL endpoint using the the provided information.
	 *
	 * @param name     a descriptive name, e.g. http://dbpedia
	 * @param endpoint the URL of the SPARQL endpoint, e.g. http://dbpedia.org/sparql
	 *
	 * @return an initialized {@link EndpointBase} containing the repository
	 *
	 * @throws FedXException
	 */
	public static Endpoint loadSPARQLEndpoint(String name, String endpoint) throws FedXException {

		SPARQLProvider repProvider = new SPARQLProvider();
		return repProvider.loadEndpoint(new SPARQLRepositoryInformation(name, endpoint));
	}

	/**
	 * Construct a SPARQL endpoint using the the provided information and the host of the url as name.
	 *
	 * @param endpoint the URL of the SPARQL endpoint, e.g. http://dbpedia.org/sparql
	 *
	 * @return an initialized {@link EndpointBase} containing the repository
	 *
	 * @throws FedXException
	 */
	public static Endpoint loadSPARQLEndpoint(String endpoint) throws FedXException {
		try {
			String id = new URL(endpoint).getHost();
			if (id.equals("localhost")) {
				id = id + "_" + new URL(endpoint).getPort();
			}
			return loadSPARQLEndpoint("http://" + id, endpoint);
		} catch (MalformedURLException e) {
			throw new FedXException("Malformed URL: " + endpoint);
		}
	}

	public static Endpoint loadRemoteRepository(String repositoryServer, String repositoryName) throws FedXException {
		return loadRemoteRepository(repositoryServer, repositoryName, false);
	}

	public static Endpoint loadRemoteRepository(String repositoryServer, String repositoryName, boolean writable)
			throws FedXException {
		RemoteRepositoryProvider repProvider = new RemoteRepositoryProvider();
		RemoteRepositoryRepositoryInformation info = new RemoteRepositoryRepositoryInformation(repositoryServer,
				repositoryName);
		info.setWritable(writable);
		return repProvider.loadEndpoint(info);

	}

	/**
	 * Load a {@link ResolvableEndpoint}
	 *
	 * <p>
	 * The federation must be initialized with a {@link RepositoryResolver} ( see
	 * {@link FedXFactory#withRepositoryResolver(RepositoryResolver)}) and this resolver must offer a Repository with
	 * the id provided by {@link Endpoint#getId()}
	 * </p>
	 *
	 * <p>
	 * Note that the name is set to "http://" + repositoryId
	 * </p>
	 *
	 * @param repositoryId the repository identifier
	 * @return the configured {@link Endpoint}
	 * @see ResolvableRepositoryProvider
	 * @see ResolvableRepositoryInformation
	 */
	public static Endpoint loadResolvableRepository(String repositoryId) {
		return loadResolvableRepository(repositoryId, false);
	}

	/**
	 * Load a {@link ResolvableEndpoint}
	 *
	 * <p>
	 * The federation must be initialized with a {@link RepositoryResolver} ( see
	 * {@link FedXFactory#withRepositoryResolver(RepositoryResolver)}) and this resolver must offer a Repository with
	 * the id provided by {@link Endpoint#getId()}
	 * </p>
	 *
	 * <p>
	 * Note that the name is set to "http://" + repositoryId
	 * </p>
	 *
	 * @param repositoryId the repository identifier
	 * @param writable     whether to configure the endpoint as writable.
	 * @return the configured {@link Endpoint}
	 * @see ResolvableRepositoryProvider
	 * @see ResolvableRepositoryInformation
	 */
	public static Endpoint loadResolvableRepository(String repositoryId, boolean writable) {
		ResolvableRepositoryProvider repProvider = new ResolvableRepositoryProvider();
		ResolvableRepositoryInformation info = new ResolvableRepositoryInformation(repositoryId);
		info.setWritable(writable);
		return repProvider.loadEndpoint(info);
	}

	/**
	 * Load an {@link Endpoint} for a given (configured) Repository.
	 * <p>
	 * Note that {@link EndpointType} is set to {@link EndpointType#Other}
	 * </p>
	 *
	 * <p>
	 * If the repository is already initialized, it is assumed that the lifecycle is managed externally. Otherwise, FedX
	 * will make sure to take care for the lifecycle of the repository, i.e. initialize and shutdown.
	 * </p>
	 *
	 * @param id         the identifier, e.g. "myRepository"
	 * @param repository the constructed repository
	 * @return the initialized endpoint
	 * @throws FedXException
	 */
	public static Endpoint loadEndpoint(String id, Repository repository)
			throws FedXException {
		RepositoryEndpointProvider repProvider = new RepositoryEndpointProvider(repository);
		String name = "http://" + id;
		String location = "http://unknown";
		try {
			location = repository.getDataDir().getAbsolutePath();
		} catch (Exception e) {
			logger.debug("Failed to use data dir as location, using unknown instead: " + e.getMessage());
			logger.trace("Details:", e);
		}
		return repProvider.loadEndpoint(new RepositoryInformation(id, name, location, EndpointType.Other));
	}

	/**
	 * Construct a NativeStore endpoint using the provided information.
	 *
	 * <p>
	 * If the repository location denotes an absolute path, the native store directory must already exist. If a relative
	 * path is used, the repository is created on the fly (if necessary).
	 * </p>
	 *
	 * @param name     a descriptive name, e.g. http://dbpedia
	 * @param location the location of the data store, either absolute or relative in a "repositories" subfolder
	 *                 {@link FedXRepository#getDataDir()}
	 *
	 * @return an initialized endpoint containing the repository
	 *
	 * @throws FedXException
	 */
	public static Endpoint loadNativeEndpoint(String name, File location) throws FedXException {

		File baseDir = null; // not required
		NativeStoreProvider repProvider = new NativeStoreProvider(baseDir);
		return repProvider.loadEndpoint(new NativeRepositoryInformation(name, location.getAbsolutePath()));
	}

	/**
	 * Construct a {@link NativeStore} endpoint using the provided information and the file location as name.
	 *
	 * <p>
	 * If the repository location denotes an absolute path, the native store directory must already exist. If a relative
	 * path is used, the repository is created on the fly (if necessary).
	 * </p>
	 *
	 * @param location the location of the data store
	 *
	 * @return an initialized endpoint containing the repository
	 *
	 * @throws FedXException
	 */
	public static Endpoint loadNativeEndpoint(File location) throws FedXException {
		return loadNativeEndpoint("http://" + location.getName(), location);
	}

	/**
	 * Utility function to load federation members from a data configuration file.
	 *
	 * <p>
	 * A data configuration file provides information about federation members in form of turtle. Currently the types
	 * NativeStore, ResolvableEndpoint and SPARQLEndpoint are supported. For details please refer to the documentation
	 * in {@link NativeRepositoryInformation}, {@link ResolvableRepositoryInformation} and
	 * {@link SPARQLRepositoryInformation}.
	 * </p>
	 *
	 * @param dataConfig
	 *
	 * @return a list of initialized endpoints, i.e. the federation members
	 *
	 * @throws FedXException
	 */
	public static List<Endpoint> loadFederationMembers(File dataConfig, File fedXBaseDir) throws FedXException {

		if (!dataConfig.exists()) {
			throw new FedXRuntimeException("File does not exist: " + dataConfig.getAbsolutePath());
		}

		Model graph = new TreeModel();
		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		RDFHandler handler = new DefaultRDFHandler(graph);
		parser.setRDFHandler(handler);
		try (FileReader fr = new FileReader(dataConfig)) {
			parser.parse(fr, Vocabulary.FEDX.NAMESPACE);
		} catch (Exception e) {
			throw new FedXException("Unable to parse dataconfig " + dataConfig + ":" + e.getMessage());
		}

		return loadFederationMembers(graph, fedXBaseDir);
	}

	/**
	 * Utility function to load federation members from a model.
	 * <p>
	 * Currently the types NativeStore, ResolvableEndpoint and SPARQLEndpoint are supported. For details please refer to
	 * the documentation in {@link NativeRepositoryInformation}, {@link ResolvableRepositoryInformation} and
	 * {@link SPARQLRepositoryInformation}.
	 * </p>
	 *
	 * @param members
	 * @param baseDir
	 * @return list of endpoints
	 * @throws FedXException
	 */
	public static List<Endpoint> loadFederationMembers(Model members, File baseDir) throws FedXException {

		List<Endpoint> res = new ArrayList<>();
		for (Statement st : members.getStatements(null, Vocabulary.FEDX.STORE, null)) {
			Endpoint e = loadEndpoint(members, st.getSubject(), st.getObject(), baseDir);
			res.add(e);
		}

		return res;
	}

	private static Endpoint loadEndpoint(Model graph, Resource repNode, Value repType, File baseDir)
			throws FedXException {

		// NativeStore => RDF4J native store implementation
		if (repType.equals(FedXUtil.literal("NativeStore"))) {
			NativeStoreProvider repProvider = new NativeStoreProvider(baseDir);
			return repProvider.loadEndpoint(new NativeRepositoryInformation(graph, repNode));
		}

		// SPARQL Repository => SPARQLRepository
		else if (repType.equals(FedXUtil.literal("SPARQLEndpoint"))) {
			SPARQLProvider repProvider = new SPARQLProvider();
			return repProvider.loadEndpoint(new SPARQLRepositoryInformation(graph, repNode));
		}

		// Remote Repository
		else if (repType.equals(FedXUtil.literal("RemoteRepository"))) {
			RemoteRepositoryProvider repProvider = new RemoteRepositoryProvider();
			return repProvider.loadEndpoint(new RemoteRepositoryRepositoryInformation(graph, repNode));
		}

		// Resolvable Repository
		else if (repType.equals(FedXUtil.literal("ResolvableRepository"))) {
			ResolvableRepositoryProvider repProvider = new ResolvableRepositoryProvider();
			return repProvider.loadEndpoint(new ResolvableRepositoryInformation(graph, repNode));
		}

		// other generic type
		else if (repType.equals(FedXUtil.literal("Other"))) {

			// TODO add reflection techniques to allow for flexibility
			throw new UnsupportedOperationException("Operation not yet supported for generic type.");

		} else {
			throw new FedXRuntimeException("Repository type not supported: " + repType.stringValue());
		}

	}

	/**
	 * Construct a unique id for the provided SPARQL Endpoint, e.g
	 *
	 * http://dbpedia.org/ => %type%_dbpedia.org
	 *
	 * @param endpointID
	 * @param type       the repository type, e.g. native, sparql, etc
	 *
	 * @return the ID for the SPARQL endpoint
	 */
	public static String getId(String endpointID, String type) {
		String id = endpointID.replace("http://", "");
		id = id.replace("/", "_");
		return type + "_" + id;
	}

	protected static class DefaultRDFHandler implements RDFHandler {

		protected final Model graph;

		public DefaultRDFHandler(Model graph) {
			super();
			this.graph = graph;
		}

		@Override
		public void endRDF() throws RDFHandlerException {
			// no-op
		}

		@Override
		public void handleComment(String comment) throws RDFHandlerException {
			// no-op
		}

		@Override
		public void handleNamespace(String prefix, String uri)
				throws RDFHandlerException {
			// no-op
		}

		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			graph.add(st);
		}

		@Override
		public void startRDF() throws RDFHandlerException {
			// no-op
		}
	}
}
