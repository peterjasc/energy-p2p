import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import trading.BookSellerAgent;
import trading.BuyerAgent;

public class TradeAgentFactory {
    public static Trader createTradeAgent(Agent agent, ContainerController containerController) throws StaleProxyException {
        Trader trader = new Trader();
        Class<?> classForAgentToBeCreated = null;
        AgentController createdAgent;

        if (agent instanceof BuyerAgent) {
            classForAgentToBeCreated = BuyerAgent.class;
        } else if (agent instanceof BookSellerAgent) {
            classForAgentToBeCreated = BookSellerAgent.class;
        }

        trader.setTradeAgent(agent);

        createdAgent = containerController.createNewAgent(agent.getName(), classForAgentToBeCreated.getName(), agent.toArray());
        trader.setAgentController(createdAgent);

        return trader;
    }
}
