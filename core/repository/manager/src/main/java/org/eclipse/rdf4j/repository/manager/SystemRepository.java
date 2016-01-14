/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.io.File;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.event.base.NotifyingRepositoryWrapper;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME: do not extend NotifyingRepositoryWrapper, because SystemRepository
 * shouldn't expose RepositoryWrapper behaviour, just implement
 * NotifyingRepository.
 * 
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class SystemRepository extends NotifyingRepositoryWrapper {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * The repository identifier for the system repository that contains the
	 * configuration data.
	 */
	public static final String ID = "SYSTEM";

	public static final String TITLE = "System configuration repository";

	public static final String REPOSITORY_TYPE = "openrdf:SystemRepository";

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SystemRepository(File systemDir)
		throws RepositoryException
	{
		super();
		super.setDelegate(new SailRepository(new MemoryStore(systemDir)));
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void initialize()
		throws RepositoryException
	{
		super.initialize();

		RepositoryConnection con = getConnection();
		try {
			if (con.isEmpty()) {
				logger.debug("Initializing empty {} repository", ID);

				con.begin();
				con.setNamespace("rdf", RDF.NAMESPACE);
				con.setNamespace("sys", RepositoryConfigSchema.NAMESPACE);
				con.commit();

				RepositoryConfig repConfig = new RepositoryConfig(ID, TITLE, new SystemRepositoryConfig());
				RepositoryConfigUtil.updateRepositoryConfigs(con, repConfig);

			}
		}
		catch (RepositoryConfigException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
		finally {
			con.close();
		}
	}

	@Override
	public void setDelegate(Repository delegate)
	{
		throw new UnsupportedOperationException("Setting delegate on system repository not allowed");
	}
}
