package es.uni.mas.engine;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multinomial Naive Bayes text classifier bootstrapped from the rules XML.
 *
 * Training:  keyword rules with weight > 0 feed the "important" class;
 *            keyword rules with weight < 0 feed the "distraction" class.
 *            Weight magnitude acts as word frequency in the training corpus.
 *
 * Inference: only words present in the training vocabulary contribute to
 *            the score. Unknown words are ignored (no bias toward either class).
 *            Laplace smoothing prevents zero-probability issues.
 *
 * Learning:  addExample() lets the system grow its vocabulary at runtime
 *            from user feedback on uncertain messages.
 */
public class NaiveBayesClassifier {

    // ── Training data ─────────────────────────────────────────────────────────
    private final Map<String, Integer> importantFreq    = new HashMap<>();
    private final Map<String, Integer> distractionFreq  = new HashMap<>();
    private final Set<String>          vocabulary        = new HashSet<>();
    private int totalImportant   = 0;
    private int totalDistraction = 0;

    // Weight assigned to each word extracted from user-labelled messages
    private static final int LEARNED_WEIGHT = 5;

    // ── Spanish stop words ────────────────────────────────────────────────────
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "al", "algo", "algunas", "algunos", "ante", "antes", "como",
            "con", "contra", "cual", "cuando", "de", "del", "desde", "donde",
            "durante", "e", "el", "él", "ella", "ellas", "ellos", "en", "entre",
            "era", "eres", "es", "esa", "esas", "ese", "eso", "esos", "esta",
            "estas", "este", "esto", "estos", "hay", "hasta", "la", "las", "le",
            "les", "lo", "los", "más", "me", "mi", "mí", "mientras", "muy",
            "nada", "ni", "no", "nos", "o", "otro", "para", "pero", "poco",
            "por", "que", "qué", "quien", "quienes", "se", "sea", "ser", "si",
            "sí", "sin", "sobre", "su", "sus", "también", "tan", "te", "todo",
            "tu", "tú", "un", "una", "uno", "unos", "unas", "vais", "ya", "yo",
            "hola", "hoy", "aquí", "así", "eh", "ah", "oh", "bien", "pues",
            "va", "voy", "van", "vas", "tengo", "tienes", "tiene", "vamos",
            "puedo", "puede", "pueden", "quiero", "quieres", "quiere", "he",
            "has", "ha", "han", "hemos"
    ));

    // ── Training ──────────────────────────────────────────────────────────────

    /**
     * Called by RulesEngine for every keyword rule after XML parsing.
     * Positive weight → contributes to "important" class.
     * Negative weight → contributes to "distraction" class.
     */
    public void trainFromRule(String keyword, int weight) {
        String kw = keyword.toLowerCase().trim();
        vocabulary.add(kw);
        if (weight > 0) {
            importantFreq.merge(kw, weight, Integer::sum);
            totalImportant += weight;
        } else if (weight < 0) {
            int abs = Math.abs(weight);
            distractionFreq.merge(kw, abs, Integer::sum);
            totalDistraction += abs;
        }
    }

    /**
     * Runtime learning from user feedback.
     * Words are extracted from the message by the caller (see extractMeaningfulWords).
     */
    public void addExample(List<String> words, boolean isImportant) {
        for (String word : words) {
            vocabulary.add(word);
            if (isImportant) {
                importantFreq.merge(word, LEARNED_WEIGHT, Integer::sum);
                totalImportant += LEARNED_WEIGHT;
            } else {
                distractionFreq.merge(word, LEARNED_WEIGHT, Integer::sum);
                totalDistraction += LEARNED_WEIGHT;
            }
        }
        System.out.println("NaiveBayes: aprendido " + words.size() + " palabras nuevas ["
                + (isImportant ? "IMPORTANTE" : "DISTRACCION") + "]");
    }

    // ── Inference ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the message is classified as "important".
     * Only known vocabulary words contribute; unknown words are neutral.
     * Returns false (distraction) if no known words are found — the caller
     * should treat this as "uncertain" rather than a confident classification.
     */
    public boolean classifyAsImportant(String text) {
        List<String> knownWords = extractMeaningfulWords(text).stream()
                .filter(vocabulary::contains)
                .collect(Collectors.toList());

        if (knownWords.isEmpty()) return false;

        int vocabSize = vocabulary.size();
        double logImportant   = 0.0;
        double logDistraction = 0.0;

        for (String word : knownWords) {
            // Laplace-smoothed probabilities
            double pImp = (importantFreq.getOrDefault(word, 0)   + 1.0) / (totalImportant   + vocabSize);
            double pDis = (distractionFreq.getOrDefault(word, 0) + 1.0) / (totalDistraction + vocabSize);
            logImportant   += Math.log(pImp);
            logDistraction += Math.log(pDis);
        }
        return logImportant > logDistraction;
    }

    /**
     * Returns true if at least one word in the text is in the vocabulary.
     * Used by FilterAgent to decide whether to ask the user for help.
     */
    public boolean hasKnownWords(String text) {
        return extractMeaningfulWords(text).stream().anyMatch(vocabulary::contains);
    }

    // ── Text processing ───────────────────────────────────────────────────────

    /**
     * Tokenises, lowercases, removes punctuation, short tokens, and stop words.
     * Public so RulesEngine can expose it to FilterAgent for learning.
     */
    public List<String> extractMeaningfulWords(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}¿¡]+"))
                .map(String::trim)
                .filter(w -> !w.isEmpty())
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }
}