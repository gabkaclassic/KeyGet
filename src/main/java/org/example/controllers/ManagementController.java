package org.example.controllers;

import org.example.sender.KeySender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/management")
public class ManagementController {

    private final KeySender sender;

    public ManagementController(KeySender sender) {
        this.sender = sender;
    }

    @PostMapping
    public ResponseEntity changeKeys() {

        sender.sendKey();

        return ResponseEntity.ok().build();
    }
}
