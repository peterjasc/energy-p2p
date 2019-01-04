package trading.cron;

import java.math.BigInteger;
import java.util.TimerTask;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import trading.DFHelper;

public class MyTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(MyTask.class);
    private TaskedAgent agent;

    public MyTask(TaskedAgent agent) {
      this.agent = agent;
    }

    @Override
    public void run() {
        log.info(((Agent) agent).getName() + " string for " + agent.getRoundID().toString());

        if(agent.getQuantity().compareTo(BigInteger.ZERO) == 0) {
            DFHelper helper = DFHelper.getInstance();
            helper.killAgent((Agent) agent);
            this.cancel();
        }

        if (!agent.getLogsForPreviousRoundId(agent.getRoundID().add(BigInteger.ONE)).isEmpty()) {
            agent.doInteractionBehaviour();
            agent.setRoundID(agent.getRoundID().add(BigInteger.ONE));
        } else {
            log.info("No logs for current round found");
        }
    }

}


