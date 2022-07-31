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
package org.eclipse.rdf4j.sail.inferencer.fc;

/**
 * Constants representing the RDF+RDFS entailment rules from the RDF Semantics W3C Recommendation (10 February 2004).
 * See http://www.w3.org/TR/2004/REC-rdf-mt-20040210/ Each entailment rule in the specification has either one or two
 * premises. To allow the inferencer to distinguish triggering of rules for both premises, entailment rules that have
 * two premises are represented by two separate constants, one for each premise. An additional entailment rule, X1, is
 * added to capture list membership property assertions.
 */
class RDFSRules {

	/**
	 * rule rdf1: xxx aaa yyy --> aaa rdf:type rdf:Property
	 */
	public static final int Rdf1 = 0;

	/**
	 * rule rdfs2_1: xxx aaa yyy && (nt) aaa rdfs:domain zzz --> (t1) xxx rdf:type zzz (t2)
	 */
	public static final int Rdfs2_1 = 1;

	/**
	 * rule rdfs2_2: aaa rdfs:domain zzz && (nt) xxx aaa yyy --> (t1) xxx rdf:type zzz (t2)
	 */
	public static final int Rdfs2_2 = 2;

	/**
	 * rule rdfs3_1: xxx aaa uuu && (nt) aaa rdfs:range zzz --> (t1) uuu rdf:type zzz (t2)
	 */
	public static final int Rdfs3_1 = 3;

	/**
	 * rule rdfs3_2. aaa rdfs:range zzz && (nt) xxx aaa uuu --> (t1) uuu rdf:type zzz (t2)
	 */
	public static final int Rdfs3_2 = 4;

	/**
	 * rule rdfs4a: xxx aaa yyy --> xxx rdf:type rdfs:Resource
	 */
	public static final int Rdfs4a = 5;

	/**
	 * rule rdfs4b: xxx aaa uuu --> uuu rdf:type rdfs:Resource
	 */
	public static final int Rdfs4b = 6;

	/**
	 * rule rdfs5_1: aaa rdfs:subPropertyOf bbb && (nt) bbb rdfs:subPropertyOf ccc --> (t1) aaa rdfs:subPropertyOf ccc
	 * (t2) transitivity of rdfs:subPropertyOf
	 */
	public static final int Rdfs5_1 = 7;

	/**
	 * rule rdfs5_2: bbb rdfs:subPropertyOf ccc && (nt) aaa rdfs:subPropertyOf bbb --> (t1) aaa rdfs:subPropertyOf ccc
	 * (t2) transitivity of rdfs:subPropertyOf
	 */
	public static final int Rdfs5_2 = 8;

	/**
	 * rule rdfs6: xxx rdf:type rdf:Property --> xxx rdfs:subPropertyOf xxx reflexivity of rdfs:subPropertyOf
	 */
	public static final int Rdfs6 = 9;

	/**
	 * rule rdfs7_1: xxx aaa yyy && (nt) aaa rdfs:subPropertyOf bbb --> (t1) xxx bbb yyy (t2)
	 */
	public static final int Rdfs7_1 = 10;

	/**
	 * rule rdfs7_2: aaa rdfs:subPropertyOf bbb && (nt) xxx aaa yyy --> (t1) xxx bbb yyy (t2)
	 */
	public static final int Rdfs7_2 = 11;

	/**
	 * rule rdfs8: xxx rdf:type rdfs:Class --> xxx rdfs:subClassOf rdfs:Resource
	 */
	public static final int Rdfs8 = 12;

	/**
	 * rule rdfs9_1: xxx rdfs:subClassOf yyy && (nt) aaa rdf:type xxx --> (t1) aaa rdf:type yyy (t2)
	 */
	public static final int Rdfs9_1 = 13;

	/**
	 * rule rdfs9_2: aaa rdf:type xxx && (nt) xxx rdfs:subClassOf yyy --> (t1) aaa rdf:type yyy (t2)
	 */
	public static final int Rdfs9_2 = 14;

	/**
	 * rule rdfs10: xxx rdf:type rdfs:Class --> xxx rdfs:subClassOf xxx reflexivity of rdfs:subClassOf
	 */
	public static final int Rdfs10 = 15;

	/**
	 * rule rdfs11_1: xxx rdfs:subClassOf yyy && (nt) yyy rdfs:subClassOf zzz --> (t1) xxx rdfs:subClassOf zzz (t2)
	 * transitivity of rdfs:subClassOf
	 */
	public static final int Rdfs11_1 = 16;

	/**
	 * rule rdfs11_2: yyy rdfs:subClassOf zzz && (nt) xxx rdfs:subClassOf yyy --> (t1) xxx rdfs:subClassOf zzz (t2)
	 * transitivity of rdfs:subClassOf
	 */
	public static final int Rdfs11_2 = 17;

	/**
	 * rule rdfs12: xxx rdf:type rdfs:ContainerMembershipProperty --> xxx rdfs:subPropertyOf rdfs:member
	 */
	public static final int Rdfs12 = 18;

