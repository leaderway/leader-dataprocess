package org.leader.data.sentence;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.commons.io.FileUtils;
import org.leader.data.enmu.SentencePostion;
import org.leader.data.util.MapUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.CollectionUtil;
import org.leader.lucene.util.UserLibraryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**文章分句
 *
 *
 * @author ldh
 * @since 2017-02-10 21:42
 */
public class SentenceDivide {

    private static final String SPLIT_REGX = "[。；？！?!;?!]";
    private static final Logger LOGGER = LoggerFactory.getLogger(SentenceDivide.class);
    private static final String WORD_FILE_PATH = "./data/sentenceWord.txt";

    public static void main(String[] args) throws Exception{
        // 初始化用户自定义词典
        UserLibraryUtils.insertWords("./library/default.dic");
        String sql = "SELECT id, title, content FROM t_article WHERE id > ? limit 500";
        String sqlInsert = "INSERT INTO t_sentence (sentence, sentenceToken, articleId, position) VALUES (?, ?, ?, ?)";

        int index = 0;
        List<Map<String, Object>> articleList = DatabaseHelper.executeQuery(sql, index);
        Map<String, Integer> wordMap = new TreeMap<String, Integer>();// 存放词语Map

        if (CollectionUtil.isNotEmpty(articleList)) {
            for (Map<String, Object> articleMap : articleList) {
                int articleId = Integer.parseInt(articleMap.get("id").toString());
                String title = articleMap.get("title").toString();
                title = handleString(title);
                String content = articleMap.get("content").toString();
                content = handleString(content);
                String[] titleArray = title.split(SPLIT_REGX);
                String[] contentArray = content.split(SPLIT_REGX);

                LOGGER.info("目前处理文章编号："+articleId);
                for (String titleSentetnce : titleArray) {
                    List<Term> termList = analysisSentence(titleSentetnce);
                    String sentenceToken = handleAnalysisResult(termList, wordMap);
                    DatabaseHelper.executeUpdate(sqlInsert, titleSentetnce, sentenceToken, articleId, SentencePostion.TITLE.getPosition());
                    //LOGGER.info(handleAnalysisResult(titleSentetnce));
                }
                for (String contentSentence : contentArray) {
                    List<Term> termList = analysisSentence(contentSentence);
                    String sentenceToken = handleAnalysisResult(termList, wordMap);
                    DatabaseHelper.executeUpdate(sqlInsert, contentSentence, sentenceToken, articleId, SentencePostion.CONNTENT.getPosition());
                    //LOGGER.info(handleAnalysisResult(contentSentence));
                }
            }
        }

        List<String> wordList = MapUtils.sortIntegerMap(wordMap);
        LOGGER.info("正在导出词表");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = simpleDateFormat.format(new Date());
        File wordFile = new File(WORD_FILE_PATH);
        if (wordFile.exists()) {
            wordFile.delete();
        }
        FileUtils.writeLines(new File(WORD_FILE_PATH), wordList);
        LOGGER.info("导出词表完成");
        LOGGER.info("分句完毕");
    }

    /**
     * 处理句子中的特殊字符
     * @param sentence
     * @return
     */
    private static String handleString(String sentence) {
        return sentence.replaceAll("　", "").replaceAll("\\(中国电子商务研究中心讯\\)", "").replaceAll("&nbsp;", "").replaceAll("[。；？！?!;?!][”》]", "”").replaceAll("\\s+", "").trim();
    }

    /**
     * 对分词结果进行处理
     * @param termList
     * @return
     */
    private static String handleAnalysisResult(List<Term> termList, Map<String, Integer> wordMap) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Term term : termList) {
            String natureStr = term.getNatureStr();
            if ("null".equals(natureStr)) {
                natureStr = "w";
            }
            String name = term.getName();
            String sentenctResult = "<" + natureStr + ">" + name + "</" + natureStr + ">";
            if (natureStr.indexOf("w") != 0) {
                if (wordMap.containsKey(sentenctResult)) {
                    wordMap.put(sentenctResult, wordMap.get(sentenctResult) + 1);
                } else {
                    wordMap.put(sentenctResult, 1);
                }
            }
            stringBuilder.append(sentenctResult);

        }
        return stringBuilder.toString();
    }

    /**
     * 句子分词
     * @param sentence
     * @return
     */
    private static List<Term> analysisSentence(String sentence) {
        Result result = ToAnalysis.parse(sentence);
        //Result result = NlpAnalysis.parse(sentence);
        return result.getTerms();
    }
}
