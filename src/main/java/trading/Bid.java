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

    public void setPrice(BigDecimal price) {
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
