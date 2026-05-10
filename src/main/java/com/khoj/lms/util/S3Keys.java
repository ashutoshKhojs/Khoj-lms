package com.khoj.lms.util;

import java.util.UUID;

public class S3Keys {

    // courses/{courseId}/thumbnails/{uuid}.jpg
    public static String courseThumbnail(UUID courseId, String extension) {
        return "courses/" + courseId + "/thumbnails/" + UUID.randomUUID() + "." + extension;
    }

    // courses/{courseId}/modules/{moduleId}/lessons/{lessonId}/video/{uuid}.mp4
    public static String lessonVideo(UUID courseId, UUID moduleId, UUID lessonId, String extension) {
        return "courses/" + courseId
                + "/modules/" + moduleId
                + "/lessons/" + lessonId
                + "/video/" + UUID.randomUUID() + "." + extension;
    }

    // courses/{courseId}/modules/{moduleId}/lessons/{lessonId}/docs/{uuid}.pdf
    public static String lessonDocument(UUID courseId, UUID moduleId, UUID lessonId, String extension) {
        return "courses/" + courseId
                + "/modules/" + moduleId
                + "/lessons/" + lessonId
                + "/docs/" + UUID.randomUUID() + "." + extension;
    }

    // users/{userId}/avatar/{uuid}.jpg
    public static String userAvatar(UUID userId, String extension) {
        return "users/" + userId + "/avatar/" + UUID.randomUUID() + "." + extension;
    }

    public static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}