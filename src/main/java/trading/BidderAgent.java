package trading;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;

import java.math.BigDecimal;
import java.util.*;

public class BidderAgent extends Agent {
	private static final long serialVersionUID = 1L;
	private Hashtable<String, BigDecimal> availableOffers;
	private ArrayList<BigDecimal> paymentList;
	private DFHelper helper;
	private String payment = null;

    protected void setup() {
		helper = DFHelper.getInstance();
		availableOffers = new Hashtable<>();

		Object[] args = getArguments();
		if (args.length == 1) {
			payment = (String) args[0];
			
			if (payment.matches("^(\\d|.)+$")) {
                BigDecimal initialPayment = new BigDecimal(payment);

				updateJobListings(getLocalName(), initialPayment);

				paymentList = new ArrayList<>();
				paymentList.add(initialPayment);

				ServiceDescription serviceDescription = new ServiceDescription();
				serviceDescription.setType("Bidder");
				serviceDescription.setName(getLocalName());
				helper.register(this, serviceDescription);
			} else {
				System.out.println("Payment must be a positive floating point number.");
				System.out.println("Terminating: " + this.getAID().getName());
				doDelete();
			}
		} else {
			System.out.println("One argument required. Please provide a floating point number.");
			System.out.println("Terminating: " + this.getAID().getName());
			doDelete();
		}

		addBehaviour(new ContractNetInitiator(this, null) {
			private static final long serialVersionUID = 1L;
			private int globalResponses = 0;

			public Vector<ACLMessage> prepareCfps(ACLMessage init) {
				init = new ACLMessage(ACLMessage.CFP);
				Vector<ACLMessage> messages = new Vector<>();

				AID[] agents = helper.searchDF(getAgent(), "Buyer");

				System.out.println("The Directory Facilitator found the following agents labeled as \"Buyer\": ");
				for (AID agent : agents) {
					System.out.println(agent.getName());
					init.addReceiver(new AID(agent.getLocalName(), AID.ISLOCALNAME));
				}
				System.out.println();

				if (agents.length == 0) {
					System.out.println("No agents matching the type were found. Terminating: " + getAgent().getAID().getName());
					helper.killAgent(getAgent());
				} else {
					init.setProtocol(FIPANames.InteractionProtocol.FIPA_ITERATED_CONTRACT_NET);
					init.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					init.setContent(getLocalName() + "|" + payment);

					messages.addElement(init);
				}

				return messages;
			}

			protected void handlePropose(ACLMessage propose, Vector v) {
				System.out.println(propose.getSender().getName() + " proposes $" + propose.getContent() + "\".");
			}

			protected void handleRefuse(ACLMessage refuse) {
				globalResponses++;
				System.out.println(refuse.getSender().getName() + " is not willing to bid any higher.");
				helper.removeReceiverAgent(refuse.getSender(), refuse);
			}

			protected void handleFailure(ACLMessage failure) {
				globalResponses++;
				System.out.println(failure.getSender().getName() + " failed to reply.");
				helper.removeReceiverAgent(failure.getSender(), failure);
			}

			protected void handleInform(ACLMessage inform) {
				globalResponses++;
				System.out.println("\n" + getAID().getName() + " has no stored power available.");
				availableOffers.remove(getLocalName());
				for (Agent agent : helper.getRegisteredAgents()) {
					helper.killAgent(agent);
				}
			}
			
			protected void handleAllResponses(Vector responses, Vector acceptances) {
				int agentsLeft = responses.size() - globalResponses;
				globalResponses = 0;

				System.out.println("\n" + getAID().getName() + " got " + agentsLeft + " responses.");

				BigDecimal bestProposal = new BigDecimal(payment);
				ACLMessage reply = new ACLMessage(ACLMessage.CFP);
				Vector<ACLMessage> cfps = new Vector<>();
				Enumeration<?> responseList = responses.elements();
				ArrayList<ACLMessage> responderList = new ArrayList<>();

				while (responseList.hasMoreElements()) {
					ACLMessage msg = (ACLMessage) responseList.nextElement();
					if (msg.getPerformative() == ACLMessage.PROPOSE) {
						BigDecimal proposal = new BigDecimal(msg.getContent());
						reply = msg.createReply();
						reply.setPerformative(ACLMessage.CFP);
						responderList.add(reply);
						if (proposal.compareTo(bestProposal) > 0) {
							bestProposal = proposal;
						}
						cfps.addElement(reply);
					}
				}
				if (agentsLeft > 1) {
					paymentList.add(bestProposal);

					for (int i = 0; i < responderList.size(); i++) {
						responderList.get(i).setContent(getLocalName() + "|" + bestProposal);
						cfps.set(i, responderList.get(i));
					}
					
					System.out.println(agentsLeft + " buyers are still bidding. Proceeding to the next round.");
					System.out.println(getAID().getName() + " is issuing CFP's with a payment of $" + paymentList.get(paymentList.size() - 1) + ".\n");
					newIteration(cfps);
				} else if (agentsLeft == 1) {
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					if (bestProposal.compareTo(paymentList.get(paymentList.size() - 1)) > 0) {
						reply.setContent(getLocalName() + "|" + bestProposal);
						reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					}
					acceptances.addElement(reply);
				} else {
					System.out.println("No agent accepted the job.");
				}
			}

		});
	}

	private void updateJobListings(final String agentLocalName, final BigDecimal payment) {
		addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			public void action() {
				availableOffers.put(agentLocalName, payment);
				System.out.println(getAID().getName() + " has issued a new offer, at $" + payment + ".\n");
			}
		});
	}
}
