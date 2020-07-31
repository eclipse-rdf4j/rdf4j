/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * Internal wrapper that sorts statements for pretty printing and repeats blank nodes if inlining them.
 *
 * @author James Leigh
 * @since 2.3
 */
public class ArrangedWriter extends AbstractRDFWriter {

	private final static int DEFAULT_QUEUE_SIZE = 100;

	private final RDFWriter delegate;

	private boolean repeatBlankNodes;

	private int targetQueueSize;

	private int queueSize = 0;

	private final Deque<SubjectInContext> stack = new ArrayDeque<>();

	private final Map<String, String> prefixes = new TreeMap<>();

	private final Map<SubjectInContext, Set<Statement>> statementBySubject = new LinkedHashMap<>();

	private final Model blanks = new LinkedHashModel();

	private final Model blankReferences = new LinkedHashModel();

	// nodes that have not been inlined, so should never be inlined
	private final Set<Value> nonInlinedNodes = new HashSet<>();

	private final Comparator<Statement> comparator = (Statement s1, Statement s2) -> {
		IRI p1 = s1.getPredicate();
		IRI p2 = s2.getPredicate();
		if (p1.equals(RDF.TYPE) && !p2.equals(RDF.TYPE)) {
			return -1;
		} else if (!p1.equals(RDF.TYPE) && p2.equals(RDF.TYPE)) {
			return 1;
		}
		int cmp = p1.stringValue().compareTo(p2.stringValue());
		if (cmp != 0) {
			return cmp;
		}
		Value o1 = s1.getObject();
		Value o2 = s2.getObject();
		if (o1.equals(o2)) {
			return 0;
		}
		if (!(o1 instanceof BNode) && o2 instanceof BNode) {
			return -1;
		} else if (o1 instanceof BNode && !(o2 instanceof BNode)) {
			return 1;
		}
		if (!(o1 instanceof IRI) && o2 instanceof IRI) {
			return -1;
		} else if (o1 instanceof IRI && !(o2 instanceof IRI)) {
			return 1;
		}
		if (!(o1 instanceof Triple) && o2 instanceof Triple) {
			return -1;
		} else if (o1 instanceof Triple && !(o2 instanceof Triple)) {
			return 1;
		}
		int str_cmp = o1.stringValue().compareTo(o2.stringValue());
		if (str_cmp != 0) {
			return str_cmp;
		}
		Literal lit1 = (Literal) o1;
		Literal lit2 = (Literal) o2;
		int dt_cmp = lit1.getDatatype().stringValue().compareTo(lit2.getDatatype().stringValue());
		if (dt_cmp != 0) {
			return dt_cmp;
		}
		return lit1.getLanguage().orElse("").compareTo(lit2.getLanguage().orElse(""));
	};

	public ArrangedWriter(RDFWriter delegate) {
		this(delegate, 0);
	}

	public ArrangedWriter(RDFWriter delegate, int size) {
		this(delegate, size, size == -1);
	}

	public ArrangedWriter(RDFWriter delegate, int size, boolean repeatBlankNodes) {
		super(delegate.getOutputStream().orElse(null));
		this.delegate = delegate;
		this.targetQueueSize = size;
		this.repeatBlankNodes = repeatBlankNodes;
	}

	@Override
	public RDFFormat getRDFFormat() {
		return delegate.getRDFFormat();
	}

	@Override
	public RDFWriter setWriterConfig(WriterConfig config) {
		return delegate.setWriterConfig(config);
	}

	@Override
	public WriterConfig getWriterConfig() {
		return delegate.getWriterConfig();
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return delegate.getSupportedSettings();
	}

