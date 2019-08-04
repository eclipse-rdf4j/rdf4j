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
 * An individual RDF triple or statement is represented by the {@link Statement} interface. Collections of RDF
 * statements are represented by the {@link Model} interface.
 * </p>
 * <p>
 * Creation of new Model elements ({@link IRI}, {@link Literal}, {@link BNode}, {@link Statement}) is done by means of a
 * {@link ValueFactory}.
 * </p>
 * 
 * @see <a href="https://rdf4j.eclipse.org/documentation/programming/model/">rdf4j model documentation</a>
 */
package org.eclipse.rdf4j.model;