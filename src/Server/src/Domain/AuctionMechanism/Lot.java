package Domain.AuctionMechanism;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "LOT")
public class Lot implements Serializable {
    @Id
    @Column(name = "description", nullable = false)
    private String description;

    @Id
    @Column(name = "baseprice", updatable = false, nullable = false)
    private int basePrice;

    @Transient
    private String pathImage;

    @Id
    @Column(name = "vendor", updatable = false, nullable = false)
    private String vendor;

    @Id
    @Column(name = "winner", updatable = false)
    private String winner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auctionid")
    private Auction auL;



    @Override
    public int hashCode() {
        return 100;
    }



    public int getBasePrice() {
        return basePrice;
    }

    public String getVendor() {
        return vendor;
    }

    public String getDescription() { return description; }

    public String information() {
        return "Name:" + description + "\t" + "Vendor: " + vendor + "\t" + "Base Price:" + basePrice + "\n";
    }

    public String closedInformation() {
        return "Name:" + description + "\t" + "Vendor: " + vendor + "\t" + "Base Price:" + basePrice + "\t\t" + "Winner:" + winner + "\n";
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) { this.winner = winner; }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBasePrice(int basePrice) {
        this.basePrice = basePrice;
    }

    public String getPathImage() {
        return pathImage;
    }

    public void setPathImage(String pathImage) {
        this.pathImage = pathImage;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public Auction getAuL() {
        return auL;
    }

    public void setAuL(Auction auL) {
        this.auL = auL;
    }

    public Lot() {}

    public Lot(String description, int basePrice, String owner) {
        this.description = description;
        this.basePrice = basePrice;
        this.vendor = owner;
    }
}
