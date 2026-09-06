package com.example.scaler.bms.may2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scaler.bms.may2026.service.AssistantService;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    @Autowired
    private AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody String userMessage) {
        String reply = assistantService.handleMessage(userMessage);
        return ResponseEntity.ok(reply);
    }
}
