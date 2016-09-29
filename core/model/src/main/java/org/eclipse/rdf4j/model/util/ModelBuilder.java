/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.util;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

/**
 * Builder to facilitate easier creation of new RDF {@link Model} objects via a fluent interface. All methods
 * returning a {@link ModelBuilder} return an immutable reference to the current object, allowing method
 * chaining.
 * <p>
 * Usage example:
 * 
 * <pre>
 * <code>
 *    ModelBuilder builder = new ModelBuilder();
 *    
 *    // set some namespaces 
 *    builder.setNamespace("ex", "http://example.org/").setNamespace(FOAF.NS);
 *    
 *    // add a new named graph to the model
 *    builder.namedGraph("ex:graph1")
 *               // add statements about resource ex:john
 *              .subject("ex:john")                      
 *           	  .add(FOAF.NAME, "John") // add the triple (ex:john, foaf:name "John") to the named graph
 *           	  .add(FOAF.AGE, 42)
 *           	  .add(FOAF.MBOX, "john@example.org");
 *           
 *     // add a triple to the default graph
 *    builder.defaultGraph().subject("ex:graph1").add(RDF.TYPE, "ex:Graph");
 *    
 *    // return the Model object
 *    Model m = builder.build();
 * </code>
 * </pre>
 * 
 * @author Jeen Broekstra
 */
public class ModelBuilder {

	private final Model model;

	private Resource currentSubject;

	private Resource currentNamedGraph;

	/**
	 * Create a new {@link ModelBuilder}.
	 */
	public ModelBuilder() {
		this(new LinkedHashModel());
	}

	/**
	 * Create a new {@link ModelBuilder} which will append to the supplied {@link Model}.
	 */
	public ModelBuilder(Model model) {
		this.model = model;
	}

	/**
	 * Set the supplied {@link Namespace} mapping.
	 * 
	 * @param ns
	 *        a {@link Namespace} to add to the model
	 * @return the {@link ModelBuilder}
	 */
	public ModelBuilder setNamespace(Namespace ns) {
		model.setNamespace(ns);
		return this;
	}

	/**
	 * Set the namespace mapping defined by the supplied prefix and name
	 * 
	 * @param prefix
	 *        prefix of the namespace to add to the model.
	 * @param namespace
	 *        namespace name to add to the model.
	 * @return the {@link ModelBuilder}
	 */
	public ModelBuilder setNamespace(String prefix, String namespace) {
		model.setNamespace(new SimpleNamespace(prefix, namespace));
		return this;
	}

	/**
	 * Set the subject resource about which statements are to be added to the model.
	 * 
	 * @param subject
	 *        the subject resource about which statements are to be added.
	 * @return the {@link ModelBuilder}
	 */
	public ModelBuilder subject(Resource subject) {
		this.currentSubject = subject;
		return this;
	}

	/**
	 * Set the subject about which statements are to be added to the model, defined by a prefixed name or an
	 * IRI reference.
	 * 
	 * @param subject
	 *        the subject resource about which statements are to be added. This can be defined either as a
	 *        prefixed name string (e.g. "ex:john"), or as a full IRI (e.g. "http://example.org/john"). If
	 *        supplied as a prefixed name, the {@link ModelBuilder} will need to have a namespace mapping for
	 *        the prefix.
	 * @return the {@link ModelBuilder}
	 */
	public ModelBuilder subject(String prefixedNameOrIri) {
		return subject(mapToIRI(prefixedNameOrIri));
	}

	/**
	 * Set the current graph in which to add new statements to the supplied named graph. This method resets
	 * the current subject.
	 * 
	 * @param namedGraph
	 *        a named graph identifier
	 * @return this {@link ModelBuilder}
	 */
	public ModelBuilder namedGraph(Resource namedGraph) {
		this.currentSubject = null;
		this.currentNamedGraph = namedGraph;
		return this;
	}

	/**
	 * Set the current graph in which to add new statements to the supplied named graph. This method clears
	 * the current subject.
	 * 
	 * @param namedGraph
	 *        a named graph identifier. This can be defined either as a prefixed name string (e.g. "ex:john"),
	 *        or as a full IRI (e.g. "http://example.org/john"). If supplied as a prefixed name, the
	 *        {@link ModelBuilder} will need to have a namespace mapping for the prefix.
	 * @return this {@link ModelBuilder}
	 */
	public ModelBuilder namedGraph(String prefixedNameOrIRI) {
		return namedGraph(mapToIRI(prefixedNameOrIRI));
	}

