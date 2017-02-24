package org.leader.data.concept;

import org.apache.commons.io.FileUtils;
import org.leader.data.util.MapUtils;
import org.leader.data.util.MathUtils;
import org.leader.data.util.WordUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.CollectionUtil;
import org.leader.framework.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于互信息的概念抽取
 *
 * @author ldh
 * @since 2017-02-17 0:01
 */
public class MIConceptExtractionV3 {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIConceptExtractionV3.class);
    private static final int LIMIT_SIZE = 500;// 每次从数据库取出的数据大小
    private static final int NGRAMS = 2;//n-grams
    private static final String DATA_SENTENCE_WORD_TXT = "./data/sentenceWord.txt";//  记录词频的文件

    private static int FREQ_THRESHOLD = 10;// 组合词词频阈值
    private static double LEFTE_THRESHOLD = 10;// 组合词左信息熵阈值
    private static double RIGHTE_THRESHOLD = 10;// 组合词右信息熵阈值
    private static double MI_THRESHOLD = 10;// 组合词互信息阈值
    private static double TFIDF_THRESHOLD = 10;// 组合词TF-IDF阈值


    public static void main(String[] args) throws Exception{
        //String sqlSelectSentenceToken = "SELECT sentenceToken FROM t_sentence WHERE id > ? limit ?";
        String sqlSelectSentenceToken = "SELECT sentenceToken, articleId FROM t_sentence ";
        String sqlGetArticleCount = "SELECT COUNT(DISTINCT articleId)  AS articleCount FROM `t_sentence`;";
        int index = 0;

        List<Map<String, Object>> sentenceTokenList = new ArrayList<Map<String, Object>>();
        //sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken, index, LIMIT_SIZE);
        sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken);

        // 获取句子包含的文章数量
        List<Map<String, Object>> articleCountList = DatabaseHelper.executeQuery(sqlGetArticleCount);
        int articleCount = 0;
        if (articleCountList.size() == 1) {
            Map<String, Object> articleCountMap = articleCountList.get(0);
            articleCount = Integer.parseInt(String.valueOf(articleCountMap.get("articleCount")));
        }

        if (CollectionUtil.isNotEmpty(sentenceTokenList)) {
            Map<String, Integer> combineWordMap = new TreeMap<String, Integer>();// 保存ngrams组合词的及其词频
            Map<String, Set<Integer>> combineWordArticleMap = new TreeMap<String, Set<Integer>>();// 保存组合词及其所在的文章的id
            Map<String, Map<Integer, Integer>> combineWordArticleOccurTimesMap = new TreeMap<String, Map<Integer, Integer>>();// 保存组合词及其在所在的文章出现的次数
            Map<Integer, Integer> articleWordCount = new TreeMap<Integer, Integer>();// 保存文章id及其总词数
            Map<String, Map<String, Integer>> leftWordMapContainer = new TreeMap<String, Map<String, Integer>>();// 保存左词
            Map<String, Map<String, Integer>> rightWordMapContainer = new TreeMap<String, Map<String, Integer>>();// 保持右词

            String regexMI = "(<[^>]+?>([^<>]+?)</[^>]+?>){" + NGRAMS + "}";
            String regexRight = "(<[^>]+?>([^<>]+?)</[^>]+?>){" + (NGRAMS + 1) + "}";
            Pattern patternMI = Pattern.compile(regexMI);
            Pattern patternRight = Pattern.compile(regexRight);

            int combineWordCountSum = 0;// ngram分词后得到的词语总数
            int combineWordLeftRightCountSum = 0;// n+1gram分词后得到的词语总数

            for (Map<String, Object> sentenceTokenMap : sentenceTokenList) {
                String sentenceToken = String.valueOf(sentenceTokenMap.get("sentenceToken"));
                int articleId = Integer.parseInt(String.valueOf(sentenceTokenMap.get("articleId")));

                int regexIndex = 0;
                String sentence = sentenceToken;
                String lastProcessSentence;// 上一次处理的句子
                System.out.println("正在处理句子" + sentenceToken);
                while (sentence.indexOf("><") != -1) {
                    lastProcessSentence = sentence;
                    sentence = sentence.substring(regexIndex);
                    Matcher matcher = patternMI.matcher(sentence);
                    String wordMatch = "";// 匹配到的词语
                    while (matcher.find()) {
                        wordMatch = matcher.group(0);
                        if (wordMatch.indexOf("<w") != -1 ) {// 过滤标点符号
                            break;
                        }

                        // 计算当前文章总词数
                        if (articleWordCount.containsKey(articleId)) {
                            articleWordCount.put(articleId, articleWordCount.get(articleId) + 1);
                        } else {
                            articleWordCount.put(articleId, 1);
                        }
                        // 计算所有文档总词数
                        combineWordCountSum += 1;

                        // 过滤停用词
                        if (wordMatch.indexOf("<u") != -1
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

                        // 记录组合词词频
                        WordUtils.calculateWordCount(combineWordMap, wordMatch);

                        // 记录组合词出现的文章id,用来计算df
                        Set<Integer> articleSet = new HashSet<Integer>();
                        if (combineWordArticleMap.containsKey(wordMatch)) {
                            articleSet = combineWordArticleMap.get(wordMatch);
                            articleSet.add(articleId);
                        } else {
                            articleSet.add(articleId);
                        }
                        combineWordArticleMap.put(wordMatch, articleSet);

                        // 保存组合词及其在所在的文章出现的次数
                        Map<Integer, Integer> combineWordArticleOccurTimes = new HashMap<Integer, Integer>();
                        if (combineWordArticleOccurTimesMap.containsKey(wordMatch)) {
                            combineWordArticleOccurTimes = combineWordArticleOccurTimesMap.get(wordMatch);
                            // 是否存在词语在当前文章出现的次数
                            if (combineWordArticleOccurTimes.containsKey(articleId)) {
                                int occurTime = combineWordArticleOccurTimes.get(articleId);
                                combineWordArticleOccurTimes.put(articleId, occurTime + 1);
                            } else {
                                combineWordArticleOccurTimes.put(articleId, 1);
                            }
                        } else {
                            combineWordArticleOccurTimes.put(articleId, 1);
                        }
                        combineWordArticleOccurTimesMap.put(wordMatch, combineWordArticleOccurTimes);

                        System.out.println(wordMatch);
                        break;// 只获取匹配到的第一个
                    }

                    String leftWord = "";
                    String rightWord = "";
                    if (StringUtil.isNotEmpty(wordMatch)) {
                        if (regexIndex != 0) {
                            leftWord = lastProcessSentence.substring(0, regexIndex + wordMatch.length());
                            if (StringUtil.isNotEmpty(leftWord) && leftWord.indexOf("<w") == -1) {
                                Map<String, Integer> leftWordMap = new HashMap<String, Integer>();
                                if (leftWordMapContainer.containsKey(wordMatch)) {
                                    leftWordMap = leftWordMapContainer.get(wordMatch);
                                    WordUtils.calculateWordCount(leftWordMap, leftWord);
                                } else {
                                    leftWordMap.put(leftWord, 1);
                                }
                                leftWordMapContainer.put(wordMatch, leftWordMap);
                                System.out.println("left:" + leftWord);
                            }
                        }

                        Matcher matcherRight = patternRight.matcher(sentence);
                        while (matcherRight.find()) {
                            if (matcherRight.group(0).indexOf("<w") == -1) {
                                rightWord = matcherRight.group(0);
                            }
                            break;
                        }
                        if (StringUtil.isNotEmpty(rightWord) && rightWord.indexOf("<w") == -1) {
                            combineWordLeftRightCountSum += 1;
                            Map<String, Integer> rightMap = new HashMap<String, Integer>();
                            if (rightWordMapContainer.containsKey(wordMatch)) {
                                rightMap = rightWordMapContainer.get(wordMatch);
                                WordUtils.calculateWordCount(rightMap, rightWord);
                            } else {
                                rightMap.put(rightWord, 1);
                            }
                            rightWordMapContainer.put(wordMatch, rightMap);
                            System.out.println("right:" + rightWord);
                        }
                    }
                    System.out.println("==========");
                    regexIndex = sentence.indexOf("><") + 1;
                }
            }

            // 将分句后的词语及其词频实例化成一个Map
            Map<String, String> wordCountMap = MapUtils.getMapFromFile(DATA_SENTENCE_WORD_TXT, "\\s+");
            int wordCountSum = 0;// 分词后得到的单个词语总数
            // 计算总词数
            for (String wordCount : wordCountMap.values()) {
                wordCountSum += Integer.parseInt(wordCount);
            }

            Map<String, Double> miMap = new TreeMap<String, Double>();// 存储计算的信息的Map
            for (Map.Entry<String, Integer> combineEntry : combineWordMap.entrySet()) {
                String combineWord = combineEntry.getKey();
                int combineWordFreq = combineEntry.getValue();
                double pngram = combineWordFreq * 1.0d / combineWordCountSum;
                StringBuilder builder = new StringBuilder();
                //builder.append(combineWord).append(" P(ngram)=").append(pngram);
                builder.append(combineWord).append(" ").append(combineWordFreq);
                builder.append(" ").append(pngram);

                List<String> combineWordsList = WordUtils.getCombineWordsList(combineWord);
                int wordFreqSum = 0;// 单个词语词频总和
                double pwords = 1.0d;// 单个单词概率累乘
                for (String word : combineWordsList) {
                    // 计算词语出现的概率
                    int wordFreq = Integer.parseInt(wordCountMap.get(word));// tf:词频
                    double pword = wordFreq * 1.0d / wordCountSum;
                    //builder.append(" P(").append(word).append(")=").append(pword);
                    pwords *= pword;
                }

                // 计算tf-idf, 公式：tf * log(N/df)
                Set<Integer> articleSet = combineWordArticleMap.get(combineWord);
                int combineWordDF = articleSet.size();
                double tf = 0.0d;
                for (Map.Entry<Integer, Integer> articleOccurTimes : combineWordArticleOccurTimesMap.get(combineWord).entrySet()) {
                    int articleOccurTime = articleOccurTimes.getValue();
                    int articleId = articleOccurTimes.getKey();
                    int articleWordCountSum = articleWordCount.get(articleId);
                    tf += articleOccurTime * 1.0d / articleWordCountSum;
                }
                double tfIdf = tf * MathUtils.log(articleCount * 1.0d / combineWordDF, 2.0d);
                //builder.append(" tiidf:").append(tfIdf);
                builder.append(" ").append(tfIdf);

                // 计算互信息， 公式：f(mn)/(f(m)+f(n)-f(mn))
                //float mi = combineWordFreq * 1.0f / (wordFreqSum* 1.0f - combineWordFreq) ;

                // 计算互信息， 公式：P(x,y)*log(P(x,y)/(P(x)*P(y)))
                double mi = pngram * MathUtils.log(pngram / pwords, 2.0d);

                // 计算左右信息熵 公式：-P(aW|W)*log(P(aW|W))
                int leftWordCount = 0;// 以组合词为左边的词的数量
                int rightWordCount = 0;// 以组合词为右边的词的数量
                double leftE = 0.0d;// 左信息熵
                double rightE = 0.0d;// 右信息熵

                Map<String, Integer> leftWordMap = leftWordMapContainer.get(combineWord);
                Map<String, Integer> rightWordMap = rightWordMapContainer.get(combineWord);

                // 计算左信息熵
                if (CollectionUtil.isNotEmpty(leftWordMap)) {
                    for (Map.Entry<String, Integer> leftWordEntry : leftWordMap.entrySet()) {
                        int leftWordEntryValue = leftWordEntry.getValue();
                        leftWordCount += leftWordEntryValue;
                        double paW = leftWordEntryValue * 1.0d / combineWordLeftRightCountSum;
                        //leftE += (-paW * MathUtils.log(1 / paW, 2.0d));
                        leftE += (-paW * MathUtils.log(paW, 2.0d));
                    }
                }

                // 计算右信息熵
                if (CollectionUtil.isNotEmpty(rightWordMap)) {
                    for (Map.Entry<String, Integer> rightWordEntry : rightWordMap.entrySet()) {
                        int rightWordEntryValue = rightWordEntry.getValue();
                        rightWordCount += rightWordEntryValue;
                        double pWb = rightWordEntryValue * 1.0d / combineWordLeftRightCountSum;
                        rightE += (-pWb * MathUtils.log(pWb, 2.0d));
                    }
                }

                //builder.append(" L:").append(leftWordCount).append(" LE:").append(leftE);
                builder.append(" ").append(leftWordCount).append(" ").append(leftE);
                //builder.append(" R:").append(rightWordCount).append(" RE:").append(rightE);
                builder.append(" ").append(rightWordCount).append(" ").append(rightE);

                System.out.println("处理完成==" + combineWord + "已处理：" + miMap.size());
                miMap.put(builder.toString(), mi);
            }

            // 将Map按照词频降序排序
            List<String> combineWordList = MapUtils.sortIntegerMap(combineWordMap);
            // 将Map按照互信息降序排序
            List<String> miList = MapUtils.sortDoubleMap(miMap);

            LOGGER.info("正在导出规则匹配词");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateStr = simpleDateFormat.format(new Date());
            FileUtils.writeLines(new File("./data/miextraction-" + NGRAMS + "grams-" + dateStr + ".txt"), combineWordList);
            FileUtils.writeLines(new File("./data/midata-" + NGRAMS + "grams-" + dateStr + ".txt"), miList);
            LOGGER.info("导出规则匹配词完成");
        }
    }

}
