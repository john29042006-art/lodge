# Hotelook — Lodge Booking, Billing & Management

Hotelook is a JavaFX desktop application designed with a website-inspired black-and-gold interface. It provides guest booking, room pricing, bills, receipts, and an admin workspace. It stores data locally in SQLite and displays prices in Zambian Kwacha (ZMW / K).

## Features

### Guest portal
- Guest entry using the guest's full name.
- View Standard, Deluxe, and Family nightly room rates.
- Enter contact details, choose room type and room number, and select check-in/check-out dates.
- Automatic night count, estimate, booking total, and 5% discount for stays of 7 nights or more.
- Rejects past check-in dates, invalid date ranges, and overlapping reservations for the same room number.
- View only bookings under the signed-in guest name.
- View a bill and save a text receipt that can be opened or printed.
- Logout returns to the login screen.

### Admin workspace
- Admin login, bookings register, search, and dashboard summary.
- Configure Standard, Deluxe, and Family nightly prices.
- Record partial or full payments by cash, mobile money, bank transfer, or card.
- View totals received and outstanding balances.
- Save a bill/receipt for a selected booking.
- Delete a booking after confirmation.
- Logout returns to the login screen.

## Requirements
- Windows 10/11, macOS, or Linux
- JDK 17 or newer
- Maven 3.8 or newer
- Internet connection for Maven to download dependencies on the first build

## Run on Windows
1. Open the `Hotelook-Upgraded` folder in VS Code (the folder containing `pom.xml`).
2. Open **Terminal → New Terminal**.
3. Run `mvn clean javafx:run` if Maven is on your PATH.

If Maven is not on your PATH but you have the VS Code Java extension pack installed, run this in the VS Code terminal:

```cmd
"%USERPROFILE%\AppData\Roaming\Code\User\globalStorage\pleiades.java-extension-pack-jdk\maven\latest\bin\mvn.cmd" clean javafx:run
```

## Login

- Choose **Guest**, enter the guest's full name, and continue. Guest access does not require a password.
- Choose **Admin**, enter username `admin`, and enter the admin password.
- For local development only, the fallback admin password is `admin123`.

For a private admin password, use a Windows Command Prompt before launching:

```cmd
set HOTELOOK_ADMIN_PASSWORD=put-a-long-private-password-here
"%USERPROFILE%\AppData\Roaming\Code\User\globalStorage\pleiades.java-extension-pack-jdk\maven\latest\bin\mvn.cmd" javafx:run
```

Do not use the fallback password for real guest or financial records. This is a local desktop prototype, not a production internet-facing multi-user authentication system. Guest access identifies bookings by the entered name and is not identity verification.

## Default room rates

| Room type | Default nightly rate |
|---|---:|
| Standard | K450.00 |
| Deluxe | K750.00 |
| Family | K1,100.00 |

The admin can change rates in the Room Rates tab. New rates apply to new bookings only. Existing bookings keep the rate saved when they were created. Stays of at least seven nights receive a 5% discount.

## Data storage

The app creates `hotelook-portal.db` in the project working directory. This separate database name prevents the redesigned app from changing an older `hotelook.db` that may use a different schema. Back up the database by closing Hotelook and copying `hotelook-portal.db` to a safe location.

## Project structure

```text
Hotelook-Upgraded/
├── pom.xml
├── README.md
└── src/main/
    ├── java/lodge/
    │   ├── Launcher.java
    │   ├── Main.java
    │   ├── controller/
    │   │   ├── LoginController.java
    │   │   └── DashboardController.java
    │   ├── model/Booking.java
    │   └── service/Database.java
    └── resources/lodge/
        ├── styles/theme.css
        └── views/
            ├── login.fxml
            └── dashboard.fxml
```

## Important notes
- The receipt is currently a plain-text file that can be opened and printed. PDF generation and direct printer integration can be added later.
- Room availability is checked by room number and overlapping dates, but the app does not yet include an admin-managed room inventory with room photos or online reservations.
- Guest login is a convenience portal and is not a secure online guest account. Do not expose this local prototype directly to the public internet.
- Confirm the lodge's actual prices, discount policy, and local tax requirements before using it for real transactions.
