import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import trading.BookSellerAgent;
import trading.BuyerAgent;

import java.io.Serializable;
import java.util.ArrayList;

public class Simulation implements Serializable {

    private ArrayList<Trader> agents;
    private transient ContainerController container;

    private Simulation() { }

    public static void main(String[] args) throws StaleProxyException {
        Runtime jadeRuntime = Runtime.instance();

        System.out.println("Launching the platform Main Container");
        Profile profile = new ProfileImpl(null, 8888, null);
        profile.setParameter(Profile.GUI, "false");

        Simulation simulation = new Simulation();
        simulation.container = jadeRuntime.createMainContainer(profile);
        simulation.agents = createAgents(simulation.container);

    }

    private static ArrayList<Trader> createAgents(ContainerController containerController) throws StaleProxyException {
        ArrayList<Trader> agents = null;

        System.out.println("Starting up an agent..");
        BuyerAgent buyerAgent = new BuyerAgent();
        TradeAgentFactory.createTradeAgent(buyerAgent, containerController);

        System.out.println("Starting up an agent..");
        BookSellerAgent bookSellerAgent = new BookSellerAgent();
        TradeAgentFactory.createTradeAgent(bookSellerAgent, containerController);

        return agents;
    }

    public void startAll() throws StaleProxyException {
        for (Trader trader : agents) {
            trader.start();
        }
    }

    public void killAll() throws StaleProxyException {
        for (Trader trader : agents) {
            trader.kill();
        }
    }

}
