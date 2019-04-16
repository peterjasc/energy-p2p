package trading.cron;

import java.math.BigInteger;
import java.util.TimerTask;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trading.DFHelper;


//todo: make this work with 100 agents in async (main problem is ContractLoader)
public class MyTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(MyTask.class);
    private final TaskedAgent agent;

    public MyTask(TaskedAgent agent) {
      this.agent = agent;
    }

    @Override
    public void run() {
            if (agent.getQuantity().compareTo(BigInteger.ZERO) == 0) {
                DFHelper helper = DFHelper.getInstance();
                helper.killAgent((Agent) agent);
                this.cancel();
            }

            if (!agent.getLogsForPreviousRoundId(agent.getRoundID().add(BigInteger.ONE)).isEmpty()) {
                agent.doInteractionBehaviour();
                agent.setRoundID(agent.getRoundID().add(BigInteger.ONE));
            } else {
                log.debug("No logs for current round found");
            }
    }

}


