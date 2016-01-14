/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositorySchema;
import org.eclipse.rdf4j.repository.sail.config.RepositoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A manager for {@link Repository}s. Every <tt>RepositoryManager</tt> has one
 * SYSTEM repository and zero or more "user repositories". The SYSTEM repository
 * contains data that describes the configuration of the other repositories
 * (their IDs, which implementations of the Repository API to use, access
 * rights, etc.). The other repositories are instantiated based on this
 * configuration data.
 * 
 * @author Arjohn Kampman
 * @see RepositoryProvider
 */
public abstract class RepositoryManager implements RepositoryResolver, HttpClientDependent {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Map<String, Repository> initializedRepositories;

	private HttpClient httpClient;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RepositoryManager.
	 */
	public RepositoryManager() {
		this(new HashMap<String, Repository>());
	}

	/**
	 * Create a new RepositoryManager using the given map to store repository
	 * information.
	 * 
	 * @param initializedRepositories
	 *        A map that will be used to store repository information.
	 */
	public RepositoryManager(Map<String, Repository> initializedRepositories) {
		setInitializedRepositories(initializedRepositories);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Indicates if this RepositoryManager has been initialized. Note that the
	 * initialization status may change if the Repository is shut down.
	 * 
	 * @return true iff the repository manager has been initialized.
	 * @since 2.7.0
	 */
	public boolean isInitialized() {
		synchronized (initializedRepositories) {
			Repository repo = initializedRepositories.get(SystemRepository.ID);
			return repo != null && repo.isInitialized();
		}
	}

	/**
	 * @return Returns the httpClient passed to {@link Repository} construction.
	 */
	public abstract HttpClient getHttpClient();

	/**
	 * Should be called before {@link #initialize()}.
	 * 
	 * @param httpClient The httpClient to use for remote/service calls.
	 */
	public abstract void setHttpClient(HttpClient httpClient);

	/**
	 * Initializes the repository manager.
	 * 
	 * @throws RepositoryException
	 *         If the manager failed to initialize the SYSTEM repository.
	 */
	public void initialize()
		throws RepositoryException
	{
		Repository systemRepository = createSystemRepository();

		synchronized (initializedRepositories) {
			initializedRepositories.put(SystemRepository.ID, systemRepository);
		}
	}

	protected abstract Repository createSystemRepository()
		throws RepositoryException;

	/**
	 * Gets the SYSTEM repository.
	 */
	public Repository getSystemRepository() {
		if (!isInitialized())
			throw new IllegalStateException("Repository Manager is not initialized");
		synchronized (initializedRepositories) {
			return initializedRepositories.get(SystemRepository.ID);
		}
	}

	/**
	 * Generates an ID for a new repository based on the specified base name. The
	 * base name may for example be a repository name entered by the user. The
	 * generated ID will contain a variant of this name that does not occur as a
	 * repository ID in this manager yet and is suitable for use as a file name
	 * (e.g. for the repository's data directory).
	 * 
	 * @param baseName
	 *        The String on which the returned ID should be based, must not be
	 *        <tt>null</tt>.
	 * @return A new repository ID derived from the specified base name.
	 * @throws RepositoryException
	 * @throws RepositoryConfigException
	 */
	public String getNewRepositoryID(String baseName)
		throws RepositoryException, RepositoryConfigException
	{
		if (baseName != null) {
			// Filter exotic characters from the base name
			baseName = baseName.trim();

			int length = baseName.length();
			StringBuilder buffer = new StringBuilder(length);

			for (char c : baseName.toCharArray()) {
				if (Character.isLetter(c) || Character.isDigit(c) || c == '-' || c == '_' || c == '.') {
					// Convert to lower case since file names are case insensitive on
					// some/most platforms
					buffer.append(Character.toLowerCase(c));
				}
				else if (c != '"' && c != '\'') {
					buffer.append('-');
				}
			}

			baseName = buffer.toString();
		}

		// First try if we can use the base name without an appended index
		if (baseName != null && baseName.length() > 0 && !hasRepositoryConfig(baseName)) {
			return baseName;
		}

		// When the base name is null or empty, generate one
		if (baseName == null || baseName.length() == 0) {
			baseName = "repository-";
		}
		else if (!baseName.endsWith("-")) {
			baseName += "-";
		}

		// Keep appending numbers until we find an unused ID
		int index = 2;
		while (hasRepositoryConfig(baseName + index)) {
			index++;
		}

		return baseName + index;
	}

	public Set<String> getRepositoryIDs()
		throws RepositoryException
	{
		return RepositoryConfigUtil.getRepositoryIDs(getSystemRepository());
	}

	public boolean hasRepositoryConfig(String repositoryID)
		throws RepositoryException, RepositoryConfigException
	{
		return RepositoryConfigUtil.hasRepositoryConfig(getSystemRepository(), repositoryID);
	}

	public RepositoryConfig getRepositoryConfig(String repositoryID)
		throws RepositoryConfigException, RepositoryException
	{
		return RepositoryConfigUtil.getRepositoryConfig(getSystemRepository(), repositoryID);
	}

	/**
	 * Adds or updates the configuration of a repository to the manager's system
	 * repository. The system repository may already contain a configuration for
	 * a repository with the same ID as specified by <tt>config</tt>, in which
	 * case all previous configuration data for that repository will be cleared
	 * before the new configuration is added.
	 * 
	 * @param config
	 *        The repository configuration that should be added to or updated in
	 *        the system repository.
	 * @throws RepositoryException
	 *         If the manager failed to update it's system repository.
	 * @throws RepositoryConfigException
	 *         If the manager doesn't know how to update a configuration due to
	 *         inconsistent configuration data in the system repository. For
	 *         example, this happens when there are multiple existing
	 *         configurations with the concerning ID.
	 */
	public void addRepositoryConfig(RepositoryConfig config)
		throws RepositoryException, RepositoryConfigException
	{
		RepositoryConfigUtil.updateRepositoryConfigs(getSystemRepository(), config);
	}

	/**
	 * Removes the configuration for the specified repository from the manager's
	 * system repository if such a configuration is present. Returns
	 * <tt>true</tt> if the system repository actually contained the specified
	 * repository configuration.
	 * 
	 * @param repositoryID
	 *        The ID of the repository whose configuration needs to be removed.
	 * @throws RepositoryException
	 *         If the manager failed to update it's system repository.
	 * @throws RepositoryConfigException
	 *         If the manager doesn't know how to remove a configuration due to
	 *         inconsistent configuration data in the system repository. For
	 *         example, this happens when there are multiple existing
	 *         configurations with the concerning ID.
	 * @deprecated since 2.6.7. Use {@link #removeRepository(String repositoryID)}
	 *             instead.
	 */
	@Deprecated
	public boolean removeRepositoryConfig(String repositoryID)
		throws RepositoryException, RepositoryConfigException
	{
		logger.debug("Removing repository configuration for {}.", repositoryID);
		boolean isRemoved = false;

		synchronized (initializedRepositories) {
			isRemoved = RepositoryConfigUtil.removeRepositoryConfigs(getSystemRepository(), repositoryID);

			if (isRemoved) {
				logger.debug("Shutdown repository {} after removal of configuration.", repositoryID);
				Repository repository = initializedRepositories.remove(repositoryID);

				if (repository != null && repository.isInitialized()) {
					repository.shutDown();
				}

				try {
					cleanUpRepository(repositoryID);
				}
				catch (IOException e) {
					throw new RepositoryException("Unable to clean up resources for removed repository "
							+ repositoryID, e);
				}
			}
		}

		return isRemoved;
	}

	/**
	 * Checks on whether the given repository is referred to by a
	 * {@link org.eclipse.rdf4j.repository.sail.ProxyRepository} configuration.
	 * 
	 * @param repositoryID
	 *        id to check
	 * @return true if there is no existing proxy reference to the given id,
	 *         false otherwise
	 * @throws RepositoryException
	 */
	public boolean isSafeToRemove(String repositoryID)
		throws RepositoryException
	{
		RepositoryConnection connection = this.getSystemRepository().getConnection();
		try {
			return !connection.hasStatement(null, ProxyRepositorySchema.PROXIED_ID,
					connection.getValueFactory().createLiteral(repositoryID), false);
		}
		finally {
			connection.close();
		}
	}

	/**
	 * Removes the specified repository by deleting its configuration from the
	 * manager's system repository if such a configuration is present, and
	 * removing any persistent data associated with the repository. Returns
	 * <tt>true</tt> if the system repository actually contained the specified
	 * repository configuration. <strong>NB this operation can not be
	 * undone!</strong>
	 * 
	 * @since 2.6.7
	 * @param repositoryID
	 *        The ID of the repository that needs to be removed.
	 * @throws RepositoryException
	 *         If the manager failed to update its system repository.
	 * @throws RepositoryConfigException
	 *         If the manager doesn't know how to remove a repository due to
	 *         inconsistent configuration data in the system repository. For
	 *         example, this happens when there are multiple existing
	 *         configurations with the concerning ID.
	 */
	public boolean removeRepository(String repositoryID)
		throws RepositoryException, RepositoryConfigException
	{
		logger.debug("Removing repository {}.", repositoryID);
		boolean isRemoved = false;

		synchronized (initializedRepositories) {
			isRemoved = RepositoryConfigUtil.removeRepositoryConfigs(getSystemRepository(), repositoryID);

			if (isRemoved) {
				logger.debug("Shutdown repository {} after removal of configuration.", repositoryID);
				Repository repository = initializedRepositories.remove(repositoryID);

				if (repository != null && repository.isInitialized()) {
					repository.shutDown();
				}

				try {
					cleanUpRepository(repositoryID);
				}
				catch (IOException e) {
					throw new RepositoryException("Unable to clean up resources for removed repository "
							+ repositoryID, e);
				}
			}
		}

		return isRemoved;
	}

	/**
	 * Gets the repository that is known by the specified ID from this manager.
	 * 
	 * @param identity
	 *        A repository ID.
	 * @return An initialized Repository object, or <tt>null</tt> if no
	 *         repository was known for the specified ID.
	 * @throws RepositoryConfigException
	 *         If no repository could be created due to invalid or incomplete
	 *         configuration data.
	 */
	@Override
	public Repository getRepository(String identity)
		throws RepositoryConfigException, RepositoryException
	{
		synchronized (initializedRepositories) {
			Repository result = initializedRepositories.get(identity);

			if (result != null && !result.isInitialized()) {
				// repository exists but has been shut down. throw away the old
				// object so we can re-read from the config.
				initializedRepositories.remove(result);
				result = null;
			}

			if (result == null) {
				// First call (or old object thrown away), create and initialize the
				// repository.
				result = createRepository(identity);

				if (result != null) {
					initializedRepositories.put(identity, result);
				}
			}

			return result;
		}
	}

	/**
	 * Returns all initialized repositories. This method returns fast as no lazy
	 * creation of repositories takes place.
	 * 
	 * @return a collection containing the IDs of all initialized repositories.
	 * @see #getRepositoryIDs()
	 */
	public Set<String> getInitializedRepositoryIDs() {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			return new HashSet<String>(initializedRepositories.keySet());
		}
	}

