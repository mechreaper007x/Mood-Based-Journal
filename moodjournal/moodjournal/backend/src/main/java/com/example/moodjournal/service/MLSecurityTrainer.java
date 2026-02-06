package com.example.moodjournal.service;

import com.example.moodjournal.ml.GeneticThresholdEvolver;
import com.example.moodjournal.ml.GradientDescentClassifier;
import com.example.moodjournal.model.JournalEntry;
import com.example.moodjournal.model.MLModelParameters;
import com.example.moodjournal.model.SecurityEvent;
import com.example.moodjournal.repository.JournalEntryRepository;
import com.example.moodjournal.repository.MLModelParametersRepository;
import com.example.moodjournal.repository.SecurityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ML Security Trainer - The "Gym" where the defense system learns.
 * Runs periodic training using:
 * 1. Gradient Descent on attack/legit classification
 * 2. Genetic Algorithm on threshold optimization
 * 
 * This is the heart of the Neuroevolution System.
 */
@Service
public class MLSecurityTrainer {

    private static final Logger log = LoggerFactory.getLogger(MLSecurityTrainer.class);

    private final SecurityEventRepository securityEventRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final MLModelParametersRepository modelRepository;
    private final GradientDescentClassifier gdClassifier;
    private final GeneticThresholdEvolver gaEvolver;
    private final com.example.moodjournal.ml.PatternDiscoveryEngine patternEngine;
    private final com.example.moodjournal.repository.SecurityRuleRepository securityRuleRepository;

    private static final int MIN_TRAINING_SAMPLES = 10;
    private static final int GD_EPOCHS = 500;
    private static final int GA_GENERATIONS = 50;

    public MLSecurityTrainer(
            SecurityEventRepository securityEventRepository,
            JournalEntryRepository journalEntryRepository,
            MLModelParametersRepository modelRepository,
            GradientDescentClassifier gdClassifier,
            GeneticThresholdEvolver gaEvolver,
            com.example.moodjournal.ml.PatternDiscoveryEngine patternEngine,
            com.example.moodjournal.repository.SecurityRuleRepository securityRuleRepository) {
        this.securityEventRepository = securityEventRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.modelRepository = modelRepository;
        this.gdClassifier = gdClassifier;
        this.gaEvolver = gaEvolver;
        this.patternEngine = patternEngine;
        this.securityRuleRepository = securityRuleRepository;
    }

    /**
     * Load the currently active model on startup.
     */
    @jakarta.annotation.PostConstruct
    public void loadActiveModel() {
        Optional<MLModelParameters> activeGD = modelRepository.findByModelTypeAndIsActiveTrue("GRADIENT_DESCENT");
        activeGD.ifPresent(model -> {
            gdClassifier.loadWeights(model.getWeightsArray(), model.getBias());
            log.info("[ML] Loaded active Gradient Descent model v{}", model.getModelVersion());
        });
    }

    /**
     * Run training every 4 hours (or on demand).
     * Learns from blocked attacks and legitimate journal entries.
     */
    @Scheduled(fixedDelay = 14400000) // 4 hours
    @Transactional
    public void runTraining() {
        log.info(">>> [ML TRAINING] Starting Neuroevolution Cycle...");

        // 1. Gather training data
        List<String> attackSamples = gatherAttackSamples();
        List<String> legitSamples = gatherLegitSamples();

        if (attackSamples.size() < MIN_TRAINING_SAMPLES) {
            log.info(">>> [ML TRAINING] Insufficient attack data ({} < {}). Skipping.",
                    attackSamples.size(), MIN_TRAINING_SAMPLES);
            return;
        }

        log.info(">>> [ML TRAINING] Training data: {} attacks, {} legit",
                attackSamples.size(), legitSamples.size());

        // 2. Train Gradient Descent Classifier
        log.info(">>> [ML TRAINING] Phase 1: Gradient Descent Training...");
        Map<String, Double> gdMetrics = gdClassifier.train(attackSamples, legitSamples, GD_EPOCHS);

        // 3. Extract features for GA
        List<double[]> attackFeatures = attackSamples.stream()
                .map(gdClassifier::extractFeatures)
                .collect(Collectors.toList());
        List<double[]> legitFeatures = legitSamples.stream()
                .map(gdClassifier::extractFeatures)
                .collect(Collectors.toList());

        // 4. Run Genetic Algorithm to evolve thresholds
        log.info(">>> [ML TRAINING] Phase 2: Genetic Algorithm Evolution...");
        var fitnessFunction = gaEvolver.createFitnessFunction(attackFeatures, legitFeatures, gdClassifier);
        GeneticThresholdEvolver.Chromosome bestChromosome = gaEvolver.evolve(fitnessFunction, GA_GENERATIONS);

        // 5. Phase 3: Auto-Immune Pattern Discovery (Dynamic Rules)
        log.info(">>> [ML TRAINING] Phase 3: Auto-Immune System (Pattern Discovery)...");
        List<String> newPatterns = patternEngine.discoverNewPatterns(attackSamples, legitSamples);
        addNewDynamicRules(newPatterns);

        // 6. Save the new model
        saveTrainedModel(gdMetrics, bestChromosome, attackSamples.size() + legitSamples.size());

        log.info(">>> [ML TRAINING] Neuroevolution Cycle Complete!");
    }

