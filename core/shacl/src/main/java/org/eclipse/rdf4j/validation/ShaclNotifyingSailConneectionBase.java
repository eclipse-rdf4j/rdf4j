package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.UpdateContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/1/17.
 */
public class ShaclNotifyingSailConneectionBase implements shaclNotifyingSailConnection {

    private List<shaclSailConnectionListener> listeners;
    private boolean statementsRemoved;
    private Model newStatements;

    public ShaclNotifyingSailConneectionBase() {
        listeners = new ArrayList<shaclSailConnectionListener>(0);
    }


    public void notifyStatementAdded(Statement st) {
        synchronized (listeners) {
            for (shaclSailConnectionListener listener : listeners) {
                listener.statementAdded(st);

            }
        }
    }

    public void notifyStatementRemoved(Statement st) {
        synchronized (listeners) {
            for (shaclSailConnectionListener listener : listeners) {
                listener.statementRemoved(st);

            }
        }
    }

    protected Model createModel(){
        return new TreeModel();
    };



    @Override
    public void addConnectionListener(shaclSailConnectionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeConnectionListener(shaclSailConnectionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }


    @Override
    public boolean isOpen() throws SailException {
        return false;
    }

    @Override
    public void close() throws SailException {

    }

    @Override
    public CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluate(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
        return null;
    }

    @Override
    public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
        return null;
    }

    @Override
    public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
        return null;
    }

    @Override
    public long size(Resource... contexts) throws SailException {
        return 0;
    }

    @Override
    public void begin() throws SailException {

    }

    @Override
    public void begin(IsolationLevel level) throws UnknownSailTransactionStateException, SailException {

    }

    @Override
    public void flush() throws SailException {

    }

    @Override
    public void prepare() throws SailException {

    }

    @Override
    public void commit() throws SailException {

    }

    @Override
    public void rollback() throws SailException {

    }

    @Override
    public boolean isActive() throws UnknownSailTransactionStateException {
        return false;
    }

    @Override
    public void addStatement(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

    }

    @Override
    public void removeStatements(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

    }

    @Override
    public void startUpdate(UpdateContext op) throws SailException {

    }

    @Override
    public void addStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

    }

    @Override
    public void removeStatement(UpdateContext op, Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

    }

    @Override
    public void endUpdate(UpdateContext op) throws SailException {

    }

    @Override
    public void clear(Resource... contexts) throws SailException {

    }

    @Override
    public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
        return null;
    }

    @Override
    public String getNamespace(String prefix) throws SailException {
        return null;
    }

    @Override
    public void setNamespace(String prefix, String name) throws SailException {

    }

    @Override
    public void removeNamespace(String prefix) throws SailException {

    }

    @Override
    public void clearNamespaces() throws SailException {

    }
}
