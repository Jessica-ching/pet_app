package com.example.pet_app;

public class ChatMessage {
    private String content;
    private boolean isUser; // true: 使用者發送 (右邊), false: AI發送 (左邊)


    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }
    public String getContent() { return content; }
    public boolean isUser() { return isUser; }
}
