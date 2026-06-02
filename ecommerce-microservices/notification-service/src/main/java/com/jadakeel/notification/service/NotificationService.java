package com.jadakeel.notification.service;

import com.jadakeel.notification.model.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification sendNotification(UUID recipientId, UUID orderId, String subject, String message, Notification.NotificationType type);
    void sendOrderCreatedNotification(UUID orderId, UUID customerId);
    void sendPaymentSuccessNotification(UUID orderId, UUID customerId);
    void sendPaymentFailedNotification(UUID orderId, UUID customerId);
    void sendOrderShippedNotification(UUID orderId, UUID customerId, String trackingNumber);
    List<Notification> getNotificationsByRecipient(UUID recipientId);
    List<Notification> getNotificationsByOrder(UUID orderId);
}