	/**
	 * Returns all initialized repositories. This method returns fast as no lazy
	 * creation of repositories takes place.
	 * 
	 * @return a set containing the initialized repositories.
	 * @see #getAllRepositories()
	 */
	public Collection<Repository> getInitializedRepositories() {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			return new ArrayList<Repository>(initializedRepositories.values());
		}
	}

	Repository getInitializedRepository(String repositoryID) {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			return initializedRepositories.get(repositoryID);
		}
	}

	Repository removeInitializedRepository(String repositoryID) {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			return initializedRepositories.remove(repositoryID);
		}
	}

	protected void setInitializedRepositories(Map<String, Repository> nextInitializedRepositories) {
		initializedRepositories = nextInitializedRepositories;
	}

	protected void updateInitializedRepositories() {
		synchronized (initializedRepositories) {
			Iterator<Repository> iter = initializedRepositories.values().iterator();
			while (iter.hasNext()) {
				if (!iter.next().isInitialized()) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * Returns all configured repositories. This may be an expensive operation as
	 * it initializes repositories that have not been initialized yet.
	 * 
	 * @return The Set of all Repositories defined in the SystemRepository.
	 * @see #getInitializedRepositories()
	 */
	public Collection<Repository> getAllRepositories()
		throws RepositoryConfigException, RepositoryException
	{
		Set<String> idSet = getRepositoryIDs();

		ArrayList<Repository> result = new ArrayList<Repository>(idSet.size());

		for (String id : idSet) {
			result.add(getRepository(id));
		}

		return result;
	}

	/**
	 * Creates and initializes the repository with the specified ID.
	 * 
	 * @param id
	 *        A repository ID.
	 * @return The created and initialized repository, or <tt>null</tt> if no
	 *         such repository exists.
	 * @throws RepositoryConfigException
	 *         If no repository could be created due to invalid or incomplete
	 *         configuration data.
	 * @throws RepositoryException
	 *         If the repository could not be initialized.
	 */
	protected abstract Repository createRepository(String id)
		throws RepositoryConfigException, RepositoryException;

	/**
	 * Gets the repository that is known by the specified ID from this manager.
	 * 
	 * @param id
	 *        A repository ID.
	 * @return A Repository object, or <tt>null</tt> if no repository was known
	 *         for the specified ID.
	 * @throws RepositoryException
	 *         When not able to retrieve existing configurations
	 */
	public abstract RepositoryInfo getRepositoryInfo(String id)
		throws RepositoryException;

	public Collection<RepositoryInfo> getAllRepositoryInfos()
		throws RepositoryException
	{
		return getAllRepositoryInfos(false);
	}

	public Collection<RepositoryInfo> getAllUserRepositoryInfos()
		throws RepositoryException
	{
		return getAllRepositoryInfos(true);
	}

	/**
	 * @param skipSystemRepo
	 * @throws RepositoryException
	 *         When not able to retrieve existing configurations
	 */
	public abstract Collection<RepositoryInfo> getAllRepositoryInfos(boolean skipSystemRepo)
		throws RepositoryException;

	/**
	 * Shuts down all initialized user repositories.
	 * 
	 * @see #shutDown()
	 */
	public void refresh() {
		logger.debug("Refreshing repository information in manager...");

		// FIXME: uninitialized, removed repositories won't be cleaned up.
		try {
			RepositoryConnection cleanupCon = getSystemRepository().getConnection();
			try {
				synchronized (initializedRepositories) {
					Iterator<Map.Entry<String, Repository>> iter = initializedRepositories.entrySet().iterator();

					while (iter.hasNext()) {
						Map.Entry<String, Repository> entry = iter.next();
						String repositoryID = entry.getKey();
						Repository repository = entry.getValue();

						if (!SystemRepository.ID.equals(repositoryID)) {
							// remove from initialized repositories
							iter.remove();
							// refresh single repository
							refreshRepository(cleanupCon, repositoryID, repository);
						}
					}
				}
			}
			finally {
				cleanupCon.close();
			}
		}
		catch (RepositoryException re) {
			logger.error("Failed to refresh repositories", re);
		}
	}

	/**
	 * Shuts down all initialized repositories, including the SYSTEM repository.
	 * 
	 * @see #refresh()
	 */
	public void shutDown() {
		synchronized (initializedRepositories) {
			for (Repository repository : initializedRepositories.values()) {
				try {
					if (repository.isInitialized()) {
						repository.shutDown();
					}
				}
				catch (RepositoryException e) {
					logger.error("Repository shut down failed", e);
				}
			}

			initializedRepositories.clear();
		}
	}

	void refreshRepository(RepositoryConnection con, String repositoryID, Repository repository) {
		logger.debug("Refreshing repository {}...", repositoryID);
		try {
			if (repository.isInitialized()) {
				repository.shutDown();
			}
		}
		catch (RepositoryException e) {
			logger.error("Failed to shut down repository", e);
		}

		cleanupIfRemoved(con, repositoryID);
	}

	void cleanupIfRemoved(RepositoryConnection con, String repositoryID) {
		try {
			if (RepositoryConfigUtil.getContext(con, repositoryID) == null) {
				logger.debug("Cleaning up repository {}, its configuration has been removed", repositoryID);

				cleanUpRepository(repositoryID);
			}
			else {
				logger.debug("Repository {} should not be cleaned up.", repositoryID);
			}
		}
		catch (RepositoryException e) {
			logger.error("Failed to process repository configuration changes", e);
		}
		catch (RepositoryConfigException e) {
			logger.warn("Unable to determine if configuration for {} is still present in the system repository",
					repositoryID);
		}
		catch (IOException e) {
			logger.warn("Unable to remove data dir for removed repository {} ", repositoryID);
		}
	}

	/**
	 * Clean up a removed repository. Note that the configuration for this
	 * repository is no longer present in the system repository.
	 * 
	 * @param repositoryID
	 *        the ID of the repository to clean up
	 * @throws IOException
	 */
	protected abstract void cleanUpRepository(String repositoryID)
		throws IOException;

	/**
	 * Gets the URL of the server or directory.
	 * 
	 * @throws MalformedURLException
	 *         If the location cannot be represented as a URL.
	 */
	public abstract URL getLocation()
		throws MalformedURLException;
}
