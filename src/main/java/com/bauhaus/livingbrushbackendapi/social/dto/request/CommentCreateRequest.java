package com.bauhaus.livingbrushbackendapi.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 작성 요청 DTO
 * 
 * Scene 8 "정아가 댓글을 남긴다" 기능의 요청을 담당합니다.
 * 댓글 내용에 대한 유효성 검증을 포함합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentCreateRequest {

    /**
     * 댓글 내용 (1자 이상 200자 이하)
     */
    @NotBlank(message = "댓글 내용을 입력해주세요")
    @Size(min = 1, max = 200, message = "댓글은 1자 이상 200자 이하로 입력해주세요")
    private String content;

    /**
     * 생성자 (테스트용)
     * 
     * @param content 댓글 내용
     */
    public CommentCreateRequest(String content) {
        this.content = content;
    }

    /**
     * 댓글 내용 반환 (trim 처리)
     * 
     * @return 정제된 댓글 내용
     */
    public String getTrimmedContent() {
        return this.content != null ? this.content.trim() : null;
    }

    /**
     * 유효한 댓글인지 확인
     * 
     * @return 유효성 여부
     */
    public boolean isValid() {
        String trimmedContent = getTrimmedContent();
        return trimmedContent != null && 
               !trimmedContent.isEmpty() && 
               trimmedContent.length() <= 200;
    }

    /**
     * 댓글 길이 반환
     * 
     * @return 댓글 길이
     */
    public int getContentLength() {
        String trimmedContent = getTrimmedContent();
        return trimmedContent != null ? trimmedContent.length() : 0;
    }
}
