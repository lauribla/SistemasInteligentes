package es.uni.mas.gui;

import es.uni.mas.agents.UIAgent;
import es.uni.mas.model.ChatMessage;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatGUI extends JFrame {

    private final UIAgent myAgent;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    private JTextArea chatArea;
    private JLabel    statsLabel;
    private JButton   toggleButton;
    private boolean   studyModeOn = false;

    // ── Classify banner ───────────────────────────────────────────────────────
    private JPanel       bannerPanel;
    private JLabel       bannerMessageLabel;
    private JProgressBar countdownBar;
    private Timer        bannerTimer;
    private String       pendingConversationId;

    private static final int BANNER_TICKS   = 100;
    private static final int TIMER_INTERVAL = 80;   // 100 × 80 ms = 8 000 ms
    private int remainingTicks;

    // ─────────────────────────────────────────────────────────────────────────

    public ChatGUI(UIAgent agent) {
        this.myAgent = agent;
        initUI();
    }

    private void initUI() {
        setTitle("EstudioGuard — Filtro Inteligente de Chats");
        setSize(560, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // NORTH: statistics strip
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Estadísticas en tiempo real"));
        statsLabel = new JLabel("<html><i>Esperando datos...</i></html>");
        statsLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statsLabel.setBorder(new EmptyBorder(4, 6, 4, 6));
        statsPanel.add(statsLabel, BorderLayout.CENTER);
        root.add(statsPanel, BorderLayout.NORTH);

        // CENTER: classify banner (hidden) + chat area
        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.add(buildBannerPanel(), BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        centerPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);

        // SOUTH: study mode toggle
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        toggleButton = new JButton("Activar MODO ESTUDIO");
        toggleButton.setBackground(new Color(200, 255, 200));
        toggleButton.addActionListener(this::onToggleStudyMode);
        controlPanel.add(toggleButton);
        root.add(controlPanel, BorderLayout.SOUTH);

        add(root);
        addSystemMessage("Bienvenido a EstudioGuard. Pulsa el botón para filtrar.");
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    private JPanel buildBannerPanel() {
        bannerPanel = new JPanel(new BorderLayout(6, 4));
        bannerPanel.setBackground(new Color(255, 243, 205));
        bannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 180, 50), 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        bannerPanel.setVisible(false);

        bannerMessageLabel = new JLabel();
        bannerMessageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        bannerPanel.add(bannerMessageLabel, BorderLayout.CENTER);

        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setOpaque(false);

        countdownBar = new JProgressBar(0, BANNER_TICKS);
        countdownBar.setValue(BANNER_TICKS);
        countdownBar.setPreferredSize(new Dimension(0, 8));
        countdownBar.setForeground(new Color(230, 160, 30));
        countdownBar.setBackground(new Color(245, 225, 160));
        countdownBar.setBorderPainted(false);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonRow.setOpaque(false);

        JButton yesBtn = new JButton("✓ Sí");
        yesBtn.setBackground(new Color(180, 230, 180));
        yesBtn.setFocusPainted(false);
        yesBtn.addActionListener(e -> dismissBanner("YES"));

        JButton noBtn = new JButton("✗ No");
        noBtn.setBackground(new Color(230, 180, 180));
        noBtn.setFocusPainted(false);
        noBtn.addActionListener(e -> dismissBanner("NO"));

        buttonRow.add(yesBtn);
        buttonRow.add(noBtn);

        bottomRow.add(countdownBar, BorderLayout.CENTER);
        bottomRow.add(buttonRow,    BorderLayout.EAST);
        bannerPanel.add(bottomRow,  BorderLayout.SOUTH);

        return bannerPanel;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Shows the classify banner for an uncertain message (study mode OFF only). */
    public void showClassifyBanner(ChatMessage msg, String conversationId) {
        SwingUtilities.invokeLater(() -> {
            if (bannerPanel.isVisible() && pendingConversationId != null) {
                dismissBanner("TIMEOUT");
            }
            pendingConversationId = conversationId;
            String truncated = msg.getText().length() > 60
                    ? msg.getText().substring(0, 57) + "..."
                    : msg.getText();
            bannerMessageLabel.setText(
                    "<html>❓ <b>" + msg.getSender() + ":</b> " + truncated
                            + " &nbsp;<i style='color:gray'>¿Sería importante en modo estudio?</i></html>"
            );
            bannerPanel.setVisible(true);
            revalidate();
            startBannerTimer();
        });
    }

    /** Shows messages that were withheld during study mode, revealed on mode-off. */
    public void showDiscardedBatch(List<ChatMessage> messages) {
        SwingUtilities.invokeLater(() -> {
            if (messages == null || messages.isEmpty()) return;
            chatArea.append("\n══════════════════════════════════════════════════\n");
            chatArea.append("  MENSAJES RETENIDOS DURANTE EL MODO ESTUDIO (" + messages.size() + ")\n");
            chatArea.append("══════════════════════════════════════════════════\n");
            for (ChatMessage msg : messages) {
                String time = sdf.format(new Date(msg.getTimestamp()));
                chatArea.append(String.format("[%s] %s: %s%n", time, msg.getSender(), msg.getText()));
            }
            chatArea.append("══════════════════════════════════════════════════\n\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /** Normal approved message. */
    public void addMessage(ChatMessage msg) {
        SwingUtilities.invokeLater(() -> {
            String time = sdf.format(new Date(msg.getTimestamp()));
            chatArea.append(String.format("[%s] %s: %s%n", time, msg.getSender(), msg.getText()));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    /** Updates the statistics strip. */
    public void updateStats(String stats) {
        SwingUtilities.invokeLater(() ->
                statsLabel.setText("<html>" + stats.replace("\n", "<br>") + "</html>")
        );
    }

    // ── Banner timer ──────────────────────────────────────────────────────────

    private void startBannerTimer() {
        if (bannerTimer != null && bannerTimer.isRunning()) bannerTimer.stop();
        remainingTicks = BANNER_TICKS;
        countdownBar.setValue(BANNER_TICKS);
        bannerTimer = new Timer(TIMER_INTERVAL, (ActionEvent e) -> {
            remainingTicks--;
            countdownBar.setValue(remainingTicks);
            if (remainingTicks <= 0) dismissBanner("TIMEOUT");
        });
        bannerTimer.start();
    }

    private void dismissBanner(String answer) {
        if (bannerTimer != null) bannerTimer.stop();
        bannerPanel.setVisible(false);
        revalidate();
        if (pendingConversationId != null) {
            myAgent.sendClassifyAnswer(pendingConversationId, answer);
            pendingConversationId = null;
        }
    }

    // ── Study mode toggle ─────────────────────────────────────────────────────

    private void onToggleStudyMode(ActionEvent e) {
        studyModeOn = !studyModeOn;
        if (studyModeOn) {
            toggleButton.setText("Desactivar MODO ESTUDIO");
            toggleButton.setBackground(new Color(255, 200, 200));
            myAgent.sendControlCommand("START");
            addSystemMessage("SISTEMA: Modo Estudio ACTIVADO. Filtrando distracciones...");
        } else {
            toggleButton.setText("Activar MODO ESTUDIO");
            toggleButton.setBackground(new Color(200, 255, 200));
            myAgent.sendControlCommand("STOP");
            addSystemMessage("SISTEMA: Modo Estudio DESACTIVADO. Mostrando mensajes retenidos...");
        }
    }

    private void addSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("──────────────────────────────────────────────────\n");
            chatArea.append(text + "\n");
            chatArea.append("──────────────────────────────────────────────────\n");
        });
    }
}