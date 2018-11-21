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

import java.math.BigDecimal;
import java.math.BigInteger;

public class BuyerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BuyerAgent.class);

    private BigDecimal buyersHighestPriceToQuantityRatio = BigDecimal.valueOf(20);
    private BigInteger quantityToBuy = BigInteger.ZERO;

    private BigInteger roundId = BigInteger.ZERO;


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
            String roundIdString = (String) args[2];

            if (BigDecimalValidator.getInstance().validate(ratio) != null && NumberUtils.isDigits(quantity)
                    && NumberUtils.isDigits(roundIdString)) {
                roundId = NumberUtils.createBigInteger(roundIdString);
                buyersHighestPriceToQuantityRatio = NumberUtils.createBigDecimal(ratio);
                quantityToBuy = NumberUtils.createBigInteger(quantity);
            } else {
                log.error("Percentage must be a positive decimal, quantity and round ID must be positive integers");
                log.error("Terminating: " + this.getAID().getName());
                doDelete();
            }
        } else {
            log.error("Three arguments required.");
            log.error("Terminating: " + this.getAID().getName());
            doDelete();
        }

        MessageTemplate template = getInteractionProtocolBehaviourTemplate();
        addBehaviour(new CustomContractNetResponderDispatcher(this, template));
    }

    private MessageTemplate getInteractionProtocolBehaviourTemplate() {
        final String IP = FIPANames.InteractionProtocol.FIPA_CONTRACT_NET;
        return MessageTemplate.and(MessageTemplate.MatchProtocol(IP),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));
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

                    if (quantityToBuy.compareTo(BigInteger.ZERO) == 0) {
                        log.info(getAID().getName()
                                + " has bought all the energy they need and rejects the proposal from " + cfp.getSender());
                        ACLMessage exitResponse = cfp.createReply();
                        exitResponse.setPerformative(ACLMessage.REFUSE);
                        return exitResponse;
                    }

                    try {
                        String receivedContent = cfp.getContent();
                        receivedOfferPrice =  getPriceFromContent(receivedContent);
                        receivedOfferQuantity = getQuantityFromContent(receivedContent);
                    } catch (Exception e) {
                        log.error(getAID().getName() + " couldn't read the price and/or quantity.");
                    }

                    ACLMessage response = cfp.createReply();

                    if (receivedOfferQuantity.compareTo(quantityToBuy) > 0) {
                        log.info(getAID().getName() + " refused bid from " + cfp.getSender()
                                + ". They wanted quantity of " + quantityToBuy
                                + ", but were offered: " + receivedOfferQuantity);
                        response.setPerformative(ACLMessage.REFUSE);
                        return response;
                    }

                    BigDecimal buyersHighestPriceForOfferQuantity = buyersHighestPriceToQuantityRatio
                            .multiply(new BigDecimal(receivedOfferQuantity)).stripTrailingZeros();
                    // avoid the scientific notation, eg 3E+2, to avoid comparision issues
                    if (buyersHighestPriceForOfferQuantity.scale() < 0) {
                        buyersHighestPriceForOfferQuantity = buyersHighestPriceForOfferQuantity.setScale(0,BigDecimal.ROUND_HALF_UP);
                    }

                    if (buyersHighestPriceForOfferQuantity.compareTo(receivedOfferPrice) >= 0) {
                        response.setPerformative(ACLMessage.PROPOSE);
                        response.setContent(String.valueOf(receivedOfferPrice));

                    } else {
                        log.info(getAID().getName() + " refused bid. Their highest price was " + buyersHighestPriceForOfferQuantity
                                + ", but were offered: " + receivedOfferPrice);
                        response.setPerformative(ACLMessage.REFUSE);
                    }

                    return response;
                }

                protected ACLMessage handleAcceptProposal(ACLMessage msg, ACLMessage propose, ACLMessage accept) {
                    if (msg != null) {
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

                        log.info(getAID().getName() + " has accepted the offer from "
                                + accept.getSender().getName() + ", and will send $" + payment + " for " + quantity + " Wh.");

                        ContractLoader contractLoader = new ContractLoader("password",
                                "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe");
                        SmartContract smartContract = contractLoader.loadContract();

                        addContractToChain(smartContract, roundId.toString(), "1000",
                                biddersAddress, quantity.toString(), payment.toString());

                        quantityToBuy = quantityToBuy.subtract(quantity);

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
                    log.info(reject.getSender().getName() + " rejected offer from " + getAID().getName());
                }
            };
        }

//        // todo: forever in the loop, if maxSubtractableAmount too big
//        private BigDecimal generateLowerOfferInAccordanceWithConstraints(BigDecimal receivedOfferPrice,
//                                                                         BigDecimal lowerBound) {
//            int maxSubtractableAmount;
//            int scale = String.valueOf(receivedOfferPrice).length();
//
//            if (scale == 1) {
//                maxSubtractableAmount = 1;
//            } else if (scale == 2) {
//                maxSubtractableAmount = 5;
//            } else if (scale == 3) {
//                maxSubtractableAmount = 50;
//            } else {
//                maxSubtractableAmount = 200;
//            }
//
//            Random generate = new Random();
//            BigDecimal lowerOffer = lowerBound;
//            while (lowerOffer.compareTo(lowerBound) <= 0) {
//                BigDecimal randomNumber = new BigDecimal(generate.nextInt(maxSubtractableAmount) + 1);
//                lowerOffer = receivedOfferPrice.subtract(randomNumber);
//            }
//            return lowerOffer;
//        }

        private String getAddressFromContent(String content) {
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

        // todo:bidder address always the same
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
