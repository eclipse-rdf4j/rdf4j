package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailConnectionListener;

/**
 * Created by heshanjayasinghe on 6/3/17.
 */
public abstract class AbstractShaclFowardChainingInferencerConnection implements SailConnectionListener {

    private Model newStatements;
    private boolean statementsRemoved;

    @Override
    public void statementAdded(Statement st) {
        if (statementsRemoved) {
            // No need to record, starting from scratch anyway
            return;
        }

        if (newStatements == null) {
            newStatements = createModel();
        }
        newStatements.add(st);
        System.out.println("statement added : "+st);
    }

    protected abstract Model createModel();

    @Override
    public void statementRemoved(Statement st) {
        boolean removed = (newStatements != null) ? newStatements.remove(st) : false;
        if (!removed) {
            // trigger full rebuild
            statementsRemoved = true;
            newStatements = null;
        }
        System.out.println("statement removed : "+st);
    }
}
