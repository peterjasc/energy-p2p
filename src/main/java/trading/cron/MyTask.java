package trading.cron;

import java.math.BigInteger;
import java.util.TimerTask;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(MyTask.class);

    public MyTask(TaskedAgent agent) {
        log.info(((Agent) agent).getName() + "string for " + agent.getRoundID().toString());
        if (!agent.getLogsForPreviousRoundId(agent.getRoundID().add(BigInteger.ONE)).isEmpty()) {
            agent.doInteractionBehaviour();
            agent.setRoundID(agent.getRoundID().add(BigInteger.ONE));
        } else {
           log.info("No logs for previous round found");
        }
    }

    @Override
    public void run() {
    }

}


