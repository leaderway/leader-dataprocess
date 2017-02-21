package org.leader.data.pattern;

import com.sun.org.apache.xpath.internal.SourceTree;

/**
 * 测试replace方法
 *
 * @author ldh
 * @since 2017-02-16 20:05
 */
public class TestReplace {

    public static void main(String[] args) {
        String text = "　　记者从峰会上获悉，济南市试点跨境    电子商务实施方案已经上报     省政府，目前已经获批并上报国家相关部门，预计一两个月内将试运行  ";
        String textTrim = text.replaceAll("\\s*", "").replace("　", "").trim();
        String textTrim2 = text.replace("\\s*", "").trim();
        System.out.println(textTrim);
        System.out.println(textTrim2);

        String source = "(来源：站长网）";
        System.out.println(source.replaceAll("\\(来源：\\S+\\)", ""));
    }
}
