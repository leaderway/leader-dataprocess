package org.leader.data.ontology.eb;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.leader.data.util.OntologyUtils;
import org.leader.data.util.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 电子商务领域本体构建
 *
 * @author ldh
 * @since 2017-03-10 19:11
 */
public class EbOntology {
    private static final Logger LOGGER = LoggerFactory.getLogger(EbOntology.class);

    private static final String CLUSTER_RESULT_PATH = "./data/JULEIcombineWordVSM.txt";//  层次聚类结果
    private static final String AFTER_WORD_ROOT_FILTER_PATH = "./data/relation/afterWordRootFilter.txt";// 筛选后后缀词根及其词语文件
    private static final String CONCEPT_PAIR_WORD_VERB_FILTER_PATH = "./data/relation/conceptVerbFilter.txt";// 概念间非分类关系
    private static final String OWL_FILE_OUTPUT_PATH = "./owl/EBusinessOntology.owl";// 导出的owl文件
    private static final String OWL_FILE_OUTPUT_LANG = "RDF/XML-ABBREV";// 导出的owl文件格式
    private static final String EBONTOLOGY_MODEL_NAME = "ebontology";

    // 命名空间
    private static final String NAMESPACE = "http://em.scnu.edu.cn/ebusinessOntology#";

    // 定义本体模型
    private OntModel model;

    public static void main(String[] args) {
        EbOntology ebOntology = new EbOntology();
    }

    // 定义构造方法
    public EbOntology() {
        model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        model.setNsPrefix("EbOntology", NAMESPACE);
        // 构建本体

        Map<String, List> clusterClassifyConceptMap = getClassifyConceptMapFromClusterFile();
        Map<String, List> rootClassifyConceptMap = getClassifyConceptMapFromRootFile();
        Map<String, Map<String, String>> nonClassifyConceptMap = getNonClassifyConceptMapFromFile();

        // 构造分类关系
        LOGGER.info("开始构建分类关系");
        generateSupAndSubClass(model, clusterClassifyConceptMap);
        generateSupAndSubClass(model, rootClassifyConceptMap);

        // 构建非分类关系
        LOGGER.info("开始构建非分类关系");
        generateNonClassifyClass(model, nonClassifyConceptMap);

        // 导出本体文件
        OntologyUtils.outputOWLFile(model, OWL_FILE_OUTPUT_PATH, OWL_FILE_OUTPUT_LANG);
        LOGGER.info("导出本体文件完成");

        // 将本体存入数据库中
        DBConnection connection = OntologyUtils.getDBConnection();
        boolean flag =  OntologyUtils.storeOntologyToDB(connection, EBONTOLOGY_MODEL_NAME, model);
        if (flag) {
            LOGGER.info("将本体存入数据库成功");
        } else {
            LOGGER.info("将本体存入数据库失败");
        }
    }

    /**
     * 构建上下位类关系
     * @param model
     * @param classifyConceptMap
     */
    private void generateSupAndSubClass(OntModel model, Map<String, List> classifyConceptMap ) {
        for (Map.Entry<String, List> classifyConceptMapEntry : classifyConceptMap.entrySet()) {
            String superClassString = classifyConceptMapEntry.getKey();
            List<String> subClassList = classifyConceptMapEntry.getValue();
            OntClass superClass = model.createClass(NAMESPACE + superClassString);
            for (String subClassString : subClassList) {
                OntClass subClass = model.createClass(NAMESPACE + subClassString);
                superClass.addSubClass(subClass);// 为父类加入子类
                subClass.addSuperClass(superClass);// 为子类加入入类
            }
        }
    }

