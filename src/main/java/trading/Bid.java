package trading;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Bid {

    private BigDecimal price;
    private BigInteger quantity;

    public Bid(BigDecimal price, BigInteger quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigInteger getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
