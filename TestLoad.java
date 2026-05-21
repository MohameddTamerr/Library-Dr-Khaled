import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import java.util.ResourceBundle;
import java.io.File;

public class TestLoad {
    public static void main(String[] args) {
        // Initialize JavaFX toolkit
        new JFXPanel();
        
        Platform.runLater(() -> {
            try {
                System.out.println("Starting FXML Load...");
                File file = new File("src/main/resources/fxml/cashier.fxml");
                System.out.println("Absolute Path: " + file.getAbsolutePath());
                FXMLLoader loader = new FXMLLoader(file.toURI().toURL());
                
                // Set dummy bundle
                ResourceBundle bundle = ResourceBundle.getBundle("messages");
                loader.setResources(bundle);
                
                // Note: Without the correct controller factory, it might fail to instantiate CashierController
                // if it doesn't have a no-arg constructor, but the LoadException cause will still show us what's wrong.
                
                loader.load();
                System.out.println("Loaded successfully.");
            } catch (Exception e) {
                System.out.println("Exception caught!");
                e.printStackTrace();
                Throwable cause = e.getCause();
                while(cause != null) {
                    System.out.println("Caused by:");
                    cause.printStackTrace();
                    cause = cause.getCause();
                }
            } finally {
                System.exit(0);
            }
        });
    }
}
