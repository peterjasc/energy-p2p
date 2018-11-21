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
    private static String walletFilePath = "";
    private static BigInteger roundId = BigInteger.ZERO;
    private HashMap<BigInteger, Bid> bidsForRounds = new HashMap<>();
    private DFHelper helper;

    private BigInteger discountFactorB = BigInteger.valueOf(90);

    // todo: subtract quantity
    protected void setup() {
        helper = DFHelper.getInstance();

        Object[] args = getArguments();
        if (args != null && args.length == 3) {


            String price = (String) args[0];
            String quantity = (String) args[1];
            walletFilePath = (String) args[2];
            String biddersAddress = getBiddersAddressFromWalletFilePath();
            log.debug("biddersAddress is " + biddersAddress);

            boolean haveBidHistory = false;

            roundId = findRoundIdFromLastBidEvent();

            if (NumberUtils.isDigits(price) && NumberUtils.isDigits(quantity)) {
                Bid bid = new Bid(NumberUtils.createBigInteger(price), NumberUtils.createBigInteger(quantity));
                log.info(getAID().getName() + " has issued a new offer" + bid + ".\n");
                if (!haveBidHistory) {
                    bidsForRounds.put(roundId, bid);
                } else {

                    Set<SmartContract.BidAcceptedEventResponse> logsForPenultimateRoundId
                            = getLogsForPenultimateRoundId(roundId);
                    log.debug("penultimateRoundId is " + roundId);
                    Bid maxGrossProfitFromPenultimateRound = getMaxGrossForBidSet(logsForPenultimateRoundId);
                    log.debug("maxGrossProfitFromPenultimateRound is " + maxGrossProfitFromPenultimateRound);

                    List<SmartContract.BidAcceptedEventResponse> buyersBidsInTheLastRoundIfExist
                            = getBiddersBidsInTheLastRoundIfExist(logsForPenultimateRoundId, biddersAddress);

                    if (!buyersBidsInTheLastRoundIfExist.isEmpty()) {
                        //todo: if there are more than one, then what?
                        log.debug("buyersBidsInTheLastRoundIfExist is not empty");
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
                        } else {
                            bid.setPrice(maxGrossProfitFromPenultimateRound.getPrice());
                        }
                    } else {
                        log.debug("buyersBidsInTheLastRoundIfExist is  empty");
                        BigDecimal priceMultipliedByDiscountValue
                                = new BigDecimal(maxGrossProfitFromPenultimateRound.getPrice())
                                .multiply(new BigDecimal(discountFactorB)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                        bid.setPrice(priceMultipliedByDiscountValue.toBigInteger());
                    }

                    bidsForRounds.put(roundId, bid);
                }

                ServiceDescription serviceDescription = new ServiceDescription();
                serviceDescription.setType("Bidder");
                serviceDescription.setName(getLocalName());
                helper.register(this, serviceDescription);
            } else {
                log.error("Percentage, quantity must be positive integers");
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
                message.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
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
            log.info(refuse.getSender().getName() + " has rejected the proposal.");
            helper.removeReceiverAgent(refuse.getSender(), refuse);
        }

        protected void handleFailure(ACLMessage failure) {
            globalResponses++;
            log.info(failure.getSender().getName() + " failed to reply.");
            helper.removeReceiverAgent(failure.getSender(), failure);
        }

        protected void handleInform(ACLMessage inform) {
            globalResponses++;
            log.info(getAID().getName() + " got confirmation for the offer");
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {
            int agentsLeft = responses.size() - globalResponses;
            globalResponses = 0;

            log.info(getAID().getName() + " got " + agentsLeft + " responses.");

            if (agentsLeft == 0) {
                return;
            }

            Bid oldBid = bidsForRounds.get(roundId);
            BigInteger bestPriceOffer = oldBid.getPrice();

            Vector<ACLMessage> cfps = new Vector<>();
            Enumeration<?> receivedResponses = responses.elements();

            ArrayList<ACLMessage> replies = new ArrayList<>();

            // todo:need the bestpriceoffer, but dont really need to set cfps, because they will be overwritten later anyway
            while (receivedResponses.hasMoreElements()) {
                ACLMessage msg = (ACLMessage) receivedResponses.nextElement();
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    BigInteger proposal = new BigInteger(msg.getContent());
                    ACLMessage reply = msg.createReply();
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
                    log.info(getAID().getName() + " will reject all but one offer, since they have received two or more offers of equal value");
                    setRepliesAcceptingJustOne(newBid, cfps, replies);
                    newIteration(cfps);
                }

            } else if (agentsLeft == 1) {
                if (newBid.getPrice().compareTo(bidsForRounds.get(roundId).getPrice()) >= 0) {
                    ACLMessage reply = new ACLMessage(ACLMessage.CFP);
                    log.info(getAID().getName()
                            + " will accept the price offered that is higher or equal to the ones received before");
                    bidsForRounds.put(roundId, newBid);
                    reply.setContent(getBiddersAddressFromWalletFilePath() + "|" + newBid.getPrice() + "|" + oldBid.getQuantity());
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    cfps.set(0, reply);
                    newIteration(cfps);
                } else {
                    log.info(getAID().getName() + " will ask for a better price: "
                            + bidsForRounds.get(roundId).getPrice() + " instead of "
                            + newBid.getPrice() + " (an offer they previously received from another party)");
                    newBid.setPrice(bidsForRounds.get(roundId).getPrice());
                    setReplies(newBid, cfps, replies);
                    newIteration(cfps);
                }

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
