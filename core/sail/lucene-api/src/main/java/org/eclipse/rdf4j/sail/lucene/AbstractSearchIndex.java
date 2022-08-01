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
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.geo.SimpleWKTShapeParser;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lucene.util.MapOfListMaps;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public abstract class AbstractSearchIndex implements SearchIndex {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final Set<String> REJECTED_DATATYPES = new HashSet<>();

	static {
		REJECTED_DATATYPES.add("http://www.w3.org/2001/XMLSchema#float");
	}

	protected int maxDocs;

	protected Set<String> wktFields = Collections.singleton(SearchFields.getPropertyField(GEO.AS_WKT));

	private Set<String> indexedLangs;

	private Map<IRI, Set<IRI>> indexedTypeMapping;

	@Override
	public void initialize(Properties parameters) throws Exception {
		String maxDocParam = parameters.getProperty(LuceneSail.MAX_DOCUMENTS_KEY);
		maxDocs = (maxDocParam != null) ? Integer.parseInt(maxDocParam) : -1;

		String wktFieldParam = parameters.getProperty(LuceneSail.WKT_FIELDS);
		if (wktFieldParam != null) {
			wktFields = Sets.newHashSet(wktFieldParam.split("\\s+"));
		}

		if (parameters.containsKey(LuceneSail.INDEXEDLANG)) {
			String indexedlangString = parameters.getProperty(LuceneSail.INDEXEDLANG);

			indexedLangs = new HashSet<>();
			indexedLangs.addAll(Arrays.asList(indexedlangString.toLowerCase().split("\\s+")));
		}

		if (parameters.containsKey(LuceneSail.INDEXEDTYPES)) {
			String indexedtypesString = parameters.getProperty(LuceneSail.INDEXEDTYPES);
			Properties prop = new Properties();
			try {
				try (Reader reader = new StringReader(indexedtypesString)) {
					prop.load(reader);
				}
			} catch (IOException e) {
				throw new SailException("Could read " + LuceneSail.INDEXEDTYPES + ": " + indexedtypesString, e);
			}

			indexedTypeMapping = new HashMap<>();
			for (Object key : prop.keySet()) {
				String keyStr = key.toString();
				Set<IRI> objects = new HashSet<>();
				for (String obj : prop.getProperty(keyStr).split("\\s+")) {
					objects.add(vf.createIRI(obj));
				}

				IRI keyIRI;

				// special case to use the rdf:type "a"
				if (keyStr.equals("a")) {
					keyIRI = RDF.TYPE;
				} else {
					keyIRI = vf.createIRI(keyStr);
				}

				indexedTypeMapping.put(keyIRI, objects);
			}
		}
	}

	protected abstract SpatialContext getSpatialContext(String property);

	/**
	 * Returns whether the provided literal is accepted by the LuceneIndex to be indexed. It for instance does not make
	 * much since to index xsd:float.
	 *
	 * @param literal the literal to be accepted
	 * @return true if the given literal will be indexed by this LuceneIndex
	 */
	@Override
	public boolean accept(Literal literal) {
		// we reject null literals
		if (literal == null) {
			return false;
		}

		// we reject literals that are in the list of rejected data types
		if ((literal.getDatatype() != null) && (REJECTED_DATATYPES.contains(literal.getDatatype().stringValue()))) {
			return false;
		}

		// we reject literals that aren't in the list of the indexed lang
		if (indexedLangs != null
				&& (!literal.getLanguage().isPresent()
						|| !indexedLangs.contains(literal.getLanguage().get().toLowerCase()
						))) {
			return false;
		}

		return true;
	}

	@Override
	public boolean isGeoField(String fieldName) {
		return (wktFields != null) && wktFields.contains(fieldName);
	}

	@Override
	public boolean isTypeStatement(Statement statement) {
		return isTypeFilteringEnabled()
				&& statement.getObject().isIRI()
				&& indexedTypeMapping.get(statement.getPredicate()) != null;
	}

	@Override
	public boolean isTypeFilteringEnabled() {
		return indexedTypeMapping != null;
	}

	@Override
	public boolean isIndexedTypeStatement(Statement statement) {
		if (!isTypeFilteringEnabled() || !statement.getObject().isIRI()) {
			return false;
		}
		Set<IRI> objects = indexedTypeMapping.get(statement.getPredicate());
		return objects != null && objects.contains((IRI) statement.getObject());
	}

	@Override
	public Map<IRI, Set<IRI>> getIndexedTypeMapping() {
		return indexedTypeMapping;
	}

	/**
	 * Indexes the specified Statement.
	 */
	@Override
	public final synchronized void addStatement(Statement statement) throws IOException {
		// determine stuff to store
		String text = SearchFields.getLiteralPropertyValueAsString(statement);
		if (text == null) {
			return;
		}

		String field = SearchFields.getPropertyField(statement.getPredicate());

		// fetch the Document representing this Resource
		String resourceId = SearchFields.getResourceID(statement.getSubject());
		String contextId = SearchFields.getContextID(statement.getContext());

		String id = SearchFields.formIdString(resourceId, contextId);
		SearchDocument document = getDocument(id);

		if (document == null) {
			// there is no such Document: create one now
			document = newDocument(id, resourceId, contextId);
			addProperty(field, text, document);

			// add it to the index
			addDocument(document);
		} else {
			// update this Document when this triple has not been stored already
			if (!document.hasProperty(field, text)) {
				// create a copy of the old document; updating the retrieved
				// Document instance works ok for stored properties but indexed data
				// gets lost when doing an IndexWriter.updateDocument with it
				SearchDocument newDocument = copyDocument(document);

				// add the new triple to the cloned document
				addProperty(field, text, newDocument);

				// update the index with the cloned document
				updateDocument(newDocument);
			}
		}
	}

	@Override
	public final synchronized void removeStatement(Statement statement) throws IOException {
		String text = SearchFields.getLiteralPropertyValueAsString(statement);
		if (text == null) {
			return;
		}

		// fetch the Document representing this Resource
		String resourceId = SearchFields.getResourceID(statement.getSubject());
		String contextId = SearchFields.getContextID(statement.getContext());
		String id = SearchFields.formIdString(resourceId, contextId);
		SearchDocument document = getDocument(id);

		if (document != null) {
			// determine the values used in the index for this triple
			String fieldName = SearchFields.getPropertyField(statement.getPredicate());

			// see if this triple occurs in this Document
			if (document.hasProperty(fieldName, text)) {
				// if the Document only has one predicate field, we can remove the
				// document
				int nrProperties = countPropertyValues(document);
				if (nrProperties == 1) {
					deleteDocument(document);
				} else {
					// there are more triples encoded in this Document: remove the
					// document and add a new Document without this triple
					SearchDocument newDocument = newDocument(id, resourceId, contextId);
					boolean mutated = copyDocument(newDocument, document,
							Collections.singletonMap(fieldName, Collections.singleton(text)));
					if (mutated) {
						updateDocument(newDocument);
					}
				}
			}
		}
	}

	/**
	 * Add many statements at the same time, remove many statements at the same time. Ordering by resource has to be
	 * done inside this method. The passed added/removed sets are disjunct, no statement can be in both
	 *
	 * @param added   all added statements, can have multiple subjects
	 * @param removed all removed statements, can have multiple subjects
	 */
	@Override
	public final synchronized void addRemoveStatements(Collection<Statement> added, Collection<Statement> removed)
			throws IOException {
		// Buffer per resource
		MapOfListMaps<Resource, String, Statement> rsAdded = new MapOfListMaps<>();
		MapOfListMaps<Resource, String, Statement> rsRemoved = new MapOfListMaps<>();

		HashSet<Resource> resources = new HashSet<>();
		for (Statement s : added) {
			rsAdded.add(s.getSubject(), SearchFields.getContextID(s.getContext()), s);
			resources.add(s.getSubject());
		}
		for (Statement s : removed) {
			rsRemoved.add(s.getSubject(), SearchFields.getContextID(s.getContext()), s);
			resources.add(s.getSubject());
		}

		logger.debug("Removing " + removed.size() + " statements, adding " + added.size() + " statements");

		BulkUpdater updater = newBulkUpdate();
		// for each resource, add/remove
		for (Resource resource : resources) {
			Map<String, List<Statement>> stmtsToRemove = rsRemoved.get(resource);
			Map<String, List<Statement>> stmtsToAdd = rsAdded.get(resource);

			Set<String> contextsToUpdate = new HashSet<>(stmtsToAdd.keySet());
			contextsToUpdate.addAll(stmtsToRemove.keySet());

			Map<String, SearchDocument> docsByContext = new HashMap<>();
			// is the resource in the store?
			// fetch the Document representing this Resource
			String resourceId = SearchFields.getResourceID(resource);
			Iterable<? extends SearchDocument> documents = getDocuments(resourceId);

			for (SearchDocument doc : documents) {
				docsByContext.put(doc.getContext(), doc);
			}

			for (String contextId : contextsToUpdate) {
				String id = SearchFields.formIdString(resourceId, contextId);

				SearchDocument document = docsByContext.get(contextId);
				if (document == null) {
					// there are no such Documents: create one now
					document = newDocument(id, resourceId, contextId);
					// add all statements, remember the contexts
					// HashSet<Resource> contextsToAdd = new HashSet<Resource>();
					List<Statement> list = stmtsToAdd.get(contextId);
					if (list != null) {
						for (Statement s : list) {
							addProperty(s, document);
						}
					}

					// add it to the index
					updater.add(document);

					// THERE SHOULD BE NO DELETED TRIPLES ON A NEWLY ADDED RESOURCE
					if (stmtsToRemove.containsKey(contextId)) {
						logger.info(
								"Statements are marked to be removed that should not be in the store, for resource {} and context {}. Nothing done.",
								resource, contextId);
					}
				} else {
					// update the Document

					// buffer the removed literal statements
					Map<String, Set<String>> removedOfResource = null;
					{
						List<Statement> removedStatements = stmtsToRemove.get(contextId);
						if (removedStatements != null && !removedStatements.isEmpty()) {
							removedOfResource = new HashMap<>();
							for (Statement r : removedStatements) {
								String val = SearchFields.getLiteralPropertyValueAsString(r);
								if (val != null) {
									// remove value from both property field and the
									// corresponding text field
									String field = SearchFields.getPropertyField(r.getPredicate());
									Set<String> removedValues = removedOfResource.get(field);
									if (removedValues == null) {
										removedValues = new HashSet<>();
										removedOfResource.put(field, removedValues);
									}
									removedValues.add(val);
								}
							}
						}
					}

					SearchDocument newDocument = newDocument(id, resourceId, contextId);
					boolean mutated = copyDocument(newDocument, document, removedOfResource);

					// add all statements to this document, except for those which
					// are already there
					{
						List<Statement> addedToResource = stmtsToAdd.get(contextId);
						String val;
						if (addedToResource != null && !addedToResource.isEmpty()) {
							PropertyCache propertyCache = new PropertyCache(newDocument);
							for (Statement s : addedToResource) {
								val = SearchFields.getLiteralPropertyValueAsString(s);
								if (val != null) {
									String field = SearchFields.getPropertyField(s.getPredicate());
									if (!propertyCache.hasProperty(field, val)) {
										addProperty(s, newDocument);
										mutated = true;
									}
								}
							}
						}
					}

					// update the index with the cloned document, if it contains any
					// meaningful non-system properties
					int nrProperties = countPropertyValues(newDocument);
					if (nrProperties > 0) {
						if (mutated) {
							updater.update(newDocument);
						}
					} else {
						updater.delete(document);
					}
				}
			}
		}
		updater.end();
	}

	/**
	 * Creates a copy of the old document; updating the retrieved Document instance works ok for stored properties but
	 * indexed data gets lost when doing an IndexWriter.updateDocument with it.
	 */
	private boolean copyDocument(SearchDocument newDocument, SearchDocument document,
			Map<String, Set<String>> removedProperties) {
		// track if newDocument is actually different from document
		boolean mutated = false;
		for (String oldFieldName : document.getPropertyNames()) {
			newDocument.addProperty(oldFieldName);
			List<String> oldValues = document.getProperty(oldFieldName);
			if (oldValues != null) {
				// which fields were removed?
				Set<String> objectsRemoved = (removedProperties != null) ? removedProperties.get(oldFieldName) : null;
				for (String oldValue : oldValues) {
					// do not copy removed properties to the new version of the
					// document
					if ((objectsRemoved != null) && (objectsRemoved.contains(oldValue))) {
						mutated = true;
					} else {
						addProperty(oldFieldName, oldValue, newDocument);
					}
				}
			}
		}
		return mutated;
	}

	private static int countPropertyValues(SearchDocument document) {
		int numValues = 0;
		Collection<String> propertyNames = document.getPropertyNames();
		for (String propertyName : propertyNames) {
			List<String> propertyValues = document.getProperty(propertyName);
			if (propertyValues != null) {
				numValues += propertyValues.size();
			}
		}
		return numValues;
	}

	/**
	 * Add a complete Lucene Document based on these statements. Do not search for an existing document with the same
	 * subject id. (assume the existing document was deleted)
	 *
	 * @param statements the statements that make up the resource
	 * @throws IOException
	 */
	@Override
	public final synchronized void addDocuments(Resource subject, List<Statement> statements) throws IOException {

		String resourceId = SearchFields.getResourceID(subject);

		SetMultimap<String, Statement> stmtsByContextId = HashMultimap.create();

		String contextId;
		for (Statement statement : statements) {
			contextId = SearchFields.getContextID(statement.getContext());

			stmtsByContextId.put(contextId, statement);
		}

		BulkUpdater batch = newBulkUpdate();
		for (Entry<String, Collection<Statement>> entry : stmtsByContextId.asMap().entrySet()) {
			// create a new document
			String id = SearchFields.formIdString(resourceId, entry.getKey());
			SearchDocument document = newDocument(id, resourceId, entry.getKey());

			for (Statement stmt : entry.getValue()) {
				// determine stuff to store
				addProperty(stmt, document);
			}
			// add it to the index
			batch.add(document);
		}
		batch.end();
	}

	/**
	 * check if the passed statement should be added (is it indexed? is it stored?) and add it as predicate to the
	 * passed document. No checks whether the predicate was already there.
	 *
	 * @param statement the statement to add
	 * @param document  the document to add to
	 */
	private void addProperty(Statement statement, SearchDocument document) {
		String value = SearchFields.getLiteralPropertyValueAsString(statement);
		if (value == null) {
			return;
		}
		String field = SearchFields.getPropertyField(statement.getPredicate());
		addProperty(field, value, document);
	}

	private void addProperty(String field, String value, SearchDocument document) {
		if (isGeoField(field)) {
			document.addGeoProperty(field, value);
		} else {
			document.addProperty(field, value);
		}
	}

	@Override
	public final Collection<BindingSet> evaluate(SearchQueryEvaluator evaluator) throws SailException {
		if (evaluator instanceof QuerySpec) {
			QuerySpec query = (QuerySpec) evaluator;
			Iterable<? extends DocumentScore> result = evaluateQuery(query);
			return generateBindingSets(query, result);
		} else if (evaluator instanceof DistanceQuerySpec) {
			DistanceQuerySpec query = (DistanceQuerySpec) evaluator;
			Iterable<? extends DocumentDistance> result = evaluateQuery(query);
			return generateBindingSets(query, result);
		} else if (evaluator instanceof GeoRelationQuerySpec) {
			GeoRelationQuerySpec query = (GeoRelationQuerySpec) evaluator;
			Iterable<? extends DocumentResult> result = evaluateQuery(query);
			return generateBindingSets(query, result);
		} else {
			throw new IllegalArgumentException("Unsupported " + SearchQueryEvaluator.class.getSimpleName() + ": "
					+ evaluator.getClass().getName());
		}
	}

	/**
	 * Evaluates one Lucene Query. It distinguishes between two cases, the one where no subject is given and the one
	 * were it is given.
	 *
	 * @param query the Lucene query to evaluate
	 * @return QueryResult consisting of hits and highlighter
	 */
	private Iterable<? extends DocumentScore> evaluateQuery(QuerySpec query) {
		Iterable<? extends DocumentScore> hits = null;

		try {
			// parse the query string to a lucene query

			String sQuery = query.getQueryString();

			if (!sQuery.isEmpty()) {
				// if the query requests for the snippet, create a highlighter using
				// this query
				boolean highlight = (query.getSnippetVariableName() != null || query.getPropertyVariableName() != null);

				// distinguish the two cases of subject == null
				hits = query(query.getSubject(), query.getQueryString(), query.getPropertyURI(), highlight);
			} else {
				hits = null;
			}
		} catch (Exception e) {
			logger.error("There was a problem evaluating query '" + query.getQueryString() + "' for property '"
					+ query.getPropertyURI() + "!", e);
		}

		return hits;
	}

	/**
	 * This method generates bindings from the given result of a Lucene query.
	 *
	 * @param query the Lucene query
	 * @return a LinkedHashSet containing generated bindings
	 * @throws SailException
	 */
	private Collection<BindingSet> generateBindingSets(QuerySpec query, Iterable<? extends DocumentScore> hits)
			throws SailException {
		// Since one resource can be returned many times, it can lead now to
		// multiple occurrences
		// of the same binding tuple in the BINDINGS clause. This in turn leads to
		// duplicate answers in the original SPARQL query.
		// We want to avoid this, so BindingSets added to the result must be
		// unique.
		LinkedHashSet<BindingSet> bindingSets = new LinkedHashSet<>();

		Set<String> bindingNames = new HashSet<>();
		final String matchVar = query.getMatchesVariableName();
		if (matchVar != null) {
			bindingNames.add(matchVar);
		}
		final String scoreVar = query.getScoreVariableName();
		if (scoreVar != null) {
			bindingNames.add(scoreVar);
		}
		final String snippetVar = query.getSnippetVariableName();
		if (snippetVar != null) {
			bindingNames.add(snippetVar);
		}
		final String propertyVar = query.getPropertyVariableName();
		if (propertyVar != null && query.getPropertyURI() == null) {
			bindingNames.add(propertyVar);
		}

		if (hits != null) {
			// for each hit ...
			for (DocumentScore hit : hits) {
				// this takes the new bindings
				QueryBindingSet derivedBindings = new QueryBindingSet();

				// get the current hit
				SearchDocument doc = hit.getDocument();
				if (doc == null) {
					continue;
				}

				// get the score of the hit
				float score = hit.getScore();

				// bind the respective variables
				if (matchVar != null) {
					Resource resource = getResource(doc);
					derivedBindings.addBinding(matchVar, resource);
				}

				if ((scoreVar != null) && (score > 0.0f)) {
					derivedBindings.addBinding(scoreVar, SearchFields.scoreToLiteral(score));
				}

				if (snippetVar != null || propertyVar != null) {
					if (hit.isHighlighted()) {
						// limit to the queried field, if there was one
						Collection<String> fields;
						if (query.getPropertyURI() != null) {
							String fieldname = SearchFields.getPropertyField(query.getPropertyURI());
							fields = Collections.singleton(fieldname);
						} else {
							fields = doc.getPropertyNames();
						}

						// extract snippets from Lucene's query results
						for (String field : fields) {
							Iterable<String> snippets = hit.getSnippets(field);
							if (snippets != null) {
								for (String snippet : snippets) {
									if (snippet != null && !snippet.isEmpty()) {
										// create an individual binding set for each
										// snippet
										QueryBindingSet snippetBindings = new QueryBindingSet(derivedBindings);

										if (snippetVar != null) {
											snippetBindings.addBinding(snippetVar, vf.createLiteral(snippet));
										}

										if (propertyVar != null && query.getPropertyURI() == null) {
											snippetBindings.addBinding(propertyVar, vf.createIRI(field));
										}

										bindingSets.add(snippetBindings);
									}
								}
							}
						}
					} else {
						logger.warn(
								"Lucene Query requests snippet, but no highlighter was generated for it, no snippets will be generated!\n{}",
								query);
						bindingSets.add(derivedBindings);
					}
				} else {
					bindingSets.add(derivedBindings);
				}
			}
		}

		// we succeeded
		return new BindingSetCollection(bindingNames, bindingSets);
	}

	private Iterable<? extends DocumentDistance> evaluateQuery(DistanceQuerySpec query) {
		Iterable<? extends DocumentDistance> hits = null;

		Literal from = query.getFrom();
		double distance = query.getDistance();
		IRI units = query.getUnits();
		IRI geoProperty = query.getGeoProperty();
		try {
			if (!GEO.WKT_LITERAL.equals(from.getDatatype())) {
				throw new MalformedQueryException("Unsupported datatype: " + from.getDatatype());
			}
			Shape shape = parseQueryPoint(SearchFields.getPropertyField(geoProperty), from.getLabel());
			if (!(shape instanceof Point)) {
				throw new MalformedQueryException("Geometry literal is not a point: " + from.getLabel());
			}
			Point p = (Point) shape;
			hits = geoQuery(geoProperty, p, units, distance, query.getDistanceVar(), query.getContextVar());
		} catch (Exception e) {
			logger.error("There was a problem evaluating distance query 'within " + distance + getUnitSymbol(units)
					+ " of " + from.getLabel() + "'!", e);
		}

		return hits;
	}

	private static String getUnitSymbol(IRI units) {
		if (GEOF.UOM_METRE.equals(units)) {
			return "m";
		} else {
			return "";
		}
	}

	private Collection<BindingSet> generateBindingSets(DistanceQuerySpec query,
			Iterable<? extends DocumentDistance> hits) throws SailException {
		// Since one resource can be returned many times, it can lead now to
		// multiple occurrences
		// of the same binding tuple in the BINDINGS clause. This in turn leads to
		// duplicate answers in the original SPARQL query.
		// We want to avoid this, so BindingSets added to the result must be
		// unique.
		LinkedHashSet<BindingSet> bindingSets = new LinkedHashSet<>();

		Set<String> bindingNames = new HashSet<>();
		final String subjVar = query.getSubjectVar();
		if (subjVar != null) {
			bindingNames.add(subjVar);
		}
		final String geoVar = query.getGeoVar();
		if (geoVar != null) {
			bindingNames.add(geoVar);
		}
		final String distanceVar = query.getDistanceVar();
		if (distanceVar != null) {
			bindingNames.add(distanceVar);
		}
		final Var contextVar = query.getContextVar();
		if (contextVar != null && !contextVar.hasValue()) {
			bindingNames.add(contextVar.getName());
		}

		if (hits != null) {
			double maxDistance = query.getDistance();
			// for each hit ...
			for (DocumentDistance hit : hits) {
				// get the current hit
				SearchDocument doc = hit.getDocument();
				if (doc == null) {
					continue;
				}

				List<String> geometries = doc.getProperty(SearchFields.getPropertyField(query.getGeoProperty()));
				for (String geometry : geometries) {
					double distance = hit.getDistance();
					// Distance queries are generally implemented by checking
					// if indexed points intersect with a bounding disc.
					// Unfortunately, this means the results may potentially also
					// include other indexed shapes that intersect with the disc.
					// The distances assigned to these other shapes may well be
					// greater than the original bounding distance.
					// We could exclude such results by checking if the shapes are
					// points,
					// but instead we do a faster sanity check of the distance.
					// This has the potential (desirable?) side-effect of extending
					// the distance function
					// to arbitrary shapes.
					if (distance < maxDistance) {
						QueryBindingSet derivedBindings = new QueryBindingSet();
						if (subjVar != null) {
							Resource resource = getResource(doc);
							derivedBindings.addBinding(subjVar, resource);
						}
						if (contextVar != null && !contextVar.hasValue()) {
							Resource ctx = SearchFields.createContext(doc.getContext());
							if (ctx != null) {
								derivedBindings.addBinding(contextVar.getName(), ctx);
							}
						}
						if (geoVar != null) {
							derivedBindings.addBinding(geoVar, SearchFields.wktToLiteral(geometry));
						}
						if (distanceVar != null) {
							derivedBindings.addBinding(distanceVar, SearchFields.distanceToLiteral(distance));
						}

						bindingSets.add(derivedBindings);
					}
				}
			}
		}

		// we succeeded
		return new BindingSetCollection(bindingNames, bindingSets);
	}

	private Iterable<? extends DocumentResult> evaluateQuery(GeoRelationQuerySpec query) {
		Iterable<? extends DocumentResult> hits = null;

		Literal qgeom = query.getQueryGeometry();
		IRI geoProperty = query.getGeoProperty();
		try {
			if (!GEO.WKT_LITERAL.equals(qgeom.getDatatype())) {
				throw new MalformedQueryException("Unsupported datatype: " + qgeom.getDatatype());
			}
			hits = geoRelationQuery(query.getRelation(), geoProperty, qgeom.getLabel(), query.getContextVar());
		} catch (Exception e) {
			logger.error("There was a problem evaluating spatial relation query '" + query.getRelation() + " "
					+ qgeom.getLabel() + "'!", e);
		}

		return hits;
	}

	private Collection<BindingSet> generateBindingSets(GeoRelationQuerySpec query,
			Iterable<? extends DocumentResult> hits) throws SailException {
		// Since one resource can be returned many times, it can lead now to
		// multiple occurrences
		// of the same binding tuple in the BINDINGS clause. This in turn leads to
		// duplicate answers in the original SPARQL query.
		// We want to avoid this, so BindingSets added to the result must be
		// unique.
		LinkedHashSet<BindingSet> bindingSets = new LinkedHashSet<>();

		Set<String> bindingNames = new HashSet<>();
		final String subjVar = query.getSubjectVar();
		if (subjVar != null) {
			bindingNames.add(subjVar);
		}
		final String geoVar = query.getGeoVar();
		if (geoVar != null) {
			bindingNames.add(geoVar);
		}
		final String fVar = query.getFunctionValueVar();
		if (fVar != null) {
			bindingNames.add(fVar);
		}
		final Var contextVar = query.getContextVar();
		if (contextVar != null && !contextVar.hasValue()) {
			bindingNames.add(contextVar.getName());
		}

		if (hits != null) {
			// for each hit ...
			for (DocumentResult hit : hits) {
				// get the current hit
				SearchDocument doc = hit.getDocument();
				if (doc == null) {
					continue;
				}

				List<String> geometries = doc.getProperty(SearchFields.getPropertyField(query.getGeoProperty()));
				for (String geometry : geometries) {
					QueryBindingSet derivedBindings = new QueryBindingSet();
					if (subjVar != null) {
						Resource resource = getResource(doc);
						derivedBindings.addBinding(subjVar, resource);
					}
					if (contextVar != null && !contextVar.hasValue()) {
						Resource ctx = SearchFields.createContext(doc.getContext());
						if (ctx != null) {
							derivedBindings.addBinding(contextVar.getName(), ctx);
						}
					}
					if (geoVar != null) {
						derivedBindings.addBinding(geoVar, SearchFields.wktToLiteral(geometry));
					}
					if (fVar != null) {
						derivedBindings.addBinding(fVar, BooleanLiteral.TRUE);
					}

					bindingSets.add(derivedBindings);
				}
			}
		}

		// we succeeded
		return new BindingSetCollection(bindingNames, bindingSets);
	}

	protected Object parseLuceneQueryShape(String property, String value) throws ParseException, IOException {
		return SimpleWKTShapeParser.parse(value);
	}

	protected Shape parseQueryShape(String property, String value) throws ParseException {
		return getSpatialContext(property).readShapeFromWkt(value);
	}

	protected Shape parseQueryPoint(String property, String value) throws ParseException {
		return getSpatialContext(property).readShapeFromWkt(value);
	}

	/**
	 * Returns the Resource corresponding with the specified Document.
	 */
	protected Resource getResource(SearchDocument document) {
		return SearchFields.createResource(document.getResource());
	}

	protected abstract SearchDocument getDocument(String id) throws IOException;

	protected abstract Iterable<? extends SearchDocument> getDocuments(String resourceId) throws IOException;

	protected abstract SearchDocument newDocument(String id, String resourceId, String context);

	protected abstract SearchDocument copyDocument(SearchDocument doc);

	protected abstract void addDocument(SearchDocument doc) throws IOException;

	protected abstract void updateDocument(SearchDocument doc) throws IOException;

	protected abstract void deleteDocument(SearchDocument doc) throws IOException;

	protected abstract Iterable<? extends DocumentScore> query(Resource subject, String q, IRI property,
			boolean highlight) throws MalformedQueryException, IOException;

	protected abstract Iterable<? extends DocumentDistance> geoQuery(IRI geoProperty, Point p, IRI units,
			double distance, String distanceVar, Var context) throws MalformedQueryException, IOException;

	protected abstract Iterable<? extends DocumentResult> geoRelationQuery(String relation, IRI geoProperty,
			String wkt, Var context) throws MalformedQueryException, IOException;

	protected abstract BulkUpdater newBulkUpdate();
}
