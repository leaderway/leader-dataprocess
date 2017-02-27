package org.leader.data.ontology.test;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * RDF推理机
 *
 * @author ldh
 * @since 2017-02-25 15:17
 */
public class RDFInf {

    public static void main(String[] args) {
        String NS = "urn:x-hp-jena:eg/";

        Model rdfModel = ModelFactory.createDefaultModel();
        Property p = rdfModel.createProperty(NS, "p");
        Property q = rdfModel.createProperty(NS, "q");
        rdfModel.add(p, RDFS.subPropertyOf, q);
        rdfModel.createResource(NS + "a").addProperty(p, "foo");

        // 创建拥有RDFS推理机的RDFSModel
        InfModel inf = ModelFactory.createRDFSModel(rdfModel);
        // 推理
        Resource a = inf.getResource(NS + "a");
        System.out.println("Statement: " + a.getProperty(q));
    }
}
