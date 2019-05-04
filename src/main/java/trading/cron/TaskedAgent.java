package trading.cron;

import smartcontract.app.generated.SmartContract;

import java.math.BigInteger;
import java.util.Set;

public interface TaskedAgent {

    public boolean isSecondTimeQuantityIsZero();

    public void setSecondTimeQuantityIsZero(boolean secondTimeQuantityIsZero);

    Set<SmartContract.BidAcceptedEventResponse> getLogsForPreviousRoundId(BigInteger currentRoundId);
    void doInteractionBehaviour();
    BigInteger getQuantity();
}
