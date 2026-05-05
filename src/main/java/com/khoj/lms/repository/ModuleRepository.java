package com.khoj.lms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.khoj.lms.entity.Module;

public interface ModuleRepository extends JpaRepository<Module, Long> {
}
