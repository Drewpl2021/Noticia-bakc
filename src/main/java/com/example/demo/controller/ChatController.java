package com.example.demo.controller;

import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.service.GroqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GroqService groqService;

    @PostMapping
    public Mono<ChatResponse> chat(@RequestBody ChatRequest req) {
        return groqService.askGroq(req.getMessage())
                .map(ChatResponse::new);
    }
}
