package es.uni.mas.agents;

import es.uni.mas.engine.RulesEngine;
import es.uni.mas.engine.ContactManager;
import es.uni.mas.model.ChatMessage;
import es.uni.mas.model.FilterEvent;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.ArrayList;
import java.util.List;

public class FilterAgent extends Agent {

    private RulesEngine       engine;
    private ContactManager    contactManager;
    private ACLMessage        currentMsg;
    private ChatMessage       chatMsg;
    private int               lastScore             = 0;
    private boolean           studyModeActive       = false;
    private String            currentConversationId;

    // Messages discarded during study mode, flushed to UI when mode turns off
    private final List<ChatMessage> discardedBuffer = new ArrayList<>();

    // True while the classify banner is open — incoming messages are forwarded
    // directly during this window instead of triggering a second popup.
    private boolean awaitingClassify = false;

    // Safety-net timeout: slightly longer than the GUI timer (8 s)
    private static final long CLASSIFY_TIMEOUT_MS = 9_000;

    private static final String WAITING          = "WAITING";
    private static final String ANALYZING        = "ANALYZING";
    private static final String FORWARDING       = "FORWARDING";
    private static final String DISCARDING       = "DISCARDING";
    private static final String FORWARD_AND_ASK  = "FORWARD_AND_ASK";

