import java.io.Serializable;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Trader implements Serializable{

    private transient AgentController agentController;
    private transient Agent tradeAgent;


    public void start() throws StaleProxyException{
        agentController.start();
    }

    public void kill() throws StaleProxyException{
        agentController.kill();
    }

    public Agent getTradeAgent() {
        return tradeAgent;
    }

    public void setTradeAgent(Agent tradeAgent) {
        this.tradeAgent = tradeAgent;
    }

    public AgentController getAgentController() {
        return agentController;
    }
    public void setAgentController(AgentController agentController) {
        this.agentController = agentController;
    }

}