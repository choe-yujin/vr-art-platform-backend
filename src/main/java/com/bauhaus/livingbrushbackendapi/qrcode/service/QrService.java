package com.bauhaus.livingbrushbackendapi.qrcode.service;

import com.bauhaus.livingbrushbackendapi.exception.common.CustomException;
import com.bauhaus.livingbrushbackendapi.qrcode.dto.QrGenerateResponse;

/**
 * QR 코드 생성 서비스 인터페이스 (리팩토링 v2.0)
 *
 * 예외 처리 방식을 CustomException으로 통일하여 애플리케이션의 일관성을 확보합니다.
 *
 * @author Bauhaus Team
 * @version 2.0
 */
public interface QrService {

    /**
     * 작품에 대한 QR 코드를 생성합니다.
     *
     * 이 메소드를 구현하는 클래스는 비즈니스 로직 실패 시 CustomException을 발생시켜야 합니다.
     *
     * @param artworkId QR 코드를 생성할 작품의 ID
     * @return QR 이미지 URL을 포함한 응답 객체
     * @throws CustomException 작품이 존재하지 않는 경우 (예: ErrorCode.ARTWORK_NOT_FOUND),
     *                         또는 QR 생성 중 내부 오류가 발생한 경우.
     */
    QrGenerateResponse generateQr(Long artworkId);

    /**
     * QR 코드 이미지의 바이트 데이터를 생성합니다.
     *
     * @param qrData QR 코드에 포함될 데이터
     * @return QR 이미지의 바이트 배열
     * @throws CustomException QR 생성 중 내부 오류가 발생한 경우
     */
    byte[] generateQrImageBytes(String qrData);
}