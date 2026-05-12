package es.uni.mas.agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ChatSimulatorAgent extends Agent {
    private TickerBehaviour simulatorTicker;
    private boolean isRunning = false;

    @Override
    protected void setup() {
        System.out.println("Agente Simulador de Chat inicializado: " + getLocalName());
        
        // Arrancamos la simulación automáticamente
        startSimulation();

        // Ya no necesitamos escuchar START/STOP para detener el tráfico, 
        // pero mantenemos el comportamiento por si queremos ampliarlo.
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    // El simulador ahora es siempre-encendido para simular un chat real
                } else {
                    block();
                }
            }
        });
    }

    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;
            simulatorTicker = new TickerBehaviour(this, 3000) {
                @Override
                protected void onTick() {
                    createMessageFetcher();
                }
            };
            addBehaviour(simulatorTicker);
        }
    }

    private void createMessageFetcher() {
        try {
            AgentContainer container = (AgentContainer) getContainerController();
            // Nombre aleatorio para el agente efímero
            String name = "Fetcher_" + System.currentTimeMillis();
            AgentController fetcher = container.createNewAgent(name, "es.uni.mas.agents.MessageFetcherAgent", null);
            fetcher.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
