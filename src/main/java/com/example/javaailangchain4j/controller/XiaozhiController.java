package com.example.javaailangchain4j.controller;

import com.example.javaailangchain4j.assistant.XiaozhiAgent;
import com.example.javaailangchain4j.safety.MedicalSafetyGuard;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequestMapping("/xiaozhi")
@RestController
public class XiaozhiController {

    @Resource
    private XiaozhiAgent xiaozhiAgent;

    @Operation(summary = "对话")
    @PostMapping("/chat")
    public Flux<String> chat(@RequestParam("id") int id, @RequestParam("message") String message) {
        if (MedicalSafetyGuard.isEmergency(message)) {
            return Flux.just(MedicalSafetyGuard.emergencyResponse());
        }
        return xiaozhiAgent.chat(id, message);
    }
}
