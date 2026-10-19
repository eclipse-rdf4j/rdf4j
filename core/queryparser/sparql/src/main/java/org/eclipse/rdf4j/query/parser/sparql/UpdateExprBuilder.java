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
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.AnnotationTripleRef;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.ReifiedTripleRef;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAdd;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTClear;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCopy;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCreate;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteWhere;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDrop;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphOrDefault;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphRefAll;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInsertClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInsertData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLoad;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTModify;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMove;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQuadsNotTriples;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTReifiedTriple;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTripleTerm;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUnparsedQuadDataBlock;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpdate;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTWhereClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.SimpleNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * Extension of TupleExprBuilder that builds Update Expressions.
 *
 * @author Jeen Broekstra
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class UpdateExprBuilder extends TupleExprBuilder {

	TupleExpr where;
	private final Map<String, BNodeGenerator> bNodeGenerators = new HashMap<>();

	/**
	 * @param valueFactory
	 */
	public UpdateExprBuilder(ValueFactory valueFactory) {
		super(valueFactory);
	}

	@Override
	public UpdateExpr visit(ASTUpdate node, Object data) throws VisitorException {
		if (node instanceof ASTModify) {
			return this.visit((ASTModify) node, data);
		} else if (node instanceof ASTInsertData) {
			return this.visit((ASTInsertData) node, data);
		}

		return null;
	}

	@Override
	public InsertData visit(ASTInsertData node, Object data) throws VisitorException {
		ASTUnparsedQuadDataBlock dataBlock = node.jjtGetChild(ASTUnparsedQuadDataBlock.class);
		InsertData insertData = new InsertData(dataBlock.getDataBlock());

		insertData.setLineNumberOffset(dataBlock.getAddedDefaultPrefixes());
		return insertData;
	}

	@Override
	public DeleteData visit(ASTDeleteData node, Object data) throws VisitorException {

		ASTUnparsedQuadDataBlock dataBlock = node.jjtGetChild(ASTUnparsedQuadDataBlock.class);
		DeleteData deleteData = new DeleteData(dataBlock.getDataBlock());

		deleteData.setLineNumberOffset(dataBlock.getAddedDefaultPrefixes());
		return deleteData;

	}

	@Override
	public TupleExpr visit(ASTQuadsNotTriples node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern();

		ValueExpr contextNode = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, data);

		Var contextVar = mapValueExprToVar(contextNode);
		graphPattern.setContextVar(contextVar);
		graphPattern.setStatementPatternScope(Scope.NAMED_CONTEXTS);

		for (int i = 1; i < node.jjtGetNumChildren(); i++) {
			node.jjtGetChild(i).jjtAccept(this, data);
		}

		TupleExpr result = graphPattern.buildTupleExpr();
		parentGP.addRequiredTE(result);

		graphPattern = parentGP;

		return result;
	}

	@Override
	public Modify visit(ASTDeleteWhere node, Object data) throws VisitorException {
		// Collect delete clause triples
		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern();

		// inherit scope & context
		graphPattern.setStatementPatternScope(parentGP.getStatementPatternScope());
		graphPattern.setContextVar(parentGP.getContextVar());

		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			node.jjtGetChild(i).jjtAccept(this, data);
		}

		where = graphPattern.buildTupleExpr();
		graphPattern = parentGP;
		Map<String, Object> tripleVars = TripleRefCollector.process(where);

		TupleExpr deleteExpr = where.clone();

		// FIXME we should adapt the grammar so we can avoid doing this
		// post-processing.
		VarCollector collector = new VarCollector();
		deleteExpr.visit(collector);
		for (Var var : collector.getCollectedVars()) {
			// skip vars that are provided by ValueExprTripleRef - added as Extentsion
			if (tripleVars.containsKey(var.getName())) {
				continue;
			}

			if (var.isAnonymous() && !var.hasValue()) {
				throw new VisitorException("DELETE WHERE may not contain blank nodes");
			}
		}

		return new Modify(deleteExpr, null, where);
	}

	@Override
	public Load visit(ASTLoad node, Object data) throws VisitorException {

		ValueConstant source = (ValueConstant) node.jjtGetChild(0).jjtAccept(this, data);

		Load load = new Load(source);
		load.setSilent(node.isSilent());
		if (node.jjtGetNumChildren() > 1) {
			ValueConstant graph = (ValueConstant) node.jjtGetChild(1).jjtAccept(this, data);
			load.setGraph(graph);
		}

		return load;
	}

	@Override
	public Clear visit(ASTClear node, Object data) throws VisitorException {
		Clear clear = new Clear();
		clear.setSilent(node.isSilent());

		ASTGraphRefAll graphRef = node.jjtGetChild(ASTGraphRefAll.class);

		if (graphRef.jjtGetNumChildren() > 0) {
			ValueConstant graph = (ValueConstant) graphRef.jjtGetChild(0).jjtAccept(this, data);
			clear.setGraph(graph);
		} else {
			if (graphRef.isDefault()) {
				clear.setScope(Scope.DEFAULT_CONTEXTS);
			} else if (graphRef.isNamed()) {
				clear.setScope(Scope.NAMED_CONTEXTS);
			}
		}
		return clear;
	}

	@Override
	public Clear visit(ASTDrop node, Object data) throws VisitorException {
		// implementing drop as a synonym of clear, in RDF4J this is really the
		// same thing, as empty
		// graphs are not recorded.

		Clear clear = new Clear();
		clear.setSilent(node.isSilent());

		ASTGraphRefAll graphRef = node.jjtGetChild(ASTGraphRefAll.class);

		if (graphRef.jjtGetNumChildren() > 0) {
			ValueConstant graph = (ValueConstant) graphRef.jjtGetChild(0).jjtAccept(this, data);
			clear.setGraph(graph);
		} else {
			if (graphRef.isDefault()) {
				clear.setScope(Scope.DEFAULT_CONTEXTS);
			} else if (graphRef.isNamed()) {
				clear.setScope(Scope.NAMED_CONTEXTS);
			}
		}
		return clear;
	}

	@Override
	public Create visit(ASTCreate node, Object data) throws VisitorException {
		ValueConstant graph = (ValueConstant) node.jjtGetChild(0).jjtAccept(this, data);

		Create create = new Create(graph);
		create.setSilent(node.isSilent());
		return create;
	}

	@Override
	public Copy visit(ASTCopy node, Object data) throws VisitorException {
		Copy copy = new Copy();
		copy.setSilent(node.isSilent());

		ASTGraphOrDefault sourceNode = (ASTGraphOrDefault) node.jjtGetChild(0);
		if (sourceNode.jjtGetNumChildren() > 0) {
			ValueConstant sourceGraph = (ValueConstant) sourceNode.jjtGetChild(0).jjtAccept(this, data);
			copy.setSourceGraph(sourceGraph);
		}

		ASTGraphOrDefault destinationNode = (ASTGraphOrDefault) node.jjtGetChild(1);
		if (destinationNode.jjtGetNumChildren() > 0) {
			ValueConstant destinationGraph = (ValueConstant) destinationNode.jjtGetChild(0).jjtAccept(this, data);
			copy.setDestinationGraph(destinationGraph);
		}
		return copy;
	}

	@Override
	public Move visit(ASTMove node, Object data) throws VisitorException {
		Move move = new Move();
		move.setSilent(node.isSilent());

		ASTGraphOrDefault sourceNode = (ASTGraphOrDefault) node.jjtGetChild(0);
		if (sourceNode.jjtGetNumChildren() > 0) {
			ValueConstant sourceGraph = (ValueConstant) sourceNode.jjtGetChild(0).jjtAccept(this, data);
			move.setSourceGraph(sourceGraph);
		}

		ASTGraphOrDefault destinationNode = (ASTGraphOrDefault) node.jjtGetChild(1);
		if (destinationNode.jjtGetNumChildren() > 0) {
			ValueConstant destinationGraph = (ValueConstant) destinationNode.jjtGetChild(0).jjtAccept(this, data);
			move.setDestinationGraph(destinationGraph);
		}
		return move;
	}

	@Override
	public Add visit(ASTAdd node, Object data) throws VisitorException {
		Add add = new Add();
		add.setSilent(node.isSilent());

		ASTGraphOrDefault sourceNode = (ASTGraphOrDefault) node.jjtGetChild(0);
		if (sourceNode.jjtGetNumChildren() > 0) {
			ValueConstant sourceGraph = (ValueConstant) sourceNode.jjtGetChild(0).jjtAccept(this, data);
			add.setSourceGraph(sourceGraph);
		}

		ASTGraphOrDefault destinationNode = (ASTGraphOrDefault) node.jjtGetChild(1);
		if (destinationNode.jjtGetNumChildren() > 0) {
			ValueConstant destinationGraph = (ValueConstant) destinationNode.jjtGetChild(0).jjtAccept(this, data);
			add.setDestinationGraph(destinationGraph);
		}
		return add;
	}

	@Override
	public Modify visit(ASTModify node, Object data) throws VisitorException {
		ASTWhereClause whereClause = node.getWhereClause();

		where = null;
		if (whereClause != null) {
			where = (TupleExpr) whereClause.jjtAccept(this, data);
		}

		TupleExpr delete = null;
		ASTDeleteClause deleteNode = node.getDeleteClause();
		if (deleteNode != null) {
			delete = (TupleExpr) deleteNode.jjtAccept(this, data);
		}

		TupleExpr insert = null;
		ASTInsertClause insertNode = node.getInsertClause();
		if (insertNode != null) {
			insert = (TupleExpr) insertNode.jjtAccept(this, data);
			var tripleBNodes = TripleRefBNodeVarCollector.process(where);
			if (!tripleBNodes.isEmpty()) {
				List<ExtensionElem> elems = new ArrayList<>();
				for (Var tripleBNode : tripleBNodes) {
					createBNodeExtensionElem(tripleBNode, elems);
				}
				Extension ext = prependExtensions(where, elems);

				if (ext == null) {
					ext = new Extension(where);
					ext.addElements(elems);
				}
				where = ext;
			}
		}

		return new Modify(delete, insert, where);
	}

	@Override
	public TupleExpr visit(ASTDeleteClause node, Object data) throws VisitorException {

		// Collect construct triples
		GraphPattern parentGP = graphPattern;

		graphPattern = new GraphPattern();

		// inherit scope & context
		graphPattern.setStatementPatternScope(parentGP.getStatementPatternScope());
		graphPattern.setContextVar(parentGP.getContextVar());

		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			node.jjtGetChild(i).jjtAccept(this, data);
		}

		TupleExpr deleteExpr = graphPattern.buildTupleExpr();
		Map<String, Object> tripleVars = TripleRefCollector.process(where);

		// FIXME we should adapt the grammar so we can avoid doing this in
		// post-processing.
		VarCollector collector = new VarCollector();
		deleteExpr.visit(collector);
		for (Var var : collector.getCollectedVars()) {
			// skip vars that are provided by ValueExprTripleRef - added as Extentsion
			if (tripleVars.containsKey(var.getName())) {
				continue;
			}
			if (var.isAnonymous() && !var.hasValue()) {
				// blank node in delete pattern, not allowed by SPARQL spec.
				throw new VisitorException("DELETE clause may not contain blank nodes");
			}
		}

		graphPattern = parentGP;

		return deleteExpr;

	}

	@Override
	public TupleExpr visit(ASTInsertClause node, Object data) throws VisitorException {

		// Collect insert clause triples
		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern();

		// inherit scope & context
		graphPattern.setStatementPatternScope(parentGP.getStatementPatternScope());
		graphPattern.setContextVar(parentGP.getContextVar());

		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			node.jjtGetChild(i).jjtAccept(this, data);
		}

		TupleExpr insertExpr = graphPattern.buildTupleExpr();

		graphPattern = parentGP;

		return insertExpr;

	}

	@Override
	protected Var buildReifiedTripleVar(Object reifier, Var subjVar, Var predVar, Var objVar) throws VisitorException {
		if (where == null) {
			return super.buildReifiedTripleVar(reifier, subjVar, predVar, objVar);
		}
		AnnotationTripleRef rtr = buildReifiedTripleRef(subjVar, predVar, objVar, reifier);
		Extension ext = new Extension(where);
		ext.addElement(new ExtensionElem(castToValueExpr(rtr), rtr.getExprVar().getName()));
		graphPattern.addRequiredSP(rtr.getReifVar().clone(), REIFIES_VAR.clone(), rtr.getExprVar().clone());
		where = ext;
		return rtr.getReifVar();
	}

	@Override
	public TupleExpr visit(ASTTripleTerm node, Object data) throws VisitorException {
		if (where == null) {
			return super.visit(node, data);
		}
		TripleRef ret = constructTripleRefFromAST(node);

		Extension ext = new Extension(where);
		ext.addElement(new ExtensionElem(castToValueExpr(ret), ret.getExprVar().getName()));
		where = ext;

		return ret;
	}

	@Override
	public TupleExpr visit(ASTReifiedTriple node, Object data) throws VisitorException {
		if (where == null) {
			return super.visit(node, data);
		}
		ReifiedTripleRef ret = new ReifiedTripleRef();

		SimpleNode subjNode = node.getSubj();
		// Recursively handle nested reified triples in subject position
		if (subjNode instanceof ASTReifiedTriple) {
			ReifiedTripleRef nestedRef = new ReifiedTripleRef();
			var retSubj = mapValueExprToVar(subjNode.jjtAccept(this, nestedRef));
			// Mark nested reifier as blank node so it gets collected
			// by TripleRefBNodeVarCollector and bound to a BNodeGenerator in INSERT/WHERE
			retSubj.setBNode(true);
			ret.setSubjectVar(retSubj);
		} else {
			ret.setSubjectVar(mapValueExprToVar(subjNode.jjtAccept(this, data)));
		}
		ret.setPredicateVar(mapValueExprToVar(node.getPred().jjtAccept(this, ret)));

		SimpleNode objNode = node.getObj();
		// Recursively handle nested reified triples in object position
		if (objNode instanceof ASTReifiedTriple) {
			ReifiedTripleRef nestedRef = new ReifiedTripleRef();
			var retObj = mapValueExprToVar(objNode.jjtAccept(this, nestedRef));
			// Mark nested reifier as blank node so it gets collected
			// by TripleRefBNodeVarCollector and bound to a BNodeGenerator in INSERT/WHERE
			retObj.setBNode(true);
			ret.setObjectVar(retObj);
		} else {
			ret.setObjectVar(mapValueExprToVar(objNode.jjtAccept(this, ret)));
		}
		ret.setExprVar(createAnonVar());

		// Use explicit reifier if provided; otherwise generate an anonymous blank node reifier
		Var reifier;
		if (node.getReifier() != null) {
			reifier = mapValueExprToVar(node.getReifier().jjtAccept(this, ret));
		} else {
			reifier = createAnonVar();
		}
		ret.setReifVar(reifier);

		Extension ext = new Extension(where);
		ext.addElement(new ExtensionElem(castToValueExpr(ret), ret.getExprVar().getName()));

		// Add the reification statement: reifier rdf:reifies <triple-expression>
		graphPattern.addRequiredSP(ret.getReifVar().clone(), REIFIES_VAR.clone(), ret.getExprVar().clone());
		where = ext;

		return ret;
	}

	/**
	 * Creates and registers an {@link ExtensionElem} that binds a blank node variable to a {@link BNodeGenerator}, if
	 * the variable has not been encountered before.
	 * <p>
	 * Blank node variables used in SPARQL UPDATE templates (e.g. INSERT or DELETE) must be associated with a
	 * {@link BNodeGenerator} so that a fresh RDF blank node is produced during evaluation. The generated value is then
	 * reused for all subsequent references to the same variable within the operation.
	 * <p>
	 * The {@code bNodeGenerators} map acts as a registry of variables that have already been assigned a generator. When
	 * a variable is first encountered, a new {@link BNodeGenerator} is created and an {@link ExtensionElem} binding is
	 * added to the supplied list. If the variable was already registered, no additional element is created.
	 *
	 * @param var   the variable representing a blank node in the update template
	 * @param elems the list to which a new {@link ExtensionElem} binding should be added if this is the first
	 *              occurrence of the variable
	 */
	private void createBNodeExtensionElem(Var var, List<ExtensionElem> elems) {
		// computeIfAbsent only creates a new BNodeGenerator on first encounter;
		// if already present the existing generator is returned but not re-added to the extension.
		if (!bNodeGenerators.containsKey(var.getName())) {
			ValueExpr valueExpr = bNodeGenerators.computeIfAbsent(var.getName(),
					ignored -> new BNodeGenerator());
			elems.add(new ExtensionElem(valueExpr, var.getName()));
		}
	}

	private Extension prependExtensions(TupleExpr expr, List<ExtensionElem> topElems) {

		if (!(expr instanceof Extension)) {
			return null;
		}

		List<ExtensionElem> elems = new ArrayList<>();
		TupleExpr arg = expr;

		// walk down the chain
		while (arg instanceof Extension) {
			elems.addAll(0, ((Extension) arg).getElements());
			arg = ((Extension) arg).getArg();
		}

		Extension flat = new Extension(arg);

		flat.addElements(topElems);

		for (ExtensionElem e : elems) {
			flat.addElement(e);
		}

		return flat;
	}
}
