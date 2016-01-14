/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene3;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.function.CustomScoreProvider;
import org.apache.lucene.search.function.CustomScoreQuery;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.spatial.geohash.GeoHashDistanceFilter;
import org.apache.lucene.spatial.tier.DistanceFilter;
import org.apache.lucene.spatial.tier.FixedCartesianPolyFilterBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneIndex;
import org.eclipse.rdf4j.sail.lucene.AbstractReaderMonitor;
import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.DocumentResult;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.SearchQuery;
import org.eclipse.rdf4j.sail.lucene.SimpleBulkUpdater;
import org.eclipse.rdf4j.sail.lucene.util.GeoUnits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.SpatialContextFactory;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

/**
 * A LuceneIndex is a one-stop-shop abstraction of a Lucene index. It takes care
 * of proper synchronization of IndexReaders, IndexWriters and IndexSearchers in
 * a way that is suitable for a LuceneSail.
 * 
 * @see LuceneSail
 */
public class LuceneIndex extends AbstractLuceneIndex {

	static {
		// do NOT set this to Integer.MAX_VALUE, because this breaks fuzzy queries
		BooleanQuery.setMaxClauseCount(1024 * 1024);
	}

	public static final String GEOHASH_FIELD_PREFIX = "_geohash_";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * The Directory that holds the Lucene index files.
	 */
	private Directory directory;

	/**
	 * The Analyzer used to tokenize strings and queries.
	 */
	private Analyzer analyzer;

	private Analyzer queryAnalyzer;

	/**
	 * The IndexWriter that can be used to alter the index' contents. Created
	 * lazily.
	 */
	private IndexWriter indexWriter;

	/**
	 * This holds IndexReader and IndexSearcher.
	 */
	protected ReaderMonitor currentMonitor;

	private Function<? super String,? extends SpatialStrategy> geoStrategyMapper;

	public LuceneIndex() {
	}

	/**
	 * Creates a new LuceneIndex.
	 * 
	 * @param directory
	 *        The Directory in which an index can be found and/or in which index
	 *        files are written.
	 * @param analyzer
	 *        The Analyzer that will be used for tokenizing strings to index and
	 *        queries.
	 * @throws IOException
	 *         When the Directory could not be unlocked.
	 */
	public LuceneIndex(Directory directory, Analyzer analyzer)
		throws IOException
	{
		this.directory = directory;
		this.analyzer = analyzer;
		this.geoStrategyMapper = createSpatialStrategyMapper(Collections.<String, String>emptyMap());

		postInit();
	}

	@Override
	public void initialize(Properties parameters)
		throws Exception
	{
		super.initialize(parameters);
		this.directory = createDirectory(parameters);
		this.analyzer = createAnalyzer(parameters);
		// slightly hacky cast to cope with the fact that Properties is
		// Map<Object,Object>
		// even though it is effectively Map<String,String>
		this.geoStrategyMapper = createSpatialStrategyMapper((Map<String, String>)(Map<?, ?>)parameters);

		postInit();
	}

	protected Directory createDirectory(Properties parameters)
		throws IOException
	{
		Directory dir;
		if (parameters.containsKey(LuceneSail.LUCENE_DIR_KEY)) {
			dir = FSDirectory.open(new File(parameters.getProperty(LuceneSail.LUCENE_DIR_KEY)));
		}
		else if (parameters.containsKey(LuceneSail.LUCENE_RAMDIR_KEY)
				&& "true".equals(parameters.getProperty(LuceneSail.LUCENE_RAMDIR_KEY)))
		{
			dir = new RAMDirectory();
		}
		else {
			throw new IOException("No luceneIndex set, and no '" + LuceneSail.LUCENE_DIR_KEY + "' or '"
					+ LuceneSail.LUCENE_RAMDIR_KEY + "' parameter given. ");
		}
		return dir;
	}

	protected Analyzer createAnalyzer(Properties parameters)
		throws Exception
	{
		Analyzer analyzer;
		if (parameters.containsKey(LuceneSail.ANALYZER_CLASS_KEY)) {
			analyzer = (Analyzer)Class.forName(parameters.getProperty(LuceneSail.ANALYZER_CLASS_KEY)).newInstance();
		}
		else {
			analyzer = new StandardAnalyzer(Version.LUCENE_35);
		}
		return analyzer;
	}

