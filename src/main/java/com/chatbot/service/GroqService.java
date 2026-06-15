package com.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * GroqService acts as the intelligent fallback for all questions that the
 * local OpenNLP classifier cannot confidently answer.
 * Uses Jackson ObjectMapper to build JSON safely — avoids any escaping issues.
 */
@Service
public class GroqService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";
    private static final String SYSTEM_PROMPT =
            "You are a helpful AI Assistant embedded in a chat widget. " +
            "Answer questions concisely and in a friendly, conversational tone. " +
            "Do not use markdown, bullet points, or headers — respond in plain sentences only.";

    @Value("${groq.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String ask(String userMessage) {
        try {
            String requestBody = buildRequestBody(userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Groq API status: " + response.statusCode());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                System.err.println("Groq API error " + response.statusCode() + ": " + response.body());
                return "I'm having trouble connecting to my knowledge base right now. Please try again.";
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error calling Groq API: " + e.getMessage());
            return "I encountered an error while fetching your answer. Please try again later.";
        }
    }

    /**
     * Builds the JSON request body using Jackson ObjectNode to guarantee
     * correct JSON structure regardless of special characters in the user input.
     */
    private String buildRequestBody(String userMessage) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", MODEL);

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Parses the Groq API OpenAI-compatible response and extracts the reply text.
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText("Sorry, I could not parse the response.");
        } catch (Exception e) {
            System.err.println("Failed to parse Groq response: " + e.getMessage());
            return "I received a response but could not read it. Please try again.";
        }
    }
}
