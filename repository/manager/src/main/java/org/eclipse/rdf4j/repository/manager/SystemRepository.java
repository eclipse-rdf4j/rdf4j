/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.event.base.NotifyingRepositoryWrapper;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: do not extend NotifyingRepositoryWrapper, because SystemRepository shouldn't expose RepositoryWrapper
 * behaviour, just implement NotifyingRepository.
 * 
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
@Deprecated
public class SystemRepository extends NotifyingRepositoryWrapper {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String CONFIG_SYSTEM_TTL = "org/eclipse/rdf4j/repository/config/system.ttl";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * The repository identifier for the system repository that contains the configuration data.
	 */
	public static final String ID = "SYSTEM";

	public static final String TITLE = "System configuration repository";

	public static final String REPOSITORY_TYPE = "openrdf:SystemRepository";

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SystemRepository(File systemDir) throws RepositoryException {
		super();
		super.setDelegate(createDelegate());
		setDataDir(systemDir);
	}

	SystemRepository() throws RepositoryException {
		super();
		super.setDelegate(createDelegate());
	}

	/*---------*
	 * Methods *
	 *---------*/

	private Repository createDelegate() {
		RepositoryConfig repoConfig = getSystemConfig();
		if (repoConfig == null) {
			throw new RepositoryConfigException("Could not find SYSTEM configuration");
		}
		repoConfig.validate();
		RepositoryImplConfig config = repoConfig.getRepositoryImplConfig();
		RepositoryFactory factory = RepositoryRegistry.getInstance()
				.get(config.getType())
				.orElseThrow(
						() -> new RepositoryConfigException("Repository type not in classpath: " + config.getType()));
		return factory.getRepository(config);
	}

	private RepositoryConfig getSystemConfig() {
		URL ttl = this.getClass().getClassLoader().getResource(CONFIG_SYSTEM_TTL);
		if (ttl == null) {
			return null;
		}
		try (InputStream in = ttl.openStream()) {
			Model model = Rio.parse(in, ttl.toString(), RDFFormat.TURTLE);
			return RepositoryConfigUtil.getRepositoryConfig(model, ID);
		} catch (IOException e) {
			throw new RepositoryConfigException(e);
		}
	}

	@Override
	public void initialize() throws RepositoryException {
		super.initialize();

		try (RepositoryConnection con = getConnection()) {
			if (con.isEmpty()) {
				logger.debug("Initializing empty {} repository", ID);

				con.begin();
				con.setNamespace("rdf", RDF.NAMESPACE);
				con.setNamespace("sys", RepositoryConfigSchema.NAMESPACE);
				con.commit();

				RepositoryConfig repConfig = new RepositoryConfig(ID, TITLE, new SystemRepositoryConfig());
				RepositoryConfigUtil.updateRepositoryConfigs(con, repConfig);

			}
		} catch (RepositoryConfigException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	@Override
	public void setDelegate(Repository delegate) {
		throw new UnsupportedOperationException("Setting delegate on system repository not allowed");
	}
}
