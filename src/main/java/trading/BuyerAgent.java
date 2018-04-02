package trading;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;

import java.math.BigDecimal;
import java.util.Random;

public class BuyerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private DFHelper helper;
    private BigDecimal initialOffer = BigDecimal.ZERO;
    private int allowablePercentageDivergenceFromInitialOffer = 90;

    protected void setup() {
        helper = DFHelper.getInstance();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("Buyer");
        serviceDescription.setName(getLocalName());
        helper.register(this, serviceDescription);

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String percentageArg = (String) args[0];
            if (percentageArg.matches("^\\d+$")) {
                allowablePercentageDivergenceFromInitialOffer = Integer.parseInt(percentageArg);
            }
        }

        registerBehaviour();
    }

    private void registerBehaviour() {
        final String IP = FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET;
        MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchProtocol(IP),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        addBehaviour(new CustomContractNetResponder(this, template));
    }

    private class CustomContractNetResponder extends SSResponderDispatcher {
        private static final long serialVersionUID = 1L;

        private CustomContractNetResponder(Agent agent, MessageTemplate template) {
            super(agent, template);
        }

        protected Behaviour createResponder(ACLMessage message) {
            return new SSIteratedContractNetResponder(myAgent, message) {
                private static final long serialVersionUID = 1L;

                protected ACLMessage handleCfp(ACLMessage cfp) {
                    BigDecimal receivedOffer = BigDecimal.ZERO;
                    try {
                        receivedOffer = new BigDecimal(cfp.getContent().substring(cfp.getContent().lastIndexOf("|") + 1));
                    } catch (Exception e) {
                        System.out.println(getAID().getName() + " couldn't read the price.");
                    }

                    if (initialOffer.compareTo(BigDecimal.ZERO) == 0) {
                        initialOffer = receivedOffer;
                    }

                    Random generate = new Random();

                    int upperBound;
                    int length = String.valueOf(receivedOffer).length();

                    if (length == 1) {
                        upperBound = 1;
                    } else if (length == 2) {
                        upperBound = 5;
                    } else if (length == 3) {
                        upperBound = 50;
                    } else {
                        upperBound = 200;
                    }

                    BigDecimal lowerBound = initialOffer
                            .multiply(new BigDecimal(allowablePercentageDivergenceFromInitialOffer))
                            .divide(new BigDecimal(100), BigDecimal.ROUND_CEILING);
                    BigDecimal lowerOffer = lowerBound;

                    while (lowerOffer.compareTo(lowerBound) <= 0) {
                        BigDecimal randomNumber = new BigDecimal(generate.nextInt(upperBound) + 1);
                        lowerOffer = receivedOffer.subtract(randomNumber);
                    }

                    ACLMessage response = cfp.createReply();

                    if (lowerOffer.compareTo(lowerBound) > 0) {
                        response.setPerformative(ACLMessage.PROPOSE);
                        if (helper.getRespondersRemaining() == 1) {
                            response.setContent(String.valueOf(receivedOffer));
                        } else {
                            response.setContent(String.valueOf(lowerOffer));
                        }
                    } else {
                        response.setPerformative(ACLMessage.REFUSE);
                    }
                    return response;
                }

                protected ACLMessage handleAcceptProposal(ACLMessage msg, ACLMessage propose, ACLMessage accept) {
                    if (msg != null) {
                        String agentName = null;
                        BigDecimal payment = BigDecimal.ZERO;
                        try {
                            agentName = accept.getContent().substring(0, accept.getContent().indexOf("|"));
                            payment = new BigDecimal(accept.getContent().substring(accept.getContent().lastIndexOf("|") + 1));
                        } catch (Exception e) {
                        }

                        System.out.println(getAID().getName() + " has accepted the offer from "
                                + accept.getSender().getName() + ", and will receive $" + payment + " for completing it.");
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
                    System.out.println(reject.getSender().getName() + " rejected offer " + getAID().getName()
                            + " for unexpected reasons");
                }
            };
        }
    }
}
