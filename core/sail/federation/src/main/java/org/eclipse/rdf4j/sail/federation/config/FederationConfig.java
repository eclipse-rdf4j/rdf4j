/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.config;

import static org.eclipse.rdf4j.repository.config.AbstractRepositoryImplConfig.create;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * Lists the members of a federation and which properties describe a resource
 * subject in a unique member.
 * 
 * @author James Leigh
 */
public class FederationConfig extends AbstractSailImplConfig {

	/** http://www.openrdf.org/config/sail/federation# */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/federation#";

	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	
	public static final IRI MEMBER = vf.createIRI(NAMESPACE + "member");

	/**
	 * For all triples with a predicate in this space, the container RDF store
	 * contains all triples with that subject and any predicate in this space.
	 */
	public static final IRI LOCALPROPERTYSPACE = vf.createIRI(NAMESPACE // NOPMD
			+ "localPropertySpace");

	/**
	 * If no two members contain the same statement.
	 */
	public static final IRI DISTINCT = vf.createIRI(NAMESPACE + "distinct");

	/**
	 * If the federation should not try and add statements to its members.
	 */
	public static final IRI READ_ONLY = vf.createIRI(NAMESPACE + "readOnly");

	private List<RepositoryImplConfig> members = new ArrayList<RepositoryImplConfig>();

	private final Set<String> localPropertySpace = new HashSet<String>(); // NOPMD

	private boolean distinct;

	private boolean readOnly;

	public List<RepositoryImplConfig> getMembers() {
		return members;
	}

	public void setMembers(List<RepositoryImplConfig> members) {
		this.members = members;
	}

	public void addMember(RepositoryImplConfig member) {
		members.add(member);
	}

	public Set<String> getLocalPropertySpace() {
		return localPropertySpace;
	}

	public void addLocalPropertySpace(String localPropertySpace) { // NOPMD
		this.localPropertySpace.add(localPropertySpace);
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean disjoint) {
		this.distinct = disjoint;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public Resource export(Model model) {
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		Resource self = super.export(model);
		for (RepositoryImplConfig member : getMembers()) {
			model.add(self, MEMBER, member.export(model));
		}
		for (String space : getLocalPropertySpace()) {
			model.add(self, LOCALPROPERTYSPACE, valueFactory.createIRI(space));
		}
		model.add(self, DISTINCT, valueFactory.createLiteral(distinct));
		model.add(self, READ_ONLY, valueFactory.createLiteral(readOnly));
		return self;
	}

	@Override
	public void parse(Model graph, Resource implNode)
		throws SailConfigException
	{
		super.parse(graph, implNode);
		LinkedHashModel model = new LinkedHashModel(graph);
		for (Value member : model.filter(implNode, MEMBER, null).objects()) {
			try {
				addMember(create(graph, (Resource)member));
			}
			catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
		for (Value space : model.filter(implNode, LOCALPROPERTYSPACE, null).objects()) {
			addLocalPropertySpace(space.stringValue());
		}
		try {
			Optional<Literal> bool = Models.objectLiteral(model.filter(implNode, DISTINCT, null));
			if (bool.isPresent() && bool.get().booleanValue()) {
				distinct = true;
			}
			bool = Models.objectLiteral(model.filter(implNode, READ_ONLY, null));
			if (bool.isPresent() && bool.get().booleanValue()) {
				readOnly = true;
			}
		}
		catch (ModelException e) {
			throw new SailConfigException(e);
		}
	}

	@Override
	public void validate()
		throws SailConfigException
	{
		super.validate();
		if (members.isEmpty()) {
			throw new SailConfigException("No federation members specified");
		}
		for (RepositoryImplConfig member : members) {
			try {
				member.validate();
			}
			catch (RepositoryConfigException e) {
				throw new SailConfigException(e);
			}
		}
	}

}
