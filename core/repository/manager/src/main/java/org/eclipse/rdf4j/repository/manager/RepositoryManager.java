/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientDependent;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A manager for {@link Repository}s.
 *
 * @author Arjohn Kampman
 * @see RepositoryProvider
 */
public abstract class RepositoryManager implements RepositoryResolver, HttpClientDependent {

	/*-----------*
	 * Constants *
	 *-----------*/

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * The {@link org.eclipse.rdf4j.repository.sail.ProxyRepository} schema namespace (
	 * <var>http://www.openrdf.org/config/repository/proxy#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/proxy#";

	/** <var>http://www.openrdf.org/config/repository/proxy#proxiedID</var> */
	public final static IRI PROXIED_ID = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "proxiedID");

	/*-----------*
	 * Variables *
	 *-----------*/

	private ModelFactory modelFactory = new TreeModelFactory();

	protected Map<String, Repository> initializedRepositories;

	private boolean initialized;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RepositoryManager.
	 */
	protected RepositoryManager() {
		this(new HashMap<>());
	}

	/**
	 * Create a new RepositoryManager using the given map to store repository information.
	 *
	 * @param initializedRepositories A map that will be used to store repository information.
	 */
	protected RepositoryManager(Map<String, Repository> initializedRepositories) {
		setInitializedRepositories(initializedRepositories);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Indicates if this RepositoryManager has been initialized. Note that the initialization status may change if the
	 * Repository is shut down.
	 *
	 * @return true iff the repository manager has been initialized.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @return Returns the httpClient passed to {@link Repository} construction.
	 */
	@Override
	public abstract HttpClient getHttpClient();

	/**
	 * Should be called before {@link #init()}.
	 *
	 * @param httpClient The httpClient to use for remote/service calls.
	 */
	@Override
	public abstract void setHttpClient(HttpClient httpClient);

	/**
	 * Get the {@link ModelFactory} used for creating new {@link Model} objects in the manager.
	 *
	 * @return the modelFactory
	 * @since 3.0
	 */
	public ModelFactory getModelFactory() {
		return modelFactory;
	}

	/**
	 * Set the {@link ModelFactory} to use for creating new {@link Model} objects in the manager.
	 *
	 * @param modelFactory the modelFactory to set. May not be <code>null</code>.
	 * @since 3.0
	 */
	public void setModelFactory(ModelFactory modelFactory) {
		Objects.requireNonNull(modelFactory);
		this.modelFactory = modelFactory;
	}

	/**
	 * Initializes the repository manager.
	 *
	 * @throws RepositoryException If the manager failed to initialize.
	 * @since 2.5
	 */
	public void init() throws RepositoryException {
		initialized = true;
	}

	/**
	 * Generates an ID for a new repository based on the specified base name. The base name may for example be a
	 * repository name entered by the user. The generated ID will contain a variant of this name that does not occur as
	 * a repository ID in this manager yet and is suitable for use as a file name (e.g. for the repository's data
	 * directory).
	 *
	 * @param baseName The String on which the returned ID should be based, must not be <var>null</var>.
	 * @return A new repository ID derived from the specified base name.
	 * @throws RepositoryException
	 * @throws RepositoryConfigException
	 */
	public String getNewRepositoryID(String baseName) throws RepositoryException, RepositoryConfigException {
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
				} else if (c != '"' && c != '\'') {
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
		} else if (!baseName.endsWith("-")) {
			baseName += "-";
		}

		// Keep appending numbers until we find an unused ID
		int index = 2;
		while (hasRepositoryConfig(baseName + index)) {
			index++;
		}

		return baseName + index;
	}

	/**
	 * Get the IDs of all available repositories. Note that this is potentially slow as it may initialize all available
	 * repository configurations.
	 *
	 * @return a list of repository ID strings.
	 * @throws RepositoryException
	 * @see {@link #getInitializedRepositoryIDs()}
	 */
	public Set<String> getRepositoryIDs() throws RepositoryException {
		Set<String> idSet = new LinkedHashSet<>();
		getAllRepositoryInfos().forEach(info -> {
			idSet.add(info.getId());
		});
		return idSet;
	}

	public boolean hasRepositoryConfig(String repositoryID) throws RepositoryException, RepositoryConfigException {
		return getRepositoryInfo(repositoryID) != null;
	}

