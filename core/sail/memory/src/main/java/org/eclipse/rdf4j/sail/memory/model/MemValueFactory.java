/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.util.Collections;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * A factory for MemValue objects that keeps track of created objects to prevent the creation of duplicate objects,
 * minimizing memory usage as a result.
 *
 * @author Arjohn Kampman
 * @author David Huynh
 */
public class MemValueFactory extends AbstractValueFactory {

	/*------------*
	 * Attributes *
	 *------------*/

	/**
	 * Registry containing the set of MemURI objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<MemIRI> iriRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemTriple objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<MemTriple> tripleRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemBNode objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<MemBNode> bnodeRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemLiteral objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<MemLiteral> literalRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of namespce strings as used by MemURI objects in a MemoryStore. This registry enables
	 * the reuse of objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<String> namespaceRegistry = new WeakObjectRegistry<>();

	/*---------*
	 * Methods *
	 *---------*/

	public void clear() {
		iriRegistry.clear();
		bnodeRegistry.clear();
		literalRegistry.clear();
		namespaceRegistry.clear();
	}

	/**
	 * Returns a previously created MemValue that is equal to the supplied value, or <var>null</var> if the supplied
	 * value is a new value or is equal to <var>null</var>.
	 *
	 * @param value The MemValue equivalent of the supplied value, or <var>null</var>.
	 * @return A previously created MemValue that is equal to <var>value</var>, or <var>null</var> if no such value
	 *         exists or if <var>value</var> is equal to <var>null</var>.
	 */
	public MemValue getMemValue(Value value) {
		if (value instanceof Resource) {
			return getMemResource((Resource) value);
		} else if (value instanceof Literal) {
			return getMemLiteral((Literal) value);
		} else if (value == null) {
			return null;
		} else {
			throw new IllegalArgumentException("value is not a Resource or Literal: " + value);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemResource getMemResource(Resource resource) {
		if (resource instanceof IRI) {
			return getMemURI((IRI) resource);
		} else if (resource instanceof BNode) {
			return getMemBNode((BNode) resource);
		} else if (resource instanceof Triple) {
			return getMemTriple((Triple) resource);
		} else if (resource == null) {
			return null;
		} else {
			throw new IllegalArgumentException("resource is not a URI or BNode: " + resource);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemIRI getMemURI(IRI uri) {
		if (isOwnMemValue(uri)) {
			return (MemIRI) uri;
		} else {
			return iriRegistry.get(uri);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemBNode getMemBNode(BNode bnode) {
		if (isOwnMemValue(bnode)) {
			return (MemBNode) bnode;
		} else {
			return bnodeRegistry.get(bnode);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemLiteral getMemLiteral(Literal literal) {
		if (isOwnMemValue(literal)) {
			return (MemLiteral) literal;
		} else {
			return literalRegistry.get(literal);
		}
	}

	/**
	 * Checks whether the supplied value is an instance of <var>MemValue</var> and whether it has been created by this
	 * MemValueFactory.
	 */
	private boolean isOwnMemValue(Value value) {
		return value instanceof MemValue && ((MemValue) value).getCreator() == this;
	}

	/**
	 * Gets all URIs that are managed by this value factory.
	 * <p>
	 * <b>Warning:</b> This method is not synchronized.
	 *
	 * @return An unmodifiable Set of MemURI objects.
	 * @deprecated Use getMemIRIsIterator() instead.
	 */
	@Deprecated(forRemoval = true, since = "4.0.0")
	public Set<MemIRI> getMemURIs() {
		return Collections.unmodifiableSet(iriRegistry);
	}

	/**
	 * Gets all bnodes that are managed by this value factory.
	 * <p>
	 * <b>Warning:</b> This method is not synchronized.
	 *
	 * @return An unmodifiable Set of MemBNode objects.
	 * @deprecated Use getMemBNodesIterator() instead.
	 */
	@Deprecated(forRemoval = true, since = "4.0.0")
	public Set<MemBNode> getMemBNodes() {
		return Collections.unmodifiableSet(bnodeRegistry);
	}

	/**
	 * Gets all literals that are managed by this value factory.
	 * <p>
	 * <b>Warning:</b> This method is not synchronized.
	 *
	 * @return An unmodifiable Set of MemURI objects.
	 * @deprecated Use getMemLiteralsIterator() instead.
	 */
	@Deprecated(forRemoval = true, since = "4.0.0")
	public Set<MemLiteral> getMemLiterals() {
		return Collections.unmodifiableSet(literalRegistry);
	}

	/**
	 * Gets all URIs that are managed by this value factory.
	 *
	 * @return An autocloseable iterator.
	 */
	public CloseableIterator<MemIRI> getMemIRIsIterator() {
		return iriRegistry.closeableIterator();
	}

	/**
	 * Gets all bnodes that are managed by this value factory.
	 *
	 * @return An autocloseable iterator.
	 */
	public CloseableIterator<MemBNode> getMemBNodesIterator() {
		return bnodeRegistry.closeableIterator();
	}

	/**
	 * Gets all literals that are managed by this value factory.
	 *
	 * @return An autocloseable iterator.
	 */
	public CloseableIterator<MemLiteral> getMemLiteralsIterator() {
		return literalRegistry.closeableIterator();
	}

	/**
	 * Gets or creates a MemValue for the supplied Value. If the factory already contains a MemValue object that is
	 * equivalent to the supplied value then this equivalent value will be returned. Otherwise a new MemValue will be
	 * created, stored for future calls and then returned.
	 *
	 * @param value A Resource or Literal.
	 * @return The existing or created MemValue.
	 */
	public MemValue getOrCreateMemValue(Value value) {
		if (value instanceof Resource) {
			return getOrCreateMemResource((Resource) value);
		} else if (value instanceof Literal) {
			return getOrCreateMemLiteral((Literal) value);
		} else {
			throw new IllegalArgumentException("value is not a Resource or Literal: " + value);
		}
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemResource getOrCreateMemResource(Resource resource) {
		if (resource instanceof IRI) {
			return getOrCreateMemURI((IRI) resource);
		} else if (resource instanceof BNode) {
			return getOrCreateMemBNode((BNode) resource);
		} else if (resource instanceof Triple) {
			return getOrCreateMemTriple((Triple) resource);
		} else {
			throw new IllegalArgumentException("resource is not a URI or BNode: " + resource);
		}
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemIRI getOrCreateMemURI(IRI uri) {
		return iriRegistry.getOrAdd(uri, () -> {
			String namespace = uri.getNamespace();
			assert namespace != null;
			String sharedNamespace = namespaceRegistry.get(namespace);

			if (sharedNamespace == null) {
				// New namespace, add it to the registry
				namespaceRegistry.add(namespace);
			} else {
				// Use the shared namespace
				namespace = sharedNamespace;
			}

			// Create a MemURI and add it to the registry
			return new MemIRI(this, namespace, uri.getLocalName());
		});

	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemBNode getOrCreateMemBNode(BNode bnode) {
		return bnodeRegistry.getOrAdd(bnode, () -> new MemBNode(this, bnode.getID()));
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemLiteral getOrCreateMemLiteral(Literal literal) {
		return literalRegistry.getOrAdd(literal, () -> {
			String label = literal.getLabel();
			IRI datatype = literal.getDatatype();
			MemLiteral memLiteral;

			if (Literals.isLanguageLiteral(literal)) {
				memLiteral = new MemLiteral(this, label, literal.getLanguage().get());
			} else {
				try {
					if (XMLDatatypeUtil.isIntegerDatatype(datatype)) {
						memLiteral = new IntegerMemLiteral(this, label, literal.integerValue(), datatype);
					} else if (datatype.equals(XSD.DECIMAL)) {
						memLiteral = new DecimalMemLiteral(this, label, literal.decimalValue(), datatype);
					} else if (datatype.equals(XSD.FLOAT)) {
						memLiteral = new NumericMemLiteral(this, label, literal.floatValue(), datatype);
					} else if (datatype.equals(XSD.DOUBLE)) {
						memLiteral = new NumericMemLiteral(this, label, literal.doubleValue(), datatype);
					} else if (datatype.equals(XSD.BOOLEAN)) {
						memLiteral = new BooleanMemLiteral(this, label, literal.booleanValue());
					} else if (datatype.equals(XSD.DATETIME)) {
						memLiteral = new CalendarMemLiteral(this, label, datatype, literal.calendarValue());
					} else if (datatype.equals(XSD.DATETIMESTAMP)) {
						memLiteral = new CalendarMemLiteral(this, label, datatype, literal.calendarValue());
					} else {
						memLiteral = new MemLiteral(this, label, datatype);
					}
				} catch (IllegalArgumentException e) {
					// Unable to parse literal label to primitive type
					memLiteral = new MemLiteral(this, label, datatype);
				}
			}

			return memLiteral;
		});
	}

	@Override
	public IRI createIRI(String uri) {
		return getOrCreateMemURI(super.createIRI(uri));
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		return iriRegistry.getOrAdd(Values.iri(namespace, localName), () -> {

			if (namespace.indexOf(':') == -1) {
				throw new IllegalArgumentException("Not a valid (absolute) URI: " + namespace + localName);
			}

			String correctNamespace;
			String correctLocalName;

			if (!URIUtil.isCorrectURISplit(namespace, localName)) {
				IRI iri = super.createIRI(namespace + localName);
				correctNamespace = iri.getNamespace();
				correctLocalName = iri.getLocalName();

			} else {
				correctNamespace = namespace;
				correctLocalName = localName;
			}

			String sharedNamespace = namespaceRegistry.get(correctNamespace);

			if (sharedNamespace == null) {
				// New namespace, add it to the registry
				namespaceRegistry.add(correctNamespace);
				sharedNamespace = correctNamespace;
			} else {
				// Use the shared namespace
				sharedNamespace = correctNamespace;
			}

			// Create a MemURI and add it to the registry
			return new MemIRI(this, sharedNamespace, correctLocalName);

		});

	}

	@Override
	public BNode createBNode(String nodeID) {
		return getOrCreateMemBNode(super.createBNode(nodeID));
	}

	@Override
	public Literal createLiteral(String value) {
		return getOrCreateMemLiteral(super.createLiteral(value));
	}

	@Override
	public Literal createLiteral(String value, String language) {
		return getOrCreateMemLiteral(super.createLiteral(value, language));
	}

	@Override
	public Literal createLiteral(String value, IRI datatype) {
		return getOrCreateMemLiteral(super.createLiteral(value, datatype));
	}

	@Override
	public Literal createLiteral(boolean value) {
		MemLiteral newLiteral = new BooleanMemLiteral(this, value);
		return getSharedLiteral(newLiteral);
	}

	@Override
	public Literal createLiteral(XMLGregorianCalendar calendar) {
		MemLiteral newLiteral = new CalendarMemLiteral(this, calendar);
		return getSharedLiteral(newLiteral);
	}

	private Literal getSharedLiteral(MemLiteral newLiteral) {
		return literalRegistry.getOrAdd(newLiteral, () -> newLiteral);
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	private MemTriple getOrCreateMemTriple(Triple triple) {
		MemTriple memTriple = getMemTriple(triple);

		if (memTriple == null) {
			// Create a MemTriple and add it to the registry
			MemTriple newMemTriple = new MemTriple(this, getOrCreateMemResource(triple.getSubject()),
					getOrCreateMemURI(triple.getPredicate()), getOrCreateMemValue(triple.getObject()));
			boolean wasNew = tripleRegistry.add(newMemTriple);

			if (!wasNew) {
				return tripleRegistry.getOrAdd(triple, () -> newMemTriple);
			} else {
				return newMemTriple;
			}
		} else {
			return memTriple;
		}

	}

	private MemTriple getMemTriple(Triple triple) {
		if (isOwnMemValue(triple)) {
			return (MemTriple) triple;
		} else {
			return tripleRegistry.get(triple);
		}
	}

}
