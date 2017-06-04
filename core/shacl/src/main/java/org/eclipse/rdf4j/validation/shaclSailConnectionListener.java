package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.model.Statement;

/**
 * Created by heshanjayasinghe on 6/3/17.
 */
public interface shaclSailConnectionListener {

    public void statementAdded(Statement st);

    public void statementRemoved(Statement st);
}
