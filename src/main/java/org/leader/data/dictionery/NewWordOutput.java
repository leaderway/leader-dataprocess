package org.leader.data.dictionery;

import org.apache.commons.io.FileUtils;
import org.leader.framework.helper.DatabaseHelper;
import org.leader.framework.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 新词导出到txt文件中
 *
 * @author ldh
 * @since 2017-02-13 21:33
 */
public class NewWordOutput {

    private static final String newWordPath = "G:\\文件\\研究生\\学习\\开题\\实验\\userdic.txt";

    public static void main(String[] args) throws Exception{
        String sql = "SELECT keyword,freq FROM `t_keywordfreq_core_ansj` where status != 0;";
        List<Map<String, Object>> newWordMapList = DatabaseHelper.executeQuery(sql);
        List<String> newWordList = new ArrayList<String>();
        for (Map<String, Object> newWordMap : newWordMapList) {
            String newWord = String.valueOf(newWordMap.get("keyword")).replace("/", " " );
            String freq = String.valueOf(newWordMap.get("freq"));
            if (StringUtil.isNotEmpty(newWord)) {
                newWordList.add(newWord + " " + freq);
            }
        }

        FileUtils.writeLines(new File(newWordPath), newWordList);
        System.out.println("文件写入结束");
    }
}
