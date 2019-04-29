import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trading.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;

public class Simulation implements Serializable {

    public static final String WALLET_HOME = "/home/peter/Documents/energy-p2p/private-testnet/keystore/";
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
        simulation.createAgents(simulation.container);
    }

    private void createAgents(ContainerController containerController) throws StaleProxyException {
        int wallet_id = 0;
        int buyer = 0;
        int bidder = 0;
        RoundHelper.setRoundId(BigInteger.valueOf(34));

//            todo: for some reason, JADE won't allow us to create new agents, once the agents have started bidding
//        for (int i = 0; i < 10; i++) {
            ArrayList<Trader> agents = new ArrayList<>();

            // ratio, quantity
            for (; buyer % 10 != 0 || buyer == 0; buyer++) {
                wallet_id += 1;
                BuyerAgent buyerAgent = new BuyerAgent();
                agents.add(TradeAgentFactory.createTradeAgent("buyer"+buyer, buyerAgent, containerController,
                        "20.0", "10", WALLET_HOME + wallets.get(wallet_id)));
            }

            // ratio, quantity, price
            for (; bidder % 10 != 0 || bidder == 0; bidder++) {
                wallet_id = bidder % 10;
                BidderAgent bidderAgent = new BidderAgent();
                agents.add(TradeAgentFactory.createTradeAgent("bidder"+bidder, bidderAgent, containerController,
                        "10.0", "10", "100", WALLET_HOME + wallets.get(wallet_id)));
            }

            startAll(agents);
//            try {
//                System.out.println("Press enter to continue:\n");
//                int in = System.in.read();
//                System.out.println(in);
//            } catch (IOException e) {
//                e.printStackTrace();
//                System.exit(0);
//            }
//
//            round_id += 1;
//        }
    }

    public ArrayList<String> getWallets() {
        File dir = new File(WALLET_HOME);
        ArrayList<String> wallets = new ArrayList<>();
        for (final File fileEntry : Objects.requireNonNull(dir.listFiles())) {
            wallets.add(fileEntry.getName());
        }
        return wallets;
    }

    public void startAll(ArrayList<Trader> agents) throws StaleProxyException {
        for (Trader trader : agents) {
            log.debug("Starting up " + trader.getNickname());
            trader.start();
        }
    }

    public void killAll(ArrayList<Trader> agents) throws StaleProxyException {
        for (Trader trader : agents) {
            trader.kill();
        }
    }

}
