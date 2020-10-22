/**
 * The RDF Model API
 * <p>
 * The core RDF model interfaces are organized in the following hierarchy:
 * </p>
 *
 * <pre>
 *        Value          Statement       Model
 *       /     \
 *      /       \
 *   Resource  Literal
 *     /  \
 *    /    \
 *  IRI   BNode
 * </pre>
 * <p>
 * An individual RDF triple or statement is represented by the {@link org.eclipse.rdf4j.model.Statement Statement}
 * interface. Collections of RDF statements are represented by the {@link org.eclipse.rdf4j.model.Model Model}
 * interface.
 * </p>
 * <p>
 * Creation of new Model elements ({@link org.eclipse.rdf4j.model.IRI IRI}, {@link org.eclipse.rdf4j.model.Literal
 * Literal}, {@link org.eclipse.rdf4j.model.BNode BNode}, {@link org.eclipse.rdf4j.model.Statement Statement}) is done
 * by means of a {@link org.eclipse.rdf4j.model.ValueFactory ValueFactory}.
 * </p>
 *
 * @see <a href="https://rdf4j.org/documentation/programming/model/">RDF4J model documentation</a>
 */
package org.eclipse.rdf4j.model;
