package trading;

import java.math.BigInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RoundHelper {

    private static BigInteger roundId;
    private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();


    public static BigInteger getRoundId() {
        readWriteLock.readLock().lock();
        try {
            return roundId;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public static void setRoundId(BigInteger roundId) {
        readWriteLock.writeLock().lock();
        try {
            RoundHelper.roundId = roundId;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
