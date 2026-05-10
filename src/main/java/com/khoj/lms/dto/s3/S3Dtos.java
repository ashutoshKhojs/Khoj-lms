package com.khoj.lms.dto.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

public class S3Dtos {

    // ── Presign Requests ──────────────────────

    @Getter @Setter
    public static class VideoPresignRequest {
        @NotNull
        private UUID courseId;
        @NotNull
        private UUID moduleId;
        @NotNull
        private UUID lessonId;
        @NotBlank
        private String filename;            // "lecture1.mp4"
        @NotBlank
        private String contentType;         // "video/mp4"
        private long fileSizeBytes;         // used to decide multipart
    }

    @Getter @Setter
    public static class ImagePresignRequest {
        private UUID courseId;              // null for user avatar
        private UUID userId;
        @NotBlank
        private String filename;
        @NotBlank
        private String contentType;
        private ImagePurpose purpose;
    }

    public enum ImagePurpose {
        COURSE_THUMBNAIL,
        USER_AVATAR
    }

    // ── Presign Response ──────────────────────

    @Getter @Setter @Builder
    public static class PresignResponse {
        private String uploadUrl;           // PUT this URL directly from frontend
        private String s3Key;               // save this — use in lesson creation
        private String fileId;             // for multipart: uploadId
        private UploadStrategy strategy;
        private long expiresInSeconds;
        private List<PartPresignResponse> parts;  // for multipart only
    }

    public enum UploadStrategy {
        SINGLE_PUT,         // file < 10MB — one PUT request
        MULTIPART           // file >= 10MB — multiple parts
    }

    @Getter @Setter @Builder
    public static class PartPresignResponse {
        private int partNumber;
        private String uploadUrl;
    }

    // ── Multipart Complete ────────────────────

    @Getter @Setter
    public static class CompleteMultipartRequest {
        @NotBlank
        private String s3Key;
        @NotBlank
        private String uploadId;
        private List<CompletedPartDto> parts;
    }

    @Getter @Setter
    public static class CompletedPartDto {
        private int partNumber;
        private String eTag;
    }

    // ── Streaming URL ─────────────────────────

    @Getter @Setter @Builder
    public static class StreamUrlResponse {
        private String streamUrl;           // presigned GET URL
        private long expiresInSeconds;
    }

    // ── Delete ────────────────────────────────

    @Getter @Setter
    public static class DeleteRequest {
        @NotBlank
        private String s3Key;
    }
}