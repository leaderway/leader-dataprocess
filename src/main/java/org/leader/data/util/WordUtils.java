package org.leader.data.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 词语工具类
 *
 * @author ldh
 * @since 2017-02-18 11:25
 */
public class WordUtils {

    /**
     *计算词频
     * @param wordCountMap
     * @param word
     */
    public static void calculateWordCount(Map<String, Integer> wordCountMap, String word) {
        if (wordCountMap.containsKey(word)) {
            wordCountMap.put(word, wordCountMap.get(word) + 1);
        } else {
            wordCountMap.put(word, 1);
        }
    }

    /**
     * 获取组合词列表
     * @param combineWord
     * @return
     */
    public static List<String> getCombineWordsList(String combineWord) {
        String regex = "<[^>]+?>[^<>]+?</[^>]+?>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(combineWord);
        List<String> combineWordsList = new ArrayList<String>();
        while (matcher.find()) {
            combineWordsList.add(matcher.group(0));
        }
        return combineWordsList;
    }

    /**
     * 抽取出无词性标签的词语
     * @param tagWord
     * @return
     */
    public static String getWordWithOutTags(String tagWord) {
        StringBuilder stringBuilder = new StringBuilder();
        String regexWord = "<[^>]+?>([^<>]+?)</[^>]+?>";
        Pattern patternWord = Pattern.compile(regexWord);
        Matcher matcherWord = patternWord.matcher(tagWord);
        while (matcherWord.find()) {
            for (int i = 1; i <= matcherWord.groupCount(); i++) {
                stringBuilder.append(matcherWord.group(i));
            }
        }
        return stringBuilder.toString();
    }
}
