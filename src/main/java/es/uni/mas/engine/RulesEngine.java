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

    public RulesEngine(String rulesFile) {
        this.rules = new ArrayList<>();
        loadRules(rulesFile);
    }

    private void loadRules(String rulesFile) {
        try {
            // Intentar varias formas de cargar el recurso en Maven/IntelliJ
            InputStream is = getClass().getResourceAsStream(rulesFile);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(rulesFile.substring(1));
            }
            
            if (is == null) {
                System.err.println("CRÍTICO: No se pudo encontrar el archivo de reglas: " + rulesFile);
                this.threshold = 10; // Fallback de seguridad
                return;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // Leer umbral
            String thresholdStr = doc.getDocumentElement().getAttribute("threshold");
            this.threshold = Integer.parseInt(thresholdStr);

            // Leer reglas
            NodeList nList = doc.getElementsByTagName("rule");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String type = eElement.getAttribute("type");
                    String value = eElement.getAttribute("value");
                    int weight = Integer.parseInt(eElement.getAttribute("weight"));
                    rules.add(new Rule(type, value, weight));
                }
            }
            System.out.println("Reglas cargadas correctamente. Umbral: " + threshold);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int calculateScore(ChatMessage message) {
        int score = 0;
        String text = message.getText().toLowerCase();
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

    public boolean isImportant(ChatMessage message) {
        return calculateScore(message) >= threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    // Clase interna para representar una regla
    private static class Rule {
        String type;
        String value;
        int weight;

        Rule(String type, String value, int weight) {
            this.type = type;
            this.value = value;
            this.weight = weight;
        }
    }
}
