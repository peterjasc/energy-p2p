package trading;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class TradeAgentFactory {
    public static Trader createTradeAgent(String nickname, Agent agent, ContainerController containerController,
                                          String firstAgentArgument, String secondAgentArgument,
                                          String thirdAgentArgument) throws StaleProxyException {
        Trader trader = new Trader();
        trader.setNickname(nickname);

        Class<?> classForAgentToBeCreated = null;
        AgentController createdAgentController;
        Object[] arguments;

        if (agent instanceof BuyerAgent) {
            classForAgentToBeCreated = BuyerAgent.class;
            if (agent.getArguments() == null) {
                arguments = new Object[]{firstAgentArgument,secondAgentArgument};
            } else {
                arguments = agent.getArguments();
            }

        } else if (agent instanceof BidderAgent) {
            classForAgentToBeCreated = BidderAgent.class;
            if (agent.getArguments() == null) {
                arguments = new Object[]{firstAgentArgument,secondAgentArgument,thirdAgentArgument};
            } else {
                arguments = agent.getArguments();
            }
        } else {
            throw new IllegalArgumentException("createTradeAgent needs either a BuyerAgent or a BidderAgent agent argument");
        }



        trader.setTradeAgent(agent);

        createdAgentController = containerController
                .createNewAgent(nickname, classForAgentToBeCreated.getName(),arguments);
        trader.setAgentController(createdAgentController);

        return trader;
    }
}
