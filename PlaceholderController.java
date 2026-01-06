package attendance.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.ResourceBundle;

public class PlaceholderController implements Initializable {
    
    @FXML private Label titleLabel;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Title will be set by MainController
    }
    
    public void setTitle(String title) {
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }
}