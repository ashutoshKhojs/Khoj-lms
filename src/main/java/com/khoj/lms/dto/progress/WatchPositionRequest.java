package com.khoj.lms.dto.progress;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WatchPositionRequest {

    @NotNull(message = "Watch position is required")
    @Min(value = 0, message = "Watch position cannot be negative")
    private Long watchPositionSeconds;

    /** Optional: how many extra seconds were watched since last heartbeat */
    @Min(value = 0, message = "Delta cannot be negative")
    private Long deltaSeconds;
}