    /**
     * 构建非分类关系
     * @param model
     * @param nonClassifyConceptMap
     */
    private void generateNonClassifyClass(OntModel model, Map<String, Map<String, String>> nonClassifyConceptMap) {
        for (Map.Entry<String, Map<String, String>> nonClassifyConceptMapEntry : nonClassifyConceptMap.entrySet()) {
            String formerConceptString = nonClassifyConceptMapEntry.getKey();
            OntClass formerConceptClass = model.createClass(NAMESPACE + formerConceptString);

            Map<String, String> latterConceptMap = nonClassifyConceptMapEntry.getValue();
            for (Map.Entry<String, String> latterConceptMapEntry : latterConceptMap.entrySet()) {
                OntClass latterConceptClass = model.createClass(NAMESPACE + latterConceptMapEntry.getKey());

                OntProperty verbLabel = model.createOntProperty(NAMESPACE + latterConceptMapEntry.getValue());
                verbLabel.addDomain(formerConceptClass);
                verbLabel.addRange(latterConceptClass);
            }
        }
    }

    /**
     * 从文件中获取层次聚类的分类概念
     * @return
     */
    private static Map<String, List> getClassifyConceptMapFromClusterFile() {
        Map<String, List> classifyConceptMap = new HashMap<String, List>();
        File clusterResultFile = FileUtils.getFile(CLUSTER_RESULT_PATH);
        if (clusterResultFile.exists()) {
            try {
                LineIterator lineIterator = FileUtils.lineIterator(clusterResultFile);
                while (lineIterator.hasNext()) {
                    String line = lineIterator.next();
                    String[] conceptArray = line.split("\\-");
                    String supConcept = WordUtils.getWordWithOutTags(conceptArray[0]);
                    String[] subConceptArray = conceptArray[1].split("\\|");
                    List<String> subConceptList = new ArrayList<String>();
                    for (String subConcept : subConceptArray) {
                        subConcept = WordUtils.getWordWithOutTags(subConcept);
                        if (subConcept.equals(supConcept)) {
                            continue;
                        }
                        subConceptList.add(subConcept);
                    }
                    classifyConceptMap.put(supConcept, subConceptList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return classifyConceptMap;
    }

    /**
     * 从文件中获取基于语言学抽取的概念分类关系
     * @return
     */
    private static Map<String, List> getClassifyConceptMapFromRootFile() {
        Map<String, List> classifyConceptMap = new HashMap<String, List>();
        File clusterResultFile = FileUtils.getFile(AFTER_WORD_ROOT_FILTER_PATH);
        if (clusterResultFile.exists()) {
            try {
                LineIterator lineIterator = FileUtils.lineIterator(clusterResultFile);
                while (lineIterator.hasNext()) {
                    String line = lineIterator.next();
                    String[] conceptArray = line.split("=");
                    String supConcept = WordUtils.getWordWithOutTags(conceptArray[0]);
                    String subConcepts = conceptArray[1].substring(1, conceptArray[1].length() - 1);
                    String[] subConceptArray = subConcepts.split(", ");
                    List<String> subConceptList = new ArrayList<String>();
                    for (String subConcept : subConceptArray) {
                        subConcept = WordUtils.getWordWithOutTags(subConcept);
                        if (subConcept.equals(supConcept)) {
                            continue;
                        }
                        subConceptList.add(subConcept);
                    }
                    classifyConceptMap.put(supConcept, subConceptList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return classifyConceptMap;
    }

    /**
     * 从文件中获取基于关联规则挖掘的非分类关系
     * @return
     */
    private static Map<String, Map<String, String>> getNonClassifyConceptMapFromFile() {
        Map<String, Map<String, String>> nonClassifyConceptMap = new HashMap<String, Map<String, String>>();
        File clusterResultFile = FileUtils.getFile(CONCEPT_PAIR_WORD_VERB_FILTER_PATH);
        if (clusterResultFile.exists()) {
            try {
                LineIterator lineIterator = FileUtils.lineIterator(clusterResultFile);
                while (lineIterator.hasNext()) {
                    String line = lineIterator.next();
                    String[] lineArray = line.split("\\s+");
                    String[] conceptPair = lineArray[0].split("\\|");
                    Map<String, String> conceptVerbMap = new HashMap<String, String>();
                    conceptVerbMap.put(WordUtils.getWordWithOutTags(conceptPair[1]), WordUtils.getWordWithOutTags(lineArray[1]));
                    nonClassifyConceptMap.put(WordUtils.getWordWithOutTags(conceptPair[0]), conceptVerbMap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return nonClassifyConceptMap;
    }


}
