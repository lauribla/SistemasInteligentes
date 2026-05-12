package es.uni.mas.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ControllerAgent extends Agent {
    private AgentController filterAgent;
    private AgentController uiAgent;
    private AgentController simulatorAgent;

    @Override
    protected void setup() {
        System.out.println("Agente Controlador (Orquestador) inicializado: " + getLocalName());

        // 1. CREACIÓN PROGRAMÁTICA DE AGENTES
        try {
            AgentContainer container = (AgentContainer) getContainerController();

            filterAgent = container.createNewAgent("FiltroIA", "es.uni.mas.agents.FilterAgent", null);
            filterAgent.start();

            uiAgent = container.createNewAgent("InterfaceAgent", "es.uni.mas.agents.UIAgent", null);
            uiAgent.start();

            simulatorAgent = container.createNewAgent("SimuladorChat", "es.uni.mas.agents.ChatSimulatorAgent", null);
            simulatorAgent.start();
            
            // Esperar un segundo y arrancar el simulador por defecto
            Thread.sleep(1000);
            sendControlToAll("STOP"); // Empezamos en modo normal (STOP = modo estudio desactivado)

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. COMPORTAMIENTO: Escuchar comandos de la interfaz
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                
                if (msg != null) {
                    sendControlToAll(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    private void sendControlToAll(String command) {
        System.out.println(getLocalName() + ": Propagando comando: " + command);
        ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
        forward.setContent(command);
        // Enviamos a ambos
        forward.addReceiver(new jade.core.AID("SimuladorChat", jade.core.AID.ISLOCALNAME));
        forward.addReceiver(new jade.core.AID("FiltroIA", jade.core.AID.ISLOCALNAME));
        send(forward);
    }

    @Override
    protected void takeDown() {
        // Apagado limpio: Matar a los agentes hijos si existen
        System.out.println("Controlador finalizando. Limpiando agentes...");
        try {
            if (filterAgent != null) filterAgent.kill();
            if (uiAgent != null) uiAgent.kill();
            if (simulatorAgent != null) simulatorAgent.kill();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
