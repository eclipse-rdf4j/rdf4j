/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

public class RDFStarTestHelper {
	private final static ValueFactory VF = SimpleValueFactory.getInstance();

	private final static IRI GRAPH_NULL = null;
	private final static IRI GRAPH_1 = VF.createIRI("urn:graph1");
	private final static IRI GRAPH_2 = VF.createIRI("urn:graph2");
	private final static IRI IRI_1 = VF.createIRI("urn:a");
	private final static IRI IRI_2 = VF.createIRI("urn:b");
	private final static IRI IRI_3 = VF.createIRI("urn:c");
	private final static IRI IRI_4 = VF.createIRI("urn:d");
	private final static IRI IRI_5 = VF.createIRI("urn:e");
	private final static IRI IRI_6 = VF.createIRI("urn:f");
	private final static Literal LITERAL_1 = VF.createLiteral("literal 1");
	private final static Literal LITERAL_2 = VF.createLiteral("literal 2");
	private final static BNode BNODE_1 = VF.createBNode("bnode1");
	private final static BNode BNODE_2 = VF.createBNode("bnode2");

	public static Model createRDFStarModel() {
		Model rdfStarModel = new LinkedHashModel();
		Statements.create(VF, IRI_1, IRI_2, IRI_3, rdfStarModel, GRAPH_NULL);
		// The same triple repeated twice
		Statements.create(VF, IRI_1, IRI_2, VF.createTriple(IRI_4, IRI_5, LITERAL_1), rdfStarModel, GRAPH_1);
		Statements.create(VF, IRI_1, IRI_3, VF.createTriple(IRI_4, IRI_5, LITERAL_1), rdfStarModel, GRAPH_1);
		Statements.create(VF, VF.createTriple(IRI_5, IRI_6, IRI_4), IRI_3, BNODE_1, rdfStarModel, GRAPH_2);
		Statements.create(VF, VF.createTriple(VF.createTriple(IRI_1, IRI_2, LITERAL_2),
				IRI_3, VF.createTriple(IRI_4, IRI_5, IRI_6)),
				IRI_2, VF.createTriple(VF.createTriple(BNODE_2, IRI_3, LITERAL_2),
						IRI_4, VF.createTriple(IRI_3, IRI_6, IRI_1)),
				rdfStarModel, GRAPH_2);

		return rdfStarModel;
	}

	public static Model createRDFReificationModel() {
		Model reifiedModel = new LinkedHashModel();
		Statements.create(VF, IRI_1, IRI_2, IRI_3, reifiedModel, GRAPH_NULL);

		// maps iri1 iri2 <<iri4 iri5 "literal1">> to reification
		BNode t1 = VF.createBNode();
		Statements.create(VF, IRI_1, IRI_2, t1, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.SUBJECT, IRI_4, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.PREDICATE, IRI_5, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.OBJECT, LITERAL_1, reifiedModel, GRAPH_1);

		// maps iri1 iri2 <<iri4 iri5 "literal1">> to reification
		// same triple/reification statements as previous entry
		Statements.create(VF, IRI_1, IRI_3, t1, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.SUBJECT, IRI_4, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.PREDICATE, IRI_5, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.OBJECT, LITERAL_1, reifiedModel, GRAPH_1);

		// maps <<iri5 iri6 iri4>> iri3 _:bnode1 to reification
		BNode t2 = VF.createBNode();
		Statements.create(VF, t2, IRI_3, BNODE_1, reifiedModel, GRAPH_2);
		Statements.create(VF, t2, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t2, RDF.SUBJECT, IRI_5, reifiedModel, GRAPH_2);
		Statements.create(VF, t2, RDF.PREDICATE, IRI_6, reifiedModel, GRAPH_2);
		Statements.create(VF, t2, RDF.OBJECT, IRI_4, reifiedModel, GRAPH_2);

		// maps a complex nested statement to reification
		// t3
		// t5 t6
		// subj: << <<iri1 iri2 "literal2>> iri3 <<iri4 iri5 iri6>> >>
		// pred: iri2
		// obj: << <<_:bnode2 iri3 "literal2">> iri4 <<iri3 iri6 iri1>> >>
		// t7 t8
		// t4
		BNode t3 = VF.createBNode();
		BNode t4 = VF.createBNode();
		BNode t5 = VF.createBNode();
		BNode t6 = VF.createBNode();
		BNode t7 = VF.createBNode();
		BNode t8 = VF.createBNode();
		Statements.create(VF, t3, IRI_2, t4, reifiedModel, GRAPH_2);
		Statements.create(VF, t3, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t3, RDF.SUBJECT, t5, reifiedModel, GRAPH_2);
		Statements.create(VF, t3, RDF.PREDICATE, IRI_3, reifiedModel, GRAPH_2);
		Statements.create(VF, t3, RDF.OBJECT, t6, reifiedModel, GRAPH_2);
		Statements.create(VF, t5, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t5, RDF.SUBJECT, IRI_1, reifiedModel, GRAPH_2);
		Statements.create(VF, t5, RDF.PREDICATE, IRI_2, reifiedModel, GRAPH_2);
		Statements.create(VF, t5, RDF.OBJECT, LITERAL_2, reifiedModel, GRAPH_2);
		Statements.create(VF, t6, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t6, RDF.SUBJECT, IRI_4, reifiedModel, GRAPH_2);
		Statements.create(VF, t6, RDF.PREDICATE, IRI_5, reifiedModel, GRAPH_2);
		Statements.create(VF, t6, RDF.OBJECT, IRI_6, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.SUBJECT, t7, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.PREDICATE, IRI_4, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.OBJECT, t8, reifiedModel, GRAPH_2);
		Statements.create(VF, t7, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t7, RDF.SUBJECT, BNODE_2, reifiedModel, GRAPH_2);
		Statements.create(VF, t7, RDF.PREDICATE, IRI_3, reifiedModel, GRAPH_2);
		Statements.create(VF, t7, RDF.OBJECT, LITERAL_2, reifiedModel, GRAPH_2);
		Statements.create(VF, t8, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t8, RDF.SUBJECT, IRI_3, reifiedModel, GRAPH_2);
		Statements.create(VF, t8, RDF.PREDICATE, IRI_6, reifiedModel, GRAPH_2);
		Statements.create(VF, t8, RDF.OBJECT, IRI_1, reifiedModel, GRAPH_2);

		return reifiedModel;
	}

