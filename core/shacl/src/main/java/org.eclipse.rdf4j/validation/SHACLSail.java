package org.eclipse.rdf4j.common.io;

import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;


/**
 * Created by heshanjayasinghe on 4/16/17.
 */
public class SHACLSail extends NotifyingSailWrapper{

    @Override
    public SHACLConnection getConnection()
            throws SailException
    {
        return new SHACLConnection(this);
    }


}
