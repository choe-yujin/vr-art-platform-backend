package com.bauhaus.livingbrushbackendapi.repository;

import com.bauhaus.livingbrushbackendapi.entity.Artwork;
import com.bauhaus.livingbrushbackendapi.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * QR 코드 데이터 접근 리포지토리
 *
 * QR 코드 엔티티에 대한 데이터베이스 접근 기능을 제공합니다.
 * 활성/비활성 QR 코드 관리 및 작품별 QR 코드 조회 기능을 포함합니다.
 */
@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    /**
     * QR 토큰으로 활성화된 QR 코드를 조회합니다.
     * WebAR 접근 시 사용됩니다.
     *
     * @param qrToken QR 토큰 (UUID)
     * @return 활성화된 QR 코드 (Optional)
     */
    // FIX: Changed parameter type from String to UUID and removed the duplicate method.
    Optional<QrCode> findByQrTokenAndIsActiveTrue(UUID qrToken);

    /**
     * 작품에 대한 활성화된 QR 코드를 조회합니다.
     * AR 앱에서 작품의 현재 QR 코드 확인 시 사용됩니다.
     *
     * @param artwork 작품 엔티티
     * @return 활성화된 QR 코드 목록 (일반적으로 1개)
     */
    List<QrCode> findByArtworkAndIsActiveTrue(Artwork artwork);

    /**
     * 작품 ID로 활성화된 QR 코드를 조회합니다.
     * 성능 최적화를 위해 Artwork 엔티티 로딩 없이 ID만 사용합니다.
     *
     * @param artworkId 작품 ID
     * @return 활성화된 QR 코드 (Optional)
     */
    @Query("SELECT q FROM QrCode q WHERE q.artwork.artworkId = :artworkId AND q.isActive = true")
    Optional<QrCode> findActiveByArtworkId(@Param("artworkId") Long artworkId);

    /**
     * 작품의 모든 QR 코드를 비활성화합니다.
     * 새로운 QR 생성 전 기존 QR들을 비활성화할 때 사용됩니다.
     *
     * @param artworkId 작품 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE QrCode q SET q.isActive = false WHERE q.artwork.artworkId = :artworkId AND q.isActive = true")
    int deactivateAllByArtworkId(@Param("artworkId") Long artworkId);

    /**
     * 작품에 대한 모든 QR 코드 이력을 조회합니다.
     * 관리자 페이지나 분석 용도로 사용됩니다.
     *
     * @param artwork 작품 엔티티
     * @return QR 코드 이력 목록 (생성 순서 내림차순)
     */
    List<QrCode> findByArtworkOrderByCreatedAtDesc(Artwork artwork);

    /**
     * QR 토큰 존재 여부를 확인합니다.
     * QR 토큰 중복 방지를 위해 사용됩니다.
     *
     * @param qrToken QR 토큰
     * @return 존재 여부
     */
    boolean existsByQrToken(UUID qrToken);
}