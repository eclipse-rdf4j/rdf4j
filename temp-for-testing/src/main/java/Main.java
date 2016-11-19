import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.FastRdfsForwardChainingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.Random;


public class Main {

    static SailRepository sail = new SailRepository(new FastRdfsForwardChainingSail(new MemoryStore()));

    static {
        sail.initialize();
    }


    static Random r = new Random();

    public static void main(String[] args) {

        try (SailRepositoryConnection connection = sail.getConnection()) {
            connection.begin();
            SimpleValueFactory vf = SimpleValueFactory.getInstance();
            for (int i = 0; i < 10; i++) {
                connection.add(vf.createStatement(vf.createBNode(), RDF.TYPE, vf.createBNode()));
            }
            connection.commit();
        }

        for (int i = 0; i < 10; i++) {
            new TempThreadAbox().start();
        }
        for (int i = 0; i < 4; i++) {

            new TempThreadTbox().start();
        }


    }

    static class TempThreadAbox extends Thread {


        @Override
        public void run() {
            try {
                Thread.sleep(r.nextInt(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < 10; j++) {


                try (SailRepositoryConnection connection = sail.getConnection()) {
                    connection.begin();
                    SimpleValueFactory vf = SimpleValueFactory.getInstance();
                    for (int i = 0; i < 10; i++) {
                        connection.add(vf.createStatement(vf.createBNode(), RDF.TYPE, vf.createBNode()));
                    }
                    connection.commit();
                }
            }


        }
    }

    static class TempThreadTbox extends Thread {


        @Override
        public void run() {
            try {
                Thread.sleep(r.nextInt(5));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < 10; j++) {

                try (SailRepositoryConnection connection = sail.getConnection()) {
                    connection.begin();
                    SimpleValueFactory vf = SimpleValueFactory.getInstance();
                    for (int i = 0; i < 10; i++) {
                        connection.add(vf.createStatement(vf.createBNode(), RDFS.SUBCLASSOF, vf.createBNode()));
                    }
                    connection.commit();
                    System.err.println("TBOX COMMIT WAS OK!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
    }

}
