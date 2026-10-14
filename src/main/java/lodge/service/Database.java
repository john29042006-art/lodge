package lodge.service;

import lodge.model.Booking;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class Database {
    private static final String URL = "jdbc:sqlite:hotelook-portal.db";
    private Database() {}
    public static Connection connect() throws SQLException { Connection c=DriverManager.getConnection(URL); try(Statement s=c.createStatement()){s.execute("PRAGMA foreign_keys = ON");} return c; }
    public static void initialize() throws SQLException {
        try(Connection c=connect(); Statement s=c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, guest_name TEXT NOT NULL, phone TEXT, room_type TEXT NOT NULL, room_number TEXT NOT NULL, check_in TEXT NOT NULL, check_out TEXT NOT NULL, nights INTEGER NOT NULL, rate REAL NOT NULL, total REAL NOT NULL, paid REAL NOT NULL DEFAULT 0, status TEXT NOT NULL DEFAULT 'Booked', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS payments (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER NOT NULL, amount REAL NOT NULL, method TEXT NOT NULL, paid_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE CASCADE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS room_rates (room_type TEXT PRIMARY KEY, rate REAL NOT NULL)");
            s.executeUpdate("INSERT OR IGNORE INTO room_rates(room_type,rate) VALUES ('Standard',450),('Deluxe',750),('Family',1100)");
        }
    }
    public static List<Booking> allBookings(String search) throws SQLException {
        List<Booking> out = new ArrayList<>();
        String q = "SELECT * FROM bookings WHERE guest_name LIKE ? OR room_number LIKE ? OR CAST(id AS TEXT) LIKE ? ORDER BY id DESC";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(q)) {
            String x = "%" + (search == null ? "" : search.trim()) + "%";
            p.setString(1, x); p.setString(2, x); p.setString(3, x);
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(read(r)); }
        }
        return out;
    }

    public static List<Booking> bookingsForGuest(String guestName, String search) throws SQLException {
        List<Booking> out = new ArrayList<>();
        String q = "SELECT * FROM bookings WHERE guest_name = ? AND (room_number LIKE ? OR CAST(id AS TEXT) LIKE ?) ORDER BY id DESC";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(q)) {
            String x = "%" + (search == null ? "" : search.trim()) + "%";
            p.setString(1, guestName == null ? "" : guestName.trim()); p.setString(2, x); p.setString(3, x);
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(read(r)); }
        }
        return out;
    }
    public static Booking createBooking(String guest, String phone, String type, String room,
                                        LocalDate in, LocalDate out, double rate) throws SQLException {
        if (guest == null || guest.isBlank()) throw new IllegalArgumentException("Enter the guest's full name.");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Select a room type.");
        if (room == null || room.isBlank()) throw new IllegalArgumentException("Enter the room number.");
        if (in == null || out == null) throw new IllegalArgumentException("Choose check-in and check-out dates.");
        if (in.isBefore(LocalDate.now())) throw new IllegalArgumentException("Check-in cannot be in the past.");
        long days = ChronoUnit.DAYS.between(in, out);
        if (days < 1) throw new IllegalArgumentException("Check-out must be at least one night after check-in.");
        if (rate <= 0) throw new IllegalArgumentException("The room rate must be greater than zero.");

        try (Connection c = connect()) {
            String overlap = "SELECT COUNT(*) FROM bookings WHERE room_number = ? AND status != 'Cancelled' AND check_in < ? AND check_out > ?";
            try (PreparedStatement check = c.prepareStatement(overlap)) {
                check.setString(1, room.trim()); check.setString(2, out.toString()); check.setString(3, in.toString());
                try (ResultSet r = check.executeQuery()) {
                    if (r.next() && r.getInt(1) > 0) {
                        throw new IllegalArgumentException("That room is already booked for some of those dates. Choose another room number or dates.");
                    }
                }
            }
            double total = days * rate;
            if (days >= 7) total *= 0.95;
            String q = "INSERT INTO bookings(guest_name,phone,room_type,room_number,check_in,check_out,nights,rate,total,paid,status) VALUES(?,?,?,?,?,?,?,?,?,0,'Booked')";
            try (PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1, guest.trim()); p.setString(2, phone == null ? "" : phone.trim());
                p.setString(3, type); p.setString(4, room.trim()); p.setString(5, in.toString());
                p.setString(6, out.toString()); p.setLong(7, days); p.setDouble(8, rate); p.setDouble(9, total);
                p.executeUpdate();
                try (ResultSet k = p.getGeneratedKeys()) {
                    int id = k.next() ? k.getInt(1) : 0;
                    return new Booking(id, guest.trim(), phone == null ? "" : phone.trim(), type, room.trim(),
                        in.toString(), out.toString(), (int) days, rate, total, 0, "Booked");
                }
            }
        }
    }
    public static void addPayment(int id,double amount,String method) throws SQLException {
        if(amount<=0)throw new IllegalArgumentException("Payment must be greater than zero.");
        try(Connection c=connect()){c.setAutoCommit(false);try{double total,paid;try(PreparedStatement p=c.prepareStatement("SELECT total,paid FROM bookings WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Booking not found.");total=r.getDouble(1);paid=r.getDouble(2);}}
            if(amount>total-paid+0.0001)throw new IllegalArgumentException("Payment exceeds the outstanding balance.");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO payments(booking_id,amount,method) VALUES(?,?,?)")){p.setInt(1,id);p.setDouble(2,amount);p.setString(3,method);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE bookings SET paid=paid+?, status=CASE WHEN paid+?>=total THEN 'Paid' ELSE 'Part-paid' END WHERE id=?")){p.setDouble(1,amount);p.setDouble(2,amount);p.setInt(3,id);p.executeUpdate();}c.commit();}catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof RuntimeException re)throw re;throw new SQLException(e);}}
    }
    public static void deleteBooking(int id) throws SQLException {try(Connection c=connect();PreparedStatement p=c.prepareStatement("DELETE FROM bookings WHERE id=?")){p.setInt(1,id);p.executeUpdate();}}
    public static double rateFor(String type) throws SQLException {try(Connection c=connect();PreparedStatement p=c.prepareStatement("SELECT rate FROM room_rates WHERE room_type=?")){p.setString(1,type);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("No rate configured for "+type);return r.getDouble(1);}}}
    public static void updateRate(String type,double rate) throws SQLException {if(rate<=0)throw new IllegalArgumentException("Rate must be greater than zero.");try(Connection c=connect();PreparedStatement p=c.prepareStatement("INSERT INTO room_rates(room_type,rate) VALUES(?,?) ON CONFLICT(room_type) DO UPDATE SET rate=excluded.rate")){p.setString(1,type);p.setDouble(2,rate);p.executeUpdate();}}
    public static double totalRevenue() throws SQLException {try(Connection c=connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COALESCE(SUM(paid),0) FROM bookings")){return r.next()?r.getDouble(1):0;}}
    public static double outstanding() throws SQLException {try(Connection c=connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COALESCE(SUM(total-paid),0) FROM bookings")){return r.next()?r.getDouble(1):0;}}
    public static int bookingCount() throws SQLException {try(Connection c=connect();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM bookings")){return r.next()?r.getInt(1):0;}}
    public static int activeStays() throws SQLException {String d=LocalDate.now().toString();try(Connection c=connect();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM bookings WHERE check_in<=? AND check_out>? AND status!='Cancelled'")){p.setString(1,d);p.setString(2,d);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}}}
    private static Booking read(ResultSet r)throws SQLException{return new Booking(r.getInt("id"),r.getString("guest_name"),r.getString("phone"),r.getString("room_type"),r.getString("room_number"),r.getString("check_in"),r.getString("check_out"),r.getInt("nights"),r.getDouble("rate"),r.getDouble("total"),r.getDouble("paid"),r.getString("status"));}
}
