package com.unpar.skripsi;

import com.unpar.skripsi.model.*;
import com.unpar.skripsi.util.TimeUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Controller {

    // ===================== FXML Components =====================
    @FXML private TextField seedUrl;
    @FXML private ComboBox<String> algoChoiceBox;
    @FXML private Button checkButton;
    @FXML private Button stopButton;
    @FXML private Button exportButton;

    @FXML private Label executionStatusLabel;
    @FXML private Label totalWebpagesLabel;
    @FXML private Label totalLinksLabel;
    @FXML private Label totalBrokenLinksLabel;
    @FXML private Label durationLabel;

    @FXML private ToggleButton brokenLinkToggle;
    @FXML private ToggleButton webpageToggle;

    @FXML private VBox customPagination;

    // Broken Links Table
    @FXML private TableView<BrokenLink> brokenLinkTable;
    @FXML private TableColumn<BrokenLink, Number> colNumber1;
    @FXML private TableColumn<BrokenLink, String> colStatus1;
    @FXML private TableColumn<BrokenLink, String> colBrokenLink1;
    @FXML private TableColumn<BrokenLink, String> colAnchorText1;
    @FXML private TableColumn<BrokenLink, String> colWebpageLink1;

    // Webpages Table
    @FXML private TableView<WebpageLink> webpageTable;
    @FXML private TableColumn<WebpageLink, Number> colNumber2;
    @FXML private TableColumn<WebpageLink, String> colStatus2;
    @FXML private TableColumn<WebpageLink, String> colWebpageLink2;
    @FXML private TableColumn<WebpageLink, Integer> colLinkCount2; // <--- Integer, bukan Number
    @FXML private TableColumn<WebpageLink, String> colAccessTime2;

    // ===================== State =====================
    private final ObservableList<BrokenLink> allBroken = FXCollections.observableArrayList();
    private final ObservableList<BrokenLink> pageBroken = FXCollections.observableArrayList();

    private final ObservableList<WebpageLink> webpages = FXCollections.observableArrayList();
    private final ObservableList<WebpageLink> pageWebpages = FXCollections.observableArrayList(); // NEW

    private enum ActiveView { BROKEN, WEBPAGE }
    private ActiveView activeView = ActiveView.BROKEN; // default

    private final CrawlSummary summary = new CrawlSummary();
    private ScheduledExecutorService durationScheduler;

    private Service service;

    // Pagination config
    private static final int ROWS_PER_PAGE = 20;
    private static final int MAX_PAGE_BUTTONS = 7;
    private static final double PAGE_BUTTON_WIDTH = 40;
    private int currentPage = 1;
    private int totalPageCount = 1;

    // ===================== Lifecycle =====================
    @FXML
    public void initialize() {
        setupAlgoChoiceBox();
        setupSummaryBindings();
        setupBrokenLinkTableColumns();
        setupWebpageTableColumns();
        setupToggleView();

        brokenLinkTable.setItems(pageBroken);
        webpageTable.setItems(pageWebpages);

        brokenLinkTable.getSortOrder().clear();
        webpageTable.getSortOrder().clear();

        // Hard-disable sort if needed (mencegah sortPolicy memicu resort)
        brokenLinkTable.setSortPolicy(tv -> false);
        webpageTable.setSortPolicy(tv -> false);


        setButtonsByStatus(ExecutionStatus.IDLE);
        updatePagination();
    }

    // ===================== Summary bindings =====================
    private void setupSummaryBindings() {
        executionStatusLabel.textProperty().bind(summary.statusTextProperty());
        totalWebpagesLabel.textProperty().bind(summary.totalWebpagesProperty().asString());
        totalLinksLabel.textProperty().bind(summary.totalLinksProperty().asString());
        totalBrokenLinksLabel.textProperty().bind(summary.totalBrokenLinksProperty().asString());
        durationLabel.textProperty().bind(summary.durationTextProperty());
    }

    // ===================== Table setup =====================
    private void setupBrokenLinkTableColumns() {
        colNumber1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.08));
        colStatus1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.18));
        colBrokenLink1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.36));
        colAnchorText1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.18));
        colWebpageLink1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.20));

        colNumber1.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                (currentPage - 1) * ROWS_PER_PAGE + pageBroken.indexOf(cell.getValue()) + 1
        ));
        colStatus1.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        colBrokenLink1.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("url"));
        colAnchorText1.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("anchorText"));
        colWebpageLink1.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("webpageUrl"));

        colNumber1.setSortable(false);
        colStatus1.setSortable(false);
        colBrokenLink1.setSortable(false);
        colAnchorText1.setSortable(false);
        colWebpageLink1.setSortable(false);

    }

    private void setupWebpageTableColumns() {
        colNumber2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.08));
        colStatus2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.18));
        colWebpageLink2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.42));
        colLinkCount2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.12));
        colAccessTime2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.20));

        colNumber2.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                (currentPage - 1) * ROWS_PER_PAGE + pageWebpages.indexOf(cell.getValue()) + 1
        ));

        colStatus2.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        colWebpageLink2.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("url"));

        // IntegerProperty -> ObservableValue<Integer>
        colLinkCount2.setCellValueFactory(cell -> cell.getValue().linkCountProperty().asObject());

        // Access time (Instant) -> formatted String
        colAccessTime2.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> TimeUtil.format(cell.getValue().getAccessTime()),
                cell.getValue().accessTimeProperty()
        ));

        colNumber2.setSortable(false);
        colStatus2.setSortable(false);
        colWebpageLink2.setSortable(false);
        colLinkCount2.setSortable(false);
        colAccessTime2.setSortable(false);

    }

    // ===================== Algorithm dropdown =====================
    private void setupAlgoChoiceBox() {
        algoChoiceBox.setItems(FXCollections.observableArrayList(
                "Breadth-First Search (BFS)",
                "Depth-First Search (DFS)"
        ));
        algoChoiceBox.getSelectionModel().select(0);
    }

    // ===================== Toggle view =====================
    private void setupToggleView() {
        ToggleGroup group = new ToggleGroup();
        brokenLinkToggle.setToggleGroup(group);
        webpageToggle.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == brokenLinkToggle) {
                showBrokenLinkTable();
            } else if (newT == webpageToggle) {
                showWebpageTable();
            }
        });

        brokenLinkToggle.setSelected(true);
        showBrokenLinkTable();
    }

    private void showBrokenLinkTable() {
        activeView = ActiveView.BROKEN;
        brokenLinkTable.setVisible(true);
        webpageTable.setVisible(false);
        currentPage = 1;
        updatePagination();
    }

    private void showWebpageTable() {
        activeView = ActiveView.WEBPAGE;
        brokenLinkTable.setVisible(false);
        webpageTable.setVisible(true);
        currentPage = 1;
        updatePagination();
    }


    // ===================== Actions =====================
    @FXML
    public void onCheckClick() {
        String url = seedUrl.getText();
        if (url == null || url.isBlank()) {
            showInfo("Input Error", "Seed URL tidak boleh kosong.");
            return;
        }

        // prepend scheme
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        String algoLabel = algoChoiceBox.getValue();

        if (algoLabel == null) {
            showInfo("Input Error", "Choose an algorithm first.");
            return;
        }

        Algorithm algorithm;

        try {
            algorithm = Algorithm.fromLabel(algoLabel);
        } catch (IllegalArgumentException ex) {
            showInfo("Input Error", "Unknown algorithm: " + algoLabel);
            return;
        }


        resetData();
        summary.start(Instant.now());
        setButtonsByStatus(ExecutionStatus.CHECKING);
        startDurationTicker();

        service = new Service();
        service.setOnBrokenLink(onBrokenLink());
        service.setOnWebpage(onWebpage());
        service.setOnTotalLinkUpdate(this::onCountsUpdate);
        service.setOnComplete(this::onComplete);
        service.setOnError(this::onError);

        service.startCrawling(url, algorithm);

    }

    @FXML
    public void onStopClick() {
        if (service != null) service.stop();
        summary.stop(Instant.now());
        stopDurationTicker();
        setButtonsByStatus(ExecutionStatus.STOPPED);

    }

    @FXML
    public void onExportClick() {
        showInfo("Export", "Fitur export belum diimplementasikan.");
    }

    // ===================== Service wiring helpers =====================
    private Consumer<BrokenLink> onBrokenLink() {
        return bl -> Platform.runLater(() -> {
            allBroken.add(bl);
            if (activeView == ActiveView.BROKEN) updatePagination(); // NEW
            summary.setCounts(summary.getTotalWebpages(), summary.getTotalLinks(), allBroken.size());
        });
    }

    private Consumer<WebpageLink> onWebpage() {
        return wp -> Platform.runLater(() -> {
            webpages.add(wp);
            if (activeView == ActiveView.WEBPAGE) updatePagination(); // NEW
            summary.setCounts(webpages.size(), summary.getTotalLinks(), summary.getTotalBrokenLinks());
        });
    }


    private void onCountsUpdate(int totalLinks) {
        Platform.runLater(() ->
                summary.setCounts(summary.getTotalWebpages(), totalLinks, summary.getTotalBrokenLinks())
        );
    }

    private void onComplete() {
        Platform.runLater(() -> {
            summary.finish(Instant.now());
            stopDurationTicker();
            setButtonsByStatus(ExecutionStatus.COMPLETED);
        });
    }

    private void onError(String message) {
        Platform.runLater(() -> {
            summary.error(Instant.now());
            stopDurationTicker();
            setButtonsByStatus(ExecutionStatus.ERROR);
            showInfo("Error", message == null ? "Terjadi kesalahan saat crawling." : message);
        });
    }

    // ===================== Buttons state =====================
    private void setButtonsByStatus(ExecutionStatus status) {
        switch (status) {
            case IDLE, ERROR -> {
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(true);
            }
            case CHECKING -> {
                checkButton.setDisable(true);
                stopButton.setDisable(false);
                exportButton.setDisable(true);
            }
            case COMPLETED, STOPPED -> {
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(false);
            }
        }
    }

    // ===================== Duration ticker =====================
    private void startDurationTicker() {
        stopDurationTicker();
        durationScheduler = Executors.newSingleThreadScheduledExecutor();
        durationScheduler.scheduleAtFixedRate(
                () -> Platform.runLater(summary::tick),
                0, 1, TimeUnit.SECONDS
        );
    }

    private void stopDurationTicker() {
        if (durationScheduler != null) {
            durationScheduler.shutdownNow();
            durationScheduler = null;
        }
    }

    // ===================== Reset & Pagination =====================
    private void resetData() {
        allBroken.clear();
        pageBroken.clear();
        webpages.clear();
        pageWebpages.clear(); // NEW

        currentPage = 1;
        totalPageCount = 1;
        customPagination.getChildren().clear();

        activeView = ActiveView.BROKEN; // optional: default to broken view
        summary.setCounts(0, 0, 0);
    }


    private void updatePagination() {
        customPagination.getChildren().clear();

        int totalItems = (activeView == ActiveView.BROKEN)
                ? allBroken.size()
                : webpages.size();

        totalPageCount = Math.max(1, (int) Math.ceil((double) totalItems / ROWS_PER_PAGE));
        if (currentPage > totalPageCount) currentPage = totalPageCount;

        HBox box = new HBox(5);
        box.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button prev = createPageButton("⯇", () -> goToPage(currentPage - 1), currentPage == 1);
        Button next = createPageButton("⯈", () -> goToPage(currentPage + 1), currentPage == totalPageCount);
        box.getChildren().add(prev);

        int start = Math.max(1, currentPage - MAX_PAGE_BUTTONS / 2);
        int end = Math.min(totalPageCount, start + MAX_PAGE_BUTTONS - 1);

        for (int i = start; i <= end; i++) {
            final int page = i;
            Button btn = createPageButton(String.valueOf(i), () -> goToPage(page), false);
            if (i == currentPage) btn.getStyleClass().add("current-page");
            box.getChildren().add(btn);
        }

        box.getChildren().add(next);
        customPagination.getChildren().addAll(
                box, new Label("Page " + currentPage + " / " + totalPageCount)
        );

        updateCurrentPage(currentPage - 1);
    }

    private void updateCurrentPage(int pageIndexZeroBased) {
        int totalItems = (activeView == ActiveView.BROKEN) ? allBroken.size() : webpages.size();
        int from = Math.max(0, pageIndexZeroBased * ROWS_PER_PAGE);
        int to = Math.min(from + ROWS_PER_PAGE, totalItems);
        if (from > to) from = to;

        if (activeView == ActiveView.BROKEN) {
            pageBroken.setAll(allBroken.subList(from, to));
        } else {
            pageWebpages.setAll(webpages.subList(from, to));
        }
    }


    private Button createPageButton(String text, Runnable action, boolean disabled) {
        Button btn = new Button(text);
        btn.setMinWidth(PAGE_BUTTON_WIDTH);
        btn.setDisable(disabled);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPageCount) return;
        currentPage = page;
        updatePagination();
    }

    // ===================== Alert util =====================
    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
