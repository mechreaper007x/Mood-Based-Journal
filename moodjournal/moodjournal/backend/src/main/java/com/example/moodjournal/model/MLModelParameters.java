package com.example.moodjournal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;





@Entity
@Table(name = "ml_model_parameters")
public class MLModelParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "model_type")
    private String modelType; 

    @Column(name = "model_version")
    private int modelVersion;

    
    private double weightEntropy;
    private double weightSpecialCharRatio;
    private double weightUppercaseRatio;
    private double weightInjectionKeywordCount;
    private double weightSentenceComplexity;
    private double weightLexicalDiversity;
    private double weightSuspiciousPatternScore;
    private double bias;

    
    private double thresholdEntropy;
    private double thresholdSpecialChar;
    private double thresholdUppercase;
    private double thresholdAnomaly;

    
    private double accuracy;
    private double precision;
    private double recall;
    private double f1Score;
    private int trainingEpochs;
    private int trainingDataSize;

    private LocalDateTime createdAt;
    private LocalDateTime trainedAt;

    @Column(name = "is_active")
    private boolean isActive;

    public MLModelParameters() {
        this.createdAt = LocalDateTime.now();
        this.isActive = false;
        
        this.weightEntropy = Math.random() * 0.5;
        this.weightSpecialCharRatio = Math.random() * 0.5;
        this.weightUppercaseRatio = Math.random() * 0.5;
        this.weightInjectionKeywordCount = Math.random() * 0.5;
        this.weightSentenceComplexity = Math.random() * 0.5;
        this.weightLexicalDiversity = Math.random() * 0.5;
        this.weightSuspiciousPatternScore = Math.random() * 0.5;
        this.bias = 0.0;
    }

    public double[] getWeightsArray() {
        return new double[] {
                weightEntropy,
                weightSpecialCharRatio,
                weightUppercaseRatio,
                weightInjectionKeywordCount,
                weightSentenceComplexity,
                weightLexicalDiversity,
                weightSuspiciousPatternScore
        };
    }

    public void setWeightsFromArray(double[] weights) {
        if (weights.length >= 7) {
            this.weightEntropy = weights[0];
            this.weightSpecialCharRatio = weights[1];
            this.weightUppercaseRatio = weights[2];
            this.weightInjectionKeywordCount = weights[3];
            this.weightSentenceComplexity = weights[4];
            this.weightLexicalDiversity = weights[5];
            this.weightSuspiciousPatternScore = weights[6];
        }
    }

    public double[] getThresholdsArray() {
        return new double[] { thresholdEntropy, thresholdSpecialChar, thresholdUppercase, thresholdAnomaly };
    }

    public void setThresholdsFromArray(double[] thresholds) {
        if (thresholds.length >= 4) {
            this.thresholdEntropy = thresholds[0];
            this.thresholdSpecialChar = thresholds[1];
            this.thresholdUppercase = thresholds[2];
            this.thresholdAnomaly = thresholds[3];
        }
    }

    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public int getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(int modelVersion) {
        this.modelVersion = modelVersion;
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getF1Score() {
        return f1Score;
    }

    public void setF1Score(double f1Score) {
        this.f1Score = f1Score;
    }

    public int getTrainingEpochs() {
        return trainingEpochs;
    }

    public void setTrainingEpochs(int trainingEpochs) {
        this.trainingEpochs = trainingEpochs;
    }

    public int getTrainingDataSize() {
        return trainingDataSize;
    }

    public void setTrainingDataSize(int trainingDataSize) {
        this.trainingDataSize = trainingDataSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getTrainedAt() {
        return trainedAt;
    }

    public void setTrainedAt(LocalDateTime trainedAt) {
        this.trainedAt = trainedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public double getWeightEntropy() {
        return weightEntropy;
    }

    public void setWeightEntropy(double w) {
        this.weightEntropy = w;
    }

    public double getWeightSpecialCharRatio() {
        return weightSpecialCharRatio;
    }

    public void setWeightSpecialCharRatio(double w) {
        this.weightSpecialCharRatio = w;
    }

    public double getWeightUppercaseRatio() {
        return weightUppercaseRatio;
    }

    public void setWeightUppercaseRatio(double w) {
        this.weightUppercaseRatio = w;
    }

    public double getWeightInjectionKeywordCount() {
        return weightInjectionKeywordCount;
    }

    public void setWeightInjectionKeywordCount(double w) {
        this.weightInjectionKeywordCount = w;
    }

    public double getWeightSentenceComplexity() {
        return weightSentenceComplexity;
    }

    public void setWeightSentenceComplexity(double w) {
        this.weightSentenceComplexity = w;
    }

    public double getWeightLexicalDiversity() {
        return weightLexicalDiversity;
    }

    public void setWeightLexicalDiversity(double w) {
        this.weightLexicalDiversity = w;
    }

    public double getWeightSuspiciousPatternScore() {
        return weightSuspiciousPatternScore;
    }

    public void setWeightSuspiciousPatternScore(double w) {
        this.weightSuspiciousPatternScore = w;
    }

    public double getThresholdEntropy() {
        return thresholdEntropy;
    }

    public void setThresholdEntropy(double t) {
        this.thresholdEntropy = t;
    }

    public double getThresholdSpecialChar() {
        return thresholdSpecialChar;
    }

    public void setThresholdSpecialChar(double t) {
        this.thresholdSpecialChar = t;
    }

    public double getThresholdUppercase() {
        return thresholdUppercase;
    }

    public void setThresholdUppercase(double t) {
        this.thresholdUppercase = t;
    }

    public double getThresholdAnomaly() {
        return thresholdAnomaly;
    }

    public void setThresholdAnomaly(double t) {
        this.thresholdAnomaly = t;
    }
}
