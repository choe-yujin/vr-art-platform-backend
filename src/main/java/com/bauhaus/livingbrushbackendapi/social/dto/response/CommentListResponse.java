package com.bauhaus.livingbrushbackendapi.social.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 댓글 목록 응답 DTO
 * 
 * Scene 8 댓글 목록 조회 기능의 응답을 담당합니다.
 * 페이징 정보와 함께 댓글 목록을 제공합니다.
 * 
 * @author Bauhaus Team
 * @version 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentListResponse {

    /**
     * 댓글 목록
     */
    private List<CommentResponse> comments;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int currentPage;

    /**
     * 페이지 크기
     */
    private int pageSize;

    /**
     * 전체 댓글 수
     */
    private long totalElements;

    /**
     * 전체 페이지 수
     */
    private int totalPages;

    /**
     * 마지막 페이지 여부
     */
    private boolean isLast;

    /**
     * 댓글 개수
     */
    private int commentCount;

    /**
     * 생성자
     */
    private CommentListResponse(List<CommentResponse> comments, int currentPage, int pageSize,
                               long totalElements, int totalPages, boolean isLast) {
        this.comments = comments;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isLast = isLast;
        this.commentCount = comments.size();
    }

    /**
     * 댓글 목록 응답 생성
     * 
     * @param comments 댓글 목록
     * @param currentPage 현재 페이지
     * @param pageSize 페이지 크기
     * @param totalElements 전체 요소 수
     * @param totalPages 전체 페이지 수
     * @param isLast 마지막 페이지 여부
     * @return 댓글 목록 응답
     */
    public static CommentListResponse of(List<CommentResponse> comments, int currentPage, int pageSize,
                                        long totalElements, int totalPages, boolean isLast) {
        return new CommentListResponse(comments, currentPage, pageSize, totalElements, totalPages, isLast);
    }

    /**
     * 빈 댓글 목록 응답 생성
     * 
     * @return 빈 댓글 목록 응답
     */
    public static CommentListResponse empty() {
        return new CommentListResponse(List.of(), 0, 0, 0L, 0, true);
    }

    /**
     * 댓글이 있는지 확인
     * 
     * @return 댓글 존재 여부
     */
    public boolean hasComments() {
        return !comments.isEmpty();
    }

    /**
     * 첫 번째 페이지인지 확인
     * 
     * @return 첫 페이지 여부
     */
    public boolean isFirst() {
        return currentPage == 0;
    }

    /**
     * 다음 페이지가 있는지 확인
     * 
     * @return 다음 페이지 존재 여부
     */
    public boolean hasNext() {
        return !isLast;
    }

    /**
     * 이전 페이지가 있는지 확인
     * 
     * @return 이전 페이지 존재 여부
     */
    public boolean hasPrevious() {
        return currentPage > 0;
    }
}
