package com.khoj.lms.service;

import com.khoj.lms.dto.instructor.InstructorApplicationDtos;
import com.khoj.lms.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InstructorApplicationService {

    InstructorApplicationDtos.ApplicationResponse apply(InstructorApplicationDtos.ApplyRequest request, String applicantEmail);

    Page<InstructorApplicationDtos.ApplicationResponse> listApplications(ApplicationStatus status, Pageable pageable);

    InstructorApplicationDtos.ApplicationResponse approve(UUID applicationId, String adminEmail);

    InstructorApplicationDtos.ApplicationResponse reject(UUID applicationId, InstructorApplicationDtos.RejectRequest request, String adminEmail);

    InstructorApplicationDtos.ApplicationResponse getMyApplication(String email);
}