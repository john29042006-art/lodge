package lodge.model;

import javafx.beans.property.*;

public class Booking {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty guestName = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty roomType = new SimpleStringProperty();
    private final StringProperty roomNumber = new SimpleStringProperty();
    private final StringProperty checkIn = new SimpleStringProperty();
    private final StringProperty checkOut = new SimpleStringProperty();
    private final IntegerProperty nights = new SimpleIntegerProperty();
    private final DoubleProperty rate = new SimpleDoubleProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();
    private final DoubleProperty paid = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();
    public Booking(int id, String guestName, String phone, String roomType, String roomNumber, String checkIn, String checkOut, int nights, double rate, double total, double paid, String status) {
        this.id.set(id); this.guestName.set(guestName); this.phone.set(phone); this.roomType.set(roomType); this.roomNumber.set(roomNumber); this.checkIn.set(checkIn); this.checkOut.set(checkOut); this.nights.set(nights); this.rate.set(rate); this.total.set(total); this.paid.set(paid); this.status.set(status);
    }
    public int getId(){return id.get();} public IntegerProperty idProperty(){return id;}
    public String getGuestName(){return guestName.get();} public StringProperty guestNameProperty(){return guestName;}
    public String getPhone(){return phone.get();} public StringProperty phoneProperty(){return phone;}
    public String getRoomType(){return roomType.get();} public StringProperty roomTypeProperty(){return roomType;}
    public String getRoomNumber(){return roomNumber.get();} public StringProperty roomNumberProperty(){return roomNumber;}
    public String getCheckIn(){return checkIn.get();} public StringProperty checkInProperty(){return checkIn;}
    public String getCheckOut(){return checkOut.get();} public StringProperty checkOutProperty(){return checkOut;}
    public int getNights(){return nights.get();} public IntegerProperty nightsProperty(){return nights;}
    public double getRate(){return rate.get();} public DoubleProperty rateProperty(){return rate;}
    public double getTotal(){return total.get();} public DoubleProperty totalProperty(){return total;}
    public double getPaid(){return paid.get();} public DoubleProperty paidProperty(){return paid;}
    public String getStatus(){return status.get();} public StringProperty statusProperty(){return status;}
    public double getBalance(){return Math.max(0, getTotal()-getPaid());}
}
