package es.uni.mas.engine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LocalSentimentAnalyzer {

    // Diccionarios locales de polaridad
    private final Set<String> palabrasPositivas = new HashSet<>(Arrays.asList(
            "feliz", "genial", "alegría", "fiesta", "jajaja", "xd", "increíble",
            "buenísimo", "excelente", "ganamos", "suerte", "risa", "divertido"
    ));

    private final Set<String> palabrasNegativas = new HashSet<>(Arrays.asList(
            "triste", "enfadado", "terrible", "desastre", "llorando", "mal",
            "peor", "horror", "urgente", "emergencia",
            "hospital", "accidente", "grave", "ayuda"
    ));

    // Clase interna con el formato
    public static class SentimentResult {
        public final double score;      // De -1.0 (Muy negativo) a 1.0 (Muy positivo)
        public final double magnitude;  // Intensidad (0.0 hacia arriba)

        public SentimentResult(double score, double magnitude) {
            this.score = score;
            this.magnitude = magnitude;
        }
    }

    public SentimentResult analyzeText(String text) {
        String[] tokens = text.toLowerCase().split("\\W+");

        int positiveCount = 0;
        int negativeCount = 0;

        for (String token : tokens) {
            if (palabrasPositivas.contains(token)) positiveCount++;
            if (palabrasNegativas.contains(token)) negativeCount++;
        }

        double totalValenceWords = positiveCount + negativeCount;

        if (totalValenceWords == 0) {
            return new SentimentResult(0.0, 0.0); // Mensaje neutro
        }

        // Score: Puntuación de polaridad (-1.0 a 1.0)
        double score = (positiveCount - negativeCount) / totalValenceWords;

        // Magnitude: Cuántas palabras emocionales hay en total
        double magnitude = totalValenceWords;

        // Modificadores de intensidad (Ejemplo: letras en mayúsculas o muchos signos de exclamación)
        if (text.contains("!!!") || text.matches(".*[A-Z]{4,}.*")) {
            magnitude += 1.5; // Aumenta la intensidad si gritan
        }

        return new SentimentResult(score, magnitude);
    }
}