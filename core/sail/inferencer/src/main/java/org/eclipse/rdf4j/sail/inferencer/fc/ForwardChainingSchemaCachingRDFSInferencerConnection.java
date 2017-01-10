/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ForwardChainingSchemaCachingRDFSInferencerConnection
		extends AbstractForwardChainingInferencerConnection
{

	private final ForwardChainingSchemaCachingRDFSInferencer inferencerSail;

	long lockStamp = 0;

	private boolean inferredCleared = false;

	private long originalSchemaSize = -1;
	
	ForwardChainingSchemaCachingRDFSInferencerConnection(
			ForwardChainingSchemaCachingRDFSInferencer forwardChainingSchemaCachingRDFSInferencer,
			InferencerConnection e)
	{
		super(forwardChainingSchemaCachingRDFSInferencer, e);
		this.inferencerSail = forwardChainingSchemaCachingRDFSInferencer;
	}

	@Override
	public void rollback()
		throws SailException
	{
		super.rollback();
		if (lockStamp != 0) {
			inferencerSail.releaseLock(this);
		}
		//@TODO Do I need to clean up the tbox cache and lookup maps after rolling back? Probably if the connection has a write lock.
	}
	
	void processForSchemaCache(Statement statement) {
		final IRI predicate = statement.getPredicate();
		final Value object = statement.getObject();
		final Resource subject = statement.getSubject();

		if (predicate.equals(RDFS.SUBCLASSOF)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addSubClassOfStatement(statement);
		}
		else if (predicate.equals(RDF.TYPE) && object.equals(RDF.PROPERTY)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addProperty(subject);

		}
		else if (predicate.equals(RDFS.SUBPROPERTYOF)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addSubPropertyOfStatement(statement);
		}
		else if (predicate.equals(RDFS.RANGE)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addRangeStatement(statement);
		}
		else if (predicate.equals(RDFS.DOMAIN)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addDomainStatement(statement);
		}
		else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CLASS)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addSubClassOfStatement(
					inferencerSail.getValueFactory().createStatement(subject,
							RDFS.SUBCLASSOF, RDFS.RESOURCE));
		}
		else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.DATATYPE)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addSubClassOfStatement(
					inferencerSail.getValueFactory().createStatement(subject,
							RDFS.SUBCLASSOF, RDFS.LITERAL));
		}
		else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CONTAINERMEMBERSHIPPROPERTY)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addSubPropertyOfStatement(
					inferencerSail.getValueFactory().createStatement(subject,
							RDFS.SUBPROPERTYOF, RDFS.MEMBER));
		}
		else if (predicate.equals(RDF.TYPE)) {
			if (!inferencerSail.hasType(((Resource)object))) {
				inferencerSail.upgradeLock(this);
				inferencerSail.addType((Resource)object);
			}
		}

		if (!inferencerSail.hasProperty(predicate)) {
			inferencerSail.upgradeLock(this);
			inferencerSail.addProperty(predicate);
		}

	}


	@Override
	public void clearInferred(Resource... contexts)
		throws SailException
	{
		super.clearInferred(contexts);
		inferredCleared = true;
	}

	@Override
	public void begin(IsolationLevel level)
		throws UnknownSailTransactionStateException
	{
		super.begin(level);
		inferencerSail.readLock(this);
		originalSchemaSize = inferencerSail.getSchemaSize();
	}

	@Override
	public void commit()
		throws SailException
	{
		super.commit();
		inferencerSail.releaseLock(this);
	}

	@Override
	protected void doInferencing()
		throws SailException
	{
		prepareIteration();

		if (inferencerSail.schema == null && originalSchemaSize != inferencerSail.getSchemaSize())
		{
			inferencerSail.upgradeLock(this);

			inferencerSail.clearInferenceTables();
			addAxiomStatements();

			try (CloseableIteration<? extends Statement, SailException> statements = getWrappedConnection().getStatements(
					null, null, null, false))
			{
				while (statements.hasNext()) {
					Statement next = statements.next();
					processForSchemaCache(next);
				}
			}
			inferencerSail.calculateInferenceMaps(this);
			inferredCleared = true;
		}

		if (!inferredCleared) {
			return;
		}

		try (CloseableIteration<? extends Statement, SailException> statements = getWrappedConnection().getStatements(
				null, null, null, false))
		{
			while (statements.hasNext()) {
				Statement next = statements.next();
				addStatement(false, next.getSubject(), next.getPredicate(), next.getObject(),
						next.getContext());
			}
		}
		inferredCleared = false;

	}

	@Override
	public void close()
		throws SailException
	{
		if (lockStamp != 0) {
			inferencerSail.releaseLock(this);
		}
		super.close();

	}

	@Override
	protected Model createModel() {
		// TODO possibly use disk overflow
		return new LinkedHashModelFactory().createEmptyModel();
	}

	@Override
	protected int applyRules(Model model)
		throws SailException
	{

		// Required by extended class

		// Not used here because rules are usually applied while adding data
		// and not at the end of a transaction

		return 0;
	}

	public void addStatement(Resource subject, IRI predicate, Value object, Resource... contexts)
		throws SailException
	{
		addStatement(true, subject, predicate, object, contexts);
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts)
		throws SailException
	{
		return super.addInferredStatement(subj, pred, obj, contexts);
	}

	// actuallyAdd
	private void addStatement(boolean actuallyAdd, Resource subject, IRI predicate, Value object,
			Resource... resources)
		throws SailException
	{

		if (inferencerSail.schema == null) {
			processForSchemaCache(inferencerSail.getValueFactory().createStatement(
					subject, predicate, object));
		}

		if (inferencerSail.useAllRdfsRules) {
			addInferredStatement(subject, RDF.TYPE, RDFS.RESOURCE);

			if (object instanceof Resource) {
				addInferredStatement((Resource)object, RDF.TYPE, RDFS.RESOURCE);
			}
		}

		if (predicate.getNamespace().equals(RDF.NAMESPACE) && predicate.getLocalName().charAt(0) == '_') {

			try {
				int i = Integer.parseInt(predicate.getLocalName().substring(1));
				if (i >= 1) {
					addInferredStatement(subject, RDFS.MEMBER, object);

					addInferredStatement(predicate, RDF.TYPE, RDFS.RESOURCE);
					addInferredStatement(predicate, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY);
					addInferredStatement(predicate, RDF.TYPE, RDF.PROPERTY);
					addInferredStatement(predicate, RDFS.SUBPROPERTYOF, predicate);
					addInferredStatement(predicate, RDFS.SUBPROPERTYOF, RDFS.MEMBER);

				}
			}
			catch (NumberFormatException e) {
				// Ignore exception.

				// Means that the predicate started with rdf:_ but does not
				// comply with the container membership format of rdf:_nnn
				// and we can safely ignore this exception since it just means
				// that we didn't need to infer anything about container membership
			}

		}

		if (actuallyAdd) {
			getWrappedConnection().addStatement(subject, predicate, object, resources);

		}

		if (predicate.equals(RDF.TYPE)) {
			if (!(object instanceof Resource)) {
				throw new SailException("Expected object to a a Resource: " + object.toString());
			}

			inferencerSail.resolveTypes((Resource)object).stream().peek(inferredType -> {
				if (inferencerSail.useAllRdfsRules
						&& inferredType.equals(RDFS.CLASS))
				{
					addInferredStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE);
				}
			}).filter(inferredType -> !inferredType.equals(object)).forEach(
					inferredType -> addInferredStatement(subject, RDF.TYPE, inferredType));
		}

		inferencerSail.resolveProperties(predicate).stream().filter(
				inferredProperty -> !inferredProperty.equals(predicate)).filter(
						inferredPropery -> inferredPropery instanceof IRI).map(
								inferredPropery -> ((IRI)inferredPropery)).forEach(
										inferredProperty -> addInferredStatement(subject, inferredProperty,
												object));

		if (object instanceof Resource) {
			inferencerSail.resolveRangeTypes(predicate).stream().peek(inferredType -> {
				if (inferencerSail.useAllRdfsRules
						&& inferredType.equals(RDFS.CLASS))
				{
					addInferredStatement(((Resource)object), RDFS.SUBCLASSOF, RDFS.RESOURCE);
				}
			}).forEach(inferredType -> addInferredStatement(((Resource)object), RDF.TYPE, inferredType));
		}

		inferencerSail.resolveDomainTypes(predicate).stream().peek(inferredType -> {
			if (inferencerSail.useAllRdfsRules
					&& inferredType.equals(RDFS.CLASS))
			{
				addInferredStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			}
		}).forEach(inferredType -> addInferredStatement((subject), RDF.TYPE, inferredType));

	}

	protected void addAxiomStatements() {
		ValueFactory vf = inferencerSail.getValueFactory();

		// This is http://www.w3.org/2000/01/rdf-schema# forward chained
		// Eg. all axioms in RDFS forward chained w.r.t. RDFS.
		// All those axioms are simply listed here

		Statement statement = vf.createStatement(RDF.ALT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDF.ALT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.BAG, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.BAG, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDF.BAG);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.LIST, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.LIST, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.LIST, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.LIST, RDFS.SUBCLASSOF, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PROPERTY, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PROPERTY, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PROPERTY, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SEQ, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SEQ, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDF.SEQ);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.STATEMENT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.STATEMENT, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.STATEMENT, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.STATEMENT, RDFS.SUBCLASSOF, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.DATATYPE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDF.XMLLITERAL);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.FIRST, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.FIRST, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.FIRST, RDFS.DOMAIN, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.FIRST, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.FIRST, RDFS.SUBPROPERTYOF, RDF.FIRST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.NIL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.OBJECT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.OBJECT, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.OBJECT, RDFS.DOMAIN, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.OBJECT, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.OBJECT, RDFS.SUBPROPERTYOF, RDF.OBJECT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PREDICATE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PREDICATE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PREDICATE, RDFS.DOMAIN, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PREDICATE, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.PREDICATE, RDFS.SUBPROPERTYOF, RDF.PREDICATE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.REST, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.REST, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.REST, RDFS.DOMAIN, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.REST, RDFS.RANGE, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.REST, RDFS.SUBPROPERTYOF, RDF.REST);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SUBJECT, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SUBJECT, RDFS.DOMAIN, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SUBJECT, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.SUBJECT, RDFS.SUBPROPERTYOF, RDF.SUBJECT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.TYPE, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.TYPE, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.TYPE, RDFS.SUBPROPERTYOF, RDF.TYPE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.VALUE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.VALUE, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.VALUE, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDF.VALUE, RDFS.SUBPROPERTYOF, RDF.VALUE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CLASS, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CLASS, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CLASS, RDFS.SUBCLASSOF, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINER, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINER, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINER, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINER, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF,
				RDFS.CONTAINERMEMBERSHIPPROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DATATYPE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DATATYPE, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.DATATYPE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LITERAL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LITERAL, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LITERAL, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.COMMENT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.COMMENT, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.COMMENT, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.COMMENT, RDFS.RANGE, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.COMMENT, RDFS.SUBPROPERTYOF, RDFS.COMMENT);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DOMAIN, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DOMAIN, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DOMAIN, RDFS.DOMAIN, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DOMAIN, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.DOMAIN, RDFS.SUBPROPERTYOF, RDFS.DOMAIN);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.ISDEFINEDBY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LABEL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LABEL, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LABEL, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LABEL, RDFS.RANGE, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.LABEL, RDFS.SUBPROPERTYOF, RDFS.LABEL);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.MEMBER, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.MEMBER, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.MEMBER, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.MEMBER, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.MEMBER, RDFS.SUBPROPERTYOF, RDFS.MEMBER);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RANGE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RANGE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RANGE, RDFS.DOMAIN, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RANGE, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.RANGE, RDFS.SUBPROPERTYOF, RDFS.RANGE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SEEALSO, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SEEALSO, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SEEALSO, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SEEALSO, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SEEALSO, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.DOMAIN, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.SUBPROPERTYOF, RDFS.SUBCLASSOF);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.DOMAIN, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.RANGE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.SUBPROPERTYOF, RDFS.SUBPROPERTYOF);
		processForSchemaCache(statement);
		addInferredStatement(statement.getSubject(), statement.getPredicate(), statement.getObject());

	}

}
