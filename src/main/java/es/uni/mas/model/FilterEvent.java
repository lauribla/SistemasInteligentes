package es.uni.mas.model;

import java.io.Serializable;

/**
 * Sent from FilterAgent → StatsAgent after every filtering decision.
 * Carries the original message, the score calculated, and whether it passed.
 */
public class FilterEvent implements Serializable {
    private final ChatMessage message;
    private final boolean passed;
    private final int score;

    public FilterEvent(ChatMessage message, boolean passed, int score) {
        this.message = message;
        this.passed  = passed;
        this.score   = score;
    }

    public ChatMessage getMessage() { return message; }
    public boolean isPassed()       { return passed;  }
    public int getScore()           { return score;   }
}