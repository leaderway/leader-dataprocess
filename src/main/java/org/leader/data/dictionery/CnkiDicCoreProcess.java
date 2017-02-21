package org.leader.data.dictionery;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从Cnki获取的文献数据中提取词典
 *
 * @author ldh
 * @since 2017-02-08 15:17
 */
public class CnkiDicCoreProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(CnkiDicCoreProcess.class);

    private static final String DICPATH = "G:\\文件\\研究生\\学习\\开题\\cnki核心";
    private static final String KEYWORD_PREFIX = "Keyword";// 关键词字段前缀

    public static void main(String[] args) {
        try {
            Map<String, Integer> dicMap = new HashMap<String, Integer>(); // 存放关键词及其词频的Map

            // 从文件夹中读取关键词字段
            File dicDirectory = FileUtils.getFile(DICPATH);
            File[] files = dicDirectory.listFiles();
            for (File file : files) {
                LineIterator iterator = FileUtils.lineIterator(file);
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    if (line.startsWith(KEYWORD_PREFIX)) {
                        // 分割关键词
                        String[] keyWords = line.substring(line.indexOf(":") + 1, line.length()).trim().split("[、;,/\\s+]");
                        if (ArrayUtil.isNotEmpty(keyWords)) {
                            for (String keyWord : keyWords) {
                                // 将关键词放入Map中
                                if (StringUtils.isNotBlank(keyWord) && !keyWord.matches("[a-zA-Z]+")) {
                                    if (dicMap.containsKey(keyWord)) {
                                        int freq = dicMap.get(keyWord) + 1;
                                        dicMap.put(keyWord, freq);
                                    } else {
                                        dicMap.put(keyWord, 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 将关键词及其词频写入数据库中
            LOGGER.info("开始将关键词写入数据库");
            String sql = "INSERT INTO `t_keywordfreq_core_ansj` (keyword, freq) VALUES (?, ?)";
            String sqlNewWord = "INSERT INTO `t_keywordfreq_core_ansj` (keyword, freq, status) VALUES (?, ?, ?)";
            for (Map.Entry<String, Integer> keyWordFreq : dicMap.entrySet()) {
                String keyWord = keyWordFreq.getKey();
                Result terms = NlpAnalysis.parse(keyWord);
                List<Term> termList = terms.getTerms();
                for (Term term : termList) {
                    System.out.println(term.getName());// 词语
                    System.out.println(term.getNatureStr());// 词性
                }

                keyWord = terms.toString();
                Integer freq = keyWordFreq.getValue();
                //String sql = "INSERT INTO t_keywordFreq (keyword, freq) VALUES ('"
                //        + keyWord + "', " + freq + ")";
                if (keyWord.indexOf("nw") != -1){
                    DatabaseHelper.executeUpdate(sqlNewWord, keyWord, freq, 1);
                } else {
                    DatabaseHelper.executeUpdate(sql, keyWord, freq);
                }
                LOGGER.info("将关键词写入数据库成功，keyword={}, freq={}", keyWord, freq);
            }
            LOGGER.info("关键词写入数据库结束");
        } catch (IOException e) {
            LOGGER.error("处理关键词失败", e);
        }
    }
}