	/**
	 * Set the current graph in which to add new statements to the default graph. This method clears the
	 * current subject.
	 * 
	 * @return this {@link ModelBuilder}
	 */
	public ModelBuilder defaultGraph() {
		this.currentSubject = null;
		this.currentNamedGraph = null;
		return this;
	}

	/**
	 * Add an RDF statement with the given subject, predicate and object to the model, using the current graph
	 * (either named or default).
	 * 
	 * @param subject
	 *        the statement's subject
	 * @param predicate
	 *        the statement's predicate
	 * @param object
	 *        the statement's object. If the supplied object is a {@link BNode}, {@link IRI}, or
	 *        {@link Literal}, the object is used directly. Otherwise this method creates a typed
	 *        {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 *        appropriate XML Schema type. If no mapping is available, the method creates a literal with the
	 *        string representation of the supplied object as the value, and {@link XMLSchema#STRING} as the
	 *        datatype. Recognized types are {@link Boolean} , {@link Byte}, {@link Double}, {@link Float},
	 *        {@link Integer}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar } , and {@link Date}.
	 * @return this {@link ModelBuilder}
	 * @see #namedGraph(Resource)
	 * @see #defaultGraph()
	 * @see Literals#createLiteral(ValueFactory, Object)
	 */
	public ModelBuilder add(Resource subject, IRI predicate, Object object) {
		model.setNamespace(XMLSchema.NS);

		final Value objectValue = (object instanceof Value) ? (Value)object
				: Literals.createLiteral(SimpleValueFactory.getInstance(), object);
		if (currentNamedGraph != null) {
			model.add(subject, predicate, objectValue, currentNamedGraph);
		}
		else {
			model.add(subject, predicate, objectValue);
		}
		return this;
	}

	/**
	 * Add an RDF statement with the given subject, predicate and object to the model, using the current graph
	 * (either named or default).
	 * 
	 * @param subject
	 *        the statement's subject. This can be defined either as a prefixed name string (e.g. "ex:john"),
	 *        or as a full IRI (e.g. "http://example.org/john"). If supplied as a prefixed name, the
	 *        {@link ModelBuilder} will need to have a namespace mapping for the prefix.
	 * @param predicate
	 *        the statement's predicate
	 * @param object
	 *        the statement's object. Creates a typed {@link Literal} out of the supplied object, mapping the
	 *        runtime type of the object to the appropriate XML Schema type. If no mapping is available, the
	 *        method creates a literal with the string representation of the supplied object as the value, and
	 *        {@link XMLSchema#STRING} as the datatype. Recognized types are {@link Boolean} , {@link Byte},
	 *        {@link Double}, {@link Float}, {@link Integer}, {@link Long}, {@link Short},
	 *        {@link XMLGregorianCalendar } , and {@link Date}.
	 * @return this {@link ModelBuilder}
	 * @see #namedGraph(Resource)
	 * @see #defaultGraph()
	 * @see Literals#createLiteral(ValueFactory, Object)
	 */
	public ModelBuilder add(String subject, IRI predicate, Object object) {
		return add(mapToIRI(subject), predicate, object);
	}

	/**
	 * Add an RDF statement with the given subject, predicate and object to the model, using the current graph
	 * (either named or default).
	 * 
	 * @param subject
	 *        the statement's subject. This can be defined either as a prefixed name string (e.g. "ex:john"),
	 *        or as a full IRI (e.g. "http://example.org/john"). If supplied as a prefixed name, the
	 *        {@link ModelBuilder} will need to have a namespace mapping for the prefix.
	 * @param predicate
	 *        the statement's predicate. This can be defined either as a prefixed name string (e.g.
	 *        "ex:john"), or as a full IRI (e.g. "http://example.org/john"). If supplied as a prefixed name,
	 *        the {@link ModelBuilder} will need to have a namespace mapping for the prefix.
	 * @param object
	 *        the statement's object. If the supplied object is a {@link BNode}, {@link IRI}, or
	 *        {@link Literal}, the object is used directly. Otherwise, this method creates a typed
	 *        {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 *        appropriate XML Schema type. If no mapping is available, the method creates a literal with the
	 *        string representation of the supplied object as the value, and {@link XMLSchema#STRING} as the
	 *        datatype. Recognized types are {@link Boolean} , {@link Byte}, {@link Double}, {@link Float},
	 *        {@link Integer}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar } , and {@link Date}.
	 * @return this {@link ModelBuilder}
	 * @see #namedGraph(Resource)
	 * @see #defaultGraph()
	 * @see Literals#createLiteral(ValueFactory, Object)
	 */
	public ModelBuilder add(String subject, String predicate, Object object) {
		return add(mapToIRI(subject), mapToIRI(predicate), object);
	}

