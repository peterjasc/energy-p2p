package trading;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
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
    private BigDecimal initialPayment = BigDecimal.ZERO;
    private int percentage = 50;

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
                percentage = Integer.parseInt(percentageArg);
            }
        }

        final String IP = FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET;
        MessageTemplate template = MessageTemplate.and(MessageTemplate.MatchProtocol(IP),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        SequentialBehaviour sequential = new SequentialBehaviour();
        addBehaviour(sequential);
        ParallelBehaviour parallel = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        sequential.addSubBehaviour(parallel);
        parallel.addSubBehaviour(new CustomContractNetResponder(this, template));
    }

    private class CustomContractNetResponder extends SSResponderDispatcher {
        private static final long serialVersionUID = 1L;

        private CustomContractNetResponder(Agent agent, MessageTemplate template) {
            super(agent, template);
        }

        protected Behaviour createResponder(ACLMessage message) {
            return new SSIteratedContractNetResponder(myAgent, message) {
                private static final long serialVersionUID = 1L;

                /**
                 * Responds to the CFP message from the initiator with either a PROPOSE/REFUSE message.
                 * If the payment is too low for the agent, it declines with a REFUSE message,
                 * otherwise, the agent will respond with a PROPOSE message.
                 */
                protected ACLMessage handleCfp(ACLMessage cfp) {
                    BigDecimal payment = BigDecimal.ZERO;
                    BigDecimal backupPayment = BigDecimal.ZERO;
                    try {
                        payment = new BigDecimal(cfp.getContent().substring(cfp.getContent().lastIndexOf("|") + 1));
                        backupPayment = payment;
                    } catch (Exception e) {
                        System.out.println(getAID().getName() + " couldn't read the price.");
                    }

                    if (initialPayment.compareTo(BigDecimal.ZERO) == 0) {
                        initialPayment = payment;
                    }

                    Random generate = new Random();

                    int upperBound;
                    // todo
                    int length = String.valueOf(payment).length();

                    switch (length) {
                        case 1:
                            upperBound = 1;
                            break;
                        case 2:
                            upperBound = 5;
                            break;
                        case 3:
                            upperBound = 50;
                            break;
                        default:
                            upperBound = 300;
                            break;
                    }

                    BigDecimal randomNumber = new BigDecimal(generate.nextInt(upperBound) + 1);
                    BigDecimal lowerBound = initialPayment
                            .multiply(new BigDecimal(percentage))
                            .divide(new BigDecimal(100), BigDecimal.ROUND_CEILING);

                    if (randomNumber.compareTo(BigDecimal.ONE) < 0
                            && (payment.subtract(randomNumber)).compareTo(lowerBound) > 0) {
                        payment = (payment.subtract(randomNumber));
                    } else {
                        payment = BigDecimal.ZERO;
                    }

                    ACLMessage response = cfp.createReply();

                    if (payment.compareTo(BigDecimal.ZERO) > 0) {
                        response.setPerformative(ACLMessage.PROPOSE);
                        if (helper.getRespondersRemaining() == 1) {
                            response.setContent(String.valueOf(backupPayment));
                        } else {
                            response.setContent(String.valueOf(payment));
                        }
                    } else {
                        upperBound = generate.nextInt(3000) + 1000;
                        doWait(upperBound);

                        if (helper.getRespondersRemaining() == 1) {
                            response.setPerformative(ACLMessage.PROPOSE);
                            response.setContent(String.valueOf(backupPayment));
                        } else {
                            response.setPerformative(ACLMessage.REFUSE);
                        }
                    }
                    return response;
                }

                protected ACLMessage handleAcceptProposal(ACLMessage msg, ACLMessage propose, ACLMessage accept) {
                    if (msg != null) {
                        String agentName = null;
                        BigDecimal payment = BigDecimal.ZERO;
                        try {
                            agentName = accept.getContent().substring(0, accept.getContent().indexOf("|"));
                            payment = new BigDecimal (accept.getContent().substring(accept.getContent().lastIndexOf("|") + 1));
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
