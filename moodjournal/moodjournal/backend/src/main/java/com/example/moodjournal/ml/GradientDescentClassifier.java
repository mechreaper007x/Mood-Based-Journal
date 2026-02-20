package com.example.moodjournal.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;







@Component
public class GradientDescentClassifier {

    private static final Logger log = LoggerFactory.getLogger(GradientDescentClassifier.class);

    private static final int NUM_FEATURES = 7;
    private double[] weights;
    private double bias;
    private double learningRate = 0.01;

    
    private static final Set<String> ATTACK_KEYWORDS = Set.of(
            "ignore", "previous", "instructions", "system", "override", "sudo",
            "admin", "developer", "mode", "dan", "jailbreak", "bypass", "hack",
            "prompt", "inject", "roleplay", "pretend", "character", "unfiltered",
            "constraints", "rules", "safety", "protocol", "execute", "command");

    
    
    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(?:\\S+\\s+){0,10}instruction", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+(?:not|now|dan)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s+(?:\\S+\\s+){0,5}(?:override|prompt)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("break\\s+(?:\\S+\\s+){0,10}rules", Pattern.CASE_INSENSITIVE));

    public GradientDescentClassifier() {
        
        
        
        this.weights = new double[NUM_FEATURES];
        Random rand = new Random(42);
        double scale = 0.01; 
        for (int i = 0; i < NUM_FEATURES; i++) {
            weights[i] = rand.nextGaussian() * scale;
        }
        this.bias = -5.0;
    }

    public void loadWeights(double[] loadedWeights, double loadedBias) {
        if (loadedWeights != null && loadedWeights.length == NUM_FEATURES) {
            this.weights = loadedWeights.clone();
            this.bias = loadedBias;
            log.info("[ML] Loaded trained weights: {}", Arrays.toString(weights));
        }
    }

    


    public double[] extractFeatures(String input) {
        if (input == null || input.isEmpty()) {
            return new double[NUM_FEATURES];
        }

        String lower = input.toLowerCase();
        String[] words = input.split("\\s+");

        double[] features = new double[NUM_FEATURES];

        
        features[0] = calculateEntropy(input) / 5.0; 

        
        long specialCount = input.chars().filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count();
        features[1] = (double) specialCount / Math.max(1, input.length());

        
        long upperCount = input.chars().filter(Character::isUpperCase).count();
        long letterCount = input.chars().filter(Character::isLetter).count();
        features[2] = letterCount > 0 ? (double) upperCount / letterCount : 0;

        
        int keywordCount = 0;
        for (String word : words) {
            if (ATTACK_KEYWORDS.contains(word.toLowerCase())) {
                keywordCount++;
            }
        }
        features[3] = Math.min(1.0, keywordCount / 5.0); 

        
        
        int sentenceCount = input.split("[.!?]+").length;
        double avgWordsPerSentence = (double) words.length / Math.max(1, sentenceCount);
        features[4] = 1.0 - Math.min(1.0, avgWordsPerSentence / 20.0); 

        
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        features[5] = 1.0 - ((double) uniqueWords.size() / Math.max(1, words.length));

        
        int patternMatches = 0;
        for (Pattern p : SUSPICIOUS_PATTERNS) {
            if (p.matcher(lower).find()) {
                patternMatches++;
            }
        }
        features[6] = Math.min(1.0, patternMatches / 3.0);

        return features;
    }

    private double calculateEntropy(String input) {
        if (input == null || input.isEmpty())
            return 0;

        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toLowerCase().toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }

        double entropy = 0;
        int len = input.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            if (p > 0) {
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    


    private double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-z));
    }

    




    public double predict(String input) {
        double[] features = extractFeatures(input);
        return predict(features);
    }

    public double predict(double[] features) {
        double z = bias;
        for (int i = 0; i < NUM_FEATURES; i++) {
            z += weights[i] * features[i];
        }
        return sigmoid(z);
    }

    







    public Map<String, Double> train(List<String> attackSamples, List<String> legitimateSamples, int epochs) {
        log.info("[ML] Starting Gradient Descent training: {} attacks, {} legit, {} epochs",
                attackSamples.size(), legitimateSamples.size(), epochs);

        
        List<double[]> X = new ArrayList<>();
        List<Integer> y = new ArrayList<>();

        for (String attack : attackSamples) {
            X.add(extractFeatures(attack));
            y.add(1); 
        }
        for (String legit : legitimateSamples) {
            X.add(extractFeatures(legit));
            y.add(0); 
        }

        
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < X.size(); i++)
            indices.add(i);
        Collections.shuffle(indices, new Random(System.currentTimeMillis()));

        
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0;

            for (int idx : indices) {
                double[] features = X.get(idx);
                int label = y.get(idx);

                
                double pred = predict(features);

                
                double loss = -label * Math.log(pred + 1e-8) - (1 - label) * Math.log(1 - pred + 1e-8);
                totalLoss += loss;

                
                double error = pred - label;
                for (int i = 0; i < NUM_FEATURES; i++) {
                    weights[i] -= learningRate * error * features[i];
                }
                bias -= learningRate * error;
            }

            if (epoch % 100 == 0) {
                log.debug("[ML] Epoch {}: Loss = {}", epoch, totalLoss / X.size());
            }
        }

        
        int tp = 0, tn = 0, fp = 0, fn = 0;
        for (int i = 0; i < X.size(); i++) {
            double pred = predict(X.get(i));
            int predLabel = pred > 0.5 ? 1 : 0;
            int actual = y.get(i);

            if (predLabel == 1 && actual == 1)
                tp++;
            else if (predLabel == 0 && actual == 0)
                tn++;
            else if (predLabel == 1 && actual == 0)
                fp++;
            else
                fn++;
        }

        double accuracy = (double) (tp + tn) / (tp + tn + fp + fn);
        double precision = tp > 0 ? (double) tp / (tp + fp) : 0;
        double recall = tp > 0 ? (double) tp / (tp + fn) : 0;
        double f1 = precision + recall > 0 ? 2 * precision * recall / (precision + recall) : 0;

        log.info("[ML] Training complete! Accuracy: {:.2f}%, Precision: {:.2f}%, Recall: {:.2f}%, F1: {:.2f}%",
                accuracy * 100, precision * 100, recall * 100, f1 * 100);

        return Map.of(
                "accuracy", accuracy,
                "precision", precision,
                "recall", recall,
                "f1Score", f1,
                "trainingSize", (double) X.size());
    }

    public double[] getWeights() {
        return weights.clone();
    }

    public double getBias() {
        return bias;
    }

    public void setLearningRate(double lr) {
        this.learningRate = lr;
    }
}