	/**
	 * rule rdfs13: xxx rdf:type rdfs:Datatype --> xxx rdfs:subClassOf rdfs:Literal
	 */
	public static final int Rdfs13 = 19;

	/**
	 * rule X1: xxx rdf:_* yyy --> rdf:_* rdf:type rdfs:ContainerMembershipProperty This is an extra rule for list
	 * membership properties (_1, _2, _3, ...). The RDF Semantics Recommendation does not specificy a production for
	 * this, instead these statements are considered axiomatic. Since it is an infinite set it can not, in practice, be
	 * asserted axiomatically.
	 */
	public static final int RX1 = 20;

	public static final int RULECOUNT = 21;

	public static final String[] RULENAMES = { "   Rdf1", " Rdfs2_1", " Rdfs2_2", " Rdfs3_1", " Rdfs3_2", "  Rdfs4a",
			"  Rdfs4b", "Rdfs5_1", "Rdfs5_2", "  Rdfs6", " Rdfs7_1", " Rdfs7_2", "  Rdfs8", " Rdfs9_1", " Rdfs9_2",
			"  Rdfs10", " Rdfs11_1", " Rdfs11_2", "  Rdfs12", "  Rdfs13", "  RX1" };

	private static final boolean __ = false;

	private static final boolean XX = true;

	/**
	 * Table of triggers for entailment rules. Each column represents the triggers for an entailment rule R, that is, it
	 * encodes which entailment rules produces statements that can be used as a premise in rule R. Example: the
	 * conclusion of rule rdfs2_1 is a statement of the form: (xxx rdf:type yyy). The premise of rule rdfs9_2 is (xxx
	 * rdf:type yyy). Hence, rule rdfs2_1 triggers rule rdfs9_2.
	 */
	public static final boolean[][] TRIGGERS = {
			// 1 2_2 3_2 4b 5_2 7_1 8 9_2 11_1 12 X1
			// 2_1 3_1 4a 5_1 6 7_2 9_1 10 11_2 13
			{ __, XX, __, XX, __, XX, __, __, __, XX, XX, __, __, __, XX, __, __, __, __, __, __ }, // 1
			{ __, XX, __, XX, __, __, __, __, __, XX, XX, __, XX, __, XX, XX, __, __, XX, XX, __ }, // 2_1
			{ __, XX, __, XX, __, __, __, __, __, XX, XX, __, XX, __, XX, XX, __, __, XX, XX, __ }, // 2_2
			{ __, XX, __, XX, __, __, __, __, __, XX, XX, __, XX, __, XX, XX, __, __, XX, XX, __ }, // 3_1
			{ __, XX, __, XX, __, __, __, __, __, XX, XX, __, XX, __, XX, XX, __, __, XX, XX, __ }, // 3_2
			{ __, XX, __, XX, __, __, __, __, __, __, XX, __, __, __, XX, __, __, __, __, __, __ }, // 4a
			{ __, XX, __, XX, __, __, __, __, __, __, XX, __, __, __, XX, __, __, __, __, __, __ }, // 4b
			{ __, __, __, __, __, __, __, XX, XX, __, XX, XX, __, __, __, __, __, __, __, __, __ }, // 51
			{ __, __, __, __, __, __, __, XX, XX, __, XX, XX, __, __, __, __, __, __, __, __, __ }, // 52
			{ __, XX, __, XX, __, __, __, __, __, __, XX, XX, __, __, __, __, __, __, __, __, __ }, // 6
			{ __, XX, XX, XX, XX, __, __, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX }, // 7_1
			{ __, XX, XX, XX, XX, __, __, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX, XX }, // 7_2
			{ __, XX, __, XX, __, __, __, __, __, __, XX, __, __, XX, __, __, XX, XX, __, __, __ }, // 8
			{ __, __, __, XX, __, __, __, __, __, XX, XX, __, XX, __, XX, XX, __, __, XX, XX, __ }, // 9_1
			{ __, __, __, XX, __, __, __, __, __, XX, XX, __, XX, __, XX, XX, __, __, XX, XX, __ }, // 9_2
			{ __, XX, __, XX, __, __, __, __, __, __, XX, __, __, __, __, __, __, __, __, __, __ }, // 10
			{ __, __, __, __, __, __, __, __, __, __, XX, __, __, XX, __, __, XX, XX, __, __, __ }, // 11_1
			{ __, __, __, __, __, __, __, __, __, __, XX, __, __, XX, __, __, XX, XX, __, __, __ }, // 11_2
			{ __, XX, __, XX, __, __, XX, XX, XX, __, XX, XX, __, __, __, __, __, __, __, __, __ }, // 12
			{ __, XX, __, XX, __, __, __, __, __, __, XX, __, __, XX, __, __, XX, XX, __, __, __ }, // 13
			{ __, XX, __, XX, __, __, __, __, __, __, XX, __, __, __, XX, __, __, __, XX, __, __ },// X1
	};
}
