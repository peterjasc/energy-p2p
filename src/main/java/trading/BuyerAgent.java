package trading;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SSIteratedContractNetResponder;
import jade.proto.SSResponderDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
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
    private BigDecimal initialOffer = BigDecimal.ZERO;
    private int allowablePercentageDivergenceFromInitialOffer = 90;

    private SmartContract smartContract;

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

//            loadContractFromChain();
        }

        registerInteractionProtocolBehaviour();
    }

    private void loadContractFromChain() {
        ChainConnector chainConnector = new ChainConnector().invoke("password",
                "/home/peter/Documents/energy-p2p/private-testnet/keystore/UTC--2018-04-04T09-17-25.118212336Z--9b538e4a5eba8ac0f83d6025cbbabdbd13a32bfe");
        Web3j web3j = chainConnector.getWeb3j();
        Credentials credentials = chainConnector.getCredentials();

        Subscriber<SmartContract.BidAcceptedEventResponse> subscriber = new BuyersSubscriber();
        smartContract = new ContractLoader(web3j, credentials).invoke(subscriber);
    }

    private void registerInteractionProtocolBehaviour() {
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
                        log.info(getAID().getName() + " couldn't read the price.");
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
                            e.printStackTrace();
                        }

//                        addContractToChain();

                        log.info(getAID().getName() + " has accepted the offer from "
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
                    log.info(reject.getSender().getName() + " rejected offer " + getAID().getName()
                            + " for unexpected reasons");
                }
            };
        }

        private void addContractToChain() {
            try {
                log.info("Value stored in remote smart contract: " + smartContract.addContract(
                        new BigInteger("1", 10),
                        "0x521892450a22dc762198f6ce597cfc6d85f673a3",
                        new BigInteger("10", 10),
                        new BigInteger("10", 10)
                ).send());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
