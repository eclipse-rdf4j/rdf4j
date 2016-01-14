/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.REPOSITORYID;
import static org.eclipse.rdf4j.repository.config.RepositoryConfigSchema.REPOSITORY_CONTEXT;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.http.client.SesameClientDependent;
import org.eclipse.rdf4j.http.client.SesameClientImpl;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.eclipse.rdf4j.repository.DelegatingRepository;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.config.DelegatingRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.event.base.RepositoryConnectionListenerAdapter;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolverClient;

/**
 * An implementation of the {@link RepositoryManager} interface that operates
 * directly on the repository data files in the local file system.
 * 
 * @author Arjohn Kampman
 */
public class LocalRepositoryManager extends RepositoryManager {

	/*-----------*
	 * Constants *
	 *-----------*/

	public static final String REPOSITORIES_DIR = "repositories";

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The base dir to resolve any relative paths against.
	 */
	private final File baseDir;

	/** dependent life cycle */
	private SesameClientImpl client;

	/** dependent life cycle */
	private FederatedServiceResolverImpl serviceResolver;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RepositoryManager that operates on the specfified base
	 * directory.
	 * 
	 * @param baseDir
	 *        The base directory where data for repositories can be stored, among
	 *        other things.
	 */
	public LocalRepositoryManager(File baseDir) {
		super();

		this.baseDir = baseDir;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected SystemRepository createSystemRepository()
		throws RepositoryException
	{
		File systemDir = getRepositoryDir(SystemRepository.ID);
		SystemRepository systemRepos = new SystemRepository(systemDir);
		systemRepos.initialize();

		systemRepos.addRepositoryConnectionListener(new ConfigChangeListener());
		return systemRepos;
	}

	/**
	 * Gets the base dir against which to resolve relative paths.
	 */
	public File getBaseDir() {
		return baseDir;
	}

	/**
	 * Gets the base dir against which to resolve relative paths.
	 * 
	 * @throws MalformedURLException
	 *         If the path cannot be parsed as a URL
	 */
	public URL getLocation()
		throws MalformedURLException
	{
		return baseDir.toURI().toURL();
	}

	@Override
	public HttpClient getHttpClient() {
		if (client == null) {
			return null;
		}
		else {
			return client.getHttpClient();
		}
	}

	@Override
	public synchronized void setHttpClient(HttpClient httpClient) {
		if (client == null) {
			client = new SesameClientImpl();
		}
		client.setHttpClient(httpClient);
	}

	/**
	 * @return Returns the serviceResolver.
	 */
	protected synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (client == null) {
				client = new SesameClientImpl();
			}
			serviceResolver = new FederatedServiceResolverImpl();
			serviceResolver.setSesameClient(client);
		}
		return serviceResolver;
	}

	@Override
	public void shutDown() {
		super.shutDown();
		if (serviceResolver != null) {
			serviceResolver.shutDown();
			serviceResolver = null;
		}
		if (client != null) {
			client.shutDown();
			client = null;
		}
	}

	/**
	 * Resolves the specified path against the manager's base directory.
	 * 
	 * @see #getBaseDir
	 */
	public File resolvePath(String path) {
		return new File(getBaseDir(), path);
	}

	public File getRepositoryDir(String repositoryID) {
		File repositoriesDir = resolvePath(REPOSITORIES_DIR);
		return new File(repositoriesDir, repositoryID);
	}

	@Override
	public SystemRepository getSystemRepository() {
		return (SystemRepository)super.getSystemRepository();
	}

	@Override
	protected Repository createRepository(String id)
		throws RepositoryConfigException, RepositoryException
	{
		Repository systemRepository = getSystemRepository();

		RepositoryConnection con = systemRepository.getConnection();
		try {
			Repository repository = null;

			RepositoryConfig repConfig = RepositoryConfigUtil.getRepositoryConfig(systemRepository, id);
			if (repConfig != null) {
				repConfig.validate();

				repository = createRepositoryStack(repConfig.getRepositoryImplConfig());
				repository.setDataDir(getRepositoryDir(id));
				repository.initialize();
			}

			return repository;
		}
		finally {
			con.close();
		}
	}

	/**
	 * Creates the stack of Repository objects for the repository represented by
	 * the specified {@link org.eclipse.rdf4j.repository.config.RepositoryImplConfig}.
	 * Uses a {@link org.eclipse.rdf4j.repository.config.RepositoryFactory} to create
	 * the repository and initialize it.
	 * 
	 * @param config
	 *        The node representing the to-be-created repository in the
	 *        configuration.
	 * @return The created repository, or <tt>null</tt> if no such repository
	 *         exists.
	 * @throws RepositoryConfigException
	 *         If no repository could be created due to invalid or incomplete
	 *         configuration data.
	 */
	private Repository createRepositoryStack(RepositoryImplConfig config)
		throws RepositoryConfigException
	{
		RepositoryFactory factory = RepositoryRegistry.getInstance().get(config.getType()).orElseThrow(
				() -> new RepositoryConfigException("Unsupported repository type: " + config.getType()));
		Repository repository = factory.getRepository(config);
		if (repository instanceof RepositoryResolverClient) {
			((RepositoryResolverClient)repository).setRepositoryResolver(this);
		}
		if (repository instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient)repository).setFederatedServiceResolver(getFederatedServiceResolver());
		}
		if (repository instanceof SesameClientDependent) {
			((SesameClientDependent)repository).setSesameClient(client);
		}
		else if (repository instanceof HttpClientDependent) {
			((HttpClientDependent)repository).setHttpClient(getHttpClient());
		}
		if (config instanceof DelegatingRepositoryImplConfig) {
			RepositoryImplConfig delegateConfig = ((DelegatingRepositoryImplConfig)config).getDelegate();
			Repository delegate = createRepositoryStack(delegateConfig);
			try {
				((DelegatingRepository)repository).setDelegate(delegate);
			}
			catch (ClassCastException e) {
				throw new RepositoryConfigException(
						"Delegate specified for repository that is not a DelegatingRepository: "
								+ delegate.getClass(), e);
			}
		}
		return repository;
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String id)
		throws RepositoryException
	{
		try {
			RepositoryConfig config = null;
			if (id.equals(SystemRepository.ID)) {
				config = new RepositoryConfig(id, new SystemRepositoryConfig());
			}
			else {
				config = getRepositoryConfig(id);
			}

			RepositoryInfo repInfo = new RepositoryInfo();
			repInfo.setId(id);
			repInfo.setDescription(config.getTitle());
			try {
				repInfo.setLocation(getRepositoryDir(id).toURI().toURL());
			}
			catch (MalformedURLException mue) {
				throw new RepositoryException("Location of repository does not resolve to a valid URL", mue);
			}

			repInfo.setReadable(true);
			repInfo.setWritable(true);

			return repInfo;
		}
		catch (RepositoryConfigException e) {
			// FIXME: don't fetch info through config parsing
			throw new RepositoryException("Unable to read repository configuration", e);
		}
	}

	@Override
	public List<RepositoryInfo> getAllRepositoryInfos(boolean skipSystemRepo)
		throws RepositoryException
	{
		List<RepositoryInfo> result = new ArrayList<RepositoryInfo>();

		for (String id : getRepositoryIDs()) {
			if (!skipSystemRepo || !id.equals(SystemRepository.ID)) {
				result.add(getRepositoryInfo(id));
			}
		}

		return result;
	}

	class ConfigChangeListener extends RepositoryConnectionListenerAdapter {

		private final Map<RepositoryConnection, Set<Resource>> modifiedContextsByConnection = new HashMap<RepositoryConnection, Set<Resource>>();

		private final Map<RepositoryConnection, Boolean> modifiedAllContextsByConnection = new HashMap<RepositoryConnection, Boolean>();

		private final Map<RepositoryConnection, Set<Resource>> removedContextsByConnection = new HashMap<RepositoryConnection, Set<Resource>>();

		private Set<Resource> getModifiedContexts(RepositoryConnection conn) {
			Set<Resource> result = modifiedContextsByConnection.get(conn);
			if (result == null) {
				result = new HashSet<Resource>();
				modifiedContextsByConnection.put(conn, result);
			}
			return result;
		}

		private Set<Resource> getRemovedContexts(RepositoryConnection conn) {
			Set<Resource> result = removedContextsByConnection.get(conn);
			if (result == null) {
				result = new HashSet<Resource>();
				removedContextsByConnection.put(conn, result);
			}
			return result;
		}

		private void registerModifiedContexts(RepositoryConnection conn, Resource... contexts) {
			Set<Resource> modifiedContexts = getModifiedContexts(conn);
			// wildcard used for context
			if (contexts == null) {
				modifiedAllContextsByConnection.put(conn, true);
			}
			else {
				for (Resource context : contexts) {
					modifiedContexts.add(context);
				}
			}
		}

		@Override
		public void add(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
				Resource... contexts)
		{
			registerModifiedContexts(conn, contexts);
		}

		@Override
		public void clear(RepositoryConnection conn, Resource... contexts) {
			registerModifiedContexts(conn, contexts);
		}

		@Override
		public void remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object,
				Resource... contexts)
		{
			if (object != null && object.equals(RepositoryConfigSchema.REPOSITORY_CONTEXT)) {
				if (subject == null) {
					modifiedAllContextsByConnection.put(conn, true);
				}
				else {
					Set<Resource> removedContexts = getRemovedContexts(conn);
					removedContexts.add(subject);
				}
			}
			registerModifiedContexts(conn, contexts);
		}

		@Override
		public void rollback(RepositoryConnection conn) {
			modifiedContextsByConnection.remove(conn);
			modifiedAllContextsByConnection.remove(conn);
		}

		@Override
		public void commit(RepositoryConnection con) {
			// refresh all contexts when a wildcard was used
			// REMIND: this could still be improved if we knew whether or not a
			// *repositoryconfig* context was actually modified
			Boolean fullRefreshNeeded = modifiedAllContextsByConnection.remove(con);
			if (fullRefreshNeeded != null && fullRefreshNeeded.booleanValue()) {
				logger.debug("Reacting to commit on SystemRepository for all contexts");
				refresh();
			}
			// refresh only modified contexts that actually contain repository
			// configurations
			else {
				Set<Resource> modifiedContexts = modifiedContextsByConnection.remove(con);
				Set<Resource> removedContexts = removedContextsByConnection.remove(con);
				if (removedContexts != null && !removedContexts.isEmpty()) {
					modifiedContexts.removeAll(removedContexts);
				}
				if (modifiedContexts != null) {
					logger.debug("React to commit on SystemRepository for contexts {}", modifiedContexts);
					try {
						RepositoryConnection cleanupCon = getSystemRepository().getConnection();

						try {
							// refresh all modified contexts
							for (Resource context : modifiedContexts) {
								logger.debug("Processing modified context {}.", context);
								try {
									if (isRepositoryConfigContext(cleanupCon, context)) {
										String repositoryID = getRepositoryID(cleanupCon, context);
										logger.debug("Reacting to modified repository config for {}", repositoryID);
										Repository repository = removeInitializedRepository(repositoryID);
										if (repository != null) {
											logger.debug("Modified repository {} has been initialized, refreshing...",
													repositoryID);
											// refresh single repository
											refreshRepository(cleanupCon, repositoryID, repository);
										}
										else {
											logger.debug("Modified repository {} has not been initialized, skipping...",
													repositoryID);
										}
									}
									else {
										logger.debug("Context {} doesn't contain repository config information.",
												context);
									}
								}
								catch (RepositoryException re) {
									logger.error("Failed to process repository configuration changes", re);
								}
							}
						}
						finally {
							cleanupCon.close();
						}
					}
					catch (RepositoryException re) {
						logger.error("Failed to process repository configuration changes", re);
					}
				}
			}
		}

		private boolean isRepositoryConfigContext(RepositoryConnection con, Resource context)
			throws RepositoryException
		{
			logger.debug("Is {} a repository config context?", context);
			return con.hasStatement(context, RDF.TYPE, REPOSITORY_CONTEXT, true, (Resource)null);
		}

		private String getRepositoryID(RepositoryConnection con, Resource context)
			throws RepositoryException
		{
			String result = null;

			RepositoryResult<Statement> idStatements = con.getStatements(null, REPOSITORYID, null, true, context);
			try {
				if (idStatements.hasNext()) {
					Statement idStatement = idStatements.next();
					result = idStatement.getObject().stringValue();
				}
			}
			finally {
				idStatements.close();
			}

			return result;
		}
	}

	@Override
	protected void cleanUpRepository(String repositoryID)
		throws IOException
	{
		File dataDir = getRepositoryDir(repositoryID);

		if (dataDir.isDirectory()) {
			logger.debug("Cleaning up data dir {} for repository {}", dataDir.getAbsolutePath(), repositoryID);
			FileUtil.deleteDir(dataDir);
		}
	}
}
