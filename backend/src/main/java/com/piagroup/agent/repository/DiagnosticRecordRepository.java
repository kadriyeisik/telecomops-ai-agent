package com.piagroup.agent.repository;

import com.piagroup.agent.model.DiagnosticRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosticRecordRepository extends JpaRepository<DiagnosticRecord, Long> {

    /** Most-recent cases first, capped to last N records via service layer. */
    List<DiagnosticRecord> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
