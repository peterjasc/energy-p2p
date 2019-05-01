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
        int round_id = 34;
        int wallet_id = 0;
        int buyer = 0;
        int bidder = 0;

        for (int i = 0; i < 10; i++) {
            RoundHelper.setRoundId(BigInteger.valueOf(round_id));
            ArrayList<Trader> agents = new ArrayList<>();

            int counter = 0;
            // ratio, quantity
            for (; counter % 10 != 0 || counter == 0; counter++, buyer++) {
                wallet_id += 1;
                BuyerAgent buyerAgent = new BuyerAgent();
                agents.add(TradeAgentFactory.createTradeAgent("buyer"+buyer, buyerAgent, containerController,
                        "20.0", "10", WALLET_HOME + wallets.get(wallet_id)));
            }

            int counter2 = 0;
            // ratio, quantity, price
            for (; counter2 % 10 != 0 || counter2 == 0; counter2++, bidder++) {
                wallet_id += 1;
                BidderAgent bidderAgent = new BidderAgent();
                agents.add(TradeAgentFactory.createTradeAgent("bidder"+bidder, bidderAgent, containerController,
                        "10.0", "10", WALLET_HOME + wallets.get(wallet_id)));
            }

            startAll(agents);

            try {
                System.out.println("Index no: " + i + ". Press enter to continue:\n");
                int in = System.in.read();
                System.out.println(in);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }

            round_id += 1;
        }
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
            trader.start();
        }
    }

    public void killAll(ArrayList<Trader> agents) throws StaleProxyException {
        for (Trader trader : agents) {
            trader.kill();
        }
    }

}
