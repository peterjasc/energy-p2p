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
import smartcontract.app.generated.SmartContract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class BidderAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BidderAgent.class);
    public static String walletFilePath = "";
    private static BigInteger roundId = BigInteger.ZERO;
    private HashMap<BigInteger, Bid> bidsForRounds = new HashMap<>();
    private DFHelper helper;

    private BigInteger discountFactorB = BigInteger.valueOf(90);

    protected void setup() {
        helper = DFHelper.getInstance();

        Object[] args = getArguments();
        if (args != null && args.length == 3) {


            String price = (String) args[0];
            String quantity = (String) args[1];
            walletFilePath = (String) args[2];
            String biddersAddress = getBiddersAddressFromWalletFilePath();
            log.debug("biddersAddress is " + biddersAddress);

            boolean haveBidHistory = true;

            roundId = findRoundIdFromLastBidEvent();

            if (NumberUtils.isDigits(price) && NumberUtils.isDigits(quantity)) {
                Bid bid = new Bid(NumberUtils.createBigInteger(price), NumberUtils.createBigInteger(quantity));
                log.info(getAID().getName() + " has issued a new offer" + bid + ".\n");
                if (!haveBidHistory) {
                    bidsForRounds.put(roundId, bid);
                } else {
                    Set<SmartContract.BidAcceptedEventResponse> logsForPenultimateRoundId
                            = getLogsForPenultimateRoundId(roundId);
                    Bid maxGrossProfitFromPenultimateRound = getMaxGrossForBidSet(logsForPenultimateRoundId);

                    List<SmartContract.BidAcceptedEventResponse> buyersBidsInTheLastRoundIfExist
                            = getBiddersBidsInTheLastRoundIfExist(logsForPenultimateRoundId, biddersAddress);

                    if (!buyersBidsInTheLastRoundIfExist.isEmpty()) {
                        //todo: if there are more than one, then what?
                        SmartContract.BidAcceptedEventResponse buyersBidsInTheLastRound = buyersBidsInTheLastRoundIfExist.get(0);

                        BigDecimal soldCapacityDividedByAvailableCapacity
                                = new BigDecimal(buyersBidsInTheLastRound.quantity)
                                .divide(new BigDecimal(maxGrossProfitFromPenultimateRound.getQuantity()), RoundingMode.HALF_UP);

                        if (soldCapacityDividedByAvailableCapacity.compareTo(new BigDecimal("0.25")) <= 0) {
                            //  will lose a bit of precision here
                            BigDecimal priceMultipliedByDiscountValue
                                    = new BigDecimal(maxGrossProfitFromPenultimateRound.getPrice())
                                        .multiply(new BigDecimal(discountFactorB)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                            bid.setPrice(priceMultipliedByDiscountValue.toBigInteger());
                        }
                    } else {
                        BigDecimal priceMultipliedByDiscountValue
                                = new BigDecimal(maxGrossProfitFromPenultimateRound.getPrice())
                                .multiply(new BigDecimal(discountFactorB)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                        bid.setPrice(priceMultipliedByDiscountValue.toBigInteger());
                    }

                    bidsForRounds.put(roundId, maxGrossProfitFromPenultimateRound);
                }

                ServiceDescription serviceDescription = new ServiceDescription();
                serviceDescription.setType("Bidder");
                serviceDescription.setName(getLocalName());
                helper.register(this, serviceDescription);
            } else {
                log.error("Payment must be a positive decimal number and quantity positive integer");
                log.error("Terminating: " + this.getAID().getName());
                doDelete();
            }
        } else {
            log.error("Two arguments required.");
            log.error("Terminating: " + this.getAID().getName());
            doDelete();
        }

        addBehaviour(new CustomContractNetInitiator(this, null));
    }

    private String getBiddersAddressFromWalletFilePath() {
        if (!walletFilePath.equals("")) {
            return walletFilePath.substring(walletFilePath.lastIndexOf("--") + 2);
        } else {
            log.error("Wallet file path is empty!");
            return "";
        }
    }

    private ContractLoader getContractLoaderForThisAgent() {
        return new ContractLoader("password", walletFilePath);
    }

    private Set<SmartContract.BidAcceptedEventResponse> getLogsForPenultimateRoundId(BigInteger roundId) {
        ContractLoader contractLoader = getContractLoaderForThisAgent();
        SmartContract smartContract = contractLoader.loadContract();
        return contractLoader.getLogsForRoundId(roundId.subtract(BigInteger.ONE), smartContract);
    }

    private Bid getMaxGrossForBidSet(Set<SmartContract.BidAcceptedEventResponse> bids) {
        BigInteger maxGrossProfit = BigInteger.ZERO;
        Bid maxGrossProfitBid = new Bid(BigInteger.ZERO, BigInteger.ZERO);

        for (SmartContract.BidAcceptedEventResponse bid : bids) {
            BigInteger grossProfit = bid.quantity.multiply(bid.price);
            if (grossProfit.compareTo(maxGrossProfit) > 0) {
                maxGrossProfit = grossProfit;
                maxGrossProfitBid = new Bid(bid.price, bid.quantity);
            }
        }
        return maxGrossProfitBid;
    }

    private List<SmartContract.BidAcceptedEventResponse> getBiddersBidsInTheLastRoundIfExist(Set<SmartContract.BidAcceptedEventResponse> bids,
                                                                                             String biddersAddress) {
        return bids.stream()
                .filter(x -> x.bidder.equalsIgnoreCase(biddersAddress))
                .collect(Collectors.toList());
    }

    private BigInteger findRoundIdFromLastBidEvent() {
        ContractLoader contractLoader = getContractLoaderForThisAgent();
        SmartContract smartContract = contractLoader.loadContract();
        return contractLoader.findRoundIdFromLastBidEvent(smartContract);
    }


    private class CustomContractNetInitiator extends ContractNetInitiator {
        private static final long serialVersionUID = 1L;
        private int globalResponses;

        CustomContractNetInitiator(Agent agent, ACLMessage aclMessage) {
            super(agent, aclMessage);
            globalResponses = 0;
        }

        public Vector<ACLMessage> prepareCfps(ACLMessage message) {
            message = new ACLMessage(ACLMessage.CFP);
            Vector<ACLMessage> messages = new Vector<>();

            AID[] agents = helper.searchDF(getAgent(), "Buyer");

            log.debug("The Directory Facilitator found the following agents labeled as \"Buyer\": ");
            for (AID agent : agents) {
                log.debug(agent.getName());
                message.addReceiver(new AID(agent.getLocalName(), AID.ISLOCALNAME));
            }

            if (agents.length == 0) {
                log.info("No agents matching the type were found. Terminating: "
                        + getAgent().getAID().getName());
                helper.killAgent(getAgent());
            } else {
                message.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
                message.setReplyByDate(new Date(System.currentTimeMillis() + 10000));

                roundId = findRoundIdFromLastBidEvent();
                Bid bid = bidsForRounds.get(roundId);
                message.setContent(getBiddersAddressFromWalletFilePath() + "|" + bid.getPrice() + "|" + bid.getQuantity());

                messages.addElement(message);
            }

            return messages;
        }

        protected void handlePropose(ACLMessage propose, Vector v) {
            log.info(propose.getSender().getName() + " proposes $" + propose.getContent() + "\".");
        }

        protected void handleRefuse(ACLMessage refuse) {
            globalResponses++;
            log.info(refuse.getSender().getName() + " is not willing to buy any lower.");
            helper.removeReceiverAgent(refuse.getSender(), refuse);
        }

        protected void handleFailure(ACLMessage failure) {
            globalResponses++;
            log.info(failure.getSender().getName() + " failed to reply.");
            helper.removeReceiverAgent(failure.getSender(), failure);
        }

        protected void handleInform(ACLMessage inform) {
            globalResponses++;
            log.info(getAID().getName() + " has no stored power available.");
            helper.killAgent(myAgent);
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            int agentsLeft = responses.size() - globalResponses;
            globalResponses = 0;

            log.info(getAID().getName() + " got " + agentsLeft + " responses.");

            Bid oldBid = bidsForRounds.get(roundId);
            BigInteger bestPriceOffer = oldBid.getPrice();

            ACLMessage reply = new ACLMessage(ACLMessage.CFP);
            Vector<ACLMessage> cfps = new Vector<>();
            Enumeration<?> receivedResponses = responses.elements();

            ArrayList<ACLMessage> replies = new ArrayList<>();

            while (receivedResponses.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) receivedResponses.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    BigInteger proposal = new BigInteger(msg.getContent());
                    reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CFP);
                    replies.add(reply);
                    if (proposal.compareTo(bestPriceOffer) < 0) {
                        bestPriceOffer = proposal;
                    }
                    cfps.addElement(reply);
                }
            }

            Bid newBid = new Bid(bestPriceOffer, oldBid.getQuantity());
            if (agentsLeft > 1) {
                bidsForRounds.put(roundId, newBid);

                boolean offersAreEqual = checkIfOffersAreEqual(responses);
                if (!offersAreEqual) {
                    setReplies(newBid, cfps, replies);

                    log.info(agentsLeft + " buyers are still bidding in the current round. Moving on to the next iteration.");
                    log.info(getAID().getName()
                            + " is issuing CFP's of "
                            + bidsForRounds.get(roundId) + ".\n");
                    newIteration(cfps);
                } else {
                    setRepliesAcceptingJustOne(newBid, cfps, replies);
                    newIteration(cfps);
                }

            } else if (agentsLeft == 1) {
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);

                if (bestPriceOffer.compareTo(bidsForRounds.get(roundId).getPrice()) >= 0) {
                    bidsForRounds.put(roundId, newBid);
                    reply.setContent(getBiddersAddressFromWalletFilePath() + "|" + bestPriceOffer + "|" + oldBid.getQuantity());
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                }

                acceptances.addElement(reply);
            } else {
                log.info("No agent accepted the job.");
            }
        }

        private boolean checkIfOffersAreEqual(Vector responses) {
            Enumeration<?> receivedResponses2 = responses.elements();

            BigDecimal bestOffer = BigDecimal.ZERO;
            if (receivedResponses2.hasMoreElements()) {
                bestOffer = new BigDecimal(((ACLMessage) receivedResponses2.nextElement()).getContent());
            }

            boolean offersAreEqual = false;
            while (receivedResponses2.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) receivedResponses2.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    BigDecimal proposal = new BigDecimal(msg.getContent());
                    offersAreEqual = proposal.compareTo(bestOffer) == 0;
                }
            }
            return offersAreEqual;
        }

        private void setReplies(Bid newBid, Vector<ACLMessage> cfps, ArrayList<ACLMessage> replies) {
            for (int replyToBuyerIndex = 0; replyToBuyerIndex < replies.size(); replyToBuyerIndex++) {
                replies
                        .get(replyToBuyerIndex)
                        .setContent(getBiddersAddressFromWalletFilePath() + "|" + newBid.getPrice() + "|" + newBid.getQuantity());
                cfps.set(replyToBuyerIndex, replies.get(replyToBuyerIndex));
            }
        }

        private void setRepliesAcceptingJustOne(Bid newBid, Vector<ACLMessage> cfps, ArrayList<ACLMessage> replies) {
            for (int replyToBuyerIndex = 0; replyToBuyerIndex < replies.size(); replyToBuyerIndex++) {
                ACLMessage replyToBuyer = replies.get(replyToBuyerIndex);

                if (replyToBuyerIndex == 0) {
                    replyToBuyer
                            .setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    replyToBuyer
                            .setContent(getBiddersAddressFromWalletFilePath() + "|" + newBid.getPrice() + "|" + newBid.getQuantity());
                } else {
                    replyToBuyer.setPerformative(ACLMessage.REJECT_PROPOSAL);
                }
                cfps.set(replyToBuyerIndex, replyToBuyer);
            }
        }

    }

}
