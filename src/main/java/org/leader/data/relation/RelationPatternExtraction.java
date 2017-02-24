package org.leader.data.relation;

import org.apache.commons.io.FileUtils;
import org.leader.data.util.MapUtils;
import org.leader.data.util.WordUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.CollectionUtil;
import org.leader.framework.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 概念关系模式抽取
 * @author ldh
 * @since 2017-02-23 15:55
 */
public class RelationPatternExtraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationPatternExtraction.class);
    private static final String PATTERN_EXTRACTION_WORD_PATH = "./data/patternextraction20170223154514.txt";// 规则匹配得到的词语的文件
    private static final String CONCEPT_PAIR_WORD_INTER_PATH = "./data/conceptPairInter.txt";// 概念对之间的词语文件
    private static final String CONCEPT_PAIR_WORD_SUPORT_PATH = "./data/conceptPairSupport.txt";// 概念对支持度文件
    private static final String CONCEPT_PAIR_WORD_CONFIDENCE_PATH = "./data/conceptPairConfidence.txt";// 概念对置信度文件

    private static final int PATTERN_EXTRACTION_WORD_COUNT_THRESHOLD = 15;// 规则匹配得到的词语抽取阈值
    private static final double MI_EXTRACTION_WORD_MI_THRESHOLD = 0.00032651390174099563;// 基于统计方法得到的词语抽取阈值
    private static final int NGRAMS = 2;//n-grams

    public static void main(String[] args) throws Exception{
        Set<String> conceptWordSet = new HashSet<>();// 保存候选概念的set

        // 从规则匹配得到的词语的文件中获取超过阈值的词语
        Map<String, String> patternExtractionWordMap = MapUtils.getMapFromFile(PATTERN_EXTRACTION_WORD_PATH, "\\s+");
        for (Map.Entry<String, String> patternExtractionWordEntry : patternExtractionWordMap.entrySet()) {
            int patternExtractionWordCount = Integer.parseInt(patternExtractionWordEntry.getValue());
            if (patternExtractionWordCount >= PATTERN_EXTRACTION_WORD_COUNT_THRESHOLD) {
                String patternExtractionWord = patternExtractionWordEntry.getKey();
                conceptWordSet.add(patternExtractionWord);
            }
        }

        // 从基于统计方法得到的词语文件中获取超过阈值的词语
        String sqlMiword = "SELECT * FROM `t_combineword` WHERE mi >= ?";
        List<Map<String, Object>> miExtractionWordList = DatabaseHelper.executeQuery(sqlMiword, MI_EXTRACTION_WORD_MI_THRESHOLD);
        for (Map<String, Object> miExtractionWordMap : miExtractionWordList) {
            String miExtractionWord = String.valueOf(miExtractionWordMap.get("word"));
            conceptWordSet.add(miExtractionWord);
        }

        LOGGER.info("候选概念抽取完毕，数量为{}", conceptWordSet.size());

        // 获取所有句子，以句子为事务
        String sqlSelectSentenceToken = "SELECT sentenceToken, id FROM t_sentence ";
        List<Map<String, Object>> sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken);
        if (CollectionUtil.isNotEmpty(sentenceTokenList)) {
            //Map<Integer, Set<String>> sentenceConceptMap = new TreeMap<Integer, Set<String>>(); // 保存句子中存在的概念词语
            Map<Integer, List<String>> sentenceConceptMap = new TreeMap<Integer, List<String>>(); // 保存句子中存在的概念词语
            Map<Integer, String> sentenceIdMap = new TreeMap<Integer, String>();// 保存id和句子的映射
            String regex = "(<[^>]+?>([^<>]+?)</[^>]+?>){" + NGRAMS + "}";
            Pattern pattern = Pattern.compile(regex);

            for (Map<String, Object> sentenceTokenMap : sentenceTokenList) {
                String sentenceToken = String.valueOf(sentenceTokenMap.get("sentenceToken"));
                int id = Integer.parseInt(String.valueOf(sentenceTokenMap.get("id")));// 句子Id
                sentenceIdMap.put(id, sentenceToken);

                int regexIndex = 0;
                String sentence = sentenceToken;
                List<String> sentenctConceptWordSet = new ArrayList<String>();// 保存句子中存在的概念词
                //Set<String> sentenctConceptWordSet = new HashSet<String>();// 保存句子中存在的概念词
                System.out.println("正在处理句子" + sentenceToken);
                while (sentence.indexOf("><") != -1) {
                    sentence = sentence.substring(regexIndex);
                    Matcher matcher = pattern.matcher(sentence);
                    String wordMatch = "";// 匹配到的词语
                    while (matcher.find()) {
                        wordMatch = matcher.group(0);
                        // 过滤停用词
                        if (wordMatch.indexOf("<w") != -1 ||wordMatch.indexOf("<u") != -1
                                || wordMatch.indexOf("<m") != -1
                                || wordMatch.indexOf("<d") != -1
                                || wordMatch.indexOf("<en") != -1
                                || wordMatch.indexOf("<f") != -1
                                || wordMatch.indexOf("<b") != -1
                                || wordMatch.indexOf("<c") != -1
                                || wordMatch.indexOf("<t") != -1
                                || wordMatch.indexOf("<p") != -1
                                || wordMatch.indexOf("<q") != -1
                                || wordMatch.indexOf("<n") == -1   // 词中不包含有名词
                                || wordMatch.indexOf("<r") != -1) {
                            break;
                        }
                        // 存储句子id及其包含的概念词
                        if (conceptWordSet.contains(wordMatch)) {
                            if (sentenceConceptMap.containsKey(id)) {
                                sentenctConceptWordSet = sentenceConceptMap.get(id);
                            }
                            sentenctConceptWordSet.add(wordMatch);
                            sentenceConceptMap.put(id, sentenctConceptWordSet);
                            LOGGER.info("存储概念成功：id={}, word={}", id, wordMatch);
                        }
                        break;// 只获取匹配到的第一个
                    }
                    regexIndex = sentence.indexOf("><") + 1;
                }
            }
            LOGGER.info("概念及其存在的句子映射完成");

            Map<String, Set<String>> conceptPairInterMap = new HashMap<String,  Set<String>>();// 保存概念对之间的所有词语
            Map<String, Set<Integer>> conceptPairConOccurMap = new HashMap<String, Set<Integer>>();// 保存概念对出现的所有句子id
            Map<String, Set<Integer>> conceptSentenceIdSetMap = new HashMap<String, Set<Integer>>();// 保存单个概念出现的所有句子id，用来计算置信度

            for (Map.Entry<Integer, List<String>> sentenceConceptMapEntry : sentenceConceptMap.entrySet()) {
                List<String> sentenceConceptList = sentenceConceptMapEntry.getValue();
                int id = sentenceConceptMapEntry.getKey();
                if (sentenceConceptList.size() >= 2) {
                    String sentenceToken = sentenceIdMap.get(id);
                    int startIndex = 0;
                    int endIndex = 0;
                    for (int i=0; i<sentenceConceptList.size()-1; i++) {
                        String formerCombineWord = sentenceConceptList.get(i);
                        String afterCombineWord = sentenceConceptList.get(i + 1);

                        // 保存单个概念出现的所有句子id，用来计算置信度
                        Set<Integer> sentenceIdSet = new HashSet<Integer>();
                        if (conceptSentenceIdSetMap.containsKey(formerCombineWord)) {
                            sentenceIdSet = conceptSentenceIdSetMap.get(formerCombineWord);
                        }
                        sentenceIdSet.add(id);
                        conceptSentenceIdSetMap.put(formerCombineWord, sentenceIdSet);
                        if (i == sentenceConceptList.size() - 2) {
                            if (conceptSentenceIdSetMap.containsKey(afterCombineWord)) {
                                sentenceIdSet = conceptSentenceIdSetMap.get(afterCombineWord);
                            }
                            sentenceIdSet.add(id);
                            conceptSentenceIdSetMap.put(afterCombineWord, sentenceIdSet);
                        }

                        startIndex = sentenceToken.indexOf(formerCombineWord, endIndex);
                        endIndex = sentenceToken.indexOf(afterCombineWord, startIndex);
                        if (startIndex != -1 && endIndex != -1 && (endIndex - startIndex) > formerCombineWord.length()) {
                            String interWord = sentenceToken.substring(startIndex + formerCombineWord.length(), endIndex);
                            String conceptPairString = formerCombineWord + "|" + afterCombineWord;

                            // 保存概念对及其之间的词语
                            Set<String> interWordSet = new HashSet<String>();
                            if (conceptPairInterMap.containsKey(conceptPairString)) {
                                interWordSet = conceptPairInterMap.get(conceptPairString);
                            }
                            interWordSet.add(id + "|" + interWord);
                            conceptPairInterMap.put(conceptPairString, interWordSet);

                            // 保存概念对及其出现的句子id，用来计算支持度
                            Set<Integer> conceptPairSentenceIdSet = new HashSet<Integer>();
                            if (conceptPairConOccurMap.containsKey(conceptPairString)) {
                                conceptPairSentenceIdSet = conceptPairConOccurMap.get(conceptPairString);
                            }
                            conceptPairSentenceIdSet.add(id);
                            conceptPairConOccurMap.put(conceptPairString, conceptPairSentenceIdSet);
                        }
                    }
                } else {
                    String conceptString = sentenceConceptList.get(0);
                    Set<Integer> sentenceIdSet = new HashSet<Integer>();
                    if (conceptSentenceIdSetMap.containsKey(conceptString)) {
                        sentenceIdSet = conceptSentenceIdSetMap.get(conceptString);
                    }
                    sentenceIdSet.add(id);
                    conceptSentenceIdSetMap.put(conceptString, sentenceIdSet);
                }
            }
            LOGGER.info("概念之间词语抽取结束");

            Map<String, Double> supportMap = new TreeMap<String, Double>();// 保存两个概念的支持度
            Map<String, Double> confidenceMap = new TreeMap<String, Double>();// 保存两个概念的置信度
            int sentenceTotal = sentenceTokenList.size();// 事务总数，即句子总数
            for (Map.Entry<String, Set<Integer>> conceptPairConOccurMapEntry : conceptPairConOccurMap.entrySet()) {
                String conceptPair = conceptPairConOccurMapEntry.getKey();
                String[] conceptPairArray = conceptPair.split("\\|");
                String conceptPairReverse = conceptPairArray[1] + "|" + conceptPairArray[0];// 反转概念对
                int conceptPairOccurTimes = conceptPairConOccurMapEntry.getValue().size();
                int conceptPairReverseOccurTimes = 0;
                if (conceptPairConOccurMap.containsKey(conceptPairReverse)) {
                    conceptPairReverseOccurTimes = conceptPairConOccurMap.get(conceptPairReverse).size();// 反转概念对出现次数
                }
                int conceptPairTotalOccurTimes = conceptPairOccurTimes + conceptPairReverseOccurTimes;

                // 计算支持度 t(X,Y)/N
                double support = conceptPairTotalOccurTimes * 1.0d / sentenceTotal;
                supportMap.put(conceptPair, support);

                // 计算置信度 t(X,Y)/t(X)
                double confidence = conceptPairTotalOccurTimes*1.0d / conceptSentenceIdSetMap.get(conceptPairArray[0]).size();
                confidenceMap.put(conceptPair, confidence);
            }

            LOGGER.info("计算概念对的支持度和置信度完成");
            LOGGER.info("开始输出文件");
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_INTER_PATH), new ArrayList<Map.Entry<String, Set<String>>>(conceptPairInterMap.entrySet()));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_SUPORT_PATH), new ArrayList<Map.Entry<String, Double>>(supportMap.entrySet()));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_CONFIDENCE_PATH), new ArrayList<Map.Entry<String, Double>>(confidenceMap.entrySet()));
            LOGGER.info("结束输出文件");

        }


    }

}
