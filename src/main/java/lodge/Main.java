package lodge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lodge.service.Database;

public class Main extends Application {
    @Override public void start(Stage stage) throws Exception {
        Database.initialize();
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/lodge/views/login.fxml"));
        Scene scene = new Scene(loader.load(), 1050, 700);
        scene.getStylesheets().add(Main.class.getResource("/lodge/styles/theme.css").toExternalForm());
        stage.setTitle("Hotelook | Lodge Management");
        stage.setMinWidth(900); stage.setMinHeight(620); stage.setScene(scene); stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