    private void addNewDynamicRules(List<String> patterns) {
        for (String pattern : patterns) {
            // Check if rule already exists (fuzzy check)
            boolean exists = securityRuleRepository.findAll().stream()
                    .anyMatch(r -> r.getPattern().equalsIgnoreCase(pattern));

            if (!exists) {
                com.example.moodjournal.model.SecurityRule rule = new com.example.moodjournal.model.SecurityRule(
                        pattern,
                        "Auto-Immune Generated Rule: " + pattern,
                        true,
                        true // Start in SHADOW MODE for safety
                );
                securityRuleRepository.save(rule);
                log.info("[AUTO-IMMUNE] NEW VACCINE CREATED: Rule '{}'", pattern);
            }
        }
    }

    private List<String> gatherAttackSamples() {
        // Get recent blocked attacks from security events
        List<SecurityEvent> events = securityEventRepository.findTop100ByOrderByTimestampDesc();
        return events.stream()
                .filter(e -> e.getRiskScore() != null && e.getRiskScore() >= 7.0) // High risk = likely attack
                .map(SecurityEvent::getContent)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> gatherLegitSamples() {
        // Get recent legitimate journal entries (low risk score)
        // For now, sample recent entries and assume they're legitimate
        List<JournalEntry> entries = journalEntryRepository.findTop100ByOrderByCreatedAtDesc();
        return entries.stream()
                .map(JournalEntry::getContent)
                .filter(Objects::nonNull)
                .filter(content -> content.length() > 20) // Non-trivial entries
                .limit(100)
                .collect(Collectors.toList());
    }

    @Transactional
    void saveTrainedModel(Map<String, Double> gdMetrics,
            GeneticThresholdEvolver.Chromosome gaResult,
            int trainingSize) {
        // Deactivate previous models
        List<MLModelParameters> activeModels = modelRepository.findByIsActiveTrue();
        for (MLModelParameters model : activeModels) {
            model.setActive(false);
            modelRepository.save(model);
        }

        // Get next version number
        List<MLModelParameters> previousModels = modelRepository
                .findByModelTypeOrderByModelVersionDesc("GRADIENT_DESCENT");
        int nextVersion = previousModels.isEmpty() ? 1 : previousModels.get(0).getModelVersion() + 1;

        // Create new model
        MLModelParameters newModel = new MLModelParameters();
        newModel.setModelType("GRADIENT_DESCENT");
        newModel.setModelVersion(nextVersion);

        // Set GD weights
        newModel.setWeightsFromArray(gdClassifier.getWeights());
        newModel.setBias(gdClassifier.getBias());

        // Set GA thresholds
        newModel.setThresholdsFromArray(gaResult.genes);

        // Set metrics
        newModel.setAccuracy(gdMetrics.getOrDefault("accuracy", 0.0));
        newModel.setPrecision(gdMetrics.getOrDefault("precision", 0.0));
        newModel.setRecall(gdMetrics.getOrDefault("recall", 0.0));
        newModel.setF1Score(gdMetrics.getOrDefault("f1Score", 0.0));
        newModel.setTrainingEpochs(GD_EPOCHS);
        newModel.setTrainingDataSize(trainingSize);
        newModel.setTrainedAt(LocalDateTime.now());
        newModel.setActive(true);

        modelRepository.save(newModel);

        log.info(">>> [ML TRAINING] Saved Model v{}: Accuracy={:.2f}%, F1={:.2f}%",
                nextVersion, newModel.getAccuracy() * 100, newModel.getF1Score() * 100);
    }

    /**
     * Get current ML prediction for an input.
     * Can be used by AISecurityService as Layer 6.
     */
    public double getAttackProbability(String input) {
        return gdClassifier.predict(input);
    }

    /**
     * Force immediate training (for testing/admin).
     */
    public void forceTraining() {
        runTraining();
    }
}
