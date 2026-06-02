package com.jadakeel.notification.dto;

import com.jadakeel.notification.model.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    @NotNull
    private UUID recipientId;

    private UUID orderId;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;

    private Notification.NotificationType type;
}
