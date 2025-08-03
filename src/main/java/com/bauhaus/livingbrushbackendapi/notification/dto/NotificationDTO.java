package com.bauhaus.livingbrushbackendapi.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long notificationId;
    private String type;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedUserNickname;
    private String relatedUserProfileImage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("isRead")
    private boolean isRead;
}
