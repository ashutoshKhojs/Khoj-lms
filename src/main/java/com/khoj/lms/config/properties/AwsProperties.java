package com.khoj.lms.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws")
@Getter @Setter
public class AwsProperties {

    private String region;
    private S3 s3 = new S3();

    @Getter @Setter
    public static class S3 {
        private String bucketName;
        private String accessKey;
        private String secretKey;
        private long presignedUrlExpiryMinutes = 15;
        private long videoUrlExpiryHours = 2;
        private long imageUrlExpiryHours = 24;
        private long multipartThresholdMb = 10;
        private long multipartPartSizeMb = 10;
    }
}