package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * Created by heshanjayasinghe on 6/1/17.
 */
public interface shaclNotifyingSailConnection extends SailConnection {

    public void statementAdded(Statement st);

    public void statementRemoved(Statement st);
}
