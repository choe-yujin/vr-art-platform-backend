package com.bauhaus.livingbrushbackendapi.qrcode.repository;

import com.bauhaus.livingbrushbackendapi.artwork.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.qrcode.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QR 코드 데이터 접근 리포지토리 (리팩토링 v2.0)
 *
 * QR 코드 엔티티에 대한 데이터베이스 접근 기능을 제공합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    // ==================== Token 기반 조회 ====================

    /**
     * QR 토큰으로 QR 코드를 조회합니다. (활성 상태 무관)
     * QR 스캔 시, 토큰의 존재 여부 및 활성 상태를 순차적으로 확인하기 위해 사용됩니다.
     *
     * @param qrToken QR 토큰 (UUID)
     * @return QR 코드 (Optional)
     */
    Optional<QrCode> findByQrToken(UUID qrToken);

    /**
     * QR 토큰으로 '활성화된' QR 코드를 조회합니다.
     *
     * @param qrToken QR 토큰 (UUID)
     * @return 활성화된 QR 코드 (Optional)
     */
    Optional<QrCode> findByQrTokenAndIsActiveTrue(UUID qrToken);

    /**
     * QR 토큰의 존재 여부를 확인합니다.
     * 새로운 QR 토큰 생성 시 중복을 방지하기 위해 사용됩니다.
     *
     * @param qrToken 확인할 QR 토큰
     * @return 존재하면 true
     */
    boolean existsByQrToken(UUID qrToken);


    // ==================== Artwork 기반 조회 ====================

    /**
     * 특정 작품에 연결된 '활성화된' QR 코드 목록을 조회합니다.
     * (정상적인 로직에서는 항상 0개 또는 1개의 결과만 반환됩니다)
     *
     * @param artwork 작품 엔티티
     * @return 활성화된 QR 코드 목록
     */
    List<QrCode> findByArtworkAndIsActiveTrue(Artwork artwork);

    /**
     * 특정 작품에 대한 모든 QR 코드 이력을 생성일 내림차순으로 조회합니다.
     * 관리자 페이지나 분석 용도로 사용될 수 있습니다.
     *
     * @param artwork 작품 엔티티
     * @return 해당 작품의 모든 QR 코드 목록
     */
    List<QrCode> findByArtworkOrderByCreatedAtDesc(Artwork artwork);


    // ==================== 커스텀 쿼리 및 수정 ====================

    /**
     * 특정 작품에 연결된 모든 '활성화된' QR 코드를 비활성화 상태로 변경합니다.
     * 새로운 QR 코드를 생성하기 직전에 호출됩니다.
     *
     * @param artworkId 비활성화할 작품의 ID
     * @return 업데이트된 레코드의 수
     */
    @Modifying
    @Query("UPDATE QrCode q SET q.isActive = false WHERE q.artwork.id = :artworkId AND q.isActive = true")
    int deactivateAllByArtworkId(@Param("artworkId") Long artworkId);
}