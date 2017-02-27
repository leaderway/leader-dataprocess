package org.leader.data.ontology.test;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 *  Creates a model in the MySQL database, and loads the 3 WordNet RDF graphs
 *  into it. This could also be accomplished on the commandline via
 *  jena.dbcreate and jena.dbload.
 *
 * @author ldh
 * @since 2017-02-25 13:23
 */
public class ImportWordnet {
    /** MySQL driver classname */
    private static final String mysqlDriver = "com.mysql.jdbc.Driver";

    /** URL of database to use */
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydata";
    private static final String DB_TYPE = "MySQL";

    /** User credentials */
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "xb1234";

    /** Name of the Jena model to create */
    private static final String MODEL_NAME = "wordnet";

    /** Locations of wordnet graphs to load */
    private static String WN_NOUNS    = "wordnet_nouns-20010201.rdf";
    private static String WN_GLOSSARY = "wordnet_glossary-20010201.rdf";
    private static String WN_HYPONYMS = "wordnet_hyponyms-20010201.rdf";

    /**
     * Creates a MySQL backed model and reads the wordnet RDF files into it
     */
    public static void main(String args[]) {

        try {
            // Instantiate database driver
            Class.forName(mysqlDriver);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver class not found");
            System.exit(-1);
        }

        // Get a connection to the db
        DBConnection connection = new DBConnection(DB_URL, DB_USER, DB_PASSWORD, DB_TYPE);

        // Get a ModelMaker for database-backed models
        ModelMaker maker = ModelFactory.createModelRDBMaker(connection);

        // Create a new model named "wordnet".
        Model wordnetModel = maker.createModel(MODEL_NAME,true);

        try {

            // Importing models inside a transaction helps performance.
            // Without this, the model each statement is auto-committed as it is
            // added.
            wordnetModel.begin();

            // Read each of the three WordNet documents into the model
            readFileIntoModel(WN_NOUNS, wordnetModel);
            readFileIntoModel(WN_GLOSSARY, wordnetModel);
            readFileIntoModel(WN_HYPONYMS, wordnetModel);

            // Commit the transaction
            wordnetModel.commit();

        } catch (FileNotFoundException e) {

            System.err.println(e.toString());
        } finally {

            try {
                // Close the database connection
                connection.close();
            } catch (java.sql.SQLException e) {}
        }
    }

    /**
     * Reads RDF from a file into a model
     * @param filename Name of the RDF file to read
     * @param model The model to read the RDF into
     * @throws FileNotFoundException if the file can't be located on the classpath
     */
    private static void readFileIntoModel(String filename, Model model)
            throws FileNotFoundException {

        // Use the class loader to find the input file
        InputStream in = ImportWordnet.class.getClassLoader().getResourceAsStream(filename);

        if (in == null) {
            throw new FileNotFoundException("File not found on classpath: "+ filename);
        }

        // Read the triples from the file into the model
        model.read(in,null);
    }
}
