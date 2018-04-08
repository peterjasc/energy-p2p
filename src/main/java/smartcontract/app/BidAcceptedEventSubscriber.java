package smartcontract.app;

import rx.Subscriber;
import smartcontract.app.generated.SmartContract;

class BidAcceptedEventSubscriber extends Subscriber<SmartContract.BidAcceptedEventResponse> {
    @Override
    public void onCompleted() {
        System.out.println("onCompleted");
    }

    @Override
    public void onError(Throwable e) {
        System.out.println("error" + e);
    }

    @Override
    public void onNext(SmartContract.BidAcceptedEventResponse o) {
        System.out.println("objectEvent " + o);
    }
}
