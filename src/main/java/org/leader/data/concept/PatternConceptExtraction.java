package org.leader.data.concept;

import org.apache.commons.io.FileUtils;
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
 * 基于规则的概念抽取
 *
 * @author ldh
 * @since 2017-02-17 0:01
 */
public class PatternConceptExtraction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatternConceptExtraction.class);
    private static final int LIMIT_SIZE = 500;// 每次从数据库取出的数据大小

    public static void main(String[] args) throws Exception{
        //String sqlSelectSentenceToken = "SELECT sentenceToken FROM t_sentence WHERE id > ? limit ?";
        String sqlSelectSentenceToken = "SELECT sentenceToken FROM t_sentence ";
        String sqlSelectPattern = "SELECT * FROM t_pattern WHERE status = 1";
        int index = 0;

        List<Map<String, Object>> sentenceTokenList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> patternList = new ArrayList<Map<String, Object>>();
        //sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken, index, LIMIT_SIZE);
        sentenceTokenList = DatabaseHelper.executeQuery(sqlSelectSentenceToken);
        patternList = DatabaseHelper.executeQuery(sqlSelectPattern);

        Map<Integer, Pattern> patternMap = new HashMap<Integer, Pattern>();
        for (Map<String, Object> patternMapEntry : patternList) {
            int patternId = Integer.parseInt(String.valueOf(patternMapEntry.get("id")));
            String patternStr = String.valueOf(patternMapEntry.get("pattern"));
            Pattern pattern = Pattern.compile(patternStr);
            patternMap.put(patternId, pattern);
        }

        if (CollectionUtil.isNotEmpty(sentenceTokenList)) {
            Map<String, Integer> combineWordMap = new HashMap<String, Integer>();// 保存组合词的及其词频

            for (Map<String, Object> sentenceTokenMap : sentenceTokenList) {
                String sentenceToken = String.valueOf(sentenceTokenMap.get("sentenceToken"));

                // 调用正则进行规则匹配
                for (Map.Entry<Integer, Pattern> patternEntry : patternMap.entrySet()) {
                    int patternId = patternEntry.getKey();
                    Pattern pattern = patternEntry.getValue();
                    Matcher matcher = pattern.matcher(sentenceToken);
                    while (matcher.find()) {
                        String combineWord = matcher.group(0);
                        if (combineWordMap.containsKey(combineWord)) {
                            int count = combineWordMap.get(combineWord);
                            combineWordMap.put(combineWord, count + 1);// 词频+1
                        } else {
                            combineWordMap.put(combineWord, 1);
                        }
                        System.out.println(combineWord+ "--" + patternId);
                    }
                }
            }

            List<String> combineWordList = new ArrayList<String>();
            for (Map.Entry<String, Integer> combineWordMapEntry : combineWordMap.entrySet()) {
                String combineWordStr = combineWordMapEntry.getKey() + "  " + combineWordMapEntry.getValue();
                combineWordList.add(combineWordStr);
            }

            LOGGER.info("正在导出规则匹配词");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String dateStr = simpleDateFormat.format(new Date());
            FileUtils.writeLines(new File("./data/patternextraction" +  dateStr + ".txt"), combineWordList);
            LOGGER.info("导出规则匹配词完成");
        }
    }

}
