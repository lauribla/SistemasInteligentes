package es.uni.mas.gui;

import es.uni.mas.agents.UIAgent;
import es.uni.mas.model.ChatMessage;
import es.uni.mas.engine.ContactManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ChatGUI extends JFrame {

    private final UIAgent myAgent;
    private final ContactManager contactManager;
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

    private Timer studyModeTimer;
    private JLabel timerLabel;
    private int studyMinutesRemaining = 0;
    // ─────────────────────────────────────────────────────────────────────────

    public ChatGUI(UIAgent agent) {
        this.myAgent = agent;
        this.contactManager = ContactManager.getInstance();
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

        // SOUTH: study mode toggle + contact configuration
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        toggleButton = new JButton("Activar MODO ESTUDIO");
        toggleButton.setBackground(new Color(200, 255, 200));
        toggleButton.addActionListener(this::onToggleStudyMode);
        controlPanel.add(toggleButton);
        
        JButton configContactsBtn = new JButton("Configurar Contactos");
        configContactsBtn.setBackground(new Color(220, 220, 255));
        configContactsBtn.addActionListener(this::onOpenContactsConfig);
        controlPanel.add(configContactsBtn);
        
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
        if (!studyModeOn) {
            // === ACTIVANDO MODO ESTUDIO ===
            Object[] options = {"Hasta que lo desactive", "Por tiempo limitado"};
            int choice = JOptionPane.showOptionDialog(this,
                    "¿Cómo quieres activar el Modo Estudio?",
                    "Activar Modo Estudio",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (choice == 0) {
                // Modo indefinido (sin temporizador)
                activateStudyMode(0);
            } else if (choice == 1) {
                String input = JOptionPane.showInputDialog(this,
                        "Duración del Modo Estudio (en minutos):",
                        "Tiempo limitado",
                        JOptionPane.PLAIN_MESSAGE);

                if (input == null) return; // Cancelado

                try {
                    int minutes = Integer.parseInt(input.trim());
                    if (minutes < 1) {
                        JOptionPane.showMessageDialog(this, "El tiempo debe ser al menos 1 minuto.");
                        return;
                    }
                    activateStudyMode(minutes);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Por favor introduce un número válido.");
                }
            }
        } else {
            // === DESACTIVANDO MODO ESTUDIO ===
            deactivateStudyMode();
        }
    }

    private void activateStudyMode(int minutes) {
        studyModeOn = true;
        toggleButton.setText(minutes > 0 ? "Desactivar MODO ESTUDIO" : "Desactivar MODO ESTUDIO");
        toggleButton.setBackground(new Color(255, 200, 200));

        String command = minutes > 0 ? "START:" + minutes : "START";
        myAgent.sendControlCommand(command);

        addSystemMessage("SISTEMA: Modo Estudio ACTIVADO" +
                (minutes > 0 ? " por " + minutes + " minutos." : " (indefinido)."));

        if (minutes > 0) {
            startStudyModeCountdown(minutes*60);
        }
    }

    private void deactivateStudyMode() {
        studyModeOn = false;
        toggleButton.setText("Activar MODO ESTUDIO");
        toggleButton.setBackground(new Color(200, 255, 200));
        myAgent.sendControlCommand("STOP");

        stopStudyModeCountdown();
        addSystemMessage("SISTEMA: Modo Estudio DESACTIVADO. Mostrando mensajes retenidos...");
    }

    private void addSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("──────────────────────────────────────────────────\n");
            chatArea.append(text + "\n");
            chatArea.append("──────────────────────────────────────────────────\n");
        });
    }

    // ── Contact configuration dialog ──────────────────────────────────────────

    private void onOpenContactsConfig(ActionEvent e) {
        JDialog dialog = new JDialog(this, "Configurar Contactos", true);
        dialog.setSize(600, 600);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Título
        JLabel titleLabel = new JLabel("Selecciona qué contactos permitir o bloquear durante modo estudio:");
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Tabla de contactos
        Set<String> allContacts = contactManager.getAllKnownContacts();
        Set<String> whitelist = contactManager.getWhitelist();
        Set<String> blacklist = contactManager.getBlacklist();

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (String contact : new TreeSet<>(allContacts)) {
            JPanel contactRow = createContactRow(contact, whitelist.contains(contact), 
                    blacklist.contains(contact));
            listPanel.add(contactRow);
            listPanel.add(Box.createVerticalStrut(6));
        }

        listPanel.add(Box.createVerticalGlue());
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Contactos"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Botones inferiores
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton closeBtn = new JButton("Cerrar");
        closeBtn.addActionListener(e2 -> dialog.dispose());
        buttonPanel.add(closeBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private JPanel createContactRow(String contact, boolean isWhitelisted, boolean isBlacklisted) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        row.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        row.setBackground(isWhitelisted ? new Color(220, 255, 220) 
                : (isBlacklisted ? new Color(255, 220, 220) : Color.WHITE));

        JLabel contactLabel = new JLabel(contact);
        contactLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        contactLabel.setPreferredSize(new Dimension(200, 20));
        row.add(contactLabel);

        JButton allowBtn = new JButton("Permitir");
        allowBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        allowBtn.setBackground(new Color(180, 230, 180));
        allowBtn.setFocusPainted(false);
        allowBtn.addActionListener(e -> {
            contactManager.allowContact(contact);
            row.setBackground(new Color(220, 255, 220));
        });
        row.add(allowBtn);

        JButton blockBtn = new JButton("Bloquear");
        blockBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        blockBtn.setBackground(new Color(230, 180, 180));
        blockBtn.setFocusPainted(false);
        blockBtn.addActionListener(e -> {
            contactManager.blockContact(contact);
            row.setBackground(new Color(255, 220, 220));
        });
        row.add(blockBtn);

        JButton resetBtn = new JButton("Por defecto");
        resetBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        resetBtn.setBackground(new Color(220, 220, 220));
        resetBtn.setFocusPainted(false);
        resetBtn.addActionListener(e -> {
            contactManager.resetContact(contact);
            row.setBackground(Color.WHITE);
        });
        row.add(resetBtn);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void startStudyModeCountdown(int minutes) {
        studyMinutesRemaining = minutes;
        stopStudyModeCountdown(); // Limpiar anterior si existía

        // Crear etiqueta
        timerLabel = new JLabel(" ⏱ " + formatTime(minutes));
        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        timerLabel.setForeground(new Color(220, 50, 50));

        // Añadir al panel de controles
        JPanel controlPanel = (JPanel) toggleButton.getParent();
        controlPanel.add(timerLabel);
        controlPanel.revalidate();
        controlPanel.repaint();

        // Timer cada segundo (más fluido)
        studyModeTimer = new Timer(1000, e -> {
            studyMinutesRemaining--;

            if (studyMinutesRemaining <= 0) {
                stopStudyModeCountdown();
                if (studyModeOn) {
                    deactivateStudyMode();
                }
            } else {
                timerLabel.setText(" ⏱ " + formatTime(studyMinutesRemaining));
            }
        });

        studyModeTimer.start();
    }

    private String formatTime(int totalMinutes) {
        int hours = totalMinutes /3600;
        int aux = totalMinutes % 3600;
        int minutes = aux / 60;
        int seconds = aux % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private void stopStudyModeCountdown() {
        if (studyModeTimer != null) {
            studyModeTimer.stop();
            studyModeTimer = null;
        }

        if (timerLabel != null) {
            JPanel controlPanel = (JPanel) toggleButton.getParent();
            if (controlPanel != null) {
                controlPanel.remove(timerLabel);
                controlPanel.revalidate();
                controlPanel.repaint();
            }
            timerLabel = null;
        }
    }

    public ContactManager getContactManager() {
        return contactManager;
    }
}