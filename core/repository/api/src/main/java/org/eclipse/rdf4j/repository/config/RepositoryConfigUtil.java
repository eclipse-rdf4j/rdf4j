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
package org.eclipse.rdf4j.repository.config;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;

public class RepositoryConfigUtil {

	public static RepositoryConfig getRepositoryConfig(Model model, String repositoryID) {
		Statement idStatement = getIDStatement(model, repositoryID);
		if (idStatement == null) {
			// No such config
			return null;
		}
		Resource repositoryNode = idStatement.getSubject();
		Resource context = idStatement.getContext();
		Model contextGraph = model.filter(null, null, null, context);
		return RepositoryConfig.create(contextGraph, repositoryNode);
	}

	public static Model getRepositoryConfigModel(Model model, String repositoryID) {
		Statement idStatement = getIDStatement(model, repositoryID);
		if (idStatement == null) {
			// No such config
			return null;
		}
		return model.filter(null, null, null, idStatement.getContext());
	}

	public static Set<String> getRepositoryIDs(Model model) throws RepositoryException {
		Set<String> idSet = new LinkedHashSet<>();
		model.filter(null, CONFIG.REPOSITORY_ID, null).forEach(idStatement -> {
			if (idStatement.getObject() instanceof Literal) {
				Literal idLiteral = (Literal) idStatement.getObject();
				idSet.add(idLiteral.getLabel());
			}
		});
		return idSet;
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Resource} if present, falling back to a property
	 * with the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query repository config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the value of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property if the
	 *                          supplied property has no value in the model.
	 * @return the resource value for supplied subject and property (or the property with the supplied
	 *         fallbackNamespace), if present.
	 */
	@InternalUseOnly
	public static Optional<Resource> getPropertyAsResource(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		return Optional
				.ofNullable(Models.objectResource(model.getStatements(subject, property, null)))
				.orElseGet(() -> {
					IRI fallback = iri(fallbackNamespace, property.getLocalName());
					return Models.objectResource(model.getStatements(subject, fallback, null));
				});
	}

	/**
	 * Retrieve a property value for the supplied subject as a {@link Literal} if present, falling back to a property
	 * with the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query repository config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the value of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property if the
	 *                          supplied property has no value in the model.
	 * @return the literal value for supplied subject and property (or the property with the supplied
	 *         fallbackNamespace), if present.
	 */
	@InternalUseOnly
	public static Optional<Literal> getPropertyAsLiteral(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		return Optional
				.ofNullable(Models.objectLiteral(model.getStatements(subject, property, null)))
				.orElseGet(() -> {
					IRI fallback = iri(fallbackNamespace, property.getLocalName());
					return Models.objectLiteral(model.getStatements(subject, fallback, null));
				});
	}

	/**
	 * Retrieve all property values for the supplied subject as a Set of values and include all values for any property
	 * with the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query repository config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the values of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property.
	 * @return the set of values for supplied subject and property (and/or the property with the supplied
	 *         fallbackNamespace).
	 */
	@InternalUseOnly
	public static Set<Value> getPropertyValues(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		final Set<Value> results = new HashSet<>();
		results.addAll(model.filter(subject, property, null).objects());
		results.addAll(model.filter(subject, iri(fallbackNamespace, property.getLocalName()), null).objects());
		return results;
	}

	/**
	 * Retrieve a property value for the supplied subject as an {@link IRI} if present, falling back to a property with
	 * the same local name in the supplied fallbackNamespace.
	 *
	 * This method allows use to query repository config models with a mix of old and new namespaces.
	 *
	 * @param model             the model to retrieve property values from.
	 * @param subject           the subject of the property.
	 * @param property          the property to retrieve the value of.
	 * @param fallbackNamespace namespace to use in combination with the local name of the supplied property if the
	 *                          supplied property has no value in the model.
	 * @return the IRI value for supplied subject and property (or the property with the supplied fallbackNamespace), if
	 *         present.
	 */
	@InternalUseOnly
	public static Optional<IRI> getPropertyAsIRI(Model model, Resource subject, IRI property,
			String fallbackNamespace) {
		return Optional
				.ofNullable(Models.objectIRI(model.getStatements(subject, property, null)))
				.orElseGet(() -> {
					IRI fallback = iri(fallbackNamespace, property.getLocalName());
					return Models.objectIRI(model.getStatements(subject, fallback, null));
				});
	}

	private static Statement getIDStatement(Model model, String repositoryID) {
		Literal idLiteral = literal(repositoryID);
		Model idStatementList = model.filter(null, CONFIG.REPOSITORY_ID, idLiteral);

		if (idStatementList.isEmpty()) {
			IRI fallback = iri(RepositoryConfigSchema.NAMESPACE_OBSOLETE, CONFIG.REPOSITORY_ID.getLocalName());
			idStatementList = model.filter(null, fallback, idLiteral);
		}

		if (idStatementList.size() == 1) {
			return idStatementList.iterator().next();
		} else if (idStatementList.isEmpty()) {
			return null;
		} else {
			throw new RepositoryConfigException("Multiple ID-statements for repository ID " + repositoryID);
		}
	}

	@Deprecated
	public static Set<String> getRepositoryIDs(Repository repository) throws RepositoryException {
		try (RepositoryConnection con = repository.getConnection()) {
			Set<String> idSet = new LinkedHashSet<>();

			try (RepositoryResult<Statement> idStatementIter = con.getStatements(null, CONFIG.REPOSITORY_ID, null,
					true)) {
				while (idStatementIter.hasNext()) {
					Statement idStatement = idStatementIter.next();

					if (idStatement.getObject() instanceof Literal) {
						Literal idLiteral = (Literal) idStatement.getObject();
						idSet.add(idLiteral.getLabel());
					}
				}
			}

			return idSet;
		}
	}

