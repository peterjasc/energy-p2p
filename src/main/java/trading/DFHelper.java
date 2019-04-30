package trading;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public final class DFHelper extends Agent {
	private static final long serialVersionUID = 1L;
	private int respondersRemaining = 0;

	private static final Logger log = LoggerFactory.getLogger(DFHelper.class);
	private static DFHelper instance = null;
	private ArrayList<Agent> registeredAgents = new ArrayList<Agent>();

	private DFHelper() {
	}

	public static synchronized DFHelper getInstance() {
		if (instance == null) {
			instance = new DFHelper();
		}
		return instance;
	}

	public void register(Agent agent, ServiceDescription serviceDescription) {
		DFAgentDescription dfAgentDescription = new DFAgentDescription();
		dfAgentDescription.setName(getAID());
		dfAgentDescription.addServices(serviceDescription);

		try {
			registeredAgents.add(agent);
			DFService.register(agent, dfAgentDescription);
//			log.info(agent.getName() + " registered as: " + serviceDescription.getType() + ".");
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	public AID[] searchDF(Agent agent, String service) {
		DFAgentDescription dfAgentDescription = new DFAgentDescription();
		ServiceDescription serviceDescription = new ServiceDescription();
		serviceDescription.setType(service);
		dfAgentDescription.addServices(serviceDescription);

		SearchConstraints findAll = new SearchConstraints();
		findAll.setMaxResults((long) -1);
		
		try {
			DFAgentDescription[] result = DFService.search(agent, dfAgentDescription, findAll);
			AID[] agents = new AID[result.length];
			for (int i = 0; i < result.length; i++) {
				agents[i] = result[i].getName();
				respondersRemaining++;
			}
			return agents;
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Removes a receiver from the ongoing auction, but does not terminate it.
	 * @param agent - the agent to remove
	 * @param msg - the message it's associated with
	 */
	public void removeReceiverAgent(AID agent, ACLMessage msg) {
		respondersRemaining--;
		log.debug(agent.getName() + " was removed from receivers.");
		msg.removeReceiver(agent);
	}

	public void killAgent(Agent agent) {
		try {
			log.debug(agent.getAID().getName() + " left.");
			DFService.deregister(agent);
			agent.doDelete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getRespondersRemaining() {
		return respondersRemaining;
	}

	public ArrayList<Agent> getRegisteredAgents() {
		return registeredAgents;
	}
}
