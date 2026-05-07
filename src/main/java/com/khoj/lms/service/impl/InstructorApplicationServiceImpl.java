package com.khoj.lms.service.impl;

import com.khoj.lms.dto.instructor.InstructorApplicationDtos.*;
import com.khoj.lms.entity.*;
import com.khoj.lms.enums.ApplicationStatus;
import com.khoj.lms.enums.RoleName;
import com.khoj.lms.exception.*;
import com.khoj.lms.repository.*;
import com.khoj.lms.service.InstructorApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorApplicationServiceImpl implements InstructorApplicationService {

    private final InstructorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // ── Apply ─────────────────────────────────

    @Override
    @Transactional
    public ApplicationResponse apply(ApplyRequest request, String applicantEmail) {

        User user = userRepository.findByEmailAndIsDeletedFalse(applicantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", applicantEmail));

        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.INSTRUCTOR)) {
            throw new BadRequestException("You are already an instructor.");
        }

        if (applicationRepository.existsByUserIdAndStatusAndIsDeletedFalse(
                user.getId(), ApplicationStatus.PENDING)) {
            throw new BadRequestException("You already have a pending application.");
        }

        InstructorApplication application = applicationRepository
                .findByUserIdAndIsDeletedFalse(user.getId())
                .orElse(new InstructorApplication());

        application.setUser(user);
        application.setExpertise(request.getExpertise());
        application.setMotivation(request.getMotivation());
        application.setLinkedinUrl(request.getLinkedinUrl());
        application.setPortfolioUrl(request.getPortfolioUrl());
        application.setExperience(request.getExperience());
        application.setQualifications(request.getQualifications());
        application.setStatus(ApplicationStatus.PENDING);
        application.setRejectionReason(null);
        application.setReviewedAt(null);
        application.setReviewedBy(null);

        applicationRepository.save(application);

        log.info("Instructor application submitted by: {}", applicantEmail);

        return mapToResponse(application);
    }

    // ── Admin: List Applications ──────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationResponse> listApplications(ApplicationStatus status, Pageable pageable) {

        Page<InstructorApplication> page = (status != null)
                ? applicationRepository.findByStatusAndIsDeletedFalse(status, pageable)
                : applicationRepository.findAll(pageable);

        return page.map(this::mapToResponse);
    }

    // ── Admin: Approve ────────────────────────

    @Override
    @Transactional
    public ApplicationResponse approve(UUID applicationId, String adminEmail) {

        InstructorApplication application = applicationRepository.findByIdWithUser(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new BadRequestException("Already approved.");
        }

        User admin = userRepository.findByEmailAndIsDeletedFalse(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        Role instructorRole = roleRepository.findByName(RoleName.INSTRUCTOR)
                .orElseThrow(() -> new IllegalStateException("INSTRUCTOR role not found"));

        User applicant = application.getUser();
        applicant.addRole(instructorRole);
        userRepository.save(applicant);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(null);
        applicationRepository.save(application);

        log.info("Instructor APPROVED: {} by {}", applicant.getEmail(), adminEmail);

        return mapToResponse(application);
    }

    // ── Admin: Reject ─────────────────────────

    @Override
    @Transactional
    public ApplicationResponse reject(UUID applicationId, RejectRequest request, String adminEmail) {

        InstructorApplication application = applicationRepository.findByIdWithUser(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId.toString()));

        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new BadRequestException("Already rejected.");
        }

        User admin = userRepository.findByEmailAndIsDeletedFalse(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(request.getReason());
        applicationRepository.save(application);

        log.info("Instructor REJECTED: {} by {}", application.getUser().getEmail(), adminEmail);

        return mapToResponse(application);
    }

    // ── Get My Application ───────────────────

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getMyApplication(String email) {

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        InstructorApplication application = applicationRepository
                .findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No application found"));

        return mapToResponse(application);
    }

    // ── Mapper ───────────────────────────────

    private ApplicationResponse mapToResponse(InstructorApplication a) {
        return ApplicationResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .applicantName(a.getUser().getFullName())
                .applicantEmail(a.getUser().getEmail())
                .expertise(a.getExpertise())
                .motivation(a.getMotivation())
                .linkedinUrl(a.getLinkedinUrl())
                .portfolioUrl(a.getPortfolioUrl())
                .experience(a.getExperience())
                .qualifications(a.getQualifications())
                .status(a.getStatus())
                .rejectionReason(a.getRejectionReason())
                .reviewedAt(a.getReviewedAt())
                .appliedAt(a.getCreatedAt())
                .build();
    }
}