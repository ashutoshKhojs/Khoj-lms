package com.khoj.lms.service.impl;

import com.khoj.lms.audit.AuditLogger;
import com.khoj.lms.dto.course.*;
import com.khoj.lms.dto.lesson.LessonSummary;
import com.khoj.lms.dto.module.ModuleResponse;
import com.khoj.lms.entity.*;
import com.khoj.lms.entity.Module;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.CourseService;
import com.khoj.lms.specification.CourseSpecification;
import com.khoj.lms.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository   courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository     userRepository;
    private final ModuleRepository   moduleRepository;
    private final AuditLogger        auditLogger;

    // ================= PUBLIC =================

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> getPublishedCourses(
            CourseFilter filter, Pageable pageable) {

        return courseRepository.findAll(
                CourseSpecification.withFilters(filter, CourseStatus.PUBLISHED),
                pageable
        ).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseBySlug(String slug) {
        Course course = courseRepository
                .findBySlugWithModules(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "slug", slug));

        if (!course.isPublished()) {
            throw new ResourceNotFoundException("Course", "slug", slug);
        }

        return toFullResponse(course);
    }

    // ================= INSTRUCTOR =================

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getInstructorCourses(
            String instructorEmail, Pageable pageable) {

        User instructor = findUserByEmail(instructorEmail);
        return courseRepository
                .findByInstructor(instructor.getId(), pageable)
                .map(this::toFullResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getInstructorCourseById(UUID courseId,
                                                  String instructorEmail) {
        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        assertInstructorOwns(course, instructorEmail);

        log.debug("Instructor fetching own course: id={} instructor={}",
                courseId, instructorEmail);

        return toFullResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request,
                                       String instructorEmail) {

        User instructor = findUserByEmail(instructorEmail);

        String slug = SlugUtil.makeUnique(
                SlugUtil.toSlug(request.getTitle()),
                s -> courseRepository.existsBySlugAndIsDeletedFalse(s)
        );

        Category category = resolveCategory(request.getCategoryId());

        Course course = Course.builder()
                .title(request.getTitle())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .whatYouWillLearn(request.getWhatYouWillLearn())
                .prerequisites(request.getPrerequisites())
                .targetAudience(request.getTargetAudience())
                .thumbnailUrl(request.getThumbnailUrl())
                .previewVideoUrl(request.getPreviewVideoUrl())
                .category(category)
                .difficultyLevel(request.getDifficultyLevel())
                .language(request.getLanguage() != null
                        ? request.getLanguage() : "English")
                .tags(request.getTags())
                .instructor(instructor)
                .isFree(request.getIsFree() != null
                        ? request.getIsFree() : true)
                .price(request.getPrice())
                .hasCertificate(request.getHasCertificate() != null
                        ? request.getHasCertificate() : true)
                .certificateThreshold(request.getCertificateThreshold() != null
                        ? request.getCertificateThreshold() : 80)
                .status(CourseStatus.DRAFT)
                .build();

        course = courseRepository.save(course);

        log.info("Course created: id={} title='{}' instructor={}",
                course.getId(), course.getTitle(), instructorEmail);
        auditLogger.courseCreated(course.getTitle(), instructorEmail);

        return toFullResponse(
                courseRepository.findByIdWithModules(course.getId())
                        .orElse(course)
        );
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(UUID courseId,
                                       CourseRequest request,
                                       String instructorEmail) {

        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        assertInstructorOwns(course, instructorEmail);

        // Only DRAFT or REJECTED courses can be fully edited
        if (course.getStatus() == CourseStatus.PUBLISHED ||
                course.getStatus() == CourseStatus.PENDING) {
            throw new BadRequestException(
                    "Published or pending courses cannot be edited. " +
                            "Archive it first or wait for rejection.");
        }

        // ✅ Update ALL fields from request — null-safe
        course.setTitle(request.getTitle());

        if (request.getShortDescription() != null)
            course.setShortDescription(request.getShortDescription());
        if (request.getDescription() != null)
            course.setDescription(request.getDescription());
        if (request.getWhatYouWillLearn() != null)
            course.setWhatYouWillLearn(request.getWhatYouWillLearn());
        if (request.getPrerequisites() != null)
            course.setPrerequisites(request.getPrerequisites());
        if (request.getTargetAudience() != null)
            course.setTargetAudience(request.getTargetAudience());
        if (request.getThumbnailUrl() != null)
            course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getPreviewVideoUrl() != null)
            course.setPreviewVideoUrl(request.getPreviewVideoUrl());
        if (request.getCategoryId() != null)
            course.setCategory(resolveCategory(request.getCategoryId()));
        if (request.getDifficultyLevel() != null)
            course.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getLanguage() != null)
            course.setLanguage(request.getLanguage());
        if (request.getTags() != null)
            course.setTags(request.getTags());
        if (request.getIsFree() != null)
            course.setIsFree(request.getIsFree());
        if (request.getPrice() != null)
            course.setPrice(request.getPrice());
        if (request.getHasCertificate() != null)
            course.setHasCertificate(request.getHasCertificate());
        if (request.getCertificateThreshold() != null)
            course.setCertificateThreshold(request.getCertificateThreshold());

        // Regenerate slug if title changed
        String newSlug = SlugUtil.toSlug(request.getTitle());
        if (!course.getSlug().startsWith(newSlug)) {
            course.setSlug(SlugUtil.makeUnique(
                    newSlug,
                    s -> !s.equals(course.getSlug()) &&
                            courseRepository.existsBySlugAndIsDeletedFalse(s)
            ));
        }

        courseRepository.save(course);

        log.info("Course updated: id={} title='{}' instructor={}",
                courseId, course.getTitle(), instructorEmail);

        return toFullResponse(
                courseRepository.findByIdWithModules(courseId)
                        .orElse(course)
        );
    }

    @Override
    @Transactional
    public CourseResponse submitForReview(UUID courseId,
                                          String instructorEmail) {

        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        assertInstructorOwns(course, instructorEmail);

        if (course.getStatus() != CourseStatus.DRAFT &&
                course.getStatus() != CourseStatus.REJECTED) {
            throw new BadRequestException(
                    "Only DRAFT or REJECTED courses can be submitted for review.");
        }

        if (course.getModules() == null || course.getModules().isEmpty()) {
            throw new BadRequestException(
                    "Course must have at least one module before submission.");
        }

        course.setStatus(CourseStatus.PENDING);
        course.setRejectionReason(null); // clear previous rejection reason

        auditLogger.courseSubmitted(course.getTitle(), instructorEmail);

        log.info("Course submitted for review: id={} instructor={}",
                courseId, instructorEmail);

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public void deleteCourse(UUID courseId, String instructorEmail) {

        Course course = findCourseOrThrow(courseId);
        assertInstructorOwns(course, instructorEmail);

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Published courses cannot be deleted. Archive it first.");
        }

        if (course.getEnrolledCount() > 0) {
            throw new BadRequestException(
                    "Cannot delete course with enrolled students.");
        }

        course.softDelete();
        courseRepository.save(course);

        log.info("Course soft-deleted: id={} title='{}' instructor={}",
                courseId, course.getTitle(), instructorEmail);
    }

    // ================= ADMIN =================

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> getAllCoursesForAdmin(
            CourseStatus status, Pageable pageable) {

        return courseRepository.findAllForAdmin(status, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getAdminCourseById(UUID courseId) {

        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        log.debug("Admin fetching course: id={}", courseId);

        return toFullResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse approveCourse(UUID courseId, String adminEmail) {

        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        findUserByEmail(adminEmail);

        if (course.getStatus() != CourseStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING courses can be approved.");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        course.setRejectionReason(null);

        auditLogger.courseApproved(course.getTitle(), adminEmail);

        log.info("Course approved: id={} title='{}' admin={}",
                courseId, course.getTitle(), adminEmail);

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse rejectCourse(UUID courseId,
                                       CourseRejectionRequest request,
                                       String adminEmail) {

        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        findUserByEmail(adminEmail);

        if (course.getStatus() != CourseStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING courses can be rejected.");
        }

        course.setStatus(CourseStatus.REJECTED);
        course.setRejectionReason(request.getReason());

        auditLogger.courseRejected(
                course.getTitle(), adminEmail, request.getReason());

        log.info("Course rejected: id={} admin={} reason={}",
                courseId, adminEmail, request.getReason());

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse archiveCourse(UUID courseId) {

        Course course = courseRepository
                .findByIdWithModules(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Only PUBLISHED courses can be archived.");
        }

        course.setStatus(CourseStatus.ARCHIVED);

        log.info("Course archived: id={} title='{}'",
                courseId, course.getTitle());

        return toFullResponse(courseRepository.save(course));
    }

    // ================= INTERNAL =================

    @Override
    @Transactional
    public void recalculateStats(UUID courseId) {
        int modules = moduleRepository.countByCourseId(courseId);
        courseRepository.updateStats(courseId, modules, 0, 0L);
    }

    @Override
    public Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", id));
    }

    // ================= HELPERS =================

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "email", email));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category", "id", categoryId));
    }

    private void assertInstructorOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            log.warn("Ownership check failed — email={} courseId={}",
                    email, course.getId());
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    // ================= MAPPERS =================

    private CourseSummary toSummary(Course c) {
        return CourseSummary.builder()
                .id(c.getId())
                .title(c.getTitle())
                .slug(c.getSlug())
                .shortDescription(c.getShortDescription())
                .thumbnailUrl(c.getThumbnailUrl())
                .categoryName(c.getCategory() != null
                        ? c.getCategory().getName() : null)
                .difficultyLevel(c.getDifficultyLevel())
                .language(c.getLanguage())
                .instructorName(c.getInstructor().getFullName())
                .isFree(c.getIsFree())
                .price(c.getPrice())
                .status(c.getStatus())
                .totalModules(c.getTotalModules())
                .totalLessons(c.getTotalLessons())
                .totalDurationSeconds(c.getTotalDurationSeconds())
                .enrolledCount(c.getEnrolledCount())
                .averageRating(c.getAverageRating())
                .ratingCount(c.getRatingCount())
                .build();
    }

    private CourseResponse toFullResponse(Course c) {
        List<ModuleResponse> moduleResponses = c.getModules().stream()
                .filter(m -> !m.getIsDeleted())
                .sorted(Comparator.comparingInt(Module::getDisplayOrder))
                .map(m -> {
                    List<LessonSummary> lessonSummaries = m.getLessons().stream()
                            .filter(l -> !l.getIsDeleted())
                            .sorted(Comparator.comparingInt(
                                    Lesson::getDisplayOrder))
                            .map(l -> LessonSummary.builder()
                                    .id(l.getId())
                                    .title(l.getTitle())
                                    .lessonType(l.getLessonType())
                                    .displayOrder(l.getDisplayOrder())
                                    .videoDurationSeconds(
                                            l.getVideoDurationSeconds())
                                    .isPreview(l.getIsPreview())
                                    .isCompleted(false)
                                    .build())
                            .collect(Collectors.toList());

                    return ModuleResponse.builder()
                            .id(m.getId())
                            .courseId(c.getId())
                            .title(m.getTitle())
                            .description(m.getDescription())
                            .displayOrder(m.getDisplayOrder())
                            .isPublished(m.getIsPublished())
                            .isLocked(m.getIsLocked())
                            .totalLessons(m.getTotalLessons())
                            .totalDurationSeconds(m.getTotalDurationSeconds())
                            .lessons(lessonSummaries)
                            .build();
                })
                .collect(Collectors.toList());

        return CourseResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .slug(c.getSlug())
                .shortDescription(c.getShortDescription())
                .description(c.getDescription())
                .whatYouWillLearn(c.getWhatYouWillLearn())
                .prerequisites(c.getPrerequisites())
                .targetAudience(c.getTargetAudience())
                .thumbnailUrl(c.getThumbnailUrl())
                .previewVideoUrl(c.getPreviewVideoUrl())
                .categoryId(c.getCategory() != null
                        ? c.getCategory().getId() : null)
                .categoryName(c.getCategory() != null
                        ? c.getCategory().getName() : null)
                .difficultyLevel(c.getDifficultyLevel())
                .language(c.getLanguage())
                .tags(c.getTags())
                .instructorId(c.getInstructor().getId())
                .instructorName(c.getInstructor().getFullName())
                .instructorAvatar(c.getInstructor().getProfilePictureUrl())
                .isFree(c.getIsFree())
                .price(c.getPrice())
                .status(c.getStatus())
                .publishedAt(c.getPublishedAt())
                .totalModules(c.getTotalModules())
                .totalLessons(c.getTotalLessons())
                .totalDurationSeconds(c.getTotalDurationSeconds())
                .enrolledCount(c.getEnrolledCount())
                .averageRating(c.getAverageRating())
                .ratingCount(c.getRatingCount())
                .hasCertificate(c.getHasCertificate())
                .certificateThreshold(c.getCertificateThreshold())
                .modules(moduleResponses)
                .build();
    }

    private InstructorCourseView toInstructorView(Course c) {
        return InstructorCourseView.builder()
                .id(c.getId())
                .title(c.getTitle())
                .slug(c.getSlug())
                .thumbnailUrl(c.getThumbnailUrl())
                .status(c.getStatus())
                .rejectionReason(c.getRejectionReason())
                .enrolledCount(c.getEnrolledCount())
                .completionCount(c.getCompletionCount())
                .averageRating(c.getAverageRating())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}