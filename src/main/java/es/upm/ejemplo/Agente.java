package es.upm.ejemplo;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
public class Agente extends Agent {
    protected CyclicBehaviour cyclicBehaviour;
    public void setup()
    {
        System.out.println("Soy el Agente 1");
        cyclicBehaviour = new CyclicBehaviour(this)
        {
            public void action()
            {
                block();
            }
        };
        addBehaviour(cyclicBehaviour);
    }
}