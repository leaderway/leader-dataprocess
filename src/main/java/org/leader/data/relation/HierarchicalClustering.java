package org.leader.data.relation;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * 基于层次聚类的概念关系抽取
 *
 * @author ldh
 * @since 2017-03-07 20:08
 */
public class HierarchicalClustering {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalClustering.class);

    private static final String WORD_TFIDF_PATH = "./data/combineWordSingleArticleTFIDF.txt";//  记录词语及其TFIDF的文件
    private static final String VSM_PATH = "./data/combineWordVSM.txt";//  记录词语向量空间模型
    private static final String CLUSTER_RESULT_PATH = "./data/JULEIcombineWordVSM.txt";//  记录聚类后词语向量空间模型
    private static final double MI_EXTRACTION_WORD_MI_THRESHOLD = 0.00032651390174099563;// 基于统计方法得到的词语抽取阈值

    public static void main(String[] args) throws Exception{
        Set<String> conceptWordSet = new HashSet<>();// 保存候选概念的set
        // 从基于统计方法得到的词语文件中获取超过阈值的词语
        String sqlMiword = "SELECT * FROM `t_combineword` WHERE mi >= ?";
        List<Map<String, Object>> miExtractionWordList = DatabaseHelper.executeQuery(sqlMiword, MI_EXTRACTION_WORD_MI_THRESHOLD);
        for (Map<String, Object> miExtractionWordMap : miExtractionWordList) {
            String miExtractionWord = String.valueOf(miExtractionWordMap.get("word"));
            conceptWordSet.add(miExtractionWord);
        }

        LOGGER.info("候选概念抽取完毕，数量为{}", conceptWordSet.size());

        // 使用层次聚类进行概念分类关系抽取
        Map<String, Map<Integer, Double>> combineWordSingleArticleTFIDFMap = new TreeMap<String, Map<Integer, Double>>();// 保存组合词及其在每篇文章中的TFIDF值
        File combineWordSingleArticleTFIDFFile = FileUtils.getFile(WORD_TFIDF_PATH);
        if (combineWordSingleArticleTFIDFFile.exists()) {
            LineIterator lineIterator = FileUtils.lineIterator(combineWordSingleArticleTFIDFFile);
            while (lineIterator.hasNext()) {
                String line = lineIterator.next();
                String[] combineWordSingleArticleTFIDFArray = line.split("-");
                String combineWord = combineWordSingleArticleTFIDFArray[0];// 组合词

                String[] singleArticleTFIDFArray = combineWordSingleArticleTFIDFArray[1].split(" ");
                for (String singleArticleTFIDF : singleArticleTFIDFArray) {
                    Map<Integer, Double> singleArticleTFIDFMap = new HashMap<Integer, Double>();// 保存每篇文章的id及词语在此文章的TFIDF
                    String[] articleTFIDFArray = singleArticleTFIDF.split(":");
                    int articleId = Integer.parseInt(articleTFIDFArray[0]);
                    double tfidf = Double.parseDouble(articleTFIDFArray[1]);
                    if (combineWordSingleArticleTFIDFMap.containsKey(combineWord)) {
                        singleArticleTFIDFMap = combineWordSingleArticleTFIDFMap.get(combineWord);
                    }
                    singleArticleTFIDFMap.put(articleId, tfidf);
                    combineWordSingleArticleTFIDFMap.put(combineWord, singleArticleTFIDFMap);
                }
            }
        }
        LOGGER.info("读入组合词TFIDF文件成功");

        // 构造向量空间模型
        Map<String, Double> vsmMap = new TreeMap<String, Double>();// 保存向量空间模型:组合词对：相似度
        Map<String, Double> vsmSingleMap = new TreeMap<String, Double>();// 保存向量空间模型(去重):组合词对：相似度
        double firstMaxSim = 0.0d;
        String maxSimWordPair = "";
        if (CollectionUtil.isNotEmpty(combineWordSingleArticleTFIDFMap)) {
            for (String combineWord : conceptWordSet) {
                for (String combineWordOther : conceptWordSet) {

                    String vsmName = combineWord + "|" + combineWordOther;
                    String vsmNameReverse = combineWordOther + "|" + combineWord;
                    // 已经计算过的就不需要重复计算了
                    if (vsmMap.containsKey(vsmName) || vsmMap.containsKey(vsmNameReverse)) {
                        continue;
                    }

                   // 计算余弦距离
                    Map<Integer, Double> combineWordTFIDFMap = combineWordSingleArticleTFIDFMap.get(combineWord);
                    Map<Integer, Double> combineWordOtherTFIDFMap = combineWordSingleArticleTFIDFMap.get(combineWordOther);
                    //if (CollectionUtil.isEmpty(combineWordOtherTFIDFMap)) {
                    //    LOGGER.error("combineWordOtherTFIDFMap为空：combineWordOther：{}", combineWordOther);
                    //}

                    double sumCombineWordMultOther = 0.0d;// 两向量乘积
                    double combineWordSq = 0.0d;// combineWord的平方和
                    double combineWordOtherSq = 0.0d;// combineWordOther的平方和
                    for (Map.Entry<Integer, Double> combineWordTFIDF : combineWordTFIDFMap.entrySet()) {
                        double combineWordTfidf = combineWordTFIDF.getValue();
                        combineWordSq += Math.pow(combineWordTfidf, 2.0d);

                        int articleId = combineWordTFIDF.getKey();
                        if (combineWordOtherTFIDFMap.containsKey(articleId)) {
                            double combineWordOtherTfidf = combineWordOtherTFIDFMap.get(articleId);
                            sumCombineWordMultOther += combineWordOtherTfidf * combineWordTfidf;
                        }
                    }

                    for (Map.Entry<Integer, Double> combineWordOtherTFIDF : combineWordOtherTFIDFMap.entrySet()) {
                        double combineWordOtherTfidf = combineWordOtherTFIDF.getValue();
                        combineWordOtherSq += Math.pow(combineWordOtherTfidf, 2.0d);
                    }
                    // 计算余弦距离
                    double similarity = sumCombineWordMultOther / (Math.pow(combineWordSq * combineWordOtherSq, 0.5d));
                    if (similarity != 1 && similarity > firstMaxSim) {
                        firstMaxSim = similarity;
                        maxSimWordPair = vsmName;
                    }
                    vsmSingleMap.put(vsmName, similarity);
                    vsmMap.put(vsmName, similarity);
                    vsmMap.put(vsmNameReverse, similarity);
                }
            }
        }
        if (CollectionUtil.isNotEmpty(vsmMap)) {
            List<Map.Entry<String, Double>> vsmMapList = new ArrayList<Map.Entry<String, Double>>(vsmSingleMap.entrySet());
            FileUtils.writeLines(new File(VSM_PATH), vsmMapList);
        }
        Map<String, Double> afterClusterSingleVSMMap = vsmSingleMap;// 聚类后重新构建去重空间向量模型，第一次与vsmSingleMap相同
        hieCluster(conceptWordSet,afterClusterSingleVSMMap, vsmSingleMap, 0.05);
        LOGGER.info("构建向量空间模型完成,最大的相似度是：{}，词组为{}",firstMaxSim, maxSimWordPair);
    }

    /**
     * 层次聚类
     * @param conceptWordSet
     * @param afterClusterSingleVSMMap
     * @param vsmSingleMap
     * @param simThreshold
     * @throws Exception
     */
    private static void hieCluster(Set<String> conceptWordSet, Map<String, Double> afterClusterSingleVSMMap, Map<String, Double> vsmSingleMap, double simThreshold) throws Exception{

        // 计算已经聚成的簇与剩余的词语的相似度，使用簇中每个词语与其他词语的平均相似度
        Map<String, Double> afterClusterVSMMap = new TreeMap<String, Double>();// 聚类后重新构建空间向量模型
        afterClusterSingleVSMMap = new HashMap<>();// 聚类后重新构建去重空间向量模型
        double maxSim = 0.0;
        String maxSimCombineWord = "";
        String maxSimCombineWordOther = "";

        for (String combineWord : conceptWordSet) {
            for (String combineWordOther : conceptWordSet) {

                String vsmName = combineWord + "|" + combineWordOther;
                String vsmNameReverse = combineWordOther + "|" + combineWord;
                if (afterClusterVSMMap.containsKey(vsmName) || afterClusterVSMMap.containsKey(vsmNameReverse)) {
                    continue;
                }
                if (combineWord.equals(combineWordOther)) {
                    //System.out.println("相同词语处理" + combineWord);
                    afterClusterVSMMap.put(vsmName, 1.0d);
                    afterClusterVSMMap.put(vsmNameReverse, 1.0d);
                    afterClusterSingleVSMMap.put(vsmName, 1.0d);
                    continue;
                }

                String[] combineWordArray = combineWord.split("\\|");
                String[] combineWordOtherArray = combineWordOther.split("\\|");
                double simSum = 0.0d;
                for (String combineWordEntry : combineWordArray) {
                    for (String combineWordOtherEntry : combineWordOtherArray) {
                        String vsmNameEntry = combineWordEntry + "|" + combineWordOtherEntry;
                        String vsmReverseNameEntry = combineWordOtherEntry + "|" + combineWordEntry;
                        if (vsmSingleMap.containsKey(vsmNameEntry)) {
                            simSum += vsmSingleMap.get(vsmNameEntry);
                        } else  if (vsmSingleMap.containsKey(vsmReverseNameEntry)){
                            simSum += vsmSingleMap.get(vsmReverseNameEntry);
                        } else {
                            System.out.println("vsmSingleMap不存在key" + vsmNameEntry);
                        }
                    }
                }
                double simAvg = simSum / (combineWordArray.length * combineWordOtherArray.length);// 平均相似度

                if (simAvg != 1.0d && simAvg > maxSim) {
                    maxSim = simAvg;
                    maxSimCombineWord = combineWord;
                    maxSimCombineWordOther = combineWordOther;
                }
                afterClusterVSMMap.put(vsmName, simAvg);
                afterClusterVSMMap.put(vsmNameReverse, simAvg);
                afterClusterSingleVSMMap.put(vsmName, simAvg);
            }
        }
        conceptWordSet.remove(maxSimCombineWord);
        conceptWordSet.remove(maxSimCombineWordOther);
        conceptWordSet.add(maxSimCombineWord + "|" + maxSimCombineWordOther);// 将组合词作为一簇放入到词表中
        System.out.println("相似度最大的组合词对" + maxSimCombineWord + "|" + maxSimCombineWordOther + " 相似度为：" + maxSim);
        if (maxSim < simThreshold) {
            System.out.println("层次聚类结束");
            Set<String> clusterResult = new HashSet<String>();
            for (String cluster : conceptWordSet) {
                if (cluster.indexOf("|") != -1) {
                    clusterResult.add(cluster);
                }
            }
            FileUtils.writeLines(new File(CLUSTER_RESULT_PATH), clusterResult);
            return;// 结束执行
        }
        // 递归调用
        hieCluster(conceptWordSet, afterClusterSingleVSMMap, vsmSingleMap, simThreshold);

    }

}
