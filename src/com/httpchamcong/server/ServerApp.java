package com.httpchamcong.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ServerApp extends Application {

    private final HttpSocketServer server = new HttpSocketServer();

    private Label statusBadge;
    private Label activeClientsVal;
    private Label totalClientsVal;
    private Label totalRequestsVal;
    private Label uptimeVal;
    private Label serverIpLabel;
    private TextArea logArea;
    private Button startBtn;
    private Button stopBtn;

    // ⭐ Live JSON — raw HTTP Response Body
    private TextArea liveJsonArea;
    private Label liveJsonMeta;
    private CheckBox liveJsonAutoScroll;
    private long lastLiveJsonId = 0;

    private TableView<ClientManager.ClientInfo> clientTable;
    private ObservableList<ClientManager.ClientInfo> clientList;

    // ⭐ Request log — dùng DATA THẬT từ ServerStats
    public static class RequestLog {
        public String time, method, path, status, duration, bytes, clientIp;
        public RequestLog(String time, String method, String path, String status,
                          String duration, String bytes, String clientIp) {
            this.time = time; this.method = method; this.path = path;
            this.status = status; this.duration = duration;
            this.bytes = bytes; this.clientIp = clientIp;
        }
    }
    private ObservableList<RequestLog> requestLogs = FXCollections.observableArrayList();
    private TableView<RequestLog> requestTable;

    // ⭐ Charts
    private LineChart<String, Number> requestChart;
    private XYChart.Series<String, Number> requestSeries;
    private PieChart statusPieChart;
    private Label methodGetVal, methodPostVal, methodDeleteVal, methodPutVal, methodPatchVal;
    private Label reqPerSecVal, dataTransferVal, avgLatencyVal, clientsOnlineVal;

    // ⭐ Settings toggles
    private CheckBox keepAliveCheck;
    private CheckBox gzipCheck;
    private CheckBox corsCheck;
    private CheckBox rateLimitCheck;

    // ⭐ Tester
    private TextArea rawRequestArea;
    private TextArea rawResponseArea;
    private ComboBox<String> testerMethodBox;
    private TextField testerPathField;
    private TextArea testerBodyArea;

    private Set<String> previousOnline = new HashSet<>();
    
    // ⭐ Theme toggle
    private boolean isDarkMode = false;

    // ===== PALETTE - ENTERPRISE NAVY / TEAL / GOLD =====
    private static final String C_BG       = "#f4f7f9";
    private static final String C_CARD     = "#ffffff";
    private static final String C_CARD2    = "#f8fafc";
    private static final String C_BORDER   = "#dce6ee";
    private static final String C_TEXT     = "#0b1f33";
    private static final String C_TEXT2    = "#24445f";
    private static final String C_MUTED    = "#64748b";
    private static final String C_ACCENT   = "#0f4c75";
    private static final String C_ACCENT2  = "#0b3b5c";
    private static final String C_GREEN    = "#16a34a";
    private static final String C_ORANGE   = "#b45309";
    private static final String C_BLUE     = "#0f766e";
    private static final String C_SKY      = "#0f766e";
    private static final String C_RED      = "#dc2626";
    private static final String C_TEAL     = "#0b3b5c";
    private static final String C_AMBER    = "#b78a2e";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Server Dashboard - HttpSocketChamCong");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + C_BG + ";");

        // ============ HEADER ============
        HBox header = new HBox(15);
        header.setPadding(new Insets(18, 32, 18, 32));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER + "; -fx-border-width: 0 0 1 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");

        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(46, 46);
        logoCircle.setMaxSize(46, 46);
        logoCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_TEAL + "); -fx-background-radius: 23; -fx-effect: dropshadow(gaussian, rgba(15,76,117,0.45), 10, 0, 0, 3);");
        FontIcon logoIcon = new FontIcon(FontAwesomeSolid.SERVER);
        logoIcon.setIconSize(22);
        logoIcon.setIconColor(Color.WHITE);
        logoCircle.getChildren().add(logoIcon);

        VBox titleBox = new VBox(2);
        Label title = new Label("ATTENDANCE SERVER CONTROL");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));
        Label sub = new Label("Enterprise Attendance  |  HTTP/1.1 over TCP Socket  |  Port " + HttpSocketServer.PORT);
        sub.setFont(Font.font("Consolas", 11));
        sub.setTextFill(Color.web(C_MUTED));
        titleBox.getChildren().addAll(title, sub);

        HBox logoBox = new HBox(12, logoCircle, titleBox);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button openWebBtn = new Button("  WEB CLIENT");
        FontIcon globeIcon = new FontIcon(FontAwesomeSolid.GLOBE);
        globeIcon.setIconSize(13);
        globeIcon.setIconColor(Color.WHITE);
        openWebBtn.setGraphic(globeIcon);
        openWebBtn.setPrefHeight(38);
        openWebBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        openWebBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0 16 0 16; -fx-effect: dropshadow(gaussian, rgba(15,76,117,0.30), 10, 0, 0, 3);");
        openWebBtn.setOnAction(e -> getHostServices().showDocument("http://localhost:" + HttpSocketServer.PORT + "/"));

        Button openAdminBtn = new Button("  ADMIN");
        FontIcon shieldIcon = new FontIcon(FontAwesomeSolid.SHIELD_ALT);
        shieldIcon.setIconSize(13);
        shieldIcon.setIconColor(Color.web(C_ACCENT));
        openAdminBtn.setGraphic(shieldIcon);
        openAdminBtn.setPrefHeight(38);
        openAdminBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        openAdminBtn.setStyle("-fx-background-color: #edf5f9; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0 16 0 16; -fx-border-color: #b9d5e2; -fx-border-radius: 10; -fx-border-width: 1;");
        openAdminBtn.setOnAction(e -> getHostServices().showDocument("http://localhost:" + HttpSocketServer.PORT + "/#/admin-users"));

        // ⭐ Nút Dark/Light toggle
        Button themeToggleBtn = new Button("🌙");
        themeToggleBtn.setPrefSize(42, 42);
        themeToggleBtn.setFont(Font.font("Segoe UI", 16));
        themeToggleBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 21; -fx-cursor: hand; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 21; -fx-border-width: 1;");
        themeToggleBtn.setOnMouseEntered(e -> themeToggleBtn.setStyle("-fx-background-color: #edf5f9; -fx-background-radius: 21; -fx-cursor: hand; -fx-border-color: " + C_ACCENT + "; -fx-border-radius: 21; -fx-border-width: 1;"));
        themeToggleBtn.setOnMouseExited(e -> themeToggleBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 21; -fx-cursor: hand; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 21; -fx-border-width: 1;"));

        serverIpLabel = new Label();
        serverIpLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        serverIpLabel.setTextFill(Color.web(C_ACCENT));
        serverIpLabel.setStyle("-fx-background-color: #edf5f9; -fx-background-radius: 20; -fx-padding: 8 16 8 16; -fx-border-color: #b9d5e2; -fx-border-radius: 20; -fx-border-width: 1;");

        statusBadge = new Label("  STOPPED");
        FontIcon stopIcon = new FontIcon(FontAwesomeSolid.STOP_CIRCLE);
        stopIcon.setIconSize(13);
        stopIcon.setIconColor(Color.web(C_RED));
        statusBadge.setGraphic(stopIcon);
        statusBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        statusBadge.setTextFill(Color.web(C_RED));
        statusBadge.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 20; -fx-padding: 8 18 8 18; -fx-border-color: #fecaca; -fx-border-radius: 20; -fx-border-width: 1;");

        header.getChildren().addAll(logoBox, sp, openWebBtn, openAdminBtn, themeToggleBtn, serverIpLabel, statusBadge);

        // ============ LEFT PANEL ============
        VBox leftPanel = new VBox(18);
        leftPanel.setPrefWidth(320);
        leftPanel.setMinWidth(320);
        leftPanel.setPadding(new Insets(22, 0, 22, 22));

        VBox controlCard = new VBox(14);
        controlCard.setPadding(new Insets(22));
        controlCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox ctrlHeader = new HBox(10);
        ctrlHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon ctrlIcn = new FontIcon(FontAwesomeSolid.COG);
        ctrlIcn.setIconSize(14);
        ctrlIcn.setIconColor(Color.web(C_ACCENT));
        Label ctrlTitle = new Label("  Điều khiển Server");
        ctrlTitle.setGraphic(ctrlIcn);
        ctrlTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        ctrlTitle.setTextFill(Color.web(C_TEXT));
        ctrlHeader.getChildren().add(ctrlTitle);

        startBtn = new Button("  START SERVER");
        FontIcon playIcn = new FontIcon(FontAwesomeSolid.PLAY);
        playIcn.setIconSize(14);
        playIcn.setIconColor(Color.WHITE);
        startBtn.setGraphic(playIcn);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setPrefHeight(48);
        startBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        startBtn.setStyle("-fx-background-color: " + C_GREEN + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(22,163,74,0.4), 12, 0, 0, 4);");
        startBtn.setOnAction(e -> startServer());

        stopBtn = new Button("  STOP SERVER");
        FontIcon stopIcn = new FontIcon(FontAwesomeSolid.STOP);
        stopIcn.setIconSize(14);
        stopIcn.setIconColor(Color.web(C_MUTED));
        stopBtn.setGraphic(stopIcn);
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setPrefHeight(48);
        stopBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        stopBtn.setDisable(true);
        stopBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_MUTED + "; -fx-background-radius: 10; -fx-cursor: not-allowed; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
        stopBtn.setOnAction(e -> stopServer());

        HBox portInfo = new HBox(8);
        portInfo.setAlignment(Pos.CENTER_LEFT);
        FontIcon infoIcn = new FontIcon(FontAwesomeSolid.INFO_CIRCLE);
        infoIcn.setIconSize(12);
        infoIcn.setIconColor(Color.web(C_MUTED));
        Label portLbl = new Label("Port " + HttpSocketServer.PORT + "  •  " + HttpSocketServer.POOL_SIZE + " threads");
        portLbl.setFont(Font.font("Consolas", 11));
        portLbl.setTextFill(Color.web(C_MUTED));
        portInfo.getChildren().addAll(infoIcn, portLbl);

        controlCard.getChildren().addAll(ctrlHeader, startBtn, stopBtn, portInfo);

        VBox statsCard = new VBox(14);
        statsCard.setPadding(new Insets(22));
        statsCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox statsHeader = new HBox(10);
        statsHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon statIcn = new FontIcon(FontAwesomeSolid.CHART_BAR);
        statIcn.setIconSize(14);
        statIcn.setIconColor(Color.web(C_GREEN));
        Label statsTitle = new Label("  Thống kê Server");
        statsTitle.setGraphic(statIcn);
        statsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        statsTitle.setTextFill(Color.web(C_TEXT));
        statsHeader.getChildren().add(statsTitle);

        activeClientsVal = new Label("0");
        totalClientsVal  = new Label("0");
        totalRequestsVal = new Label("0");
        uptimeVal        = new Label("00:00:00");

        VBox statsList = new VBox(12);
        statsList.getChildren().addAll(
            statRow(FontAwesomeSolid.USERS, "Client online", activeClientsVal, C_GREEN),
            statRow(FontAwesomeSolid.USER_PLUS, "Tổng client", totalClientsVal, C_BLUE),
            statRow(FontAwesomeSolid.EXCHANGE_ALT, "Tổng request", totalRequestsVal, C_SKY),
            statRow(FontAwesomeSolid.CLOCK, "Uptime", uptimeVal, C_ORANGE)
        );

        statsCard.getChildren().addAll(statsHeader, statsList);

        VBox threadCard = new VBox(12);
        threadCard.setPadding(new Insets(22));
        threadCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox threadHeader = new HBox(10);
        threadHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon threadIcn = new FontIcon(FontAwesomeSolid.MICROCHIP);
        threadIcn.setIconSize(14);
        threadIcn.setIconColor(Color.web(C_TEAL));
        Label threadTitle = new Label("  Thread Pool");
        threadTitle.setGraphic(threadIcn);
        threadTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        threadTitle.setTextFill(Color.web(C_TEXT));
        threadHeader.getChildren().add(threadTitle);

        ProgressBar threadBar = new ProgressBar(0);
        threadBar.setPrefWidth(Double.MAX_VALUE);
        threadBar.setPrefHeight(8);
        threadBar.setStyle("-fx-accent: " + C_TEAL + ";");

        Label threadLbl = new Label("0 / " + HttpSocketServer.POOL_SIZE + " threads");
        threadLbl.setFont(Font.font("Consolas", 11));
        threadLbl.setTextFill(Color.web(C_MUTED));

        threadCard.getChildren().addAll(threadHeader, threadBar, threadLbl);

        leftPanel.getChildren().addAll(controlCard, statsCard, threadCard);

        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background: " + C_BG + "; -fx-background-color: " + C_BG + "; -fx-border-color: transparent;");

        // ============ CENTER — Tabs ============
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: transparent; -fx-tab-min-height: 42px; -fx-tab-max-height: 42px;");

        Tab dashTab = new Tab("   📊 Dashboard   ");
        dashTab.setContent(buildDashboardPane());

        Tab reqTab = new Tab("   📡 Requests   ");
        reqTab.setContent(buildRequestPane());

        Tab logTab = new Tab("   📜 Live Log   ");
        logTab.setContent(buildLogPane());

        Tab jsonTab = new Tab("   🧾 Live JSON   ");
        jsonTab.setContent(buildLiveJsonPane());

        Tab epTab = new Tab("   🔌 Endpoints   ");
        epTab.setContent(buildEndpointPane());

        Tab testerTab = new Tab("   🧪 HTTP Tester   ");
        testerTab.setContent(buildTesterPane());

        Tab settingsTab = new Tab("   ⚙️ Settings   ");
        settingsTab.setContent(buildSettingsPane());

        Tab clientTab = new Tab("   👥 Clients   ");
        clientTab.setContent(buildClientTablePane());

        tabs.getTabs().addAll(dashTab, reqTab, logTab, jsonTab, epTab, testerTab, settingsTab, clientTab);

        VBox centerPanel = new VBox(tabs);
        centerPanel.setPadding(new Insets(22));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        root.setTop(header);
        root.setLeft(leftScroll);
        root.setCenter(centerPanel);

        // ============ LOG LISTENER ============
        server.setLogListener(msg -> Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(msg + "\n");
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        }));

        startStatsTimer();
        startChartUpdateTimer(threadBar, threadLbl);
        updateServerIp();

        Scene scene = new Scene(root, 1500, 900);

        // ⭐ Load theme AtlantaFX — primer-light.css
        try {
            java.io.File cssFile = new java.io.File("lib/primer-light.css");
            if (cssFile.exists()) {
                scene.getStylesheets().add(cssFile.toURI().toString());
                System.out.println("[THEME] Loaded primer-light.css");
            } else {
                System.out.println("[THEME] Không tìm thấy lib/primer-light.css — dùng CSS mặc định");
            }
        } catch (Exception e) {
            System.out.println("[THEME] Lỗi load CSS: " + e.getMessage());
        }

        // ⭐ Load CSS custom (đè lên theme ở những chỗ muốn)
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));

        // ⭐ Theme toggle handler
        themeToggleBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            scene.getStylesheets().clear();
            try {
                String cssName = isDarkMode ? "lib/primer-dark.css" : "lib/primer-light.css";
                java.io.File f = new java.io.File(cssName);
                if (f.exists()) scene.getStylesheets().add(f.toURI().toString());
            } catch (Exception ex) {}
            scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));
            themeToggleBtn.setText(isDarkMode ? "☀️" : "🌙");
        });

        stage.setScene(scene);
        stage.show();

        log("=== Server Dashboard sẵn sàng ===");
        log("Bấm 'START SERVER' để khởi động.");

        // Cho phép chạy full project bằng run.bat: ServerApp --autostart
        if (getParameters().getRaw().contains("--autostart")) {
            Platform.runLater(this::startServer);
        }
    }

    private String getGlobalCss() {
        return
            ".root { -fx-font-family: 'Segoe UI'; -fx-background-color: " + C_BG + "; }" +
            ".label { -fx-text-fill: " + C_TEXT + "; }" +
            ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: transparent; }" +
            ".tab-pane .tab { -fx-background-color: transparent; -fx-border-color: transparent transparent " + C_BORDER + " transparent; -fx-border-width: 0 0 2 0; -fx-padding: 8 16 8 16; }" +
            ".tab-pane .tab:selected { -fx-background-color: " + C_CARD + "; -fx-border-color: transparent transparent " + C_ACCENT + " transparent; }" +
            ".tab-pane .tab .tab-label { -fx-text-fill: " + C_TEXT2 + "; -fx-font-weight: bold; -fx-font-size: 13; }" +
            ".tab-pane .tab:selected .tab-label { -fx-text-fill: " + C_ACCENT + "; }" +
            ".tab-pane .tab:hover .tab-label { -fx-text-fill: " + C_ACCENT + "; }" +
            ".table-view { -fx-background-color: " + C_CARD + "; -fx-control-inner-background: " + C_CARD + "; -fx-table-cell-border-color: " + C_BORDER + "; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: " + C_BORDER + "; -fx-border-width: 1; }" +
            ".table-view .column-header-background { -fx-background-color: " + C_CARD2 + "; -fx-background-radius: 12 12 0 0; }" +
            ".table-view .column-header, .table-view .filler { -fx-background-color: transparent; -fx-border-color: transparent " + C_BORDER + " " + C_BORDER + " transparent; -fx-size: 42; }" +
            ".table-view .column-header .label { -fx-text-fill: " + C_TEXT + "; -fx-font-weight: bold; -fx-alignment: center-left; -fx-padding: 0 12 0 12; }" +
            ".table-view .table-cell { -fx-text-fill: " + C_TEXT + "; -fx-border-color: transparent " + C_BORDER + " transparent transparent; -fx-padding: 9 12 9 12; -fx-alignment: center-left; }" +
            ".table-view .table-row-cell { -fx-background-color: " + C_CARD + "; -fx-cell-size: 44; -fx-border-color: transparent transparent " + C_BORDER + " transparent; -fx-border-width: 0 0 1 0; }" +
            ".table-view .table-row-cell:odd { -fx-background-color: " + C_CARD2 + "; }" +
            ".table-view .table-row-cell:hover { -fx-background-color: #edf5f9; }" +
            ".table-view .table-row-cell:selected { -fx-background-color: #dceaf1; }" +
            ".scroll-pane { -fx-background-color: transparent; }" +
            ".scroll-pane > .viewport { -fx-background-color: transparent; }" +
            ".scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; }" +
            ".scroll-bar .thumb { -fx-background-color: " + C_BORDER + "; -fx-background-radius: 8; }" +
            ".scroll-bar .thumb:hover { -fx-background-color: " + C_MUTED + "; }" +
            ".text-field { -fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_MUTED + "; }" +
            ".combo-box .list-cell { -fx-text-fill: " + C_TEXT + "; -fx-background-color: " + C_CARD + "; }" +
            ".combo-box-popup .list-view .list-cell:filled:hover { -fx-background-color: #edf5f9; -fx-text-fill: " + C_ACCENT + "; }" +
            ".combo-box-popup .list-view .list-cell:filled:selected { -fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; }" +
            ".chart { -fx-background-color: transparent; }" +
            ".chart-plot-background { -fx-background-color: " + C_CARD + "; }" +
            ".chart-vertical-grid-lines, .chart-horizontal-grid-lines { -fx-stroke: " + C_BORDER + "; -fx-stroke-dash-array: 3 3; }" +
            ".chart-alternative-row-fill { -fx-fill: transparent; }" +
            ".chart-axis { -fx-tick-label-fill: " + C_MUTED + "; -fx-font-size: 11; }" +
            ".chart-legend { -fx-background-color: transparent; }" +
            ".chart-legend-item { -fx-text-fill: " + C_TEXT2 + "; }" +
            ".default-color0.chart-series-line { -fx-stroke: " + C_ACCENT + "; -fx-stroke-width: 2.5; }" +
            ".default-color0.chart-line-symbol { -fx-background-color: " + C_ACCENT + ", white; }" +
            ".check-box .box { -fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 4; -fx-padding: 4; }" +
            ".check-box:selected .mark { -fx-background-color: white; }" +
            ".check-box:selected .box { -fx-background-color: " + C_ACCENT + "; -fx-border-color: " + C_ACCENT + "; }";
    }

    // ============================================================
    // TAB 1: DASHBOARD
    // ============================================================
    private VBox buildDashboardPane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(FontAwesomeSolid.CHART_LINE);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_ACCENT));
        Label title = new Label("  Tổng quan hoạt động Server");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label liveTag = new Label("●  LIVE");
        liveTag.setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: #d1fae5; -fx-background-radius: 16; -fx-padding: 6 14 6 14; -fx-border-color: #6ee7b7; -fx-border-radius: 16; -fx-border-width: 1;");

        headerRow.getChildren().addAll(title, sp, liveTag);

        HBox cards = new HBox(16);
        cards.setMaxWidth(Double.MAX_VALUE);

        VBox c1 = bigStatCardRef(FontAwesomeSolid.EXCHANGE_ALT, "Requests/sec", "0", "req/s", C_ACCENT);
        reqPerSecVal = (Label) c1.getProperties().get("valueLabel");

        VBox c2 = bigStatCardRef(FontAwesomeSolid.USERS, "Clients Online", "0", "người", C_GREEN);
        clientsOnlineVal = (Label) c2.getProperties().get("valueLabel");

        VBox c3 = bigStatCardRef(FontAwesomeSolid.DATABASE, "Data Transfer", "0", "KB", C_BLUE);
        dataTransferVal = (Label) c3.getProperties().get("valueLabel");

        VBox c4 = bigStatCardRef(FontAwesomeSolid.TACHOMETER_ALT, "Avg Latency", "0", "ms", C_ORANGE);
        avgLatencyVal = (Label) c4.getProperties().get("valueLabel");

        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        HBox.setHgrow(c3, Priority.ALWAYS);
        HBox.setHgrow(c4, Priority.ALWAYS);
        cards.getChildren().addAll(c1, c2, c3, c4);

        HBox chartsRow = new HBox(16);
        chartsRow.setMaxWidth(Double.MAX_VALUE);

        VBox lineCard = new VBox(12);
        lineCard.setPadding(new Insets(20));
        lineCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");
        HBox.setHgrow(lineCard, Priority.ALWAYS);
        lineCard.setPrefWidth(700);

        HBox lineHeader = new HBox(10);
        lineHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon lineIcn = new FontIcon(FontAwesomeSolid.CHART_LINE);
        lineIcn.setIconSize(14);
        lineIcn.setIconColor(Color.web(C_ACCENT));
        Label lineTitle = new Label("  Requests per second (real-time)");
        lineTitle.setGraphic(lineIcn);
        lineTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lineTitle.setTextFill(Color.web(C_TEXT));
        lineHeader.getChildren().add(lineTitle);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Requests");
        yAxis.setMinorTickVisible(false);

        requestChart = new LineChart<>(xAxis, yAxis);
        requestChart.setLegendVisible(false);
        requestChart.setAnimated(false);
        requestChart.setCreateSymbols(true);
        requestChart.setPrefHeight(280);
        requestChart.setStyle("-fx-background-color: transparent;");

        requestSeries = new XYChart.Series<>();
        requestSeries.setName("Requests");
        requestChart.getData().add(requestSeries);

        for (int i = 19; i >= 0; i--) {
            java.time.LocalTime t = java.time.LocalTime.now().minusSeconds(i * 3);
            requestSeries.getData().add(new XYChart.Data<>(
                String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond()), 0));
        }

        lineCard.getChildren().addAll(lineHeader, requestChart);

        VBox pieCard = new VBox(12);
        pieCard.setPadding(new Insets(20));
        pieCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");
        pieCard.setPrefWidth(400);

        HBox pieHeader = new HBox(10);
        pieHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon pieIcn = new FontIcon(FontAwesomeSolid.CHART_PIE);
        pieIcn.setIconSize(14);
        pieIcn.setIconColor(Color.web(C_SKY));
        Label pieTitle = new Label("  HTTP Status Codes");
        pieTitle.setGraphic(pieIcn);
        pieTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        pieTitle.setTextFill(Color.web(C_TEXT));
        pieHeader.getChildren().add(pieTitle);

        statusPieChart = new PieChart();
        statusPieChart.setLegendVisible(true);
        statusPieChart.setAnimated(false);
        statusPieChart.setPrefHeight(280);
        statusPieChart.setLabelLineLength(10);
        statusPieChart.setLabelsVisible(false);
        statusPieChart.setStyle("-fx-background-color: transparent;");

        statusPieChart.getData().addAll(
            new PieChart.Data("200 OK", 0),
            new PieChart.Data("201 Created", 0),
            new PieChart.Data("400 Bad Request", 0),
            new PieChart.Data("401 Unauthorized", 0),
            new PieChart.Data("404 Not Found", 0),
            new PieChart.Data("5xx Server Error", 0)
        );

        pieCard.getChildren().addAll(pieHeader, statusPieChart);
        chartsRow.getChildren().addAll(lineCard, pieCard);

        VBox methodCard = new VBox(12);
        methodCard.setPadding(new Insets(20));
        methodCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox methodHeader = new HBox(10);
        methodHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon methodIcn = new FontIcon(FontAwesomeSolid.CODE);
        methodIcn.setIconSize(14);
        methodIcn.setIconColor(Color.web(C_TEAL));
        Label methodTitle = new Label("  HTTP Methods Distribution");
        methodTitle.setGraphic(methodIcn);
        methodTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        methodTitle.setTextFill(Color.web(C_TEXT));
        methodHeader.getChildren().add(methodTitle);

        HBox methodBars = new HBox(20);
        methodBars.setAlignment(Pos.CENTER_LEFT);

        methodGetVal    = new Label("0");
        methodPostVal   = new Label("0");
        methodDeleteVal = new Label("0");
        methodPutVal    = new Label("0");
        methodPatchVal  = new Label("0");

        methodBars.getChildren().addAll(
            methodStatBar("GET", methodGetVal, C_BLUE),
            methodStatBar("POST", methodPostVal, C_GREEN),
            methodStatBar("DELETE", methodDeleteVal, C_RED),
            methodStatBar("PUT", methodPutVal, C_ORANGE),
            methodStatBar("PATCH", methodPatchVal, C_SKY)
        );

        methodCard.getChildren().addAll(methodHeader, methodBars);

        box.getChildren().addAll(headerRow, cards, chartsRow, methodCard);
        return box;
    }

    private VBox bigStatCardRef(FontAwesomeSolid icon, String title, String value, String unit, String color) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setMinWidth(200);
        card.setPrefHeight(130);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(42, 42);
        iconCircle.setMaxSize(42, 42);
        iconCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 21;");
        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icn);

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        t.setTextFill(Color.web(C_TEXT2));
        top.getChildren().addAll(iconCircle, t);

        HBox valRow = new HBox(6);
        valRow.setAlignment(Pos.BASELINE_LEFT);
        Label v = new Label(value);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        v.setTextFill(Color.web(color));
        Label u = new Label(unit);
        u.setFont(Font.font("Segoe UI", 12));
        u.setTextFill(Color.web(C_MUTED));
        valRow.getChildren().addAll(v, u);

        card.getChildren().addAll(top, valRow);
        card.getProperties().put("valueLabel", v);
        return card;
    }

    private VBox methodStatBar(String method, Label countLbl, String color) {
        VBox v = new VBox(6);
        v.setAlignment(Pos.CENTER);
        v.setPrefWidth(140);

        Label lbl = new Label(method);
        lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web(color));
        lbl.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 6; -fx-padding: 4 10 4 10;");

        countLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        countLbl.setTextFill(Color.web(C_TEXT));

        v.getChildren().addAll(lbl, countLbl);
        return v;
    }

    // ============================================================
    // TAB 2: REQUESTS
    // ============================================================
    private VBox buildRequestPane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.SATELLITE_DISH);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_BLUE));

        Label title = new Label("  Nhật ký HTTP Requests (thời gian thực)");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label countLbl = new Label("0 requests");
        countLbl.setStyle("-fx-text-fill: " + C_ACCENT + "; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: #edf5f9; -fx-background-radius: 16; -fx-padding: 6 14 6 14; -fx-border-color: #b9d5e2; -fx-border-radius: 16; -fx-border-width: 1;");

        Button clearBtn = new Button("  Clear view");
        FontIcon clearIcn = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        clearIcn.setIconSize(11);
        clearIcn.setIconColor(Color.web(C_RED));
        clearBtn.setGraphic(clearIcn);
        clearBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        clearBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14 6 14; -fx-border-color: #fecaca; -fx-border-radius: 8; -fx-border-width: 1;");
        clearBtn.setOnAction(e -> {
            requestLogs.clear();
            countLbl.setText("0 requests");
        });

        headerRow.getChildren().addAll(title, sp, countLbl, clearBtn);

        requestTable = new TableView<>();
        requestTable.setItems(requestLogs);
        requestTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        requestTable.setPrefHeight(650);
        VBox.setVgrow(requestTable, Priority.ALWAYS);

        TableColumn<RequestLog, String> cTime = new TableColumn<>("⏰ Time");
        cTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().time));
        cTime.setPrefWidth(110);

        TableColumn<RequestLog, String> cMethod = new TableColumn<>("📤 Method");
        cMethod.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().method));
        cMethod.setPrefWidth(100);
        cMethod.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    String color = C_MUTED;
                    if ("GET".equals(item)) color = C_BLUE;
                    else if ("POST".equals(item)) color = C_GREEN;
                    else if ("DELETE".equals(item)) color = C_RED;
                    else if ("PUT".equals(item)) color = C_ORANGE;
                    else if ("PATCH".equals(item)) color = C_SKY;
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-family: 'Consolas';");
                }
            }
        });

        TableColumn<RequestLog, String> cPath = new TableColumn<>("🔗 Path");
        cPath.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().path));
        cPath.setPrefWidth(300);
        cPath.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: " + C_TEXT + ";");
                }
            }
        });

        TableColumn<RequestLog, String> cStatus = new TableColumn<>("📊 Status");
        cStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        cStatus.setPrefWidth(90);
        cStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    String color = C_MUTED;
                    String bg = "#f1f5f9";
                    if (item.startsWith("2")) { color = C_GREEN; bg = "#d1fae5"; }
                    else if (item.startsWith("3")) { color = C_BLUE; bg = "#dceaf1"; }
                    else if (item.startsWith("4")) { color = C_ORANGE; bg = "#fed7aa"; }
                    else if (item.startsWith("5")) { color = C_RED; bg = "#fecaca"; }
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-background-color: " + bg + "; -fx-background-radius: 6; -fx-padding: 2 8 2 8;");
                }
            }
        });

        TableColumn<RequestLog, String> cDur = new TableColumn<>("⚡ Duration");
        cDur.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().duration));
        cDur.setPrefWidth(110);

        TableColumn<RequestLog, String> cBytes = new TableColumn<>("📦 Bytes");
        cBytes.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().bytes));
        cBytes.setPrefWidth(90);

        TableColumn<RequestLog, String> cIp = new TableColumn<>("🌐 Client IP");
        cIp.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().clientIp));
        cIp.setPrefWidth(150);
        cIp.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: " + C_MUTED + ";");
                }
            }
        });

        requestTable.getColumns().addAll(cTime, cMethod, cPath, cStatus, cDur, cBytes, cIp);

        requestLogs.addListener((javafx.collections.ListChangeListener<RequestLog>) c -> {
            countLbl.setText(requestLogs.size() + " requests");
        });

        box.getChildren().addAll(headerRow, requestTable);
        return box;
    }

    // ============================================================
    // TAB 3: LIVE LOG
    // ============================================================
    private VBox buildLogPane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.TERMINAL);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_GREEN));

        Label title = new Label("  Live Log — Sự kiện Server");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button clearBtn = new Button("  Xóa log");
        FontIcon clearIcn = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        clearIcn.setIconSize(11);
        clearIcn.setIconColor(Color.web(C_RED));
        clearBtn.setGraphic(clearIcn);
        clearBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        clearBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14 6 14; -fx-border-color: #fecaca; -fx-border-radius: 8; -fx-border-width: 1;");
        clearBtn.setOnAction(e -> logArea.clear());

        headerRow.getChildren().addAll(title, sp, clearBtn);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12.5; -fx-control-inner-background: #082337; -fx-text-fill: #5eead4; -fx-background-radius: 12; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 12; -fx-border-width: 1;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        box.getChildren().addAll(headerRow, logArea);
        return box;
    }

    // ============================================================
    // TAB 4: LIVE JSON
    // ============================================================
    private VBox buildLiveJsonPane() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.CODE);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_ACCENT));

        Label title = new Label("  Live JSON — Raw HTTP Response Body");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        liveJsonMeta = new Label("0 JSON responses");
        liveJsonMeta.setStyle("-fx-text-fill: " + C_ACCENT + "; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: #edf5f9; -fx-background-radius: 16; -fx-padding: 6 14 6 14; -fx-border-color: #b9d5e2; -fx-border-radius: 16; -fx-border-width: 1;");

        liveJsonAutoScroll = new CheckBox("Auto scroll");
        liveJsonAutoScroll.setSelected(true);
        liveJsonAutoScroll.setTextFill(Color.web(C_TEXT2));
        liveJsonAutoScroll.setStyle("-fx-font-size: 12;");

        Button clearBtn = new Button("  Xóa");
        FontIcon clearIcn = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        clearIcn.setIconSize(11);
        clearIcn.setIconColor(Color.web(C_RED));
        clearBtn.setGraphic(clearIcn);
        clearBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        clearBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14 6 14; -fx-border-color: #fecaca; -fx-border-radius: 8; -fx-border-width: 1;");
        clearBtn.setOnAction(e -> {
            if (liveJsonArea != null) liveJsonArea.clear();
            ServerStats.clearJsonTraces();
            lastLiveJsonId = 0;
        });

        headerRow.getChildren().addAll(title, sp, liveJsonMeta, liveJsonAutoScroll, clearBtn);

        Label hint = new Label("Mỗi response JSON của server được ghi lại nguyên văn tại đây — dùng để chụp ảnh báo cáo HTTP/1.1.");
        hint.setStyle("-fx-text-fill: " + C_MUTED + "; -fx-font-size: 12;");

        liveJsonArea = new TextArea();
        liveJsonArea.setEditable(false);
        liveJsonArea.setWrapText(false);
        liveJsonArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12.5; " +
            "-fx-control-inner-background: #082337; -fx-text-fill: #5eead4; " +
            "-fx-background-radius: 12; -fx-border-color: " + C_BORDER + "; " +
            "-fx-border-radius: 12; -fx-border-width: 1;");
        VBox.setVgrow(liveJsonArea, Priority.ALWAYS);

        box.getChildren().addAll(headerRow, hint, liveJsonArea);
        return box;
    }

    // ============================================================
    // TAB 5: ENDPOINTS
    // ============================================================
    private VBox buildEndpointPane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.PLUG);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_BLUE));

        Label title = new Label("  HTTP Endpoints — Danh sách API");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label countLbl = new Label("29 endpoints");
        countLbl.setStyle("-fx-text-fill: " + C_BLUE + "; -fx-font-weight: bold; -fx-font-size: 12; -fx-background-color: #dceaf1; -fx-background-radius: 16; -fx-padding: 6 14 6 14; -fx-border-color: #99f6e4; -fx-border-radius: 16; -fx-border-width: 1;");

        headerRow.getChildren().addAll(title, sp, countLbl);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox eps = new VBox(10);
        eps.setPadding(new Insets(0, 8, 0, 0));

        eps.getChildren().add(sectionHeader("🔐 AUTHENTICATION", C_SKY));
        eps.getChildren().addAll(
            endpointCard("POST", "/api/login", "Đăng nhập, trả về bearer token"),
            endpointCard("POST", "/api/logout", "Đăng xuất, hủy session token"),
            endpointCard("POST", "/api/register", "Đăng ký tài khoản mới"),
            endpointCard("POST", "/api/change-password", "Đổi mật khẩu người dùng"),
            endpointCard("GET", "/api/profile", "Lấy thông tin cá nhân")
        );

        eps.getChildren().add(sectionHeader("⏰ CHẤM CÔNG", C_GREEN));
        eps.getChildren().addAll(
            endpointCard("POST", "/api/checkin", "Chấm công vào — ghi giờ vào"),
            endpointCard("POST", "/api/checkout", "Chấm công ra — ghi giờ ra"),
            endpointCard("GET", "/api/attendance/today", "Trạng thái chấm công hôm nay"),
            endpointCard("GET", "/api/history", "Lịch sử chấm công (filter + paging)"),
            endpointCard("GET", "/api/export/csv", "Export CSV lịch sử chấm công")
        );

        eps.getChildren().add(sectionHeader("🏖️ NGHỈ PHÉP", C_ORANGE));
        eps.getChildren().addAll(
            endpointCard("POST", "/api/leave-request", "Gửi đơn xin nghỉ phép"),
            endpointCard("GET", "/api/leave-list", "Xem danh sách đơn của mình")
        );

        eps.getChildren().add(sectionHeader("🕐 CA LÀM VIỆC", C_TEAL));
        eps.getChildren().addAll(
            endpointCard("POST", "/api/work-shifts/register", "User đăng ký ca làm việc"),
            endpointCard("GET", "/api/work-shifts/list", "User xem ca của mình"),
            endpointCard("DELETE", "/api/work-shifts/{id}", "User xóa ca của mình")
        );

        eps.getChildren().add(sectionHeader("📝 GHI CHÚ CÔNG VIỆC", C_AMBER));
        eps.getChildren().addAll(
            endpointCard("POST", "/api/notes/save", "Lưu/cập nhật ghi chú công việc"),
            endpointCard("GET", "/api/notes/my", "Xem ghi chú của chính mình"),
            endpointCard("GET", "/api/notes/today", "Ghi chú hôm nay"),
            endpointCard("GET", "/api/notes/all", "Admin xem ghi chú toàn bộ NV"),
            endpointCard("DELETE", "/api/notes/{id}", "Xóa ghi chú")
        );

        eps.getChildren().add(sectionHeader("👑 ADMIN", C_RED));
        eps.getChildren().addAll(
            endpointCard("GET", "/api/admin/all", "Admin xem tất cả chấm công"),
            endpointCard("GET", "/api/admin/users", "Admin xem danh sách nhân viên"),
            endpointCard("POST", "/api/admin/user/add", "Admin thêm nhân viên"),
            endpointCard("POST", "/api/admin/user/update", "Admin sửa thông tin NV"),
            endpointCard("POST", "/api/admin/user/delete", "Admin xóa nhân viên"),
            endpointCard("GET", "/api/admin/leaves", "Admin xem toàn bộ đơn nghỉ"),
            endpointCard("POST", "/api/admin/leave/update", "Admin duyệt/từ chối đơn nghỉ"),
            endpointCard("GET", "/api/admin/work-shifts", "Admin xem TẤT CẢ ca làm việc")
        );

        eps.getChildren().add(sectionHeader("🔧 UTILITY", C_MUTED));
        eps.getChildren().addAll(
            endpointCard("GET", "/api/ping", "Kiểm tra server còn sống")
        );

        scroll.setContent(eps);
        box.getChildren().addAll(headerRow, scroll);
        return box;
    }

    private HBox sectionHeader(String title, String color) {
        HBox h = new HBox();
        h.setPadding(new Insets(16, 0, 6, 4));
        h.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(title);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(color));
        h.getChildren().add(lbl);
        return h;
    }

    private HBox endpointCard(String method, String path, String desc) {
        HBox row = new HBox(16);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 10; -fx-border-color: " + C_ACCENT + "; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15,76,117,0.18), 10, 0, 0, 3);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 6, 0, 0, 2);"));

        Label methodLbl = new Label(method);
        methodLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        methodLbl.setTextFill(Color.WHITE);
        methodLbl.setMinWidth(72);
        methodLbl.setAlignment(Pos.CENTER);

        String bgColor;
        if ("POST".equals(method)) bgColor = C_GREEN;
        else if ("GET".equals(method)) bgColor = C_BLUE;
        else if ("DELETE".equals(method)) bgColor = C_RED;
        else if ("PUT".equals(method)) bgColor = C_ORANGE;
        else if ("PATCH".equals(method)) bgColor = C_SKY;
        else bgColor = C_MUTED;

        methodLbl.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 6; -fx-padding: 5 12 5 12;");

        Label pathLbl = new Label(path);
        pathLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        pathLbl.setTextFill(Color.web(C_TEXT));
        pathLbl.setMinWidth(280);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label descLbl = new Label(desc);
        descLbl.setFont(Font.font("Segoe UI", 12));
        descLbl.setTextFill(Color.web(C_MUTED));

        row.getChildren().addAll(methodLbl, pathLbl, sp, descLbl);
        return row;
    }

    // ============================================================
    // TAB 5: HTTP TESTER
    // ============================================================
    private VBox buildTesterPane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.FLASK);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_SKY));

        Label title = new Label("  HTTP Tester — Gửi request trực tiếp");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        headerRow.getChildren().add(title);

        VBox formCard = new VBox(14);
        formCard.setPadding(new Insets(20));
        formCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label formTitle = new Label("📝 Cấu hình Request");
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        formTitle.setTextFill(Color.web(C_TEXT));

        HBox methodRow = new HBox(12);
        methodRow.setAlignment(Pos.CENTER_LEFT);

        VBox methodBox = new VBox(6);
        Label methodLbl = new Label("METHOD");
        methodLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        methodLbl.setTextFill(Color.web(C_MUTED));
        testerMethodBox = new ComboBox<>();
        testerMethodBox.getItems().addAll("GET", "POST", "DELETE", "PUT", "PATCH");
        testerMethodBox.setValue("GET");
        testerMethodBox.setPrefWidth(140);
        testerMethodBox.setPrefHeight(42);
        testerMethodBox.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 8; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8; -fx-border-width: 1.5;");
        methodBox.getChildren().addAll(methodLbl, testerMethodBox);

        VBox pathBox = new VBox(6);
        Label pathLbl = new Label("PATH");
        pathLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pathLbl.setTextFill(Color.web(C_MUTED));
        testerPathField = new TextField("/api/ping");
        testerPathField.setPrefHeight(42);
        testerPathField.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 8; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-text-fill: " + C_TEXT + "; -fx-font-family: 'Consolas';");
        HBox.setHgrow(testerPathField, Priority.ALWAYS);
        pathBox.getChildren().addAll(pathLbl, testerPathField);

        methodRow.getChildren().addAll(methodBox, pathBox);

        Label bodyLbl = new Label("BODY (JSON)");
        bodyLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        bodyLbl.setTextFill(Color.web(C_MUTED));

        testerBodyArea = new TextArea();
        testerBodyArea.setPromptText("{\n  \"key\": \"value\"\n}");
        testerBodyArea.setPrefHeight(100);
        testerBodyArea.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-control-inner-background: " + C_CARD2 + "; -fx-text-fill: " + C_TEXT + "; -fx-font-family: 'Consolas'; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1.5;");

        HBox btnRow = new HBox(10);
        Button sendBtn = new Button("  SEND REQUEST");
        FontIcon sendIcn = new FontIcon(FontAwesomeSolid.PAPER_PLANE);
        sendIcn.setIconSize(12);
        sendIcn.setIconColor(Color.WHITE);
        sendBtn.setGraphic(sendIcn);
        sendBtn.setPrefHeight(44);
        sendBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        sendBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 24 0 24; -fx-effect: dropshadow(gaussian, rgba(15,76,117,0.35), 12, 0, 0, 4);");
        sendBtn.setOnAction(e -> doSendTest());

        Button clearBtn = new Button("  Clear");
        FontIcon clearIcn = new FontIcon(FontAwesomeSolid.ERASER);
        clearIcn.setIconSize(12);
        clearIcn.setIconColor(Color.web(C_MUTED));
        clearBtn.setGraphic(clearIcn);
        clearBtn.setPrefHeight(44);
        clearBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        clearBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_TEXT2 + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 20 0 20; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8; -fx-border-width: 1;");
        clearBtn.setOnAction(e -> {
            testerBodyArea.clear();
            rawRequestArea.clear();
            rawResponseArea.clear();
        });

        btnRow.getChildren().addAll(sendBtn, clearBtn);

        formCard.getChildren().addAll(formTitle, methodRow, bodyLbl, testerBodyArea, btnRow);

        HBox resultRow = new HBox(16);
        resultRow.setMaxWidth(Double.MAX_VALUE);

        VBox reqCard = new VBox(10);
        reqCard.setPadding(new Insets(16));
        reqCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");
        HBox.setHgrow(reqCard, Priority.ALWAYS);

        Label reqTitle = new Label("📤 Raw Request");
        reqTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        reqTitle.setTextFill(Color.web(C_TEXT));

        rawRequestArea = new TextArea();
        rawRequestArea.setEditable(false);
        rawRequestArea.setPrefHeight(220);
        rawRequestArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11.5; -fx-control-inner-background: #082337; -fx-text-fill: #5eead4; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");

        reqCard.getChildren().addAll(reqTitle, rawRequestArea);

        VBox resCard = new VBox(10);
        resCard.setPadding(new Insets(16));
        resCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");
        HBox.setHgrow(resCard, Priority.ALWAYS);

        Label resTitle = new Label("📥 Raw Response");
        resTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        resTitle.setTextFill(Color.web(C_TEXT));

        rawResponseArea = new TextArea();
        rawResponseArea.setEditable(false);
        rawResponseArea.setPrefHeight(220);
        rawResponseArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11.5; -fx-control-inner-background: #082337; -fx-text-fill: #5eead4; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");

        resCard.getChildren().addAll(resTitle, rawResponseArea);

        resultRow.getChildren().addAll(reqCard, resCard);

        box.getChildren().addAll(headerRow, formCard, resultRow);
        return box;
    }

    private void doSendTest() {
        String method = testerMethodBox.getValue();
        String path = testerPathField.getText();
        String body = testerBodyArea.getText();

        rawRequestArea.setText("⏳ Đang gửi...");
        rawResponseArea.setText("⏳ Đang chờ response...");

        new Thread(() -> {
            try (Socket sock = new Socket("localhost", HttpSocketServer.PORT)) {
                sock.setSoTimeout(5000);

                StringBuilder req = new StringBuilder();
                req.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
                req.append("Host: localhost:").append(HttpSocketServer.PORT).append("\r\n");
                req.append("User-Agent: ServerDashboard/1.0\r\n");
                req.append("Accept: */*\r\n");
                if (body != null && !body.trim().isEmpty()) {
                    byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    req.append("Content-Type: application/json\r\n");
                    req.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
                    req.append("\r\n");
                    req.append(body);
                } else {
                    req.append("Content-Length: 0\r\n");
                    req.append("\r\n");
                }

                String rawReq = req.toString();

                OutputStream out = sock.getOutputStream();
                out.write(rawReq.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();

                InputStream in = sock.getInputStream();
                BufferedReader br = new BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder res = new StringBuilder();
                String line;
                long startTime = System.currentTimeMillis();
                while ((line = br.readLine()) != null) {
                    res.append(line).append("\n");
                    if (System.currentTimeMillis() - startTime > 5000) break;
                }

                Platform.runLater(() -> {
                    rawRequestArea.setText(rawReq);
                    rawResponseArea.setText(res.toString());
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    rawRequestArea.setText("❌ Lỗi kết nối: " + ex.getMessage());
                    rawResponseArea.setText("Không nhận được response.\n\nCó thể:\n" +
                        "1. Server chưa START\n" +
                        "2. Port " + HttpSocketServer.PORT + " bị chặn\n" +
                        "3. Endpoint không tồn tại");
                });
            }
        }).start();
    }

    // ============================================================
    // TAB 6: SETTINGS
    // ============================================================
    private VBox buildSettingsPane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.SLIDERS_H);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_ORANGE));

        Label title = new Label("  HTTP/1.1 Protocol Settings");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        headerRow.getChildren().add(title);

        VBox protoCard = new VBox(14);
        protoCard.setPadding(new Insets(22));
        protoCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label protoTitle = new Label("🔧 HTTP/1.1 Features");
        protoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        protoTitle.setTextFill(Color.web(C_TEXT));

        Label protoDesc = new Label("Bật/tắt các tính năng của giao thức HTTP/1.1");
        protoDesc.setFont(Font.font("Segoe UI", 12));
        protoDesc.setTextFill(Color.web(C_MUTED));

        keepAliveCheck  = settingCheck("Keep-Alive Connection", "Duy trì kết nối TCP — giảm overhead handshake", "Connection: keep-alive", C_GREEN);
        gzipCheck       = settingCheck("Gzip Compression", "Nén response body — giảm bandwidth", "Accept-Encoding: gzip", C_BLUE);
        corsCheck       = settingCheck("CORS Headers", "Cho phép cross-origin requests từ browser", "Access-Control-Allow-Origin: *", C_SKY);
        rateLimitCheck  = settingCheck("Rate Limiting", "Giới hạn 100 requests/phút/IP — chống DoS", "429 Too Many Requests", C_RED);

        protoCard.getChildren().addAll(protoTitle, protoDesc, new Separator(),
            keepAliveCheck, gzipCheck, corsCheck, rateLimitCheck);

        VBox infoCard = new VBox(14);
        infoCard.setPadding(new Insets(22));
        infoCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label infoTitle = new Label("📋 Cấu hình hiện tại");
        infoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        infoTitle.setTextFill(Color.web(C_TEXT));

        VBox infoList = new VBox(10);
        infoList.getChildren().addAll(
            settingInfoRow("Port", String.valueOf(HttpSocketServer.PORT)),
            settingInfoRow("Thread Pool", HttpSocketServer.POOL_SIZE + " threads"),
            settingInfoRow("Protocol", "HTTP/1.1 over TCP Socket"),
            settingInfoRow("Charset", "UTF-8"),
            settingInfoRow("Max Request Size", "1 MB"),
            settingInfoRow("Session Timeout", "30 phút")
        );

        infoCard.getChildren().addAll(infoTitle, new Separator(), infoList);

        HBox applyRow = new HBox(10);
        applyRow.setAlignment(Pos.CENTER_RIGHT);

        Button applyBtn = new Button("  Áp dụng cấu hình");
        FontIcon applyIcn = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        applyIcn.setIconSize(12);
        applyIcn.setIconColor(Color.WHITE);
        applyBtn.setGraphic(applyIcn);
        applyBtn.setPrefHeight(44);
        applyBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        applyBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 0 24 0 24; -fx-effect: dropshadow(gaussian, rgba(15,76,117,0.35), 12, 0, 0, 4);");
        applyBtn.setOnAction(e -> {
            log("⚙️ Cấu hình HTTP/1.1 đã được cập nhật:");
            log("   • Keep-Alive: " + (keepAliveCheck.isSelected() ? "ON" : "OFF"));
            log("   • Gzip: " + (gzipCheck.isSelected() ? "ON" : "OFF"));
            log("   • CORS: " + (corsCheck.isSelected() ? "ON" : "OFF"));
            log("   • Rate Limit: " + (rateLimitCheck.isSelected() ? "ON" : "OFF"));
        });

        applyRow.getChildren().add(applyBtn);

        box.getChildren().addAll(headerRow, protoCard, infoCard, applyRow);
        return box;
    }

    private CheckBox settingCheck(String title, String desc, String header, String color) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");

        CheckBox cb = new CheckBox(title);
        cb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        cb.setStyle("-fx-text-fill: " + C_TEXT + ";");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.setPadding(new Insets(2, 0, 0, 24));

        Label d = new Label(desc);
        d.setFont(Font.font("Segoe UI", 11.5));
        d.setTextFill(Color.web(C_MUTED));

        Label h = new Label(header);
        h.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        h.setTextFill(Color.web(color));
        h.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 6; -fx-padding: 2 8 2 8;");

        metaRow.getChildren().addAll(d, h);
        box.getChildren().addAll(cb, metaRow);

        VBox wrapper = new VBox();
        wrapper.getChildren().add(box);
        wrapper.setUserData(cb);

        cb.setUserData(box);
        return cb;
    }

    private HBox settingInfoRow(String key, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label k = new Label(key);
        k.setFont(Font.font("Segoe UI", 12.5));
        k.setTextFill(Color.web(C_MUTED));
        k.setMinWidth(180);

        Label v = new Label(value);
        v.setFont(Font.font("Consolas", FontWeight.BOLD, 12.5));
        v.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        row.getChildren().addAll(k, sp, v);
        return row;
    }

    // ============================================================
    // TAB 7: CLIENTS
    // ============================================================
    private VBox buildClientTablePane() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.USERS);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(C_ACCENT));

        Label title = new Label("  Danh sách Clients đã kết nối");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label hint = new Label("  Tự động cập nhật mỗi 1 giây");
        FontIcon hintIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        hintIcn.setIconSize(11);
        hintIcn.setIconColor(Color.web(C_MUTED));
        hint.setGraphic(hintIcn);
        hint.setFont(Font.font("Segoe UI", 11));
        hint.setTextFill(Color.web(C_MUTED));

        headerRow.getChildren().addAll(title, sp, hint);

        clientTable = new TableView<>();
        clientList = FXCollections.observableArrayList();
        clientTable.setItems(clientList);
        clientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        clientTable.setPrefHeight(600);
        VBox.setVgrow(clientTable, Priority.ALWAYS);

        TableColumn<ClientManager.ClientInfo, String> colUser = new TableColumn<>("👤 Username");
        colUser.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        colUser.setPrefWidth(150);

        TableColumn<ClientManager.ClientInfo, String> colIP = new TableColumn<>("🌐 IP Address");
        colIP.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getIp()));
        colIP.setPrefWidth(160);

        TableColumn<ClientManager.ClientInfo, String> colPort = new TableColumn<>("🔌 Port");
        colPort.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getPort())));
        colPort.setPrefWidth(100);

        TableColumn<ClientManager.ClientInfo, String> colLogin = new TableColumn<>("⏰ Login Time");
        colLogin.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getLoginTimeStr()));
        colLogin.setPrefWidth(140);

        TableColumn<ClientManager.ClientInfo, String> colLast = new TableColumn<>("⚡ Last Activity");
        colLast.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getLastActivityStr()));
        colLast.setPrefWidth(150);

        TableColumn<ClientManager.ClientInfo, String> colReq = new TableColumn<>("📨 Requests");
        colReq.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getRequestCount())));
        colReq.setPrefWidth(110);

        TableColumn<ClientManager.ClientInfo, String> colStatus = new TableColumn<>("📊 Status");
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        colStatus.setPrefWidth(120);
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    if ("ONLINE".equals(item)) {
                        setText("● ONLINE");
                        setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;");
                    } else {
                        setText("○ OFFLINE");
                        setStyle("-fx-text-fill: " + C_MUTED + ";");
                    }
                }
            }
        });

        clientTable.getColumns().addAll(colUser, colIP, colPort, colLogin, colLast, colReq, colStatus);

        Button refreshBtn = new Button("  Làm mới");
        FontIcon rIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        rIcn.setIconSize(12);
        rIcn.setIconColor(Color.web(C_ACCENT));
        refreshBtn.setGraphic(rIcn);
        refreshBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        refreshBtn.setStyle("-fx-background-color: #edf5f9; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20 10 20; -fx-border-color: #b9d5e2; -fx-border-radius: 8; -fx-border-width: 1;");
        refreshBtn.setOnAction(e -> refreshClientTable());

        box.getChildren().addAll(headerRow, clientTable, refreshBtn);

        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                Platform.runLater(this::refreshClientTable);
            }
        });
        t.setDaemon(true);
        t.start();

        return box;
    }

    private void refreshClientTable() {
        if (clientList == null) return;

        Set<String> currentOnline = new HashSet<>();
        for (ClientManager.ClientInfo c : ClientManager.getAll().values()) {
            if ("ONLINE".equals(c.getStatus())) {
                currentOnline.add(c.getUsername());
            }
        }

        for (String user : previousOnline) {
            if (!currentOnline.contains(user)) {
                log("🔌 Client OFFLINE: " + user);
            }
        }

        for (String user : currentOnline) {
            if (!previousOnline.contains(user)) {
                log("✅ Client ONLINE: " + user);
            }
        }

        previousOnline = currentOnline;
        clientList.setAll(ClientManager.getAll().values());
    }

    private HBox statRow(FontAwesomeSolid icon, String title, Label valueLbl, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(36, 36);
        iconBox.setMaxSize(36, 36);
        iconBox.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 18;");

        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(16);
        icn.setIconColor(Color.web(color));
        iconBox.getChildren().add(icn);

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", 12.5));
        titleLbl.setTextFill(Color.web(C_MUTED));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        valueLbl.setTextFill(Color.web(color));

        row.getChildren().addAll(iconBox, titleLbl, sp, valueLbl);
        return row;
    }

    private void updateServerIp() {
        try {
            String bestIp = null;
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface iface = ifaces.nextElement();
                String name = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();

                if (iface.isLoopback() || iface.isVirtual()) continue;
                if (name.contains("vmware") || name.contains("virtual")
                    || name.contains("vbox") || name.contains("vethernet")
                    || displayName.contains("virtualbox")) continue;

                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            bestIp = ip;
                        } else if (bestIp == null) {
                            bestIp = ip;
                        }
                    }
                }
            }
            if (bestIp != null) {
                serverIpLabel.setText("🌐  " + bestIp + ":" + HttpSocketServer.PORT);
            } else {
                serverIpLabel.setText("🌐  localhost:" + HttpSocketServer.PORT);
            }
        } catch (Exception e) {
            serverIpLabel.setText("🌐  localhost:" + HttpSocketServer.PORT);
        }
    }

    private void startServer() {
        new Thread(() -> {
            try {
                server.start();
                Platform.runLater(() -> {
                    statusBadge.setText("  RUNNING");
                    FontIcon runIcon = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
                    runIcon.setIconSize(13);
                    runIcon.setIconColor(Color.web(C_GREEN));
                    statusBadge.setGraphic(runIcon);
                    statusBadge.setTextFill(Color.web(C_GREEN));
                    statusBadge.setStyle("-fx-background-color: #d1fae5; -fx-background-radius: 20; -fx-padding: 8 18 8 18; -fx-border-color: #6ee7b7; -fx-border-radius: 20; -fx-border-width: 1;");

                    startBtn.setDisable(true);
                    startBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_MUTED + "; -fx-background-radius: 10; -fx-cursor: not-allowed; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");

                    stopBtn.setDisable(false);
                    stopBtn.setStyle("-fx-background-color: " + C_RED + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(185,28,28,0.35), 12, 0, 0, 4);");
                });
            } catch (Exception e) {
                Platform.runLater(() -> log("❌ Lỗi start: " + e.getMessage()));
            }
        }).start();
    }

    private void stopServer() {
        server.stop();
        statusBadge.setText("  STOPPED");
        FontIcon stopIcon = new FontIcon(FontAwesomeSolid.STOP_CIRCLE);
        stopIcon.setIconSize(13);
        stopIcon.setIconColor(Color.web(C_RED));
        statusBadge.setGraphic(stopIcon);
        statusBadge.setTextFill(Color.web(C_RED));
        statusBadge.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 20; -fx-padding: 8 18 8 18; -fx-border-color: #fecaca; -fx-border-radius: 20; -fx-border-width: 1;");

        startBtn.setDisable(false);
        startBtn.setStyle("-fx-background-color: " + C_GREEN + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(22,163,74,0.4), 12, 0, 0, 4);");

        stopBtn.setDisable(true);
        stopBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_MUTED + "; -fx-background-radius: 10; -fx-cursor: not-allowed; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1;");
    }

    private void startStatsTimer() {
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                Platform.runLater(() -> {
                    activeClientsVal.setText(String.valueOf(ClientManager.getOnlineCount()));
                    totalClientsVal.setText(String.valueOf(ServerStats.getTotalClients()));
                    totalRequestsVal.setText(String.valueOf(ServerStats.getTotalRequests()));
                    uptimeVal.setText(ServerStats.getUptime());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void startChartUpdateTimer(ProgressBar threadBar, Label threadLbl) {
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
                Platform.runLater(() -> {
                    if (requestSeries != null && requestChart != null) {
                        java.time.LocalTime now = java.time.LocalTime.now();
                        String label = String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
                        int newVal = ServerStats.getRequestsInLastNSeconds(3);

                        requestSeries.getData().add(new XYChart.Data<>(label, newVal));
                        if (requestSeries.getData().size() > 20) {
                            requestSeries.getData().remove(0);
                        }
                    }

                    if (statusPieChart != null) {
                        statusPieChart.getData().get(0).setPieValue(ServerStats.getStatusCount(0));
                        statusPieChart.getData().get(1).setPieValue(ServerStats.getStatusCount(1));
                        statusPieChart.getData().get(2).setPieValue(ServerStats.getStatusCount(2));
                        statusPieChart.getData().get(3).setPieValue(ServerStats.getStatusCount(3));
                        statusPieChart.getData().get(4).setPieValue(ServerStats.getStatusCount(4));
                        statusPieChart.getData().get(5).setPieValue(ServerStats.getStatusCount(5));
                    }

                    if (reqPerSecVal != null) {
                        reqPerSecVal.setText(String.valueOf(ServerStats.getRequestsInLastNSeconds(1)));
                    }
                    if (clientsOnlineVal != null) {
                        clientsOnlineVal.setText(String.valueOf(ClientManager.getOnlineCount()));
                    }
                    if (dataTransferVal != null) {
                        long bytes = ServerStats.getTotalBytes();
                        double kb = bytes / 1024.0;
                        dataTransferVal.setText(String.format("%.1f", kb));
                    }
                    if (avgLatencyVal != null) {
                        double avg = ServerStats.getAvgLatency();
                        avgLatencyVal.setText(String.format("%.0f", avg));
                    }

                    if (methodGetVal != null) methodGetVal.setText(String.valueOf(ServerStats.getMethodCount("GET")));
                    if (methodPostVal != null) methodPostVal.setText(String.valueOf(ServerStats.getMethodCount("POST")));
                    if (methodDeleteVal != null) methodDeleteVal.setText(String.valueOf(ServerStats.getMethodCount("DELETE")));
                    if (methodPutVal != null) methodPutVal.setText(String.valueOf(ServerStats.getMethodCount("PUT")));
                    if (methodPatchVal != null) methodPatchVal.setText(String.valueOf(ServerStats.getMethodCount("PATCH")));

                    if (requestLogs != null) {
                        java.util.List<ServerStats.RequestInfo> recent = ServerStats.getRecentRequests();
                        if (recent.size() != requestLogs.size()
                            || (recent.size() > 0 && requestLogs.size() > 0
                                && !recent.get(0).time.equals(requestLogs.get(0).time))) {
                            requestLogs.clear();
                            for (ServerStats.RequestInfo r : recent) {
                                requestLogs.add(new RequestLog(
                                    r.time, r.method, r.path,
                                    String.valueOf(r.status),
                                    r.duration + "ms",
                                    String.valueOf(r.bytes),
                                    r.clientIp
                                ));
                            }
                        }
                    }

                    // Live JSON: nối các response JSON mới vào tab.
                    if (liveJsonArea != null) {
                        java.util.List<ServerStats.JsonTrace> traces = ServerStats.getJsonTraces();
                        StringBuilder add = new StringBuilder();
                        long newest = lastLiveJsonId;
                        for (int i = traces.size() - 1; i >= 0; i--) {
                            ServerStats.JsonTrace tr = traces.get(i);
                            if (tr.id <= lastLiveJsonId) continue;
                            add.append("┌─ ").append(tr.time).append("  ")
                               .append(tr.method).append(" ").append(tr.path)
                               .append("  ->  HTTP ").append(tr.status).append("\n")
                               .append(tr.body).append("\n")
                               .append("└────────────────────────────────────────────\n\n");
                            newest = Math.max(newest, tr.id);
                        }
                        if (add.length() > 0) {
                            liveJsonArea.appendText(add.toString());
                            lastLiveJsonId = newest;
                            if (liveJsonAutoScroll == null || liveJsonAutoScroll.isSelected()) {
                                liveJsonArea.setScrollTop(Double.MAX_VALUE);
                            }
                        }
                        if (liveJsonMeta != null) {
                            liveJsonMeta.setText(traces.size() + " JSON responses");
                        }
                    }

                    if (threadBar != null) {
                        int active = ClientManager.getOnlineCount();
                        double pct = Math.min(1.0, active / (double) HttpSocketServer.POOL_SIZE);
                        threadBar.setProgress(pct);
                        threadLbl.setText(active + " / " + HttpSocketServer.POOL_SIZE + " threads");
                    }
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void log(String msg) {
        if (logArea != null) {
            logArea.appendText(msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        }
        System.out.println(msg);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
