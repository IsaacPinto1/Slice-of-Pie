package com.isaac.sliceofpie.thesis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThesisRepository extends JpaRepository<Thesis, Long> {

    Optional<Thesis> findByUserIdAndInstrumentId(Long userId, Long instrumentId);
}