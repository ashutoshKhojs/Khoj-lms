package com.khoj.lms.config;

import com.khoj.lms.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final AwsProperties awsProperties;

    // ── Credentials ───────────────────────────

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        awsProperties.getS3().getAccessKey(),
                        awsProperties.getS3().getSecretKey()
                )
        );
    }

    private Region region() {
        return Region.of(awsProperties.getRegion());
    }

    // ── Sync S3 client (metadata, delete ops) ─

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(region())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    // ── Async S3 client (multipart uploads) ───
    // Uses AWS CRT for maximum throughput

    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.crtBuilder()
                .region(region())
                .credentialsProvider(credentialsProvider())
                .targetThroughputInGbps(10.0)          // max throughput
                .minimumPartSizeInBytes(
                        awsProperties.getS3().getMultipartPartSizeMb() * 1024 * 1024)
                .build();
    }

    // ── Transfer Manager (multipart orchestration) ─

    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        return S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
    }

    // ── Presigner (presigned URLs) ─────────────

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(region())
                .credentialsProvider(credentialsProvider())
                .build();
    }
}