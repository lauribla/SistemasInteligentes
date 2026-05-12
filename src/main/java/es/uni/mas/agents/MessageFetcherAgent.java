package es.uni.mas.agents;

import es.uni.mas.model.ChatMessage;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.Random;

public class MessageFetcherAgent extends Agent {

    // Datos realistas emparejados
    private static final String[][] REALISTIC_DATA = {
        {"Profesor", "Recordad que el examen es el lunes"},
        {"Profesor", "He subido el PDF con los apuntes de JADE"},
        {"AmigoCercano", "¡URGENTE! Quedada en la biblioteca a las 10"},
        {"AmigoCercano", "Pasadme la práctica de Sistemas Inteligentes"},
        {"AmigoCercano", "¿Vais a salir hoy de fiesta?"},
        {"GrupoUni", "Cervezas en el bar de abajo al acabar"},
        {"GrupoUni", "Alguien ha visto mi paraguas?"},
        {"GrupoFamilia", "Hola hijo, ¿vas a venir a comer?"},
        {"GrupoFamilia", "Mira este sticker de un perrito jajaja"},
        {"Desconocido", "Venta de criptomonedas garantizada"},
        {"Desconocido", "Has ganado un premio, pulsa aquí"}
    };

    @Override
    protected void setup() {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                // 1. Seleccionar un par realista
                Random r = new Random();
                String[] pair = REALISTIC_DATA[r.nextInt(REALISTIC_DATA.length)];
                ChatMessage chatMsg = new ChatMessage(pair[0], pair[1]);

                // 2. Buscar al Agente Filtro en el DF
                try {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("filtrado-spam");
                    template.addServices(sd);
                    DFAgentDescription[] results = DFService.search(myAgent, template);

                    if (results.length > 0) {
                        ACLMessage acl = new ACLMessage(ACLMessage.INFORM);
                        acl.addReceiver(results[0].getName());
                        acl.setContentObject(chatMsg);
                        send(acl);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                doDelete();
            }
        });
    }
}
