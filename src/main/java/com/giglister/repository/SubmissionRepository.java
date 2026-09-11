package com.giglister.repository;

import com.giglister.domain.Submission;
import com.giglister.domain.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStatusOrderBySubmittedAtDesc(SubmissionStatus status);

    List<Submission> findAllByOrderBySubmittedAtDesc();
}
