package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatResponse;
import com.chatbot.service.NlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final NlpService nlpService;

    @Autowired
    public ChatController(NlpService nlpService) {
        this.nlpService = nlpService;
    }

    @MessageMapping("/message")
    @SendTo("/topic/replies")
    public ChatResponse handleMessage(ChatMessage message) {
        String botReply = nlpService.processAndRespond(message.getContent());
        return new ChatResponse(botReply, "AI Assistant");
    }
}
