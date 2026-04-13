package model; // 或者放在你的实体类包下

public class CommentInfo {
    public String username;
    public int avatarResId; // 头像的图片资源ID
    public String content;
    public String date;

    public CommentInfo(String username, int avatarResId, String content, String date) {
        this.username = username;
        this.avatarResId = avatarResId;
        this.content = content;
        this.date = date;
    }
}