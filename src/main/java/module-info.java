module com.unpar.webcrawler {
    requires org.jsoup;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;

    exports com.unpar.webcrawler;
    opens com.unpar.webcrawler to javafx.fxml;
}
