package org.leader.data.sentence;

/**
 * 分句测试
 *
 * @author ldh
 * @since 2017-02-10 22:58
 */
public class SentetnceTest {
    public static void main(String[] args) {
        String regex = "&nbsp;";
        String sentence = "BI中文站 11月17日报道 &nbsp;&nbsp;没有人知道美国当选总统唐纳德•特朗普(Donald Trump)在履职后会采取什么样的经济政策，但是有一点他已经反复强调过，要对来自中国的商品课以重税。";
        sentence = sentence.replace(regex, "");
        System.out.println(sentence);
        String sentenceFilter = sentence.replace(regex, "");
        System.out.println(sentenceFilter);
    }
}
