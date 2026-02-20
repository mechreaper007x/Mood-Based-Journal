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
import java.util.regex.Pattern;

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

    private static final int MIN_ATTACK_SAMPLES = 15;
    private static final int MIN_LEGIT_SAMPLES = 30;
    private static final int GD_EPOCHS = 500;
    private static final int GA_GENERATIONS = 50;
    private static final int MAX_ATTACK_SAMPLES = 250;
    private static final int MAX_LEGIT_SAMPLES = 400;
    private static final double VALIDATION_SPLIT = 0.2;
    private static final int MIN_VALIDATION_PER_CLASS = 5;
    private static final double MIN_VALIDATION_PRECISION = 0.90;
    private static final double MIN_VALIDATION_RECALL = 0.60;
    private static final double MIN_VALIDATION_F1 = 0.72;
    private static final double MAX_VALIDATION_FALSE_POSITIVE_RATE = 0.12;
    private static final double MIN_DECISION_THRESHOLD = 0.55;
    private static final double MAX_DECISION_THRESHOLD = 0.90;
    private static final Pattern SUSPICIOUS_LEGIT_PATTERN = Pattern.compile(
            "(ignore\\s+previous\\s+instructions|system\\s*override|developer\\s*mode|\\byou\\s+are\\s+dan\\b|roleplay\\s+as|jailbreak|bypass\\s+safety|prompt\\s+injection|just\\s+reply\\s+with\\s+the\\s+word)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

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
        List<String> attackSamples = new ArrayList<>(gatherAttackSamples());
        List<String> legitSamples = new ArrayList<>(gatherLegitSamples());

        if (attackSamples.size() < MIN_ATTACK_SAMPLES || legitSamples.size() < MIN_LEGIT_SAMPLES) {
            log.info(">>> [ML TRAINING] Insufficient curated data. attacks={} (min {}), legit={} (min {}). Skipping.",
                    attackSamples.size(), MIN_ATTACK_SAMPLES, legitSamples.size(), MIN_LEGIT_SAMPLES);
            return;
        }

        // Prevent heavy class imbalance from poisoning the learner.
        Collections.shuffle(attackSamples, new Random(42));
        Collections.shuffle(legitSamples, new Random(84));
        if (attackSamples.size() > MAX_ATTACK_SAMPLES) {
            attackSamples = new ArrayList<>(attackSamples.subList(0, MAX_ATTACK_SAMPLES));
        }
        int maxLegitForBalance = Math.min(MAX_LEGIT_SAMPLES, attackSamples.size() * 3);
        if (legitSamples.size() > maxLegitForBalance) {
            legitSamples = new ArrayList<>(legitSamples.subList(0, maxLegitForBalance));
        }

        DataSplit split = splitData(attackSamples, legitSamples);
        if (split.validationAttacks().size() < MIN_VALIDATION_PER_CLASS
                || split.validationLegit().size() < MIN_VALIDATION_PER_CLASS) {
            log.warn(">>> [ML TRAINING] Validation split too small (attacks={}, legit={}). Skipping.",
                    split.validationAttacks().size(), split.validationLegit().size());
            return;
        }

        log.info(">>> [ML TRAINING] Training data: {} attacks, {} legit",
                split.trainingAttacks().size(), split.trainingLegit().size());

        // Snapshot existing model in case candidate fails validation.
        double[] previousWeights = gdClassifier.getWeights();
        double previousBias = gdClassifier.getBias();

        // 2. Train Gradient Descent Classifier
        log.info(">>> [ML TRAINING] Phase 1: Gradient Descent Training...");
        Map<String, Double> gdMetrics = gdClassifier.train(split.trainingAttacks(), split.trainingLegit(), GD_EPOCHS);

        // 3. Extract features for GA from training split only.
        List<double[]> attackFeatures = split.trainingAttacks().stream()
                .map(gdClassifier::extractFeatures)
                .collect(Collectors.toList());
        List<double[]> legitFeatures = split.trainingLegit().stream()
                .map(gdClassifier::extractFeatures)
                .collect(Collectors.toList());

        // 4. Run Genetic Algorithm to evolve thresholds
        log.info(">>> [ML TRAINING] Phase 2: Genetic Algorithm Evolution...");
        var fitnessFunction = gaEvolver.createFitnessFunction(attackFeatures, legitFeatures, gdClassifier);
        GeneticThresholdEvolver.Chromosome bestChromosome = gaEvolver.evolve(fitnessFunction, GA_GENERATIONS);
        bestChromosome.genes[3] = clamp(bestChromosome.genes[3], MIN_DECISION_THRESHOLD, MAX_DECISION_THRESHOLD);

        // 5. Validate candidate before promotion to resist model poisoning.
        ValidationMetrics validation = evaluateCandidate(split.validationAttacks(), split.validationLegit(),
                bestChromosome.genes[3]);
        log.info(">>> [ML TRAINING] Validation: precision={}% recall={}% f1={} fpr={} (threshold={})",
                String.format("%.2f", validation.precision() * 100),
                String.format("%.2f", validation.recall() * 100),
                String.format("%.2f", validation.f1()),
                String.format("%.2f", validation.falsePositiveRate()),
                String.format("%.2f", bestChromosome.genes[3]));

        if (!isPromotionSafe(validation)) {
            gdClassifier.loadWeights(previousWeights, previousBias);
            log.warn(">>> [ML TRAINING] Candidate model rejected by anti-poisoning gate. Previous model restored.");
            return;
        }

        // 6. Phase 3: Auto-Immune Pattern Discovery (Dynamic Rules)
        log.info(">>> [ML TRAINING] Phase 3: Auto-Immune System (Pattern Discovery)...");
        List<String> newPatterns = patternEngine.discoverNewPatterns(split.trainingAttacks(), split.trainingLegit());
        addNewDynamicRules(newPatterns);

        // 7. Save the new model
        saveTrainedModel(gdMetrics, bestChromosome, split.trainingAttacks().size() + split.trainingLegit().size());

        log.info(">>> [ML TRAINING] Neuroevolution Cycle Complete!");
    }

    private void addNewDynamicRules(List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }

            String normalizedPattern = pattern.trim();
            if (normalizedPattern.length() > 500) {
                continue;
            }

            try {
                Pattern.compile(normalizedPattern);
            } catch (Exception ex) {
                log.warn("[AUTO-IMMUNE] Skipping invalid regex pattern: {}", normalizedPattern);
                continue;
            }

            if (!securityRuleRepository.existsByPatternIgnoreCase(normalizedPattern)) {
                com.example.moodjournal.model.SecurityRule rule = new com.example.moodjournal.model.SecurityRule(
                        normalizedPattern,
                        "Auto-Immune Generated Rule (shadow): " + normalizedPattern,
                        true,
                        true // Start in SHADOW MODE for safety
                );
                securityRuleRepository.save(rule);
                log.info("[AUTO-IMMUNE] NEW VACCINE CREATED (shadow mode): Rule '{}'", normalizedPattern);
            }
        }
    }

    private List<String> gatherAttackSamples() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        List<SecurityEvent> events = securityEventRepository.findByTimestampAfterOrderByTimestampDesc(since);
        if (events == null || events.isEmpty()) {
            events = securityEventRepository.findTop500ByOrderByTimestampDesc();
        }

        Map<String, String> unique = new LinkedHashMap<>();
        for (SecurityEvent event : events) {
            if (!isTrustedAttackLabel(event)) {
                continue;
            }

            String sanitized = sanitizeTrainingText(event.getContent());
            if (sanitized.length() < 18) {
                continue;
            }

            unique.putIfAbsent(canonicalizeSample(sanitized), sanitized);
            if (unique.size() >= MAX_ATTACK_SAMPLES) {
                break;
            }
        }

        return new ArrayList<>(unique.values());
    }

    private List<String> gatherLegitSamples() {
        List<JournalEntry> entries = journalEntryRepository.findTop500ByOrderByCreatedAtDesc();

        Map<String, String> unique = new LinkedHashMap<>();
        for (JournalEntry entry : entries) {
            String content = sanitizeTrainingText(entry.getContent());
            if (content.length() < 20 || content.length() > 5000) {
                continue;
            }
            if (looksLikePromptInjection(content)) {
                continue;
            }
            if (calculateSpecialCharacterRatio(content) > 0.28) {
                continue;
            }

            unique.putIfAbsent(canonicalizeSample(content), content);
            if (unique.size() >= MAX_LEGIT_SAMPLES) {
                break;
            }
        }

        return new ArrayList<>(unique.values());
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
        double[] safeThresholds = gaResult.genes.clone();
        for (int i = 0; i < safeThresholds.length; i++) {
            safeThresholds[i] = clamp(safeThresholds[i], 0.0, 1.0);
        }
        safeThresholds[3] = clamp(safeThresholds[3], MIN_DECISION_THRESHOLD, MAX_DECISION_THRESHOLD);
        newModel.setThresholdsFromArray(safeThresholds);

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

        log.info(">>> [ML TRAINING] Saved Model v{}: Accuracy={}%, F1={}%, DecisionThreshold={}",
                nextVersion,
                String.format("%.2f", newModel.getAccuracy() * 100),
                String.format("%.2f", newModel.getF1Score() * 100),
                String.format("%.2f", newModel.getThresholdAnomaly()));
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

    private boolean isTrustedAttackLabel(SecurityEvent event) {
        if (event == null || event.getContent() == null || event.getViolationType() == null) {
            return false;
        }

        String violationType = event.getViolationType().toUpperCase(Locale.ROOT);
        double risk = event.getRiskScore() != null ? event.getRiskScore() : 0.0;

        boolean trustedViolation = violationType.startsWith("LAYER_2_")
                || violationType.equals("LAYER_3_AI")
                || violationType.equals("LAYER_5_ML_ANOMALY")
                || violationType.equals("LAYER_6_ML_NEURAL")
                || violationType.startsWith("MANUAL_RED_TEAM")
                || violationType.startsWith("SEED_ATTACK");

        return trustedViolation && risk >= 8.0;
    }

    private DataSplit splitData(List<String> attacks, List<String> legit) {
        List<String> shuffledAttacks = new ArrayList<>(attacks);
        List<String> shuffledLegit = new ArrayList<>(legit);
        Collections.shuffle(shuffledAttacks, new Random(1337));
        Collections.shuffle(shuffledLegit, new Random(7331));

        int attackValidationCount = Math.max(MIN_VALIDATION_PER_CLASS,
                (int) Math.round(shuffledAttacks.size() * VALIDATION_SPLIT));
        int legitValidationCount = Math.max(MIN_VALIDATION_PER_CLASS,
                (int) Math.round(shuffledLegit.size() * VALIDATION_SPLIT));

        attackValidationCount = Math.min(attackValidationCount, Math.max(1, shuffledAttacks.size() / 2));
        legitValidationCount = Math.min(legitValidationCount, Math.max(1, shuffledLegit.size() / 2));

        List<String> validationAttacks = new ArrayList<>(shuffledAttacks.subList(0, attackValidationCount));
        List<String> trainingAttacks = new ArrayList<>(shuffledAttacks.subList(attackValidationCount, shuffledAttacks.size()));
        List<String> validationLegit = new ArrayList<>(shuffledLegit.subList(0, legitValidationCount));
        List<String> trainingLegit = new ArrayList<>(shuffledLegit.subList(legitValidationCount, shuffledLegit.size()));

        return new DataSplit(trainingAttacks, validationAttacks, trainingLegit, validationLegit);
    }

    private ValidationMetrics evaluateCandidate(List<String> validationAttacks, List<String> validationLegit,
            double decisionThreshold) {
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        for (String sample : validationAttacks) {
            boolean predictedAttack = gdClassifier.predict(sample) >= decisionThreshold;
            if (predictedAttack) {
                tp++;
            } else {
                fn++;
            }
        }

        for (String sample : validationLegit) {
            boolean predictedAttack = gdClassifier.predict(sample) >= decisionThreshold;
            if (predictedAttack) {
                fp++;
            } else {
                tn++;
            }
        }

        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1 = (precision + recall) > 0 ? (2 * precision * recall) / (precision + recall) : 0.0;
        double falsePositiveRate = (fp + tn) > 0 ? (double) fp / (fp + tn) : 1.0;

        return new ValidationMetrics(precision, recall, f1, falsePositiveRate);
    }

    private boolean isPromotionSafe(ValidationMetrics validation) {
        return validation.precision() >= MIN_VALIDATION_PRECISION
                && validation.recall() >= MIN_VALIDATION_RECALL
                && validation.f1() >= MIN_VALIDATION_F1
                && validation.falsePositiveRate() <= MAX_VALIDATION_FALSE_POSITIVE_RATE;
    }

    private String sanitizeTrainingText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = MULTI_SPACE.matcher(text).replaceAll(" ").trim();
        if (normalized.length() > 5000) {
            return normalized.substring(0, 5000);
        }
        return normalized;
    }

    private String canonicalizeSample(String text) {
        String collapsed = MULTI_SPACE.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
        return UUID_PATTERN.matcher(collapsed).replaceAll("[uuid]");
    }

    private boolean looksLikePromptInjection(String text) {
        return SUSPICIOUS_LEGIT_PATTERN.matcher(text).find();
    }

    private double calculateSpecialCharacterRatio(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        long special = text.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();
        return (double) special / text.length();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DataSplit(
            List<String> trainingAttacks,
            List<String> validationAttacks,
            List<String> trainingLegit,
            List<String> validationLegit) {
    }

    private record ValidationMetrics(
            double precision,
            double recall,
            double f1,
            double falsePositiveRate) {
    }
}
