package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/** Clone-and-rename utilities for Vars. */
public final class VarRenamer {

	private VarRenamer() {
	}

	@SuppressWarnings("unchecked")
	public static <T extends QueryModelNode> T renameClone(T node, java.util.Map<String, String> mapping) {
		T clone = (T) node.clone();
		renameInPlace(clone, mapping);
		return clone;
	}

	public static void renameInPlace(QueryModelNode node, java.util.Map<String, String> mapping) {
		node.visit(new AbstractQueryModelVisitor<>() {
			@Override
			public void meet(Var var) {
				if (!var.hasValue()) {
					String nn = mapping.get(var.getName());
					if (nn != null && !nn.equals(var.getName())) {
						var.replaceWith(new Var(nn, var.getValue(), var.isAnonymous(), var.isConstant()));
					}
				}
			}
		});
	}
}
