package com.example.audit_service.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.kafka")
public class AuditKafkaProperties {

    private boolean enabled;
    private String topic = "platform.task-events";
    private String userTopic = "platform.user-events";
    private String notificationTopic = "platform.notification-events";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getUserTopic() {
        return userTopic;
    }

    public void setUserTopic(String userTopic) {
        this.userTopic = userTopic;
    }

    public String getNotificationTopic() {
        return notificationTopic;
    }

    public void setNotificationTopic(String notificationTopic) {
        this.notificationTopic = notificationTopic;
    }
}
