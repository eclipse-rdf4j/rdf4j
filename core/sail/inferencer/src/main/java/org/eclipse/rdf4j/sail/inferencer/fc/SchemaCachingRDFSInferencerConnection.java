/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Mikkelsen Ottestad
 */

public class SchemaCachingRDFSInferencerConnection extends InferencerConnectionWrapper
		implements SailConnectionListener {

	private static final Logger logger = LoggerFactory.getLogger(SchemaCachingRDFSInferencerConnection.class);
	private static final Resource[] DEFAULT_CONTEXT = { null };

	private final SchemaCachingRDFSInferencer sail;

	private final NotifyingSailConnection connection;

	/**
	 * true if the base Sail reported removed statements.
	 */
	private boolean statementsRemoved;

	/**
	 * true if the base Sail reported added statements.
	 */
	private boolean statementsAdded;

	/**
	 * Predicates that determine if a statement changes the schema cache
	 */
	private final List<IRI> schemaPredicates = Arrays.asList(RDFS.SUBCLASSOF, RDFS.SUBPROPERTYOF, RDFS.RANGE,
			RDFS.DOMAIN, RDF.TYPE);

	/**
	 * true iff the schema was changed as part of the transaction.
	 */
	private boolean schemaChange;

	private long addedInferredStatementsCount = 0;

	SchemaCachingRDFSInferencerConnection(SchemaCachingRDFSInferencer sail, InferencerConnection connection) {

		super(connection);
		connection.addConnectionListener(this);

		this.sail = sail;
		this.connection = connection;

	}

	void processForSchemaCache(Statement statement) {
		sail.acquireExclusiveWriteLock();

		final IRI predicate = statement.getPredicate();
		final Value object = statement.getObject();
		final Resource subject = statement.getSubject();

		if (predicate.equals(RDFS.SUBCLASSOF)) {
			sail.addSubClassOfStatement(statement);
			schemaChange = true;
		} else if (predicate.equals(RDF.TYPE) && object.equals(RDF.PROPERTY)) {
			sail.addProperty(subject);
			schemaChange = true;
		} else if (predicate.equals(RDFS.SUBPROPERTYOF)) {
			sail.addSubPropertyOfStatement(statement);
			schemaChange = true;
		} else if (predicate.equals(RDFS.RANGE)) {
			sail.addRangeStatement(statement);
			schemaChange = true;
		} else if (predicate.equals(RDFS.DOMAIN)) {
			sail.addDomainStatement(statement);
			schemaChange = true;
		} else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CLASS)) {
			sail.addSubClassOfStatement(
					sail.getValueFactory().createStatement(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE));
			schemaChange = true;
		} else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.DATATYPE)) {
			sail.addSubClassOfStatement(sail.getValueFactory().createStatement(subject, RDFS.SUBCLASSOF, RDFS.LITERAL));
			schemaChange = true;
		} else if (predicate.equals(RDF.TYPE) && object.equals(RDFS.CONTAINERMEMBERSHIPPROPERTY)) {
			sail.addSubPropertyOfStatement(
					sail.getValueFactory().createStatement(subject, RDFS.SUBPROPERTYOF, RDFS.MEMBER));
			schemaChange = true;
		} else if (predicate.equals(RDF.TYPE)) {
			if (!sail.hasType(((Resource) object))) {
				sail.addType((Resource) object);
				schemaChange = true;
			}
		}

		if (!sail.hasProperty(predicate)) {
			sail.addProperty(predicate);
			schemaChange = true;
		}

	}

	private boolean inferredCleared = false;

	@Override
	public void clearInferred(Resource... contexts) throws SailException {
		logger.debug("Clearing all inferred statements");
		super.clearInferred(contexts);
		inferredCleared = true;
	}

	@Override
	public void commit() throws SailException {
		super.commit();

		statementsRemoved = false;
		statementsAdded = false;
		schemaChange = false;

		sail.releaseExclusiveWriteLock();
	}

	void doInferencing() throws SailException {
		logger.debug("Do inferencing");
		if (!sail.usesPredefinedSchema() && schemaChange) {
			regenerateCacheAndInferenceMaps(true);
			inferredCleared = true;
		}

		if (!inferredCleared) {
			return;
		}

		logger.debug("Forward chain all explicit statements");
		long count = 0;

		try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null, null,
				null, false)) {
			while (statements.hasNext()) {
				Statement next = statements.next();
				addStatement(false, next.getSubject(), next.getPredicate(), next.getObject(), next.getContext());
				if (logger.isDebugEnabled()) {
					if (++count % 1000000 == 0) {
						logger.debug("Forward chained {} statements", count);
					}
				}
			}
		}
		inferredCleared = false;

		logger.debug("Inferencing complete");

	}

	private void regenerateCacheAndInferenceMaps(boolean addInferredStatements) {
		logger.debug("Regenerate cache and inference maps");
		sail.clearInferenceTables();
		if (addInferredStatements) {
			addAxiomStatements();
		}

		try (CloseableIteration<? extends Statement, SailException> statements = connection.getStatements(null, null,
				null, sail.useInferredToCreateSchema)) {
			while (statements.hasNext()) {
				Statement next = statements.next();
				processForSchemaCache(sail.getValueFactory()
						.createStatement(next.getSubject(), next.getPredicate(), next.getObject()));
			}
		}
		sail.calculateInferenceMaps(this, addInferredStatements);
	}

	boolean addInferredStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		if (logger.isTraceEnabled()) {
			logger.trace("Adding inferred statement: <{}> <{}> <{}> <{}>", subj, pred, obj, Arrays.toString(contexts));
		}
		if (logger.isDebugEnabled()) {
			if (++addedInferredStatementsCount % 1000000 == 0) {
				logger.debug("Added {} inferred statements", addedInferredStatementsCount);
			}
		}
		return super.addInferredStatement(subj, pred, obj, contexts);
	}

	@Override
	public boolean addInferredStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		sail.useInferredToCreateSchema = true;
		addStatement(false, subj, pred, obj, contexts);
		return super.addInferredStatement(subj, pred, obj, contexts);
	}

	@Override
	public void addStatement(Resource subject, IRI predicate, Value object, Resource... contexts) throws SailException {
		addStatement(true, subject, predicate, object, contexts);
	}

	// actuallyAdd
	private void addStatement(boolean actuallyAdd, Resource subject, IRI predicate, Value object, Resource... context)
			throws SailException {

		if (logger.isTraceEnabled()) {
			logger.trace("Adding statement: <{}> <{}> <{}> <{}>", subject, predicate, object, Arrays.toString(context));
		}

		Resource[] inferredContext;
		if (sail.isAddInferredStatementsToDefaultContext()) {
			inferredContext = DEFAULT_CONTEXT;
		} else {
			inferredContext = context;
		}
		if (!(schemaChange && actuallyAdd)) {
			sail.acquireExclusiveWriteLock();
			if (!sail.usesPredefinedSchema()) {
				processForSchemaCache(sail.getValueFactory().createStatement(subject, predicate, object));
			}

			if (sail.useAllRdfsRules) {
				addInferredStatementInternal(subject, RDF.TYPE, RDFS.RESOURCE, inferredContext);

				if (object instanceof Resource) {
					addInferredStatementInternal((Resource) object, RDF.TYPE, RDFS.RESOURCE, inferredContext);
				}
			}

			if (predicate.getNamespace().equals(RDF.NAMESPACE) && predicate.getLocalName().charAt(0) == '_') {

				try {
					int i = Integer.parseInt(predicate.getLocalName().substring(1));
					if (i >= 1) {
						addInferredStatementInternal(subject, RDFS.MEMBER, object, inferredContext);
						addInferredStatementInternal(predicate, RDF.TYPE, RDFS.RESOURCE, inferredContext);
						addInferredStatementInternal(predicate, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY,
								inferredContext);
						addInferredStatementInternal(predicate, RDF.TYPE, RDF.PROPERTY, inferredContext);
						addInferredStatementInternal(predicate, RDFS.SUBPROPERTYOF, predicate, inferredContext);
						addInferredStatementInternal(predicate, RDFS.SUBPROPERTYOF, RDFS.MEMBER, inferredContext);

					}
				} catch (NumberFormatException e) {
					// Ignore exception.

					// Means that the predicate started with rdf:_ but does not
					// comply with the container membership format of rdf:_nnn
					// and we can safely ignore this exception since it just means
					// that we didn't need to infer anything about container membership
				}

			}

			if (predicate.equals(RDF.TYPE)) {
				if (!(object instanceof Resource)) {
					throw new SailException("Expected object to a a Resource: " + object.toString());
				}

				sail.resolveTypes((Resource) object)
						.stream()
						.peek(inferredType -> {
							if (sail.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
								addInferredStatementInternal(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE, inferredContext);
							}
						})
						.filter(inferredType -> !inferredType.equals(object))
						.forEach(inferredType -> addInferredStatementInternal(subject, RDF.TYPE, inferredType,
								inferredContext));
			}

			sail.resolveProperties(predicate)
					.stream()
					.filter(inferredProperty -> !inferredProperty.equals(predicate))
					.filter(inferredPropery -> inferredPropery instanceof IRI)
					.map(inferredPropery -> ((IRI) inferredPropery))
					.forEach(inferredProperty -> addInferredStatementInternal(subject, inferredProperty, object,
							inferredContext));

			if (object instanceof Resource) {
				sail.resolveRangeTypes(predicate)
						.stream()
						.peek(inferredType -> {
							if (sail.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
								addInferredStatementInternal(((Resource) object), RDFS.SUBCLASSOF, RDFS.RESOURCE,
										inferredContext);
							}
						})
						.forEach(inferredType -> addInferredStatementInternal(((Resource) object), RDF.TYPE,
								inferredType, inferredContext));
			}

			sail.resolveDomainTypes(predicate)
					.stream()
					.peek(inferredType -> {
						if (sail.useAllRdfsRules && inferredType.equals(RDFS.CLASS)) {
							addInferredStatementInternal(subject, RDFS.SUBCLASSOF, RDFS.RESOURCE, inferredContext);
						}
					})
					.forEach(inferredType -> addInferredStatementInternal((subject), RDF.TYPE, inferredType,
							inferredContext));

		}

		if (actuallyAdd) {
			connection.addStatement(subject, predicate, object, context);
		}
	}

	void addAxiomStatements() {
		sail.acquireExclusiveWriteLock();

		ValueFactory vf = sail.getValueFactory();

		// This is http://www.w3.org/2000/01/rdf-schema# forward chained
		// Eg. all axioms in RDFS forward chained w.r.t. RDFS.
		// All those axioms are simply listed here

		Statement statement = vf.createStatement(RDF.ALT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.ALT, RDFS.SUBCLASSOF, RDF.ALT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.BAG, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.BAG, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.BAG, RDFS.SUBCLASSOF, RDF.BAG);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.LIST, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.LIST, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.LIST, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.LIST, RDFS.SUBCLASSOF, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PROPERTY, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PROPERTY, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PROPERTY, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SEQ, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SEQ, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDF.SEQ);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.STATEMENT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.STATEMENT, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.STATEMENT, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.STATEMENT, RDFS.SUBCLASSOF, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.DATATYPE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDF.XMLLITERAL);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.FIRST, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.FIRST, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.FIRST, RDFS.DOMAIN, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.FIRST, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.FIRST, RDFS.SUBPROPERTYOF, RDF.FIRST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.NIL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.OBJECT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.OBJECT, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.OBJECT, RDFS.DOMAIN, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.OBJECT, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.OBJECT, RDFS.SUBPROPERTYOF, RDF.OBJECT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PREDICATE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PREDICATE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PREDICATE, RDFS.DOMAIN, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PREDICATE, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.PREDICATE, RDFS.SUBPROPERTYOF, RDF.PREDICATE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.REST, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.REST, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.REST, RDFS.DOMAIN, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.REST, RDFS.RANGE, RDF.LIST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.REST, RDFS.SUBPROPERTYOF, RDF.REST);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SUBJECT, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SUBJECT, RDFS.DOMAIN, RDF.STATEMENT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SUBJECT, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.SUBJECT, RDFS.SUBPROPERTYOF, RDF.SUBJECT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.TYPE, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.TYPE, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.TYPE, RDFS.SUBPROPERTYOF, RDF.TYPE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.VALUE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.VALUE, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.VALUE, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDF.VALUE, RDFS.SUBPROPERTYOF, RDF.VALUE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CLASS, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CLASS, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CLASS, RDFS.SUBCLASSOF, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINER, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINER, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINER, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINER, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF,
				RDFS.CONTAINERMEMBERSHIPPROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DATATYPE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DATATYPE, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.DATATYPE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LITERAL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LITERAL, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LITERAL, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.COMMENT, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.COMMENT, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.COMMENT, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.COMMENT, RDFS.RANGE, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.COMMENT, RDFS.SUBPROPERTYOF, RDFS.COMMENT);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DOMAIN, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DOMAIN, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DOMAIN, RDFS.DOMAIN, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DOMAIN, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.DOMAIN, RDFS.SUBPROPERTYOF, RDFS.DOMAIN);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.ISDEFINEDBY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LABEL, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LABEL, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LABEL, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LABEL, RDFS.RANGE, RDFS.LITERAL);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.LABEL, RDFS.SUBPROPERTYOF, RDFS.LABEL);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.MEMBER, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.MEMBER, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.MEMBER, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.MEMBER, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.MEMBER, RDFS.SUBPROPERTYOF, RDFS.MEMBER);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RANGE, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RANGE, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RANGE, RDFS.DOMAIN, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RANGE, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.RANGE, RDFS.SUBPROPERTYOF, RDFS.RANGE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SEEALSO, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SEEALSO, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SEEALSO, RDFS.DOMAIN, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SEEALSO, RDFS.RANGE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SEEALSO, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.DOMAIN, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.RANGE, RDFS.CLASS);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBCLASSOF, RDFS.SUBPROPERTYOF, RDFS.SUBCLASSOF);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDF.TYPE, RDFS.RESOURCE);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDF.TYPE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.DOMAIN, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.RANGE, RDF.PROPERTY);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);
		statement = vf.createStatement(RDFS.SUBPROPERTYOF, RDFS.SUBPROPERTYOF, RDFS.SUBPROPERTYOF);
		processForSchemaCache(statement);
		addInferredStatementInternal(statement.getSubject(), statement.getPredicate(), statement.getObject(),
				DEFAULT_CONTEXT);

	}

	@Override
	public void rollback() throws SailException {

		super.rollback();

		if (statementsAdded || statementsRemoved || schemaChange) {
			sail.clearInferenceTables();
			regenerateCacheAndInferenceMaps(false);
		}

		statementsRemoved = false;
		statementsAdded = false;
		schemaChange = false;

		sail.releaseExclusiveWriteLock();
	}

	@Override
	public void begin() throws SailException {
		this.begin(sail.getDefaultIsolationLevel());
	}

	@Override
	public void begin(IsolationLevel level) throws SailException {
		addedInferredStatementsCount = 0;
		if (level == null) {
			level = sail.getDefaultIsolationLevel();
		}

		IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(level,
				sail.getSupportedIsolationLevels());
		if (compatibleLevel == null) {
			throw new UnknownSailTransactionStateException(
					"Isolation level " + level + " not compatible with this Sail");
		}
		super.begin(compatibleLevel);

		schemaChange = false;
	}

	@Override
	public void flushUpdates() throws SailException {
		logger.debug("Flush updates");
		if (statementsRemoved) {
			logger.debug("full recomputation needed, starting inferencing from scratch");
			clearInferred();
			super.flushUpdates();

			addAxiomStatements();
			super.flushUpdates();
			doInferencing();
			super.flushUpdates();
		} else if (statementsAdded) {
			super.flushUpdates();
			doInferencing();
		} else {
			super.flushUpdates();
		}

		statementsAdded = false;
		statementsRemoved = false;
	}

	// Called by base sail
	@Override
	public void statementAdded(Statement st) {
		statementsAdded = true;
	}

	// Called by base sail
	@Override
	public void statementRemoved(Statement st) {
		statementsRemoved = true;
		if (!schemaChange && isSchemaStatement(st)) {
			schemaChange = true;
		}
	}

	private boolean isSchemaStatement(Statement st) {
		final IRI predicate = st.getPredicate();
		return schemaPredicates.contains(predicate);
	}

	@Override
	public void addStatement(UpdateContext modify, Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {

		addStatement(false, subj, pred, obj, contexts);
		super.addStatement(modify, subj, pred, obj, contexts);
	}
}
