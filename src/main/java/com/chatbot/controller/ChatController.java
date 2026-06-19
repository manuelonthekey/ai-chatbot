package com.chatbot.controller;

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

    @Autowired
    public ChatController(NlpService nlpService, SimpMessagingTemplate messagingTemplate) {
        this.nlpService = nlpService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/message")
    public void handleMessage(
            ChatMessage message,
            @Header(value = "sessionId", required = false) String sessionId,
            @Header("simpSessionId") String fallbackSessionId) {

        // Use the sessionId from the client header; fall back to STOMP session ID
        String routingId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : fallbackSessionId;

        String botReply = nlpService.processAndRespond(message.getContent());
        ChatResponse response = new ChatResponse(botReply, "AI Assistant");

        messagingTemplate.convertAndSend("/topic/replies-" + routingId, response);
    }
}
