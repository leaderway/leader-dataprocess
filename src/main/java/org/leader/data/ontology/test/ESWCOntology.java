package org.leader.data.ontology.test;

import com.hp.hpl.jena.ontology.*;
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
        model.setNsPrefix("", namespace);
        // 构建本体
        OntClass furniture = model.createClass(namespace + "Furniture");
        OntClass chair = model.createClass(namespace + "Chair");
        furniture.addSubClass(chair);
        OntClass bed = model.createClass(namespace + "Bed");
        furniture.addSubClass(bed);
        OntClass zhongwen = model.createClass(namespace + "Zhongwen");
        //Resource xiongBB = model.createOntResource(namespace + "xiongBB");
        furniture.addSubClass(zhongwen);
        //Individual xiongBB = model.createIndividual(namespace + "xiongBB", zhongwen);
        zhongwen.createIndividual(namespace+"hanyu");
        OntProperty pro = model.createOntProperty(namespace+"qianzhui");
        OntProperty pro2 = model.createOntProperty(namespace+"qianzhui");
        zhongwen.addProperty(pro,"value");
        //zhongwen.removeProperty(pro,"value");
        //bed.addRDFType(xiongBB);
        //zhongwen.addSubClass();
        //zhongwen.addSuperClass();
        //model.getOntClass()

    }

    public static void main(String[] args) throws Exception{
        OntModel model = new ESWCOntology().model;

        // 输出owl文件
        String filePath = "./owl/Furniture.owl";
        FileOutputStream os = new FileOutputStream(filePath);
        //RDFWriter rdfWriter = model.getWriter("RDF/XML");
        RDFWriter rdfWriter = model.getWriter("RDF/XML-ABBREV");
        rdfWriter.setProperty("showXMLDeclaration","true");
        rdfWriter.setProperty("showDoctypeDeclaration", "true");
        rdfWriter.write(model, os, null);
        os.close();

    }
}
