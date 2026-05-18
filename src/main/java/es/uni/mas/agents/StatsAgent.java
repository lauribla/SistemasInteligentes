package es.uni.mas.agents;

import es.uni.mas.model.FilterEvent;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passive statistics agent.
 *
 * Receives a FilterEvent from FilterAgent after every message decision,
 * accumulates counters, then pushes a formatted summary to UIAgent
 * so the GUI can display live stats.
 *
 * It is completely decoupled from the main filter/UI flow —
 * removing it does not break anything else.
 */
public class StatsAgent extends Agent {

    // Running counters
    private int totalReceived  = 0;
    private int totalPassed    = 0;
    private int totalDiscarded = 0;

    // Message count per sender (insertion-ordered for display stability)
    private final Map<String, Integer> bySender = new LinkedHashMap<>();

    @Override
    protected void setup() {
        System.out.println("Agente Estadísticas inicializado: " + getLocalName());
        registerInDF();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Only handle filter-event ontology messages
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology("filter-event")
                );
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    try {
                        FilterEvent event = (FilterEvent) msg.getContentObject();
                        accumulate(event);
                        pushToUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    private void accumulate(FilterEvent event) {
        totalReceived++;
        if (event.isPassed()) totalPassed++;
        else                   totalDiscarded++;

        String sender = event.getMessage().getSender();
        bySender.merge(sender, 1, Integer::sum);
    }

    // -------------------------------------------------------------------------
    private void pushToUI() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("visualizacion-chat");
            template.addServices(sd);
            DFAgentDescription[] results = DFService.search(this, template);

            if (results.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(results[0].getName());
                msg.setOntology("stats-update");   // UIAgent uses this to route the message
                msg.setContent(buildSummary());
                send(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    private String buildSummary() {
        int filterRate = (totalReceived > 0)
                ? (totalDiscarded * 100) / totalReceived
                : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Recibidos: %d  |  Pasados: %d  |  Filtrados: %d  |  Tasa: %d%%",
                totalReceived, totalPassed, totalDiscarded, filterRate
        ));

        // Top 4 senders by message count
        sb.append("\n");
        bySender.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(4)
                .forEach(e -> sb.append(String.format("  [%s: %d]  ", e.getKey(), e.getValue())));

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    private void registerInDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("estadisticas-chat");
        sd.setName("servicio-estadisticas");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
        System.out.println("Agente Estadísticas finalizado.");
    }
}