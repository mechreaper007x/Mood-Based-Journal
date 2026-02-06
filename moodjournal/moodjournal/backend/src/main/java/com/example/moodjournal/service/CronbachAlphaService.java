package com.example.moodjournal.service;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Calculates Cronbach's Alpha for psychometric assessments.
 * 
 * Cronbach's Alpha measures internal consistency (reliability) of a scale.
 * - α ≥ 0.9: Excellent
 * - 0.8 ≤ α < 0.9: Good
 * - 0.7 ≤ α < 0.8: Acceptable
 * - 0.6 ≤ α < 0.7: Questionable
 * - 0.5 ≤ α < 0.6: Poor
 * - α < 0.5: Unacceptable
 * 
 * For psychological assessments like PHQ-9, expected α is 0.86-0.89.
 */
@Service
public class CronbachAlphaService {

    private static final Logger log = LoggerFactory.getLogger(CronbachAlphaService.class);

    /**
     * Calculate Cronbach's Alpha for a set of item responses.
     * 
     * Formula: α = (k / (k-1)) * (1 - Σ(σi²) / σt²)
     * Where:
     * k = number of items
     * σi² = variance of item i
     * σt² = variance of total scores
     * 
     * @param itemResponses List of item scores per respondent, e.g., [[1,2,3],
     *                      [2,2,2], ...]
     * @return Cronbach's Alpha (0 to 1), or -1 if insufficient data
     */
    public double calculate(List<List<Integer>> itemResponses) {
        if (itemResponses == null || itemResponses.size() < 2) {
            log.warn("Insufficient data for Cronbach's Alpha (need at least 2 respondents)");
            return -1.0;
        }

        int numRespondents = itemResponses.size();
        int numItems = itemResponses.get(0).size();

        if (numItems < 2) {
            log.warn("Insufficient items for Cronbach's Alpha (need at least 2 items)");
            return -1.0;
        }

        // Calculate variance for each item
        double sumItemVariances = 0.0;
        for (int i = 0; i < numItems; i++) {
            double[] itemScores = new double[numRespondents];
            for (int r = 0; r < numRespondents; r++) {
                itemScores[r] = itemResponses.get(r).get(i);
            }
            sumItemVariances += variance(itemScores);
        }

        // Calculate variance of total scores
        double[] totalScores = new double[numRespondents];
        for (int r = 0; r < numRespondents; r++) {
            totalScores[r] = itemResponses.get(r).stream().mapToInt(Integer::intValue).sum();
        }
        double totalVariance = variance(totalScores);

        if (totalVariance == 0) {
            log.warn("Zero variance in total scores - all respondents answered identically");
            return 0.0;
        }

        // Cronbach's Alpha formula
        double alpha = ((double) numItems / (numItems - 1)) * (1 - (sumItemVariances / totalVariance));

        log.debug("Cronbach's Alpha calculated: {} (k={}, n={})", alpha, numItems, numRespondents);
        return Math.max(0, Math.min(1, alpha)); // Clamp to [0, 1]
    }

    /**
     * Interpret Cronbach's Alpha value.
     */
    public String interpret(double alpha) {
        if (alpha < 0)
            return "INSUFFICIENT_DATA";
        if (alpha >= 0.9)
            return "EXCELLENT";
        if (alpha >= 0.8)
            return "GOOD";
        if (alpha >= 0.7)
            return "ACCEPTABLE";
        if (alpha >= 0.6)
            return "QUESTIONABLE";
        if (alpha >= 0.5)
            return "POOR";
        return "UNACCEPTABLE";
    }

    /**
     * Calculate variance of an array.
     */
    private double variance(double[] values) {
        if (values.length == 0)
            return 0.0;

        double mean = 0.0;
        for (double v : values)
            mean += v;
        mean /= values.length;

        double sumSquaredDiff = 0.0;
        for (double v : values) {
            sumSquaredDiff += Math.pow(v - mean, 2);
        }

        return sumSquaredDiff / values.length; // Population variance
    }
}
