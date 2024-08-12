module by.aghmi.tikomed {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.desktop;

    opens by.aghmi.tikomed to javafx.fxml;
    exports by.aghmi.tikomed;
}