	public static Model createIncompleteRDFReificationModel() {
		Model reifiedModel = new LinkedHashModel();
		Statements.create(VF, IRI_1, IRI_2, IRI_3, reifiedModel, GRAPH_NULL);

		// maps iri1 iri2 <<iri4 iri5 "literal1">> to reification
		BNode t1 = VF.createBNode();
		Statements.create(VF, IRI_1, IRI_2, t1, reifiedModel, GRAPH_1);
		// Incomplete reification: missing t1 RDF.TYPE RDF.STATEMENT
		Statements.create(VF, t1, RDF.SUBJECT, IRI_4, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.PREDICATE, IRI_5, reifiedModel, GRAPH_1);
		Statements.create(VF, t1, RDF.OBJECT, LITERAL_1, reifiedModel, GRAPH_1);

		// maps <<iri5 iri6 iri4>> iri3 _:bnode1 to reification
		BNode t2 = VF.createBNode();
		Statements.create(VF, t2, IRI_3, BNODE_1, reifiedModel, GRAPH_2);
		Statements.create(VF, t2, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		// Incomplete reification: missing t2 RDF.SUBJECT iri5
		Statements.create(VF, t2, RDF.PREDICATE, IRI_6, reifiedModel, GRAPH_2);
		Statements.create(VF, t2, RDF.OBJECT, IRI_4, reifiedModel, GRAPH_2);

		// maps <<iri1 iri2 iri4>> iri3 iri6 to reification
		BNode t3 = VF.createBNode();
		Statements.create(VF, t3, IRI_3, IRI_6, reifiedModel, GRAPH_2);
		Statements.create(VF, t3, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t3, RDF.SUBJECT, IRI_1, reifiedModel, GRAPH_2);
		// Incomplete reification: missing t3 RDF.PREDICATE iri2
		Statements.create(VF, t3, RDF.OBJECT, IRI_4, reifiedModel, GRAPH_2);

		// maps iri6 iri3 <<iri1 iri2 "literal2">> to reification
		BNode t4 = VF.createBNode();
		Statements.create(VF, IRI_6, IRI_3, t4, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.TYPE, RDF.STATEMENT, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.SUBJECT, IRI_1, reifiedModel, GRAPH_2);
		Statements.create(VF, t4, RDF.PREDICATE, IRI_2, reifiedModel, GRAPH_2);
		// Incomplete reification: missing t4 RDF.OBJECT "literal2"

		return reifiedModel;
	}
}