	/**
	 * Is configuration information for the specified repository ID present in the (system) repository?
	 *
	 * @param repository   the repository to look in
	 * @param repositoryID the repositoryID to look for
	 * @return true if configurion information for the specified repository ID was found, false otherwise
	 * @throws RepositoryException       if an error occurred while trying to retrieve information from the (system)
	 *                                   repository
	 * @throws RepositoryConfigException
	 */
	@Deprecated
	public static boolean hasRepositoryConfig(Repository repository, String repositoryID)
			throws RepositoryException, RepositoryConfigException {
		try (RepositoryConnection con = repository.getConnection()) {
			return getIDStatement(con, repositoryID) != null;
		}
	}

	@Deprecated
	public static RepositoryConfig getRepositoryConfig(Repository repository, String repositoryID)
			throws RepositoryConfigException, RepositoryException {
		try (RepositoryConnection con = repository.getConnection()) {
			Statement idStatement = getIDStatement(con, repositoryID);
			if (idStatement == null) {
				// No such config
				return null;
			}

			Resource repositoryNode = idStatement.getSubject();
			Resource context = idStatement.getContext();

			if (context == null) {
				throw new RepositoryException("No configuration context for repository " + repositoryID);
			}

			Model contextGraph = QueryResults.asModel(con.getStatements(null, null, null, true, context));

			return RepositoryConfig.create(contextGraph, repositoryNode);
		}
	}

	/**
	 * Update the specified Repository with the specified set of RepositoryConfigs. This will overwrite all existing
	 * configurations in the Repository that have a Repository ID occurring in these RepositoryConfigs.
	 *
	 * @param repository The Repository whose contents will be modified.
	 * @param configs    The RepositoryConfigs that should be added to or updated in the Repository. The
	 *                   RepositoryConfig's ID may already occur in the Repository, in which case all previous
	 *                   configuration data for that Repository will be cleared before the RepositoryConfig is added.
	 * @throws RepositoryException       When access to the Repository's RepositoryConnection causes a
	 *                                   RepositoryException.
	 * @throws RepositoryConfigException
	 */
	@Deprecated
	public static void updateRepositoryConfigs(Repository repository, RepositoryConfig... configs)
			throws RepositoryException, RepositoryConfigException {
		try (RepositoryConnection con = repository.getConnection()) {
			updateRepositoryConfigs(con, configs);
		}
	}

	/**
	 * Update the specified RepositoryConnection with the specified set of RepositoryConfigs. This will overwrite all
	 * existing configurations in the Repository that have a Repository ID occurring in these RepositoryConfigs. Note:
	 * this method does NOT commit the updates on the connection.
	 *
	 * @param con     the repository connection to perform the update on
	 * @param configs The RepositoryConfigs that should be added to or updated in the Repository. The RepositoryConfig's
	 *                ID may already occur in the Repository, in which case all previous configuration data for that
	 *                Repository will be cleared before the RepositoryConfig is added.
	 * @throws RepositoryException
	 * @throws RepositoryConfigException
	 */
	@Deprecated
	public static void updateRepositoryConfigs(RepositoryConnection con, RepositoryConfig... configs)
			throws RepositoryException, RepositoryConfigException {
		ValueFactory vf = con.getRepository().getValueFactory();

		con.begin();

		for (RepositoryConfig config : configs) {
			Resource context = getContext(con, config.getID());

			if (context != null) {
				con.clear(context);
			} else {
				context = vf.createBNode();
			}

			con.add(context, RDF.TYPE, RepositoryConfigSchema.REPOSITORY_CONTEXT);

			Model graph = new LinkedHashModel();
			config.export(graph);
			con.add(graph, context);
		}

		con.commit();
	}

	/**
	 * Removes one or more Repository configurations from a Repository. Nothing happens when this Repository does not
	 * contain configurations for these Repository IDs.
	 *
	 * @param repository    The Repository to remove the configurations from.
	 * @param repositoryIDs The IDs of the Repositories whose configurations need to be removed.
	 * @throws RepositoryException       Whenever access to the Repository's RepositoryConnection causes a
	 *                                   RepositoryException.
	 * @throws RepositoryConfigException
	 */
	@Deprecated
	public static boolean removeRepositoryConfigs(Repository repository, String... repositoryIDs)
			throws RepositoryException, RepositoryConfigException {
		boolean changed = false;

		try (RepositoryConnection con = repository.getConnection()) {
			con.begin();

			for (String id : repositoryIDs) {
				Resource context = getContext(con, id);
				if (context != null) {
					con.clear(context);
					con.remove(context, RDF.TYPE, RepositoryConfigSchema.REPOSITORY_CONTEXT);
					changed = true;
				}
			}

			con.commit();
		}

		return changed;
	}

	@Deprecated
	public static Resource getContext(RepositoryConnection con, String repositoryID)
			throws RepositoryException, RepositoryConfigException {
		Resource context = null;

		Statement idStatement = getIDStatement(con, repositoryID);
		if (idStatement != null) {
			context = idStatement.getContext();
		}

		return context;
	}

	private static Statement getIDStatement(RepositoryConnection con, String repositoryID)
			throws RepositoryException, RepositoryConfigException {
		Literal idLiteral = con.getRepository().getValueFactory().createLiteral(repositoryID);
		List<Statement> idStatementList = Iterations
				.asList(con.getStatements(null, CONFIG.REPOSITORY_ID, idLiteral, true));

		if (idStatementList.size() == 1) {
			return idStatementList.get(0);
		} else if (idStatementList.isEmpty()) {
			return null;
		} else {
			throw new RepositoryConfigException("Multiple ID-statements for repository ID " + repositoryID);
		}
	}
}
