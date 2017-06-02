package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.helpers.AbstractSail;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/1/17.
 */
public abstract class ShaclNotifyingSailConneectionBaseShacl extends AbstractSailConnection implements shaclNotifyingSailConnection {

    private List<shaclNotifyingSailConnection> listeners;

    public ShaclNotifyingSailConneectionBaseShacl(AbstractSail sailBase) {
        super(sailBase);
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
            removeStatements(st.getSubject(),st.getPredicate(),st.getObject(),st.getContext());
            System.out.println("Statement removed :"+st);
        }
    }
}
