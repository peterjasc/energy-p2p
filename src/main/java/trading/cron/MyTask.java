package trading.cron;

import java.math.BigInteger;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyTask extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(MyTask.class);

    public MyTask(TaskedAgent agent) {
        if (!agent.getLogsForPreviousRoundId(agent.getRoundID()).isEmpty()) {
            agent.setRoundID(agent.getRoundID().add(BigInteger.ONE));
            agent.doInteractionBehaviour();
        } else {
           log.error("No logs for previous round found");
        }
    }

    @Override
    public void run() {
    }

}


