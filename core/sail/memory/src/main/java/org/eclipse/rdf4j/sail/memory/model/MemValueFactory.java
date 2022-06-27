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
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.eclipse.rdf4j.model.vocabulary.RDF;

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
	private final WeakObjectRegistry<IRI, MemIRI> iriRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemTriple objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<Triple, MemTriple> tripleRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemBNode objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<BNode, MemBNode> bnodeRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of MemLiteral objects as used by a MemoryStore. This registry enables the reuse of
	 * objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<Literal, MemLiteral> literalRegistry = new WeakObjectRegistry<>();

	/**
	 * Registry containing the set of namespce strings as used by MemURI objects in a MemoryStore. This registry enables
	 * the reuse of objects, minimizing the number of objects in main memory.
	 */
	private final WeakObjectRegistry<String, String> namespaceRegistry = new WeakObjectRegistry<>();

	/**
	 * A cache of the most common IRIs to improve lookup performance when users use our vocabularies (eg.
	 * {@link RDF#TYPE}).
	 */

//	private final Cache<Value, MemLiteral> literalCache = CacheBuilder.newBuilder().concurrencyLevel(Runtime.getRuntime().availableProcessors()).weakKeys().weakValues().initialCapacity(1000).maximumSize(1000).build();
//	private final Cache<Value, MemIRI> iriCache = CacheBuilder.newBuilder().concurrencyLevel(Runtime.getRuntime().availableProcessors()).weakKeys().weakValues().initialCapacity(1000).maximumSize(1000).build();
//	private final Cache<Value, MemBNode> bNodeCache = CacheBuilder.newBuilder().concurrencyLevel(Runtime.getRuntime().availableProcessors()).weakKeys().weakValues().initialCapacity(1000).maximumSize(1000).build();
//	private final Cache<Value, MemTriple> tripleCache = CacheBuilder.newBuilder().concurrencyLevel(Runtime.getRuntime().availableProcessors()).weakKeys().weakValues().initialCapacity(1000).maximumSize(1000).build();
	public MemValueFactory() {
	}

	public void clear() {
		iriRegistry.clear();
		tripleRegistry.clear();
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
		if (value == null) {
			return null;
		} else if (value.isIRI()) {
			return getMemURI((IRI) value);
		} else if (value.isBNode()) {
			return getMemBNode((BNode) value);
		} else if (value.isTriple()) {
			return getMemTriple((Triple) value);
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
		if (uri == null) {
			return null;
		} else if (isOwnMemIRI(uri)) {
			return (MemIRI) uri;
		} else {
			return iriRegistry.get(uri);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemBNode getMemBNode(BNode bnode) {
		if (bnode == null) {
			return null;
		} else if (isOwnMemBnode(bnode)) {
			return (MemBNode) bnode;
		} else {
			return bnodeRegistry.get(bnode);
		}
	}

	/**
	 * See getMemValue() for description.
	 */
	public MemLiteral getMemLiteral(Literal literal) {
		if (literal == null) {
			return null;
		} else if (isOwnMemLiteral(literal)) {
			return (MemLiteral) literal;
		} else {
			return literalRegistry.get(literal);
		}
	}

	private MemTriple getMemTriple(Triple triple) {
		if (triple == null) {
			return null;
		} else if (isOwnMemTriple(triple)) {
			return (MemTriple) triple;
		} else {
			return tripleRegistry.get(triple);
		}
	}

	/**
	 * Checks whether the supplied value is an instance of <var>MemValue</var> and whether it has been created by this
	 * MemValueFactory.
	 */

	private boolean isOwnMemBnode(BNode value) {
		return value instanceof MemBNode && ((MemBNode) value).getCreator() == this;
	}

	private boolean isOwnMemLiteral(Literal value) {
		return value instanceof MemLiteral && ((MemLiteral) value).getCreator() == this;
	}

	private boolean isOwnMemTriple(Triple value) {
		return value instanceof MemTriple && ((MemTriple) value).getCreator() == this;
	}

	private boolean isOwnMemIRI(IRI value) {
		return value instanceof MemIRI && ((MemIRI) value).getCreator() == this;
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
	public WeakObjectRegistry.AutoCloseableIterator<MemIRI> getMemIRIsIterator() {
		return iriRegistry.closeableIterator();
	}

	/**
	 * Gets all bnodes that are managed by this value factory.
	 *
	 * @return An autocloseable iterator.
	 */
	public WeakObjectRegistry.AutoCloseableIterator<MemBNode> getMemBNodesIterator() {
		return bnodeRegistry.closeableIterator();
	}

	/**
	 * Gets all literals that are managed by this value factory.
	 *
	 * @return An autocloseable iterator.
	 */
	public WeakObjectRegistry.AutoCloseableIterator<MemLiteral> getMemLiteralsIterator() {
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
		if (isOwnMemIRI(uri)) {
			return (MemIRI) uri;
		}

		return iriRegistry.getOrAdd(uri, () -> {

			String namespace = uri.getNamespace();

			String sharedNamespace = namespaceRegistry.getOrAdd(namespace, () -> namespace);

			// Create a MemURI and add it to the registry
			return new MemIRI(this, sharedNamespace, uri.getLocalName());
		});
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemBNode getOrCreateMemBNode(BNode bnode) {
		if (isOwnMemBnode(bnode)) {
			return (MemBNode) bnode;
		}
		return bnodeRegistry.getOrAdd(bnode, () -> new MemBNode(this, bnode.getID()));
	}

	/**
	 * See {@link #getOrCreateMemValue(Value)} for description.
	 */
	public MemLiteral getOrCreateMemLiteral(Literal literal) {
		if (isOwnMemLiteral(literal)) {
			return (MemLiteral) literal;
		}

		return literalRegistry.getOrAdd(literal, () -> {
			String label = literal.getLabel();
			CoreDatatype coreDatatype = literal.getCoreDatatype();
			IRI datatype = coreDatatype != CoreDatatype.NONE ? coreDatatype.getIri() : literal.getDatatype();

			if (Literals.isLanguageLiteral(literal)) {
				return new MemLiteral(this, label, literal.getLanguage().get());
			} else {
				try {
					if (coreDatatype.isXSDDatatype()) {
						if (((CoreDatatype.XSD) coreDatatype).isIntegerDatatype()) {
							return new IntegerMemLiteral(this, label, literal.integerValue(), coreDatatype);
						} else if (coreDatatype == CoreDatatype.XSD.DECIMAL) {
							return new DecimalMemLiteral(this, label, literal.decimalValue(), coreDatatype);
						} else if (coreDatatype == CoreDatatype.XSD.FLOAT) {
							return new NumericMemLiteral(this, label, literal.floatValue(), coreDatatype);
						} else if (coreDatatype == CoreDatatype.XSD.DOUBLE) {
							return new NumericMemLiteral(this, label, literal.doubleValue(), coreDatatype);
						} else if (coreDatatype == CoreDatatype.XSD.BOOLEAN) {
							return new BooleanMemLiteral(this, label, literal.booleanValue());
						} else if (coreDatatype == CoreDatatype.XSD.DATETIME) {
							return new CalendarMemLiteral(this, label, coreDatatype, literal.calendarValue());
						} else if (coreDatatype == CoreDatatype.XSD.DATETIMESTAMP) {
							return new CalendarMemLiteral(this, label, coreDatatype, literal.calendarValue());
						}
					}

					return new MemLiteral(this, label, datatype, coreDatatype);

				} catch (IllegalArgumentException e) {
					// Unable to parse literal label to primitive type
					return new MemLiteral(this, label, datatype);
				}
			}
		});
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

	@Override
	public IRI createIRI(String uri) {
		return getOrCreateMemURI(super.createIRI(uri));
	}

	@Override
	public IRI createIRI(String namespace, String localName) {
		return iriRegistry.getOrAdd(SimpleValueFactory.getInstance().createIRI(namespace, localName), () -> {

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

			String sharedNamespace = namespaceRegistry.getOrAdd(correctNamespace, () -> correctNamespace);

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
	public Literal createLiteral(String value, CoreDatatype datatype) {
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

}
