package org.leader.data.ontology.test;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.rdf.model.*;

/**
 * 从数据库中获取本体
 *
 * @author ldh
 * @since 2017-02-25 16:07
 */
public class GetOntologyFromDB {
    /** MySQL driver classname */
    private static final String mysqlDriver = "com.mysql.jdbc.Driver";

    /** URL of database to use */
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ontology";
    private static final String DB_TYPE = "MySQL";

    /** User credentials */
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "xb1234";

    private static final String MODEL_NAME = "family";
    // Namespace declarations
    private static final String familyUri = "http://family/";
    private static final String relationshipUri = "http://purl.org/vocab/relationship/";

    public static void main(String[] args) {
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

        Model model = maker.openModel(MODEL_NAME);

        // List everyone in the model who has a child:
        ResIterator parents = model.listSubjectsWithProperty(model.getProperty(relationshipUri, "parentOf"));

        // Because subjects of statements are Resources, the method returned a ResIterator
        while (parents.hasNext()) {
            // Print the URI of the resource
            Resource parent = parents.nextResource();
            System.out.println(parent.getURI());
        }

        // 修改数据库中的本体
        model.begin();

        model.remove(model.createStatement(model.getResource(familyUri+"edward"),
                model.getProperty(relationshipUri, "childOf"),
                model.getResource(familyUri+"dotty")));

        //model.add(model.createStatement(model.getResource(familyUri+"edward"),
        //        model.getProperty(relationshipUri, "childOf"),
        //        model.getResource(familyUri+"dotty")));

        model.commit();

        try {
            // Close the database connection
            connection.close();
        } catch (java.sql.SQLException e) {}


    }
}
