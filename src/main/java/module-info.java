module com.unpar.skripsi {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;

    // Modul tambahan dari pustaka pihak ketiga
    requires org.jsoup;
    requires java.net.http;

    // Controller di-load via FXML (reflection)
    opens com.unpar.skripsi to javafx.fxml;

    // Model dipakai TableView/PropertyValueFactory (reflection)
    opens com.unpar.skripsi.model to javafx.base;

    // (opsional) jika ada view lain yang pakai reflection ke package crawler/util via FXML, tambahkan:
    // opens com.unpar.brokenlinkcheckervcrawler to javafx.base;
    // opens com.unpar.brokenlinkcheckervutil to javafx.base;

    // Export jika package ingin dipakai modul lain (opsional)
    exports com.unpar.skripsi;
    exports com.unpar.skripsi.model;
}