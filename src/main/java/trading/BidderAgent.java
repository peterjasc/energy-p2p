package trading;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import smartcontract.app.BiddersSubscriber;
import smartcontract.app.generated.SmartContract;

import java.math.BigDecimal;
import java.math.BigInteger;
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
    private String quantity = null;

    protected void setup() {
        helper = DFHelper.getInstance();

        Object[] args = getArguments();
        if (args.length == 2) {
            offer = (String) args[0];
            quantity = (String) args[1];

//            Subscriber<SmartContract.BidAcceptedEventResponse> subscriber = new BiddersSubscriber();
//            ContractLoader contractLoader = new ContractLoader("password",
//                    "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe");
//            SmartContract smartContract = contractLoader.loadContractWithSubscriber(subscriber);

            if (NumberUtils.isCreatable(offer) && NumberUtils.isDigits(quantity)) {
                BigDecimal initialOffer = NumberUtils.createBigDecimal(offer);

                log.info(getAID().getName() + " has issued a new offer, at $" + offer + " for " + quantity + ".\n");

                receivedOffers = new ArrayList<>();
                receivedOffers.add(initialOffer);

                ServiceDescription serviceDescription = new ServiceDescription();
                serviceDescription.setType("Bidder");
                serviceDescription.setName(getLocalName());
                helper.register(this, serviceDescription);
            } else {
                log.info("Payment must be a positive decimal number and quantity positive integer");
                log.info("Terminating: " + this.getAID().getName());
                doDelete();
            }
        } else {
            log.info("Two arguments required.");
            log.info("Terminating: " + this.getAID().getName());
            doDelete();
        }

        addBehaviour(new CustomContractNetInitiator(this, null));
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

            log.info("The Directory Facilitator found the following agents labeled as \"Buyer\": ");
            for (AID agent : agents) {
                log.info(agent.getName());
                init.addReceiver(new AID(agent.getLocalName(), AID.ISLOCALNAME));
            }

            if (agents.length == 0) {
                log.info("No agents matching the type were found. Terminating: "
                        + getAgent().getAID().getName());
                helper.killAgent(getAgent());
            } else {
                init.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
                init.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                init.setContent(getLocalName() + "|" + offer + "|" + quantity);

                messages.addElement(init);
            }

            return messages;
        }

        protected void handlePropose(ACLMessage propose, Vector v) {
            log.info(propose.getSender().getName() + " proposes $" + propose.getContent() + "\".");
        }

        protected void handleRefuse(ACLMessage refuse) {
            globalResponses++;
            log.info(refuse.getSender().getName() + " is not willing to bid any higher.");
            helper.removeReceiverAgent(refuse.getSender(), refuse);
        }

        protected void handleFailure(ACLMessage failure) {
            globalResponses++;
            log.info(failure.getSender().getName() + " failed to reply.");
            helper.removeReceiverAgent(failure.getSender(), failure);
        }

        protected void handleInform(ACLMessage inform) {
            globalResponses++;
            log.info("\n" + getAID().getName() + " has no stored power available.");
            for (Agent agent : helper.getRegisteredAgents()) {
                helper.killAgent(agent);
            }
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            int agentsLeft = responses.size() - globalResponses;
            globalResponses = 0;

            log.info("\n" + getAID().getName() + " got " + agentsLeft + " responses.");

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

                log.info(agentsLeft + " buyers are still bidding. Proceeding to the next round.");
                log.info(getAID().getName()
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
                log.info("No agent accepted the job.");
            }
        }

    }

}
