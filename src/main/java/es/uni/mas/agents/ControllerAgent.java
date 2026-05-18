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
    private AgentController statsAgent;

    @Override
    protected void setup() {
        System.out.println("Agente Controlador (Orquestador) inicializado: " + getLocalName());

        try {
            AgentContainer container = (AgentContainer) getContainerController();

            filterAgent    = container.createNewAgent("FiltroIA",          "es.uni.mas.agents.FilterAgent",        null);
            uiAgent        = container.createNewAgent("InterfaceAgent",     "es.uni.mas.agents.UIAgent",            null);
            simulatorAgent = container.createNewAgent("SimuladorChat",      "es.uni.mas.agents.ChatSimulatorAgent", null);
            statsAgent     = container.createNewAgent("EstadisticasAgent",  "es.uni.mas.agents.StatsAgent",         null);

            filterAgent.start();
            uiAgent.start();
            simulatorAgent.start();
            statsAgent.start();

            // BUG FIX: The original code used Thread.sleep(1000) then sent a
            // STOP command to establish the default state. Two problems:
            //
            //   1. Thread.sleep() inside setup() blocks the JADE scheduler thread,
            //      potentially starving other agents during startup.
            //   2. The sleep is a fragile race: on a slow machine the agents may
            //      not be ready in 1 second; on a fast machine the sleep is wasted.
            //
            // Fix: simply remove both. FilterAgent already initialises with
            // studyModeActive = false, so no STOP command is needed at startup.
            // Each agent's own setup() is the right place to establish defaults.

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Listen for UI commands and broadcast them to the relevant agents
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    broadcastCommand(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    /**
     * Propagates a START/STOP command to all agents that care about it.
     * Currently only FilterAgent reacts; sending to others is harmless
     * and keeps the architecture open for extension.
     */
    private void broadcastCommand(String command) {
        System.out.println(getLocalName() + ": Propagando comando: " + command);
        ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
        forward.setContent(command);
        forward.addReceiver(new jade.core.AID("SimuladorChat",     jade.core.AID.ISLOCALNAME));
        forward.addReceiver(new jade.core.AID("FiltroIA",          jade.core.AID.ISLOCALNAME));
        forward.addReceiver(new jade.core.AID("EstadisticasAgent", jade.core.AID.ISLOCALNAME));
        send(forward);
    }

    @Override
    protected void takeDown() {
        System.out.println("Controlador finalizando. Limpiando agentes...");
        kill(filterAgent);
        kill(uiAgent);
        kill(simulatorAgent);
        kill(statsAgent);
    }

    private void kill(AgentController ac) {
        if (ac == null) return;
        try { ac.kill(); } catch (Exception e) { /* already dead */ }
    }
}