	private void postInit()
		throws IOException
	{
		this.queryAnalyzer = new StandardAnalyzer(Version.LUCENE_35);

		// get rid of any locks that may have been left by previous (crashed)
		// sessions
		if (IndexWriter.isLocked(directory)) {
			logger.info("unlocking directory {}", directory);
			IndexWriter.unlock(directory);
		}

		// do some initialization for new indices
		if (!IndexReader.indexExists(directory)) {
			logger.info("creating new Lucene index in directory {}", directory);
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			indexWriterConfig.setOpenMode(OpenMode.CREATE);
			IndexWriter writer = new IndexWriter(directory, indexWriterConfig);
			writer.close();
		}
	}

	protected Function<String, ? extends SpatialStrategy> createSpatialStrategyMapper(Map<String,String> parameters) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final SpatialContext geoContext = SpatialContextFactory.makeSpatialContext(parameters, classLoader);
		String minMileProp = parameters.get("minMiles");
		String maxMileProp = parameters.get("maxMiles");
		final int maxTier = (minMileProp != null) ? SpatialStrategy.getTier(Double.parseDouble(minMileProp))
				: SpatialStrategy.DEFAULT_MAX_TIER;
		final int minTier = (maxMileProp != null) ? SpatialStrategy.getTier(Double.parseDouble(maxMileProp))
				: SpatialStrategy.DEFAULT_MIN_TIER;

