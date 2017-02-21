package org.leader.data.concept;

import org.apache.commons.io.FileUtils;
import org.leader.data.util.MapUtils;
import org.leader.data.util.MathUtils;
import org.leader.data.util.WordUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.CollectionUtil;
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
public class MIConceptExtraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(MIConceptExtraction.class);
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
            Map<String, Integer> combineWordAddOneMap = new TreeMap<String, Integer>();// 保存(n+1)grams组合词的及其词频
            String regexMI = "(<[^>]+?>([^<>]+?)</[^>]+?>){" + NGRAMS + "}";
            String regexNGRAMSADDONE = "(<[^>]+?>([^<>]+?)</[^>]+?>){" + (NGRAMS + 1) + "}";// 增加组合词范围，用作求左右信息熵
            Pattern patternMI = Pattern.compile(regexMI);
            Pattern patternNGRAMSADDONE = Pattern.compile(regexNGRAMSADDONE);

            int combineWordCountSum = 0;// ngram分词后得到的词语总数
            int combineWordAddOneCountSum = 0;// n+1gram分词后得到的词语总数


            for (Map<String, Object> sentenceTokenMap : sentenceTokenList) {
                String sentenceToken = String.valueOf(sentenceTokenMap.get("sentenceToken"));
                int regexIndex = 0;
                String sentence = sentenceToken;
                System.out.println("正在处理句子" + sentenceToken);
                while (sentence.indexOf("><") != -1) {
                    sentence = sentence.substring(regexIndex);
                    Matcher matcher = patternMI.matcher(sentence);
                    while (matcher.find()) {
                        String wordMatch = matcher.group(0);
                        if (wordMatch.indexOf("<w") != -1) {// 过滤标点符号
                            break;
                        }
                        WordUtils.calculateWordCount(combineWordMap, wordMatch);
                        combineWordCountSum += 1;// 每获得一个词就+1
                        break;// 只获取匹配到的第一个
                    }

                    // 获取n+1 grams词语，用作计算左右信息熵
                    Matcher matcherNGRAMSADDONE = patternNGRAMSADDONE.matcher(sentence);
                    while (matcherNGRAMSADDONE.find()) {
                        String wordMatch = matcherNGRAMSADDONE.group(0);
                        if (wordMatch.indexOf("<w") != -1) {// 过滤标点符号
                            break;
                        }
                        WordUtils.calculateWordCount(combineWordAddOneMap, wordMatch);
                        combineWordAddOneCountSum += 1;// 每获得一个词就+1
                        break;// 只获取匹配到的第一个
                    }
                    regexIndex = sentence.indexOf("><") + 1;
                }
            }

            // 将Map按照词频降序排序
            List<String> combineWordList = MapUtils.sortIntegerMap(combineWordMap);

            // 计算左右信息熵方法二：将N+1Grams组合词Map的key转换成String，使用正则去匹配
            StringBuilder combineWordAddOneBuilder = new StringBuilder();
            for (String  combineWordAddOne: combineWordAddOneMap.keySet()) {
                combineWordAddOneBuilder.append(combineWordAddOne).append(" ");
            }
            String combineWordAddOneString = combineWordAddOneBuilder.toString();

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

                // 方法一：循环遍历N+1Grams的组合词Map
                //for (Map.Entry<String, Integer> combineWordAddOne : combineWordAddOneMap.entrySet()) {
                //    String word = combineWordAddOne.getKey();
                //    int count = combineWordAddOne.getValue();
                //    if (word.startsWith(combineWord)) {
                //        rightWordCount += 1;
                //        double pWb = count * 1.0d / combineWordAddOneCountSum;
                //        rightE += (-pWb * MathUtils.log(1 / pWb, 2.0d));
                //    } else if (word.endsWith(combineWord)) {
                //        leftWordCount += 1;
                //        double paW = count * 1.0d / combineWordAddOneCountSum;
                //        leftE += (-paW * MathUtils.log(1 / paW, 2.0d));
                //    }
                //}

                // 方法二：将N+1Grams组合词Map的key转换成String，使用正则去匹配
                String regexLeft = "<[^>]+?>([^<>]+?)</[^>]+?>" + combineWord;
                String regexRight = combineWord + "<[^>]+?>([^<>]+?)</[^>]+?>" ;
                Pattern patternLeft = Pattern.compile(regexLeft);
                Pattern patternRight = Pattern.compile(regexRight);
                Matcher matcherLeft = patternLeft.matcher(combineWordAddOneString);
                Matcher matcherRight = patternRight.matcher(combineWordAddOneString);
                // 计算左信息熵
                while (matcherLeft.find()) {
                    String wordLeft = matcherLeft.group(0);
                    int count = combineWordAddOneMap.get(wordLeft);
                    leftWordCount += 1;
                    double paW = count * 1.0d / combineWordAddOneCountSum;
                    leftE += (-paW * MathUtils.log(1 / paW, 2.0d));
                }
                // 计算右信息熵
                while (matcherRight.find()) {
                    String wordRight = matcherRight.group(0);
                    int count = combineWordAddOneMap.get(wordRight);
                    rightWordCount += 1;
                    double pWb = count * 1.0d / combineWordAddOneCountSum;
                    rightE += (-pWb * MathUtils.log(1 / pWb, 2.0d));
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
