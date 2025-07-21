package com.bauhaus.livingbrushbackendapi.storage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS S3 설정 클래스
 *
 * S3Client Bean을 등록하여 S3 파일 업로드 서비스에서 사용할 수 있도록 합니다.
 * local 프로필이 아닌 환경(dev, prod)에서만 활성화됩니다.
 *
 * @author Bauhaus Team
 * @since 1.0
 */
@Slf4j
@Configuration
@Profile("!local")
public class AwsS3Config {

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * S3Client Bean을 생성합니다.
     *
     * DefaultCredentialsProvider는 다음 순서로 자격증명을 찾습니다:
     * 1. 환경변수 (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * 2. Java 시스템 속성
     * 3. ~/.aws/credentials 파일
     * 4. EC2 인스턴스 프로파일 (IAM Role)
     *
     * @return S3Client 인스턴스
     */
    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        log.info("S3Client Bean 생성 완료 - Region: {}", region);
        return client;
    }
}