		return new Function<String,SpatialStrategy>()
		{
			@Override
			public SpatialStrategy apply(String field) {
				return new SpatialStrategy(field, minTier, maxTier, geoContext);
			}
		};
	}

	@Override
	protected SpatialContext getSpatialContext(String property) {
		return geoStrategyMapper.apply(property).getSpatialContext();
	}

	// //////////////////////////////// Setters and getters

	public Directory getDirectory() {
		return directory;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public Function<? super String, ? extends SpatialStrategy> getSpatialStrategyMapper() {
		return geoStrategyMapper;
	}

	// //////////////////////////////// Methods for controlled index access
	// For quick'n'easy access to reader, the indexreader is returned directly
	// result LuceneQueryIterators use the more elaborate
	// ReaderMonitor directly to be able to close the reader when they
	// are done.

	public IndexReader getIndexReader()
		throws IOException
	{
		return getIndexSearcher().getIndexReader();
	}

	public IndexSearcher getIndexSearcher()
		throws IOException
	{
		return getCurrentMonitor().getIndexSearcher();
	}

	/**
	 * Current monitor holds instance of IndexReader and IndexSearcher It is used
	 * to keep track of readers
	 */
	@Override
	public ReaderMonitor getCurrentMonitor() {
		if (currentMonitor == null)
			currentMonitor = new ReaderMonitor(this, directory);
		return currentMonitor;
	}

	public IndexWriter getIndexWriter()
		throws IOException
	{

		if (indexWriter == null) {
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			indexWriter = new IndexWriter(directory, indexWriterConfig);
		}
		return indexWriter;
	}

	@Override
	public void shutDown()
		throws IOException
	{
		// try-finally setup ensures that closing of an instance is not skipped
		// when an earlier instance resulted in an IOException
		// FIXME: is there a more elegant way to ensure this?

		// This close oldMonitors which hold InderReader and IndexSeracher
		// Monitor close IndexReader and IndexSearcher
		if (currentMonitor != null) {
			currentMonitor.close();
			currentMonitor = null;
		}
		if (oldmonitors.size() > 0) {
			logger.warn(
					"LuceneSail: On shutdown {} IndexReaders were not closed. This is due to non-closed Query Iterators, which must be closed!",
					oldmonitors.size());
		}
		for (AbstractReaderMonitor monitor : oldmonitors) {
			monitor.close();
		}
		oldmonitors.clear();

		try {
			if (indexWriter != null) {
				indexWriter.close();
			}
		}
		finally {
			indexWriter = null;
		}
	}

	// //////////////////////////////// Methods for updating the index

	@Override
	protected SearchDocument getDocument(String id)
		throws IOException
	{
		Document document = getDocument(idTerm(id));
		return (document != null) ? new LuceneDocument(document, geoStrategyMapper) : null;
	}

	@Override
	protected Iterable<? extends SearchDocument> getDocuments(String resourceId)
		throws IOException
	{
		List<Document> docs = getDocuments(new Term(SearchFields.URI_FIELD_NAME, resourceId));
		return Iterables.transform(docs, new Function<Document, SearchDocument>() {

			@Override
			public SearchDocument apply(Document doc) {
				return new LuceneDocument(doc, geoStrategyMapper);
			}
		});
	}

	@Override
	protected SearchDocument newDocument(String id, String resourceId, String context) {
		return new LuceneDocument(id, resourceId, context, geoStrategyMapper);
	}

	@Override
	protected SearchDocument copyDocument(SearchDocument doc) {
		Document document = ((LuceneDocument)doc).getDocument();
		Document newDocument = new Document();

		// add all existing fields (including id, uri, context, and text)
		for (Fieldable oldField : document.getFields()) {
			newDocument.add(oldField);
		}
		return new LuceneDocument(newDocument, geoStrategyMapper);
	}

	@Override
	protected void addDocument(SearchDocument doc)
		throws IOException
	{
		getIndexWriter().addDocument(((LuceneDocument)doc).getDocument());
	}

	@Override
	protected void updateDocument(SearchDocument doc)
		throws IOException
	{
		getIndexWriter().updateDocument(idTerm(doc.getId()), ((LuceneDocument)doc).getDocument());
	}

	@Override
	protected void deleteDocument(SearchDocument doc)
		throws IOException
	{
		getIndexWriter().deleteDocuments(idTerm(doc.getId()));
	}

	@Override
	protected BulkUpdater newBulkUpdate() {
		return new SimpleBulkUpdater(this);
	}

	private Term idTerm(String id) {
		return new Term(SearchFields.ID_FIELD_NAME, id);
	}

	/**
	 * Returns a Document representing the specified document ID (combination of
	 * resource and context), or null when no such Document exists yet.
	 */
	private Document getDocument(Term idTerm)
		throws IOException
	{
		IndexReader reader = getIndexReader();
		TermDocs termDocs = reader.termDocs(idTerm);

		try {
			if (termDocs.next()) {
				// return the Document and make sure there are no others
				int docNr = termDocs.doc();
				if (termDocs.next()) {
					throw new IOException("Multiple Documents for resource " + idTerm.text());
				}

				return reader.document(docNr);
			}
			else {
				// no such Document
				return null;
			}
		}
		finally {
			termDocs.close();
		}
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty
	 * when no such Document exists yet). Each document represent a set of
	 * statements with the specified Resource as a subject, which are stored in a
	 * specific context
	 */
	private List<Document> getDocuments(Term uriTerm)
		throws IOException
	{
		List<Document> result = new ArrayList<Document>();

		IndexReader reader = getIndexReader();
		TermDocs termDocs = reader.termDocs(uriTerm);

		try {
			while (termDocs.next()) {
				int docNr = termDocs.doc();
				result.add(reader.document(docNr));
			}
		}
		finally {
			termDocs.close();
		}

		return result;
	}

	/**
	 * Returns a Document representing the specified Resource & Context
	 * combination, or null when no such Document exists yet.
	 */
	public Document getDocument(Resource subject, Resource context)
		throws IOException
	{
		// fetch the Document representing this Resource
		String resourceId = SearchFields.getResourceID(subject);
		String contextId = SearchFields.getContextID(context);
		Term idTerm = new Term(SearchFields.ID_FIELD_NAME, SearchFields.formIdString(resourceId, contextId));
		return getDocument(idTerm);
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty
	 * when no such Document exists yet). Each document represent a set of
	 * statements with the specified Resource as a subject, which are stored in a
	 * specific context
	 */
	public List<Document> getDocuments(Resource subject)
		throws IOException
	{
		String resourceId = SearchFields.getResourceID(subject);
		Term uriTerm = new Term(SearchFields.URI_FIELD_NAME, resourceId);
		return getDocuments(uriTerm);
	}

	/**
	 * Stores and indexes an ID in a Document.
	 */
	public static void addIDField(String id, Document document) {
		document.add(new Field(SearchFields.ID_FIELD_NAME, id, Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS));
	}

	/**
	 * Add the "context" value to the doc
	 * 
	 * @param context
	 *        the context or null, if null-context
	 * @param document
	 *        the document
	 * @param ifNotExists
	 *        check if this context exists
	 */
	public static void addContextField(String context, Document document) {
		if (context != null) {
			document.add(new Field(SearchFields.CONTEXT_FIELD_NAME, context, Field.Store.YES,
					Field.Index.NOT_ANALYZED_NO_NORMS));
		}
	}

	/**
	 * Stores and indexes the resource ID in a Document.
	 */
	public static void addResourceField(String resourceId, Document document) {
		document.add(new Field(SearchFields.URI_FIELD_NAME, resourceId, Field.Store.YES,
				Field.Index.NOT_ANALYZED_NO_NORMS));
	}

	public static void addPredicateField(String predicate, String text, Document document) {
		// store this predicate
		addPredicateField(predicate, text, document, Field.Index.ANALYZED);
	}

	public static void addStoredOnlyPredicateField(String predicate, String text, Document document) {
		// store this predicate
		addPredicateField(predicate, text, document, Field.Index.NO);
	}

	private static void addPredicateField(String predicate, String text, Document document,
			Field.Index indexOptions)
	{
		// store this predicate
		document.add(new Field(predicate, text, Field.Store.YES, indexOptions));
	}

	public static void addTextField(String text, Document document) {
		// and in TEXT_FIELD_NAME
		document.add(new Field(SearchFields.TEXT_FIELD_NAME, text, Field.Store.YES, Field.Index.ANALYZED));
	}

	/**
	 * invalidate readers, free them if possible (readers that are still open by
	 * a {@link LuceneQueryConnection} will not be closed. Synchronized on
	 * oldmonitors because it manipulates them
	 * 
	 * @throws IOException
	 */
	private void invalidateReaders()
		throws IOException
	{
		synchronized (oldmonitors) {
			// Move current monitor to old monitors and set null
			if (currentMonitor != null)
				// we do NOT close it directly as it may be used by an open result
				// iterator, hence moving it to the
				// list of oldmonitors where it is handled as other older monitors
				oldmonitors.add(currentMonitor);
			currentMonitor = null;

			// close all monitors if possible
			for (Iterator<AbstractReaderMonitor> i = oldmonitors.iterator(); i.hasNext();) {
				AbstractReaderMonitor monitor = i.next();
				if (monitor.closeWhenPossible()) {
					i.remove();
				}
			}

			// check if all readers were closed
			if (oldmonitors.isEmpty()) {
				logger.debug("Deleting unused files from Lucene index");

				// clean up unused files (marked as 'deletable' in Luke Filewalker)
				getIndexWriter().deleteUnusedFiles();

				// logIndexStats();
			}
		}
	}

	private void logIndexStats() {
		try {
			IndexReader reader = null;
			try {
				reader = getIndexReader();

				Document doc;
				int totalFields = 0;

				Set<String> ids = new HashSet<String>();
				String[] idArray;
				int count = 0;
				for (int i = 0; i < reader.maxDoc(); i++) {
					if (isDeleted(reader, i))
						continue;
					doc = readDocument(reader, i, null);
					totalFields += doc.getFields().size();
					count++;
					idArray = doc.getValues("id");
					for (String id : idArray)
						ids.add(id);

				}

				logger.info("Total documents in the index: " + reader.numDocs()
						+ ", number of deletable documents in the index: " + reader.numDeletedDocs()
						+ ", valid documents: " + count + ", total fields in all documents: " + totalFields
						+ ", average number of fields per document: " + ((double)totalFields) / reader.numDocs());
				logger.info("Distinct ids in the index: " + ids.size());

			}
			finally {
				if (currentMonitor != null) {
					currentMonitor.closeWhenPossible();
					currentMonitor = null;
				}
			}
		}
		catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}

	}

	@Override
	public void begin()
		throws IOException
	{
		// nothing to do
	}

	/**
	 * Commits any changes done to the LuceneIndex since the last commit. The
	 * semantics is synchronous to SailConnection.commit(), i.e. the LuceneIndex
	 * should be committed/rollbacked whenever the LuceneSailConnection is
	 * committed/rollbacked.
	 */
	@Override
	public void commit()
		throws IOException
	{
		getIndexWriter().commit();
		// the old IndexReaders/Searchers are not outdated
		invalidateReaders();
	}

	@Override
	public void rollback()
		throws IOException
	{
		getIndexWriter().rollback();
	}

	// //////////////////////////////// Methods for querying the index

	/**
	 * Parse the passed query.
	 * To be removed, no longer used.
	 * @param query
	 *        string
	 * @return the parsed query
	 * @throws ParseException
	 *         when the parsing brakes
	 */
	@Override
	@Deprecated
	protected SearchQuery parseQuery(String query, URI propertyURI) throws MalformedQueryException
	{
		Query q;
		try {
			q = getQueryParser(propertyURI).parse(query);
		}
		catch (ParseException e) {
			throw new MalformedQueryException(e);
		}
		return new LuceneQuery(q, this);
	}

	/**
	 * Parse the passed query.
	 * 
	 * @param query
	 *        string
	 * @return the parsed query
	 * @throws ParseException
	 *         when the parsing brakes
	 */
	@Override
	protected Iterable<? extends DocumentScore> query(Resource subject, String query, URI propertyURI,
			boolean highlight)
		throws MalformedQueryException, IOException
	{
		Query q;
		try {
			q = getQueryParser(propertyURI).parse(query);
		}
		catch (ParseException e) {
			throw new MalformedQueryException(e);
		}

		final Highlighter highlighter;
		if (highlight) {
			Formatter formatter = new SimpleHTMLFormatter(SearchFields.HIGHLIGHTER_PRE_TAG,
					SearchFields.HIGHLIGHTER_POST_TAG);
			highlighter = new Highlighter(formatter, new QueryScorer(q));
		}
		else {
			highlighter = null;
		}

		TopDocs docs;
		if (subject != null) {
			docs = search(subject, q);
		}
		else {
			docs = search(q);
		}
		return Iterables.transform(Arrays.asList(docs.scoreDocs), new Function<ScoreDoc, DocumentScore>() {

			@Override
			public DocumentScore apply(ScoreDoc doc) {
				return new LuceneDocumentScore(doc, highlighter, LuceneIndex.this);
			}
		});
	}

	@Override
	protected Iterable<? extends DocumentDistance> geoQuery(final URI geoProperty,
			Point p, final URI units, double distance, String distanceVar, Var contextVar)
		throws MalformedQueryException, IOException
	{
		final double miles = GeoUnits.toMiles(distance, units);
		double lon = p.getX();
		double lat = p.getY();

		final String geoField = SearchFields.getPropertyField(geoProperty);
		SpatialStrategy tiers = getSpatialStrategyMapper().apply(geoField);
		FixedCartesianPolyFilterBuilder cpf = new FixedCartesianPolyFilterBuilder(tiers.getFieldPrefix(),
				tiers.getMinTier(), tiers.getMaxTier());
		Filter cartesianFilter = cpf.getBoundingArea(lat, lon, miles);
		final DistanceFilter distanceFilter = new GeoHashDistanceFilter(cartesianFilter, lat, lon, miles,
				GEOHASH_FIELD_PREFIX + geoField);

		Query q;
		if(contextVar != null) {
			Resource ctx = (Resource) contextVar.getValue();
			q = new TermQuery(new Term(SearchFields.CONTEXT_FIELD_NAME,
					SearchFields.getContextID(ctx)));
			if(ctx == null) {
				BooleanQuery notQuery = new BooleanQuery();
				notQuery.add(new MatchAllDocsQuery(), Occur.MUST); // required for negation
				notQuery.add(q, Occur.MUST_NOT);
				q = notQuery;
			}
		}
		else {
			q = new MatchAllDocsQuery();
		}
		CustomScoreQuery customScore = new CustomScoreQuery(q) {
			private static final long serialVersionUID = -4637297669739149113L;

			@Override
			protected CustomScoreProvider getCustomScoreProvider(IndexReader reader) {
				return new CustomScoreProvider(reader) {

					@Override
					public float customScore(int doc, float subQueryScore, float valSrcScore) {
						Double distance = distanceFilter.getDistance(doc);
						if (distance == null) {
							return 0.0f;
						}
						// we normalise the score between 0 and 1
						float score = (float)(1.0 - distance / miles);
						return score;
					}
				};
			}
		};

		TopDocs docs = getIndexSearcher().search(customScore, distanceFilter, getMaxDocs());
		final boolean requireContext = (contextVar != null && !contextVar.hasValue());
		return Iterables.transform(Arrays.asList(docs.scoreDocs), new Function<ScoreDoc, DocumentDistance>() {

			@Override
			public DocumentDistance apply(ScoreDoc doc) {
				return new LuceneDocumentDistance(doc, geoField, units, distanceFilter, requireContext, LuceneIndex.this);
			}
		});
	}

	@Override
	protected Iterable<? extends DocumentResult> geoRelationQuery(String relation,
			URI geoProperty, Shape shape, Var contextVar)
		throws MalformedQueryException, IOException
	{
		// not supported
		return null;
	}

	/**
	 * Returns the lucene hit with the given id of the respective lucene query
	 * 
	 * @param id
	 *        the id of the document to return
	 * @return the requested hit, or null if it fails
	 */
	public Document getDocument(int docId, Set<String> fieldsToLoad) {
		try {
			return readDocument(getIndexReader(), docId, fieldsToLoad);
		}
		catch (CorruptIndexException e) {
			logger.error("The index seems to be corrupted:", e);
			return null;
		}
		catch (IOException e) {
			logger.error("Could not read from index:", e);
			return null;
		}
	}

	public String getSnippet(String fieldName, String text, Highlighter highlighter) {
		String snippet;
		try {
			TokenStream tokenStream = getAnalyzer().tokenStream(fieldName, new StringReader(text));
			snippet = highlighter.getBestFragments(tokenStream, text, 2, "...");
		}
		catch (Exception e) {
			logger.error("Exception while getting snippet for field " + fieldName, e);
			snippet = null;
		}
		return snippet;
	}

	// /**
	// * Parses an id-string used for a context filed (a serialized resource)
	// back to a resource.
	// * <b>CAN RETURN NULL</b>
	// * Inverse method of {@link #getResourceID(Resource)}
	// * @param idString
	// * @return null if the passed idString was the {@link #CONTEXT_NULL}
	// constant
	// */
	// private Resource getContextResource(String idString) {
	// if (CONTEXT_NULL.equals(idString))
	// return null;
	// else
	// return getResource(idString);
	// }

	/**
	 * Evaluates the given query only for the given resource.
	 */
	public TopDocs search(Resource resource, Query query)
		throws IOException
	{
		// rewrite the query
		TermQuery idQuery = new TermQuery(new Term(SearchFields.URI_FIELD_NAME,
				SearchFields.getResourceID(resource)));
		BooleanQuery combinedQuery = new BooleanQuery();
		combinedQuery.add(idQuery, Occur.MUST);
		combinedQuery.add(query, Occur.MUST);
		return search(combinedQuery);
	}

	/**
	 * Evaluates the given query and returns the results as a TopDocs instance.
	 */
	public TopDocs search(Query query)
		throws IOException
	{
		return getIndexSearcher().search(query, getMaxDocs());
	}

	private int getMaxDocs()
		throws IOException
	{
		int nDocs;
		if (maxDocs > 0) {
			nDocs = maxDocs;
		}
		else {
			nDocs = Math.max(getIndexReader().numDocs(), 1);
		}
		return nDocs;
	}

	private QueryParser getQueryParser(URI propertyURI) {
		// check out which query parser to use, based on the given property URI
		if (propertyURI == null)
			// if we have no property given, we create a default query parser which
			// has the TEXT_FIELD_NAME as the default field
			return new QueryParser(Version.LUCENE_35, SearchFields.TEXT_FIELD_NAME, this.queryAnalyzer);
		else
			// otherwise we create a query parser that has the given property as
			// the default field
			return new QueryParser(Version.LUCENE_35, SearchFields.getPropertyField(propertyURI), this.queryAnalyzer);
	}

	/**
	 * @param contexts
	 * @param sail
	 *        - the underlying native sail where to read the missing triples from
	 *        after deletion
	 * @throws SailException
	 */
	@Override
	public synchronized void clearContexts(Resource... contexts)
		throws IOException
	{

		// logger.warn("Clearing contexts operation did not change the index: contexts are not indexed at the moment");

		logger.debug("deleting contexts: {}", Arrays.toString(contexts));
		// these resources have to be read from the underlying rdf store
		// and their triples have to be added to the luceneindex after deletion of
		// documents
		// HashSet<Resource> resourcesToUpdate = new HashSet<Resource>();

		// remove all contexts passed
		for (Resource context : contexts) {
			// attention: context can be NULL!
			String contextString = SearchFields.getContextID(context);
			Term contextTerm = new Term(SearchFields.CONTEXT_FIELD_NAME, contextString);
			// IndexReader reader = getIndexReader();

			// now check all documents, and remember the URI of the resources
			// that were in multiple contexts
			// TermDocs termDocs = reader.termDocs(contextTerm);
			// try {
			// while (termDocs.next()) {
			// Document document = readDocument(reader, termDocs.doc());
			// // does this document have any other contexts?
			// Field[] fields = document.getFields(CONTEXT_FIELD_NAME);
			// for (Field f : fields)
			// {
			// if
			// (!contextString.equals(f.stringValue())&&!f.stringValue().equals("null"))
			// // there is another context
			// {
			// logger.debug("test new contexts: {}", f.stringValue());
			// // is it in the also contexts (lucky us if it is)
			// Resource otherContextOfDocument =
			// getContextResource(f.stringValue()); // can return null
			// boolean isAlsoDeleted = false;
			// for (Resource c: contexts){
			// if (c==null) {
			// if (otherContextOfDocument == null)
			// isAlsoDeleted = true;
			// } else
			// if (c.equals(otherContextOfDocument))
			// isAlsoDeleted = true;
			// }
			// // the otherContextOfDocument is now eihter marked for deletion or
			// not
			// if (!isAlsoDeleted) {
			// // get ID of document
			// Resource r = getResource(document);
			// resourcesToUpdate.add(r);
			// }
			// }
			// }
			// }
			// } finally {
			// termDocs.close();
			// }

			// now delete all documents from the deleted context
			getIndexWriter().deleteDocuments(contextTerm);
		}

		// now add those again, that had other contexts also.
		// SailConnection con = sail.getConnection();
		// try {
		// // for each resource, add all
		// for (Resource resource : resourcesToUpdate) {
		// logger.debug("re-adding resource {}", resource);
		// ArrayList<Statement> toAdd = new ArrayList<Statement>();
		// CloseableIteration<? extends Statement, SailException> it =
		// con.getStatements(resource, null, null, false);
		// while (it.hasNext()) {
		// Statement s = it.next();
		// toAdd.add(s);
		// }
		// addDocument(resource, toAdd);
		// }
		// } finally {
		// con.close();
		// }

	}

	/**
	 * 
	 */
	@Override
	public synchronized void clear()
		throws IOException
	{
		// clear
		// the old IndexReaders/Searchers are not outdated
		invalidateReaders();
		if (indexWriter != null)
			indexWriter.close();

		// crate new writer
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, analyzer);
		indexWriterConfig.setOpenMode(OpenMode.CREATE);
		indexWriter = new IndexWriter(directory, indexWriterConfig);
		indexWriter.close();
		indexWriter = null;

	}

	//
	// Lucene helper methods
	//

	private static boolean isDeleted(IndexReader reader, int docId) {
		return reader.isDeleted(docId);
	}

	private static Document readDocument(IndexReader reader, int docId, Set<String> fieldsToLoad)
		throws IOException
	{
		return (fieldsToLoad == null) ? reader.document(docId) : reader.document(docId,
				new DocumentFieldSelector(fieldsToLoad));
	}

	static class DocumentFieldSelector implements FieldSelector {

		private static final long serialVersionUID = -6104764003694180068L;

		private final Set<String> fieldsToLoad;

		DocumentFieldSelector(Set<String> fieldsToLoad) {
			this.fieldsToLoad = fieldsToLoad;
		}

		@Override
		public FieldSelectorResult accept(String fieldName) {
			return (fieldsToLoad == null || fieldsToLoad.contains(fieldName)) ? FieldSelectorResult.LOAD
					: FieldSelectorResult.NO_LOAD;
		}
	}
}
