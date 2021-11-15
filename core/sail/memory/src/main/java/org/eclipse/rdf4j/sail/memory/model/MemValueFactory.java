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
	private final ConcurrentWeakObjectRegistry<MemIRI> uriRegistry = new ConcurrentWeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemTriple objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final ConcurrentWeakObjectRegistry<MemTriple> tripleRegistry = new ConcurrentWeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemBNode objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final ConcurrentWeakObjectRegistry<MemBNode> bnodeRegistry = new ConcurrentWeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemLiteral objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final ConcurrentWeakObjectRegistry<MemLiteral> literalRegistry = new ConcurrentWeakObjectRegistry<>();

	/**
	 * Registry containing the set of namespce strings as used by MemURI objects in a MemoryStore. This registry enables
	 * the reuse of objects, minimizing the number of objects in main memory.
	 */
	private final ConcurrentWeakObjectRegistry<String> namespaceRegistry = new ConcurrentWeakObjectRegistry<>();

	/*---------*
	 * Methods *
	 *---------*/

	public void clear() {
		uriRegistry.clear();
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
		if (isOwnMemValue(value)) {
			return (MemValue) value;
		}

		if (value == null) {
			return null;
		} else if (value.isResource()) {
			return getMemResource((Resource) value);
		} else if (value.isLiteral()) {
			return getMemLiteral((Literal) value);
		} else {
			throw new IllegalArgumentException("value is not a Resource or Literal: " + value);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemResource getMemResource(Resource resource) {
		if (isOwnMemValue(resource)) {
			return (MemResource) resource;
		}

		if (resource == null) {
			return null;
		} else if (resource.isIRI()) {
			return getMemURI((IRI) resource);
		} else if (resource.isBNode()) {
			return getMemBNode((BNode) resource);
		} else if (resource.isTriple()) {
			return getMemTriple((Triple) resource);
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
			return uriRegistry.get(uri);
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
	 * <b>Warning:</b> This method is not synchronized. To iterate over the returned set in a thread-safe way, this
	 * method should only be called while synchronizing on this object.
	 *
	 * @return An unmodifiable Set of MemURI objects.
	 */
	public Set<MemIRI> getMemURIs() {
		return Collections.unmodifiableSet(uriRegistry);
	}

	/**
	 * Gets all bnodes that are managed by this value factory.
	 * <p>
	 * <b>Warning:</b> This method is not synchronized. To iterate over the returned set in a thread-safe way, this
	 * method should only be called while synchronizing on this object.
	 *
	 * @return An unmodifiable Set of MemBNode objects.
	 */
	public Set<MemBNode> getMemBNodes() {
		return Collections.unmodifiableSet(bnodeRegistry);
	}

	/**
	 * Gets all literals that are managed by this value factory.
	 * <p>
	 * <b>Warning:</b> This method is not synchronized. To iterate over the returned set in a thread-safe way, this
	 * method should only be called while synchronizing on this object.
	 *
	 * @return An unmodifiable Set of MemURI objects.
	 */
	public Set<MemLiteral> getMemLiterals() {
		return Collections.unmodifiableSet(literalRegistry);
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
		if (value.isResource()) {
			return getOrCreateMemResource((Resource) value);
		} else if (value.isLiteral()) {
			return getOrCreateMemLiteral((Literal) value);
		} else {
			throw new IllegalArgumentException("value is not a Resource or Literal: " + value);
		}
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemResource getOrCreateMemResource(Resource resource) {
		if (resource.isIRI()) {
			return getOrCreateMemURI((IRI) resource);
		} else if (resource.isBNode()) {
			return getOrCreateMemBNode((BNode) resource);
		} else if (resource.isTriple()) {
			return getOrCreateMemTriple((Triple) resource);
		} else {
			throw new IllegalArgumentException("resource is not a URI or BNode: " + resource);
		}
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemIRI getOrCreateMemURI(IRI uri) {
		MemIRI memURI = getMemURI(uri);
		if (memURI != null) {
			return memURI;
		}

		// Namespace strings are relatively large objects and are shared
		// between uris
		String namespace = namespaceRegistry.getOrAdd(uri.getNamespace());

		// Create a MemURI and add it to the registry
		return uriRegistry.getOrAdd(new MemIRI(this, namespace, uri.getLocalName()));
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemBNode getOrCreateMemBNode(BNode bnode) {
		MemBNode memBNode = getMemBNode(bnode);

		if (memBNode != null) {
			return memBNode;
		}

		return bnodeRegistry.getOrAdd(new MemBNode(this, bnode.getID()));

	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemLiteral getOrCreateMemLiteral(Literal literal) {
		MemLiteral memLiteral = getMemLiteral(literal);
		if (memLiteral != null) {
			return memLiteral;
		}

		String label = literal.getLabel();
		IRI datatype = literal.getDatatype();

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

		return literalRegistry.getOrAdd(memLiteral);
	}

	@Override
	public IRI createIRI(String uri) {
		return getOrCreateMemURI(super.createIRI(uri));
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		IRI tempURI;

		// Reuse supplied namespace and local name strings if possible
		if (URIUtil.isCorrectURISplit(namespace, localName)) {
			if (namespace.indexOf(':') == -1) {
				throw new IllegalArgumentException("Not a valid (absolute) URI: " + namespace + localName);
			}

			tempURI = new MemIRI(null, namespace, localName);
		} else {
			tempURI = super.createIRI(namespace + localName);
		}

		return getOrCreateMemURI(tempURI);
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
		return literalRegistry.getOrAdd(newLiteral);
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	private MemTriple getOrCreateMemTriple(Triple triple) {
		MemTriple memTriple = getMemTriple(triple);
		if (memTriple != null) {
			return memTriple;
		}

		// Create a MemTriple and add it to the registry
		MemTriple memTriple2 = new MemTriple(this, getOrCreateMemResource(triple
				.getSubject()),
				getOrCreateMemURI(triple.getPredicate()), getOrCreateMemValue(triple.getObject()));
		return tripleRegistry.getOrAdd(memTriple2);

	}

	private MemTriple getMemTriple(Triple triple) {
		if (isOwnMemValue(triple)) {
			return (MemTriple) triple;
		} else {
			return tripleRegistry.get(triple);
		}
	}

}
