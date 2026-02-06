package com.example.moodjournal.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Genetic Algorithm for evolving optimal detection thresholds.
 * Implements selection, crossover, and mutation on threshold chromosomes.
 * 
 * The "Evolution" in Neuroevolution.
 */
@Component
public class GeneticThresholdEvolver {

    private static final Logger log = LoggerFactory.getLogger(GeneticThresholdEvolver.class);

    private static final int POPULATION_SIZE = 20;
    private static final int TOURNAMENT_SIZE = 3;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.1;
    private static final double MUTATION_SIGMA = 0.1;

    private final Random random = new Random();

    /**
     * A chromosome representing threshold values.
     * [entropyThreshold, specialCharThreshold, uppercaseThreshold,
     * anomalyThreshold]
     */
    public static class Chromosome {
        public double[] genes;
        public double fitness;

        public Chromosome(double[] genes) {
            this.genes = genes.clone();
            this.fitness = 0;
        }

        public Chromosome copy() {
            Chromosome c = new Chromosome(this.genes);
            c.fitness = this.fitness;
            return c;
        }
    }

    /**
     * Evolve optimal thresholds over multiple generations.
     * 
     * @param fitnessFunction Function that evaluates how well thresholds perform
     * @param generations     Number of evolution cycles
     * @return Best chromosome found
     */
    public Chromosome evolve(ToDoubleFunction<double[]> fitnessFunction, int generations) {
        log.info("[GA] Starting Genetic Evolution: {} population, {} generations",
                POPULATION_SIZE, generations);

        // Initialize population with random thresholds
        List<Chromosome> population = initializePopulation();

        Chromosome allTimeBest = null;

        for (int gen = 0; gen < generations; gen++) {
            // Evaluate fitness for all chromosomes
            for (Chromosome c : population) {
                c.fitness = fitnessFunction.applyAsDouble(c.genes);
            }

            // Track best
            Chromosome genBest = Collections.max(population, Comparator.comparingDouble(c -> c.fitness));
            if (allTimeBest == null || genBest.fitness > allTimeBest.fitness) {
                allTimeBest = genBest.copy();
            }

            if (gen % 10 == 0) {
                log.debug("[GA] Generation {}: Best Fitness = {:.4f}, Thresholds = {}",
                        gen, genBest.fitness, Arrays.toString(genBest.genes));
            }

            // Create next generation
            List<Chromosome> nextGen = new ArrayList<>();

            // Elitism: Keep the best
            nextGen.add(genBest.copy());

            while (nextGen.size() < POPULATION_SIZE) {
                // Selection
                Chromosome parent1 = tournamentSelect(population);
                Chromosome parent2 = tournamentSelect(population);

                // Crossover
                Chromosome child;
                if (random.nextDouble() < CROSSOVER_RATE) {
                    child = crossover(parent1, parent2);
                } else {
                    child = parent1.copy();
                }

                // Mutation
                mutate(child);

                // Clamp values to valid range [0, 1]
                for (int i = 0; i < child.genes.length; i++) {
                    child.genes[i] = Math.max(0, Math.min(1, child.genes[i]));
                }

                nextGen.add(child);
            }

            population = nextGen;
        }

        log.info("[GA] Evolution complete! Best Fitness: {:.4f}", allTimeBest.fitness);
        log.info("[GA] Best Thresholds: entropy={:.3f}, specialChar={:.3f}, uppercase={:.3f}, anomaly={:.3f}",
                allTimeBest.genes[0], allTimeBest.genes[1], allTimeBest.genes[2], allTimeBest.genes[3]);

        return allTimeBest;
    }

    private List<Chromosome> initializePopulation() {
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double[] genes = new double[] {
                    0.3 + random.nextDouble() * 0.4, // entropy: 0.3-0.7
                    0.05 + random.nextDouble() * 0.15, // special: 0.05-0.2
                    0.1 + random.nextDouble() * 0.3, // uppercase: 0.1-0.4
                    0.3 + random.nextDouble() * 0.4 // anomaly: 0.3-0.7
            };
            population.add(new Chromosome(genes));
        }
        return population;
    }

    private Chromosome tournamentSelect(List<Chromosome> population) {
        Chromosome best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2) {
        double[] childGenes = new double[p1.genes.length];
        for (int i = 0; i < childGenes.length; i++) {
            // Uniform crossover with blend
            if (random.nextBoolean()) {
                childGenes[i] = p1.genes[i];
            } else {
                childGenes[i] = p2.genes[i];
            }
            // BLX-alpha crossover: occasionally blend
            if (random.nextDouble() < 0.3) {
                double alpha = 0.5;
                childGenes[i] = alpha * p1.genes[i] + (1 - alpha) * p2.genes[i];
            }
        }
        return new Chromosome(childGenes);
    }

    private void mutate(Chromosome c) {
        for (int i = 0; i < c.genes.length; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                // Gaussian mutation
                c.genes[i] += random.nextGaussian() * MUTATION_SIGMA;
            }
        }
    }

    /**
     * Create a fitness function for prompt injection detection.
     * 
     * @param attackFeatures Features of known attack samples
     * @param legitFeatures  Features of known legitimate samples
     * @param classifier     The gradient descent classifier to use for predictions
     * @return Fitness function
     */
    public ToDoubleFunction<double[]> createFitnessFunction(
            List<double[]> attackFeatures,
            List<double[]> legitFeatures,
            GradientDescentClassifier classifier) {

        return thresholds -> {
            int tp = 0, tn = 0, fp = 0, fn = 0;

            // Thresholds: [entropy, specialChar, uppercase, overallAnomaly]
            double entropyThresh = thresholds[0];
            double specialThresh = thresholds[1];
            double upperThresh = thresholds[2];
            double anomalyThresh = thresholds[3];

            // Evaluate on attacks (should detect)
            for (double[] features : attackFeatures) {
                boolean isSuspicious = features[0] < entropyThresh || // Low entropy is suspicious
                        features[1] > specialThresh || // High special chars
                        features[2] > upperThresh || // High uppercase
                        classifier.predict(features) > anomalyThresh; // ML score

                if (isSuspicious)
                    tp++;
                else
                    fn++;
            }

            // Evaluate on legit (should NOT detect)
            for (double[] features : legitFeatures) {
                boolean isSuspicious = features[0] < entropyThresh ||
                        features[1] > specialThresh ||
                        features[2] > upperThresh ||
                        classifier.predict(features) > anomalyThresh;

                if (isSuspicious)
                    fp++;
                else
                    tn++;
            }

            // Fitness = TPR - 2*FPR (penalize false positives more)
            double tpr = attackFeatures.isEmpty() ? 0 : (double) tp / attackFeatures.size();
            double fpr = legitFeatures.isEmpty() ? 0 : (double) fp / legitFeatures.size();

            return tpr - 2 * fpr;
        };
    }
}
