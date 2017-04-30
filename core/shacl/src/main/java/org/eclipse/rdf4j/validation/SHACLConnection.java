package org.eclipse.rdf4j.validation;


import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 4/23/17.
 */
public class SHACLConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

    private  SHACLSail sail;
    List<Statement> addedStatement = new ArrayList<Statement>();

    /**
     * Creates a new {@link org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper} object that wraps the supplied connection.
     *
     * @param wrappedCon
     */


    public SHACLConnection(NotifyingSailConnection wrappedCon) {
        super(wrappedCon);
    }

    public SHACLConnection(SHACLSail shaclSail) {
        super(null);
        this.sail = shaclSail;
    }

    @Override
    public void begin(IsolationLevel level) throws SailException {
        super.begin(level);
    }

    @Override
    public void statementAdded(Statement statement) {
        addedStatement.add(statement);
    }

    @Override
    public void statementRemoved(Statement st) {

    }

    @Override
    public void commit() throws SailException {
        super.commit();
    }
}
