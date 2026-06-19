package com.chatbot.controller;

import com.chatbot.config.SessionManager;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatResponse;
import com.chatbot.service.NlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final NlpService nlpService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionManager sessionManager;

    @Autowired
    public ChatController(NlpService nlpService, SimpMessagingTemplate messagingTemplate, SessionManager sessionManager) {
        this.nlpService = nlpService;
        this.messagingTemplate = messagingTemplate;
        this.sessionManager = sessionManager;
    }

    @MessageMapping("/message")
    public void handleMessage(ChatMessage message, @Header("simpSessionId") String sessionId) {
        String chatId = sessionManager.getChatId(sessionId);
        String botReply = nlpService.processAndRespond(message.getContent());
        ChatResponse response = new ChatResponse(botReply, "AI Assistant");
        
        if (chatId != null) {
            messagingTemplate.convertAndSend("/topic/replies-" + chatId, response);
        } else {
            messagingTemplate.convertAndSend("/topic/replies", response);
        }
    }
}
