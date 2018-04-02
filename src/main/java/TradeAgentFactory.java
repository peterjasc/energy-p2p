import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import trading.BidderAgent;
import trading.BuyerAgent;

public class TradeAgentFactory {
    public static Trader createTradeAgent(String nickname, Agent agent, ContainerController containerController) throws StaleProxyException {
        Trader trader = new Trader();
        trader.setNickname(nickname);

        Class<?> classForAgentToBeCreated = null;
        AgentController createdAgentController;
        Object[] arguments = null;

        if (agent instanceof BuyerAgent) {
            classForAgentToBeCreated = BuyerAgent.class;
        } else if (agent instanceof BidderAgent) {
            classForAgentToBeCreated = BidderAgent.class;
            if (agent.getArguments() == null) {
                arguments = new Object[]{"300"};
            } else {
                arguments = agent.getArguments();
            }
        }

        trader.setTradeAgent(agent);



        createdAgentController = containerController
                .createNewAgent(nickname, classForAgentToBeCreated.getName(),arguments);
        trader.setAgentController(createdAgentController);

        return trader;
    }
}
