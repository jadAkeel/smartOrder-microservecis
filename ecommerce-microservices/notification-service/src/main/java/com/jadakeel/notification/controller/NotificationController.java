package com.jadakeel.notification.controller;

import com.jadakeel.notification.dto.NotificationRequest;
import com.jadakeel.notification.model.Notification;
import com.jadakeel.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send-email")
    public ResponseEntity<Notification> sendEmail(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.sendNotification(
                request.getRecipientId(),
                request.getOrderId(),
                request.getSubject(),
                request.getMessage(),
                Notification.NotificationType.EMAIL
        ));
    }

    @PostMapping("/send-sms")
    public ResponseEntity<Notification> sendSms(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.sendNotification(
                request.getRecipientId(),
                request.getOrderId(),
                request.getSubject(),
                request.getMessage(),
                Notification.NotificationType.SMS
        ));
    }

    @PostMapping("/order-update")
    public ResponseEntity<Notification> sendOrderUpdate(@Valid @RequestBody NotificationRequest request) {
        Notification.NotificationType type = request.getType() == null
                ? Notification.NotificationType.ORDER_CONFIRMED
                : request.getType();
        return ResponseEntity.ok(notificationService.sendNotification(
                request.getRecipientId(),
                request.getOrderId(),
                request.getSubject(),
                request.getMessage(),
                type
        ));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Notification>> getNotificationsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRecipient(customerId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getNotificationsByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(notificationService.getNotificationsByOrder(orderId));
    }
}
