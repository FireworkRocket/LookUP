package org.fireworkrocket.lookup.ui.fxmlcontroller.Set;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Today {

    @FXML
    public Label WelconeLabel;

    public static Today today;

    @FXML
    void initialize() {
        today = this;
        String username = System.getProperty("user.name");
        WelconeLabel.setText("欢迎回来,\n" + username);
        WelconeLabel.setStyle("-fx-font-size: 18px;");
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.getDefault());
        String formattedDate = currentDate.format(dateFormatter);

        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        String dayOfWeekDisplay = dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault());
        WelconeLabel.setText(dayOfWeekDisplay + ",\n" + formattedDate);
        WelconeLabel.setStyle("-fx-font-size: 30px");
    }
}
