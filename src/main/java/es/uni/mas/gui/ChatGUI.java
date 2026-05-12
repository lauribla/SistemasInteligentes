package es.uni.mas.gui;

import es.uni.mas.agents.UIAgent;
import es.uni.mas.model.ChatMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatGUI extends JFrame {
    private UIAgent myAgent;
    private JTextArea chatArea;
    private JButton toggleButton;
    private boolean studyModeOn = false;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public ChatGUI(UIAgent agent) {
        this.myAgent = agent;
        initUI();
    }

    private void initUI() {
        setTitle("EstudioGuard - Filtro Inteligente de Chats");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel Principal
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Area de Chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de Control
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        toggleButton = new JButton("Activar MODO ESTUDIO");
        toggleButton.setBackground(new Color(200, 255, 200));
        
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                    addSystemMessage("SISTEMA: Modo Estudio DESACTIVADO. Recibiendo todo.");
                }
            }
        });

        controlPanel.add(toggleButton);
        panel.add(controlPanel, BorderLayout.SOUTH);

        add(panel);
        
        addSystemMessage("Bienvenido a EstudioGuard. Pulsa el botón para empezar a filtrar.");
    }

    public void addMessage(ChatMessage msg) {
        String time = sdf.format(new Date(msg.getTimestamp()));
        chatArea.append(String.format("[%s] %s: %s\n", time, msg.getSender(), msg.getText()));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void addSystemMessage(String text) {
        chatArea.append("--------------------------------------------------\n");
        chatArea.append(text + "\n");
        chatArea.append("--------------------------------------------------\n");
    }
}
