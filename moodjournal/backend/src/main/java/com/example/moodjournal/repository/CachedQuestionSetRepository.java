package com.example.moodjournal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.moodjournal.model.CachedQuestionSet;

@Repository
public interface CachedQuestionSetRepository extends JpaRepository<CachedQuestionSet, Long> {

    /**
     * Get a random cached question set.
     * Uses native query for random selection.
     */
    @Query(value = "SELECT * FROM cached_question_set ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<CachedQuestionSet> findRandom();

    long count();
}
