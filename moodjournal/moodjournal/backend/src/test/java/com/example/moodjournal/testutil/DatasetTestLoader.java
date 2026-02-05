package com.example.moodjournal.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Utility class for loading and sampling the master dataset for empirical
 * tests.
 * 
 * Dataset structure:
 * - id: Entry identifier
 * - text: Journal entry content
 * - category: One of [Nuanced_Depression, Complex_Human, Dark_Reality,
 * Bio_Social, Unaware_Disorder]
 * - subtype: Category-specific subtype
 * - detail_1, detail_2: Additional classification details
 */
public class DatasetTestLoader {

    private static final String DATASET_PATH = "../../../../mistral_master_raw_dataset.csv";
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility

    public record DatasetEntry(
            int id,
            String text,
            String category,
            String subtype,
            String detail1,
            String detail2) {
    }

    public enum Category {
        NUANCED_DEPRESSION("Nuanced_Depression"),
        COMPLEX_HUMAN("Complex_Human"),
        DARK_REALITY("Dark_Reality"),
        BIO_SOCIAL("Bio_Social"),
        UNAWARE_DISORDER("Unaware_Disorder");

        private final String value;

        Category(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private List<DatasetEntry> allEntries;

    public DatasetTestLoader() {
        this.allEntries = new ArrayList<>();
    }

    /**
     * Load the dataset from the CSV file.
     * Call this before using other methods.
     */
    public void load() throws IOException {
        Path datasetPath = resolveDatasetPath();

        try (BufferedReader reader = Files.newBufferedReader(datasetPath)) {
            // Skip header
            String header = reader.readLine();
            if (header == null) {
                throw new IOException("Dataset file is empty");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                DatasetEntry entry = parseCsvLine(line);
                if (entry != null) {
                    allEntries.add(entry);
                }
            }
        }
    }

    private Path resolveDatasetPath() {
        // Try multiple paths to locate the dataset
        String[] possiblePaths = {
                "src/test/resources/mistral_master_raw_dataset.csv",
                "../../../mistral_master_raw_dataset.csv",
                "../../../../mistral_master_raw_dataset.csv",
                System.getProperty("user.dir") + "/../../../../mistral_master_raw_dataset.csv"
        };

        // First try the project root relative path
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path datasetInRoot = projectRoot.getParent().getParent().resolve("mistral_master_raw_dataset.csv");
        if (Files.exists(datasetInRoot)) {
            return datasetInRoot;
        }

        for (String path : possiblePaths) {
            Path p = Paths.get(path);
            if (Files.exists(p)) {
                return p;
            }
        }

        // Fallback to test resources
        return Paths.get("src/test/resources/mistral_master_raw_dataset.csv");
    }

    /**
     * Parse a CSV line handling quoted fields with commas and newlines.
     */
    private DatasetEntry parseCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        List<String> fields = parseCsvFields(line);
        if (fields.size() < 4) {
            return null;
        }

        try {
            int id = Integer.parseInt(fields.get(0).trim());
            String text = fields.get(1);
            String category = fields.size() > 2 ? fields.get(2) : "";
            String subtype = fields.size() > 3 ? fields.get(3) : "";
            String detail1 = fields.size() > 4 ? fields.get(4) : "";
            String detail2 = fields.size() > 5 ? fields.get(5) : "";

            return new DatasetEntry(id, text, category, subtype, detail1, detail2);
        } catch (NumberFormatException e) {
            return null; // Skip malformed lines
        }
    }

    /**
     * Parse CSV fields handling quoted strings with embedded commas.
     */
    private List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields;
    }

    /**
     * Get all entries for a specific category.
     */
    public List<DatasetEntry> getEntriesByCategory(Category category) {
        return allEntries.stream()
                .filter(e -> category.getValue().equals(e.category()))
                .collect(Collectors.toList());
    }

    /**
     * Get a random sample of entries from a specific category.
     */
    public List<DatasetEntry> sampleByCategory(Category category, int sampleSize) {
        List<DatasetEntry> categoryEntries = getEntriesByCategory(category);
        if (categoryEntries.size() <= sampleSize) {
            return new ArrayList<>(categoryEntries);
        }

        List<DatasetEntry> shuffled = new ArrayList<>(categoryEntries);
        Collections.shuffle(shuffled, RANDOM);
        return shuffled.subList(0, sampleSize);
    }

    /**
     * Get a random sample from all entries.
     */
    public List<DatasetEntry> sampleAll(int sampleSize) {
        if (allEntries.size() <= sampleSize) {
            return new ArrayList<>(allEntries);
        }

        List<DatasetEntry> shuffled = new ArrayList<>(allEntries);
        Collections.shuffle(shuffled, RANDOM);
        return shuffled.subList(0, sampleSize);
    }

    /**
     * Get entries containing specific keywords in their text.
     */
    public List<DatasetEntry> searchByKeywords(String... keywords) {
        return allEntries.stream()
                .filter(e -> {
                    String lowerText = e.text().toLowerCase();
                    for (String keyword : keywords) {
                        if (lowerText.contains(keyword.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get entries by subtype.
     */
    public List<DatasetEntry> getEntriesBySubtype(String subtype) {
        return allEntries.stream()
                .filter(e -> subtype.equals(e.subtype()))
                .collect(Collectors.toList());
    }

    /**
     * Get total count of loaded entries.
     */
    public int getTotalCount() {
        return allEntries.size();
    }

    /**
     * Get count by category.
     */
    public Map<String, Long> getCountByCategory() {
        return allEntries.stream()
                .collect(Collectors.groupingBy(DatasetEntry::category, Collectors.counting()));
    }

    /**
     * Get predefined high-risk entries (Dark_Reality with specific subtypes).
     */
    public List<DatasetEntry> getHighRiskEntries() {
        return allEntries.stream()
                .filter(e -> "Dark_Reality".equals(e.category()))
                .filter(e -> e.subtype().contains("Void") ||
                        e.detail1().contains("disappear") ||
                        e.text().toLowerCase().contains("end it"))
                .collect(Collectors.toList());
    }

    /**
     * Get entries with somatic symptoms (Bio_Social category).
     */
    public List<DatasetEntry> getSomaticEntries() {
        return getEntriesByCategory(Category.BIO_SOCIAL).stream()
                .filter(e -> "Somatic".equals(e.subtype()))
                .collect(Collectors.toList());
    }
}
