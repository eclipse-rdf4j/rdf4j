/**
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.lucene.spin;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import static org.eclipse.rdf4j.sail.lucene.LuceneSail.INDEXEDFIELDS;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;
import org.eclipse.rdf4j.sail.lucene.SearchIndexQueryContextInitializer;
import org.eclipse.rdf4j.sail.lucene.util.SearchIndexUtils;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activates support of {@link SearchIndex} feature inside {@link SpinSail} and manages the index during statements
 * adding/removing. Technically this sail binds {@link SearchIndex} using .addQueryContextInitializer and wraps
 * connection from baseSail by wrapped which modify SearchIndex.
 *
 * @author jacek grzebyta
 * 
 * @deprecated since 3.0. The experimental LuceneSpinSail is scheduled to be removed by the next major release.
 */
@Deprecated
@Experimental
public class LuceneSpinSail extends NotifyingSailWrapper {

	private final Logger log = LoggerFactory.getLogger(LuceneSpinSail.class);

	private SearchIndex si;

	private Properties parameters = new Properties();

	private Set<IRI> indexedFields;

	private Map<IRI, IRI> indexedFieldsMapping;

	public LuceneSpinSail() {
	}

	public LuceneSpinSail(SpinSail baseSail) {
		super(baseSail);
	}

	public Properties getParameters() {
		return parameters;
	}

	/**
	 * Replaces existing parameters.
	 * <p>
	 * By default parameters field is instantiated in constructor. Using this method replaces the existing field. If you
	 * wish only add missing parameters use {@link #addAbsentParameters(java.util.Properties)}.
	 * </p>
	 * 
	 * @param parameters
	 */
	public void setParameters(Properties parameters) {
		this.parameters = parameters;
	}

	/**
	 * Add only absent parameters from argument.
	 * 
	 * @see Properties#putIfAbsent(java.lang.Object, java.lang.Object)
	 * @param parameters
	 */
	public void addAbsentParameters(Properties parameters) {
		parameters.forEach((Object k, Object v) -> {
			LuceneSpinSail.this.parameters.putIfAbsent(k, v);
		});
	}

	/**
	 * Creates absolute path to Lucene Index. If the properties contains no absolute path to lucene index than it is
	 * created here. The generic pattern of lisp-like pseudocode in that case is: <br/>
	 * <code>
	* (Paths/get (absolute datadir) + (or (getProperty parameters LuceneSail/LUCENE_DIR_KEY) "index/"))
	* </code>
	 * 
	 * @return
	 */
	private Path getAbsoluteLuceneIndexDir() {
		Path parametersIndexDir = Paths.get(parameters.getProperty(LuceneSail.LUCENE_DIR_KEY, "index/"));
		if (!parametersIndexDir.isAbsolute()) {
			parametersIndexDir = Paths.get(getDataDir().getAbsolutePath()).resolve(parametersIndexDir);
		}
		return parametersIndexDir;
	}

	/**
	 * @throws SailException
	 */
	@Override
	public void initialize() throws SailException {
		// Add support for indexed fields
		if (parameters.containsKey(INDEXEDFIELDS)) {
			String indexedfieldsString = parameters.getProperty(INDEXEDFIELDS);
			Properties prop = new Properties();
			try (Reader reader = new StringReader(indexedfieldsString)) {
				prop.load(reader);
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

		((SpinSail) getBaseSail()).setEvaluationMode(TupleFunctionEvaluationMode.TRIPLE_SOURCE);
		parameters.setProperty(LuceneSail.INDEX_CLASS_KEY,
				getParameters().getProperty(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS));
		Path indexLocation = getAbsoluteLuceneIndexDir();
		log.debug("index location: {}", indexLocation);
		Properties newParameters = (Properties) this.parameters.clone();
		newParameters.setProperty(LuceneSail.LUCENE_DIR_KEY, indexLocation.toString());
		try {
			si = SearchIndexUtils.createSearchIndex(newParameters);
			// bind index to SpinSail
			((SpinSail) getBaseSail()).addQueryContextInitializer(new SearchIndexQueryContextInitializer(si));
		} catch (Exception ex) {
			log.warn("error occured during set up of the search index. It might affect functionality.");
			throw new SailException(ex);
		}

		super.initialize();
	}

	/**
	 * @return @throws SailException
	 */
	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		if (si == null) {
			throw new SailException("Index is not created");
		}
		// the connection from the super is created only when the index exists
		return new LuceneSpinSailConnection(super.getConnection(), si, this);
	}

	/**
	 * Copy of {@link LuceneSail#mapStatement(org.eclipse.rdf4j.model.Statement) }
	 * 
	 * @param statement
	 * @return
	 */
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

}
