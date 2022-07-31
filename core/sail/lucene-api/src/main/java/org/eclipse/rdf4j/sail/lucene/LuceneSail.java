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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.lucene.util.SearchIndexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A LuceneSail wraps an arbitrary existing Sail and extends it with support for full-text search on all Literals.
 * <h2>Setting up a LuceneSail</h2> LuceneSail works in two modes: storing its data into a directory on the harddisk or
 * into a RAMDirectory in RAM (which is discarded when the program ends). Example with storage in a folder:
 *
 * <pre>
 * // create a sesame memory sail
 * MemoryStore memoryStore = new MemoryStore();
 *
 * // create a lucenesail to wrap the memorystore
 * LuceneSail lucenesail = new LuceneSail();
 * // set this parameter to store the lucene index on disk
 * lucenesail.setParameter(LuceneSail.LUCENE_DIR_KEY, "./data/mydirectory");
 *
 * // wrap memorystore in a lucenesail
 * lucenesail.setBaseSail(memoryStore);
 *
 * // create a Repository to access the sails
 * SailRepository repository = new SailRepository(lucenesail);
 * repository.initialize();
 * </pre>
 *
 * Example with storage in a RAM directory:
 *
 * <pre>
 * // create a sesame memory sail
 * MemoryStore memoryStore = new MemoryStore();
 *
 * // create a lucenesail to wrap the memorystore
 * LuceneSail lucenesail = new LuceneSail();
 * // set this parameter to let the lucene index store its data in ram
 * lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
 *
 * // wrap memorystore in a lucenesail
 * lucenesail.setBaseSail(memoryStore);
 *
 * // create a Repository to access the sails
 * SailRepository repository = new SailRepository(lucenesail);
 * repository.initialize();
 * </pre>
 *
 * <h2>Asking full-text queries</h2> Text queries are expressed using the virtual properties of the LuceneSail. An
 * example query looks like this (SERQL): <code>
 * SELECT Subject, Score, Snippet
 * FROM {Subject} <http://www.openrdf.org/contrib/lucenesail#matches> {}
 * <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> {<http://www.openrdf.org/contrib/lucenesail#LuceneQuery>};
 * <http://www.openrdf.org/contrib/lucenesail#query> {"my Lucene query"};
 * <http://www.openrdf.org/contrib/lucenesail#score> {Score};
 * <http://www.openrdf.org/contrib/lucenesail#snippet> {Snippet}</code>
 *
 * In SPARQL: <code>
 * SELECT ?subject ?score ?snippet ?resource WHERE {
 * ?subject <http://www.openrdf.org/contrib/lucenesail#matches> [
 *      a <http://www.openrdf.org/contrib/lucenesail#LuceneQuery> ;
 *      <http://www.openrdf.org/contrib/lucenesail#query> "my Lucene query" ;
 *      <http://www.openrdf.org/contrib/lucenesail#score> ?score ;
 *      <http://www.openrdf.org/contrib/lucenesail#snippet> ?snippet ;
 *      <http://www.openrdf.org/contrib/lucenesail#resource> ?resource
 *   ]
 * }
 * </code> When defining queries, these properties <b>type and query are mandatory</b>. Also, the <b>matches relation is
 * mandatory</b>. When one of these misses, the query will not be executed as expected. The failure behavior can be
 * configured, setting the Sail property "incompletequeryfail" to true will throw a SailException when such patterns are
 * found, this is the default behavior to help finding inaccurate queries. Set it to false to have warnings logged
 * instead. <b>Multiple queries</b> can be issued to the sail, the results of the queries will be integrated. Note that
 * you cannot use the same variable for multiple Text queries, if you want to combine text searches, use Lucenes query
 * syntax.
 * <h2 id="storedindexed">Fields are stored/indexed</h2> All fields are stored and indexed. The "text" fields (gathering
 * all literals) have to be stored, because when a new literal is added to a document, the previous texts need to be
 * copied from the existing document to the new Document, this does not work when they are only "indexed". Fields that
 * are not stored, cannot be retrieved using full-text querying.
 * <h2>Deleting a Lucene index</h2> At the moment, deleting the lucene index can be done in two ways:
 * <ul>
 * <li>Delete the folder where the data is stored while the application is not running</li>
 * <li>Call the repository's
 * <code>{@link org.eclipse.rdf4j.repository.RepositoryConnection#clear(org.eclipse.rdf4j.model.Resource[])}</code>
 * method with no arguments. <code>clear()</code>. This will delete the index.</li>
 * </ul>
 * <h2>Handling of Contexts</h2> Each lucene document contains a field for every contextIDs that contributed to the
 * document. <b>NULL</b> contexts are marked using the String
 * {@link org.eclipse.rdf4j.sail.lucene.SearchFields#CONTEXT_NULL} ("null") and stored in the lucene field
 * {@link org.eclipse.rdf4j.sail.lucene.SearchFields#CONTEXT_FIELD_NAME} ("context"). This means that when
 * adding/appending to a document, all additional context-uris are added to the document. When deleting individual
 * triples, the context is ignored. In clear(Resource ...) we make a query on all Lucene-Documents that were possibly
 * created by this context(s). Given a document D that context C(1-n) contributed to. D' is the new document after
 * clear(). - if there is only one C then D can be safely removed. There is no D' (I hope this is the standard case:
 * like in ontologies, where all triples about a resource are in one document) - if there are multiple C, remember the
 * uri of D, delete D, and query (s,p,o, ?) from the underlying store after committing the operation- this returns the
 * literals of D', add D' as new document This will probably be both fast in the common case and capable enough in the
 * multiple-C case.
 * <h2 name="indexedfieldssyntax">Defining the indexed Fields</h2> The property {@link #INDEXEDFIELDS} is to configure
 * which fields to index and to project a property to another. Syntax:
 *
 * <pre>
 * # only index label and comment
 * index.1=http://www.w3.org/2000/01/rdf-schema#label
 * index.2=http://www.w3.org/2000/01/rdf-schema#comment
 * # project http://xmlns.com/foaf/0.1/name to rdfs:label
 * http\://xmlns.com/foaf/0.1/name=http\://www.w3.org/2000/01/rdf-schema#label
 * </pre>
 *
 * <h2 name="indexidsyntax">Set and select Lucene sail by id</h2> The property {@link #INDEX_ID} is to configure the id
 * of the index and filter every request without the search:indexid predicate, the request would be:
 *
 * <pre>
 * ?subj search:matches [
 * 	      search:indexid my:lucene_index_id;
 * 	      search:query "search terms...";
 * 	      search:property my:property;
 * 	      search:score ?score;
 * 	      search:snippet ?snippet ] .
 * </pre>
 *
 * If a LuceneSail is using another LuceneSail as a base sail, the evaluation mode should be set to
 * {@link TupleFunctionEvaluationMode#NATIVE}.
 *
 * <h2 name="indexedtypelangsyntax">Defining the indexed Types/Languages</h2> The properties {@link #INDEXEDTYPES} and
 * {@link #INDEXEDLANG} are to configure which fields to index by their language or type. {@link #INDEXEDTYPES} Syntax:
 *
 * <pre>
 * # only index object of rdf:type ex:mytype1, rdf:type ex:mytype2 or ex:mytypedef ex:mytype3
 * http\://www.w3.org/1999/02/22-rdf-syntax-ns#type=http://example.org/mytype1 http://example.org/mytype2
 * http\://example.org/mytypedef=http://example.org/mytype3
 * </pre>
 *
 * {@link #INDEXEDLANG} Syntax:
 *
 * <pre>
 * # syntax to index only French(fr) and English(en) literals
 * fr en
 * </pre>
 *
 * <h2>Datatypes</h2> Datatypes are ignored in the LuceneSail.
 */
public class LuceneSail extends NotifyingSailWrapper {

	/*
	 * FIXME: Add a proper reference to the ISWC paper in the Javadoc. Gunnar: only when/if the paper is accepted
	 * Enrico: paper was rejected Leo: We need to resubmit it. FIXME: Add settings that instruct a LuceneSailConnection
	 * or LuceneIndex which properties are to be handled in which way. This is conceptually similar to Lucene's Field
	 * types: should properties be stored in the wrapped Sail (enabling retrieval through RDF queries), indexed in the
	 * LuceneIndex (enabling full-text search using Lucene queries embedded in RDF graph queries) or both? Gunnar and
	 * Leo: we had this in the old version, we might add later. Enrico: in beagle we set the default setting to index
	 * AND store a field, so that when you extend the ontology you can be sure it is indexed and stored by the
	 * lucenesail without touching it. For certain (very rare) predicates (like the full text of the resource) we then
	 * explicitly turned off the store option. That would be a desired behaviour. In the old version an RDF file was
	 * used, but it should be done differently, that is too hard-coded! can't that information be stored in the wrapped
	 * sail itself? Annotate a predicate with the proper lucene values (store / index / storeAndIndex), if nothing is
	 * given, take the default, and read this on starting the lucenesail. Leo: ok, default = index and store, agreed.
	 * Leo: about configuration: RDF config is agreed, if passed as file, inside the wrapped sail, or in an extra sail
	 * should all be possible.
	 */

	/*
	 * FIXME: This code can only handle RDF queries containing a single "Lucene expression" (i.e. a combination of
	 * matches, query and optionally other predicates from the LuceneSail's namespace), the other expressions are
	 * ignored. Extending this to support an arbitrary number of search expressions is theoretically possible but easier
	 * said then done, especially because of the number of different cases that need to be handled: variable subject vs.
	 * specified subject, expressions operating on the same subject vs. expressions operating on different subjects,
	 * etc. Gunnar: I would we restrict this to one. Enrico might have other requirements? Enrico: we need 1) an
	 * arbitrary number of lucene expressions and 2) an arbitrary combination with ordinary structured queries (see
	 * lucenesail paper, fig. 1 on page 6) Leo: combining lucene query with normal query is required, having multiple
	 * lucene queries in one SPARQL query is a good idea, which should be doable. Lower priority. FIXME: We should
	 * escape those chars in predicates/field names that have a special meaning in Lucene's query syntax, using ":" in a
	 * field name might lead to problems (it will when you start to query on these fields). Enrico: yes, we escaped
	 * those : sucessfully with a simple \, the only difficuilty was to figure out how many \ are needed (how often they
	 * get unescaped until they arrive at Lucene) Leo noticed this. Gunnar asks: Does lucene not have a escape syntax?
	 * FIXME: The getScore method is a convenient and efficient way of testing whether a given document matches a query,
	 * as it adds the document URI to the Lucene query instead of firing the query and looping over the result set. The
	 * problem with this method is that I am not sure whether adding the URI to the Lucene query will lead to a
	 * different score for that document. For most applications this is probably not a problem as you either will use
	 * the search method with the scores reposted to its listener, or the getScore method, but not both. The order of
	 * matching documents will probably be the same when sorting on score (field is indexed without normalization + only
	 * unique values). Still, it is counterintuitive when a particular document is returned with a given score and a
	 * getScore for that same URI gives a different score. FIXME: the code is very much NOT thread-safe, especially when
	 * you are changing the index and querying it with LuceneSailConnection at the same time: the IndexReaders/Searchers
	 * are closed after each statement addition or removal but they must also remain open while we are looping over
	 * search results. Also, internal document numbers are used in the communication between LuceneIndex and
	 * LuceneSailConnection, which is not a good idea. Some mechanism has to be introduced to support external querying
	 * while the index is being modified (basically: make sure that a single search process keeps using the same
	 * IndexSearcher). Gunnar and Leo: we are not sure if the original lucenesail was 100% threadsafe, but at least it
	 * had "synchronized" everywhere :)
	 * http://gnowsis.opendfki.de/repos/gnowsis/trunk/lucenesail/src/java/org/openrdf/sesame/sailimpl/
	 * lucenesail/LuceneIndex.java This might be a big issue in Nepomuk... Enrico: do we have multiple threads? do we
	 * need separate threads? Leo: we have separate threads, but we don't care much for now.
	 */

	final static private Logger logger = LoggerFactory.getLogger(LuceneSail.class);

	/**
	 * Set the parameter "reindexQuery=" to configure the statements to index over. Default value is "SELECT ?s ?p ?o ?c
	 * WHERE {{?s ?p ?o} UNION {GRAPH ?c {?s ?p ?o.}}} ORDER BY ?s" . NB: the query must contain the bindings ?s, ?p, ?o
	 * and ?c and must be ordered by ?s.
	 */
	public static final String REINDEX_QUERY_KEY = "reindexQuery";

	/**
	 * Set the parameter "indexedfields=..." to configure a selection of fields to index, and projections of properties.
	 * Only the configured fields will be indexed. A property P projected to Q will cause the index to contain Q instead
	 * of P, when triples with P were indexed. Syntax of indexedfields - see <a href="#indexedfieldssyntax">above</a>
	 */
	public static final String INDEXEDFIELDS = "indexedfields";

	/**
	 * Set the parameter "indexedtypes=..." to configure a selection of field type to index. Only the fields with the
	 * specific type will be indexed. Syntax of indexedtypes - see <a href="#indexedtypelangsyntax">above</a>
	 */
	public static final String INDEXEDTYPES = "indexedtypes";

	/**
	 * Set the parameter "indexedlang=..." to configure a selection of field language to index. Only the fields with the
	 * specific language will be indexed. Syntax of indexedlang - see <a href="#indexedtypelangsyntax">above</a>
	 */
	public static final String INDEXEDLANG = "indexedlang";
	/**
	 * See {@link org.eclipse.rdf4j.sail.lucene.TypeBacktraceMode}
	 */
	public static final String INDEX_TYPE_BACKTRACE_MODE = "indexBacktraceMode";
	/**
	 * Set the key "lucenedir=&lt;path&gt;" as sail parameter to configure the Lucene Directory on the filesystem where
	 * to store the lucene index.
	 */
	public static final String LUCENE_DIR_KEY = "lucenedir";

	/**
	 * Set the default directory of the Lucene index files. The value is always relational to the {@code dataDir}
	 * location as a parent directory.
	 */
	public static final String DEFAULT_LUCENE_DIR = ".index";

	/**
	 * Set the key "useramdir=true" as sail parameter to let the LuceneSail store its Lucene index in RAM. This is not
	 * intended for production environments.
	 */
	public static final String LUCENE_RAMDIR_KEY = "useramdir";

	/**
	 * Set the key "maxDocuments=&lt;n&gt;" as sail parameter to limit the maximum number of documents to return from a
	 * search query. The default is to return all documents. NB: this may involve extra cost for some SearchIndex
	 * implementations as they may have to determine this number.
	 */
	public static final String MAX_DOCUMENTS_KEY = "maxDocuments";

	/**
	 * Set this key to configure which fields contain WKT and should be spatially indexed. The value should be a
	 * space-separated list of URIs. Default is http://www.opengis.net/ont/geosparql#asWKT.
	 */
	public static final String WKT_FIELDS = "wktFields";

	/**
	 * Set this key to configure the SearchIndex class implementation. Default is
	 * org.eclipse.rdf4j.sail.lucene.LuceneIndex.
	 */
	public static final String INDEX_CLASS_KEY = "index";

	/**
	 * Set this key to configure the filtering of queries, if this parameter is set, the match object should contain the
	 * search:indexid parameter, see the syntax <a href="#indexidsyntax">above</a>
	 */
	public static final String INDEX_ID = "indexid";

	public static final String DEFAULT_INDEX_CLASS = "org.eclipse.rdf4j.sail.lucene.impl.LuceneIndex";

	/**
	 * Set this key as sail parameter to configure the Lucene analyzer class implementation to use for text analysis.
	 */
	public static final String ANALYZER_CLASS_KEY = "analyzer";

	/**
	 * Set this key as sail parameter to configure {@link org.apache.lucene.search.similarities.Similarity} class
	 * implementation to use for text analysis.
	 */
	public static final String SIMILARITY_CLASS_KEY = "similarity";

	/**
	 * Set this key as sail parameter to influence whether incomplete queries are treated as failure (Malformed queries)
	 * or whether they are ignored. Set to either "true" or "false". When omitted in the properties, true is default
	 * (failure on incomplete queries). see {@link #isIncompleteQueryFails()}
	 */
	public static final String INCOMPLETE_QUERY_FAIL_KEY = "incompletequeryfail";

	/**
	 * See {@link TupleFunctionEvaluationMode}.
	 */
	public static final String EVALUATION_MODE_KEY = "evaluationMode";

	/**
	 * Set this key as sail parameter to influence the fuzzy prefix length.
	 */
	public static final String FUZZY_PREFIX_LENGTH_KEY = "fuzzyPrefixLength";

	/**
	 * The LuceneIndex holding the indexed literals.
	 */
	private volatile SearchIndex luceneIndex;

	protected final Properties parameters = new Properties();

	private volatile String reindexQuery = "SELECT ?s ?p ?o ?c WHERE {{?s ?p ?o} UNION {GRAPH ?c {?s ?p ?o.}}} ORDER BY ?s";

	private volatile boolean incompleteQueryFails = true;

	private volatile TupleFunctionEvaluationMode evaluationMode = TupleFunctionEvaluationMode.TRIPLE_SOURCE;

	private volatile TypeBacktraceMode indexBacktraceMode = TypeBacktraceMode.DEFAULT_TYPE_BACKTRACE_MODE;

	private TupleFunctionRegistry tupleFunctionRegistry = TupleFunctionRegistry.getInstance();

	private FederatedServiceResolver serviceResolver = new SPARQLServiceResolver();

	private Set<IRI> indexedFields;

	private Map<IRI, IRI> indexedFieldsMapping;

	private IRI indexId = null;

	private IndexableStatementFilter filter = null;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	public void setLuceneIndex(SearchIndex luceneIndex) {
		this.luceneIndex = luceneIndex;
	}

	public SearchIndex getLuceneIndex() {
		return luceneIndex;
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		if (!closed.get()) {
			return new LuceneSailConnection(super.getConnection(), luceneIndex, this);
		} else {
			throw new SailException("Sail is shut down or not initialized");
		}
	}

	@Override
	public void shutDown() throws SailException {
		if (closed.compareAndSet(false, true)) {
			logger.debug("LuceneSail shutdown");
			try {
				SearchIndex toShutDownLuceneIndex = luceneIndex;
				luceneIndex = null;
				if (toShutDownLuceneIndex != null) {
					toShutDownLuceneIndex.shutDown();
				}
			} catch (IOException e) {
				throw new SailException(e);
			} finally {
				// ensure that super is also invoked when the LuceneIndex causes an
				// IOException
				super.shutDown();
			}
		}
	}

	@Override
	public void setDataDir(File dataDir) {
		Path luceneDir = Paths.get(parameters.getProperty(LuceneSail.LUCENE_DIR_KEY, DEFAULT_LUCENE_DIR), "");
		String luceneDirAbsolute = dataDir.getAbsoluteFile().toPath().resolve(luceneDir).toString();
		this.setParameter(LuceneSail.LUCENE_DIR_KEY, luceneDirAbsolute);
		logger.debug("Absolute path to lucene index dir: {}", luceneDirAbsolute);
		this.getBaseSail().setDataDir(dataDir);
	}

	@Override
	public void init() throws SailException {
		super.init();
		if (parameters.containsKey(INDEXEDFIELDS)) {
			String indexedfieldsString = parameters.getProperty(INDEXEDFIELDS);
			Properties prop = new Properties();
			try {
				try (Reader reader = new StringReader(indexedfieldsString)) {
					prop.load(reader);
				}
			} catch (IOException e) {
				throw new SailException("Could read " + INDEXEDFIELDS + ": " + indexedfieldsString, e);
			}
			ValueFactory vf = getValueFactory();
			indexedFields = new HashSet<>();
			indexedFieldsMapping = new HashMap<>();
			for (Object key : prop.keySet()) {
				String keyStr = key.toString();
				if (keyStr.startsWith("index.")) {
					indexedFields.add(vf.createIRI(prop.getProperty(keyStr)));
				} else {
					indexedFieldsMapping.put(vf.createIRI(keyStr), vf.createIRI(prop.getProperty(keyStr)));
				}
			}
		}

		if (parameters.containsKey(INDEX_ID)) {
			indexId = getValueFactory().createIRI(parameters.getProperty(INDEX_ID));
		}

		try {
			if (parameters.containsKey(REINDEX_QUERY_KEY)) {
				setReindexQuery(parameters.getProperty(REINDEX_QUERY_KEY));
			}
			if (parameters.containsKey(INCOMPLETE_QUERY_FAIL_KEY)) {
				setIncompleteQueryFails(Boolean.parseBoolean(parameters.getProperty(INCOMPLETE_QUERY_FAIL_KEY)));
			}
			if (parameters.containsKey(EVALUATION_MODE_KEY)) {
				setEvaluationMode(TupleFunctionEvaluationMode.valueOf(parameters.getProperty(EVALUATION_MODE_KEY)));
			}
			if (parameters.containsKey(FUZZY_PREFIX_LENGTH_KEY)) {
				setFuzzyPrefixLength(NumberUtils.toInt(parameters.getProperty(FUZZY_PREFIX_LENGTH_KEY), 0));
			}
			if (luceneIndex == null) {
				initializeLuceneIndex();
			}
		} catch (Exception e) {
			throw new SailException("Could not initialize LuceneSail: " + e.getMessage(), e);
		}
	}

	/**
	 * The method is relocated to {@link SearchIndexUtils#createSearchIndex(java.util.Properties) }.
	 *
	 * @param parameters
	 * @return search index
	 * @throws Exception
	 * @deprecated
	 */
	@Deprecated
	protected static SearchIndex createSearchIndex(Properties parameters) throws Exception {
		return SearchIndexUtils.createSearchIndex(parameters);
	}

	protected void initializeLuceneIndex() throws Exception {
		SearchIndex index = SearchIndexUtils.createSearchIndex(parameters);
		setLuceneIndex(index);
	}

	public void setParameter(String key, String value) {
		parameters.setProperty(key, value);
	}

	public String getParameter(String key) {
		return parameters.getProperty(key);
	}

	public Set<String> getParameterNames() {
		return parameters.stringPropertyNames();
	}

	/**
	 * See REINDEX_QUERY_KEY parameter.
	 */
	public String getReindexQuery() {
		return reindexQuery;
	}

	/**
	 * See REINDEX_QUERY_KEY parameter.
	 */
	public void setReindexQuery(String query) {
		this.setParameter(REINDEX_QUERY_KEY, query);
		this.reindexQuery = query;
	}

	/**
	 * When this is true, incomplete queries will trigger a SailException. You can set this value either using
	 * {@link #setIncompleteQueryFails(boolean)} or using the parameter "incompletequeryfail"
	 *
	 * @return Returns the incompleteQueryFails.
	 */
	public boolean isIncompleteQueryFails() {
		return incompleteQueryFails;
	}

	/**
	 * Set this to true, so that incomplete queries will trigger a SailException. Otherwise, incomplete queries will be
	 * logged with level WARN. Default is true. You can set this value also using the parameter "incompletequeryfail".
	 *
	 * @param incompleteQueryFails true or false
	 */
	public void setIncompleteQueryFails(boolean incompleteQueryFails) {
		this.setParameter(INCOMPLETE_QUERY_FAIL_KEY, Boolean.toString(incompleteQueryFails));
		this.incompleteQueryFails = incompleteQueryFails;
	}

	/**
	 * See EVALUATION_MODE_KEY parameter.
	 */
	public TupleFunctionEvaluationMode getEvaluationMode() {
		return evaluationMode;
	}

	/**
	 * See EVALUATION_MODE_KEY parameter.
	 */
	public void setEvaluationMode(TupleFunctionEvaluationMode mode) {
		Objects.requireNonNull(mode);
		this.setParameter(EVALUATION_MODE_KEY, mode.name());
		this.evaluationMode = mode;
	}

	/**
	 * See {@link #INDEX_TYPE_BACKTRACE_MODE} parameter.
	 */
	public TypeBacktraceMode getIndexBacktraceMode() {
		return indexBacktraceMode;
	}

	/**
	 * See {@link #INDEX_TYPE_BACKTRACE_MODE} parameter.
	 */
	public void setIndexBacktraceMode(TypeBacktraceMode mode) {
		Objects.requireNonNull(mode);
		this.setParameter(INDEX_TYPE_BACKTRACE_MODE, mode.name());
		this.indexBacktraceMode = mode;
	}

	public void setFuzzyPrefixLength(int fuzzyPrefixLength) {
		setParameter(FUZZY_PREFIX_LENGTH_KEY, String.valueOf(fuzzyPrefixLength));
	}

	public TupleFunctionRegistry getTupleFunctionRegistry() {
		return tupleFunctionRegistry;
	}

	public void setTupleFunctionRegistry(TupleFunctionRegistry registry) {
		this.tupleFunctionRegistry = registry;
	}

	public FederatedServiceResolver getFederatedServiceResolver() {
		return serviceResolver;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		serviceResolver = resolver;
		super.setFederatedServiceResolver(resolver);
	}

	/**
	 * Starts a reindexation process of the whole sail. Basically, this will delete and add all data again, a
	 * long-lasting process.
	 *
	 * @throws SailException If the Sail could not be reindex
	 */
	public void reindex() throws SailException {
		try {
			// clear
			logger.info("Reindexing sail: clearing...");
			luceneIndex.clear();
			logger.info("Reindexing sail: adding...");

			try {
				luceneIndex.begin();
				// iterate
				SailRepository repo = new SailRepository(new NotifyingSailWrapper(getBaseSail()) {

					@Override
					public void init() {
						// don't re-initialize the Sail when we initialize the repo
					}

					@Override
					public void shutDown() {
						// don't shutdown the underlying sail
						// when we shutdown the repo.
					}
				});
				try (SailRepositoryConnection connection = repo.getConnection()) {
					TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, reindexQuery);
					try (TupleQueryResult res = query.evaluate()) {
						Resource current = null;
						ValueFactory vf = getValueFactory();
						List<Statement> statements = new ArrayList<>();
						while (res.hasNext()) {
							BindingSet set = res.next();
							Resource r = (Resource) set.getValue("s");
							IRI p = (IRI) set.getValue("p");
							Value o = set.getValue("o");
							Resource c = (Resource) set.getValue("c");
							if (current == null) {
								current = r;
							} else if (!current.equals(r)) {
								if (logger.isDebugEnabled()) {
									logger.debug("reindexing resource " + current);
								}
								// commit
								luceneIndex.addDocuments(current, statements);

								// re-init
								current = r;
								statements.clear();
							}
							statements.add(vf.createStatement(r, p, o, c));
						}

						// make sure to index statements for last resource
						if (current != null && !statements.isEmpty()) {
							if (logger.isDebugEnabled()) {
								logger.debug("reindexing resource " + current);
							}
							// commit
							luceneIndex.addDocuments(current, statements);
						}
					}
				} finally {
					repo.shutDown();
				}
				// commit the changes
				luceneIndex.commit();

				logger.info("Reindexing sail: done.");
			} catch (Exception e) {
				logger.error("Rolling back", e);
				luceneIndex.rollback();
				throw e;
			}
		} catch (Exception e) {
			throw new SailException("Could not reindex LuceneSail: " + e.getMessage(), e);
		}
	}

	/**
	 * Sets a filter which determines whether a statement should be considered for indexing when performing complete
	 * reindexing.
	 */
	public void registerStatementFilter(IndexableStatementFilter filter) {
		this.filter = filter;
	}

	protected boolean acceptStatementToIndex(Statement s) {
		IndexableStatementFilter nextFilter = filter;
		return (nextFilter != null) ? nextFilter.accept(s) : true;
	}

	public Statement mapStatement(Statement statement) {
		IRI p = statement.getPredicate();
		boolean predicateChanged = false;
		Map<IRI, IRI> nextIndexedFieldsMapping = indexedFieldsMapping;
		if (nextIndexedFieldsMapping != null) {
			IRI res = nextIndexedFieldsMapping.get(p);
			if (res != null) {
				p = res;
				predicateChanged = true;
			}
		}
		Set<IRI> nextIndexedFields = indexedFields;
		if (nextIndexedFields != null && !nextIndexedFields.contains(p)) {
			return null;
		}

		if (predicateChanged) {
			return getValueFactory().createStatement(statement.getSubject(), p, statement.getObject(),
					statement.getContext());
		} else {
			return statement;
		}
	}

	protected Collection<SearchQueryInterpreter> getSearchQueryInterpreters() {
		return Arrays.<SearchQueryInterpreter>asList(new QuerySpecBuilder(incompleteQueryFails, indexId),
				new DistanceQuerySpecBuilder(luceneIndex), new GeoRelationQuerySpecBuilder(luceneIndex));
	}
}

/*
 * ********************************************************************* BELOW FIXMES are assumed to be fixed or an
 * agreement was reached. They can be removed in Oct 2007.
 */

/*
 * FIXME: The LuceneSail does not alter the datadir (i.e., passes it as-is to the wrapped Sail) and requires you to
 * specify a LuceneIndex. This means more work on the side of the integrator but allows for fine-grained control over
 * the type of storage used by the LuceneIndex: file-based, memory-based, db-based, etc. An alternative method is to
 * give the wrapped Sail a subdir in the datadir and let the LuceneSail take care of creating the LuceneIndex and
 * associated index dir. This gives the LuceneSail/Index more freedom in how it organizes data, e.g. when one wants to
 * store non-committed information in a temporary index without having to use the system's tmp dir. Which method is to
 * be preferred or whether both approaches can be combined has yet to be determined. Gunnar and Leo: Added a
 * sail-parameter, the intialize method will create the luceneindex with sensible defaults if not set. Enrico: sounds
 * good! FIXME: In light of all the issues mentioned in LuceneIndex and given the fact that in most applications,
 * integrators are able to provide statements in a more structured manner that randomly sorted triples, it may be a good
 * idea to provide some extension points that allow integrators to "do their own thing". In a way this is already
 * possible, as they are able to set the LuceneIndex. More sophisticated ways are e.g. an API for updating all
 * statements with the same subject at once. Gunnar and Leo: Proper transaction handling in LuceneIndex shoudl be all we
 * need, or? FIXME: The SailConnectionListener wraps IOExceptions in RuntimeException so that they can be rethrown. This
 * is a temporary fix until we have decided on the design of the SailConnectionListener API; it may even be extended to
 * allow throwing of SailExceptions. FIXME: Investigate whether LuceneSailConnection.clear should address the
 * LuceneIndex directly with a clear command, whether removed statements are reported already through the
 * SailConnectionListener, or whether the latter API will be extended with a separate clear event. FIXME: Gunnar and
 * Leo: Why isn't this implemented as a simple connectionwrapper? The connection-wrapper already forwards all calls, we
 * can just override methods where lucene interaction is needed, or? Do we gain anything by doing it as a listener?
 * Chris: it's been a while but I think this has to do with the SailConnection.clear accepting a number of contexts. As
 * context info is not stored in the Lucene index, we have no idea which info to remove. *If* removed statements are
 * reported to SailConnectionListeners (talk to Arjohn about this), we can use this event to update the index. On the
 * other hand, if we go with Leo's approach of storing multiple context IDs in a single Document (see LuceneIndex), this
 * may become a non-issue. Leo: Then I would implement LuceneSailConnection and do it with the multiple contexts. FIXME:
 * should we use the wrapped Sail's ValueFactory when creating Literals and URIs? Gunnar and Leo: sure, no other
 * solution. Enrico: yes! FIXME: Lucene's query parsing may result in a TooManyClauses Exception, e.g. when a wildcard
 * query matches more than 1024 query terms in the index. This default threshold of max. 1024 terms is configurable
 * through BooleanQuery.setMaxClauseCount but this may lead to very large memory usage (potentially OutOfMemoryErrors)
 * and is also global for all Lucene indices running in the same JVM. Perhaps a modified QueryParser is a solution, e.g.
 * by skipping term 1025 and beyond in order to approximate the query result? Leo: This only applies when we have no
 * "all" field. FIXME: All Literal properties of a Resource are both stored separately as separate Fields, as well as
 * concatenated and indexed as a single field. By *indexing* the former fields as well, we would be able to easily
 * support searching for specific predicates, besides only for entire Resources. We may even need this to support
 * returning snippets, or else we have no idea which property the query matched with. Cons: indexing these fields will
 * increase index size and decrease upload performance. Also, this way of searching for a specific predicate is a bit
 * strange for RDF, as the predicate restriction is part of the Lucene query string instead of the RDF graph query.
 * Gunnar and Leo: index all fields! For proper individual ranking indexing each fields is important. Enrico: yes, index
 * all fields (not only THE ALL field), we need it! Agreement: we index all fields, later make it configurable FIXME: It
 * may seem logical at first to set IndexWriter's auto-commit (available in Lucene 2.2) to false when adding triples, as
 * this could be useful for implementing Sesame's transactions: just commit the IndexWriter whenever the SailConnection
 * is committed. The main problem with this approach is that you are not able to search for Documents that have not been
 * committed yet, which is needed in order to update them with new properties for that subject. Consequently,
 * LuceneIndex' operation is very slow: each change on the IndexWriter is immediately flushed (resulting in disk I/O
 * when using a FSDirectory) and a new IndexReader is created for every added triple, which does some non-trivial
 * initialization. Alternative strategies: (1) don't write Documents right away to the IndexWriter but cache them in
 * main memory and only add them when a commit on the LuceneIndex is issued by the LuceneSailConnection. Potential risk
 * for out-of-memory errors because you have no idea how much memory this is using. (2) Different mechanism but
 * conceptually similar: buffer statements to add and process them in order of subject when a commit is issued or the
 * cache overflows, so that you only need to fetch the Document for that subject once. The size of the cache can be
 * approximated fairly well by looking at the sizes of the strings in their statements. Gunnar and Leo: We had (1) in
 * Gnowsis, and we never ran out of memory :) at least not for this reason ... (2) is harded to implement, we suggest
 * doing (1) and replacing with when it becomes a problem? Gunnar and Leo will do (1) in the next few days. Enrico: we
 * also suggest to use (1), just keep the lucene doc until the transaction is committed so you can continue filling the
 * doc and don't need to get it back from the index. Chris: (1) works for applications like Gnowsis and AutoFocus which
 * probably do a commit after processing every crawled resource, the amount of statements in a transaction is then very
 * small. Note however that uploading a large RDF file to a Repository (also a common Sesame use case) is a single
 * transaction, that's where I expect you can easily get into trouble. Leo: ok, with bigger transactions there is
 * trouble, which we leave to fix once the trouble arises. Chris (in skype-chat): (1) is ok for now, go for it. When
 * statements arrive more or less in order of subject and we tune the caching a bit (e.g. by each time only processing
 * half of the cache and selecting those statements whose subjects we haven't seen in a while), this delayed processing
 * strategy may in some scenarios even lead to the most optimal case where Documents are retrieved and/or written at
 * most once. Changing the index because of cache overflow still breaks SailConnection's contract though: the index
 * should only be altered in a permanent way when the SailConnection gets a commit. At first I thought that a
 * triple-centric Document setup (each triple has its own Document) would solve all this, as opposed to the current
 * Resource-centric setup (all properties with the same subject in a single Document). However, (1) you still need to
 * check the index in order to prevent adding duplicates, which cannot be done on uncommitted Documents - perhaps
 * SailConnectionListener can tell us when a really new triple is added? But even then: probably works for quads, not
 * for triples). Also, (2) when you *are* storing quads (assuming this leads to a context field in the Document), the
 * deletion of a statement no longer simply maps on an IndexWriter.deleteDocuments(Term) invocation, so you need to
 * query again to see which Documents need to be deleted. FIXME: Right now, all literals are stored and indexed,
 * datatypes are ignored. Should we process some datatypes differently? Does it make sense to index booleans, numbers,
 * etc.? Enrico: we don't use data type and language for querying anyways, so does not affect us Agreement: Datatypes
 * are ignored. FIXME: The context of triples is completely ignored at this moment. Perhaps this can simply be solved by
 * giving each Document a context ID besides the Resource ID? Leo (#1): yes, and multiple contextIDs, to state all
 * contexts that contributed to the doc (see below, #2) FIXME: The clear(Resource...) is not implemented as we do not
 * deal with contexts in this LuceneSail implementation and thus do not know which triples to remove. This is
 * problematic when people do a clear with a specific context on a LuceneSail, as the LuceneIndex will then still keep
 * legacy triples around. Only a global clear can be implemented, but not a clear on a specific context. To me this
 * strongly suggests that we add a separate Document for each (Resource, context) pair, even though the objections
 * raised in the paper (troubles with creating scores) are reasonable, because else we are not able to create a proper
 * Sail implementation. This only adds to the issue we realized before with ingoring context, namely that full-text
 * queries cannot be restricted to properties in a certain context. Leo: #2 An optimized approach would be to add
 * multiple contextIDs, to state all contexts that contributed to the doc (see above #1) This means that when
 * adding/appending to a document, all additional context-uris are added to the document. When deleting individual
 * triples, the context is ignored. In clear(Resource ...) we make a query on all Lucene-Documents that were possibly
 * created by this context(s). Given a document D that context C(1-n) contributed to. D' is the new document after
 * clear(). - if there is only one C then D can be safely removed. There is no D' (I hope this is the standard case:
 * like in ontologies, where all triples about a resource are in one document) - if there are multiple C, remember the
 * uri of D, delete D, and query (s,p,o, ?) from the underlying store after committing the operation- this returns the
 * literals of D', add D' as new document This will probably be both fast in the common case and capable enough in the
 * multiple-C case. Any objections? Gunnar? Enrico? Enrico: we dont query contexts at all, so score is better in this
 * way than habving (resource, context) paired docuemts. So this looks like a working solution that keeps the lucene
 * index valid.
 */
