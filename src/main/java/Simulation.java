import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import trading.BidderAgent;
import trading.BuyerAgent;

import java.io.Serializable;
import java.util.ArrayList;

public class Simulation implements Serializable {

    private ArrayList<Trader> agents;
    private transient ContainerController container;

    private Simulation() { }

    public static void main(String[] args) throws StaleProxyException {
        Runtime jadeRuntime = Runtime.instance();

        Profile profile = new ProfileImpl(null, 8567, null);
        profile.setParameter(Profile.GUI, "false");

        Simulation simulation = new Simulation();
        simulation.container = jadeRuntime.createMainContainer(profile);
        simulation.agents = createAgents(simulation.container);

        simulation.startAll();

    }

    private static ArrayList<Trader> createAgents(ContainerController containerController) throws StaleProxyException {
        ArrayList<Trader> agents = new ArrayList<>();

        BuyerAgent buyerAgent = new BuyerAgent();

        agents.add(TradeAgentFactory.createTradeAgent("buyer1",buyerAgent, containerController));

        BuyerAgent buyerAgent2 = new BuyerAgent();

        agents.add(TradeAgentFactory.createTradeAgent("buyer2",buyerAgent2, containerController));

        BidderAgent bidderAgent = new BidderAgent();
        agents.add(TradeAgentFactory.createTradeAgent("seller1",bidderAgent, containerController));

        return agents;
    }

    public void startAll() throws StaleProxyException {
        for (Trader trader : agents) {
            System.out.println("Starting up " + trader.getNickname());
            trader.start();
        }
    }

    public void killAll() throws StaleProxyException {
        for (Trader trader : agents) {
            trader.kill();
        }
    }

}
