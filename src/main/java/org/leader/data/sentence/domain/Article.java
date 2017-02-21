package org.leader.data.sentence.domain;

import java.util.List;

/**
 * 文章类
 *
 * @author ldh
 * @since 2016-10-13 11:22
 */
public class Article {
    /**
     * 主键
     */
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 发表日期
     */
    private String date;
    /**
     * 文章所在地址
     */
    private String url;
    /**
     * 文章内容
     */
    private String content;
    /**
     * 文章关键词
     */
    private List<String> keywords;
    /**
     * 所属专题
     */
    private String topic;

    public Article() {
    }

    public Article(String title, String date, String url, String content, List<String> keywords, String topic) {
        this.title = title;
        this.date = date;
        this.url = url;
        this.content = content;
        this.keywords = keywords;
        this.topic = topic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", url='" + url + '\'' +
                ", content='" + content + '\'' +
                ", keywords=" + keywords +
                ", topic='" + topic + '\'' +
                '}';
    }
}
