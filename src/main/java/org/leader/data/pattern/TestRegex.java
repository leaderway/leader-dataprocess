package org.leader.data.pattern;

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
        //while (sentence.indexOf("><") != -1) {
        //    sentence = sentence.substring(regexIndex);
        //    System.out.println(sentence);
        //    regexIndex = sentence.indexOf("><") + 1;
        //    System.out.println(regexIndex);
        //}
        //while (matcherMI.find()) {
        //    System.out.println(matcherMI.group(0));
        //    System.out.println("===================");
        //}

        while (matcherCW.find()) {
            System.out.println(matcherCW.group(0));
        }


    }
}
