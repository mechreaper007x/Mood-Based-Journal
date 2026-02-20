package com.example.moodjournal.service;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;














@Service
public class CronbachAlphaService {

    private static final Logger log = LoggerFactory.getLogger(CronbachAlphaService.class);

    












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

        
        double sumItemVariances = 0.0;
        for (int i = 0; i < numItems; i++) {
            double[] itemScores = new double[numRespondents];
            for (int r = 0; r < numRespondents; r++) {
                itemScores[r] = itemResponses.get(r).get(i);
            }
            sumItemVariances += variance(itemScores);
        }

        
        double[] totalScores = new double[numRespondents];
        for (int r = 0; r < numRespondents; r++) {
            totalScores[r] = itemResponses.get(r).stream().mapToInt(Integer::intValue).sum();
        }
        double totalVariance = variance(totalScores);

        if (totalVariance == 0) {
            log.warn("Zero variance in total scores - all respondents answered identically");
            return 0.0;
        }

        
        double alpha = ((double) numItems / (numItems - 1)) * (1 - (sumItemVariances / totalVariance));

        log.debug("Cronbach's Alpha calculated: {} (k={}, n={})", alpha, numItems, numRespondents);
        return Math.max(0, Math.min(1, alpha)); 
    }

    


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

        return sumSquaredDiff / values.length; 
    }
}