	@Override
	public <T> RDFWriter set(RioSetting<T> setting, T value) {
		return delegate.set(setting, value);
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();
		if (getWriterConfig().get(BasicWriterSettings.INLINE_BLANK_NODES)) {
			targetQueueSize = -1;
			repeatBlankNodes = true;
		} else if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
			targetQueueSize = DEFAULT_QUEUE_SIZE;
		}
		delegate.startRDF();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		trimNamespaces();
		flushStatements();
		delegate.endRDF();
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		flushStatements();
		if (targetQueueSize == 0) {
			delegate.handleNamespace(prefix, uri);
		} else if (!prefixes.containsKey(uri)) {
			prefixes.put(uri, prefix);
		}
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		flushStatements();
		delegate.handleComment(comment);
	}

	@Override
	protected synchronized void consumeStatement(Statement st) throws RDFHandlerException {
		if (targetQueueSize == 0) {
			delegate.handleStatement(st);
		} else {
			queueStatement(st);
		}
		while (targetQueueSize >= 0 && queueSize > targetQueueSize) {
			flushNamespaces();
			delegate.handleStatement(nextStatement());
		}
	}

	private synchronized Statement nextStatement() {

		if (statementBySubject.isEmpty() && blanks.isEmpty()) {
			assert queueSize == 0;
			return null;
		}
		Set<Statement> statements = null;
		while (statements == null) {
			SubjectInContext last = stack.peekLast();
			statements = statementBySubject.get(last);
			if (statements == null && last != null
					&& blanks.contains(last.getSubject(), null, null, last.getContext())) {
				statements = queueBlankStatements(last);
			} else if (statements == null) {
				stack.pollLast();
			}
			if (stack.isEmpty() && statementBySubject.isEmpty()) {
				Statement st = blanks.iterator().next();
				statements = queueBlankStatements(new SubjectInContext(st));
			} else if (stack.isEmpty()) {
				statements = statementBySubject.values().iterator().next();
			}
		}
		Iterator<Statement> iter = statements.iterator();
		Statement next = iter.next();
		queueSize--;
		iter.remove();
		SubjectInContext key = new SubjectInContext(next);
		if (!key.equals(stack.peekLast())) {
			stack.addLast(key);
		}
		if (!iter.hasNext()) {
			statementBySubject.remove(key);
		}
		Value obj = next.getObject();
		if (obj instanceof BNode) {
			// follow blank nodes before continuing with this subject
			SubjectInContext bkey = new SubjectInContext((BNode) obj, next.getContext());
			if (stack.contains(bkey)) {
				// cycle detected
				if (repeatBlankNodes) {
					throw new RDFHandlerException("Blank node cycle detected. Try disabling "
							+ BasicWriterSettings.INLINE_BLANK_NODES.getKey());
				}
			} else {
				stack.addLast(bkey);
			}
		}
		return next;
	}

	private synchronized Set<Statement> queueBlankStatements(SubjectInContext key) {
		Model firstMatch = blanks.filter(key.getSubject(), null, null, key.getContext());
		Model matches = firstMatch.isEmpty() ? blankReferences.filter(key.getSubject(), null, null, key.getContext())
				: firstMatch;
		if (matches.isEmpty()) {
			return null;
		}
		Set<Statement> set = statementBySubject.get(key);
		if (set == null) {
			statementBySubject.put(key, set = new TreeSet<>(comparator));
		}
		set.addAll(matches);
		if (firstMatch.isEmpty()) {
			// repeat blank node values
			queueSize += matches.size();
		} else {
			if (repeatBlankNodes && key.getSubject() instanceof BNode && isStillReferenced(key)) {
				blankReferences.addAll(matches);
			}
			blanks.remove(key.getSubject(), null, null, key.getContext());
		}
		return set;
	}

	private boolean isStillReferenced(SubjectInContext key) {
		if (blanks.contains(null, null, key.getSubject(), key.getContext())) {
			return true;
		}
		for (SubjectInContext subj : stack) {
			Set<Statement> stmts = statementBySubject.get(subj);
			if (stmts != null) {
				for (Statement st : stmts) {
					if (st.getObject().equals(key.getSubject()) || Objects.equals(st.getContext(), key.getContext())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private synchronized void queueStatement(Statement st) {
		SubjectInContext key = new SubjectInContext(st);
		Set<Statement> stmts = statementBySubject.get(key);
		if (stmts == null && st.getSubject() instanceof BNode && !stack.contains(key)) {
			blanks.add(st);
		} else {
			if (stmts == null) {
				statementBySubject.put(key, stmts = new TreeSet<>(comparator));
			}
			stmts.add(st);
		}
		queueSize++;
	}

	private synchronized void flushStatements() throws RDFHandlerException {
		if (!statementBySubject.isEmpty() || !blanks.isEmpty()) {
			flushNamespaces();

			// used to store all the statements
			ArrayList<Statement> statements = new ArrayList<>();

			// used to store blank nodes along with the number of times they are used as an object in a statement.
			Map<BNode, Integer> bNodeOccurrences = new HashMap<>();

			Statement st;
			while ((st = nextStatement()) != null) {
				statements.add(st);

				Value obj = st.getObject();

				// if the object in the statement is a blank node, we will update its number of occurrences
				if (obj instanceof BNode) {
					bNodeOccurrences.compute(
							(BNode) obj,
							(key, i) -> i == null ? 1 : i + 1
					);

					if (bNodeOccurrences.get(obj) > 1) {
						if (st.getSubject() instanceof BNode) {
							nonInlinedNodes.add(st.getSubject());
						}
						nonInlinedNodes.add(obj);
					}
				}
			}

			boolean inlineBlankNodesInitialValue = getWriterConfig().get(BasicWriterSettings.INLINE_BLANK_NODES);

			for (Statement statement : statements) {
				if (inlineBlankNodesInitialValue) {
					Value obj = statement.getObject();
					Resource subj = statement.getSubject();

					if (nonInlinedNodes.contains(obj) || nonInlinedNodes.contains(subj)) {
						getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, false);

						// since we are not inlining anything in this statement we must make sure to not inline
						// other uses of the subject as an object later
						if (obj instanceof BNode) {
							nonInlinedNodes.add(obj);
						}
						if (subj instanceof BNode) {
							nonInlinedNodes.add(subj);
						}
					}

					if (obj instanceof BNode) {
						BNode bNode = (BNode) obj;
						if (bNodeOccurrences.get(bNode) > 1) {
							/*
							 * if INLINE_BLANK_NODES is true and the blank node is repeated, we will set
							 * INLINE_BLANK_NODES as false so as to make sure that the blank node object is not inlined.
							 */
							if (subj instanceof BNode) {
								nonInlinedNodes.add(subj);
							}
							nonInlinedNodes.add(obj);
							getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, false);
						}

					} else if (subj instanceof BNode) {
						/*
						 * if the subject of a statement is a blank node that has been repeated, it should not be
						 * inlined.
						 */
						BNode bNode = (BNode) subj;
						if (bNodeOccurrences.containsKey(bNode) && bNodeOccurrences.get(bNode) > 1) {
							nonInlinedNodes.add(subj);
							if (obj instanceof BNode) {
								nonInlinedNodes.add(obj);
							}
							getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, false);
						}
					}
				}
				delegate.handleStatement(statement);

				// resetting the value of INLINE_BLANK_NODES
				getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, inlineBlankNodesInitialValue);
			}

			assert queueSize == 0;
		}
	}

	private synchronized void flushNamespaces() throws RDFHandlerException {
		Map<String, String> namespaces = new TreeMap<>();
		for (Map.Entry<String, String> e : prefixes.entrySet()) {
			namespaces.put(e.getValue(), e.getKey());
		}
		for (Map.Entry<String, String> e : namespaces.entrySet()) {
			delegate.handleNamespace(e.getKey(), e.getValue());
		}
		prefixes.clear();
	}

	private synchronized void trimNamespaces() {
		if (!prefixes.isEmpty()) {
			Set<String> used = new HashSet<>(prefixes.size());
			for (Set<Statement> stmts : statementBySubject.values()) {
				getUsedNamespaces(stmts, used);
			}
			getUsedNamespaces(blanks, used);
			prefixes.keySet().retainAll(used);
		}
	}

	private void getUsedNamespaces(Set<Statement> stmts, Set<String> used) {
		for (Statement st : stmts) {
			if (st.getSubject() instanceof IRI) {
				IRI uri = (IRI) st.getSubject();
				used.add(uri.getNamespace());
			}
			used.add(st.getPredicate().getNamespace());
			if (st.getObject() instanceof IRI) {
				IRI uri = (IRI) st.getObject();
				used.add(uri.getNamespace());
			} else if (st.getObject() instanceof Literal) {
				Literal lit = (Literal) st.getObject();
				if (lit.getDatatype() != null) {
					used.add(lit.getDatatype().getNamespace());
				}
			}
		}
	}

	private static class SubjectInContext {

		private final Resource subject;

		private final Resource context;

		private SubjectInContext(Statement st) {
			this(st.getSubject(), st.getContext());
		}

		private SubjectInContext(Resource subject, Resource context) {
			assert subject != null;
			this.subject = subject;
			this.context = context;
		}

		public Resource getSubject() {
			return subject;
		}

		public Resource getContext() {
			return context;
		}

		@Override
		public String toString() {
			if (context == null) {
				return subject.toString();
			} else {
				return subject.toString() + " [" + context.toString() + "]";
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + subject.hashCode();
			result = prime * result + ((context == null) ? 0 : context.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			SubjectInContext other = (SubjectInContext) obj;
			if (!subject.equals(other.subject)) {
				return false;
			}
			if (context == null) {
				return other.context == null;
			} else {
				return context.equals(other.context);
			}
		}
	}

}
