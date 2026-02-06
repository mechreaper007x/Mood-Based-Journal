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
     * Uses native query for random selection (PostgreSQL).
     * 
     * SECURITY NOTE: This query uses no user-supplied parameters and is therefore
     * immune to SQL injection attacks. If parameters are ever added to this query,
     * you MUST use parameterized queries (`:param`) - NEVER string concatenation.
     * 
     * @see <a href="https://owasp.org/www-community/attacks/SQL_Injection">OWASP
     *      SQL Injection</a>
     */
    @Query(value = "SELECT * FROM cached_question_set ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<CachedQuestionSet> findRandom();

    long count();
}
