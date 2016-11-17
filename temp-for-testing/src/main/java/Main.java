import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.FastRdfsForwardChainingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;


public class Main {

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            new TempThread().start();
        }


    }

    static class TempThread extends Thread {

        static SailRepository sail = new SailRepository(new FastRdfsForwardChainingSail(new MemoryStore()));

        static {
            sail.initialize();
        }


        @Override
        public void run() {
            for (int j = 0; j < 10; j++) {

                try (SailRepositoryConnection connection = sail.getConnection()) {
                    connection.begin();
                    SimpleValueFactory vf = SimpleValueFactory.getInstance();
                    for (int i = 0; i < 10; i++) {
                        connection.add(vf.createStatement(vf.createBNode(), RDFS.SUBCLASSOF, vf.createBNode()));
                    }
                    connection.commit();
                }
            }


        }
    }

}
