package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/1/17.
 */
public class ShaclNotifyingSailConneectionBase extends AbstractSailConnection implements shaclNotifyingSailConnection {

    private List<shaclNotifyingSailConnection> listeners;
    private boolean statementsRemoved;
    private Model newStatements;

    public ShaclNotifyingSailConneectionBase(AbstractSail abstractSail) {
        super(abstractSail);
        listeners = new ArrayList<shaclNotifyingSailConnection>(0);
    }


    protected void notifyStatementAdded(Statement st) {
        synchronized (listeners) {
            for (shaclNotifyingSailConnection listener : listeners) {
                listener.statementAdded(st);

            }
         //   addStatement(st.getSubject(),st.getPredicate(),st.getObject(),st.getContext());
            System.out.println("Statement added :"+st);

        }
    }

    protected void notifyStatementRemoved(Statement st) {
        synchronized (listeners) {
            for (shaclNotifyingSailConnection listener : listeners) {
                listener.statementRemoved(st);

            }
           // removeStatements(st.getSubject(),st.getPredicate(),st.getObject(),st.getContext());
            System.out.println("Statement removed :"+st);
        }
    }

    @Override
    public void statementAdded(Statement statement) {
        if (statementsRemoved) {
            return;
        }

        if (newStatements == null) {
            newStatements = createModel();
        }
        newStatements.add(statement);
    }

    @Override
    public void statementRemoved(Statement statement) {
        boolean removed = (newStatements != null) ? newStatements.remove(statement) : false;
        if (!removed) {
            statementsRemoved = true;
            newStatements = null;
        }
    }

    protected Model createModel(){
        return new TreeModel();
    };

    @Override
    protected void closeInternal() throws SailException {

    }

    @Override
    protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
        return null;
    }

    @Override
    protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
        return null;
    }

    @Override
    protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
        return null;
    }

    @Override
    protected long sizeInternal(Resource... contexts) throws SailException {
        return 0;
    }

    @Override
    protected void startTransactionInternal() throws SailException {

    }

    @Override
    protected void commitInternal() throws SailException {

    }

    @Override
    protected void rollbackInternal() throws SailException {

    }

    @Override
    protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

    }

    @Override
    protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

    }

    @Override
    protected void clearInternal(Resource... contexts) throws SailException {

    }

    @Override
    protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
        return null;
    }

    @Override
    protected String getNamespaceInternal(String prefix) throws SailException {
        return null;
    }

    @Override
    protected void setNamespaceInternal(String prefix, String name) throws SailException {

    }

    @Override
    protected void removeNamespaceInternal(String prefix) throws SailException {

    }

    @Override
    protected void clearNamespacesInternal() throws SailException {

    }
}
