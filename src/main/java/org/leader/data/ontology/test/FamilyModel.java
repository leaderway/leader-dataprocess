package org.leader.data.ontology.test;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.rdf.model.*;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Jena测试
 *
 * @author ldh
 * @since 2017-02-25 10:00
 */
public class FamilyModel {

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
    static final String familyUri = "http://family/";
    static final String relationshipUri = "http://purl.org/vocab/relationship/";

    // Jena model representing the family
    private Model model;

    /**
     * Creates a model and populates it with family members and their
     * relationships
     */
    private FamilyModel() {

        // Create an empty Model
        model = ModelFactory.createDefaultModel();

        // Create the types of Property we need to describe relationships
        // in the model
        Property childOf = model.createProperty(relationshipUri,"childOf");
        Property parentOf = model.createProperty(relationshipUri,"parentOf");
        Property siblingOf = model.createProperty(relationshipUri,"siblingOf");
        Property spouseOf = model.createProperty(relationshipUri,"spouseOf");

        // Create resources representing the people in our model
        Resource adam = model.createResource(familyUri+"adam");
        Resource beth = model.createResource(familyUri+"beth");
        Resource chuck = model.createResource(familyUri+"chuck");
        Resource dotty = model.createResource(familyUri+"dotty");
        Resource edward = model.createResource(familyUri+"edward");
        Resource fran = model.createResource(familyUri+"fran");
        Resource greg = model.createResource(familyUri+"greg");
        Resource harriet = model.createResource(familyUri+"harriet");

        // Add properties to describing the relationships between them
        adam.addProperty(siblingOf,beth);
        adam.addProperty(spouseOf,dotty);
        adam.addProperty(parentOf,edward);
        adam.addProperty(parentOf,fran);

        beth.addProperty(siblingOf,adam);
        beth.addProperty(spouseOf,chuck);

        chuck.addProperty(spouseOf,beth);

        dotty.addProperty(spouseOf,adam);
        dotty.addProperty(parentOf,edward);
        dotty.addProperty(parentOf,fran);

        // Statements can also be directly created ...
        Statement statement1 = model.createStatement(edward,childOf,adam);
        Statement statement2 = model.createStatement(edward,childOf,dotty);
        Statement statement3 = model.createStatement(edward,siblingOf,fran);

        // ... then added to the model:
        model.add(statement1);
        model.add(statement2);
        model.add(statement3);

        // Arrays of Statements can also be added to a Model:
        Statement statements[] = new Statement[5];
        statements[0] = model.createStatement(fran,childOf,adam);
        statements[1] = model.createStatement(fran,childOf,dotty);
        statements[2] = model.createStatement(fran,siblingOf,edward);
        statements[3] = model.createStatement(fran,spouseOf,greg);
        statements[4] = model.createStatement(fran,parentOf,harriet);
        model.add(statements);

        // A List of Statements can also be added
        List list = new ArrayList();

        list.add(model.createStatement(greg,spouseOf,fran));
        list.add(model.createStatement(greg,parentOf,harriet));

        list.add(model.createStatement(harriet,childOf,fran));
        list.add(model.createStatement(harriet,childOf,greg));

        model.add(list);
    }

    /**
     * Creates a FamilyModel and dumps the content of its RDF representation
     */
    public static void main(String args[]) throws Exception{

        // Create a model representing the family
        FamilyModel theFamily = new FamilyModel();

        // Dump out a String representation of the model
        System.out.println(theFamily.model);

        Model model = theFamily.model;

/*        // List everyone in the model who has a child:
        ResIterator parents = model.listSubjectsWithProperty(model.getProperty(relationshipUri, "parentOf"));

        // Because subjects of statements are Resources, the method returned a ResIterator
        while (parents.hasNext()) {
            // Print the URI of the resource
            Resource parent = parents.nextResource();
            System.out.println(parent.getURI());
        }

        // Can also find all the parents by getting the objects of all "childOf" statements
        // Objects of statements could be Resources or literals, so the Iterator returned
        // contains RDFNodes
        NodeIterator moreParents = model.listObjectsOfProperty(model.getProperty(relationshipUri, "childOf"));
        System.out.println("=======find all the parents by getting the objects of all \"childOf\" statements");
        while (moreParents.hasNext()) {
            RDFNode rdfNode = moreParents.nextNode();
            System.out.println(rdfNode.toString());
        }

        // To find all the siblings of a specific person, the model itself can be queried
        System.out.println("======find all the siblings of a specific person, the model itself can be queried");
        NodeIterator siblings = model.listObjectsOfProperty(model.getResource(familyUri+"edward"), model.getProperty(relationshipUri, "siblingOf"));
        while (siblings.hasNext()) {
            RDFNode rdfNode = siblings.nextNode();
            System.out.println(rdfNode.toString());
        }

        // But it's more elegant to ask the Resource directly
        // This method yields an iterator over Statements
        System.out.println(" it's more elegant to ask the Resource directly");
        StmtIterator moreSiblings = model.getResource(familyUri+"edward").listProperties(model.getProperty(relationshipUri, "siblingOf"));
        while (moreSiblings.hasNext()) {
            Statement statement = moreSiblings.nextStatement();
            System.out.println(statement.getResource().getURI());
        }*/

        // General select method
        // Find the exact statement "adam is a spouse of dotty"
        //StmtIterator adamDottyIterator = model.listStatements(model.getResource(familyUri+"adam"),model.getProperty(relationshipUri, "spouseOf"),model.getResource(familyUri+"dotty"));

        // Find all statements with adam as the subject and dotty as the object
        //StmtIterator adamDottyIterator = model.listStatements(model.getResource(familyUri+"adam"),null,model.getResource(familyUri+"dotty"));

        // Find any statements made about adam
        //StmtIterator adamDottyIterator = model.listStatements(model.getResource(familyUri+"adam"), null, null, null);

        // Find any statement with the siblingOf property
        StmtIterator adamDottyIterator = model.listStatements(null,model.getProperty(relationshipUri, "spouseOf"),null, null);

        while (adamDottyIterator.hasNext()) {
            Statement statement = adamDottyIterator.nextStatement();
            System.out.println(statement.toString());
        }

        // 输出rdf文件
        String owlPath = "./data/testfamily.xml";
        FileOutputStream os = new FileOutputStream(owlPath);
        RDFWriter rdfWriter = model.getWriter("RDF/XML-ABBREV");
        rdfWriter.setProperty("showXMLDeclaration","true");
        rdfWriter.setProperty("showDoctypeDeclaration", "true");
        rdfWriter.write(model, os, null);
        os.close();

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
        // Create a new model named "family".
        Model familyPersistentModel = maker.createModel(MODEL_NAME,true);

        // Importing models inside a transaction helps performance.
        // Without this, the model each statement is auto-committed as it is
        // added.
        familyPersistentModel.begin();

        familyPersistentModel.add(model);

        // Commit the transaction
        familyPersistentModel.commit();


        try {
            // Close the database connection
            connection.close();
        } catch (java.sql.SQLException e) {}


    }
}
