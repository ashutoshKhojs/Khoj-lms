package com.khoj.lms.service.impl;

import com.khoj.lms.config.properties.AwsProperties;
import com.khoj.lms.dto.s3.S3Dtos.*;
import com.khoj.lms.exception.BadRequestException;
import com.khoj.lms.service.S3Service;
import com.khoj.lms.util.S3Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final S3Client        s3Client;
    private final S3Presigner     s3Presigner;
    private final AwsProperties   awsProperties;

    private static final long MB = 1024L * 1024L;

    // ─────────────────────────────────────────
    // PRESIGN VIDEO UPLOAD
    // ─────────────────────────────────────────

    @Override
    public PresignResponse presignVideoUpload(VideoPresignRequest request) {

        validateContentType(request.getContentType(), List.of(
                "video/mp4", "video/webm", "video/quicktime"
        ));

        String extension = S3Keys.extractExtension(request.getFilename());
        String s3Key = S3Keys.lessonVideo(
                request.getCourseId(),
                request.getModuleId(),
                request.getLessonId(),
                extension
        );

        long thresholdBytes = awsProperties.getS3().getMultipartThresholdMb() * MB;

        // Large video → multipart upload
        if (request.getFileSizeBytes() > thresholdBytes) {
            return buildMultipartPresignResponse(s3Key, request.getContentType(),
                    request.getFileSizeBytes());
        }

        // Small video → single PUT
        return buildSinglePutPresignResponse(s3Key, request.getContentType(),
                awsProperties.getS3().getPresignedUrlExpiryMinutes());
    }

    // ─────────────────────────────────────────
    // PRESIGN IMAGE UPLOAD
    // ─────────────────────────────────────────

    @Override
    public PresignResponse presignImageUpload(ImagePresignRequest request) {

        validateContentType(request.getContentType(), List.of(
                "image/jpeg", "image/png", "image/webp"
        ));

        String extension = S3Keys.extractExtension(request.getFilename());
        String s3Key;

        switch (request.getPurpose()) {
            case COURSE_THUMBNAIL ->
                    s3Key = S3Keys.courseThumbnail(request.getCourseId(), extension);
            case USER_AVATAR ->
                    s3Key = S3Keys.userAvatar(request.getUserId(), extension);
            default -> throw new BadRequestException("Unknown image purpose");
        }

        return buildSinglePutPresignResponse(s3Key, request.getContentType(),
                awsProperties.getS3().getPresignedUrlExpiryMinutes());
    }

    // ─────────────────────────────────────────
    // MULTIPART — INITIATE
    // ─────────────────────────────────────────

    @Override
    public String initiateMultipartUpload(String s3Key, String contentType) {
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(awsProperties.getS3().getBucketName())
                        .key(s3Key)
                        .contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256)  // encrypt at rest
                        .build()
        );
        log.info("Multipart upload initiated: key={}, uploadId={}", s3Key, response.uploadId());
        return response.uploadId();
    }

    // ─────────────────────────────────────────
    // MULTIPART — PRESIGN EACH PART
    // ─────────────────────────────────────────

    @Override
    public PresignResponse presignMultipartParts(String s3Key, String uploadId, int totalParts) {
        List<PartPresignResponse> parts = new ArrayList<>();

        for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(
                            awsProperties.getS3().getPresignedUrlExpiryMinutes()))
                    .uploadPartRequest(UploadPartRequest.builder()
                            .bucket(awsProperties.getS3().getBucketName())
                            .key(s3Key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .build())
                    .build();

            PresignedUploadPartRequest presigned = s3Presigner.presignUploadPart(presignRequest);

            parts.add(PartPresignResponse.builder()
                    .partNumber(partNumber)
                    .uploadUrl(presigned.url().toString())
                    .build());
        }

        return PresignResponse.builder()
                .s3Key(s3Key)
                .fileId(uploadId)
                .strategy(UploadStrategy.MULTIPART)
                .parts(parts)
                .expiresInSeconds(awsProperties.getS3().getPresignedUrlExpiryMinutes() * 60)
                .build();
    }

    // ─────────────────────────────────────────
    // MULTIPART — COMPLETE
    // ─────────────────────────────────────────

    @Override
    public void completeMultipartUpload(CompleteMultipartRequest request) {
        List<CompletedPart> completedParts = request.getParts().stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getETag())
                        .build())
                .collect(Collectors.toList());

        s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(awsProperties.getS3().getBucketName())
                        .key(request.getS3Key())
                        .uploadId(request.getUploadId())
                        .multipartUpload(CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build())
                        .build()
        );
        log.info("Multipart upload completed: key={}", request.getS3Key());
    }

    // ─────────────────────────────────────────
    // MULTIPART — ABORT (cleanup on failure)
    // ─────────────────────────────────────────

    @Override
    public void abortMultipartUpload(String s3Key, String uploadId) {
        try {
            s3Client.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                            .bucket(awsProperties.getS3().getBucketName())
                            .key(s3Key)
                            .uploadId(uploadId)
                            .build()
            );
            log.info("Multipart upload aborted: key={}, uploadId={}", s3Key, uploadId);
        } catch (Exception e) {
            log.error("Failed to abort multipart upload: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // STREAM URL (students watching video)
    // ─────────────────────────────────────────

    @Override
    public StreamUrlResponse generateStreamUrl(String s3Key, boolean isVideo) {
        long expiryHours = isVideo
                ? awsProperties.getS3().getVideoUrlExpiryHours()
                : awsProperties.getS3().getImageUrlExpiryHours();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(expiryHours))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(awsProperties.getS3().getBucketName())
                        .key(s3Key)
                        .build())
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();

        return StreamUrlResponse.builder()
                .streamUrl(url)
                .expiresInSeconds(expiryHours * 3600)
                .build();
    }

    // ─────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────

    @Override
    public void deleteObject(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(awsProperties.getS3().getBucketName())
                    .key(s3Key)
                    .build());
            log.info("S3 object deleted: {}", s3Key);
        } catch (Exception e) {
            log.error("Failed to delete S3 object {}: {}", s3Key, e.getMessage());
        }
    }

    @Override
    public void deleteFolder(String s3Prefix) {
        // List all objects with prefix, then delete in batches of 1000
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(awsProperties.getS3().getBucketName())
                .prefix(s3Prefix)
                .build();

        ListObjectsV2Response listResponse;
        do {
            listResponse = s3Client.listObjectsV2(listRequest);
            List<ObjectIdentifier> toDelete = listResponse.contents().stream()
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .collect(Collectors.toList());

            if (!toDelete.isEmpty()) {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(awsProperties.getS3().getBucketName())
                        .delete(Delete.builder().objects(toDelete).build())
                        .build());
                log.info("Deleted {} objects under prefix: {}", toDelete.size(), s3Prefix);
            }

            listRequest = listRequest.toBuilder()
                    .continuationToken(listResponse.nextContinuationToken())
                    .build();

        } while (listResponse.isTruncated());
    }

    // ─────────────────────────────────────────
    // VALIDATION HELPERS
    // ─────────────────────────────────────────

    @Override
    public boolean objectExists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(awsProperties.getS3().getBucketName())
                    .key(s3Key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public long getObjectSizeBytes(String s3Key) {
        HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucketName())
                .key(s3Key)
                .build());
        return head.contentLength();
    }

    // ─────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────

    private PresignResponse buildSinglePutPresignResponse(
            String s3Key, String contentType, long expiryMinutes) {

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(awsProperties.getS3().getBucketName())
                        .key(s3Key)
                        .contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build())
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

        return PresignResponse.builder()
                .uploadUrl(uploadUrl)
                .s3Key(s3Key)
                .strategy(UploadStrategy.SINGLE_PUT)
                .expiresInSeconds(expiryMinutes * 60)
                .build();
    }

    private PresignResponse buildMultipartPresignResponse(
            String s3Key, String contentType, long fileSizeBytes) {

        long partSizeBytes = awsProperties.getS3().getMultipartPartSizeMb() * MB;
        int totalParts = (int) Math.ceil((double) fileSizeBytes / partSizeBytes);

        String uploadId = initiateMultipartUpload(s3Key, contentType);
        PresignResponse partsResponse = presignMultipartParts(s3Key, uploadId, totalParts);

        return PresignResponse.builder()
                .s3Key(s3Key)
                .fileId(uploadId)
                .strategy(UploadStrategy.MULTIPART)
                .parts(partsResponse.getParts())
                .expiresInSeconds(awsProperties.getS3().getPresignedUrlExpiryMinutes() * 60)
                .build();
    }

    private void validateContentType(String contentType, List<String> allowed) {
        if (!allowed.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid content type: " + contentType +
                            ". Allowed: " + String.join(", ", allowed)
            );
        }
    }
}