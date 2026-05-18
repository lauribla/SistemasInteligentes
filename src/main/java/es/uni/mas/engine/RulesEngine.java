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

    public RulesEngine(String rulesFile) {
        this.rules      = new ArrayList<>();
        this.classifier = new NaiveBayesClassifier();
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

    // Rule-based scoring
    public int calculateScore(ChatMessage message) {
        int    score  = 0;
        String text   = message.getText().toLowerCase();
        String sender = message.getSender();

        for (Rule rule : rules) {
            if (rule.type.equals("sender") && rule.value.equalsIgnoreCase(sender)) {
                score += rule.weight;
            } else if (rule.type.equals("keyword") && text.contains(rule.value.toLowerCase())) {
                score += rule.weight;
            }
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

    public int     getThreshold()              { return threshold; }
    public boolean isImportant(ChatMessage m)  { return calculateScore(m) >= threshold; }

    private static class Rule {
        final String type;
        final String value;
        final int    weight;
        Rule(String type, String value, int weight) {
            this.type = type; this.value = value; this.weight = weight;
        }
    }
}