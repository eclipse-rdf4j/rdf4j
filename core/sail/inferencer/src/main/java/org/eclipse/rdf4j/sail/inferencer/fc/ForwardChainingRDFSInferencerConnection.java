/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forward-chaining RDF Schema inferencer, using the rules from the
 * <a href="http://www.w3.org/TR/2004/REC-rdf-mt-20040210/">RDF Semantics Recommendation (10 February 2004)</a>. This
 * inferencer can be used to add RDF Schema semantics to any Sail that returns {@link InferencerConnection}s from their
 * {@link Sail#getConnection()} method.
 *
 * @deprecated since 2.5. This inferencer implementation will be phased out. Consider switching to the
 *             {@link SchemaCachingRDFSInferencer} instead.
 */
@Deprecated
class ForwardChainingRDFSInferencerConnection extends AbstractForwardChainingInferencerConnection {

	static private final Logger logger = LoggerFactory.getLogger(ForwardChainingRDFSInferencerConnection.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	private Model newThisIteration;

	/**
	 * Flags indicating which rules should be evaluated.
	 */
	private final boolean[] checkRule = new boolean[RDFSRules.RULECOUNT];

	/**
	 * Flags indicating which rules should be evaluated next iteration.
	 */
	private final boolean[] checkRuleNextIter = new boolean[RDFSRules.RULECOUNT];

	/**
	 * The number of inferred statements per rule.
	 */
	private final int[] ruleCount = new int[RDFSRules.RULECOUNT];

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ForwardChainingRDFSInferencerConnection(Sail sail, InferencerConnection con) {
		super(sail, con);
	}

	/*---------*
	 * Methods *
	 *---------*/

	// Called by base sail
	@Override
	protected Model createModel() {
		return new DynamicModelFactory().createEmptyModel();
	}

	/**
	 * Adds all basic set of axiom statements from which the complete set can be inferred to the underlying Sail.
	 */
	@Override
	protected void addAxiomStatements() throws SailException {
		logger.debug("Inserting axiom statements");

		// RDF axiomatic triples (from RDF Semantics, section 3.1):

		addInferredStatement(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		addInferredStatement(RDF.SUBJECT, RDF.TYPE, RDF.PROPERTY);
		addInferredStatement(RDF.PREDICATE, RDF.TYPE, RDF.PROPERTY);
		addInferredStatement(RDF.OBJECT, RDF.TYPE, RDF.PROPERTY);

		addInferredStatement(RDF.FIRST, RDF.TYPE, RDF.PROPERTY);
		addInferredStatement(RDF.REST, RDF.TYPE, RDF.PROPERTY);
		addInferredStatement(RDF.VALUE, RDF.TYPE, RDF.PROPERTY);

		addInferredStatement(RDF.NIL, RDF.TYPE, RDF.LIST);

		// RDFS axiomatic triples (from RDF Semantics, section 4.1):

		addInferredStatement(RDF.TYPE, RDFS.DOMAIN, RDFS.RESOURCE);
		addInferredStatement(RDFS.DOMAIN, RDFS.DOMAIN, RDF.PROPERTY);
		addInferredStatement(RDFS.RANGE, RDFS.DOMAIN, RDF.PROPERTY);
		addInferredStatement(RDFS.SUBPROPERTYOF, RDFS.DOMAIN, RDF.PROPERTY);
		addInferredStatement(RDFS.SUBCLASSOF, RDFS.DOMAIN, RDFS.CLASS);
		addInferredStatement(RDF.SUBJECT, RDFS.DOMAIN, RDF.STATEMENT);
		addInferredStatement(RDF.PREDICATE, RDFS.DOMAIN, RDF.STATEMENT);
		addInferredStatement(RDF.OBJECT, RDFS.DOMAIN, RDF.STATEMENT);
		addInferredStatement(RDFS.MEMBER, RDFS.DOMAIN, RDFS.RESOURCE);
		addInferredStatement(RDF.FIRST, RDFS.DOMAIN, RDF.LIST);
		addInferredStatement(RDF.REST, RDFS.DOMAIN, RDF.LIST);
		addInferredStatement(RDFS.SEEALSO, RDFS.DOMAIN, RDFS.RESOURCE);
		addInferredStatement(RDFS.ISDEFINEDBY, RDFS.DOMAIN, RDFS.RESOURCE);
		addInferredStatement(RDFS.COMMENT, RDFS.DOMAIN, RDFS.RESOURCE);
		addInferredStatement(RDFS.LABEL, RDFS.DOMAIN, RDFS.RESOURCE);
		addInferredStatement(RDF.VALUE, RDFS.DOMAIN, RDFS.RESOURCE);

		addInferredStatement(RDF.TYPE, RDFS.RANGE, RDFS.CLASS);
		addInferredStatement(RDFS.DOMAIN, RDFS.RANGE, RDFS.CLASS);
		addInferredStatement(RDFS.RANGE, RDFS.RANGE, RDFS.CLASS);
		addInferredStatement(RDFS.SUBPROPERTYOF, RDFS.RANGE, RDF.PROPERTY);
		addInferredStatement(RDFS.SUBCLASSOF, RDFS.RANGE, RDFS.CLASS);
		addInferredStatement(RDF.SUBJECT, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDF.PREDICATE, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDF.OBJECT, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDFS.MEMBER, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDF.FIRST, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDF.REST, RDFS.RANGE, RDF.LIST);
		addInferredStatement(RDFS.SEEALSO, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDFS.ISDEFINEDBY, RDFS.RANGE, RDFS.RESOURCE);
		addInferredStatement(RDFS.COMMENT, RDFS.RANGE, RDFS.LITERAL);
		addInferredStatement(RDFS.LABEL, RDFS.RANGE, RDFS.LITERAL);
		addInferredStatement(RDF.VALUE, RDFS.RANGE, RDFS.RESOURCE);

		addInferredStatement(RDF.ALT, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		addInferredStatement(RDF.BAG, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		addInferredStatement(RDF.SEQ, RDFS.SUBCLASSOF, RDFS.CONTAINER);
		addInferredStatement(RDFS.CONTAINERMEMBERSHIPPROPERTY, RDFS.SUBCLASSOF, RDF.PROPERTY);

		addInferredStatement(RDFS.ISDEFINEDBY, RDFS.SUBPROPERTYOF, RDFS.SEEALSO);

		addInferredStatement(RDF.XMLLITERAL, RDF.TYPE, RDFS.DATATYPE);
		addInferredStatement(RDF.XMLLITERAL, RDFS.SUBCLASSOF, RDFS.LITERAL);
		addInferredStatement(RDFS.DATATYPE, RDFS.SUBCLASSOF, RDFS.CLASS);
	}

	@Override
	protected void doInferencing() throws SailException {
		// All rules need to be checked:
		for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
			ruleCount[i] = 0;
			checkRuleNextIter[i] = true;
		}

		super.doInferencing();

		// Print some statistics
		logger.debug("---RdfMTInferencer statistics:---");
		logger.debug("total statements inferred = " + totalInferred);
		for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
			logger.debug("rule " + RDFSRules.RULENAMES[i] + ":\t#inferred=" + ruleCount[i]);
		}
		logger.debug("---end of statistics:---");
	}

	@Override
	protected int applyRules(Model iteration) throws SailException {
		newThisIteration = iteration;
		int nofInferred = 0;
		nofInferred += applyRule(RDFSRules.Rdf1);
		nofInferred += applyRule(RDFSRules.Rdfs2_1);
		nofInferred += applyRule(RDFSRules.Rdfs2_2);
		nofInferred += applyRule(RDFSRules.Rdfs3_1);
		nofInferred += applyRule(RDFSRules.Rdfs3_2);
		nofInferred += applyRule(RDFSRules.Rdfs4a);
		nofInferred += applyRule(RDFSRules.Rdfs4b);
		nofInferred += applyRule(RDFSRules.Rdfs5_1);
		nofInferred += applyRule(RDFSRules.Rdfs5_2);
		nofInferred += applyRule(RDFSRules.Rdfs6);
		nofInferred += applyRule(RDFSRules.Rdfs7_1);
		nofInferred += applyRule(RDFSRules.Rdfs7_2);
		nofInferred += applyRule(RDFSRules.Rdfs8);
		nofInferred += applyRule(RDFSRules.Rdfs9_1);
		nofInferred += applyRule(RDFSRules.Rdfs9_2);
		nofInferred += applyRule(RDFSRules.Rdfs10);
		nofInferred += applyRule(RDFSRules.Rdfs11_1);
		nofInferred += applyRule(RDFSRules.Rdfs11_2);
		nofInferred += applyRule(RDFSRules.Rdfs12);
		nofInferred += applyRule(RDFSRules.Rdfs13);
		nofInferred += applyRule(RDFSRules.RX1);
		newThisIteration = null;
		return nofInferred;
	}

	@Override
	protected Model prepareIteration() {
		for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
			checkRule[i] = checkRuleNextIter[i];

			// reset for next iteration:
			checkRuleNextIter[i] = false;
		}
		return super.prepareIteration();
	}

	protected void updateTriggers(int ruleNo, int nofInferred) {
		if (nofInferred > 0) {
			ruleCount[ruleNo] += nofInferred;

			// Check which rules are triggered by this one.
			boolean[] triggers = RDFSRules.TRIGGERS[ruleNo];

			for (int i = 0; i < RDFSRules.RULECOUNT; i++) {
				if (triggers[i] == true) {
					checkRuleNextIter[i] = true;
				}
			}
		}
	}

	protected int applyRule(int rule) throws SailException {
		if (!checkRule[rule]) {
			return 0;
		}
		int nofInferred;

		nofInferred = applyRuleInternal(rule);

		updateTriggers(rule, nofInferred);

		return nofInferred;
	}

	protected int applyRuleInternal(int rule) throws SailException {
		int result;

		switch (rule) {
		case RDFSRules.Rdf1:
			result = applyRuleRdf1();
			break;
		case RDFSRules.Rdfs2_1:
			result = applyRuleRdfs2_1();
			break;
		case RDFSRules.Rdfs2_2:
			result = applyRuleRdfs2_2();
			break;
		case RDFSRules.Rdfs3_1:
			result = applyRuleRdfs3_1();
			break;
		case RDFSRules.Rdfs3_2:
			result = applyRuleRdfs3_2();
			break;
		case RDFSRules.Rdfs4a:
			result = applyRuleRdfs4a();
			break;
		case RDFSRules.Rdfs4b:
			result = applyRuleRdfs4b();
			break;
		case RDFSRules.Rdfs5_1:
			result = applyRuleRdfs5_1();
			break;
		case RDFSRules.Rdfs5_2:
			result = applyRuleRdfs5_2();
			break;
		case RDFSRules.Rdfs6:
			result = applyRuleRdfs6();
			break;
		case RDFSRules.Rdfs7_1:
			result = applyRuleRdfs7_1();
			break;
		case RDFSRules.Rdfs7_2:
			result = applyRuleRdfs7_2();
			break;
		case RDFSRules.Rdfs8:
			result = applyRuleRdfs8();
			break;
		case RDFSRules.Rdfs9_1:
			result = applyRuleRdfs9_1();
			break;
		case RDFSRules.Rdfs9_2:
			result = applyRuleRdfs9_2();
			break;
		case RDFSRules.Rdfs10:
			result = applyRuleRdfs10();
			break;
		case RDFSRules.Rdfs11_1:
			result = applyRuleRdfs11_1();
			break;
		case RDFSRules.Rdfs11_2:
			result = applyRuleRdfs11_2();
			break;
		case RDFSRules.Rdfs12:
			result = applyRuleRdfs12();
			break;
		case RDFSRules.Rdfs13:
			result = applyRuleRdfs13();
			break;
		case RDFSRules.RX1:
			result = applyRuleX1();
			break;
		default:
			throw new AssertionError("Unexpected rule: " + rule);
		}
		// ThreadLog.trace("Rule " + RDFSRules.RULENAMES[rule] + " inferred " +
		// result + " new triples.");
		return result;
	}

	// xxx aaa yyy --> aaa rdf:type rdf:Property
	private int applyRuleRdf1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, null, null);

		for (Statement st : iter) {
			boolean added = addInferredStatement(st.getPredicate(), RDF.TYPE, RDF.PROPERTY);

			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx aaa yyy (nt) && aaa rdfs:domain zzz (t1) --> xxx rdf:type zzz
	private int applyRuleRdfs2_1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, null, null);

		for (Statement nt : ntIter) {
			Resource xxx = nt.getSubject();
			IRI aaa = nt.getPredicate();

			CloseableIteration<? extends Statement, SailException> t1Iter;
			t1Iter = getWrappedConnection().getStatements(aaa, RDFS.DOMAIN, null, true);

			while (t1Iter.hasNext()) {
				Statement t1 = t1Iter.next();

				Value zzz = t1.getObject();
				if (zzz instanceof Resource) {
					boolean added = addInferredStatement(xxx, RDF.TYPE, zzz);
					if (added) {
						nofInferred++;
					}
				}
			}
			t1Iter.close();
		}

		return nofInferred;
	}

	// aaa rdfs:domain zzz (nt) && xxx aaa yyy (t1) --> xxx rdf:type zzz
	private int applyRuleRdfs2_2() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.DOMAIN, null);

		for (Statement nt : ntIter) {
			Resource aaa = nt.getSubject();
			Value zzz = nt.getObject();

			if (aaa instanceof IRI && zzz instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(null, (IRI) aaa, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Resource xxx = t1.getSubject();
					boolean added = addInferredStatement(xxx, RDF.TYPE, zzz);
					if (added) {
						nofInferred++;
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// xxx aaa uuu (nt) && aaa rdfs:range zzz (t1) --> uuu rdf:type zzz
	private int applyRuleRdfs3_1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, null, null);

		for (Statement nt : ntIter) {
			IRI aaa = nt.getPredicate();
			Value uuu = nt.getObject();

			if (uuu instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(aaa, RDFS.RANGE, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Value zzz = t1.getObject();
					if (zzz instanceof Resource) {
						boolean added = addInferredStatement((Resource) uuu, RDF.TYPE, zzz);
						if (added) {
							nofInferred++;
						}
					}
				}
				t1Iter.close();
			}
		}
		return nofInferred;
	}

	// aaa rdfs:range zzz (nt) && xxx aaa uuu (t1) --> uuu rdf:type zzz
	private int applyRuleRdfs3_2() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.RANGE, null);

		for (Statement nt : ntIter) {
			Resource aaa = nt.getSubject();
			Value zzz = nt.getObject();

			if (aaa instanceof IRI && zzz instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(null, (IRI) aaa, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Value uuu = t1.getObject();
					if (uuu instanceof Resource) {
						boolean added = addInferredStatement((Resource) uuu, RDF.TYPE, zzz);
						if (added) {
							nofInferred++;
						}
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;

	}

	// xxx aaa yyy --> xxx rdf:type rdfs:Resource
	private int applyRuleRdfs4a() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, null, null);

		for (Statement st : iter) {
			boolean added = addInferredStatement(st.getSubject(), RDF.TYPE, RDFS.RESOURCE);
			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx aaa uuu --> uuu rdf:type rdfs:Resource
	private int applyRuleRdfs4b() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, null, null);

		for (Statement st : iter) {
			Value uuu = st.getObject();
			if (uuu instanceof Resource) {
				boolean added = addInferredStatement((Resource) uuu, RDF.TYPE, RDFS.RESOURCE);
				if (added) {
					nofInferred++;
				}
			}
		}

		return nofInferred;
	}

	// aaa rdfs:subPropertyOf bbb (nt) && bbb rdfs:subPropertyOf ccc (t1)
	// --> aaa rdfs:subPropertyOf ccc
	private int applyRuleRdfs5_1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.SUBPROPERTYOF, null);

		for (Statement nt : ntIter) {
			Resource aaa = nt.getSubject();
			Value bbb = nt.getObject();

			if (bbb instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements((Resource) bbb, RDFS.SUBPROPERTYOF, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Value ccc = t1.getObject();
					if (ccc instanceof Resource) {
						boolean added = addInferredStatement(aaa, RDFS.SUBPROPERTYOF, ccc);
						if (added) {
							nofInferred++;
						}
					}
				}
				t1Iter.close();

			}
		}

		return nofInferred;
	}

	// bbb rdfs:subPropertyOf ccc (nt) && aaa rdfs:subPropertyOf bbb (t1)
	// --> aaa rdfs:subPropertyOf ccc
	private int applyRuleRdfs5_2() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.SUBPROPERTYOF, null);

		for (Statement nt : ntIter) {
			Resource bbb = nt.getSubject();
			Value ccc = nt.getObject();

			if (ccc instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(null, RDFS.SUBPROPERTYOF, bbb, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Resource aaa = t1.getSubject();
					boolean added = addInferredStatement(aaa, RDFS.SUBPROPERTYOF, ccc);
					if (added) {
						nofInferred++;
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// xxx rdf:type rdf:Property --> xxx rdfs:subPropertyOf xxx
	private int applyRuleRdfs6() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, RDF.TYPE, RDF.PROPERTY);

		for (Statement st : iter) {
			Resource xxx = st.getSubject();
			boolean added = addInferredStatement(xxx, RDFS.SUBPROPERTYOF, xxx);
			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx aaa yyy (nt) && aaa rdfs:subPropertyOf bbb (t1) --> xxx bbb yyy
	private int applyRuleRdfs7_1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, null, null);

		for (Statement nt : ntIter) {
			Resource xxx = nt.getSubject();
			IRI aaa = nt.getPredicate();
			Value yyy = nt.getObject();

			CloseableIteration<? extends Statement, SailException> t1Iter;
			t1Iter = getWrappedConnection().getStatements(aaa, RDFS.SUBPROPERTYOF, null, true);

			while (t1Iter.hasNext()) {
				Statement t1 = t1Iter.next();

				Value bbb = t1.getObject();
				if (bbb instanceof IRI) {
					boolean added = addInferredStatement(xxx, (IRI) bbb, yyy);
					if (added) {
						nofInferred++;
					}
				}
			}
			t1Iter.close();
		}

		return nofInferred;
	}

	// aaa rdfs:subPropertyOf bbb (nt) && xxx aaa yyy (t1) --> xxx bbb yyy
	private int applyRuleRdfs7_2() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.SUBPROPERTYOF, null);

		for (Statement nt : ntIter) {
			Resource aaa = nt.getSubject();
			Value bbb = nt.getObject();

			if (aaa instanceof IRI && bbb instanceof IRI) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(null, (IRI) aaa, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Resource xxx = t1.getSubject();
					Value yyy = t1.getObject();

					boolean added = addInferredStatement(xxx, (IRI) bbb, yyy);
					if (added) {
						nofInferred++;
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// xxx rdf:type rdfs:Class --> xxx rdfs:subClassOf rdfs:Resource
	private int applyRuleRdfs8() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, RDF.TYPE, RDFS.CLASS);

		for (Statement st : iter) {
			Resource xxx = st.getSubject();

			boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx rdfs:subClassOf yyy (nt) && aaa rdf:type xxx (t1) --> aaa rdf:type yyy
	private int applyRuleRdfs9_1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.SUBCLASSOF, null);

		for (Statement nt : ntIter) {
			Resource xxx = nt.getSubject();
			Value yyy = nt.getObject();

			if (yyy instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(null, RDF.TYPE, xxx, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Resource aaa = t1.getSubject();

					boolean added = addInferredStatement(aaa, RDF.TYPE, yyy);
					if (added) {
						nofInferred++;
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// aaa rdf:type xxx (nt) && xxx rdfs:subClassOf yyy (t1) --> aaa rdf:type yyy
	private int applyRuleRdfs9_2() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDF.TYPE, null);

		for (Statement nt : ntIter) {
			Resource aaa = nt.getSubject();
			Value xxx = nt.getObject();

			if (xxx instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements((Resource) xxx, RDFS.SUBCLASSOF, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Value yyy = t1.getObject();

					if (yyy instanceof Resource) {
						boolean added = addInferredStatement(aaa, RDF.TYPE, yyy);
						if (added) {
							nofInferred++;
						}
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// xxx rdf:type rdfs:Class --> xxx rdfs:subClassOf xxx
	private int applyRuleRdfs10() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, RDF.TYPE, RDFS.CLASS);

		for (Statement st : iter) {
			Resource xxx = st.getSubject();

			boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, xxx);
			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx rdfs:subClassOf yyy (nt) && yyy rdfs:subClassOf zzz (t1)
	// --> xxx rdfs:subClassOf zzz
	private int applyRuleRdfs11_1() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.SUBCLASSOF, null);

		for (Statement nt : ntIter) {
			Resource xxx = nt.getSubject();
			Value yyy = nt.getObject();

			if (yyy instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements((Resource) yyy, RDFS.SUBCLASSOF, null, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Value zzz = t1.getObject();

					if (zzz instanceof Resource) {
						boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, zzz);
						if (added) {
							nofInferred++;
						}
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// yyy rdfs:subClassOf zzz (nt) && xxx rdfs:subClassOf yyy (t1)
	// --> xxx rdfs:subClassOf zzz
	private int applyRuleRdfs11_2() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> ntIter = newThisIteration.getStatements(null, RDFS.SUBCLASSOF, null);

		for (Statement nt : ntIter) {
			Resource yyy = nt.getSubject();
			Value zzz = nt.getObject();

			if (zzz instanceof Resource) {
				CloseableIteration<? extends Statement, SailException> t1Iter;
				t1Iter = getWrappedConnection().getStatements(null, RDFS.SUBCLASSOF, yyy, true);

				while (t1Iter.hasNext()) {
					Statement t1 = t1Iter.next();

					Resource xxx = t1.getSubject();

					boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, zzz);
					if (added) {
						nofInferred++;
					}
				}
				t1Iter.close();
			}
		}

		return nofInferred;
	}

	// xxx rdf:type rdfs:ContainerMembershipProperty
	// --> xxx rdfs:subPropertyOf rdfs:member
	private int applyRuleRdfs12() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY);

		for (Statement st : iter) {
			Resource xxx = st.getSubject();

			boolean added = addInferredStatement(xxx, RDFS.SUBPROPERTYOF, RDFS.MEMBER);
			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx rdf:type rdfs:Datatype --> xxx rdfs:subClassOf rdfs:Literal
	private int applyRuleRdfs13() throws SailException {
		int nofInferred = 0;

		Iterable<Statement> iter = newThisIteration.getStatements(null, RDF.TYPE, RDFS.DATATYPE);

		for (Statement st : iter) {
			Resource xxx = st.getSubject();

			boolean added = addInferredStatement(xxx, RDFS.SUBCLASSOF, RDFS.LITERAL);
			if (added) {
				nofInferred++;
			}
		}

		return nofInferred;
	}

	// xxx rdf:_* yyy --> rdf:_* rdf:type rdfs:ContainerMembershipProperty
	// This is an extra rule for list membership properties (_1, _2, _3, ...).
	// The RDF MT does not specificy a production for this.
	private int applyRuleX1() throws SailException {
		int nofInferred = 0;

		String prefix = RDF.NAMESPACE + "_";
		Iterable<Statement> iter = newThisIteration.getStatements(null, null, null);

		for (Statement st : iter) {
			IRI predNode = st.getPredicate();
			String predURI = predNode.toString();

			if (predURI.startsWith(prefix) && isValidPredicateNumber(predURI.substring(prefix.length()))) {
				boolean added = addInferredStatement(predNode, RDF.TYPE, RDFS.CONTAINERMEMBERSHIPPROPERTY);
				if (added) {
					nofInferred++;
				}
			}
		}

		return nofInferred;
	}

	/**
	 * Util method for {@link #applyRuleX1}.
	 */
	private boolean isValidPredicateNumber(String str) {
		int strLength = str.length();

		if (strLength == 0) {
			return false;
		}

		for (int i = 0; i < strLength; i++) {
			if (!ASCIIUtil.isNumber(str.charAt(i))) {
				return false;
			}
		}

		// No leading zeros
		if (str.charAt(0) == '0') {
			return false;
		}

		return true;
	}
}