	public abstract RepositoryConfig getRepositoryConfig(String repositoryID)
			throws RepositoryConfigException, RepositoryException;

	/**
	 * Adds or updates the configuration of a repository to the manager. The manager may already contain a configuration
	 * for a repository with the same ID as specified by <var>config</var>, in which case all previous configuration
	 * data for that repository will be cleared before the new configuration is added.
	 *
	 * @param config The repository configuration that should be added to or updated in the manager.
	 * @throws RepositoryException       If the manager failed to update.
	 * @throws RepositoryConfigException If the manager doesn't know how to update a configuration due to inconsistent
	 *                                   configuration data. For example, this happens when there are multiple existing
	 *                                   configurations with the concerning ID.
	 */
	public abstract void addRepositoryConfig(RepositoryConfig config)
			throws RepositoryException, RepositoryConfigException;

	/**
	 * Checks on whether the given repository is referred to by a
	 * {@link org.eclipse.rdf4j.repository.sail.ProxyRepository} configuration.
	 *
	 * @param repositoryID id to check
	 * @return true if there is no existing proxy reference to the given id, false otherwise
	 * @throws RepositoryException
	 */
	public boolean isSafeToRemove(String repositoryID) throws RepositoryException {
		for (String id : getRepositoryIDs()) {
			RepositoryConfig config = getRepositoryConfig(id);
			Model model = new LinkedHashModel();
			config.export(model, Values.bnode());
			if (model.contains(null, PROXIED_ID, Values.literal(repositoryID))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes the specified repository by deleting its configuration if such a configuration is present, and removing
	 * any persistent data associated with the repository. Returns <var>true</var> if the specified repository
	 * configuration was actually present. <strong>NB this operation can not be undone!</strong>
	 *
	 * @param repositoryID The ID of the repository that needs to be removed.
	 * @throws RepositoryException       If the manager failed to update the configuration.
	 * @throws RepositoryConfigException If the manager doesn't know how to remove a repository due to inconsistent
	 *                                   configuration data. For example, this can happen when there are multiple
	 *                                   existing configurations with the concerning ID.
	 */
	public boolean removeRepository(String repositoryID) throws RepositoryException, RepositoryConfigException {
		logger.debug("Removing repository {}.", repositoryID);
		boolean isRemoved = hasRepositoryConfig(repositoryID);

		synchronized (initializedRepositories) {
			if (isRemoved) {
				logger.debug("Shutdown repository {} after removal of configuration.", repositoryID);
				Repository repository = initializedRepositories.remove(repositoryID);

				if (repository != null && repository.isInitialized()) {
					repository.shutDown();
				}
			}
		}

		return isRemoved;
	}

	/**
	 * Gets the repository that is known by the specified ID from this manager.
	 *
	 * @param identity A repository ID.
	 * @return An initialized Repository object, or <var>null</var> if no repository was known for the specified ID.
	 * @throws RepositoryConfigException If no repository could be created due to invalid or incomplete configuration
	 *                                   data.
	 */
	@Override
	public Repository getRepository(String identity) throws RepositoryConfigException, RepositoryException {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			Repository result = initializedRepositories.get(identity);

			if (result != null && !result.isInitialized()) {
				// repository exists but has been shut down. throw away the old
				// object so we can re-read from the config.
				initializedRepositories.remove(identity);
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
	 * Returns all initialized repositories. This method returns fast as no lazy creation of repositories takes place.
	 *
	 * @return a collection containing the IDs of all initialized repositories.
	 * @see #getRepositoryIDs()
	 */
	public Set<String> getInitializedRepositoryIDs() {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			return new HashSet<>(initializedRepositories.keySet());
		}
	}

	/**
	 * Returns all initialized repositories. This method returns fast as no lazy creation of repositories takes place.
	 *
	 * @return a set containing the initialized repositories.
	 * @see #getAllRepositories()
	 */
	public Collection<Repository> getInitializedRepositories() {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			return new ArrayList<>(initializedRepositories.values());
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
				Repository next = iter.next();
				if (!next.isInitialized()) {
					iter.remove();
					try {
						next.shutDown();
					} catch (RepositoryException e) {

					}
				}
			}
		}
	}

	/**
	 * Returns all configured repositories. This may be an expensive operation as it initializes repositories that have
	 * not been initialized yet.
	 *
	 * @return The Set of all configured Repositories.
	 * @see #getInitializedRepositories()
	 */
	public Collection<Repository> getAllRepositories() throws RepositoryConfigException, RepositoryException {
		Set<String> idSet = getRepositoryIDs();

		ArrayList<Repository> result = new ArrayList<>(idSet.size());

		for (String id : idSet) {
			result.add(getRepository(id));
		}

		return result;
	}

	/**
	 * Creates and initializes the repository with the specified ID.
	 *
	 * @param id A repository ID.
	 * @return The created and initialized repository, or <var>null</var> if no such repository exists.
	 * @throws RepositoryConfigException If no repository could be created due to invalid or incomplete configuration
	 *                                   data.
	 * @throws RepositoryException       If the repository could not be initialized.
	 */
	protected abstract Repository createRepository(String id) throws RepositoryConfigException, RepositoryException;

	/**
	 * Gets the repository that is known by the specified ID from this manager.
	 *
	 * @param id A repository ID.
	 * @return A Repository object, or <var>null</var> if no repository was known for the specified ID.
	 * @throws RepositoryException When not able to retrieve existing configurations
	 */
	public RepositoryInfo getRepositoryInfo(String id) throws RepositoryException {
		for (RepositoryInfo repInfo : getAllRepositoryInfos()) {
			if (repInfo.getId().equals(id)) {
				return repInfo;
			}
		}

		return null;
	}

	/**
	 * Retrieve meta information of all configured repositories.
	 *
	 * @return a collection of {@link RepositoryInfo} objects
	 * @throws RepositoryException if the repository meta information could not be retrieved.
	 */
	public abstract Collection<RepositoryInfo> getAllRepositoryInfos() throws RepositoryException;

	/**
	 * @deprecated since 4.0 - use {@link #getAllRepositoryInfos()} instead.
	 */
	@Deprecated
	public Collection<RepositoryInfo> getAllUserRepositoryInfos() throws RepositoryException {
		return getAllRepositoryInfos();
	}

	/**
	 * @deprecated since 4.0 - use {@link #getAllRepositoryInfos()} instead.
	 */
	@Deprecated
	public Collection<RepositoryInfo> getAllRepositoryInfos(boolean skipSystemRepo) throws RepositoryException {
		return getAllRepositoryInfos();
	}

	/**
	 * Shuts down all initialized user repositories.
	 *
	 * @see #shutDown()
	 */
	public void refresh() {
		logger.debug("Refreshing repository information in manager...");

		// FIXME: uninitialized, removed repositories won't be cleaned up.
		try {
			synchronized (initializedRepositories) {
				Iterator<Map.Entry<String, Repository>> iter = initializedRepositories.entrySet().iterator();

				while (iter.hasNext()) {
					Map.Entry<String, Repository> entry = iter.next();
					String repositoryID = entry.getKey();
					Repository repository = entry.getValue();

					// remove from initialized repositories
					iter.remove();
					// refresh single repository
					refreshRepository(repositoryID, repository);
				}
			}
		} catch (RepositoryException re) {
			logger.error("Failed to refresh repositories", re);
		}
	}

	/**
	 * Shuts down all initialized repositories.
	 *
	 * @see #refresh()
	 */
	public void shutDown() {
		synchronized (initializedRepositories) {
			updateInitializedRepositories();
			for (Repository repository : initializedRepositories.values()) {
				try {
					if (repository.isInitialized()) {
						repository.shutDown();
					}
				} catch (RepositoryException e) {
					logger.error("Repository shut down failed", e);
				}
			}

			initializedRepositories.clear();
			initialized = false;
		}
	}

	void refreshRepository(String repositoryID, Repository repository) {
		logger.debug("Refreshing repository {}...", repositoryID);
		try {
			if (repository.isInitialized()) {
				repository.shutDown();
			}
		} catch (RepositoryException e) {
			logger.error("Failed to shut down repository", e);
		}
	}

	/**
	 * Gets the URL of the server or directory.
	 *
	 * @throws MalformedURLException If the location cannot be represented as a URL.
	 */
	public abstract URL getLocation() throws MalformedURLException;
}
