package com.khoj.lms.service.impl;

import com.khoj.lms.dto.course.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.CourseStatus;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.CourseService;
import com.khoj.lms.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;

    // ================= PUBLIC =================

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> getPublishedCourses(CourseFilter filter, Pageable pageable) {
        return courseRepository.findPublished(
                filter.getSearch(),
                filter.getCategoryId(),
                filter.getDifficulty(),
                filter.getLanguage(),
                filter.getIsFree(),
                pageable
        ).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "slug", slug));

        if (!course.isPublished()) {
            throw new ResourceNotFoundException("Course", "slug", slug);
        }

        return toFullResponse(course);
    }

    // ================= INSTRUCTOR =================

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorCourseView> getInstructorCourses(String instructorEmail, Pageable pageable) {
        User instructor = findUserByEmail(instructorEmail);

        return courseRepository.findByInstructor(instructor.getId(), pageable)
                .map(this::toInstructorView);
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request, String instructorEmail) {
        User instructor = findUserByEmail(instructorEmail);

        String baseSlug = SlugUtil.toSlug(request.getTitle());
        String slug = SlugUtil.makeUnique(
                baseSlug,
                s -> courseRepository.existsBySlugAndIsDeletedFalse(s)
        );

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(c -> !c.getIsDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .category(category)
                .instructor(instructor)
                .status(CourseStatus.DRAFT)
                .isFree(request.getIsFree())
                .build();

        course = courseRepository.save(course);

        log.info("Course created: {}", course.getTitle());

        return toFullResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(UUID courseId, CourseRequest request, String instructorEmail) {
        Course course = findCourseOrThrow(courseId);
        assertInstructorOwns(course, instructorEmail);

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setShortDescription(request.getShortDescription());
        course.setIsFree(request.getIsFree());

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse submitForReview(UUID courseId, String instructorEmail) {
        Course course = findCourseOrThrow(courseId);
        assertInstructorOwns(course, instructorEmail);

        // ✅ Validation from your test file
        if (course.getModules() == null || course.getModules().isEmpty()) {
            throw new BadRequestException("Course must have at least one module before submission.");
        }

        course.setStatus(CourseStatus.PENDING);

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public void deleteCourse(UUID courseId, String instructorEmail) {
        Course course = findCourseOrThrow(courseId);
        assertInstructorOwns(course, instructorEmail);

        // ✅ Validation from your test
        if (course.getEnrolledCount() > 0) {
            throw new BadRequestException("Cannot delete course with enrolled students.");
        }

        course.softDelete();
        courseRepository.save(course);
    }

    // ================= ADMIN =================

    @Override
    @Transactional(readOnly = true)
    public Page<CourseSummary> getAllCoursesForAdmin(CourseStatus status, Pageable pageable) {
        return courseRepository.findAllForAdmin(status, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional
    public CourseResponse approveCourse(UUID courseId, String adminEmail) {
        Course course = findCourseOrThrow(courseId);

        findUserByEmail(adminEmail); // ensure admin exists

        if (course.getStatus() != CourseStatus.PENDING) {
            throw new BadRequestException("Only pending courses can be approved.");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(LocalDateTime.from(Instant.now()));

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse rejectCourse(UUID courseId, CourseRejectionRequest request, String adminEmail) {
        Course course = findCourseOrThrow(courseId);

        findUserByEmail(adminEmail);

        if (course.getStatus() != CourseStatus.PENDING) {
            throw new BadRequestException("Only pending courses can be rejected.");
        }

        course.setStatus(CourseStatus.REJECTED);
        course.setRejectionReason(request.getReason());

        return toFullResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseResponse archiveCourse(UUID courseId) {
        Course course = findCourseOrThrow(courseId);

        course.setStatus(CourseStatus.ARCHIVED);

        return toFullResponse(courseRepository.save(course));
    }

    // ================= INTERNAL =================

    @Override
    @Transactional
    public void recalculateStats(UUID courseId) {
        int modules = moduleRepository.countByCourseId(courseId);

        courseRepository.updateStats(
                courseId,
                modules,
                0,
                0L
        );
    }

    @Override
    public Course findCourseOrThrow(UUID id) {
        return courseRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    // ================= HELPERS =================

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void assertInstructorOwns(Course course, String email) {
        if (!course.getInstructor().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("You do not own this course.");
        }
    }

    // ================= MAPPERS =================

    private CourseSummary toSummary(Course c) {
        return CourseSummary.builder()
                .id(c.getId())
                .title(c.getTitle())
                .slug(c.getSlug())
                .build();
    }

    private CourseResponse toFullResponse(Course c) {
        return CourseResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .slug(c.getSlug())
                .status(c.getStatus())
                .publishedAt(c.getPublishedAt())
                .build();
    }

    private InstructorCourseView toInstructorView(Course c) {
        return InstructorCourseView.builder()
                .id(c.getId())
                .title(c.getTitle())
                .build();
    }
}