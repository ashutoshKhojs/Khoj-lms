package com.khoj.lms.service;

import com.khoj.lms.dto.s3.S3Dtos.*;

public interface S3Service {

    // Presigned upload URLs
    PresignResponse presignVideoUpload(VideoPresignRequest request);
    PresignResponse presignImageUpload(ImagePresignRequest request);

    // Multipart (large videos)
    String initiateMultipartUpload(String s3Key, String contentType);
    PresignResponse presignMultipartParts(String s3Key, String uploadId, int totalParts);
    void completeMultipartUpload(CompleteMultipartRequest request);
    void abortMultipartUpload(String s3Key, String uploadId);

    // Streaming URLs (for students watching video)
    StreamUrlResponse generateStreamUrl(String s3Key, boolean isVideo);

    // Delete
    void deleteObject(String s3Key);
    void deleteFolder(String s3Prefix);

    // Validation
    boolean objectExists(String s3Key);
    long getObjectSizeBytes(String s3Key);
}