package org.leader.data.pattern;

import org.apache.commons.lang3.StringUtils;
import org.leader.data.util.WordUtils;
import org.leader.framework.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试正则表达式
 *
 * @author ldh
 * @since 2017-02-16 17:51
 */
public class TestRegex {
    public static void main(String[] args) {
        String text = "<nR>吃饭</nR><b>吃饭b</b><n>吃饭2</n><n>吃饭3</n><wb>吃饭b2</wb><a1>吃饭3</a1><b>吃饭b2</b>";

        String regex = "<[an]\\S*?>([^<>]+?)</[an]\\S*?><b>([^<>]+?)</b>";
        String regexMI = "(<[^>]+?>([^<>]+?)</[^>]+?>){2}";
        String regexCombineWord = "<[^>]+?>[^<>]+?</[^>]+?>";

        Pattern pattern = Pattern.compile(regex);
        Pattern patternMI = Pattern.compile(regexMI);
        Pattern patternCW = Pattern.compile(regexCombineWord);

        Matcher matcher = pattern.matcher(text);
        Matcher matcherMI = patternMI.matcher(text);
        Matcher matcherCW = patternCW.matcher(text);

        //while (matcher.find()) {
        //    System.out.println(matcher.group(0));
        //    System.out.println(matcher.group(1));
        //    System.out.println(matcher.group(2));
        //    System.out.println("===================");
        //}


        //int regexIndex = 0;
        //String sentence = text;
        //String lastSentence;
        //while (sentence.indexOf("><") != -1) {
        //    lastSentence = sentence;
        //    sentence = sentence.substring(regexIndex);
        //    System.out.println(sentence);
        //    System.out.println(lastSentence);
        //    regexIndex = sentence.indexOf("><") + 1;
        //    System.out.println(regexIndex);
        //}
        //while (matcherMI.find()) {
        //    System.out.println(matcherMI.group(0));
        //    System.out.println("===================");
        //}

        //while (matcherCW.find()) {
        //    System.out.println(matcherCW.group(0));
        //}

        //int regexIndex = 0;
        //String sentence = text;
        //String lastProcessSentence;// 上一次处理的句子
        //Map<String, Map<String, Integer>> leftWordMapContainer = new TreeMap<String, Map<String, Integer>>();// 保存左词
        //Map<String, Map<String, Integer>> rightWordMapContainer = new TreeMap<String, Map<String, Integer>>();// 保持右词
        //Map<String, Integer> leftWordMap = new HashMap<String, Integer>();
        //Map<String, Integer> rightWordMap = new HashMap<String, Integer>();

        //while (sentence.indexOf("><") != -1) {
        //    lastProcessSentence = sentence;
        //    sentence = lastProcessSentence.substring(regexIndex);
        //    Matcher matcher2 = patternMI.matcher(sentence);
        //    String wordMatch = "";// 匹配到的词语
        //    while (matcher2.find()) {
        //        wordMatch = matcher2.group(0);
        //        if (wordMatch.indexOf("<w") != -1) {// 过滤标点符号
        //            System.out.println("过滤w");
        //            break;
        //        }
        //        System.out.println(wordMatch);
        //        break;// 只获取匹配到的第一个
        //    }
        //    String leftWord;
        //    String rightWord;
        //    if (StringUtil.isNotEmpty(wordMatch)) {
        //        if (regexIndex != 0) {
        //            leftWord = lastProcessSentence.substring(0, regexIndex + wordMatch.length());
        //            if (StringUtil.isNotEmpty(leftWord) && leftWord.indexOf("<w") == -1) {
        //                Map<String, Integer> leftWordMap = new HashMap<String, Integer>();
        //                if (leftWordMapContainer.containsKey(wordMatch)) {
        //                    leftWordMap = leftWordMapContainer.get(wordMatch);
        //                    WordUtils.calculateWordCount(leftWordMap, leftWord);
        //                } else {
        //                    leftWordMap.put(leftWord, 1);
        //                }
        //                leftWordMapContainer.put(wordMatch, leftWordMap);
        //                System.out.println("left:" + leftWord);
        //            }
        //        }
        //
        //        rightWord = sentence.substring(0, sentence.indexOf("><", wordMatch.length()) + 1);
        //        if (StringUtil.isNotEmpty(rightWord) && rightWord.indexOf("<w") == -1) {
        //            Map<String, Integer> rightMap = new HashMap<String, Integer>();
        //            if (rightWordMapContainer.containsKey(wordMatch)) {
        //                rightMap = rightWordMapContainer.get(wordMatch);
        //                WordUtils.calculateWordCount(rightMap, rightWord);
        //            } else {
        //                rightMap.put(rightWord, 1);
        //            }
        //            rightWordMapContainer.put(wordMatch, rightMap);
        //            System.out.println("right:" + rightWord);
        //        }
        //    }
        //    System.out.println("==========");
        //    regexIndex = sentence.indexOf("><") + 1;
        //}

        String[] afterSplit = "sentence".split("\\|");
        for (String s : afterSplit) {
            System.out.println(s);
        }
        System.out.println(afterSplit.length);


    }
}
