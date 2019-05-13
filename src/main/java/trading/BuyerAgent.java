package trading;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSContractNetResponder;
import jade.proto.SSResponderDispatcher;
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
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Semaphore;

public class BuyerAgent extends Agent implements TaskedAgent {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BuyerAgent.class);

    private BigDecimal buyersHighestPriceToQuantityRatio = BigDecimal.valueOf(20);
    private BigInteger quantityToBuy = BigInteger.ZERO;
    private String walletFilePath = "";

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
        return quantityToBuy;
    }

    protected void setup() {
        DFHelper helper = DFHelper.getInstance();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Buyer");
        serviceDescription.setName(getLocalName());
        helper.register(this, serviceDescription);

        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            String ratio = (String) args[0];
            String quantity = (String) args[1];
            walletFilePath = (String) args[2];

            if (BigDecimalValidator.getInstance().validate(ratio) != null && NumberUtils.isDigits(quantity)) {
                roundId = RoundHelper.getRoundId();
                buyersHighestPriceToQuantityRatio = NumberUtils.createBigDecimal(ratio);
                quantityToBuy = NumberUtils.createBigInteger(quantity);
            } else {
                log.error("Percentage must be a positive decimal, quantity and round ID must be positive integers");
                log.error("Terminating: " + this.getAID().getName());
                doDelete();
            }
        } else {
            log.error("Wrong number of arguments.");
            log.error("Terminating: " + this.getAID().getName());
            doDelete();
        }

        doInteractionBehaviour();
        Timer t = new Timer();
        AgentTask mTask = new AgentTask(this);
        t.scheduleAtFixedRate(mTask, 0, 20000);

    }

    public void doInteractionBehaviour() {
        roundId = getRoundID();
        MessageTemplate template = getInteractionProtocolBehaviourTemplate();
        addBehaviour(new CustomContractNetResponderDispatcher(this, template));
    }

    private MessageTemplate getInteractionProtocolBehaviourTemplate() {
        final String IP = FIPANames.InteractionProtocol.FIPA_CONTRACT_NET;
        return MessageTemplate.and(MessageTemplate.MatchProtocol(IP),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));
    }

    private ContractLoader getContractLoaderForThisAgent() {
        return new ContractLoader("password", walletFilePath);
    }

    //todo: unused
    public Set<SmartContract.BidAcceptedEventResponse> getLogsForPreviousRoundId(BigInteger currentRoundId) {
        Set<SmartContract.BidAcceptedEventResponse> logs = new HashSet<>();
        return logs;
    }


    private class CustomContractNetResponderDispatcher extends SSResponderDispatcher {
        private static final long serialVersionUID = 1L;

        private CustomContractNetResponderDispatcher(Agent agent, MessageTemplate template) {
            super(agent, template);
        }

        protected Behaviour createResponder(ACLMessage message) {
            return new SSContractNetResponder(myAgent, message) {
                private static final long serialVersionUID = 1L;

                protected ACLMessage handleCfp(ACLMessage cfp) {

                    BigDecimal receivedOfferPrice = BigDecimal.ZERO;
                    BigInteger receivedOfferQuantity = BigInteger.ZERO;

                    ACLMessage exitResponse = refuseUnnecessaryBid(cfp);
                    if (exitResponse != null) return exitResponse;

                    try {
                        String receivedContent = cfp.getContent();
                        receivedOfferPrice = getPriceFromContent(receivedContent);
                        receivedOfferQuantity = getQuantityFromContent(receivedContent);
                    } catch (Exception e) {
                        log.error(getAID().getName() + " couldn't read the price and/or quantity.");
                    }

                    ACLMessage response = cfp.createReply();

                    if (receivedOfferQuantity.compareTo(quantityToBuy) < 0) {
                        log.info(getAID().getName() + " refused bid from " + cfp.getSender()
                                + ". They wanted quantity of " + quantityToBuy
                                + ", but were offered: " + receivedOfferQuantity);
                        response.setPerformative(ACLMessage.REFUSE);
                        return response;
                    } else if (receivedOfferQuantity.compareTo(quantityToBuy) > 0) {
                        BigDecimal biddersPriceToQuantityRatio
                                = receivedOfferPrice.divide(new BigDecimal(receivedOfferQuantity), RoundingMode.HALF_UP);
                        receivedOfferPrice = new BigDecimal(quantityToBuy).multiply(biddersPriceToQuantityRatio);
                        receivedOfferQuantity = quantityToBuy;
                    }

                    BigDecimal buyersHighestPriceForOfferQuantity = buyersHighestPriceToQuantityRatio
                            .multiply(new BigDecimal(receivedOfferQuantity)).stripTrailingZeros();

                    // avoid using the scientific notation, eg 3E+2, to avoid comparision issues
                    if (buyersHighestPriceForOfferQuantity.scale() < 0) {
                        buyersHighestPriceForOfferQuantity = buyersHighestPriceForOfferQuantity.setScale(0, BigDecimal.ROUND_HALF_UP);
                    }

                    if (buyersHighestPriceForOfferQuantity.compareTo(receivedOfferPrice) >= 0) {
                        response.setPerformative(ACLMessage.PROPOSE);
                        response.setContent(receivedOfferPrice + "|" + receivedOfferQuantity);
                    } else {
                        log.info(getAID().getName() + " refused bid from " +  cfp.getSender().getName()
                                + ". Their highest price was " + buyersHighestPriceForOfferQuantity
                                + ", but were offered: " + receivedOfferPrice);
                        response.setPerformative(ACLMessage.REFUSE);
                    }

                    return response;
                }

                protected ACLMessage handleAcceptProposal(ACLMessage msg, ACLMessage propose, ACLMessage accept) {
                    if (msg != null) {
                        ACLMessage exitResponse = failUnnecessaryBid(msg, accept);
                        if (exitResponse != null) {
                            return exitResponse;
                        }

                        String biddersAddress = null;
                        BigInteger payment = BigInteger.ZERO;
                        BigInteger quantity = BigInteger.ZERO;
                        try {
                            String content = accept.getContent();
                            biddersAddress = getAddressFromContent(content);
                            payment = getPriceFromContent(content).toBigInteger();
                            quantity = getQuantityFromContent(content);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            semaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        log.info(getAID().getName() + " has accepted the offer from "
                                + accept.getSender().getName() + ", and will send $" + payment + " for " + quantity + " Wh.");
                        ContractLoader contractLoader = new ContractLoader("password",
                                walletFilePath);
                        SmartContract smartContract = contractLoader.loadContract();

                        addContractToChain(smartContract, roundId.toString(), "1000",
                                biddersAddress, quantity.toString(), payment.toString());
                        semaphore.release();
                        quantityToBuy = quantityToBuy.subtract(quantity);

                        ACLMessage inform = accept.createReply();
                        inform.setPerformative(ACLMessage.INFORM);
                        inform.setContent(quantity.toString());
                        return inform;

                    } else {
                        ACLMessage failure = accept.createReply();
                        failure.setPerformative(ACLMessage.FAILURE);
                        return failure;
                    }
                }

                protected void handleRejectProposal(ACLMessage msg, ACLMessage propose, ACLMessage reject) {
                }
            };
        }

        private ACLMessage refuseUnnecessaryBid(ACLMessage msg) {
            if (quantityToBuy.compareTo(BigInteger.ZERO) == 0) {
//                log.info(getAID().getName()
//                        + " has bought all the energy they need and rejects the proposal from " + msg.getSender());
                ACLMessage exitResponse = msg.createReply();
                exitResponse.setPerformative(ACLMessage.REFUSE);

                return exitResponse;
            }
            return null;
        }

        private ACLMessage failUnnecessaryBid(ACLMessage msg, ACLMessage accept) {
            if (quantityToBuy.compareTo(BigInteger.ZERO) == 0) {
                log.info(getAgent().getName() + " refused offer from "
                        + msg.getSender().getName() + ", because they already bought enough energy");
                ACLMessage exitResponse = msg.createReply();
                exitResponse.setContent(getQuantityFromContent(accept.getContent()).toString());
                exitResponse.setPerformative(ACLMessage.FAILURE);

                return exitResponse;
            }
            return null;
        }

        private String getAddressFromContent(String content) {
            return content.substring(0, content.indexOf("|"));
        }

        private BigInteger getQuantityFromContent(String content) {
            return new BigInteger(
                    content.substring(content.lastIndexOf("|") + 1));
        }

        private BigDecimal getPriceFromContent(String content) {
            return new BigDecimal(
                    content.substring(content.indexOf("|") + 1,
                            content.lastIndexOf("|")));
        }

        private void addContractToChain(SmartContract smartContract,
                                        String roundId, String contractId,
                                        String bidderAddress, String quantity, String price) {
            try {
                smartContract.addContract(
                        new BigInteger(roundId, 10),
                        new BigInteger(contractId, 10),
                        bidderAddress,
                        new BigInteger(quantity, 10),
                        new BigInteger(price, 10)).sendAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
