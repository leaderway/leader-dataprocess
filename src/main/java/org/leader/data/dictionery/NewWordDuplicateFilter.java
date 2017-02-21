package org.leader.data.dictionery;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.leader.framework.util.StringUtil;

import java.io.File;
import java.util.*;

/**
 * 新词重复过滤
 *
 * @author ldh
 * @since 2017-02-15 23:07
 */
public class NewWordDuplicateFilter {
    private static final String newWordPath = "G:\\文件\\研究生\\学习\\开题\\实验\\userdic.txt";
    private static final String newWordNoduplicatePath = "G:\\文件\\研究生\\学习\\开题\\实验\\userdicduplicate.txt";

    public static void main(String[] args) throws Exception{
        LineIterator iterator = FileUtils.lineIterator(new File(newWordPath));
        Map<String, String> newWordMap = new HashMap<String, String>();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (StringUtil.isNotEmpty(line)) {
                String[] lineArray = line.split("\\s+");
                String newWord = lineArray[0];
                if (!newWordMap.containsKey(newWord)) {
                    newWordMap.put(newWord, line);
                }
            }
        }

        Collection<String> newWordColletction = newWordMap.values();
        FileUtils.writeLines(new File(newWordNoduplicatePath), newWordColletction);
        System.out.println("新词过滤成功");
    }
}
