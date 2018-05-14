import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trading.BidderAgent;
import trading.BuyerAgent;
import trading.TradeAgentFactory;
import trading.Trader;

import java.io.Serializable;
import java.util.ArrayList;

public class Simulation implements Serializable {

    public static final String WALLET_HOME = "/home/peter/Documents/energy-p2p/private-testnet/keystore/";
    private ArrayList<Trader> agents;
    private transient ContainerController container;

    private static final Logger log = LoggerFactory.getLogger(Simulation.class);
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


        agents.add(TradeAgentFactory.createTradeAgent("buyer1",buyerAgent, containerController,
                "90", "15","1"));

        BuyerAgent buyerAgent2 = new BuyerAgent();

        agents.add(TradeAgentFactory.createTradeAgent("buyer2",buyerAgent2, containerController,
                "90", "15","1"));

        BidderAgent bidderAgent = new BidderAgent();
        agents.add(TradeAgentFactory.createTradeAgent("bidder1",bidderAgent, containerController,
                "300", "15",
                WALLET_HOME +
                        "UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe"));

        BidderAgent bidderAgent2 = new BidderAgent();
        agents.add(TradeAgentFactory.createTradeAgent("bidder2",bidderAgent, containerController,
                "300", "15",
                WALLET_HOME +
                        "UTC--2018-05-14T07-25-36.048259657Z--86d4f62e3053951089399ba3e8533b6f93498ae5"));
        return agents;
    }

    public void startAll() throws StaleProxyException {
        for (Trader trader : agents) {
            log.debug("Starting up " + trader.getNickname());
            trader.start();
        }
    }

    public void killAll() throws StaleProxyException {
        for (Trader trader : agents) {
            trader.kill();
        }
    }

}
