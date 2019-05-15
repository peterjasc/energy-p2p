package trading;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.BigDecimalValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartcontract.app.generated.SmartContract;
import trading.cron.AgentTask;
import trading.cron.TaskedAgent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class BidderAgent extends Agent implements TaskedAgent {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BidderAgent.class);
    private String walletFilePath = "";
    private BigInteger quantityToSell = BigInteger.ZERO;
    private BigDecimal priceToQuantityRatio = BigDecimal.ZERO;
    private HashMap<BigInteger, Bid> bidsForRounds = new HashMap<>();
    private DFHelper helper;

    private BigInteger discountFactorB = BigInteger.valueOf(90);

    private BigInteger roundId = BigInteger.ZERO;

    private static final Semaphore semaphore = new Semaphore(3, true);

    private boolean secondTimeQuantityIsZero;

    public boolean isSecondTimeQuantityIsZero() {
        return secondTimeQuantityIsZero;
    }

    public void setSecondTimeQuantityIsZero(boolean secondTimeQuantityIsZero) {
        this.secondTimeQuantityIsZero = secondTimeQuantityIsZero;
    }

    private BigInteger getRoundID() {
        return RoundHelper.getRoundId();
    }

    public BigInteger getQuantity() {
        return quantityToSell;
    }

    protected void setup() {
        helper = DFHelper.getInstance();

        Object[] args = getArguments();
        if (args != null && args.length == 3) {


            String ratio = (String) args[0];
            String quantity = (String) args[1];
            walletFilePath = (String) args[2];

            if (BigDecimalValidator.getInstance().validate(ratio) != null
                    && NumberUtils.isDigits(quantity)) {
                roundId = RoundHelper.getRoundId();
                quantityToSell = new BigInteger(quantity);
                priceToQuantityRatio = new BigDecimal(ratio);

                Bid bid = calculateBid();
                bidsForRounds.put(roundId, bid);
                log.info(getAID().getName() + " will issue a new offer" + bid);

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
            log.error("Wrong number of arguments.");
            log.error("Terminating: " + this.getAID().getName());
            doDelete();
        }
        Timer t = new Timer();
        AgentTask mTask = new AgentTask(this);
        t.scheduleAtFixedRate(mTask, 0, 21000);
    }

    private Bid calculateBid() {
        String biddersAddress = getBiddersAddressFromWalletFilePath(walletFilePath);
        BigDecimal price;
        BigInteger quantity;

        Set<SmartContract.BidAcceptedEventResponse> logsForPenultimateRoundId
                = getLogsForPreviousRoundId(roundId);
        log.debug("current RoundId is " + roundId);
        Bid bestBidFromLastRound = getBestBidFromBidSet(logsForPenultimateRoundId);
        log.debug("bestBidFromLastRound is " + bestBidFromLastRound);

        List<SmartContract.BidAcceptedEventResponse> biddersAcceptedBidsInTheLastRound
                = getBiddersBidsInTheLastRoundIfExist(logsForPenultimateRoundId, biddersAddress);


        if (!biddersAcceptedBidsInTheLastRound.isEmpty()) {
            BigInteger soldCapacityInTheLastRound = BigInteger.ZERO;
            for (SmartContract.BidAcceptedEventResponse biddersAcceptedBids : biddersAcceptedBidsInTheLastRound) {
                soldCapacityInTheLastRound = soldCapacityInTheLastRound.add(biddersAcceptedBids.quantity);
            }

            BigDecimal soldCapacityDividedByAvailableCapacity = new BigDecimal(soldCapacityInTheLastRound)
                    .divide(new BigDecimal(quantityToSell), RoundingMode.HALF_UP);

            log.debug("soldCapacityDividedByAvailableCapacity is " + soldCapacityDividedByAvailableCapacity);

            if (soldCapacityDividedByAvailableCapacity.compareTo(new BigDecimal("0.25")) <= 0) {
                BigDecimal discountPrice = getDiscountPrice(bestBidFromLastRound);
                quantity = bestBidFromLastRound.getQuantity();
                if (quantity.compareTo(quantityToSell) > 0) {
                    quantity = quantityToSell;
                    price = bestBidFromLastRound.getPrice();
                } else if (quantity.compareTo(BigInteger.ZERO) != 0 && discountPrice
                        .compareTo(priceToQuantityRatio.multiply(new BigDecimal(quantity))) >= 0) {
                    price = discountPrice;
                } else if (bidsForRounds.get(roundId.subtract(BigInteger.ONE)) != null) {
                    price = getDiscountPrice(bidsForRounds.get(roundId.subtract(BigInteger.ONE)));
                    quantity = bidsForRounds.get(roundId.subtract(BigInteger.ONE)).getQuantity();
                } else {
                    price = priceToQuantityRatio.multiply(new BigDecimal(quantity));
                }
            } else {
                if (bestBidFromLastRound.getPrice()
                        .compareTo(priceToQuantityRatio.multiply(new BigDecimal(quantityToSell))) >= 0) {
                    price = bestBidFromLastRound.getPrice();
                    quantity = bestBidFromLastRound.getQuantity();
                } else if (bidsForRounds.get(roundId.subtract(BigInteger.ONE)) != null) {
                    price = bidsForRounds.get(roundId.subtract(BigInteger.ONE)).getPrice();
                    quantity = bidsForRounds.get(roundId.subtract(BigInteger.ONE)).getQuantity();
                } else {
                    price = priceToQuantityRatio.multiply(new BigDecimal(quantityToSell));
                    quantity = quantityToSell;
                }
            }
        } else {
            BigDecimal discountPrice = getDiscountPrice(bestBidFromLastRound);
            quantity = bestBidFromLastRound.getQuantity();
            if (quantity.compareTo(quantityToSell) > 0) {
                quantity = quantityToSell;
                price = bestBidFromLastRound.getPrice();
            } else if (quantity.compareTo(BigInteger.ZERO) != 0 && discountPrice
                    .compareTo(priceToQuantityRatio.multiply(new BigDecimal(quantity))) >= 0) {
                price = discountPrice;
            } else if (bidsForRounds.get(roundId.subtract(BigInteger.ONE)) != null) {
                price = getDiscountPrice(bidsForRounds.get(roundId.subtract(BigInteger.ONE)));
                quantity = bidsForRounds.get(roundId.subtract(BigInteger.ONE)).getQuantity();
            } else {
                price = priceToQuantityRatio.multiply(new BigDecimal(quantityToSell));
                quantity = quantityToSell;
            }
        }

        return new Bid(price, quantity);
    }

    public void doInteractionBehaviour() {
        roundId = getRoundID();
        addBehaviour(new CustomContractNetInitiator(this, null));
    }

    // todo: figure out a way to not use static semaphores for ContractLoader (too many instances cause out of memory exceptions)
    public Set<SmartContract.BidAcceptedEventResponse> getLogsForPreviousRoundId(BigInteger currentRoundId) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error(this.getAID().getName() + " was interrupted while waiting for semaphore");
        }
        ContractLoader contractLoader = getContractLoaderForThisAgent();
        SmartContract smartContract = contractLoader.loadContract();
        Set<SmartContract.BidAcceptedEventResponse> logs
                = contractLoader.getLogsForRoundId(currentRoundId.subtract(BigInteger.ONE), smartContract);
        semaphore.release();
        return logs;
    }

    private ContractLoader getContractLoaderForThisAgent() {
        return new ContractLoader("password", walletFilePath);
    }

    private BigDecimal getDiscountPrice(Bid bid) {
        return bid.getPrice()
                .multiply(new BigDecimal(discountFactorB)).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

    private String getBiddersAddressFromWalletFilePath(String walletFilePath) {
        if (!walletFilePath.equals("")) {
            return walletFilePath.substring(walletFilePath.lastIndexOf("--") + 2);
        } else {
            log.error("Wallet file path is empty!");
            return "";
        }
    }

    // todo: if we store bigdecimal as biginteger for quantity, then additional math would be needed
    private Bid getBestBidFromBidSet(Set<SmartContract.BidAcceptedEventResponse> bids) {
        BigInteger maxGrossProfit = BigInteger.ZERO;
        Bid maxGrossProfitBid = new Bid(BigDecimal.ZERO, BigInteger.ZERO);

        for (SmartContract.BidAcceptedEventResponse bid : bids) {
            BigInteger grossProfit = bid.quantity.multiply(bid.price);
            if (grossProfit.compareTo(maxGrossProfit) > 0) {
                maxGrossProfit = grossProfit;
                maxGrossProfitBid = new Bid(new BigDecimal(bid.price), bid.quantity);
            }
        }
        return maxGrossProfitBid;
    }

    private List<SmartContract.BidAcceptedEventResponse> getBiddersBidsInTheLastRoundIfExist(Set<SmartContract.BidAcceptedEventResponse> bids,
                                                                                             String biddersAddress) {
        return bids.stream()
                .filter(x -> x.bidder.equalsIgnoreCase("0x" + biddersAddress))
                .collect(Collectors.toList());
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
                message.addReceiver(new AID(agent.getLocalName(), AID.ISLOCALNAME));
            }

            if (agents.length == 0) {
                log.error("No agents matching the type were found. Terminating: "
                        + getAgent().getAID().getName());
                helper.killAgent(getAgent());
            } else {
                message.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                message.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                Bid bid;
                //todo: we need to call calculateBid() in the setup so that the buyer
                // actually accepts the offer, otherwise instantiating ContractLoader creates deadlock
                //: we do not want to call calculate bid more than once per round
                if (bidsForRounds.get(roundId) != null) {
                    bid = bidsForRounds.get(roundId);
                } else {
                    bid = calculateBid();
                    bidsForRounds.put(roundId, bid);
                    log.info(getAID().getName() + " has issued a new offer" + bid);
                }


                message.setContent(getBiddersAddressFromWalletFilePath(walletFilePath)
                        + "|" + bid.getPrice() + "|" + bid.getQuantity());

                messages.addElement(message);
            }

            return messages;
        }

        protected void handlePropose(ACLMessage propose, Vector acceptances) {
            BigDecimal proposedPrice = getPriceFromContent(propose.getContent());
            BigInteger proposedQuantity = getQuantityFromContent(propose.getContent());

            ACLMessage reply = propose.createReply();


            if (quantityToSell.compareTo(proposedQuantity) >= 0
                    && proposedPrice.compareTo(priceToQuantityRatio
                    .multiply(new BigDecimal(proposedQuantity)).setScale(0, BigDecimal.ROUND_HALF_UP)) >= 0) {
                quantityToSell = quantityToSell.subtract(proposedQuantity);
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                reply.setContent(getBiddersAddressFromWalletFilePath(walletFilePath)
                        + "|" + proposedPrice + "|" + proposedQuantity);
            } else {
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
//                    log.info(getAID().getName() + " rejected proposal, because it was too low ("
//                            + proposedPrice + "<" + priceToQuantityRatio.multiply(new BigDecimal(proposedQuantity)) +
//                             ") OR not enough to sell (" + quantityToSell + "<" + proposedQuantity + ")");
            }

            acceptances.addElement(reply);

        }

        protected void handleRefuse(ACLMessage refuse) {
            globalResponses++;
        }

        protected void handleFailure(ACLMessage failure) {
            globalResponses++;
            if (failure.getContent() != null) {
//                log.info(this.getAgent().getName() + " was refused/failed proposal from " + failure.getSender().getName());
                BigDecimal isValidNumber = BigDecimalValidator.getInstance().validate(failure.getContent());
                if (isValidNumber == null) {
                } else {
                    BigInteger quantityNotSold = new BigDecimal(failure.getContent()).toBigInteger();
                    quantityToSell = quantityToSell.add(quantityNotSold);
                }
            }
        }

        protected void handleInform(ACLMessage inform) {
            globalResponses++;
        }


        protected void handleAllResponses(Vector responses, Vector acceptances) {
            int agentsLeft = responses.size() - globalResponses;
            globalResponses = 0;

            log.debug(getAID().getName() + " got " + agentsLeft + " responses.");

        }

        private BigInteger getQuantityFromContent(String content) {
            return new BigInteger(
                    content.substring(content.lastIndexOf("|") + 1));
        }

        private BigDecimal getPriceFromContent(String content) {
            return new BigDecimal(
                    content.substring(0, content.indexOf("|")));
        }
    }

}
