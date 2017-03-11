package org.leader.data.util;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import org.leader.framework.helper.ConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * 本体工具类
 *
 * @author ldh
 * @since 2017-03-11 12:55
 */
public class OntologyUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyUtils.class);
    private static final String DB_TYPE = "MySQL";

    /**
     * 导出本体OWL文件
     * @param model
     */
    public static void outputOWLFile(Model model, String owlFilePath, String lang) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(owlFilePath);
            RDFWriter rdfWriter = model.getWriter(lang);
            rdfWriter.setProperty("showXMLDeclaration","true");
            rdfWriter.setProperty("showDoctypeDeclaration", "true");
            rdfWriter.write(model, fileOutputStream, null);
        } catch (FileNotFoundException e) {
            LOGGER.error("导出本体文件异常{}", e);
        }
    }

    /**
     * 将本体模型存入数据库中
     * @param modelName
     * @param model
     * @return
     */
    public static boolean storeOntologyToDB(DBConnection connection, String modelName, Model model) {
        try {
            ModelMaker maker = ModelFactory.createModelRDBMaker(connection);
            Model persistantModel = maker.createModel(modelName, true);
            persistantModel.begin();
            persistantModel.add(model);
            persistantModel.commit();
            return true;
        } catch (Exception e) {
            LOGGER.error("获取数据库驱动失败");
            return false;
        }
    }

    public static Model getModelFromDB(DBConnection connection, String modelName) {
        ModelMaker maker = ModelFactory.createModelRDBMaker(connection);
        return maker.openModel(modelName);
    }

    /**
     * 获取ModelMaker
     * @param connection
     * @return
     */
    public static ModelMaker getModelMaker(DBConnection connection) {
        return ModelFactory.createModelRDBMaker(connection);
    }

    /**
     * 获取数据库连接
     * @return
     */
    public static DBConnection getDBConnection() {
        try {
            Class.forName(ConfigHelper.getJdbcDriver());
            DBConnection connection = new DBConnection(ConfigHelper.getJdbcUrl(),
                    ConfigHelper.getJdbcUsername(), ConfigHelper.getJdbcPassword(), DB_TYPE);
            return connection;
        } catch (Exception e) {
            LOGGER.error("获取数据库连接失败：{}", e);
            return null;
        }
    }
}
