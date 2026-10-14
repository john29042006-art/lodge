package lodge.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lodge.Main;
import lodge.model.Booking;
import lodge.service.Database;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardController {
    @FXML private Label welcomeLabel, roleLabel, bookingCountLabel, occupancyLabel, revenueLabel, balanceLabel, messageLabel;
    @FXML private Label rateInfoLabel, estimateLabel, allRatesLabel;
    @FXML private TabPane tabs;
    @FXML private Tab bookingTab, paymentTab, ratesTab;
    @FXML private TextField guestNameField, phoneField, roomNumberField, searchField, paymentBookingField, paymentAmountField, rateStandardField, rateDeluxeField, rateFamilyField;
    @FXML private ComboBox<String> roomTypeBox, paymentMethodBox;
    @FXML private DatePicker checkInPicker, checkOutPicker;
    @FXML private TableView<Booking> bookingsTable;
    @FXML private TableColumn<Booking, Number> colId, colNights;
    @FXML private TableColumn<Booking, String> colGuest, colRoom, colType, colIn, colOut, colStatus;
    @FXML private TableColumn<Booking, Number> colTotal, colPaid, colBalance;
    @FXML private Button createBookingButton, addPaymentButton, deleteBookingButton, saveRatesButton, receiptButton;

    private boolean ownerMode;
    private String sessionName = "Guest";
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    @FXML private void initialize() {
        roomTypeBox.setItems(FXCollections.observableArrayList("Standard", "Deluxe", "Family"));
        roomTypeBox.getSelectionModel().selectFirst();
        paymentMethodBox.setItems(FXCollections.observableArrayList("Cash", "Mobile Money", "Bank Transfer", "Card"));
        paymentMethodBox.getSelectionModel().selectFirst();
        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colIn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        colOut.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        colNights.setCellValueFactory(new PropertyValueFactory<>("nights"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paid"));
        colBalance.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getBalance()));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) paymentBookingField.setText(String.valueOf(selected.getId()));
        });
        roomTypeBox.valueProperty().addListener((obs, old, value) -> updateEstimate());
        checkInPicker.valueProperty().addListener((obs, old, value) -> updateEstimate());
        checkOutPicker.valueProperty().addListener((obs, old, value) -> updateEstimate());
        try { loadRates(); refresh(); updateEstimate(); }
        catch (Exception e) { showMessage("Could not load saved data: " + e.getMessage(), true); }
    }

    public void setSession(String name, boolean owner) {
        sessionName = name == null ? "Guest" : name.trim();
        ownerMode = owner;
        welcomeLabel.setText("Welcome, " + sessionName);
        roleLabel.setText(owner ? "ADMIN ACCESS" : "GUEST ACCESS");
        if (!owner) {
            tabs.getTabs().removeAll(paymentTab, ratesTab);
            deleteBookingButton.setVisible(false); deleteBookingButton.setManaged(false);
            saveRatesButton.setVisible(false); saveRatesButton.setManaged(false);
            addPaymentButton.setVisible(false); addPaymentButton.setManaged(false);
            paymentAmountField.setDisable(true); paymentBookingField.setDisable(true); paymentMethodBox.setDisable(true);
            guestNameField.setText(sessionName);
            guestNameField.setEditable(false);
            searchField.setPromptText("Search your bookings by room or ID…");
        }
        try { refresh(); } catch (Exception e) { showMessage("Could not load bookings: " + e.getMessage(), true); }
        updateEstimate();
    }

    @FXML private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/lodge/views/login.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 740);
            scene.getStylesheets().add(Main.class.getResource("/lodge/styles/theme.css").toExternalForm());
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Hotelook | Welcome");
            stage.setScene(scene);
            stage.setMinWidth(900); stage.setMinHeight(650); stage.centerOnScreen();
        } catch (Exception e) { showMessage("Could not return to login: " + e.getMessage(), true); }
    }

    @FXML private void handleCreateBooking() {
        try {
            String guest = guestNameField.getText() == null ? "" : guestNameField.getText().trim();
            String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
            String room = roomNumberField.getText() == null ? "" : roomNumberField.getText().trim();
            if (guest.isBlank() || room.isBlank()) throw new IllegalArgumentException("Guest name and room number are required.");
            if (checkInPicker.getValue() == null || checkOutPicker.getValue() == null) throw new IllegalArgumentException("Choose check-in and check-out dates.");
            if (checkInPicker.getValue().isBefore(LocalDate.now())) throw new IllegalArgumentException("Check-in cannot be in the past.");
            String type = roomTypeBox.getValue();
            double rate = Database.rateFor(type);
            Booking booking = Database.createBooking(guest, phone, type, room, checkInPicker.getValue(), checkOutPicker.getValue(), rate);
            if (ownerMode) { guestNameField.clear(); phoneField.clear(); roomNumberField.clear(); }
            else { phoneField.clear(); roomNumberField.clear(); }
            checkInPicker.setValue(LocalDate.now()); checkOutPicker.setValue(LocalDate.now().plusDays(1));
            refresh(); updateEstimate(); showMessage("Booking #" + booking.getId() + " confirmed. Bill total: " + money(booking.getTotal()), false);
            bookingsTable.getSelectionModel().selectFirst();
            showBill(booking);
        } catch (Exception e) { showMessage(e.getMessage() == null ? "Could not create booking." : e.getMessage(), true); }
    }

    @FXML private void handleSearch() {
        try { loadBookings(); } catch (Exception e) { showMessage(e.getMessage(), true); }
    }
    @FXML private void handleRefresh() {
        try { refresh(); showMessage("Records refreshed.", false); } catch (Exception e) { showMessage(e.getMessage(), true); }
    }
    @FXML private void handleAddPayment() {
        try {
            int id = Integer.parseInt(paymentBookingField.getText().trim());
            double amount = Double.parseDouble(paymentAmountField.getText().trim());
            Database.addPayment(id, amount, paymentMethodBox.getValue());
            paymentAmountField.clear(); refresh(); showMessage("Payment recorded successfully.", false);
        } catch (Exception e) { showMessage(e.getMessage() == null ? "Enter a valid booking ID and payment amount." : e.getMessage(), true); }
    }
    @FXML private void handleDeleteBooking() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showMessage("Select a booking to delete.", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete booking #" + selected.getId() + " for " + selected.getGuestName() + "? This cannot be undone.",
            ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Delete booking");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try { Database.deleteBooking(selected.getId()); refresh(); showMessage("Booking deleted.", false); }
            catch (Exception e) { showMessage(e.getMessage(), true); }
        }
    }
    @FXML private void handleSaveRates() {
        try {
            Database.updateRate("Standard", parseRate(rateStandardField.getText()));
            Database.updateRate("Deluxe", parseRate(rateDeluxeField.getText()));
            Database.updateRate("Family", parseRate(rateFamilyField.getText()));
            loadRates(); updateEstimate(); showMessage("Room rates saved. New bookings will use these prices.", false);
        } catch (Exception e) { showMessage(e.getMessage(), true); }
    }

    @FXML private void handleBill() {
        Booking b = bookingsTable.getSelectionModel().getSelectedItem();
        if (b == null) { showMessage("Select a booking to view its bill.", true); return; }
        showBill(b);
    }

    private void showBill(Booking b) {
        double subtotal = b.getRate() * b.getNights();
        double discount = Math.max(0, subtotal - b.getTotal());
        String bill = "HOTELOOK  |  BOOKING BILL\n" +
            "----------------------------------------\n" +
            "Booking number: #" + b.getId() + "\n" +
            "Guest: " + b.getGuestName() + "\n" +
            "Phone: " + (b.getPhone() == null || b.getPhone().isBlank() ? "Not provided" : b.getPhone()) + "\n" +
            "Room: " + b.getRoomNumber() + " — " + b.getRoomType() + "\n" +
            "Check-in: " + b.getCheckIn() + "\nCheck-out: " + b.getCheckOut() + "\n" +
            "Nights: " + b.getNights() + "\nRate per night: " + money(b.getRate()) + "\n" +
            "Subtotal: " + money(subtotal) + "\nDiscount: -" + money(discount) + "\n" +
            "TOTAL: " + money(b.getTotal()) + "\nPaid: " + money(b.getPaid()) + "\n" +
            "BALANCE DUE: " + money(b.getBalance()) + "\nStatus: " + b.getStatus() + "\n\n" +
            "Thank you for choosing Hotelook.";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hotelook Bill"); alert.setHeaderText("Bill for booking #" + b.getId()); alert.setContentText(bill);
        alert.getDialogPane().setMinWidth(460); alert.showAndWait();
    }

    @FXML private void handleReceipt() {
        Booking b = bookingsTable.getSelectionModel().getSelectedItem();
        if (b == null) { showMessage("Select a booking to create a receipt.", true); return; }
        FileChooser chooser = new FileChooser(); chooser.setTitle("Save Hotelook bill / receipt");
        chooser.setInitialFileName("Hotelook-Receipt-" + b.getId() + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text receipt", "*.txt"));
        File file = chooser.showSaveDialog(bookingsTable.getScene().getWindow());
        if (file == null) return;
        double subtotal = b.getRate() * b.getNights();
        double discount = Math.max(0, subtotal - b.getTotal());
        String receipt = "HOTELOOK — LODGE BILL / RECEIPT\n" +
            "========================================\n" +
            "Receipt / Booking No: #" + b.getId() + "\n" +
            "Issued: " + LocalDate.now().format(dateFormat) + "\n" +
            "Guest: " + b.getGuestName() + "\nPhone: " + b.getPhone() + "\n" +
            "Room: " + b.getRoomNumber() + " (" + b.getRoomType() + ")\n" +
            "Check-in: " + b.getCheckIn() + "\nCheck-out: " + b.getCheckOut() + "\n" +
            "Nights: " + b.getNights() + "\nRate per night: " + money(b.getRate()) + "\n" +
            "Subtotal: " + money(subtotal) + "\nDiscount: -" + money(discount) + "\n" +
            "TOTAL CHARGES: " + money(b.getTotal()) + "\nAMOUNT PAID: " + money(b.getPaid()) + "\n" +
            "BALANCE DUE: " + money(b.getBalance()) + "\nPayment status: " + b.getStatus() + "\n\n" +
            "Thank you for choosing Hotelook.\n";
        try { Files.writeString(file.toPath(), receipt); showMessage("Receipt saved to " + file.getAbsolutePath(), false); }
        catch (Exception e) { showMessage("Could not save receipt: " + e.getMessage(), true); }
    }

    private void loadRates() throws Exception {
        rateStandardField.setText(String.format(Locale.US, "%.2f", Database.rateFor("Standard")));
        rateDeluxeField.setText(String.format(Locale.US, "%.2f", Database.rateFor("Deluxe")));
        rateFamilyField.setText(String.format(Locale.US, "%.2f", Database.rateFor("Family")));
        if (allRatesLabel != null) {
            allRatesLabel.setText("STANDARD  " + money(Database.rateFor("Standard")) + " / night     •     DELUXE  " + money(Database.rateFor("Deluxe")) + " / night     •     FAMILY  " + money(Database.rateFor("Family")) + " / night");
        }
    }
    private void updateEstimate() {
        if (rateInfoLabel == null || estimateLabel == null || roomTypeBox == null) return;
        try {
            String type = roomTypeBox.getValue() == null ? "Standard" : roomTypeBox.getValue();
            double rate = Database.rateFor(type);
            rateInfoLabel.setText(type + " room · " + money(rate) + " per night");
            LocalDate in = checkInPicker.getValue(), out = checkOutPicker.getValue();
            if (in == null || out == null || !out.isAfter(in)) { estimateLabel.setText("Choose valid dates to calculate your bill."); return; }
            long nights = java.time.temporal.ChronoUnit.DAYS.between(in, out);
            double subtotal = nights * rate;
            double discount = nights >= 7 ? subtotal * 0.05 : 0;
            estimateLabel.setText("Estimated bill: " + money(subtotal - discount) + " for " + nights + " night(s)" + (discount > 0 ? " · 5% weekly discount included" : ""));
        } catch (Exception e) { rateInfoLabel.setText("Room price unavailable"); estimateLabel.setText("Could not calculate estimate."); }
    }
    private void refresh() throws Exception {
        loadBookings();
        bookingCountLabel.setText(ownerMode ? String.valueOf(Database.bookingCount()) : String.valueOf(Database.bookingsForGuest(sessionName, "").size()));
        occupancyLabel.setText(ownerMode ? String.valueOf(Database.activeStays()) : "—");
        revenueLabel.setText(ownerMode ? money(Database.totalRevenue()) : "Private");
        balanceLabel.setText(ownerMode ? money(Database.outstanding()) : "Your balances only");
    }
    private void loadBookings() throws Exception {
        String search = searchField == null ? "" : searchField.getText();
        bookingsTable.setItems(FXCollections.observableArrayList(ownerMode
            ? Database.allBookings(search)
            : Database.bookingsForGuest(sessionName, search)));
    }
    private double parseRate(String s) { double v = Double.parseDouble(s.trim()); if (!Double.isFinite(v) || v <= 0) throw new IllegalArgumentException("Rates must be greater than zero."); return v; }
    private String money(double value) { return String.format(Locale.US, "K %,.2f", value); }
    private void showMessage(String text, boolean error) {
        messageLabel.setText(text == null ? "Something went wrong." : text);
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.getStyleClass().add(error ? "message-error" : "message-success");
    }
}
