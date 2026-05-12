package com.khoj.lms.controller;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.common.ApiResponse;
import com.khoj.lms.dto.s3.S3Dtos.*;
import com.khoj.lms.service.S3Service;
import com.khoj.lms.util.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.Upload.BASE)
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Presigned URL generation for S3 direct upload")
public class UploadController {

    private final S3Service   s3Service;
    private final AuditLogger auditLogger;

    // ─────────────────────────────────────────
    // Video — get presigned upload URL
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Upload.VIDEO_PRESIGN)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Get presigned URL to upload lesson video directly to S3")
    public ResponseEntity<ApiResponse<PresignResponse>> presignVideoUpload(
            @Valid @RequestBody VideoPresignRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {  // ← added

        PresignResponse response = s3Service.presignVideoUpload(request);

        auditLogger.videoUploadInitiated(
                userDetails.getUsername(),
                response.getS3Key()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Presigned URL generated. Upload video directly to uploadUrl.",
                response));
    }

    // ─────────────────────────────────────────
    // Video — complete multipart upload
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Upload.COMPLETE_MULTIPART)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Complete multipart upload after all parts uploaded")
    public ResponseEntity<ApiResponse<Void>> completeMultipart(
            @Valid @RequestBody CompleteMultipartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {  // ← added

        s3Service.completeMultipartUpload(request);

        auditLogger.videoUploadCompleted(
                userDetails.getUsername(),
                request.getS3Key()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Video upload completed successfully."));
    }

    // ─────────────────────────────────────────
    // Video — abort multipart (on frontend failure)
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Upload.ABORT_MULTIPART)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Abort multipart upload and clean up S3")
    public ResponseEntity<ApiResponse<Void>> abortMultipart(
            @RequestParam String s3Key,
            @RequestParam String uploadId,
            @AuthenticationPrincipal UserDetails userDetails) {  // ← added

        s3Service.abortMultipartUpload(s3Key, uploadId);

        auditLogger.videoUploadAborted(
                userDetails.getUsername(),
                s3Key
        );

        return ResponseEntity.ok(
                ApiResponse.success("Upload aborted and cleaned up."));
    }

    // ─────────────────────────────────────────
    // Image — thumbnail / avatar
    // ─────────────────────────────────────────

    @PostMapping(ApiRoutes.Upload.IMAGE_PRESIGN)
    @Operation(summary = "Get presigned URL to upload image (thumbnail or avatar)")
    public ResponseEntity<ApiResponse<PresignResponse>> presignImageUpload(
            @Valid @RequestBody ImagePresignRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {  // ← already correct

        PresignResponse response = s3Service.presignImageUpload(request);

        auditLogger.imageUploaded(
                userDetails.getUsername(),
                response.getS3Key()
        );

        return ResponseEntity.ok(ApiResponse.success(
                "Presigned URL generated. Upload image directly to uploadUrl.",
                response));
    }

    // ─────────────────────────────────────────
    // Get stream URL (student watching video)
    // ─────────────────────────────────────────

    @GetMapping(ApiRoutes.Upload.STREAM_URL)
    @Operation(summary = "Get temporary streaming URL for a video or image")
    public ResponseEntity<ApiResponse<StreamUrlResponse>> getStreamUrl(
            @RequestParam String s3Key,
            @RequestParam(defaultValue = "true") boolean isVideo) {

        StreamUrlResponse response = s3Service.generateStreamUrl(s3Key, isVideo);

        return ResponseEntity.ok(
                ApiResponse.success("Stream URL generated.", response));
    }
}