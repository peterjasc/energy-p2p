package trading;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import rx.Subscriber;
import smartcontract.app.BiddersSubscriber;
import smartcontract.app.BuyersSubscriber;
import smartcontract.app.generated.SmartContract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class BidderAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BidderAgent.class);

    private ArrayList<BigDecimal> receivedOffers;
    private DFHelper helper;
    private String offer = null;

    protected void setup() {
        helper = DFHelper.getInstance();

        Object[] args = getArguments();
        if (args.length == 1) {
            offer = (String) args[0];

            loadContractFromChain();

            if (offer.matches("^(\\d|.)+$")) {
                BigDecimal initialOffer = new BigDecimal(offer);
                System.out.println(getAID().getName() + " has issued a new offer, at $" + offer + ".\n");

                receivedOffers = new ArrayList<>();
                receivedOffers.add(initialOffer);

                ServiceDescription serviceDescription = new ServiceDescription();
                serviceDescription.setType("Bidder");
                serviceDescription.setName(getLocalName());
                helper.register(this, serviceDescription);
            } else {
                System.out.println("Payment must be a positive decimal number.");
                System.out.println("Terminating: " + this.getAID().getName());
                doDelete();
            }
        } else {
            System.out.println("One argument required. Please provide a floating point number.");
            System.out.println("Terminating: " + this.getAID().getName());
            doDelete();
        }

        addBehaviour(new CustomContractNetInitiator(this, null));
    }

    private void loadContractFromChain() {
        ChainConnector chainConnector = new ChainConnector().invoke("password",
                "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe");
        Web3j web3j = chainConnector.getWeb3j();
        Credentials credentials = chainConnector.getCredentials();
        Subscriber<SmartContract.BidAcceptedEventResponse> subscriber = new BiddersSubscriber();
        SmartContract contract = new ContractLoader(web3j, credentials).invoke(subscriber);
    }

    private class CustomContractNetInitiator extends ContractNetInitiator {
        private static final long serialVersionUID = 1L;
        private int globalResponses;

        CustomContractNetInitiator(Agent agent, ACLMessage aclMessage) {
            super(agent, aclMessage);
            globalResponses = 0;
        }

        public Vector<ACLMessage> prepareCfps(ACLMessage init) {
            init = new ACLMessage(ACLMessage.CFP);
            Vector<ACLMessage> messages = new Vector<>();

            AID[] agents = helper.searchDF(getAgent(), "Buyer");

            System.out.println("The Directory Facilitator found the following agents labeled as \"Buyer\": ");
            for (AID agent : agents) {
                System.out.println(agent.getName());
                init.addReceiver(new AID(agent.getLocalName(), AID.ISLOCALNAME));
            }
            System.out.println();

            if (agents.length == 0) {
                System.out.println("No agents matching the type were found. Terminating: "
                        + getAgent().getAID().getName());
                helper.killAgent(getAgent());
            } else {
                init.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
                init.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                init.setContent(getLocalName() + "|" + offer);

                messages.addElement(init);
            }

            return messages;
        }

        protected void handlePropose(ACLMessage propose, Vector v) {
            System.out.println(propose.getSender().getName() + " proposes $" + propose.getContent() + "\".");
        }

        protected void handleRefuse(ACLMessage refuse) {
            globalResponses++;
            System.out.println(refuse.getSender().getName() + " is not willing to bid any higher.");
            helper.removeReceiverAgent(refuse.getSender(), refuse);
        }

        protected void handleFailure(ACLMessage failure) {
            globalResponses++;
            System.out.println(failure.getSender().getName() + " failed to reply.");
            helper.removeReceiverAgent(failure.getSender(), failure);
        }

        protected void handleInform(ACLMessage inform) {
            globalResponses++;
            System.out.println("\n" + getAID().getName() + " has no stored power available.");
            for (Agent agent : helper.getRegisteredAgents()) {
                helper.killAgent(agent);
            }
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            int agentsLeft = responses.size() - globalResponses;
            globalResponses = 0;

            System.out.println("\n" + getAID().getName() + " got " + agentsLeft + " responses.");

            BigDecimal bestProposal = new BigDecimal(offer);
            ACLMessage reply = new ACLMessage(ACLMessage.CFP);
            Vector<ACLMessage> cfps = new Vector<>();
            Enumeration<?> receivedResponses = responses.elements();

            ArrayList<ACLMessage> replies = new ArrayList<>();

            while (receivedResponses.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) receivedResponses.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    BigDecimal proposal = new BigDecimal(msg.getContent());
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CFP);
                    replies.add(reply);
                    if (proposal.compareTo(bestProposal) > 0) {
                        bestProposal = proposal;
                    }
                    cfps.addElement(reply);
                }
            }
            if (agentsLeft > 1) {
                receivedOffers.add(bestProposal);

                for (int i = 0; i < replies.size(); i++) {
                    replies.get(i).setContent(getLocalName() + "|" + bestProposal);
                    cfps.set(i, replies.get(i));
                }

                System.out.println(agentsLeft + " buyers are still bidding. Proceeding to the next round.");
                System.out.println(getAID().getName()
                        + " is issuing CFP's with a offer of $"
                        + receivedOffers.get(receivedOffers.size() - 1) + ".\n");
                newIteration(cfps);
            } else if (agentsLeft == 1) {
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                if (bestProposal.compareTo(receivedOffers.get(receivedOffers.size() - 1)) >= 0) {
                    reply.setContent(getLocalName() + "|" + bestProposal);
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                }
                acceptances.addElement(reply);
            } else {
                System.out.println("No agent accepted the job.");
            }
        }

    }

}
