package org.eclipse.rdf4j.federated.algebra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * A join group around a {@link TripleRefStatementPattern}: besides the {@link TripleRefStatementPattern} it contains
 * the list of {@link StatementPattern} sharing the same subject (typically a blank node).
 *
 * {@link TripleRefJoinGroup} are exclusive to a single {@link StatementSource}.
 */
public class TripleRefJoinGroup extends AbstractQueryModelNode implements FedXTupleExpr {

	private static final long serialVersionUID = 3505148693453950138L;

	private final TripleRefStatementPattern tripleRef;
	private final List<StatementPattern> stmts;
	private final StatementSource statementSource;
	private final QueryInfo queryInfo;

	private Set<String> assuredBindingNames;
	private List<String> freeVars;

	public TripleRefJoinGroup(TripleRefStatementPattern tripleRef, List<StatementPattern> stmts,
			StatementSource statementSource, QueryInfo queryInfo) {
		super();
		this.tripleRef = tripleRef;
		this.stmts = stmts;
		this.statementSource = statementSource;
		this.queryInfo = queryInfo;
	}

	@Override
	public Set<String> getBindingNames() {
		return getAssuredBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		if (assuredBindingNames == null) {
			assuredBindingNames = new HashSet<>();
			assuredBindingNames.addAll(tripleRef.getAssuredBindingNames());
			stmts.forEach(st -> assuredBindingNames.addAll(st.getAssuredBindingNames()));
		}
		return assuredBindingNames;
	}

	public StatementSource getSource() {
		return statementSource;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {

		statementSource.visit(visitor);

		tripleRef.visit(visitor);

		for (var stmt : stmts) {
			stmt.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		throw new UnsupportedOperationException();

	}

	@Override
	public List<String> getFreeVars() {
		if (freeVars == null) {
			Set<String> freeVarsSet = new HashSet<>();
			freeVarsSet.addAll(tripleRef.getFreeVars());
			for (var st : stmts) {
				if (st instanceof VariableExpr e) {
					freeVarsSet.addAll(e.getFreeVars());
				} else {
					freeVarsSet.addAll(QueryAlgebraUtil.getFreeVars(st));
				}
			}
			freeVars = new ArrayList<String>(freeVarsSet);
		}
		return freeVars;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

	@Override
	public TripleRefJoinGroup clone() {
		return (TripleRefJoinGroup) super.clone();
	}
}
