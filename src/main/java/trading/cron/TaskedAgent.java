package trading.cron;

import smartcontract.app.generated.SmartContract;

import java.math.BigInteger;
import java.util.Set;

public interface TaskedAgent {
    Set<SmartContract.BidAcceptedEventResponse> getLogsForPreviousRoundId(BigInteger roundId);
    BigInteger getRoundID();
    void setRoundID(BigInteger roundID);
    void doInteractionBehaviour();
}
