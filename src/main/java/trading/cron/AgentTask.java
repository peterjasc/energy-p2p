package trading.cron;

import java.math.BigInteger;
import java.util.TimerTask;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trading.DFHelper;


public class AgentTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(AgentTask.class);
    private final TaskedAgent agent;

    public AgentTask(TaskedAgent agent) {
      this.agent = agent;
    }

    @Override
    public void run() {
        if (!agent.isSecondTimeQuantityIsZero() && agent.getQuantity().compareTo(BigInteger.ZERO) == 0) {
            agent.setSecondTimeQuantityIsZero(true);
        } else if (agent.isSecondTimeQuantityIsZero() && agent.getQuantity().compareTo(BigInteger.ZERO) == 0) {
            DFHelper helper = DFHelper.getInstance();
            log.info("killed agent " + ((Agent) agent).getName());
            helper.killAgent((Agent) agent);
            this.cancel();
        } else {
            agent.doInteractionBehaviour();
        }

    }

}


