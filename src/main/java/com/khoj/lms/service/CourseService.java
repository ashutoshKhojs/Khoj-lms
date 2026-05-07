package com.khoj.lms.service;

import com.khoj.lms.dto.course.*;
import com.khoj.lms.entity.Course;
import com.khoj.lms.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseService {

    // PUBLIC
    Page<CourseSummary> getPublishedCourses(CourseFilter filter, Pageable pageable);

    CourseResponse getCourseBySlug(String slug);

    // INSTRUCTOR
    Page<InstructorCourseView> getInstructorCourses(String instructorEmail, Pageable pageable);

    CourseResponse createCourse(CourseRequest request, String instructorEmail);

    CourseResponse updateCourse(UUID courseId, CourseRequest request, String instructorEmail);

    CourseResponse submitForReview(UUID courseId, String instructorEmail);

    void deleteCourse(UUID courseId, String instructorEmail);

    // ADMIN
    Page<CourseSummary> getAllCoursesForAdmin(CourseStatus status, Pageable pageable);

    CourseResponse approveCourse(UUID courseId, String adminEmail);

    CourseResponse rejectCourse(UUID courseId, CourseRejectionRequest request, String adminEmail);

    CourseResponse archiveCourse(UUID courseId);

    // INTERNAL
    void recalculateStats(UUID courseId);

    Course findCourseOrThrow(UUID id);
}