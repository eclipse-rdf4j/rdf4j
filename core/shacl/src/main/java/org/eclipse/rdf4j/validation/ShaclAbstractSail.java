package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;

/**
 * Created by heshanjayasinghe on 6/2/17.
 */
public class ShaclAbstractSail extends AbstractSail{



    @Override
    public boolean isWritable() throws SailException {
        return false;
    }

    @Override
    public ValueFactory getValueFactory() {
        return null;
    }

    @Override
    protected void shutDownInternal() throws SailException {

    }

    @Override
    protected SailConnection getConnectionInternal() throws SailException {
        return null;
    }
}
