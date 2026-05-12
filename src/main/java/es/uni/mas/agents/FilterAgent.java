package es.uni.mas.agents;

import es.uni.mas.engine.RulesEngine;
import es.uni.mas.model.ChatMessage;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class FilterAgent extends Agent {
    private RulesEngine engine;
    private ACLMessage currentMsg;
    private ChatMessage chatMsg;
    private boolean studyModeActive = false; // Por defecto desactivado

    // Nombres de los estados
    private static final String WAITING = "WAITING";
    private static final String ANALYZING = "ANALYZING";
    private static final String FORWARDING = "FORWARDING";
    private static final String DISCARDING = "DISCARDING";

    @Override
    protected void setup() {
        System.out.println("Agente Filtro inicializado: " + getLocalName());
        
        engine = new RulesEngine("/data/rules.xml");
        registerInDF();

        // Comportamiento para recibir órdenes del Controller (START/STOP modo estudio)
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    studyModeActive = msg.getContent().equalsIgnoreCase("START");
                    System.out.println(getLocalName() + ": Modo estudio cambiado a: " + studyModeActive);
                } else {
                    block();
                }
            }
        });

        FSMBehaviour fsm = new FSMBehaviour(this);

        // 1. Estado WAITING
        fsm.registerFirstState(new OneShotBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                currentMsg = blockingReceive(mt);
                try {
                    chatMsg = (ChatMessage) currentMsg.getContentObject();
                } catch (UnreadableException e) { chatMsg = null; }
            }
            @Override
            public int onEnd() { return (chatMsg != null) ? 1 : 0; }
        }, WAITING);

        // 2. Estado ANALYZING: Procesa el mensaje
        fsm.registerState(new OneShotBehaviour() {
            private int result;
            @Override
            public void action() {
                if (!studyModeActive) {
                    System.out.println(getLocalName() + ": [BYPASS] Modo estudio OFF. Pasa todo.");
                    result = 1; // Pasa siempre
                    return;
                }

                int score = engine.calculateScore(chatMsg);
                System.out.println(getLocalName() + ": Analizando [" + chatMsg.getSender() + "] -> Score: " + score);
                
                result = (score >= engine.getThreshold()) ? 1 : 0;
            }
            @Override
            public int onEnd() { return result; }
        }, ANALYZING);

        // 3. Estado FORWARDING: Reenvía el mensaje a la UI
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println(getLocalName() + ": [APROBADO] Reenviando a UI...");
                // En una implementación real buscaríamos al UIAgent en el DF
                // Por ahora enviamos una respuesta si fuera necesario o buscamos el servicio
                ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
                // Aquí el UIAgent se encontrará dinámicamente o por nombre
                // Nota: En el flujo real, el ControllerAgent nos dirá quién es el UIAgent
                // o lo buscaremos en el DF.
                
                // Simulación de búsqueda rápida en DF para el UIAgent
                try {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("visualizacion-chat");
                    template.addServices(sd);
                    DFAgentDescription[] results = DFService.search(myAgent, template);
                    if (results.length > 0) {
                        forward.addReceiver(results[0].getName());
                        forward.setContentObject(chatMsg);
                        send(forward);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, FORWARDING);

        // 4. Estado DISCARDING: Log de descarte
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println(getLocalName() + ": [DESCARTADO] El mensaje no cumple el umbral de importancia.");
            }
        }, DISCARDING);

        // Definir transiciones
        fsm.registerDefaultTransition(WAITING, ANALYZING, new String[]{WAITING, ANALYZING});
        fsm.registerTransition(ANALYZING, FORWARDING, 1);
        fsm.registerTransition(ANALYZING, DISCARDING, 0);
        fsm.registerDefaultTransition(FORWARDING, WAITING);
        fsm.registerDefaultTransition(DISCARDING, WAITING);

        addBehaviour(fsm);
    }

    private void registerInDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("filtrado-spam");
        sd.setName("servicio-filtrado");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agente Filtro finalizado.");
    }
}
