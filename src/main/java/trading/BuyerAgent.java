package trading;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;
import smartcontract.app.BuyersSubscriber;
import smartcontract.app.generated.SmartContract;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

public class BuyerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BuyerAgent.class);

    private DFHelper helper;
    private Integer allowablePercentageDivergenceFromInitialOffer = 90;
    private BigDecimal lowestPriceToQuantityRatio = BigDecimal.valueOf(0.5);
    private BigInteger quantityToBuy = BigInteger.ZERO;


    protected void setup() {
        helper = DFHelper.getInstance();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Buyer");
        serviceDescription.setName(getLocalName());
        helper.register(this, serviceDescription);

        Object[] args = getArguments();
        if (args != null && args.length == 2) {
            String percentage = (String) args[0];
            String quantity = (String) args[1];

            if (NumberUtils.isDigits(percentage) && NumberUtils.isDigits(quantity)) {
                allowablePercentageDivergenceFromInitialOffer = NumberUtils.createInteger(percentage);
                quantityToBuy = NumberUtils.createBigInteger(quantity);
            } else {
                log.error("Percentage must be a positive decimal number and quantity positive integer");
                log.error("Terminating: " + this.getAID().getName());
                doDelete();
            }
        } else {
            log.error("Two arguments required.");
            log.error("Terminating: " + this.getAID().getName());
            doDelete();
        }

        MessageTemplate template = getInteractionProtocolBehaviourTemplate();
        addBehaviour(new CustomContractNetResponderDispatcher(this, template));
    }

    private MessageTemplate getInteractionProtocolBehaviourTemplate() {
        final String IP = FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET;
        return MessageTemplate.and(MessageTemplate.MatchProtocol(IP),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));
    }

    private class CustomContractNetResponderDispatcher extends SSResponderDispatcher {
        private static final long serialVersionUID = 1L;

        private CustomContractNetResponderDispatcher(Agent agent, MessageTemplate template) {
            super(agent, template);
        }

        protected Behaviour createResponder(ACLMessage message) {
            return new SSIteratedContractNetResponder(myAgent, message) {
                private static final long serialVersionUID = 1L;

                protected ACLMessage handleCfp(ACLMessage cfp) {
                    BigDecimal receivedOfferPrice = BigDecimal.ZERO;
                    BigInteger receivedOfferQuantity = BigInteger.ZERO;
                    try {
                        String receivedContent = cfp.getContent();
                        receivedOfferPrice = getPriceFromContent(receivedContent);
                        receivedOfferQuantity = getQuantityFromContent(receivedContent);
                    } catch (Exception e) {
                        log.error(getAID().getName() + " couldn't read the price and/or quantity.");
                    }

                    BigDecimal lowerBound = receivedOfferPrice
                            .multiply(new BigDecimal(allowablePercentageDivergenceFromInitialOffer))
                            .divide(new BigDecimal(100), BigDecimal.ROUND_CEILING);
                    BigDecimal buyersLowestPriceForOfferQuantity = lowestPriceToQuantityRatio
                            .multiply(new BigDecimal(receivedOfferQuantity));

                    ACLMessage response = cfp.createReply();

                    if (buyersLowestPriceForOfferQuantity.compareTo(receivedOfferPrice) < 0
                            && helper.getRespondersRemaining() == 1) {
                        response.setPerformative(ACLMessage.PROPOSE);
                        response.setContent(String.valueOf(receivedOfferPrice));

                    } else if (buyersLowestPriceForOfferQuantity.compareTo(lowerBound) < 0
                            && helper.getRespondersRemaining() > 1) {
                        BigDecimal lowerOffer = generateLowerOfferInAccordanceWithConstraints(receivedOfferPrice, lowerBound);

                        response.setPerformative(ACLMessage.PROPOSE);
                        response.setContent(String.valueOf(lowerOffer));

                    } else {
                        log.info(getAID().getName() + " refused bid with price " + receivedOfferPrice
                                + " and quantity " + receivedOfferQuantity);
                        response.setPerformative(ACLMessage.REFUSE);
                    }

                    return response;
                }

                protected ACLMessage handleAcceptProposal(ACLMessage msg, ACLMessage propose, ACLMessage accept) {
                    if (msg != null) {
                        String agentName = null;
                        BigDecimal payment = BigDecimal.ZERO;
                        BigInteger quantity = BigInteger.ZERO;
                        try {
                            String content = accept.getContent();
                            agentName = getAgentNameFromContent(content);
                            payment = getPriceFromContent(content);
                            quantity = getQuantityFromContent(content);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Subscriber<SmartContract.BidAcceptedEventResponse> subscriber = new BuyersSubscriber();
                        ContractLoader contractLoader = new ContractLoader("password",
                                "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe");
                        SmartContract smartContract = contractLoader.loadContractWithSubscriberFromEarliestToLatest(subscriber);

                        addContractToChain(smartContract,"1","10",
                                "0x521892450a22dc762198f6ce597cfc6d85f673a3", "10", "10");

                        log.info(getAID().getName() + " has accepted the offer from "
                                + accept.getSender().getName() + ", and will send $" + payment + " for " + quantity + " Wh.");
                        ACLMessage inform = accept.createReply();
                        inform.setPerformative(ACLMessage.INFORM);
                        return inform;

                    } else {
                        ACLMessage failure = accept.createReply();
                        failure.setPerformative(ACLMessage.FAILURE);
                        return failure;
                    }
                }

                protected void handleRejectProposal(ACLMessage msg, ACLMessage propose, ACLMessage reject) {
                    log.info(reject.getSender().getName() + " rejected offer from " + getAID().getName()
                            + " for unexpected reasons");
                }
            };
        }

        private BigDecimal generateLowerOfferInAccordanceWithConstraints(BigDecimal receivedOfferPrice,
                                                                         BigDecimal lowerBound) {
            BigDecimal lowerOffer = lowerBound;
            Random generate = new Random();

            int upperBound;
            int scale = String.valueOf(receivedOfferPrice).length();

            if (scale == 1) {
                upperBound = 1;
            } else if (scale == 2) {
                upperBound = 5;
            } else if (scale == 3) {
                upperBound = 50;
            } else {
                upperBound = 200;
            }

            while (lowerOffer.compareTo(lowerBound) <= 0) {
                BigDecimal randomNumber = new BigDecimal(generate.nextInt(upperBound) + 1);
                lowerOffer = receivedOfferPrice.subtract(randomNumber);
            }
            return lowerOffer;
        }

        private String getAgentNameFromContent(String content) {
            return content.substring(0, content.indexOf("|"));
        }

        private BigInteger getQuantityFromContent(String content) {
            return new BigInteger(
                    content.substring(content.lastIndexOf("|") + 1,
                            content.length()));
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
                log.info("Value stored in remote smart contract: " + smartContract.addContract(
                        new BigInteger(roundId, 10),
                        new BigInteger(contractId, 10),
                        bidderAddress,
                        new BigInteger(quantity, 10),
                        new BigInteger(price, 10)
                ).send());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
