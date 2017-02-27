package org.leader.data.ontology.test;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;

import java.io.FileOutputStream;

/**
 * @author ldh
 * @since 2017-02-25 14:38
 */
public class ESWCOntology {

    private static final String namespace = "http://www.scnu.edu.cn/ESWC#";

    private OntModel model;

    private ESWCOntology() {
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

        // 构建本体
        OntClass furniture = model.createClass(namespace + "Furniture");
        OntClass chair = model.createClass(namespace + "Chair");
        furniture.addSubClass(chair);
        OntClass bed = model.createClass(namespace + "Bed");
        furniture.addSubClass(bed);
        OntClass zhongwen = model.createClass(namespace + "Zhongwen");

        Resource xiongBB = model.createOntResource(namespace + "xiongBB");


    }

    public static void main(String[] args) throws Exception{
        OntModel model = new ESWCOntology().model;

        // 输出owl文件
        String filePath = "./owl/Furniture.owl";
        FileOutputStream os = new FileOutputStream(filePath);
        RDFWriter rdfWriter = model.getWriter("RDF/XML-ABBREV");
        rdfWriter.setProperty("showXMLDeclaration","true");
        rdfWriter.setProperty("showDoctypeDeclaration", "true");
        rdfWriter.write(model, os, null);
        os.close();

    }
}
