package es.uni.mas.agents;

import es.uni.mas.gui.ChatGUI;
import es.uni.mas.model.ChatMessage;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

public class UIAgent extends Agent {
    private ChatGUI gui;

    @Override
    protected void setup() {
        System.out.println("Agente de Interfaz inicializado: " + getLocalName());
        registerInDF();

        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                gui = new ChatGUI(this);
                gui.setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ── Behaviour 1: INFORM messages ──────────────────────────────────────
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String ontology = msg.getOntology();

                    if ("stats-update".equals(ontology)) {
                        gui.updateStats(msg.getContent());

                    } else if ("discarded-batch".equals(ontology)) {
                        // Batch of messages withheld during study mode — show them now
                        try {
                            @SuppressWarnings("unchecked")
                            List<ChatMessage> batch = (List<ChatMessage>) msg.getContentObject();
                            gui.showDiscardedBatch(batch);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        // Normal approved message
                        try {
                            ChatMessage chatMsg = (ChatMessage) msg.getContentObject();
                            gui.addMessage(chatMsg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    block();
                }
            }
        });

        // ── Behaviour 2: classify-request from FilterAgent ────────────────────
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchOntology("classify-request")
                );
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    try {
                        ChatMessage chatMsg = (ChatMessage) msg.getContentObject();
                        gui.showClassifyBanner(chatMsg, msg.getConversationId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }

    // ── Methods called by ChatGUI ─────────────────────────────────────────────

    public void sendControlCommand(String command) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new jade.core.AID("agController", jade.core.AID.ISLOCALNAME));
        msg.setContent(command);
        send(msg);
    }

    public void sendClassifyAnswer(String conversationId, String answer) {
        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
        reply.addReceiver(new jade.core.AID("FiltroIA", jade.core.AID.ISLOCALNAME));
        reply.setOntology("classify-response");
        reply.setConversationId(conversationId);
        reply.setContent(answer);
        send(reply);
    }

    // ── DF registration ───────────────────────────────────────────────────────

    private void registerInDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("visualizacion-chat");
        sd.setName("servicio-visualizacion");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        if (gui != null) gui.dispose();
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
        System.out.println("Agente UI finalizado.");
    }
}