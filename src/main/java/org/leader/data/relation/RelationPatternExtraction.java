package org.leader.data.relation;

import org.apache.commons.io.FileUtils;
import org.leader.data.util.MapUtils;
import org.leader.data.util.MathUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.CollectionUtil;
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
    private static final String FORMER_WORD_ROOT_PATH = "./data/formerWordRoot.txt";// 前缀词根及其词语文件
    private static final String FORMER_WORD_ROOT_FILTER_PATH = "./data/formerWordRootFilter.txt";// 筛选后前缀词根及其词语文件
    private static final String AFTER_WORD_ROOT_PATH = "./data/afterWordRoot.txt";// 后缀词根及其词语文件
    private static final String AFTER_WORD_ROOT_FILTER_PATH = "./data/afterWordRootFilter.txt";// 筛选后后缀词根及其词语文件
    private static final String CONCEPT_PAIR_WORD_VERB_PMI_PATH = "./data/conceptVerbPMI.txt";// 概念对与动词点互信息文件
    private static final String CONCEPT_PAIR_WORD_VERB_FILTER_PATH = "./data/conceptVerbFilter.txt";// 概念对与动词经过筛选后的文件

    private static final int PATTERN_EXTRACTION_WORD_COUNT_THRESHOLD = 15;// 规则匹配得到的词语抽取阈值
    private static final double MI_EXTRACTION_WORD_MI_THRESHOLD = 0.00032651390174099563;// 基于统计方法得到的词语抽取阈值
    private static final double SUPPORT_THRESHOLD = 0.0d;// 支持度抽取阈值
    private static final double CONFIDENCE_THRESHOLD = 0.0d;// 置信度抽取阈值
    private static final double PMI_THRESHOLD = 0.0d;// 点置信度抽取阈值
    private static final int NGRAMS = 2;//n-grams
    private static final int INTER_MAX_NGRAMS = 3;//概念对之间词语的最大grams

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

        // 通过词根构建分类关系
        String regexCombineWordSeperate = "<[^>]+?>([^<>]+?)</[^>]+?>";
        Pattern patternCombineWordSeperate = Pattern.compile(regexCombineWordSeperate);
        Map<String, Set<String>> combineWordFormerRootMap = new TreeMap<String, Set<String>>();// 前缀词根
        Map<String, Set<String>> combineWordAfterRootMap = new TreeMap<String, Set<String>>();// 后缀词根
        for (String combineWord : conceptWordSet) {
            // 分割组合词
            List<String> singleWordList = new ArrayList<String>();
            Matcher matcherCombineWordSeperate = patternCombineWordSeperate.matcher(combineWord);
            while (matcherCombineWordSeperate.find()) {
                String singleWord = matcherCombineWordSeperate.group(0);
                singleWordList.add(singleWord);
            }

            Set<String> combineWordFormerRootSet = new HashSet<String>();
            Set<String> combineWordAfterRootSet = new HashSet<String>();
            String formerRoot = singleWordList.get(0);
            String afterRoot = singleWordList.get(singleWordList.size() - 1);
            // 前缀词根
            if (combineWordFormerRootMap.containsKey(formerRoot)) {
                combineWordFormerRootSet = combineWordFormerRootMap.get(formerRoot);
            }
            combineWordFormerRootSet.add(combineWord);
            combineWordFormerRootMap.put(formerRoot, combineWordFormerRootSet);
            // 后缀词根
            if (combineWordAfterRootMap.containsKey(afterRoot)) {
                combineWordAfterRootSet = combineWordAfterRootMap.get(afterRoot);
            }
            combineWordAfterRootSet.add(combineWord);
            combineWordAfterRootMap.put(afterRoot, combineWordAfterRootSet);
        }

        // 筛选出词根中词语数量大于2的词根
        Map<String, Set<String>> combineWordFormerRootFilterMap = new TreeMap<String, Set<String>>();// 筛选后的前缀词根
        Map<String, Set<String>> combineWordAfterRootFilterMap = new TreeMap<String, Set<String>>();// 筛选后的后缀词根

        for (Map.Entry<String, Set<String>> combineWordFormerRootMapEntry : combineWordFormerRootMap.entrySet()) {
            Set<String> formerRootWordSet = combineWordFormerRootMapEntry.getValue();
            if (formerRootWordSet.size() >= 2) {
                combineWordFormerRootFilterMap.put(combineWordFormerRootMapEntry.getKey(), formerRootWordSet);
            }
        }
        for (Map.Entry<String, Set<String>> combineWordAfterRootMapEntry : combineWordAfterRootMap.entrySet()) {
            Set<String> afterRootWordSet = combineWordAfterRootMapEntry.getValue();
            if (afterRootWordSet.size() >= 2) {
                combineWordAfterRootFilterMap.put(combineWordAfterRootMapEntry.getKey(), afterRootWordSet);
            }
        }

        // 获取所有句子，以句子为事务
        String sqlSelectSentenceToken = "SELECT sentenceToken, id FROM t_sentence ";
        List<Map<String, Object>> sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken);
        if (CollectionUtil.isNotEmpty(sentenceTokenList)) {
            Map<Integer, List<String>> sentenceConceptMap = new TreeMap<Integer, List<String>>(); // 保存句子中存在的概念词语，目的是获取概念对之间的词语
            Map<Integer, String> sentenceIdMap = new TreeMap<Integer, String>();// 保存id和句子的映射，目的是根据id获取句子
            Map<String, Set<Integer>> verbIdMap = new TreeMap<String, Set<Integer>>();// 保存所有动词和句子id的映射,目的是获取动词出现的事务数，计算点互信息

            String regex = "(<[^>]+?>([^<>]+?)</[^>]+?>){" + NGRAMS + "}";
            String regexVerb = "<v[^>]*?>([^<>]+?)</v[^>]*?>";// 提取句子中的所有动词
            Pattern pattern = Pattern.compile(regex);
            Pattern patternVerb = Pattern.compile(regexVerb);

            for (Map<String, Object> sentenceTokenMap : sentenceTokenList) {
                String sentenceToken = String.valueOf(sentenceTokenMap.get("sentenceToken"));
                int id = Integer.parseInt(String.valueOf(sentenceTokenMap.get("id")));// 句子Id
                sentenceIdMap.put(id, sentenceToken);// 句子id与句子内容映射

                // 提取句子中的所有动词
                Matcher matcherVerb = patternVerb.matcher(sentenceToken);
                while (matcherVerb.find()) {
                    String verbWord = matcherVerb.group(0);
                    Set<Integer> verbIdSet = new HashSet<Integer>();
                    if (verbIdMap.containsKey(verbWord)) {
                        verbIdSet = verbIdMap.get(verbWord);
                    }
                    verbIdSet.add(id);
                    verbIdMap.put(verbWord, verbIdSet);
                }

                int regexIndex = 0;
                String sentence = sentenceToken;
                List<String> sentenctConceptWordList = new ArrayList<String>();// 保存句子中存在的概念词
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
                                sentenctConceptWordList = sentenceConceptMap.get(id);
                            }
                            sentenctConceptWordList.add(wordMatch);
                            sentenceConceptMap.put(id, sentenctConceptWordList);
                            LOGGER.info("存储概念成功：id={}, word={}", id, wordMatch);
                        }
                        break;// 只获取匹配到的第一个
                    }
                    regexIndex = sentence.indexOf("><") + 1;
                }
            }
            LOGGER.info("概念及其存在的句子映射完成");

            Map<String, Set<String>> conceptPairInterMap = new HashMap<String,  Set<String>>();// 保存概念对之间的所有词语
            Map<String, Map<String, Set<Integer>>> conceptPairVerbMap = new HashMap<String, Map<String, Set<Integer>>>();// 保存概念对之间的动词及出现的事务id，用以计算点互信息
            Map<String, Set<Integer>> conceptPairConOccurMap = new HashMap<String, Set<Integer>>();// 保存概念对出现的所有句子id
            Map<String, Set<Integer>> conceptSentenceIdSetMap = new HashMap<String, Set<Integer>>();// 保存单个概念出现的所有句子id，用来计算置信度

            String regexInter = "(<[^>]+?>([^<>]+?)</[^>]+?>){1," + INTER_MAX_NGRAMS + "}";// 控制概念对之间词语抽取的最大数量
            Pattern patternInter = Pattern.compile(regexInter);

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

                            // 保存概念对及其出现的句子id，用来计算支持度
                            Set<Integer> conceptPairSentenceIdSet = new HashSet<Integer>();
                            if (conceptPairConOccurMap.containsKey(conceptPairString)) {
                                conceptPairSentenceIdSet = conceptPairConOccurMap.get(conceptPairString);
                            }
                            conceptPairSentenceIdSet.add(id);
                            conceptPairConOccurMap.put(conceptPairString, conceptPairSentenceIdSet);

                            // 保存概念对及其之间的词语
                            // 判断之间的词语是否符合正则要求
                            if (! patternInter.matcher(interWord).matches()) {
                                continue;
                            }
                            // 提取概念对之间的动词
                            Matcher interVerbMatcher = patternVerb.matcher(interWord);
                            while (interVerbMatcher.find()) {
                                String interVerb = interVerbMatcher.group(0);
                                Map<String, Set<Integer>> verbIdSetMap = new HashMap<String, Set<Integer>>();
                                Set<Integer> verbIdSet = new HashSet<Integer>();
                                if (conceptPairVerbMap.containsKey(conceptPairString)) {
                                    verbIdSetMap = conceptPairVerbMap.get(conceptPairString);
                                    if (verbIdSetMap.containsKey(interVerb)) {
                                        verbIdSet = verbIdSetMap.get(interVerb);
                                    }
                                }
                                verbIdSet.add(id);
                                verbIdSetMap.put(interVerb, verbIdSet);
                                conceptPairVerbMap.put(conceptPairString, verbIdSetMap);
                            }

                            String afterAfterWord = "";
                            if ((endIndex + afterCombineWord.length()) < (sentenceToken.length() - 1)) {
                                afterAfterWord = sentenceToken.substring(endIndex + afterCombineWord.length(), sentenceToken.indexOf("</", endIndex + afterCombineWord.length()));
                            }
                            Set<String> interWordSet = new HashSet<String>();
                            if (conceptPairInterMap.containsKey(conceptPairString)) {
                                interWordSet = conceptPairInterMap.get(conceptPairString);
                            }
                            interWordSet.add(id + "|" + interWord + "|" + afterAfterWord);
                            conceptPairInterMap.put(conceptPairString, interWordSet);
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
            Map<String, Double> pmiMap = new TreeMap<String, Double>();// 保存两个概念和动词的点互信息

            int sentenceTotal = sentenceTokenList.size();// 事务总数，即句子总数
            for (Map.Entry<String, Set<Integer>> conceptPairConOccurMapEntry : conceptPairConOccurMap.entrySet()) {
                String conceptPair = conceptPairConOccurMapEntry.getKey();
                String[] conceptPairArray = conceptPair.split("\\|");
                String conceptPairReverse = conceptPairArray[1] + "|" + conceptPairArray[0];// 反转概念对
                Set<Integer> conceptPairOccurTimesSet = conceptPairConOccurMapEntry.getValue();
                if (conceptPairConOccurMap.containsKey(conceptPairReverse)) {
                    conceptPairOccurTimesSet.addAll(conceptPairConOccurMap.get(conceptPairReverse));
                }
                int conceptPairTotalOccurTimes = conceptPairOccurTimesSet.size();

                // 计算支持度 t(X,Y)/N
                double support = conceptPairTotalOccurTimes * 1.0d / sentenceTotal;
                supportMap.put(conceptPair, support);

                // 计算置信度 t(X,Y)/t(X)
                int conceptPairPrefixIdCount = conceptSentenceIdSetMap.get(conceptPairArray[0]).size();
                double confidence = conceptPairTotalOccurTimes*1.0d / conceptPairPrefixIdCount;
                //confidenceMap.put(conceptPair + "t(X,Y)=" + conceptPairTotalOccurTimes + " t(X)=" + conceptPairPrefixIdCount, confidence);
                confidenceMap.put(conceptPair, confidence);

                // 计算概念对与动词的点互信息 log(P(X,Y)/(P(X)*P(Y)))
                if (conceptPairVerbMap.containsKey(conceptPair)) {
                    Map<String, Set<Integer>> verbIdSetMap = conceptPairVerbMap.get(conceptPair);
                    for (Map.Entry<String, Set<Integer>> verbIdSetMapEntry : verbIdSetMap.entrySet()) {
                        String verb = verbIdSetMapEntry.getKey();
                        Set<Integer> verbIdSet = verbIdSetMapEntry.getValue();
                        double pXY = verbIdSet.size()* 1.0d  / sentenceTotal;
                        int verbCount = verbIdMap.get(verb).size();
                        double pY = verbCount * 1.0d / sentenceTotal;

                        double pmi = MathUtils.log(pXY / (support * pY), 2.0d);
                        pmiMap.put(conceptPair + "-->" + verb, pmi);
                    }
                }
            }

            // 根据指定的PMI、最小支持度和最小置信度筛选概念对及动词
            //Map<String, String> conceptPairFilterMap = new TreeMap<String, String>();
            List<String> conceptPairVerbFilterList = new ArrayList<String>();
            for (Map.Entry<String, Double> pmiMapEntry : pmiMap.entrySet()) {
                String conceptPairVerb = pmiMapEntry.getKey();
                String[] conceptPairVerbArray = conceptPairVerb.split("-->");
                String conceptPair = conceptPairVerbArray[0];
                String verb = conceptPairVerbArray[1];

                double pmi = pmiMapEntry.getValue();
                double support = supportMap.get(conceptPair);
                double confidence = confidenceMap.get(conceptPair);
                if (pmi >= PMI_THRESHOLD && support >= SUPPORT_THRESHOLD && confidence >= CONFIDENCE_THRESHOLD) {
                    //conceptPairFilterMap.put(conceptPairVerb, verb);
                    conceptPairVerbFilterList.add(conceptPair + " " + verb);
                }
            }

            LOGGER.info("计算概念对的支持度和置信度完成");
            LOGGER.info("开始输出文件");
            FileUtils.writeLines(new File(FORMER_WORD_ROOT_PATH), new ArrayList<Map.Entry<String, Set<String>>>(combineWordFormerRootMap.entrySet()));
            FileUtils.writeLines(new File(FORMER_WORD_ROOT_FILTER_PATH), new ArrayList<Map.Entry<String, Set<String>>>(combineWordAfterRootFilterMap.entrySet()));
            FileUtils.writeLines(new File(AFTER_WORD_ROOT_PATH), new ArrayList<Map.Entry<String, Set<String>>>(combineWordAfterRootMap.entrySet()));
            FileUtils.writeLines(new File(AFTER_WORD_ROOT_FILTER_PATH), new ArrayList<Map.Entry<String, Set<String>>>(combineWordAfterRootFilterMap.entrySet()));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_INTER_PATH), new ArrayList<Map.Entry<String, Set<String>>>(conceptPairInterMap.entrySet()));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_SUPORT_PATH), MapUtils.sortDoubleMap(supportMap));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_CONFIDENCE_PATH), MapUtils.sortDoubleMap(confidenceMap));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_VERB_PMI_PATH), MapUtils.sortDoubleMap(pmiMap));
            //FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_VERB_FILTER_PATH), new ArrayList<Map.Entry<String, String>>(conceptPairFilterMap.entrySet()));
            FileUtils.writeLines(new File(CONCEPT_PAIR_WORD_VERB_FILTER_PATH), conceptPairVerbFilterList);
            LOGGER.info("结束输出文件");

        }


    }

}
