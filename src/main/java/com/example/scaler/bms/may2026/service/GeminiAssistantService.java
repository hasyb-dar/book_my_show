package com.example.scaler.bms.may2026.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.example.scaler.bms.may2026.model.Booking;
import com.example.scaler.bms.may2026.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Same agentic booking assistant as AssistantService, but backed by
 * Google's Gemini API (free tier) instead of Claude.
 *
 * Uses plain REST calls against Gemini's documented generateContent
 * endpoint rather than the official SDK, since Google's Java SDK is
 * mid-migration to a new "Interactions API" as of 2026 and its exact
 * method surface is a moving target.
 */
@Service
@Slf4j
public class GeminiAssistantService {

    @Autowired
    private BookingServiceImpl bookingServiceImpl;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Value("${GEMINI_API_KEY:${gemini.api.key:}}")
    private String apiKey;

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant for a movie ticket booking system. "
                    + "Use the available tools to create bookings, look up tickets, and "
                    + "register or log in users, based on what the person asks for. "
                    + "Ask for any missing required information (showId, userId, seat "
                    + "numbers, email, password) before calling a tool, rather than "
                    + "guessing values. Keep replies short and confirm what happened "
                    + "after a tool call succeeds, or explain clearly if it failed.";

    public String handleMessage(String userMessage) {

        ArrayNode contents = objectMapper.createArrayNode();
        contents.add(userTextContent(userMessage));

        for (int turn = 0; turn < 5; turn++) {

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.set("contents", contents);
            requestBody.set("system_instruction", textPart(SYSTEM_PROMPT));
            requestBody.set("tools", buildToolsNode());

            JsonNode response = callGemini(requestBody);

            JsonNode candidate = response.path("candidates").path(0);
            JsonNode modelContent = candidate.path("content");

            // Echo the model's own turn back into the running conversation.
            contents.add(modelContent);

            JsonNode parts = modelContent.path("parts");
            List<JsonNode> functionCalls = new ArrayList<>();
            StringBuilder textReply = new StringBuilder();

            for (JsonNode part : parts) {
                if (part.has("text")) {
                    textReply.append(part.get("text").asText());
                }
                if (part.has("functionCall")) {
                    functionCalls.add(part.get("functionCall"));
                }
            }

            if (functionCalls.isEmpty()) {
                return textReply.toString();
            }

            ObjectNode functionResponseContent = objectMapper.createObjectNode();
            functionResponseContent.put("role", "user");
            ArrayNode responseParts = objectMapper.createArrayNode();

            for (JsonNode call : functionCalls) {
                String name = call.get("name").asText();
                JsonNode args = call.get("args");
                String result = executeTool(name, args);

                ObjectNode functionResponsePart = objectMapper.createObjectNode();
                ObjectNode functionResponse = objectMapper.createObjectNode();
                functionResponse.put("name", name);
                ObjectNode responseWrapper = objectMapper.createObjectNode();
                responseWrapper.put("result", result);
                functionResponse.set("response", responseWrapper);
                functionResponsePart.set("functionResponse", functionResponse);
                responseParts.add(functionResponsePart);
            }

            functionResponseContent.set("parts", responseParts);
            contents.add(functionResponseContent);
        }

        return "Sorry, I couldn't complete that request in a reasonable number of steps.";
    }

    private JsonNode callGemini(ObjectNode requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            String url = ENDPOINT + "?key=" + apiKey;

            String rawResponse = restTemplate.postForObject(url, entity, String.class);
            return objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    private ObjectNode userTextContent(String text) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = objectMapper.createArrayNode();
        parts.add(textPart(text).get("parts").get(0));
        content.set("parts", parts);
        return content;
    }

    private ObjectNode textPart(String text) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", text);
        parts.add(part);
        wrapper.set("parts", parts);
        return wrapper;
    }

    // ---- Tool execution: same four actions as the Claude-based assistant ----

    private String executeTool(String toolName, JsonNode args) {
        try {
            switch (toolName) {

                case "create_booking": {
                    Long showId = args.get("showId").asLong();
                    Long userId = args.get("userId").asLong();
                    List<String> seatNumbers = new ArrayList<>();
                    args.get("seatNumbers").forEach(n -> seatNumbers.add(n.asText()));

                    Booking booking = bookingServiceImpl.createBooking(seatNumbers, showId, userId);
                    return String.format(
                            "Booking created. id=%d, status=%s, totalAmount=%.2f",
                            booking.getId(), booking.getBookingStatus(), booking.getTotalAmount());
                }

                case "get_ticket": {
                    Long bookingId = args.get("bookingId").asLong();
                    Booking booking = bookingServiceImpl.getBookingById(bookingId);
                    return String.format(
                            "Ticket id=%d, status=%s, totalAmount=%.2f, bookedBy=%s",
                            booking.getId(), booking.getBookingStatus(),
                            booking.getTotalAmount(), booking.getCreatedBy().getEmail());
                }

                case "register_user": {
                    String email = args.get("email").asText();
                    String password = args.get("password").asText();
                    User user = userServiceImpl.registerUser(email, password);
                    return String.format("User registered. id=%d, email=%s", user.getId(), user.getEmail());
                }

                case "login_user": {
                    String email = args.get("email").asText();
                    String password = args.get("password").asText();
                    boolean success = userServiceImpl.login(email, password);
                    return success ? "Login successful." : "Login failed.";
                }

                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            log.warn("Tool execution failed: {}", toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    // ---- Tool schemas, in Gemini's functionDeclarations format ----

    private ArrayNode buildToolsNode() {
        ArrayNode tools = objectMapper.createArrayNode();
        ObjectNode toolEntry = objectMapper.createObjectNode();
        ArrayNode declarations = objectMapper.createArrayNode();

        declarations.add(functionDeclaration(
                "create_booking",
                "Book seats for a show. Requires an existing showId and userId.",
                Map.of(
                        "showId", Map.of("type", "INTEGER"),
                        "userId", Map.of("type", "INTEGER"),
                        "seatNumbers", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                ),
                List.of("showId", "userId", "seatNumbers")
        ));

        declarations.add(functionDeclaration(
                "get_ticket",
                "Look up an existing booking/ticket by its ID.",
                Map.of("bookingId", Map.of("type", "INTEGER")),
                List.of("bookingId")
        ));

        declarations.add(functionDeclaration(
                "register_user",
                "Register a new user account with an email and password.",
                Map.of(
                        "email", Map.of("type", "STRING"),
                        "password", Map.of("type", "STRING")
                ),
                List.of("email", "password")
        ));

        declarations.add(functionDeclaration(
                "login_user",
                "Log in an existing user with email and password.",
                Map.of(
                        "email", Map.of("type", "STRING"),
                        "password", Map.of("type", "STRING")
                ),
                List.of("email", "password")
        ));

        toolEntry.set("functionDeclarations", declarations);
        tools.add(toolEntry);
        return tools;
    }

    private ObjectNode functionDeclaration(String name, String description,
                                           Map<String, Object> properties,
                                           List<String> required) {
        ObjectNode decl = objectMapper.createObjectNode();
        decl.put("name", name);
        decl.put("description", description);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "OBJECT");
        parameters.set("properties", objectMapper.valueToTree(properties));

        ArrayNode requiredArray = objectMapper.createArrayNode();
        required.forEach(requiredArray::add);
        parameters.set("required", requiredArray);

        decl.set("parameters", parameters);
        return decl;
    }
}