    @Override
    protected void setup() {
        System.out.println("Agente Filtro inicializado: " + getLocalName());
        engine = new RulesEngine("rules.xml");
        contactManager = ContactManager.getInstance();
        registerInDF();

        // ── Behaviour 1: START / STOP commands ───────────────────────────────
        // On STOP: flush the buffer so the user sees what was withheld.
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    boolean wasActive = studyModeActive;
                    studyModeActive = msg.getContent().startsWith("START");
                    System.out.println(getLocalName() + ": Modo estudio → " + studyModeActive);

                    // Turning study mode OFF → show the user everything that was held back
                    if (wasActive && !studyModeActive) {
                        flushDiscardedBuffer();
                    }
                } else {
                    block();
                }
            }
        });

        // ── Behaviour 2: FSM ──────────────────────────────────────────────────
        FSMBehaviour fsm = new FSMBehaviour(this);

        // ── WAITING ───────────────────────────────────────────────────────────
        fsm.registerFirstState(new SimpleBehaviour(this) {
            private boolean done = false;

            @Override
            public void action() {
                // Exclude any INFORM messages that carry a known ontology tag —
                // those belong to other behaviours (classify-response, stats-update…)
                // and must NOT be consumed here or chatMsg will end up null.
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.not(MessageTemplate.MatchOntology("classify-response"))
                );
                currentMsg = myAgent.receive(mt);
                if (currentMsg != null) {
                    try {
                        chatMsg = (ChatMessage) currentMsg.getContentObject();
                    } catch (UnreadableException e) {
                        chatMsg = null;
                    }
                    done = true;
                } else {
                    block();
                }
            }

            @Override public boolean done() { return done; }
            @Override public int    onEnd() { done = false; return (chatMsg != null) ? 1 : 0; }
        }, WAITING);

        // ── ANALYZING ─────────────────────────────────────────────────────────
        //
        // Contact check (during study mode):
        //   - WHITELIST → allow
        //   - BLACKLIST → block
        //   - DEFAULT → apply rules
        //
        // Study mode ON — three outcomes:
        //   0 → DISCARDING  (clearly distraction, borderline with no NB opinion or contact in blacklist)
        //   1 → FORWARDING  (clearly important, NB confirms borderline as important or contact in whitelist)
        //
        // Study mode OFF — two outcomes:
        //   1 → FORWARDING      (message passes through normally)
        //   3 → FORWARD_AND_ASK (passes through AND triggers the classify banner)
        //
        // The banner is deliberately suppressed during study mode to avoid
        // interrupting the user. Uncertain messages are instead buffered and
        // shown all at once when study mode is turned off.
        fsm.registerState(new OneShotBehaviour() {
            private int result;

            @Override
            public void action() {
                // Safety net: chatMsg should never be null here, but guard anyway
                // so a deserialization failure in WAITING just silently drops the
                // malformed message instead of crashing the agent.
                if (chatMsg == null) { result = -1; return; }

                String sender = chatMsg.getSender();

                if (!studyModeActive) {
                    // Study mode OFF: everything passes, but trigger the banner
                    // for borderline-uncertain messages so the system can learn —
                    // UNLESS the banner is already open, in which case just forward.
                    lastScore = engine.calculateScore(chatMsg);
                    boolean borderline = lastScore >= 0 && lastScore < engine.getThreshold();
                    boolean uncertain  = engine.isUncertain(chatMsg);
                    result = (borderline && uncertain && !awaitingClassify) ? 3 : 1;
                    return;
                }

                // Study mode ON: check contact list first, then apply rules                
                ContactManager.ContactStatus status = contactManager.checkContact(sender);

                if (status == ContactManager.ContactStatus.ALLOW) {
                    System.out.println(getLocalName() + ": [" + sender + "] en WHITELIST → PERMITIR");
                    result = 1;
                    return;
                }

                if (status == ContactManager.ContactStatus.BLOCK) {
                    System.out.println(getLocalName() + ": [" + sender + "] en BLACKLIST → BLOQUEAR");
                    result = 0;
                    return;
                }

                // DEFAULT: apply rules as normal
                lastScore = engine.calculateScore(chatMsg);
                System.out.println(getLocalName() + ": [" + sender
                        + "] usando REGLAS: score=" + lastScore + " umbral=" + engine.getThreshold());

                if (lastScore >= engine.getThreshold()) {
                    result = 1;                                              // rules: important
                } else if (lastScore < 0) {
                    result = 0;                                              // rules: distraction
                } else if (engine.isUncertain(chatMsg)) {
                    result = 0;                                              // borderline + NB blind → discard & buffer
                } else {
                    result = engine.classifierSaysImportant(chatMsg) ? 1 : 0; // NB tiebreaker
                }
            }

            @Override public int onEnd() { return result; }
        }, ANALYZING);

        // ── FORWARDING ────────────────────────────────────────────────────────
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println(getLocalName() + ": [APROBADO]");
                forwardToUI(null);
                notifyStats(true);
            }
        }, FORWARDING);

        // ── DISCARDING ────────────────────────────────────────────────────────
        // During study mode the discarded message is added to the buffer.
        // The buffer is flushed to the UI when study mode turns off.
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println(getLocalName() + ": [DESCARTADO] " + chatMsg);
                if (studyModeActive) {
                    discardedBuffer.add(chatMsg);
                }
                notifyStats(false);
            }
        }, DISCARDING);

        // ── FORWARD_AND_ASK ───────────────────────────────────────────────────
        // Only entered when study mode is OFF and the message is borderline-uncertain.
        // Forwards the message to the chat immediately, then fires the classify
        // banner so the user can optionally teach the system.
        // Sets awaitingClassify=true so the FSM can keep processing new messages
        // while the popup is open; the parallel ClassifyResponseBehaviour resets it.
        fsm.registerState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println(getLocalName() + ": [FORWARD+ASK] Reenviando y preguntando...");
                forwardToUI(null);          // message appears in chat right away
                awaitingClassify = true;    // block further popups until answered/timeout
                sendClassifyRequest();      // banner appears alongside it
            }
        }, FORWARD_AND_ASK);

        // ── Transitions ───────────────────────────────────────────────────────
        fsm.registerDefaultTransition(WAITING,         ANALYZING,       new String[]{WAITING, ANALYZING});
        fsm.registerTransition(      ANALYZING,        FORWARDING,      1);
        fsm.registerTransition(      ANALYZING,        DISCARDING,      0);
        fsm.registerTransition(      ANALYZING,        FORWARD_AND_ASK, 3);
        fsm.registerTransition(      ANALYZING,        WAITING,        -1, new String[]{WAITING, ANALYZING}); // null/bad message → skip
        fsm.registerDefaultTransition(FORWARD_AND_ASK, WAITING);   // back immediately; ClassifyResponseBehaviour handles the reply
        fsm.registerDefaultTransition(FORWARDING,      WAITING);
        fsm.registerDefaultTransition(DISCARDING,      WAITING);

        addBehaviour(fsm);

        // ── Behaviour 3: classify-response (parallel, never blocks the FSM) ──
        // Runs independently of the FSM so incoming messages keep flowing while
        // the popup is open. Resets awaitingClassify when the user answers or
        // the safety-net timeout fires.
        addBehaviour(new SimpleBehaviour(this) {
            private long waitStart = 0L;

            @Override
            public void action() {
                if (!awaitingClassify) {
                    block(200);   // nothing pending — sleep cheaply
                    return;
                }

                if (waitStart == 0L) waitStart = System.currentTimeMillis();

                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.and(
                                MessageTemplate.MatchOntology("classify-response"),
                                MessageTemplate.MatchConversationId(currentConversationId)
                        )
                );
                ACLMessage reply = myAgent.receive(mt);

                if (reply != null) {
                    String answer = reply.getContent();
                    if ("YES".equals(answer)) {
                        engine.addLearnedExample(engine.extractWords(chatMsg.getText()), true);
                    } else if ("NO".equals(answer)) {
                        engine.addLearnedExample(engine.extractWords(chatMsg.getText()), false);
                    }
                    awaitingClassify = false;
                    waitStart = 0L;

                } else if (System.currentTimeMillis() - waitStart > CLASSIFY_TIMEOUT_MS) {
                    System.out.println(getLocalName() + ": [CLASSIFY] Timeout de seguridad.");
                    awaitingClassify = false;
                    waitStart = 0L;
                } else {
                    block(200);
                }
            }

            @Override public boolean done() { return false; }  // runs for agent lifetime
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Sends all buffered discarded messages to UIAgent in one batch, sorted by priority. */
    private void flushDiscardedBuffer() {
        if (discardedBuffer.isEmpty()) return;

        System.out.println(getLocalName() + ": Ordenando " + discardedBuffer.size()
                + " mensajes retenidos usando TF-IDF (Vector Space Scoring)...");

        // === APLICACIÓN DE RANKED RETRIEVAL (Basado en IntroToIR.pdf) ===

        // 1. Collection Statistics: Calcular la Frecuencia de Documento (DF) para cada término
        java.util.Map<String, Double> idfMap = new java.util.HashMap<>();
        int N = discardedBuffer.size(); // Número total de documentos en la colección

        for (es.uni.mas.model.ChatMessage msg : discardedBuffer) {
            // Extraer términos únicos del documento
            java.util.Set<String> uniqueTerms = new java.util.HashSet<>(
                    java.util.Arrays.asList(msg.getText().toLowerCase().split("\\W+"))
            );
            for (String term : uniqueTerms) {
                if (term.length() > 2) { // Ignorar palabras muy cortas
                    idfMap.put(term, idfMap.getOrDefault(term, 0.0) + 1.0);
                }
            }
        }

        // 2. Weighting Scheme: Calcular el IDF (Frecuencia Inversa de Documento) -> log10(N/df)
        for (String term : idfMap.keySet()) {
            double df = idfMap.get(term);
            idfMap.put(term, Math.log10(N / df));
        }

        // 3. Ordenamos la lista evaluando el peso TF-IDF de cada mensaje
        discardedBuffer.sort((msg1, msg2) -> {

            // Calculamos el peso vectorial TF-IDF de cada documento
            double tfIdfScore1 = calculateTFIDF(msg1.getText(), idfMap);
            double tfIdfScore2 = calculateTFIDF(msg2.getText(), idfMap);

            // Sumamos la heurística de las reglas como base (Híbrido Simbólico + Vectorial)
            double finalScore1 = tfIdfScore1 + engine.calculateScore(msg1);
            double finalScore2 = tfIdfScore2 + engine.calculateScore(msg2);

            // Si hay empate estricto, desempatamos usando Naive Bayes
            if (finalScore1 == finalScore2) {
                boolean nb1 = engine.classifierSaysImportant(msg1);
                boolean nb2 = engine.classifierSaysImportant(msg2);
                if (nb1 && !nb2) return -1;
                if (!nb1 && nb2) return 1;
            }

            // Orden descendente (de mayor a menor relevancia)
            return Double.compare(finalScore2, finalScore1);
        });
        // ======================================

        try {
            jade.domain.FIPAAgentManagement.DFAgentDescription[] results = dfSearch("visualizacion-chat");
            if (results.length > 0) {
                jade.lang.acl.ACLMessage batch = new jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.INFORM);
                batch.addReceiver(results[0].getName());
                batch.setOntology("discarded-batch");
                batch.setContentObject(new java.util.ArrayList<>(discardedBuffer));
                send(batch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Vaciamos el buffer para la próxima vez
        discardedBuffer.clear();
    }

    /**
     * Calcula el peso TF-IDF de un documento basándose en las fórmulas de Recuperación de Información.
     */
    private double calculateTFIDF(String text, java.util.Map<String, Double> idfMap) {
        String[] terms = text.toLowerCase().split("\\W+");
        java.util.Map<String, Integer> tfMap = new java.util.HashMap<>();

        // Calcular Frecuencia del Término (TF) en el documento
        for (String term : terms) {
            if (term.length() > 2) {
                tfMap.put(term, tfMap.getOrDefault(term, 0) + 1);
            }
        }

        double docScore = 0.0;
        for (java.util.Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();

            // Esquema de peso logarítmico para TF: 1 + log10(tf)
            double tfWeight = 1.0 + Math.log10(tf);

            // Obtener el IDF calculado previamente para la colección
            double idfWeight = idfMap.getOrDefault(term, 0.0);

            // Peso final del término en el documento = TF * IDF
            docScore += (tfWeight * idfWeight);
        }

        return docScore;
    }

    private void sendClassifyRequest() {
        currentConversationId = getLocalName() + "_" + System.currentTimeMillis();
        try {
            DFAgentDescription[] results = dfSearch("visualizacion-chat");
            if (results.length > 0) {
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(results[0].getName());
                req.setOntology("classify-request");
                req.setConversationId(currentConversationId);
                req.setContentObject(chatMsg);
                send(req);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void forwardToUI(String ontology) {
        try {
            DFAgentDescription[] results = dfSearch("visualizacion-chat");
            if (results.length > 0) {
                ACLMessage fwd = new ACLMessage(ACLMessage.INFORM);
                fwd.addReceiver(results[0].getName());
                if (ontology != null) fwd.setOntology(ontology);
                fwd.setContentObject(chatMsg);
                send(fwd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyStats(boolean passed) {
        if (!studyModeActive) return;
        try {
            DFAgentDescription[] results = dfSearch("estadisticas-chat");
            if (results.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(results[0].getName());
                msg.setOntology("filter-event");
                msg.setContentObject(new FilterEvent(chatMsg, passed, lastScore));
                send(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DFAgentDescription[] dfSearch(String serviceType) throws FIPAException {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        template.addServices(sd);
        return DFService.search(this, template);
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
        try { DFService.deregister(this); } catch (FIPAException fe) { fe.printStackTrace(); }
        System.out.println("Agente Filtro finalizado.");
    }
}
