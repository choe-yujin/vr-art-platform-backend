package com.bauhaus.livingbrushbackendapi.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 알림 타입 상수
 */
class NotificationType {
    public static final String NEW_FOLLOWER = "new_follower";
    public static final String NEW_LIKE = "new_like";
    public static final String NEW_COMMENT = "new_comment";
}

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
