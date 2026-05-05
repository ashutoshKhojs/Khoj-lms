package com.khoj.lms.dto.auth;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageResponse {

    private String message;
    private boolean success;
}