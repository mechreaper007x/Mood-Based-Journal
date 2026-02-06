package com.example.moodjournal.repository;

import com.example.moodjournal.model.MLModelParameters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MLModelParametersRepository extends JpaRepository<MLModelParameters, UUID> {

    Optional<MLModelParameters> findByModelTypeAndIsActiveTrue(String modelType);

    List<MLModelParameters> findByModelTypeOrderByModelVersionDesc(String modelType);

    Optional<MLModelParameters> findTopByModelTypeOrderByTrainedAtDesc(String modelType);

    List<MLModelParameters> findByIsActiveTrue();
}
