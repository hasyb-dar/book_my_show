package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scaler.bms.may2026.service.GeminiAssistantService;

@RestController
@RequestMapping("/gemini")
public class GeminiAssistantController {

    @Autowired
    private GeminiAssistantService geminiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String userMessage) {
        String reply = geminiAssistantService.handleMessage(userMessage);
        return ResponseEntity.ok(reply);
    }
}