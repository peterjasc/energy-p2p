package trading;

import java.math.BigInteger;

public class Bid {

    private BigInteger price;
    private BigInteger quantity;

    public Bid(BigInteger price, BigInteger quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public BigInteger getPrice() {
        return price;
    }

    public BigInteger getQuantity() {
        return quantity;
    }

    public void setPrice(BigInteger price) {
        this.price = price;
    }

    public void setQuantity(BigInteger quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
