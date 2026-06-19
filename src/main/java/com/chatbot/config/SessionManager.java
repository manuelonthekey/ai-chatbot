package com.chatbot.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final ConcurrentHashMap<String, String> sessionChatIds = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String chatId = accessor.getFirstNativeHeader("chatId");
        if (chatId != null && accessor.getSessionId() != null) {
            sessionChatIds.put(accessor.getSessionId(), chatId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        if (event.getSessionId() != null) {
            sessionChatIds.remove(event.getSessionId());
        }
    }

    public String getChatId(String sessionId) {
        return sessionId != null ? sessionChatIds.get(sessionId) : null;
    }
}
