package trading.cron;

import java.math.BigInteger;
import java.util.TimerTask;

public class MyTask extends TimerTask {


    public MyTask(TaskedAgent agent) {
        if (!agent.getLogsForPreviousRoundId(agent.getRoundID()).isEmpty()) {
            agent.setRoundID(agent.getRoundID().add(BigInteger.ONE));
            agent.doInteractionBehaviour();
        }
    }

    @Override
    public void run() {
    }

}


