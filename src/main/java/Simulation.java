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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Simulation implements Serializable {

    public static final String WALLET_HOME = "/home/peter/Documents/energy-p2p/private-testnet/keystore/";
    private ArrayList<Trader> agents;
    private transient ContainerController container;

    private static final Logger log = LoggerFactory.getLogger(Simulation.class);
    private ArrayList<String> wallets;

    private Simulation() {
        wallets = getWallets();
    }

    public static void main(String[] args) throws StaleProxyException {
        Runtime jadeRuntime = Runtime.instance();

        Profile profile = new ProfileImpl(null, 8567, null);
        profile.setParameter(Profile.GUI, "false");

        Simulation simulation = new Simulation();
        simulation.container = jadeRuntime.createMainContainer(profile);
        simulation.agents = simulation.createAgents(simulation.container);

        simulation.startAll();

    }

    private ArrayList<Trader> createAgents(ContainerController containerController) throws StaleProxyException {
        ArrayList<Trader> agents = new ArrayList<>();

        BuyerAgent buyerAgent = new BuyerAgent();
        agents.add(TradeAgentFactory.createTradeAgent("buyer1", buyerAgent, containerController,
                "20.0", "5", "6", WALLET_HOME + wallets.get(1)));

//        BuyerAgent buyerAgent2 = new BuyerAgent();
//
//        agents.add(TradeAgentFactory.createTradeAgent("buyer2", buyerAgent2, containerController,
//                "20", "10", "4", WALLET_HOME + "UTC--2018-11-24T19-34-55.937279473Z--f70eb6650142417be6d4887acb4d132fb784f8b2"));

        BidderAgent bidderAgent = new BidderAgent();
        agents.add(TradeAgentFactory.createTradeAgent("bidder1", bidderAgent, containerController,
                "20.0", "10",
                WALLET_HOME +
                        "UTC--2018-12-31T16-21-39.797496276Z--0d2914cd3618ba87836d51716f74cd52cb9f251a"));

//        BidderAgent bidderAgent2 = new BidderAgent();
//        agents.add(TradeAgentFactory.createTradeAgent("bidder2", bidderAgent2, containerController,
//                "20", "20",
//                WALLET_HOME +
//                        "UTC--2018-05-14T07-25-36.048259657Z--86d4f62e3053951089399ba3e8533b6f93498ae5"));
        return agents;
    }

    public ArrayList<String> getWallets() {
        File dir = new File(WALLET_HOME);
        ArrayList<String> wallets = new ArrayList<>();
        for (final File fileEntry : Objects.requireNonNull(dir.listFiles())) {
            wallets.add(fileEntry.getName());
        }
        return wallets;
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
