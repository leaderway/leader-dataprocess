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
    private static final int NGRAMS = 2;//
    private static final String DATA_SENTENCE_WORD_TXT = "./data/sentenceWord.txt";//  记录词频的文件

    public static void main(String[] args) throws Exception{
        //String sqlSelectSentenceToken = "SELECT sentenceToken FROM t_sentence WHERE id > ? limit ?";
        String sqlSelectSentenceToken = "SELECT sentenceToken FROM t_sentence ";
        int index = 0;

        List<Map<String, Object>> sentenceTokenList = new ArrayList<Map<String, Object>>();
        //sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken, index, LIMIT_SIZE);
        sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken);

        if (CollectionUtil.isNotEmpty(sentenceTokenList)) {
            Map<String, Integer> combineWordMap = new TreeMap<String, Integer>();// 保存ngrams组合词的及其词频
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
                        if (wordMatch.indexOf("<w") != -1) {// 过滤标点符号
                            break;
                        }
                        WordUtils.calculateWordCount(combineWordMap, wordMatch);
                        combineWordCountSum += 1;// 每获得一个词就+1
                        System.out.println(wordMatch);
                        break;// 只获取匹配到的第一个
                    }
                    String leftWord = "";
                    String rightWord = "";
                    if (StringUtil.isNotEmpty(wordMatch)) {
                        if (regexIndex != 0) {
                            leftWord = lastProcessSentence.substring(0, regexIndex + wordMatch.length());
                            if (StringUtil.isNotEmpty(leftWord) && leftWord.indexOf("<w") == -1) {
                                //combineWordLeftRightCountSum += 1;
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

            // 将Map按照词频降序排序
            List<String> combineWordList = MapUtils.sortIntegerMap(combineWordMap);


            // 计算词语互信息
            // 将分句后的词语及其词频实例化成一个Map
            Map<String, String> wordCountMap = MapUtils.getMapFromFile(DATA_SENTENCE_WORD_TXT, "\\s+");
            int wordCountSum = 0;// 分词后得到的单个词语总数
            // 计算总词数
            for (String wordCount : wordCountMap.values()) {
                wordCountSum += Integer.parseInt(wordCount);
            }

            Map<String, Double> miMap = new TreeMap<String, Double>();
            List<String> miList = new ArrayList<String>();
            for (Map.Entry<String, Integer> combineEntry : combineWordMap.entrySet()) {
                String combineWord = combineEntry.getKey();
                int combineWordFreq = combineEntry.getValue();
                double pngram = combineWordFreq * 1.0d / combineWordCountSum;
                StringBuilder builder = new StringBuilder();
                builder.append(combineWord).append(" P(ngram)=").append(pngram);

                List<String> combineWordsList = WordUtils.getCombineWordsList(combineWord);
                int wordFreqSum = 0;// 单个词语词频总和
                double pwords = 1.0d;// 单个单词概率累乘
                for (String word : combineWordsList) {
                    int wordFreq = Integer.parseInt(wordCountMap.get(word));
                    double pword = wordFreq * 1.0d / wordCountSum;
                    builder.append(" P(").append(word).append(")=").append(pword);
                    pwords *= pword;
                }

                // 公式为f(mn)/(f(m)+f(n)-f(mn))
                //float mi = combineWordFreq * 1.0f / (wordFreqSum* 1.0f - combineWordFreq) ;

                // 公式为P(x,y)*log(P(x,y)/(P(x)*P(y)))
                double mi = pngram * MathUtils.log(pngram / pwords, 2.0d);

                // 计算左右信息熵 公式：-P(aW|W)*log(1/P(aW|W))
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
                        leftE += (-paW * MathUtils.log(1 / paW, 2.0d));
                    }
                }

                // 计算右信息熵
                if (CollectionUtil.isNotEmpty(rightWordMap)) {
                    for (Map.Entry<String, Integer> rightWordEntry : rightWordMap.entrySet()) {
                        int rightWordEntryValue = rightWordEntry.getValue();
                        rightWordCount += rightWordEntryValue;
                        double pWb = rightWordEntryValue * 1.0d / combineWordLeftRightCountSum;
                        rightE += (-pWb * MathUtils.log(1 / pWb, 2.0d));
                    }
                }

                builder.append(" L:").append(leftWordCount).append(" LE:").append(leftE);
                builder.append(" R:").append(rightWordCount).append(" RE:").append(rightE);

                System.out.println("处理完成==" + combineWord + "已处理：" + miMap.size());
                miMap.put(builder.toString(), mi);
            }
            miList = MapUtils.sortDoubleMap(miMap);

            LOGGER.info("正在导出规则匹配词");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateStr = simpleDateFormat.format(new Date());
            FileUtils.writeLines(new File("./data/miextraction-" + NGRAMS + "grams-" + dateStr + ".txt"), combineWordList);
            FileUtils.writeLines(new File("./data/midata-" + NGRAMS + "grams-" + dateStr + ".txt"), miList);
            LOGGER.info("导出规则匹配词完成");
        }
    }

}
