package es.uni.mas.engine;

import es.uni.mas.model.ChatMessage;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RulesEngine {
    private int threshold;
    private List<Rule> rules;
    private NaiveBayesClassifier classifier;

    // --- NUEVO: Instancia del Analizador de Sentimientos ---
    private LocalSentimentAnalyzer sentimentAnalyzer;

    public RulesEngine(String rulesFile) {
        this.rules      = new ArrayList<>();
        this.classifier = new NaiveBayesClassifier();

        // --- NUEVO: Inicializar el analizador local ---
        this.sentimentAnalyzer = new LocalSentimentAnalyzer();

        loadRules(rulesFile);
    }

    private void loadRules(String rulesFile) {
        String normalised = rulesFile.startsWith("/") ? rulesFile.substring(1) : rulesFile;
        InputStream is = getClass().getClassLoader().getResourceAsStream(normalised);
        if (is == null)
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalised);

        if (is == null) {
            System.err.println("CRITICO: No se encontro el archivo de reglas: " + normalised);
            this.threshold = 10;
            return;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            this.threshold = Integer.parseInt(
                    doc.getDocumentElement().getAttribute("threshold"));

            NodeList nList = doc.getElementsByTagName("rule");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element e      = (Element) nNode;
                    String  type   = e.getAttribute("type");
                    String  value  = e.getAttribute("value");
                    int     weight = Integer.parseInt(e.getAttribute("weight"));
                    rules.add(new Rule(type, value, weight));

                    // Train the Naive Bayes classifier from keyword rules only.
                    // Sender rules are handled deterministically by calculateScore().
                    if (type.equals("keyword")) {
                        classifier.trainFromRule(value, weight);
                    }
                }
            }
            System.out.println("RulesEngine: cargadas " + rules.size()
                    + " reglas. Umbral=" + threshold);
        } catch (Exception e) {
            System.err.println("RulesEngine: error al parsear las reglas.");
            e.printStackTrace();
        }
    }

    // Rule-based scoring with Sentiment Analysis Modifiers
    public int calculateScore(ChatMessage message) {
        int    score  = 0;
        String text   = message.getText().toLowerCase();
        String sender = message.getSender();

        // 1. Puntuación base (Reglas XML)
        for (Rule rule : rules) {
            if (rule.type.equals("sender") && rule.value.equalsIgnoreCase(sender)) {
                score += rule.weight;
            } else if (rule.type.equals("keyword") && text.contains(rule.value.toLowerCase())) {
                score += rule.weight;
            }
        }

        // 2. --- NUEVO: Modificador por Análisis de Sentimiento Local ---
        LocalSentimentAnalyzer.SentimentResult sentiment = sentimentAnalyzer.analyzeText(message.getText());

        // Regla de Emergencia: Si es muy negativo y con fuerza, podría ser una urgencia real
        if (sentiment.score < -0.5 && sentiment.magnitude >= 1.0) {
            System.out.println("RulesEngine: ⚠️ Alerta emocional negativa (Posible urgencia). Score Sentimiento: " + sentiment.score);
            score += 25; // Prioridad altísima para que pase el filtro o se ponga arriba
        }
        // Regla de Euforia: Si es extremadamente positivo, suele ser distracción (broma, fiesta)
        else if (sentiment.score > 0.5 && sentiment.magnitude >= 1.0) {
            System.out.println("RulesEngine: 🎉 Alerta de euforia (Posible distracción). Score Sentimiento: " + sentiment.score);
            score -= 10; // Penalización en modo estudio para que baje su prioridad
        }

        return score;
    }

    // NB classifier facade

    /**
     * Returns true when the message contains no words the classifier knows.
     * Used by FilterAgent to decide whether to ask the user for help.
     */
    public boolean isUncertain(ChatMessage message) {
        return !classifier.hasKnownWords(message.getText());
    }

    /** Returns the NB classifier's verdict (only meaningful when !isUncertain). */
    public boolean classifierSaysImportant(ChatMessage message) {
        return classifier.classifyAsImportant(message.getText());
    }

    // Runtime learning
    public void addLearnedExample(List<String> words, boolean isImportant) {
        classifier.addExample(words, isImportant);
    }

    /** Tokenises text into meaningful words for use in addLearnedExample. */
    public List<String> extractWords(String text) {
        return classifier.extractMeaningfulWords(text);
    }

    public int getThreshold() {
        return threshold;
    }

    // --- NUEVO: Metodo para poder modificar el umbral desde un Slider (Curva ROC) ---
    public void setThreshold(int newThreshold) {
        this.threshold = newThreshold;
        System.out.println("RulesEngine: Nuevo umbral de sensibilidad establecido a " + this.threshold);
    }

    public boolean isImportant(ChatMessage m) {
        return calculateScore(m) >= threshold;
    }

    private static class Rule {
        final String type;
        final String value;
        final int    weight;
        Rule(String type, String value, int weight) {
            this.type = type; this.value = value; this.weight = weight;
        }
    }
}