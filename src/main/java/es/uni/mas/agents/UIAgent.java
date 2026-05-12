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

public class UIAgent extends Agent {
    private ChatGUI gui;

    @Override
    protected void setup() {
        System.out.println("Agente de Interfaz inicializado: " + getLocalName());

        // Registrar servicio de visualización en el DF
        registerInDF();

        // Lanzar la GUI
        gui = new ChatGUI(this);
        gui.setVisible(true);

        // Comportamiento para recibir mensajes filtrados del FilterAgent
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Solo nos interesan los INFORM que traen un ChatMessage
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);
                
                if (msg != null) {
                    try {
                        ChatMessage chatMsg = (ChatMessage) msg.getContentObject();
                        gui.addMessage(chatMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }

    // Método que llama la GUI para activar/desactivar el modo estudio
    public void sendControlCommand(String command) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        // Buscamos al Controlador por nombre (ya que es único)
        msg.addReceiver(new jade.core.AID("agController", jade.core.AID.ISLOCALNAME));
        msg.setContent(command);
        send(msg);
    }

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
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agente UI finalizado.");
    }
}
