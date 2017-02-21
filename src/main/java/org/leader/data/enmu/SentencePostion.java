package org.leader.data.enmu;

/**
 * 句子位置
 *
 * @author ldh
 * @since 2017-02-16 22:14
 */
public enum  SentencePostion {
    CONNTENT(0), TITLE(1);
    private int position;

    SentencePostion(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
