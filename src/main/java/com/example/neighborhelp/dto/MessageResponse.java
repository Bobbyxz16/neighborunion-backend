package com.example.neighborhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class MessageResponse {
    private String message;
    public MessageResponse(String message) {
        this.message = message;
    }
}