	/**
	 * Add an RDF statement with the predicate and object to the model, using the current subject and graph
	 * (either named or default).
	 * 
	 * @param predicate
	 *        the statement's predicate.
	 * @param object
	 *        the statement's object. If the supplied object is a {@link BNode}, {@link IRI}, or
	 *        {@link Literal}, the object is used directly. Otherwise, this method creates a typed
	 *        {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 *        appropriate XML Schema type. If no mapping is available, the method creates a literal with the
	 *        string representation of the supplied object as the value, and {@link XMLSchema#STRING} as the
	 *        datatype. Recognized types are {@link Boolean} , {@link Byte}, {@link Double}, {@link Float},
	 *        {@link Integer}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar } , and {@link Date}.
	 * @return this {@link ModelBuilder}
	 * @throws ModelException
	 *         if the current subject is not set using {@link #subject(Resource)} or {@link #subject(String)}.
	 */
	public ModelBuilder add(IRI predicate, Object object) {
		if (currentSubject == null) {
			throw new ModelException("subject not set");
		}
		return add(currentSubject, predicate, object);
	}

	/**
	 * Add an RDF statement with the predicate and object to the model, using the current subject and graph
	 * (either named or default).
	 * 
	 * @param predicate
	 *        the statement's predicate. This can be defined either as a prefixed name string (e.g.
	 *        "ex:john"), or as a full IRI (e.g. "http://example.org/john"). If supplied as a prefixed name,
	 *        the {@link ModelBuilder} will need to have a namespace mapping for the prefix.
	 * @param object
	 *        the statement's object. If the supplied object is a {@link BNode}, {@link IRI}, or
	 *        {@link Literal}, the object is used directly. Otherwise this method creates a typed
	 *        {@link Literal} out of the supplied object, mapping the runtime type of the object to the
	 *        appropriate XML Schema type. If no mapping is available, the method creates a literal with the
	 *        string representation of the supplied object as the value, and {@link XMLSchema#STRING} as the
	 *        datatype. Recognized types are {@link Boolean} , {@link Byte}, {@link Double}, {@link Float},
	 *        {@link Integer}, {@link Long}, {@link Short}, {@link XMLGregorianCalendar } , and {@link Date}.
	 * @return this {@link ModelBuilder}
	 * @throws ModelException
	 *         if the current subject is not set using {@link #subject(Resource)} or {@link #subject(String)}.
	 */
	public ModelBuilder add(String predicate, Object object) {
		return add(mapToIRI(predicate), object);
	}

	/**
	 * Return the created {@link Model}
	 * 
	 * @return the {@link Model}
	 */
	public Model build() {
		return model;
	}

	private IRI mapToIRI(String prefixedNameOrIRI) {
		if (prefixedNameOrIRI.indexOf(':') < 0) {
			throw new ModelException("invalid prefixed name or IRI: " + prefixedNameOrIRI);
		}

		final String prefix = prefixedNameOrIRI.substring(0, prefixedNameOrIRI.indexOf(':'));

		final ValueFactory vf = SimpleValueFactory.getInstance();

		for (Namespace ns : model.getNamespaces()) {
			if (prefix.equals(ns.getPrefix())) {
				return vf.createIRI(ns.getName(),
						prefixedNameOrIRI.substring(prefixedNameOrIRI.indexOf(':') + 1));
			}
		}

		// try mapping using some of the default / well-known namespaces
		for (Namespace ns : getDefaultNamespaces()) {
			if (prefix.equals(ns.getPrefix())) {
				model.setNamespace(ns);
				return vf.createIRI(ns.getName(),
						prefixedNameOrIRI.substring(prefixedNameOrIRI.indexOf(':') + 1));
			}
		}
		return vf.createIRI(prefixedNameOrIRI);
	}

	private Namespace[] getDefaultNamespaces() {
		return new Namespace[] { RDF.NS, RDFS.NS, OWL.NS, XMLSchema.NS, DCTERMS.NS, DC.NS, FOAF.NS, SKOS.NS };
	}

}
