package com.khoj.lms.specification;

import com.khoj.lms.dto.course.CourseFilter;
import com.khoj.lms.entity.Course;
import com.khoj.lms.enums.CourseStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CourseSpecification {

    private CourseSpecification() {}

    public static Specification<Course> withFilters(
            CourseFilter filter,
            CourseStatus status
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // not deleted
            predicates.add(
                    cb.isFalse(root.get("isDeleted"))
            );

            // status
            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            // search
            if (filter.getSearch() != null &&
                    !filter.getSearch().trim().isEmpty()) {

                predicates.add(
                        cb.like(
                                cb.lower(root.get("title")),
                                "%" + filter.getSearch().toLowerCase() + "%"
                        )
                );
            }

            // category
            if (filter.getCategoryId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("category").get("id"),
                                filter.getCategoryId()
                        )
                );
            }

            // difficulty
            if (filter.getDifficulty() != null) {
                predicates.add(
                        cb.equal(
                                root.get("difficultyLevel"),
                                filter.getDifficulty()
                        )
                );
            }

            // language
            if (filter.getLanguage() != null &&
                    !filter.getLanguage().trim().isEmpty()) {

                predicates.add(
                        cb.equal(
                                cb.lower(root.get("language")),
                                filter.getLanguage().toLowerCase()
                        )
                );
            }

            // free/paid
            if (filter.getIsFree() != null) {
                predicates.add(
                        cb.equal(
                                root.get("isFree"),
                                filter.getIsFree()
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}