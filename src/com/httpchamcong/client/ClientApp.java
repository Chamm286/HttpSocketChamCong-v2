package com.httpchamcong.client;

import com.httpchamcong.util.JsonUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.List;

public class ClientApp extends Application {

    private final SocketHttpClient http = new SocketHttpClient();
    private Stage primaryStage;
    private String fullName = "";
    private String role = "";
    private String username = "";

    private VBox contentArea;
    private Label pageTitle;
    private Label pageSubtitle;
    private Label clockLabel;
    private Button activeBtn;
    private Button attendanceBtn;
    private Timeline attendanceTicker;

    private TextField userField;
    private PasswordField passField;
    private Label loginError;

    private int currentPage = 1;
    private int pageLimit = 10;

    // ===== PALETTE — CYAN/TEAL (Không tím) =====
    private static final String C_BG       = "#f0f9ff";   // Nền xanh rất nhạt
    private static final String C_CARD     = "#ffffff";
    private static final String C_CARD2    = "#f8fafc";
    private static final String C_BORDER   = "#cbd5e1";   // Viền đậm hơn
    private static final String C_TEXT     = "#0c1220";   // Đen đậm rõ
    private static final String C_TEXT2    = "#1e293b";   // Đậm hơn
    private static final String C_MUTED    = "#475569";   // Rõ hơn
    private static final String C_INPUT_BG = "#ffffff";
    private static final String C_PROMPT   = "#94a3b8";
    private static final String C_ACCENT   = "#0891b2";   // Cyan-600
    private static final String C_ACCENT2  = "#0e7490";   // Cyan-700
    private static final String C_GREEN    = "#059669";   // Emerald-600
    private static final String C_ORANGE   = "#ea580c";   // Orange-600
    private static final String C_BLUE     = "#2563eb";   // Blue-600
    private static final String C_VIOLET   = "#0284c7";   // Sky-600 (khong dung tim)
    private static final String C_RED      = "#dc2626";   // Red-600
    private static final String C_TEAL     = "#0d9488";   // Teal-600
    private static final String C_AMBER    = "#d97706";   // Amber-600

    private static final String INPUT_STYLE =
        "-fx-background-color: " + C_INPUT_BG + "; " +
        "-fx-control-inner-background: " + C_INPUT_BG + "; " +
        "-fx-text-fill: " + C_TEXT + "; " +
        "-fx-prompt-text-fill: " + C_PROMPT + "; " +
        "-fx-highlight-fill: " + C_ACCENT + "; " +
        "-fx-highlight-text-fill: white; " +
        "-fx-background-radius: 10; " +
        "-fx-border-color: " + C_BORDER + "; " +
        "-fx-border-radius: 10; " +
        "-fx-border-width: 1.5; " +
        "-fx-padding: 10 14 10 14; " +
        "-fx-font-size: 13;";

    private static final String DATE_PICKER_STYLE =
        "-fx-background-color: " + C_INPUT_BG + "; " +
        "-fx-control-inner-background: " + C_INPUT_BG + "; " +
        "-fx-text-fill: " + C_TEXT + "; " +
        "-fx-prompt-text-fill: " + C_PROMPT + "; " +
        "-fx-background-radius: 8; " +
        "-fx-border-color: " + C_BORDER + "; " +
        "-fx-border-radius: 8; " +
        "-fx-border-width: 1.5; " +
        "-fx-font-size: 13; " +
        "-fx-pref-height: 44;";

    private static final String COMBO_BOX_STYLE =
        "-fx-background-color: " + C_INPUT_BG + "; " +
        "-fx-control-inner-background: " + C_INPUT_BG + "; " +
        "-fx-text-fill: " + C_TEXT + "; " +
        "-fx-background-radius: 8; " +
        "-fx-border-color: " + C_BORDER + "; " +
        "-fx-border-radius: 8; " +
        "-fx-border-width: 1.5; " +
        "-fx-font-size: 13; " +
        "-fx-pref-height: 44;";

    private static final String TEXT_AREA_STYLE =
        "-fx-background-color: " + C_INPUT_BG + "; " +
        "-fx-control-inner-background: " + C_INPUT_BG + "; " +
        "-fx-text-fill: " + C_TEXT + "; " +
        "-fx-prompt-text-fill: " + C_PROMPT + "; " +
        "-fx-highlight-fill: " + C_ACCENT + "; " +
        "-fx-highlight-text-fill: white; " +
        "-fx-background-radius: 10; " +
        "-fx-border-color: " + C_BORDER + "; " +
        "-fx-border-radius: 10; " +
        "-fx-border-width: 1.5; " +
        "-fx-font-size: 13;";

    private String getGlobalCss() {
        return
            ".root { -fx-font-family: 'Segoe UI'; -fx-background-color: " + C_BG + "; }" +
            ".label { -fx-text-fill: " + C_TEXT + "; }" +
            ".combo-box .list-cell { -fx-text-fill: " + C_TEXT + "; -fx-background-color: " + C_INPUT_BG + "; -fx-padding: 8 12 8 12; }" +
            ".combo-box-popup .list-view { -fx-background-color: " + C_INPUT_BG + "; -fx-border-color: " + C_BORDER + "; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); }" +
            ".combo-box-popup .list-view .list-cell:filled:hover { -fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_ACCENT + "; }" +
            ".combo-box-popup .list-view .list-cell:filled:selected { -fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; }" +
            ".combo-box .arrow { -fx-background-color: " + C_TEXT + "; }" +
            ".combo-box .arrow-button { -fx-background-color: transparent; }" +
            ".date-picker .text-field { -fx-background-color: transparent; -fx-text-fill: " + C_TEXT + "; }" +
            ".date-picker .arrow-button { -fx-background-color: transparent; -fx-background-radius: 0 8 8 0; }" +
            ".date-picker .arrow-button .arrow { -fx-background-color: " + C_TEXT + "; }" +
            ".date-picker-popup { -fx-background-color: " + C_INPUT_BG + "; -fx-border-color: " + C_BORDER + "; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); }" +
            ".date-picker-popup > * > .month-year-pane { -fx-background-color: " + C_CARD2 + "; }" +
            ".date-picker-popup > * > .month-year-pane .label { -fx-text-fill: " + C_TEXT + "; -fx-font-weight: bold; }" +
            ".date-picker-popup > * > .month-year-pane .spinner .button { -fx-background-color: transparent; }" +
            ".date-picker-popup > * > .month-year-pane .spinner .button .left-arrow, " +
            ".date-picker-popup > * > .month-year-pane .spinner .button .right-arrow { -fx-background-color: " + C_TEXT + "; }" +
            ".date-picker-popup > * > .calendar-grid { -fx-background-color: " + C_INPUT_BG + "; }" +
            ".date-picker-popup .day-cell { -fx-background-color: " + C_INPUT_BG + "; -fx-text-fill: " + C_TEXT + "; -fx-border-color: transparent; }" +
            ".date-picker-popup .day-cell:hover { -fx-background-color: " + C_CARD2 + "; }" +
            ".date-picker-popup .day-cell.selected { -fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; }" +
            ".date-picker-popup .day-name-cell { -fx-text-fill: " + C_MUTED + "; -fx-background-color: " + C_INPUT_BG + "; }" +
            ".date-picker-popup .week-number-cell { -fx-text-fill: " + C_MUTED + "; }" +
            ".text-area { -fx-text-fill: " + C_TEXT + "; -fx-background-color: " + C_INPUT_BG + "; }" +
            ".text-area .content { -fx-background-color: " + C_INPUT_BG + "; }" +
            ".text-area .scroll-pane { -fx-background-color: " + C_INPUT_BG + "; }" +
            ".text-area .scroll-pane .viewport { -fx-background-color: " + C_INPUT_BG + "; }" +
            ".text-area .scroll-pane .content { -fx-background-color: " + C_INPUT_BG + "; }" +
            ".text-field { -fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_PROMPT + "; }" +
            ".password-field { -fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_PROMPT + "; }" +
            ".table-view { -fx-background-color: " + C_INPUT_BG + "; -fx-control-inner-background: " + C_INPUT_BG + "; -fx-table-cell-border-color: " + C_BORDER + "; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: " + C_BORDER + "; -fx-border-width: 1; }" +
            ".table-view .column-header-background { -fx-background-color: " + C_CARD2 + "; -fx-background-radius: 12 12 0 0; }" +
            ".table-view .column-header, .table-view .filler { -fx-background-color: transparent; -fx-border-color: transparent " + C_BORDER + " " + C_BORDER + " transparent; -fx-size: 44; }" +
            ".table-view .column-header .label { -fx-text-fill: " + C_TEXT + "; -fx-font-weight: bold; -fx-alignment: center-left; -fx-padding: 0 12 0 12; -fx-font-size: 12.5; }" +
            ".table-view .table-cell { -fx-text-fill: " + C_TEXT + "; -fx-border-color: transparent " + C_BORDER + " transparent transparent; -fx-padding: 10 12 10 12; -fx-alignment: center-left; -fx-font-size: 12.5; }" +
            ".table-view .table-row-cell { -fx-background-color: " + C_INPUT_BG + "; -fx-cell-size: 46; -fx-border-color: transparent transparent " + C_BORDER + " transparent; -fx-border-width: 0 0 1 0; }" +
            ".table-view .table-row-cell:odd { -fx-background-color: " + C_BG + "; }" +
            ".table-view .table-row-cell:hover { -fx-background-color: #e0f2fe; }" +
            ".table-view .table-row-cell:selected { -fx-background-color: " + C_ACCENT + "22; }" +
            ".table-view .table-row-cell:selected .table-cell { -fx-text-fill: " + C_TEXT + "; }" +
            ".scroll-pane { -fx-background-color: transparent; }" +
            ".scroll-pane > .viewport { -fx-background-color: transparent; }" +
            ".scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; }" +
            ".scroll-bar .track { -fx-background-color: transparent; -fx-border-color: transparent; }" +
            ".scroll-bar .thumb { -fx-background-color: " + C_BORDER + "; -fx-background-radius: 8; }" +
            ".scroll-bar .thumb:hover { -fx-background-color: " + C_MUTED + "; }" +
            ".scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-background-color: transparent; -fx-padding: 0; }" +
            ".scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-background-color: transparent; -fx-padding: 0; -fx-shape: null; }" +
            ".dialog-pane { -fx-background-color: " + C_INPUT_BG + "; }" +
            ".dialog-pane .label { -fx-text-fill: " + C_TEXT + "; }" +
            ".dialog-pane .header-panel { -fx-background-color: " + C_CARD2 + "; }" +
            ".dialog-pane .header-panel .label { -fx-text-fill: " + C_TEXT + "; -fx-font-weight: bold; }" +
            ".dialog-pane .content.label { -fx-text-fill: " + C_TEXT2 + "; }" +
            ".dialog-pane .button-bar .button { -fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_TEXT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 20 8 20; }" +
            ".dialog-pane .button-bar .button:hover { -fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; }" +
            ".check-box .box { -fx-background-color: " + C_INPUT_BG + "; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 4; }" +
            ".check-box:selected .mark { -fx-background-color: white; }" +
            ".check-box:selected .box { -fx-background-color: " + C_ACCENT + "; }";
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Hệ thống Chấm công");
        showLogin();
        stage.show();

        stage.setOnCloseRequest(event -> {
            try {
                if (http.getToken() != null) {
                    http.post("/api/logout", "{}");
                }
            } catch (Exception ex) {
                System.out.println("Lỗi logout: " + ex.getMessage());
            }
        });
    }

    private void log(String msg) {
        System.out.println("[ClientApp] " + msg);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    private void showLogin() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #ecfeff, #f0f9ff, #f0fdfa);");

        VBox card = new VBox(0);
        card.setMaxWidth(440);
        card.setMaxHeight(720);
        card.setStyle(
            "-fx-background-color: " + C_CARD + "; " +
            "-fx-background-radius: 20; " +
            "-fx-border-color: " + C_BORDER + "; " +
            "-fx-border-radius: 20; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(8,145,178,0.18), 50, 0, 0, 20);"
        );

        VBox header = new VBox(12);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(40, 40, 20, 40));

        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(80, 80);
        logoCircle.setMaxSize(80, 80);
        logoCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_TEAL + "); -fx-background-radius: 40; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.5), 16, 0, 0, 6);");

        FontIcon logoIcon = new FontIcon(FontAwesomeSolid.CLOCK);
        logoIcon.setIconSize(36);
        logoIcon.setIconColor(Color.WHITE);
        logoCircle.getChildren().add(logoIcon);

        Label title = new Label("Chào mừng trở lại!");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 25));
        title.setTextFill(Color.web(C_TEXT));
        title.setPadding(new Insets(12, 0, 0, 0));

        Label subtitle = new Label("Đăng nhập để bắt đầu chấm công");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web(C_MUTED));

        HBox serverBox = new HBox(8);
        serverBox.setAlignment(Pos.CENTER);
        serverBox.setPadding(new Insets(8, 16, 8, 16));
        serverBox.setStyle("-fx-background-color: #ecfeff; -fx-background-radius: 16; -fx-border-color: #a5f3fc; -fx-border-radius: 16; -fx-border-width: 1;");

        FontIcon serverIcn = new FontIcon(FontAwesomeSolid.SERVER);
        serverIcn.setIconSize(12);
        serverIcn.setIconColor(Color.web(C_ACCENT));

        Label serverInfo = new Label("Server: " + SocketHttpClient.getServerInfo());
        serverInfo.setFont(Font.font("Consolas", FontWeight.BOLD, 11.5));
        serverInfo.setTextFill(Color.web(C_ACCENT));

        serverBox.getChildren().addAll(serverIcn, serverInfo);
        header.getChildren().addAll(logoCircle, title, subtitle, serverBox);

        VBox form = new VBox(14);
        form.setPadding(new Insets(10, 45, 20, 45));

        Label userLabel = new Label("TÀI KHOẢN");
        userLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        userLabel.setTextFill(Color.web(C_MUTED));

        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.setPadding(new Insets(0, 16, 0, 16));
        userBox.setPrefHeight(46);
        userBox.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1.5;");

        FontIcon userIcon = new FontIcon(FontAwesomeSolid.USER);
        userIcon.setIconSize(14);
        userIcon.setIconColor(Color.web(C_MUTED));

        userField = new TextField("pvkhai");
        userField.setFont(Font.font("Segoe UI", 14));
        userField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_PROMPT + "; -fx-highlight-fill: " + C_ACCENT + "; -fx-highlight-text-fill: white;");
        HBox.setHgrow(userField, Priority.ALWAYS);
        userBox.getChildren().addAll(userIcon, userField);

        Label passLabel = new Label("MẬT KHẨU");
        passLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        passLabel.setTextFill(Color.web(C_MUTED));

        HBox passBox = new HBox(10);
        passBox.setAlignment(Pos.CENTER_LEFT);
        passBox.setPadding(new Insets(0, 16, 0, 16));
        passBox.setPrefHeight(46);
        passBox.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 10; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 10; -fx-border-width: 1.5;");

        FontIcon passIcon = new FontIcon(FontAwesomeSolid.LOCK);
        passIcon.setIconSize(14);
        passIcon.setIconColor(Color.web(C_MUTED));

        passField = new PasswordField();
        passField.setText("Nhanvien@2025");
        passField.setFont(Font.font("Segoe UI", 14));
        passField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_PROMPT + "; -fx-highlight-fill: " + C_ACCENT + "; -fx-highlight-text-fill: white;");
        HBox.setHgrow(passField, Priority.ALWAYS);

        TextField passVisible = new TextField(passField.getText());
        passVisible.setFont(Font.font("Segoe UI", 14));
        passVisible.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_TEXT + "; -fx-prompt-text-fill: " + C_PROMPT + "; -fx-highlight-fill: " + C_ACCENT + "; -fx-highlight-text-fill: white;");
        passVisible.setManaged(false);
        passVisible.setVisible(false);
        HBox.setHgrow(passVisible, Priority.ALWAYS);
        passField.textProperty().bindBidirectional(passVisible.textProperty());

        Button eyeBtn = new Button();
        FontIcon eyeIcon = new FontIcon(FontAwesomeSolid.EYE);
        eyeIcon.setIconSize(14);
        eyeIcon.setIconColor(Color.web(C_MUTED));
        eyeBtn.setGraphic(eyeIcon);
        eyeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        eyeBtn.setOnAction(e -> {
            boolean show = !passVisible.isVisible();
            passVisible.setVisible(show);
            passVisible.setManaged(show);
            passField.setVisible(!show);
            passField.setManaged(!show);
            eyeIcon.setIconCode(show ? FontAwesomeSolid.EYE_SLASH : FontAwesomeSolid.EYE);
        });

        passBox.getChildren().addAll(passIcon, passField, passVisible, eyeBtn);

        Button loginBtn = new Button("ĐĂNG NHẬP");
        loginBtn.setPrefHeight(48);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        loginBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 12, 0, 0, 4);");
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("-fx-background-color: " + C_ACCENT2 + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.55), 16, 0, 0, 6);"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 12, 0, 0, 4);"));
        loginBtn.setOnAction(e -> doLogin());

        Button registerBtn = new Button("CHƯA CÓ TÀI KHOẢN? ĐĂNG KÝ");
        registerBtn.setPrefHeight(38);
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        registerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_ACCENT + "; -fx-cursor: hand; -fx-border-color: " + C_ACCENT + "; -fx-border-radius: 8; -fx-border-width: 1;");
        registerBtn.setOnAction(e -> showRegisterDialog());

        loginError = new Label("");
        loginError.setTextFill(Color.web(C_RED));
        loginError.setFont(Font.font("Segoe UI", 12));
        loginError.setWrapText(true);

        form.getChildren().addAll(userLabel, userBox, passLabel, passBox, loginBtn, registerBtn, loginError);

        VBox footer = new VBox(6);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15, 40, 20, 40));
        footer.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-background-radius: 0 0 20 20; -fx-border-color: " + C_BORDER + "; -fx-border-width: 1 0 0 0;");

        Label h1 = new Label("TÀI KHOẢN DEMO");
        h1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        h1.setTextFill(Color.web(C_MUTED));

        Label h2 = new Label("👤 pvkhai / Nhanvien@2025");
        h2.setFont(Font.font("Consolas", 11));
        h2.setTextFill(Color.web(C_MUTED));

        Label h3 = new Label("👑 admin  / Admin@2025");
        h3.setFont(Font.font("Consolas", 11));
        h3.setTextFill(Color.web(C_MUTED));

        footer.getChildren().addAll(h1, h2, h3);
        card.getChildren().addAll(header, form, footer);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 1000, 780);
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));
        primaryStage.setScene(scene);
    }

        // ============================================================
    // ĐĂNG KÝ DIALOG
    // ============================================================
    private void showRegisterDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Đăng ký tài khoản");

        VBox root = new VBox(16);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + C_CARD + ";");

        Label title = new Label("ĐĂNG KÝ TÀI KHOẢN MỚI");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        TextField regUser = new TextField();
        regUser.setPromptText("Tên đăng nhập");
        regUser.setPrefHeight(44);
        regUser.setStyle(INPUT_STYLE);

        PasswordField regPass = new PasswordField();
        regPass.setPromptText("Mật khẩu");
        regPass.setPrefHeight(44);
        regPass.setStyle(INPUT_STYLE);

        TextField regName = new TextField();
        regName.setPromptText("Họ và tên");
        regName.setPrefHeight(44);
        regName.setStyle(INPUT_STYLE);

        Label errLbl = new Label("");
        errLbl.setTextFill(Color.web(C_RED));
        errLbl.setWrapText(true);

        Button submit = new Button("ĐĂNG KÝ");
        submit.setPrefHeight(46);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        submit.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

        submit.setOnAction(e -> {
            String u = regUser.getText().trim();
            String p = regPass.getText().trim();
            String n = regName.getText().trim();

            if (u.isEmpty() || p.isEmpty() || n.isEmpty()) {
                errLbl.setTextFill(Color.web(C_RED));
                errLbl.setText("Vui lòng nhập đầy đủ");
                return;
            }

            new Thread(() -> {
                try {
                    String body = "{\"username\":\"" + u + "\",\"password\":\"" + p + "\",\"fullName\":\"" + n + "\"}";
                    String res = http.post("/api/register", body);
                    String msg = JsonUtil.getString(res, "msg");
                    String err = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> {
                        if (msg != null) {
                            errLbl.setTextFill(Color.web(C_GREEN));
                            errLbl.setText("✅ " + msg + " — Hãy đăng nhập!");
                            new Thread(() -> {
                                try { Thread.sleep(1500); } catch (Exception ex) {}
                                Platform.runLater(dialog::close);
                            }).start();
                        } else {
                            errLbl.setTextFill(Color.web(C_RED));
                            errLbl.setText("❌ " + (err != null ? err : res));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> errLbl.setText("❌ Lỗi: " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(title, regUser, regPass, regName, submit, errLbl);

        Scene scene = new Scene(root, 400, 420);
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));
        dialog.setScene(scene);
        dialog.show();
    }

    // ============================================================
    // DASHBOARD
    // ============================================================
    private void showMain(String name, String r) {
        this.fullName = name;
        this.role = r;
        this.username = userField.getText();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + C_BG + ";");

        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER + "; -fx-border-width: 0 1 0 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 4, 0);");

        HBox logoBox = new HBox(12);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(26, 22, 26, 22));

        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(42, 42);
        logoCircle.setMaxSize(42, 42);
        logoCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_TEAL + "); -fx-background-radius: 21; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.45), 8, 0, 0, 3);");
        FontIcon logoIcon = new FontIcon(FontAwesomeSolid.CLOCK);
        logoIcon.setIconSize(20);
        logoIcon.setIconColor(Color.WHITE);
        logoCircle.getChildren().add(logoIcon);

        VBox logoText = new VBox(1);
        Label lt1 = new Label("CHẤM CÔNG");
        lt1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lt1.setTextFill(Color.web(C_TEXT));
        Label lt2 = new Label("Online System");
        lt2.setFont(Font.font("Segoe UI", 10));
        lt2.setTextFill(Color.web(C_MUTED));
        logoText.getChildren().addAll(lt1, lt2);
        logoBox.getChildren().addAll(logoCircle, logoText);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: " + C_BORDER + ";");

        ScrollPane menuScroll = new ScrollPane();
        menuScroll.setFitToWidth(true);
        menuScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(menuScroll, Priority.ALWAYS);

        VBox menu = new VBox(4);
        menu.setPadding(new Insets(16, 12, 16, 12));

        Label menuTitle = new Label("MENU CHÍNH");
        menuTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        menuTitle.setTextFill(Color.web(C_MUTED));
        menuTitle.setPadding(new Insets(0, 0, 10, 14));

        Button homeBtn = sbBtn(FontAwesomeSolid.HOME, "Trang chủ");
        attendanceBtn = sbBtn(FontAwesomeSolid.CLOCK, "Chấm công");
        Button histBtn = sbBtn(FontAwesomeSolid.HISTORY, "Lịch sử");
        Button statBtn = sbBtn(FontAwesomeSolid.CHART_BAR, "Thống kê");

        Label accountTitle = new Label("TÀI KHOẢN");
        accountTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        accountTitle.setTextFill(Color.web(C_MUTED));
        accountTitle.setPadding(new Insets(12, 0, 10, 14));

        Button passBtn = sbBtn(FontAwesomeSolid.KEY, "Đổi mật khẩu");
        Button leaveBtn = sbBtn(FontAwesomeSolid.CALENDAR_MINUS, "Xin nghỉ phép");
        Button shiftBtn = sbBtn(FontAwesomeSolid.BUSINESS_TIME, "Đăng ký ca");
        Button noteBtn = sbBtn(FontAwesomeSolid.STICKY_NOTE, "Ghi chú công việc");

        menu.getChildren().addAll(
            menuTitle, homeBtn, attendanceBtn, histBtn, statBtn,
            accountTitle, passBtn, leaveBtn, shiftBtn, noteBtn
        );

        if ("ADMIN".equals(role)) {
            Label adminT = new Label("QUẢN TRỊ");
            adminT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            adminT.setTextFill(Color.web(C_MUTED));
            adminT.setPadding(new Insets(12, 0, 10, 14));

            Button adminBtn = sbBtn(FontAwesomeSolid.CROWN, "Quản lý chấm công");
            Button userMgmtBtn = sbBtn(FontAwesomeSolid.USERS_COG, "Quản lý nhân viên");
            Button leaveMgmtBtn = sbBtn(FontAwesomeSolid.CLIPBOARD_LIST, "Duyệt đơn nghỉ");
            Button shiftMgmtBtn = sbBtn(FontAwesomeSolid.BUSINESS_TIME, "Quản lý ca làm");
            Button noteAdminBtn = sbBtn(FontAwesomeSolid.STICKY_NOTE, "Xem ghi chú NV");

            menu.getChildren().addAll(adminT, adminBtn, userMgmtBtn, leaveMgmtBtn, shiftMgmtBtn, noteAdminBtn);
            adminBtn.setOnAction(e -> { setActive(adminBtn); showAdmin(); });
            userMgmtBtn.setOnAction(e -> { setActive(userMgmtBtn); showUserManagement(); });
            leaveMgmtBtn.setOnAction(e -> { setActive(leaveMgmtBtn); showLeaveManagement(); });
            shiftMgmtBtn.setOnAction(e -> { setActive(shiftMgmtBtn); showShiftManagement(); });
            noteAdminBtn.setOnAction(e -> { setActive(noteAdminBtn); showAllNotes(); });
        }

        homeBtn.setOnAction(e -> { setActive(homeBtn); showHome(); });
        attendanceBtn.setOnAction(e -> { setActive(attendanceBtn); showAttendance(); });
        histBtn.setOnAction(e -> { setActive(histBtn); showHistory(); });
        statBtn.setOnAction(e -> { setActive(statBtn); showStats(); });
        passBtn.setOnAction(e -> { setActive(passBtn); showChangePassword(); });
        leaveBtn.setOnAction(e -> { setActive(leaveBtn); showLeaveRequest(); });
        shiftBtn.setOnAction(e -> { setActive(shiftBtn); showWorkShift(); });
        noteBtn.setOnAction(e -> { setActive(noteBtn); showWorkNote(); });

        menuScroll.setContent(menu);
        sidebar.getChildren().addAll(logoBox, divider, menuScroll);

        VBox userBox = new VBox(10);
        userBox.setPadding(new Insets(16, 22, 22, 22));
        userBox.setStyle("-fx-border-color: " + C_BORDER + "; -fx-border-width: 1 0 0 0;");

        HBox ua = new HBox(10);
        ua.setAlignment(Pos.CENTER_LEFT);
        StackPane avCircle = new StackPane();
        avCircle.setPrefSize(38, 38);
        avCircle.setMaxSize(38, 38);
        avCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + C_ACCENT + ", " + C_TEAL + "); -fx-background-radius: 19;");
        Label avText = new Label(name.substring(0, 1).toUpperCase());
        avText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        avText.setTextFill(Color.WHITE);
        avCircle.getChildren().add(avText);

        VBox ui = new VBox(0);
        Label un = new Label(name);
        un.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        un.setTextFill(Color.web(C_TEXT));
        Label ur = new Label(role);
        ur.setFont(Font.font("Consolas", 9));
        ur.setTextFill(Color.web(C_MUTED));
        ui.getChildren().addAll(un, ur);
        ua.getChildren().addAll(avCircle, ui);

        Button logoutBtn = new Button("  Đăng xuất");
        FontIcon outIcn = new FontIcon(FontAwesomeSolid.SIGN_OUT_ALT);
        outIcn.setIconSize(13);
        outIcn.setIconColor(Color.web(C_RED));
        logoutBtn.setGraphic(outIcn);
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setPrefHeight(38);
        logoutBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        logoutBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #fecaca; -fx-border-radius: 8;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle("-fx-background-color: " + C_RED + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: " + C_RED + "; -fx-border-radius: 8;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #fecaca; -fx-border-radius: 8;"));
        logoutBtn.setOnAction(e -> { http.setToken(null); showLogin(); });

        userBox.getChildren().addAll(ua, logoutBtn);
        sidebar.getChildren().add(userBox);

        HBox header = new HBox(15);
        header.setPadding(new Insets(20, 32, 20, 32));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER + "; -fx-border-width: 0 0 1 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        VBox headerText = new VBox(3);
        pageTitle = new Label("Trang chủ");
        pageTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        pageTitle.setTextFill(Color.web(C_TEXT));

        pageSubtitle = new Label("Tổng quan");
        pageSubtitle.setFont(Font.font("Segoe UI", 13));
        pageSubtitle.setTextFill(Color.web(C_MUTED));
        headerText.getChildren().addAll(pageTitle, pageSubtitle);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox clockBox = new HBox(8);
        clockBox.setAlignment(Pos.CENTER);
        clockBox.setPadding(new Insets(9, 18, 9, 18));
        clockBox.setStyle("-fx-background-color: #ecfeff; -fx-background-radius: 20; -fx-border-color: #a5f3fc; -fx-border-radius: 20; -fx-border-width: 1;");

        FontIcon clockIcn = new FontIcon(FontAwesomeSolid.CLOCK);
        clockIcn.setIconSize(13);
        clockIcn.setIconColor(Color.web(C_ACCENT));

        clockLabel = new Label("--:--:--");
        clockLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        clockLabel.setTextFill(Color.web(C_ACCENT));

        clockBox.getChildren().addAll(clockIcn, clockLabel);
        header.getChildren().addAll(headerText, sp, clockBox);

        contentArea = new VBox(28);
        contentArea.setPadding(new Insets(32, 40, 32, 40));

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + C_BG + "; -fx-background-color: " + C_BG + ";");

        root.setLeft(sidebar);
        root.setTop(header);
        root.setCenter(scroll);

        setActive(homeBtn);
        showHome();
        startClock();

        Scene scene = new Scene(root, 1400, 850);
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));
        primaryStage.setScene(scene);
    }

    private Button sbBtn(FontAwesomeSolid icon, String text) {
        Button btn = new Button("  " + text);
        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(14);
        icn.setIconColor(Color.web(C_MUTED));
        btn.setGraphic(icn);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(42);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setFont(Font.font("Segoe UI", 13));
        btn.setPadding(new Insets(0, 0, 0, 16));
        setInactive(btn);
        btn.setOnMouseEntered(e -> {
            if (!btn.equals(activeBtn)) {
                setHover(btn);
                icn.setIconColor(Color.web(C_ACCENT));
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.equals(activeBtn)) {
                setInactive(btn);
                icn.setIconColor(Color.web(C_MUTED));
            }
        });
        return btn;
    }

    private void setInactive(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + C_TEXT2 + "; -fx-background-radius: 8; -fx-cursor: hand;");
    }

    private void setHover(Button btn) {
        btn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand;");
    }

    private void setActive(Button btn) {
        stopAttendanceTicker();
        if (activeBtn != null) {
            setInactive(activeBtn);
            if (activeBtn.getGraphic() instanceof FontIcon) {
                ((FontIcon) activeBtn.getGraphic()).setIconColor(Color.web(C_MUTED));
            }
        }
        if (btn == null) { activeBtn = null; return; }
        btn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 8, 0, 0, 2);");
        if (btn.getGraphic() instanceof FontIcon) {
            ((FontIcon) btn.getGraphic()).setIconColor(Color.WHITE);
        }
        activeBtn = btn;
    }

    // ============================================================
    // HOME
    // ============================================================
    private void showHome() {
        pageTitle.setText("Trang chủ");
        pageSubtitle.setText("Tổng quan hệ thống chấm công");
        contentArea.getChildren().clear();

        HBox welcome = new HBox(20);
        welcome.setPadding(new Insets(32, 36, 32, 36));
        welcome.setAlignment(Pos.CENTER_LEFT);
        welcome.setMaxWidth(Double.MAX_VALUE);
        welcome.setStyle("-fx-background-color: linear-gradient(to right, " + C_ACCENT + ", " + C_TEAL + "); -fx-background-radius: 18; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 20, 0, 0, 8);");

        VBox info = new VBox(10);
        Label greet = new Label("Xin chào, " + fullName + "! 👋");
        greet.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        greet.setTextFill(Color.WHITE);

        Label date = new Label(getTodayString());
        date.setFont(Font.font("Segoe UI", 15));
        date.setTextFill(Color.web("#cffafe"));

        info.getChildren().addAll(greet, date);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(12, 24, 12, 24));
        statusBox.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 24;");

        FontIcon dot = new FontIcon(FontAwesomeSolid.CIRCLE);
        dot.setIconSize(11);
        dot.setIconColor(Color.web("#4ade80"));

        Label statusLbl = new Label("ONLINE");
        statusLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        statusLbl.setTextFill(Color.WHITE);

        statusBox.getChildren().addAll(dot, statusLbl);
        welcome.getChildren().addAll(info, sp, statusBox);

        Label st = new Label("Thống kê nhanh");
        st.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        st.setTextFill(Color.web(C_TEXT));

        HBox stats = new HBox(20);
        stats.setMaxWidth(Double.MAX_VALUE);

        Label inVal = new Label("Đang tải...");
        Label outVal = new Label("Đang tải...");
        Label statusVal = new Label("Đang tải...");

        VBox s1 = miniCard(FontAwesomeSolid.CALENDAR_DAY, "Hôm nay", getTodayShort(), C_BLUE);
        VBox s2 = miniCard(FontAwesomeSolid.SIGN_IN_ALT, "Giờ vào", inVal, C_GREEN);
        VBox s3 = miniCard(FontAwesomeSolid.SIGN_OUT_ALT, "Giờ ra", outVal, C_ORANGE);
        VBox s4 = miniCard(FontAwesomeSolid.CHECK_CIRCLE, "Trạng thái", statusVal, C_ACCENT);

        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox.setHgrow(s3, Priority.ALWAYS);
        HBox.setHgrow(s4, Priority.ALWAYS);

        stats.getChildren().addAll(s1, s2, s3, s4);

        Label ft = new Label("Chức năng chính");
        ft.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        ft.setTextFill(Color.web(C_TEXT));

        HBox feats = new HBox(20);
        feats.setMaxWidth(Double.MAX_VALUE);

        VBox f1 = bigCard(FontAwesomeSolid.CLOCK, "CHẤM CÔNG", "Điểm danh vào / ra hôm nay", C_GREEN,
            () -> { setActive(attendanceBtn); showAttendance(); });
        VBox f2 = bigCard(FontAwesomeSolid.HISTORY, "LỊCH SỬ", "Xem chấm công", C_BLUE,
            () -> { setActive(null); showHistory(); });
        VBox f3 = bigCard(FontAwesomeSolid.CHART_BAR, "THỐNG KÊ", "Số liệu cá nhân", C_VIOLET,
            () -> { setActive(null); showStats(); });

        HBox.setHgrow(f1, Priority.ALWAYS);
        HBox.setHgrow(f2, Priority.ALWAYS);
        HBox.setHgrow(f3, Priority.ALWAYS);

        feats.getChildren().addAll(f1, f2, f3);
        contentArea.getChildren().addAll(welcome, st, stats, ft, feats);

        loadTodayStatus(stt -> {
            if (stt.error != null) {
                inVal.setText("—");
                outVal.setText("—");
                statusVal.setText("Mất kết nối");
                statusVal.setTextFill(Color.web(C_RED));
                return;
            }

            inVal.setText(stt.checkIn.isEmpty() ? "Chưa vào" : stt.checkIn);
            outVal.setText(stt.checkOut.isEmpty() ? "Chưa ra" : stt.checkOut);

            if ("WORKING".equals(stt.status)) {
                statusVal.setText("Đang làm việc");
                statusVal.setTextFill(Color.web(C_GREEN));
            } else if ("COMPLETED".equals(stt.status)) {
                statusVal.setText("Hoàn thành");
                statusVal.setTextFill(Color.web(C_BLUE));
            } else {
                statusVal.setText("Chưa check-in");
                statusVal.setTextFill(Color.web(C_ORANGE));
            }
        });
    }

    private VBox miniCard(FontAwesomeSolid icon, String title, String value, String color) {
        return miniCard(icon, title, new Label(value), color);
    }

    private VBox miniCard(FontAwesomeSolid icon, String title, Label v, String color) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(24, 26, 24, 26));
        card.setMinWidth(220);
        card.setPrefHeight(130);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(38, 38);
        iconCircle.setMaxSize(38, 38);
        iconCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 19;");

        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icn);

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        t.setTextFill(Color.web(C_MUTED));
        top.getChildren().addAll(iconCircle, t);

        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        v.setTextFill(Color.web(color));

        card.getChildren().addAll(top, v);
        return card;
    }

    private VBox bigCard(FontAwesomeSolid icon, String title, String desc, String color, Runnable onClick) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(260);
        card.setPrefHeight(180);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(68, 68);
        iconCircle.setMaxSize(68, 68);
        iconCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 34;");

        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(30);
        icn.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icn);

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        t.setTextFill(Color.web(color));
        Label d = new Label(desc);
        d.setFont(Font.font("Segoe UI", 13));
        d.setTextFill(Color.web(C_MUTED));

        card.getChildren().addAll(iconCircle, t, d);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + color + "; -fx-border-radius: 16; -fx-border-width: 1.5; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + color + "44, 24, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);"));
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    // ============================================================
    // CHẤM CÔNG — MỘT TRANG DUY NHẤT, STATE-AWARE
    // ============================================================
    private static class TodayStatus {
        String date = "";
        String checkIn = "";
        String checkOut = "";
        String status = "NOT_CHECKED_IN";
        String serverTime = "";
        String error = null;
        int workedMinutes = 0;

        boolean isWorking()      { return "WORKING".equals(status); }
        boolean isCompleted()    { return "COMPLETED".equals(status); }
        boolean isNotCheckedIn() { return !isWorking() && !isCompleted(); }
    }

    private void showAttendance() {
        pageTitle.setText("Chấm công hôm nay");
        pageSubtitle.setText("Điểm danh vào / ra — giờ do server ghi nhận");
        contentArea.getChildren().clear();

        VBox main = new VBox(20);

        // ================= HERO =================
        HBox hero = new HBox(28);
        hero.setPadding(new Insets(28, 32, 28, 32));
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setStyle("-fx-background-color: linear-gradient(to right, " + C_ACCENT + ", " + C_TEAL + "); -fx-background-radius: 18; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.35), 20, 0, 0, 8);");

        VBox heroLeft = new VBox(9);
        Label heroDate = new Label(getTodayString());
        heroDate.setFont(Font.font("Segoe UI", 14));
        heroDate.setTextFill(Color.web("#cffafe"));

        Label heroStatus = new Label("ĐANG TẢI...");
        heroStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        heroStatus.setTextFill(Color.WHITE);
        heroStatus.setPadding(new Insets(7, 18, 7, 18));
        heroStatus.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 20;");

        Label heroHint = new Label("Đang kiểm tra trạng thái chấm công của bạn...");
        heroHint.setFont(Font.font("Segoe UI", 13));
        heroHint.setTextFill(Color.web("#e0f2fe"));
        heroHint.setWrapText(true);
        heroLeft.getChildren().addAll(heroDate, heroStatus, heroHint);

        Region heroSp = new Region();
        HBox.setHgrow(heroSp, Priority.ALWAYS);

        VBox clockBox = new VBox(2);
        clockBox.setAlignment(Pos.CENTER_RIGHT);
        Label bigClock = new Label("--:--:--");
        bigClock.setFont(Font.font("Consolas", FontWeight.BOLD, 42));
        bigClock.setTextFill(Color.WHITE);
        Label clockNote = new Label("Giờ máy chủ: --:--:--");
        clockNote.setFont(Font.font("Consolas", 11));
        clockNote.setTextFill(Color.web("#cffafe"));
        clockBox.getChildren().addAll(bigClock, clockNote);

        hero.getChildren().addAll(heroLeft, heroSp, clockBox);

        // ================= INFO CARDS =================
        HBox infoRow = new HBox(16);

        Label inValue = new Label("--:--");
        Label outValue = new Label("--:--");
        Label workValue = new Label("--");
        Label stateValue = new Label("--");

        VBox c1 = attendanceInfoCard(FontAwesomeSolid.SIGN_IN_ALT, "GIỜ VÀO", inValue, C_GREEN);
        VBox c2 = attendanceInfoCard(FontAwesomeSolid.SIGN_OUT_ALT, "GIỜ RA", outValue, C_ORANGE);
        VBox c3 = attendanceInfoCard(FontAwesomeSolid.CLOCK, "THỜI GIAN LÀM", workValue, C_BLUE);
        VBox c4 = attendanceInfoCard(FontAwesomeSolid.CHECK_CIRCLE, "TRẠNG THÁI", stateValue, C_ACCENT);

        for (VBox c : new VBox[]{c1, c2, c3, c4}) HBox.setHgrow(c, Priority.ALWAYS);
        infoRow.getChildren().addAll(c1, c2, c3, c4);

        // ================= ACTION =================
        HBox actionRow = new HBox(20);

        VBox actionCard = new VBox(12);
        actionCard.setPadding(new Insets(26, 28, 26, 28));
        actionCard.setMaxWidth(Double.MAX_VALUE);
        actionCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");
        HBox.setHgrow(actionCard, Priority.ALWAYS);

        Label actionTitle = new Label("Một nút duy nhất — đúng trạng thái");
        actionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        actionTitle.setTextFill(Color.web(C_TEXT));

        Label actionDesc = new Label("Lần đầu trong ngày: CHECK-IN. Khi kết thúc: CHECK-OUT. Hệ thống tự đổi nút theo trạng thái thực tế.");
        actionDesc.setFont(Font.font("Segoe UI", 13));
        actionDesc.setTextFill(Color.web(C_MUTED));
        actionDesc.setWrapText(true);

        Button actionBtn = new Button("ĐANG KIỂM TRA...");
        actionBtn.setPrefHeight(56);
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        actionBtn.setDisable(true);

        Label result = new Label("");
        result.setFont(Font.font("Segoe UI", 13));
        result.setWrapText(true);

        actionCard.getChildren().addAll(actionTitle, actionDesc, actionBtn, result);

        VBox ruleCard = new VBox(12);
        ruleCard.setPrefWidth(360);
        ruleCard.setMinWidth(320);
        ruleCard.setPadding(new Insets(24, 26, 24, 26));
        ruleCard.setStyle("-fx-background-color: #f0fdfa; -fx-background-radius: 16; -fx-border-color: #99f6e4; -fx-border-radius: 16; -fx-border-width: 1;");

        Label ruleTitle = new Label("📌 Quy tắc chấm công");
        ruleTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        ruleTitle.setTextFill(Color.web(C_TEAL));

        Label r1 = ruleLabel("• Mỗi ngày chỉ CHECK-IN 1 lần.");
        Label r2 = ruleLabel("• Phải CHECK-IN trước khi CHECK-OUT.");
        Label r3 = ruleLabel("• Giờ vào / ra do SERVER ghi nhận, không sửa được từ client.");
        Label r4 = ruleLabel("• Ca làm chuẩn: 08:00 — 17:00.");
        ruleCard.getChildren().addAll(ruleTitle, r1, r2, r3, r4);

        actionRow.getChildren().addAll(actionCard, ruleCard);

        // ================= RECENT HISTORY =================
        VBox historyCard = new VBox(14);
        historyCard.setPadding(new Insets(24, 26, 24, 26));
        historyCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox historyHeader = new HBox(12);
        historyHeader.setAlignment(Pos.CENTER_LEFT);
        Label historyTitle = new Label("📅 5 ngày chấm công gần nhất");
        historyTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        historyTitle.setTextFill(Color.web(C_TEXT));
        Region historySp = new Region();
        HBox.setHgrow(historySp, Priority.ALWAYS);
        Label recentInfo = new Label("Đang tải...");
        recentInfo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        recentInfo.setTextFill(Color.web(C_ACCENT));
        historyHeader.getChildren().addAll(historyTitle, historySp, recentInfo);

        TableView<HistRow> recentTable = new TableView<>();
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentTable.setPrefHeight(230);

        TableColumn<HistRow, String> rc1 = new TableColumn<>("Ngày");
        rc1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().date));
        TableColumn<HistRow, String> rc2 = new TableColumn<>("Giờ vào");
        rc2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().in));
        TableColumn<HistRow, String> rc3 = new TableColumn<>("Giờ ra");
        rc3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().out));
        TableColumn<HistRow, String> rc4 = new TableColumn<>("Tổng giờ");
        rc4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().total));
        TableColumn<HistRow, String> rc5 = new TableColumn<>("Trạng thái");
        rc5.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        rc5.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.contains("Đủ giờ")) setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;");
                else if (item.contains("Chưa")) setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;");
                else if (item.contains("muộn")) setStyle("-fx-text-fill: " + C_RED + "; -fx-font-weight: bold;");
            }
        });

        recentTable.getColumns().addAll(rc1, rc2, rc3, rc4, rc5);
        historyCard.getChildren().addAll(historyHeader, recentTable);

        main.getChildren().addAll(hero, infoRow, actionRow, historyCard);
        contentArea.getChildren().add(main);

        final TodayStatus[] current = { new TodayStatus() };

        java.util.function.Consumer<TodayStatus> apply = st -> {
            current[0] = st;

            if (st.error != null) {
                heroStatus.setText("MẤT KẾT NỐI");
                heroHint.setText("Không kết nối được server: " + st.error);
                inValue.setText("—");
                outValue.setText("—");
                workValue.setText("—");
                stateValue.setText("Lỗi");
                stateValue.setTextFill(Color.web(C_RED));
                actionBtn.setText("THỬ LẠI KẾT NỐI");
                styleActionButton(actionBtn, C_BLUE);
                actionBtn.setDisable(false);
                return;
            }

            inValue.setText(st.checkIn.isEmpty() ? "Chưa vào" : st.checkIn);
            outValue.setText(st.checkOut.isEmpty() ? "Chưa ra" : st.checkOut);
            clockNote.setText("Giờ máy chủ: " + (st.serverTime.isEmpty() ? "--:--:--" : st.serverTime));

            if (st.isNotCheckedIn()) {
                heroStatus.setText("CHƯA CHECK-IN");
                heroHint.setText("Nhấn CHECK-IN để bắt đầu ngày làm việc. Giờ vào sẽ do server ghi lại.");
                workValue.setText("—");
                stateValue.setText("Chưa vào");
                stateValue.setTextFill(Color.web(C_ORANGE));
                actionBtn.setText("CHECK-IN NGAY");
                styleActionButton(actionBtn, C_GREEN);
                actionBtn.setDisable(false);
            } else if (st.isWorking()) {
                heroStatus.setText("ĐANG LÀM VIỆC");
                heroHint.setText("Đã ghi nhận giờ vào. Khi kết thúc, nhấn CHECK-OUT để chốt giờ ra.");
                workValue.setText(formatWorked(minutesBetween(st.checkIn, java.time.LocalTime.now())));
                stateValue.setText("Đang làm");
                stateValue.setTextFill(Color.web(C_GREEN));
                actionBtn.setText("CHECK-OUT NGAY");
                styleActionButton(actionBtn, C_ORANGE);
                actionBtn.setDisable(false);
            } else {
                heroStatus.setText("ĐÃ HOÀN THÀNH");
                heroHint.setText("Hôm nay bạn đã chấm công đầy đủ. Hẹn gặp lại vào ngày mai!");
                workValue.setText(formatWorked(st.workedMinutes));
                stateValue.setText("Hoàn thành");
                stateValue.setTextFill(Color.web(C_BLUE));
                actionBtn.setText("ĐÃ HOÀN THÀNH HÔM NAY");
                styleActionButton(actionBtn, C_BLUE);
                actionBtn.setDisable(true);
            }
        };

        Runnable loadRecent = () -> new Thread(() -> {
            try {
                String from = java.time.LocalDate.now().minusDays(6).toString();
                String to = java.time.LocalDate.now().toString();
                String res = http.get("/api/history?page=1&limit=5&from=" + from + "&to=" + to);
                ObservableList<HistRow> rows = parseHistory(res);
                Platform.runLater(() -> {
                    recentTable.setItems(rows);
                    recentInfo.setText("📊 " + rows.size() + " bản ghi");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> recentInfo.setText("❌ " + ex.getMessage()));
            }
        }).start();

        Runnable refresh = () -> {
            actionBtn.setDisable(true);
            actionBtn.setText("ĐANG KIỂM TRA...");
            loadTodayStatus(st -> {
                apply.accept(st);
                loadRecent.run();
            });
        };

        actionBtn.setOnAction(e -> {
            TodayStatus st = current[0];
            if (st == null || st.error != null) {
                refresh.run();
                return;
            }
            if (st.isCompleted()) return;
            confirmAttendance(st, actionBtn, result, refresh);
        });

        // Đồng hồ realtime + thời gian làm việc tăng theo giây
        stopAttendanceTicker();
        attendanceTicker = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.LocalTime now = java.time.LocalTime.now();
            bigClock.setText(String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));

            TodayStatus st = current[0];
            if (st != null && st.error != null && !st.error.isEmpty()) return;
            if (st != null && st.isWorking() && !st.checkIn.isEmpty()) {
                workValue.setText(formatWorked(minutesBetween(st.checkIn, now)));
            }
        }));
        attendanceTicker.setCycleCount(Timeline.INDEFINITE);
        attendanceTicker.play();

        refresh.run();
    }

    private Label ruleLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 12.5));
        l.setTextFill(Color.web("#0f766e"));
        l.setWrapText(true);
        return l;
    }

    private VBox attendanceInfoCard(FontAwesomeSolid icon, String title, Label value, String color) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 14; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 14; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 3);");

        HBox top = new HBox(9);
        top.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(13);
        icn.setIconColor(Color.web(color));
        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        t.setTextFill(Color.web(C_MUTED));
        top.getChildren().addAll(icn, t);

        value.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        value.setTextFill(Color.web(color));

        card.getChildren().addAll(top, value);
        return card;
    }

    private void styleActionButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + color + "55, 12, 0, 0, 4);");
    }

    private void loadTodayStatus(java.util.function.Consumer<TodayStatus> callback) {
        new Thread(() -> {
            TodayStatus st = new TodayStatus();
            try {
                String res = http.get("/api/attendance/today");
                String err = JsonUtil.getString(res, "error");
                if (err != null) {
                    st.error = err;
                } else {
                    st.date = extract(res, "date");
                    st.checkIn = extract(res, "checkIn");
                    st.checkOut = extract(res, "checkOut");
                    st.status = extract(res, "status");
                    st.serverTime = extract(res, "serverTime");
                    st.workedMinutes = extractInt(res, "workedMinutes");
                    if (st.status == null || st.status.isEmpty()) st.status = "NOT_CHECKED_IN";
                }
            } catch (Exception ex) {
                st.error = ex.getMessage();
            }
            Platform.runLater(() -> callback.accept(st));
        }).start();
    }

    private void confirmAttendance(TodayStatus st, Button actionBtn, Label result, Runnable refresh) {
        boolean isCheckIn = st.isNotCheckedIn();
        String actionName = isCheckIn ? "CHECK-IN" : "CHECK-OUT";
        String now = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("Xác nhận chấm công");
        alert.setHeaderText("Xác nhận " + actionName);
        alert.setContentText(
            "Thời điểm hiện tại: " + now + "\n\n" +
            (isCheckIn
                ? "Hệ thống sẽ ghi nhận GIỜ VÀO và bắt đầu tính thời gian làm việc."
                : "Hệ thống sẽ ghi nhận GIỜ RA và kết thúc ngày làm việc.") +
            "\n\n⚠ Giờ chấm công do SERVER ghi nhận, không thể sửa từ client."
        );
        alert.getDialogPane().setMinWidth(480);
        alert.getDialogPane().getStylesheets().add(
            "data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));

        ButtonType ok = new ButtonType("Xác nhận " + actionName, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, cancel);

        alert.showAndWait().ifPresent(bt -> {
            if (bt == ok) {
                performAttendance(isCheckIn ? "checkin" : "checkout", actionBtn, result, refresh);
            }
        });
    }

    private void performAttendance(String action, Button actionBtn, Label result, Runnable refresh) {
        actionBtn.setDisable(true);
        result.setText("⏳ Đang gửi yêu cầu...");
        result.setTextFill(Color.web(C_MUTED));

        new Thread(() -> {
            try {
                String res = http.post("/api/" + action, "{}");
                String msg = JsonUtil.getString(res, "msg");
                String err = JsonUtil.getString(res, "error");
                String time = JsonUtil.getString(res, "time");

                Platform.runLater(() -> {
                    if (msg != null) {
                        result.setText("✅ " + msg + (time != null ? " — " + time : ""));
                        result.setTextFill(Color.web(C_GREEN));
                    } else {
                        result.setText("❌ " + (err != null ? err : res));
                        result.setTextFill(Color.web(C_RED));
                    }
                    refresh.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    result.setText("❌ Lỗi kết nối: " + ex.getMessage());
                    result.setTextFill(Color.web(C_RED));
                    actionBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void stopAttendanceTicker() {
        if (attendanceTicker != null) {
            attendanceTicker.stop();
            attendanceTicker = null;
        }
    }

    private long minutesBetween(String from, java.time.LocalTime to) {
        try {
            String s = from.length() >= 8 ? from.substring(0, 8) : from;
            java.time.LocalTime start = java.time.LocalTime.parse(s);
            long mins = java.time.Duration.between(start, to).toMinutes();
            if (mins < 0) mins += 24 * 60;
            return Math.max(0, mins);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatWorked(long minutes) {
        if (minutes < 0) minutes = 0;
        return String.format("%d giờ %02d phút", minutes / 60, minutes % 60);
    }

    // ============================================================
    // ĐỔI MẬT KHẨU
    // ============================================================
    private void showChangePassword() {
        pageTitle.setText("Đổi mật khẩu");
        pageSubtitle.setText("Thay đổi mật khẩu đăng nhập");
        contentArea.getChildren().clear();

        VBox card = new VBox(20);
        card.setPrefWidth(560);
        card.setMaxWidth(560);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 18; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 18; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 16, 0, 0, 6);");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(FontAwesomeSolid.KEY);
        icn.setIconSize(24);
        icn.setIconColor(Color.web(C_ACCENT));
        Label title = new Label("  Đổi mật khẩu");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(C_TEXT));
        titleRow.getChildren().add(title);

        Label oldLbl = new Label("MẬT KHẨU CŨ");
        oldLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        oldLbl.setTextFill(Color.web(C_MUTED));
        PasswordField oldPass = new PasswordField();
        oldPass.setPrefHeight(46);
        oldPass.setStyle(INPUT_STYLE);

        Label newLbl = new Label("MẬT KHẨU MỚI");
        newLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        newLbl.setTextFill(Color.web(C_MUTED));
        PasswordField newPass = new PasswordField();
        newPass.setPrefHeight(46);
        newPass.setStyle(INPUT_STYLE);

        Label confirmLbl = new Label("XÁC NHẬN MẬT KHẨU MỚI");
        confirmLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        confirmLbl.setTextFill(Color.web(C_MUTED));
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPrefHeight(46);
        confirmPass.setStyle(INPUT_STYLE);

        Button btn = new Button("XÁC NHẬN ĐỔI");
        btn.setPrefHeight(50);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 12, 0, 0, 4);");

        Label result = new Label("");
        result.setFont(Font.font("Consolas", 13));
        result.setWrapText(true);

        btn.setOnAction(e -> {
            String o = oldPass.getText();
            String n = newPass.getText();
            String c = confirmPass.getText();
            if (o.isEmpty() || n.isEmpty() || c.isEmpty()) {
                result.setTextFill(Color.web(C_RED));
                result.setText("❌ Vui lòng nhập đầy đủ");
                return;
            }
            if (!n.equals(c)) {
                result.setTextFill(Color.web(C_RED));
                result.setText("❌ Mật khẩu xác nhận không khớp");
                return;
            }
            result.setTextFill(Color.web(C_MUTED));
            result.setText("⏳ Đang xử lý...");
            new Thread(() -> {
                try {
                    String body = "{\"oldPassword\":\"" + o + "\",\"newPassword\":\"" + n + "\"}";
                    String res = http.post("/api/change-password", body);
                    String msg = JsonUtil.getString(res, "msg");
                    String err = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> {
                        if (msg != null) {
                            result.setTextFill(Color.web(C_GREEN));
                            result.setText("✅ " + msg);
                            oldPass.clear(); newPass.clear(); confirmPass.clear();
                        } else {
                            result.setTextFill(Color.web(C_RED));
                            result.setText("❌ " + (err != null ? err : res));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        result.setTextFill(Color.web(C_RED));
                        result.setText("❌ Lỗi: " + ex.getMessage());
                    });
                }
            }).start();
        });

        card.getChildren().addAll(titleRow, oldLbl, oldPass, newLbl, newPass, confirmLbl, confirmPass, btn, result);
        contentArea.getChildren().add(card);
    }

        // ============================================================
    // XIN NGHỈ PHÉP
    // ============================================================
    public static class LeaveRow {
        public int id;
        public String startDate, endDate, reason, status;
        public LeaveRow(int id, String s, String e, String r, String st) {
            this.id = id;
            startDate = s; endDate = e; reason = r; status = st;
        }
    }

    private void showLeaveRequest() {
        pageTitle.setText("Xin nghỉ phép");
        pageSubtitle.setText("Gửi đơn xin nghỉ phép");
        contentArea.getChildren().clear();

        VBox main = new VBox(20);

        VBox card = new VBox(16);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(FontAwesomeSolid.CALENDAR_MINUS);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ORANGE));
        Label title = new Label("  Gửi đơn xin nghỉ phép");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));
        titleRow.getChildren().add(title);

        HBox dateRow = new HBox(15);

        VBox startBox = new VBox(6);
        Label startLbl = new Label("TỪ NGÀY");
        startLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        startLbl.setTextFill(Color.web(C_MUTED));
        DatePicker startDate = new DatePicker(java.time.LocalDate.now());
        startDate.setMaxWidth(Double.MAX_VALUE);
        startDate.setStyle(DATE_PICKER_STYLE);
        startBox.getChildren().addAll(startLbl, startDate);
        HBox.setHgrow(startBox, Priority.ALWAYS);

        VBox endBox = new VBox(6);
        Label endLbl = new Label("ĐẾN NGÀY");
        endLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        endLbl.setTextFill(Color.web(C_MUTED));
        DatePicker endDate = new DatePicker(java.time.LocalDate.now());
        endDate.setMaxWidth(Double.MAX_VALUE);
        endDate.setStyle(DATE_PICKER_STYLE);
        endBox.getChildren().addAll(endLbl, endDate);
        HBox.setHgrow(endBox, Priority.ALWAYS);

        dateRow.getChildren().addAll(startBox, endBox);

        Label reasonLbl = new Label("LÝ DO");
        reasonLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        reasonLbl.setTextFill(Color.web(C_MUTED));

        TextArea reason = new TextArea();
        reason.setPrefHeight(120);
        reason.setPromptText("Nhập lý do xin nghỉ...");
        reason.setStyle(TEXT_AREA_STYLE);

        Button sendBtn = new Button("GỬI ĐƠN");
        sendBtn.setPrefHeight(50);
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        sendBtn.setStyle("-fx-background-color: " + C_ORANGE + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(234,88,12,0.4), 12, 0, 0, 4);");

        Label result = new Label("");
        result.setFont(Font.font("Consolas", 13));
        result.setWrapText(true);

        sendBtn.setOnAction(e -> {
            if (startDate.getValue() == null || endDate.getValue() == null) {
                result.setTextFill(Color.web(C_RED));
                result.setText("❌ Chọn ngày bắt đầu và kết thúc");
                return;
            }
            result.setTextFill(Color.web(C_MUTED));
            result.setText("⏳ Đang gửi đơn...");
            sendBtn.setDisable(true);
            new Thread(() -> {
                try {
                    String body = "{\"startDate\":\"" + startDate.getValue() + "\",\"endDate\":\"" + endDate.getValue() + "\",\"reason\":\"" + reason.getText().replace("\"", "'") + "\"}";
                    String res = http.post("/api/leave-request", body);
                    String msg = JsonUtil.getString(res, "msg");
                    String err = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> {
                        sendBtn.setDisable(false);
                        if (msg != null) {
                            result.setTextFill(Color.web(C_GREEN));
                            result.setText("✅ " + msg);
                            reason.clear();
                            showLeaveRequest();
                        } else {
                            result.setTextFill(Color.web(C_RED));
                            result.setText("❌ " + (err != null ? err : res));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        sendBtn.setDisable(false);
                        result.setTextFill(Color.web(C_RED));
                        result.setText("❌ Lỗi: " + ex.getMessage());
                    });
                }
            }).start();
        });

        card.getChildren().addAll(titleRow, dateRow, reasonLbl, reason, sendBtn, result);

        VBox listCard = new VBox(15);
        listCard.setPadding(new Insets(28));
        listCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label listTitle = new Label("📋 Đơn đã gửi");
        listTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        listTitle.setTextFill(Color.web(C_TEXT));

        TableView<LeaveRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);

        TableColumn<LeaveRow, String> c1 = new TableColumn<>("Từ ngày");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().startDate));
        TableColumn<LeaveRow, String> c2 = new TableColumn<>("Đến ngày");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().endDate));
        TableColumn<LeaveRow, String> c3 = new TableColumn<>("Lý do");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reason));
        TableColumn<LeaveRow, String> c4 = new TableColumn<>("Trạng thái");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        c4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    if ("PENDING".equals(item)) { setText("⏳ Chờ duyệt"); setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;"); }
                    else if ("APPROVED".equals(item)) { setText("✅ Đã duyệt"); setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;"); }
                    else if ("REJECTED".equals(item)) { setText("❌ Từ chối"); setStyle("-fx-text-fill: " + C_RED + "; -fx-font-weight: bold;"); }
                }
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4);
        listCard.getChildren().addAll(listTitle, table);
        main.getChildren().addAll(card, listCard);
        contentArea.getChildren().add(main);

        new Thread(() -> {
            try {
                String res = http.get("/api/leave-list");
                ObservableList<LeaveRow> rows = parseLeaves(res);
                Platform.runLater(() -> table.setItems(rows));
            } catch (Exception ex) { }
        }).start();
    }

    private ObservableList<LeaveRow> parseLeaves(String json) {
        ObservableList<LeaveRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        int aEnd = json.lastIndexOf(']');
        if (aStart < 0 || aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        if (arr.trim().isEmpty()) return rows;
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String s = extract(item, "startDate");
            String e = extract(item, "endDate");
            String r = extract(item, "reason");
            String st = extract(item, "status");
            if (s == null || s.isEmpty()) continue;
            int id = extractInt(item, "id");
            rows.add(new LeaveRow(id, s, e, r, st));
        }
        return rows;
    }

    // ============================================================
    // ĐĂNG KÝ CA LÀM VIỆC (User)
    // ============================================================
    public static class ShiftRow {
        public int id;
        public String shiftDate, shiftType, note;
        public ShiftRow(int id, String date, String type, String note) {
            this.id = id;
            this.shiftDate = date;
            this.shiftType = type;
            this.note = note;
        }
    }

    private void showWorkShift() {
        pageTitle.setText("Đăng ký ca làm việc");
        pageSubtitle.setText("Đăng ký ca Sáng / Chiều / Tối");
        contentArea.getChildren().clear();

        VBox main = new VBox(20);

        VBox card = new VBox(16);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(FontAwesomeSolid.BUSINESS_TIME);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ACCENT));
        Label title = new Label("  Đăng ký ca làm việc");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));
        titleRow.getChildren().add(title);

        HBox formRow = new HBox(15);

        VBox dateBox = new VBox(6);
        Label dateLbl = new Label("NGÀY LÀM");
        dateLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        dateLbl.setTextFill(Color.web(C_MUTED));
        DatePicker shiftDate = new DatePicker(java.time.LocalDate.now());
        shiftDate.setMaxWidth(Double.MAX_VALUE);
        shiftDate.setStyle(DATE_PICKER_STYLE);
        dateBox.getChildren().addAll(dateLbl, shiftDate);
        HBox.setHgrow(dateBox, Priority.ALWAYS);

        VBox typeBox = new VBox(6);
        Label typeLbl = new Label("CA LÀM");
        typeLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        typeLbl.setTextFill(Color.web(C_MUTED));
        ComboBox<String> typeBoxCb = new ComboBox<>();
        typeBoxCb.getItems().addAll("SANG", "CHIEU", "TOI");
        typeBoxCb.setValue("SANG");
        typeBoxCb.setMaxWidth(Double.MAX_VALUE);
        typeBoxCb.setStyle(COMBO_BOX_STYLE);
        typeBox.getChildren().addAll(typeLbl, typeBoxCb);
        HBox.setHgrow(typeBox, Priority.ALWAYS);

        formRow.getChildren().addAll(dateBox, typeBox);

        Label noteLbl = new Label("GHI CHÚ");
        noteLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        noteLbl.setTextFill(Color.web(C_MUTED));
        TextField noteField = new TextField();
        noteField.setPromptText("Ghi chú (không bắt buộc)...");
        noteField.setPrefHeight(44);
        noteField.setStyle(INPUT_STYLE);

        Button submitBtn = new Button("ĐĂNG KÝ");
        submitBtn.setPrefHeight(50);
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        submitBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 12, 0, 0, 4);");

        Label result = new Label("");
        result.setFont(Font.font("Consolas", 13));
        result.setWrapText(true);

        submitBtn.setOnAction(e -> {
            if (shiftDate.getValue() == null) {
                result.setTextFill(Color.web(C_RED));
                result.setText("❌ Chọn ngày làm");
                return;
            }
            result.setTextFill(Color.web(C_MUTED));
            result.setText("⏳ Đang đăng ký...");
            submitBtn.setDisable(true);
            new Thread(() -> {
                try {
                    String body = "{\"shiftDate\":\"" + shiftDate.getValue() + "\","
                                + "\"shiftType\":\"" + typeBoxCb.getValue() + "\","
                                + "\"note\":\"" + noteField.getText().replace("\"", "'") + "\"}";
                    String res = http.post("/api/work-shifts/register", body);
                    String msg = JsonUtil.getString(res, "msg");
                    String err = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        if (msg != null) {
                            result.setTextFill(Color.web(C_GREEN));
                            result.setText("✅ " + msg);
                            noteField.clear();
                            showWorkShift();
                        } else {
                            result.setTextFill(Color.web(C_RED));
                            result.setText("❌ " + (err != null ? err : res));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        submitBtn.setDisable(false);
                        result.setTextFill(Color.web(C_RED));
                        result.setText("❌ Lỗi: " + ex.getMessage());
                    });
                }
            }).start();
        });

        card.getChildren().addAll(titleRow, formRow, noteLbl, noteField, submitBtn, result);

        VBox listCard = new VBox(15);
        listCard.setPadding(new Insets(24));
        listCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label listTitle = new Label("📋 Ca đã đăng ký");
        listTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        listTitle.setTextFill(Color.web(C_TEXT));

        TableView<ShiftRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);

        TableColumn<ShiftRow, String> c1 = new TableColumn<>("📅 Ngày");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shiftDate));
        c1.setPrefWidth(160);

        TableColumn<ShiftRow, String> c2 = new TableColumn<>("🕐 Ca");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shiftType));
        c2.setPrefWidth(120);
        c2.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if ("SANG".equals(item)) setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;");
                    else if ("CHIEU".equals(item)) setStyle("-fx-text-fill: " + C_BLUE + "; -fx-font-weight: bold;");
                    else if ("TOI".equals(item)) setStyle("-fx-text-fill: " + C_VIOLET + "; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<ShiftRow, String> c3 = new TableColumn<>("📝 Ghi chú");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().note));
        c3.setPrefWidth(300);

        TableColumn<ShiftRow, String> c4 = new TableColumn<>("Hành động");
        c4.setPrefWidth(120);
        c4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                ShiftRow row = getTableView().getItems().get(getIndex());

                Button delBtn = new Button("Xóa");
                delBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                delBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 12 5 12; -fx-border-color: #fecaca; -fx-border-radius: 6; -fx-border-width: 1;");
                delBtn.setOnAction(e -> {
                    new Thread(() -> {
                        try {
                            http.delete("/api/work-shifts/" + row.id);
                            Platform.runLater(() -> showWorkShift());
                        } catch (Exception ex) { }
                    }).start();
                });
                setGraphic(delBtn);
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4);
        listCard.getChildren().addAll(listTitle, table);
        main.getChildren().addAll(card, listCard);
        contentArea.getChildren().add(main);

        new Thread(() -> {
            try {
                String res = http.get("/api/work-shifts/list");
                ObservableList<ShiftRow> rows = parseWorkShifts(res);
                Platform.runLater(() -> table.setItems(rows));
            } catch (Exception ex) { }
        }).start();
    }

    private ObservableList<ShiftRow> parseWorkShifts(String json) {
        ObservableList<ShiftRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        int aEnd = json.lastIndexOf(']');
        if (aStart < 0 || aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        if (arr.trim().isEmpty()) return rows;
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String date = extract(item, "shiftDate");
            String type = extract(item, "shiftType");
            String note = extract(item, "note");
            if (date == null || date.isEmpty()) continue;
            int id = extractInt(item, "id");
            rows.add(new ShiftRow(id, date, type, note));
        }
        return rows;
    }

    // ============================================================
    // GHI CHÚ CÔNG VIỆC — USER
    // ============================================================
    public static class NoteRow {
        public int id;
        public String username, fullName, noteDate, content, plan, createdAt;
        public NoteRow(int id, String u, String f, String d, String c, String p, String ca) {
            this.id = id;
            this.username = u;
            this.fullName = f;
            this.noteDate = d;
            this.content = c;
            this.plan = p;
            this.createdAt = ca;
        }
    }

    private void showWorkNote() {
        pageTitle.setText("Ghi chú công việc");
        pageSubtitle.setText("Viết note hàng ngày");
        contentArea.getChildren().clear();

        VBox main = new VBox(20);

        VBox card = new VBox(16);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(FontAwesomeSolid.STICKY_NOTE);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ACCENT));
        Label title = new Label("  Ghi chú công việc hôm nay");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));
        titleRow.getChildren().add(title);

        VBox dateBox = new VBox(6);
        Label dateLbl = new Label("NGÀY");
        dateLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        dateLbl.setTextFill(Color.web(C_MUTED));
        DatePicker noteDate = new DatePicker(java.time.LocalDate.now());
        noteDate.setMaxWidth(Double.MAX_VALUE);
        noteDate.setStyle(DATE_PICKER_STYLE);
        dateBox.getChildren().addAll(dateLbl, noteDate);

        Label contentLbl = new Label("ĐÃ LÀM GÌ HÔM NAY?");
        contentLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        contentLbl.setTextFill(Color.web(C_MUTED));

        TextArea contentArea2 = new TextArea();
        contentArea2.setPrefHeight(140);
        contentArea2.setPromptText("Mô tả công việc đã làm hôm nay...");
        contentArea2.setStyle(TEXT_AREA_STYLE);

        Label planLbl = new Label("KẾ HOẠCH NGÀY MAI");
        planLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        planLbl.setTextFill(Color.web(C_MUTED));

        TextArea planArea = new TextArea();
        planArea.setPrefHeight(120);
        planArea.setPromptText("Kế hoạch cho ngày mai...");
        planArea.setStyle(TEXT_AREA_STYLE);

        Button saveBtn = new Button("💾  LƯU GHI CHÚ");
        saveBtn.setPrefHeight(50);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        saveBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(8,145,178,0.4), 12, 0, 0, 4);");

        Label result = new Label("");
        result.setFont(Font.font("Consolas", 13));
        result.setWrapText(true);

        new Thread(() -> {
            try {
                String res = http.get("/api/notes/today");
                Platform.runLater(() -> {
                    String c = extract(res, "content");
                    String p = extract(res, "plan");
                    if (c != null && !c.isEmpty()) contentArea2.setText(c);
                    if (p != null && !p.isEmpty()) planArea.setText(p);
                });
            } catch (Exception ex) { }
        }).start();

        saveBtn.setOnAction(e -> {
            if (contentArea2.getText().isEmpty()) {
                result.setTextFill(Color.web(C_RED));
                result.setText("❌ Nhập nội dung đã làm");
                return;
            }
            result.setTextFill(Color.web(C_MUTED));
            result.setText("⏳ Đang lưu...");
            saveBtn.setDisable(true);
            new Thread(() -> {
                try {
                    String c = contentArea2.getText().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                    String p = planArea.getText().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                    String body = "{\"noteDate\":\"" + noteDate.getValue() + "\","
                                + "\"content\":\"" + c + "\","
                                + "\"plan\":\"" + p + "\"}";
                    String res = http.post("/api/notes/save", body);
                    String msg = JsonUtil.getString(res, "msg");
                    String err = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        if (msg != null) {
                            result.setTextFill(Color.web(C_GREEN));
                            result.setText("✅ " + msg);
                            showWorkNote();
                        } else {
                            result.setTextFill(Color.web(C_RED));
                            result.setText("❌ " + (err != null ? err : res));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        saveBtn.setDisable(false);
                        result.setTextFill(Color.web(C_RED));
                        result.setText("❌ Lỗi: " + ex.getMessage());
                    });
                }
            }).start();
        });

        card.getChildren().addAll(titleRow, dateBox, contentLbl, contentArea2, planLbl, planArea, saveBtn, result);

        VBox listCard = new VBox(15);
        listCard.setPadding(new Insets(24));
        listCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label listTitle = new Label("📋 Lịch sử ghi chú");
        listTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        listTitle.setTextFill(Color.web(C_TEXT));

        TableView<NoteRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(320);

        TableColumn<NoteRow, String> c1 = new TableColumn<>("📅 Ngày");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().noteDate));
        c1.setPrefWidth(120);
        TableColumn<NoteRow, String> c2 = new TableColumn<>("📝 Đã làm");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().content));
        c2.setPrefWidth(400);
        TableColumn<NoteRow, String> c3 = new TableColumn<>("🎯 Kế hoạch");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().plan));
        c3.setPrefWidth(320);
        TableColumn<NoteRow, String> c4 = new TableColumn<>("Hành động");
        c4.setPrefWidth(100);
        c4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                NoteRow row = getTableView().getItems().get(getIndex());
                Button delBtn = new Button("Xóa");
                delBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                delBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 12 5 12; -fx-border-color: #fecaca; -fx-border-radius: 6; -fx-border-width: 1;");
                delBtn.setOnAction(e -> {
                    new Thread(() -> {
                        try {
                            http.delete("/api/notes/" + row.id);
                            Platform.runLater(() -> showWorkNote());
                        } catch (Exception ex) { }
                    }).start();
                });
                setGraphic(delBtn);
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4);
        listCard.getChildren().addAll(listTitle, table);
        main.getChildren().addAll(card, listCard);
        contentArea.getChildren().add(main);

        new Thread(() -> {
            try {
                String res = http.get("/api/notes/my");
                ObservableList<NoteRow> rows = parseNotes(res);
                Platform.runLater(() -> table.setItems(rows));
            } catch (Exception ex) { }
        }).start();
    }

    // ============================================================
    // GHI CHÚ CÔNG VIỆC — ADMIN
    // ============================================================
    private void showAllNotes() {
        pageTitle.setText("Ghi chú nhân viên");
        pageSubtitle.setText("Xem ghi chú của tất cả nhân viên");
        contentArea.getChildren().clear();

        VBox card = new VBox(18);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox hdr = new HBox(15);
        hdr.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon(FontAwesomeSolid.STICKY_NOTE);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ACCENT));
        Label title = new Label("  Ghi chú của tất cả nhân viên");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label countLbl = new Label("⏳ Đang tải...");
        countLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        countLbl.setTextFill(Color.web(C_ACCENT));
        countLbl.setStyle("-fx-background-color: #ecfeff; -fx-background-radius: 20; -fx-padding: 8 18 8 18; -fx-border-color: #a5f3fc; -fx-border-radius: 20; -fx-border-width: 1;");

        Button refresh = new Button("  Làm mới");
        FontIcon rIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        rIcn.setIconSize(12);
        rIcn.setIconColor(Color.web(C_ACCENT));
        refresh.setGraphic(rIcn);
        refresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        refresh.setStyle("-fx-background-color: #ecfeff; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: #a5f3fc; -fx-border-radius: 8; -fx-border-width: 1;");
        refresh.setOnAction(e -> showAllNotes());

        hdr.getChildren().addAll(title, sp, countLbl, refresh);

        TextField search = new TextField();
        search.setPromptText("🔍 Tìm theo username / họ tên / nội dung...");
        search.setPrefHeight(44);
        search.setStyle(INPUT_STYLE);

        TableView<NoteRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(560);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<NoteRow, String> c0 = new TableColumn<>("👤 Username");
        c0.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().username));
        c0.setPrefWidth(110);
        TableColumn<NoteRow, String> c1 = new TableColumn<>("📛 Họ tên");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().fullName));
        c1.setPrefWidth(170);
        TableColumn<NoteRow, String> c2 = new TableColumn<>("📅 Ngày");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().noteDate));
        c2.setPrefWidth(110);
        TableColumn<NoteRow, String> c3 = new TableColumn<>("📝 Đã làm");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().content));
        c3.setPrefWidth(380);
        TableColumn<NoteRow, String> c4 = new TableColumn<>("🎯 Kế hoạch");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().plan));
        c4.setPrefWidth(320);

        table.getColumns().addAll(c0, c1, c2, c3, c4);
        card.getChildren().addAll(hdr, search, table);
        contentArea.getChildren().add(card);

        new Thread(() -> {
            try {
                String res = http.get("/api/notes/all");
                ObservableList<NoteRow> rows = parseNotes(res);
                Platform.runLater(() -> {
                    table.setItems(rows);
                    countLbl.setText("📊 " + rows.size() + " ghi chú");
                    search.textProperty().addListener((o, ov, nv) -> {
                        if (nv == null || nv.isEmpty()) table.setItems(rows);
                        else {
                            String key = nv.toLowerCase();
                            ObservableList<NoteRow> filtered = FXCollections.observableArrayList();
                            for (NoteRow r : rows)
                                if ((r.username != null && r.username.toLowerCase().contains(key))
                                    || (r.fullName != null && r.fullName.toLowerCase().contains(key))
                                    || (r.content != null && r.content.toLowerCase().contains(key)))
                                    filtered.add(r);
                            table.setItems(filtered);
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> countLbl.setText("❌ " + ex.getMessage()));
            }
        }).start();
    }

    private ObservableList<NoteRow> parseNotes(String json) {
        ObservableList<NoteRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        if (aStart < 0) return rows;
        int aEnd = json.lastIndexOf(']');
        if (aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        if (arr.trim().isEmpty()) return rows;
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String u = extract(item, "username");
            String f = extract(item, "fullName");
            String d = extract(item, "noteDate");
            String c = extract(item, "content");
            String p = extract(item, "plan");
            if (d == null || d.isEmpty()) continue;
            int id = extractInt(item, "id");
            rows.add(new NoteRow(id, u, f != null && !f.isEmpty() ? f : u, d, c, p, ""));
        }
        return rows;
    }

    // ============================================================
    // ADMIN — QUẢN LÝ CA LÀM VIỆC
    // ============================================================
    public static class ShiftAdminRow {
        public int id;
        public String username, fullName, shiftDate, shiftType, note;
        public ShiftAdminRow(int id, String u, String f, String d, String t, String n) {
            this.id = id;
            this.username = u;
            this.fullName = f;
            this.shiftDate = d;
            this.shiftType = t;
            this.note = n;
        }
    }

    private void showShiftManagement() {
        pageTitle.setText("Quản lý ca làm việc");
        pageSubtitle.setText("Xem ca đăng ký của tất cả nhân viên");
        contentArea.getChildren().clear();

        VBox card = new VBox(18);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox hdr = new HBox(15);
        hdr.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.BUSINESS_TIME);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ACCENT));

        Label title = new Label("  Quản lý ca làm việc");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label countLbl = new Label("⏳ Đang tải...");
        countLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        countLbl.setTextFill(Color.web(C_ACCENT));
        countLbl.setStyle("-fx-background-color: #ecfeff; -fx-background-radius: 20; -fx-padding: 8 18 8 18; -fx-border-color: #a5f3fc; -fx-border-radius: 20; -fx-border-width: 1;");

        Button refresh = new Button("  Làm mới");
        FontIcon rIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        rIcn.setIconSize(12);
        rIcn.setIconColor(Color.web(C_ACCENT));
        refresh.setGraphic(rIcn);
        refresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        refresh.setStyle("-fx-background-color: #ecfeff; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: #a5f3fc; -fx-border-radius: 8; -fx-border-width: 1;");
        refresh.setOnAction(e -> showShiftManagement());

        hdr.getChildren().addAll(title, sp, countLbl, refresh);

        HBox filterRow = new HBox(15);

        VBox dateBox = new VBox(6);
        Label dateLbl = new Label("LỌC THEO NGÀY");
        dateLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        dateLbl.setTextFill(Color.web(C_MUTED));
        DatePicker filterDate = new DatePicker();
        filterDate.setMaxWidth(Double.MAX_VALUE);
        filterDate.setPromptText("Chọn ngày (bỏ trống = tất cả)");
        filterDate.setStyle(DATE_PICKER_STYLE);
        dateBox.getChildren().addAll(dateLbl, filterDate);
        HBox.setHgrow(dateBox, Priority.ALWAYS);

        VBox typeBox = new VBox(6);
        Label typeLbl = new Label("LỌC THEO CA");
        typeLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        typeLbl.setTextFill(Color.web(C_MUTED));
        ComboBox<String> filterType = new ComboBox<>();
        filterType.getItems().addAll("Tất cả", "SANG", "CHIEU", "TOI");
        filterType.setValue("Tất cả");
        filterType.setMaxWidth(Double.MAX_VALUE);
        filterType.setStyle(COMBO_BOX_STYLE);
        typeBox.getChildren().addAll(typeLbl, filterType);
        HBox.setHgrow(typeBox, Priority.ALWAYS);

        VBox searchBox = new VBox(6);
        Label searchLbl = new Label("TÌM NHÂN VIÊN");
        searchLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        searchLbl.setTextFill(Color.web(C_MUTED));
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 username hoặc họ tên...");
        searchField.setPrefHeight(44);
        searchField.setStyle(INPUT_STYLE);
        searchBox.getChildren().addAll(searchLbl, searchField);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        filterRow.getChildren().addAll(dateBox, typeBox, searchBox);

        TableView<ShiftAdminRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ShiftAdminRow, String> c0 = new TableColumn<>("👤 Username");
        c0.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().username));
        c0.setPrefWidth(120);

        TableColumn<ShiftAdminRow, String> c1 = new TableColumn<>("📛 Họ và tên");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().fullName));
        c1.setPrefWidth(180);

        TableColumn<ShiftAdminRow, String> c2 = new TableColumn<>("📅 Ngày");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shiftDate));
        c2.setPrefWidth(130);

        TableColumn<ShiftAdminRow, String> c3 = new TableColumn<>("🕐 Ca");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shiftType));
        c3.setPrefWidth(110);
        c3.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if ("SANG".equals(item)) setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;");
                    else if ("CHIEU".equals(item)) setStyle("-fx-text-fill: " + C_BLUE + "; -fx-font-weight: bold;");
                    else if ("TOI".equals(item)) setStyle("-fx-text-fill: " + C_VIOLET + "; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<ShiftAdminRow, String> c4 = new TableColumn<>("📝 Ghi chú");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().note));
        c4.setPrefWidth(280);

        TableColumn<ShiftAdminRow, String> c5 = new TableColumn<>("Hành động");
        c5.setPrefWidth(120);
        c5.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                ShiftAdminRow row = getTableView().getItems().get(getIndex());

                Button delBtn = new Button("Xóa");
                delBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                delBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 12 5 12; -fx-border-color: #fecaca; -fx-border-radius: 6; -fx-border-width: 1;");
                delBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Xác nhận xóa");
                    confirm.setHeaderText("Xóa ca của " + row.fullName + " ngày " + row.shiftDate + "?");
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.OK) {
                            new Thread(() -> {
                                try {
                                    http.delete("/api/work-shifts/" + row.id);
                                    Thread.sleep(500);
                                    Platform.runLater(() -> showShiftManagement());
                                } catch (Exception ex) { }
                            }).start();
                        }
                    });
                });
                setGraphic(delBtn);
            }
        });

        table.getColumns().addAll(c0, c1, c2, c3, c4, c5);

        card.getChildren().addAll(hdr, filterRow, table);
        contentArea.getChildren().add(card);

        final ObservableList<ShiftAdminRow> allRows = FXCollections.observableArrayList();

        Runnable applyFilter = () -> {
            ObservableList<ShiftAdminRow> filtered = FXCollections.observableArrayList();
            String key = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String fType = filterType.getValue();
            java.time.LocalDate fDate = filterDate.getValue();

            for (ShiftAdminRow r : allRows) {
                if (fDate != null && !fDate.toString().equals(r.shiftDate)) continue;
                if (fType != null && !"Tất cả".equals(fType) && !fType.equals(r.shiftType)) continue;
                if (!key.isEmpty() &&
                    !r.username.toLowerCase().contains(key) &&
                    !r.fullName.toLowerCase().contains(key)) continue;
                filtered.add(r);
            }
            table.setItems(filtered);
            countLbl.setText("📊 " + filtered.size() + " / " + allRows.size() + " ca");
        };

        new Thread(() -> {
            try {
                String res = http.get("/api/admin/work-shifts");
                ObservableList<ShiftAdminRow> rows = parseAdminShifts(res);
                Platform.runLater(() -> {
                    allRows.setAll(rows);
                    applyFilter.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> countLbl.setText("❌ " + ex.getMessage()));
            }
        }).start();

        filterDate.valueProperty().addListener((o, ov, nv) -> applyFilter.run());
        filterType.valueProperty().addListener((o, ov, nv) -> applyFilter.run());
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter.run());
    }

    private ObservableList<ShiftAdminRow> parseAdminShifts(String json) {
        ObservableList<ShiftAdminRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        int aEnd = json.lastIndexOf(']');
        if (aStart < 0 || aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        if (arr.trim().isEmpty()) return rows;
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String u = extract(item, "username");
            String f = extract(item, "fullName");
            String d = extract(item, "shiftDate");
            String t = extract(item, "shiftType");
            String n = extract(item, "note");
            if (d == null || d.isEmpty()) continue;
            if (f == null || f.isEmpty()) f = u;
            int id = extractInt(item, "id");
            rows.add(new ShiftAdminRow(id, u, f, d, t, n));
        }
        return rows;
    }

        // ============================================================
    // QUẢN LÝ NHÂN VIÊN (Admin)
    // ============================================================
    public static class UserRow {
        public String username, fullName, role;
        public UserRow(String u, String f, String r) {
            username = u; fullName = f; role = r;
        }
    }

    private void showUserManagement() {
        pageTitle.setText("Quản lý nhân viên");
        pageSubtitle.setText("Thêm / Sửa / Xóa nhân viên");
        contentArea.getChildren().clear();

        VBox card = new VBox(18);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox hdr = new HBox(15);
        hdr.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.USERS_COG);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ACCENT));

        Label title = new Label("  Quản lý nhân viên");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("  Thêm NV");
        FontIcon addIcn = new FontIcon(FontAwesomeSolid.USER_PLUS);
        addIcn.setIconSize(12);
        addIcn.setIconColor(Color.web(C_GREEN));
        addBtn.setGraphic(addIcn);
        addBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        addBtn.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: " + C_GREEN + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: #a7f3d0; -fx-border-radius: 8; -fx-border-width: 1;");
        addBtn.setOnAction(e -> showAddUserDialog());

        Button refresh = new Button("  Làm mới");
        FontIcon rIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        rIcn.setIconSize(12);
        rIcn.setIconColor(Color.web(C_ACCENT));
        refresh.setGraphic(rIcn);
        refresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        refresh.setStyle("-fx-background-color: #ecfeff; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: #a5f3fc; -fx-border-radius: 8; -fx-border-width: 1;");
        refresh.setOnAction(e -> showUserManagement());

        hdr.getChildren().addAll(title, sp, addBtn, refresh);

        TableView<UserRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<UserRow, String> c1 = new TableColumn<>("Username");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().username));
        c1.setPrefWidth(200);
        TableColumn<UserRow, String> c2 = new TableColumn<>("Họ và tên");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().fullName));
        c2.setPrefWidth(300);
        TableColumn<UserRow, String> c3 = new TableColumn<>("Vai trò");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().role));
        c3.setPrefWidth(150);
        c3.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if ("ADMIN".equals(item)) setStyle("-fx-text-fill: " + C_RED + "; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: " + C_BLUE + "; -fx-font-weight: bold;");
                }
            }
        });
        TableColumn<UserRow, String> c4 = new TableColumn<>("Hành động");
        c4.setPrefWidth(250);
        c4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                UserRow row = getTableView().getItems().get(getIndex());

                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_LEFT);

                Button editBtn = new Button("Sửa");
                editBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                editBtn.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: " + C_BLUE + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 12 5 12; -fx-border-color: #bfdbfe; -fx-border-radius: 6; -fx-border-width: 1;");
                editBtn.setOnAction(e -> showEditUserDialog(row));

                Button delBtn = new Button("Xóa");
                delBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                delBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 12 5 12; -fx-border-color: #fecaca; -fx-border-radius: 6; -fx-border-width: 1;");
                delBtn.setOnAction(e -> deleteUser(row.username));

                actions.getChildren().addAll(editBtn, delBtn);
                setGraphic(actions);
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4);
        card.getChildren().addAll(hdr, table);
        contentArea.getChildren().add(card);

        new Thread(() -> {
            try {
                String res = http.get("/api/admin/users");
                ObservableList<UserRow> rows = parseUsers(res);
                Platform.runLater(() -> table.setItems(rows));
            } catch (Exception ex) { }
        }).start();
    }

    private ObservableList<UserRow> parseUsers(String json) {
        ObservableList<UserRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        int aEnd = json.lastIndexOf(']');
        if (aStart < 0 || aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String u = extract(item, "username");
            String f = extract(item, "fullName");
            String r = extract(item, "role");
            if (u == null || u.isEmpty()) continue;
            rows.add(new UserRow(u, f, r));
        }
        return rows;
    }

    private void showAddUserDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Thêm nhân viên");

        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + C_CARD + ";");

        Label title = new Label("THÊM NHÂN VIÊN MỚI");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        TextField uField = new TextField();
        uField.setPromptText("Username");
        uField.setPrefHeight(44);
        uField.setStyle(INPUT_STYLE);

        PasswordField pField = new PasswordField();
        pField.setPromptText("Mật khẩu");
        pField.setPrefHeight(44);
        pField.setStyle(INPUT_STYLE);

        TextField nField = new TextField();
        nField.setPromptText("Họ và tên");
        nField.setPrefHeight(44);
        nField.setStyle(INPUT_STYLE);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("USER", "ADMIN");
        roleBox.setValue("USER");
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle(COMBO_BOX_STYLE);

        Label err = new Label("");
        err.setTextFill(Color.web(C_RED));
        err.setWrapText(true);

        Button submit = new Button("THÊM");
        submit.setPrefHeight(46);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        submit.setStyle("-fx-background-color: " + C_GREEN + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

        submit.setOnAction(e -> {
            if (uField.getText().isEmpty() || pField.getText().isEmpty() || nField.getText().isEmpty()) {
                err.setTextFill(Color.web(C_RED));
                err.setText("Vui lòng nhập đầy đủ");
                return;
            }
            new Thread(() -> {
                try {
                    String body = "{\"username\":\"" + uField.getText() + "\",\"password\":\"" + pField.getText() + "\",\"fullName\":\"" + nField.getText() + "\",\"role\":\"" + roleBox.getValue() + "\"}";
                    String res = http.post("/api/admin/user/add", body);
                    String msg = JsonUtil.getString(res, "msg");
                    String e2 = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> {
                        if (msg != null) { dialog.close(); showUserManagement(); }
                        else { err.setTextFill(Color.web(C_RED)); err.setText("❌ " + (e2 != null ? e2 : res)); }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> err.setText("❌ " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(title, uField, pField, nField, roleBox, submit, err);

        Scene scene = new Scene(root, 400, 440);
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));
        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditUserDialog(UserRow row) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Sửa nhân viên");

        VBox root = new VBox(14);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + C_CARD + ";");

        Label title = new Label("SỬA: " + row.username);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        TextField nField = new TextField(row.fullName);
        nField.setPrefHeight(44);
        nField.setStyle(INPUT_STYLE);

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("USER", "ADMIN");
        roleBox.setValue(row.role);
        roleBox.setMaxWidth(Double.MAX_VALUE);
        roleBox.setStyle(COMBO_BOX_STYLE);

        Label err = new Label("");
        err.setTextFill(Color.web(C_RED));

        Button submit = new Button("LƯU");
        submit.setPrefHeight(46);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        submit.setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

        submit.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String body = "{\"username\":\"" + row.username + "\",\"fullName\":\"" + nField.getText() + "\",\"role\":\"" + roleBox.getValue() + "\"}";
                    String res = http.post("/api/admin/user/update", body);
                    String msg = JsonUtil.getString(res, "msg");
                    Platform.runLater(() -> {
                        if (msg != null) { dialog.close(); showUserManagement(); }
                        else { err.setText("❌ Lỗi cập nhật"); }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> err.setText("❌ " + ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(title, nField, roleBox, submit, err);

        Scene scene = new Scene(root, 400, 340);
        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(getGlobalCss(), java.nio.charset.StandardCharsets.UTF_8));
        dialog.setScene(scene);
        dialog.show();
    }

    private void deleteUser(String username) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa nhân viên: " + username + "?");
        confirm.setContentText("Hành động này không thể hoàn tác!");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        String body = "{\"username\":\"" + username + "\"}";
                        http.post("/api/admin/user/delete", body);
                        Thread.sleep(500);
                        Platform.runLater(this::showUserManagement);
                    } catch (Exception ex) { }
                }).start();
            }
        });
    }

    // ============================================================
    // DUYỆT ĐƠN NGHỈ (Admin)
    // ============================================================
    private void showLeaveManagement() {
        pageTitle.setText("Duyệt đơn nghỉ phép");
        pageSubtitle.setText("Danh sách đơn xin nghỉ của nhân viên");
        contentArea.getChildren().clear();

        VBox card = new VBox(18);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox hdr = new HBox(15);
        hdr.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.CLIPBOARD_LIST);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_ORANGE));
        Label title = new Label("  Đơn xin nghỉ phép");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button refresh = new Button("  Làm mới");
        FontIcon rIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        rIcn.setIconSize(12);
        rIcn.setIconColor(Color.web(C_ACCENT));
        refresh.setGraphic(rIcn);
        refresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        refresh.setStyle("-fx-background-color: #ecfeff; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: #a5f3fc; -fx-border-radius: 8; -fx-border-width: 1;");
        refresh.setOnAction(e -> showLeaveManagement());

        hdr.getChildren().addAll(title, sp, refresh);

        TableView<LeaveRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(550);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<LeaveRow, String> c1 = new TableColumn<>("Từ ngày");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().startDate));
        c1.setPrefWidth(130);

        TableColumn<LeaveRow, String> c2 = new TableColumn<>("Đến ngày");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().endDate));
        c2.setPrefWidth(130);

        TableColumn<LeaveRow, String> c3 = new TableColumn<>("Lý do");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reason));
        c3.setPrefWidth(280);

        TableColumn<LeaveRow, String> c4 = new TableColumn<>("Trạng thái");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        c4.setPrefWidth(140);
        c4.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    if ("PENDING".equals(item)) { setText("⏳ Chờ duyệt"); setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;"); }
                    else if ("APPROVED".equals(item)) { setText("✅ Đã duyệt"); setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;"); }
                    else if ("REJECTED".equals(item)) { setText("❌ Từ chối"); setStyle("-fx-text-fill: " + C_RED + "; -fx-font-weight: bold;"); }
                }
            }
        });

        TableColumn<LeaveRow, String> c5 = new TableColumn<>("Hành động");
        c5.setPrefWidth(240);
        c5.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                LeaveRow row = getTableView().getItems().get(getIndex());
                if (!"PENDING".equals(row.status)) { setGraphic(null); return; }

                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_LEFT);

                Button approveBtn = new Button("✅ Duyệt");
                approveBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                approveBtn.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: " + C_GREEN + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14 6 14; -fx-border-color: #a7f3d0; -fx-border-radius: 6; -fx-border-width: 1;");
                approveBtn.setOnAction(e -> updateLeaveStatus(row.id, "APPROVED"));

                Button rejectBtn = new Button("❌ Từ chối");
                rejectBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                rejectBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: " + C_RED + "; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14 6 14; -fx-border-color: #fecaca; -fx-border-radius: 6; -fx-border-width: 1;");
                rejectBtn.setOnAction(e -> updateLeaveStatus(row.id, "REJECTED"));

                actions.getChildren().addAll(approveBtn, rejectBtn);
                setGraphic(actions);
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4, c5);
        card.getChildren().addAll(hdr, table);
        contentArea.getChildren().add(card);

        new Thread(() -> {
            try {
                String res = http.get("/api/admin/leaves");
                ObservableList<LeaveRow> rows = parseLeaves(res);
                Platform.runLater(() -> table.setItems(rows));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void updateLeaveStatus(int id, String status) {
        new Thread(() -> {
            try {
                String body = "{\"id\":" + id + ",\"status\":\"" + status + "\"}";
                http.post("/api/admin/leave/update", body);
                Thread.sleep(800);
                Platform.runLater(this::showLeaveManagement);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // ============================================================
    // LỊCH SỬ
    // ============================================================
    public static class HistRow {
        public String date, in, out, total, status;
        public HistRow(String d, String i, String o, String t, String s) {
            date = d; in = i; out = o; total = t; status = s;
        }
    }

    private void showHistory() {
        pageTitle.setText("Lịch sử chấm công");
        pageSubtitle.setText("Xem + lọc + phân trang lịch sử chấm công");
        contentArea.getChildren().clear();

        VBox main = new VBox(20);

        VBox filterCard = new VBox(15);
        filterCard.setPadding(new Insets(24));
        filterCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox filterTitle = new HBox(10);
        filterTitle.setAlignment(Pos.CENTER_LEFT);
        FontIcon fIcn = new FontIcon(FontAwesomeSolid.FILTER);
        fIcn.setIconSize(18);
        fIcn.setIconColor(Color.web(C_ACCENT));
        Label fTitle = new Label("  Bộ lọc");
        fTitle.setGraphic(fIcn);
        fTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        fTitle.setTextFill(Color.web(C_TEXT));
        filterTitle.getChildren().add(fTitle);

        HBox dateFilter = new HBox(15);

        VBox fromBox = new VBox(6);
        Label fromLbl = new Label("TỪ NGÀY");
        fromLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        fromLbl.setTextFill(Color.web(C_MUTED));
        DatePicker fromDate = new DatePicker(java.time.LocalDate.now().minusDays(30));
        fromDate.setMaxWidth(Double.MAX_VALUE);
        fromDate.setStyle(DATE_PICKER_STYLE);
        fromBox.getChildren().addAll(fromLbl, fromDate);
        HBox.setHgrow(fromBox, Priority.ALWAYS);

        VBox toBox = new VBox(6);
        Label toLbl = new Label("ĐẾN NGÀY");
        toLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        toLbl.setTextFill(Color.web(C_MUTED));
        DatePicker toDate = new DatePicker(java.time.LocalDate.now());
        toDate.setMaxWidth(Double.MAX_VALUE);
        toDate.setStyle(DATE_PICKER_STYLE);
        toBox.getChildren().addAll(toLbl, toDate);
        HBox.setHgrow(toBox, Priority.ALWAYS);

        VBox limitBox = new VBox(6);
        Label limitLbl = new Label("SỐ DÒNG/TRANG");
        limitLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        limitLbl.setTextFill(Color.web(C_MUTED));
        ComboBox<Integer> limitBoxCb = new ComboBox<>();
        limitBoxCb.getItems().addAll(5, 10, 20, 50);
        limitBoxCb.setValue(pageLimit);
        limitBoxCb.setMaxWidth(Double.MAX_VALUE);
        limitBoxCb.setStyle(COMBO_BOX_STYLE);
        limitBox.getChildren().addAll(limitLbl, limitBoxCb);
        limitBox.setPrefWidth(140);

        dateFilter.getChildren().addAll(fromBox, toBox, limitBox);

        HBox filterBtnRow = new HBox(10);
        Button applyBtn = new Button("  Áp dụng");
        FontIcon applyIcn = new FontIcon(FontAwesomeSolid.SEARCH);
        applyIcn.setIconSize(12);
        applyIcn.setIconColor(Color.WHITE);
        applyBtn.setGraphic(applyIcn);
        applyBtn.setStyle("-fx-background-color: " + C_ACCENT + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20 10 20; -fx-font-weight: bold;");

        Button resetBtn = new Button("  Reset");
        FontIcon resetIcn = new FontIcon(FontAwesomeSolid.REDO);
        resetIcn.setIconSize(12);
        resetIcn.setIconColor(Color.web(C_ORANGE));
        resetBtn.setGraphic(resetIcn);
        resetBtn.setStyle("-fx-background-color: #fffbeb; -fx-text-fill: " + C_ORANGE + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20 10 20; -fx-font-weight: bold; -fx-border-color: #fde68a; -fx-border-radius: 8; -fx-border-width: 1;");

        Button exportBtn = new Button("  Export CSV");
        FontIcon exportIcn = new FontIcon(FontAwesomeSolid.FILE_CSV);
        exportIcn.setIconSize(12);
        exportIcn.setIconColor(Color.web(C_GREEN));
        exportBtn.setGraphic(exportIcn);
        exportBtn.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: " + C_GREEN + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 10 20 10 20; -fx-font-weight: bold; -fx-border-color: #a7f3d0; -fx-border-radius: 8; -fx-border-width: 1;");

        Region fsp = new Region();
        HBox.setHgrow(fsp, Priority.ALWAYS);

        filterBtnRow.getChildren().addAll(applyBtn, resetBtn, fsp, exportBtn);

        filterCard.getChildren().addAll(filterTitle, dateFilter, filterBtnRow);

        VBox tableCard = new VBox(15);
        tableCard.setPadding(new Insets(24));
        tableCard.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label infoLbl = new Label("⏳ Đang tải...");
        infoLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        infoLbl.setTextFill(Color.web(C_ACCENT));

        TableView<HistRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<HistRow, String> c1 = new TableColumn<>("📅 Ngày");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().date));
        c1.setPrefWidth(140);
        TableColumn<HistRow, String> c2 = new TableColumn<>("⏰ Giờ vào");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().in));
        c2.setPrefWidth(120);
        TableColumn<HistRow, String> c3 = new TableColumn<>("🕐 Giờ ra");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().out));
        c3.setPrefWidth(120);
        TableColumn<HistRow, String> c4 = new TableColumn<>("⏱️ Tổng giờ");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().total));
        c4.setPrefWidth(160);
        TableColumn<HistRow, String> c5 = new TableColumn<>("📊 Trạng thái");
        c5.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        c5.setPrefWidth(180);
        c5.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if (item.contains("Đủ giờ")) setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;");
                    else if (item.contains("Chưa")) setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;");
                    else if (item.contains("muộn")) setStyle("-fx-text-fill: " + C_RED + "; -fx-font-weight: bold;");
                }
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4, c5);

        HBox pageBar = new HBox(10);
        pageBar.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("◀ Trước");
        prevBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        prevBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_TEXT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8; -fx-border-width: 1;");

        Label pageLbl = new Label("Trang 1 / 1");
        pageLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pageLbl.setTextFill(Color.web(C_TEXT));

        Button nextBtn = new Button("Sau ▶");
        nextBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nextBtn.setStyle("-fx-background-color: " + C_CARD2 + "; -fx-text-fill: " + C_TEXT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8; -fx-border-width: 1;");

        pageBar.getChildren().addAll(prevBtn, pageLbl, nextBtn);

        tableCard.getChildren().addAll(infoLbl, table, pageBar);
        main.getChildren().addAll(filterCard, tableCard);
        contentArea.getChildren().add(main);

        final int[] totalPages = {1};

        Runnable loadData = () -> {
            new Thread(() -> {
                try {
                    String path = "/api/history?page=" + currentPage + "&limit=" + pageLimit;

                    if (fromDate.getValue() != null)
                        path += "&from=" + fromDate.getValue();
                    if (toDate.getValue() != null)
                        path += "&to=" + toDate.getValue();

                    String res = http.get(path);

                    List<HistRow> rows = parseHistory(res);
                    String totalStr = JsonUtil.getString(res, "total");
                    String totalPagesStr = JsonUtil.getString(res, "totalPages");

                    int total = 0, totalPg = 1;
                    try {
                        if (totalStr != null) total = Integer.parseInt(extractNumber(res, "total"));
                        if (totalPagesStr != null) totalPg = Integer.parseInt(extractNumber(res, "totalPages"));
                    } catch (Exception ex) { }

                    final int totalFinal = total;
                    final int totalPagesFinal = totalPg;
                    totalPages[0] = totalPagesFinal;

                    Platform.runLater(() -> {
                        table.setItems(FXCollections.observableArrayList(rows));
                        infoLbl.setText("📊 Tổng: " + totalFinal + " bản ghi • Trang " + currentPage + "/" + totalPagesFinal);
                        pageLbl.setText("Trang " + currentPage + " / " + totalPagesFinal);
                        prevBtn.setDisable(currentPage <= 1);
                        nextBtn.setDisable(currentPage >= totalPagesFinal);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> infoLbl.setText("❌ Lỗi: " + ex.getMessage()));
                }
            }).start();
        };

        applyBtn.setOnAction(e -> {
            currentPage = 1;
            pageLimit = limitBoxCb.getValue();
            loadData.run();
        });

        resetBtn.setOnAction(e -> {
            fromDate.setValue(java.time.LocalDate.now().minusDays(30));
            toDate.setValue(java.time.LocalDate.now());
            limitBoxCb.setValue(10);
            currentPage = 1;
            pageLimit = 10;
            loadData.run();
        });

        prevBtn.setOnAction(e -> {
            if (currentPage > 1) { currentPage--; loadData.run(); }
        });

        nextBtn.setOnAction(e -> {
            if (currentPage < totalPages[0]) { currentPage++; loadData.run(); }
        });

        exportBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String path = "/api/export/csv";
                    if (fromDate.getValue() != null) path += "?from=" + fromDate.getValue();
                    if (toDate.getValue() != null)
                        path += (path.contains("?") ? "&" : "?") + "to=" + toDate.getValue();

                    String csv = http.get(path);
                    String filename = "chamcong_" + java.time.LocalDate.now() + ".csv";
                    java.nio.file.Files.write(java.nio.file.Paths.get(filename),
                        csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    Platform.runLater(() -> log("✅ Đã export: " + filename));
                } catch (Exception ex) {
                    Platform.runLater(() -> log("❌ Lỗi export: " + ex.getMessage()));
                }
            }).start();
        });

        loadData.run();
    }

    private String extractNumber(String json, String key) {
        if (json == null) return "0";
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return "0";
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return "0";
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return "0";
        return json.substring(start, end);
    }

    private void showStats() {
        pageTitle.setText("Thống kê");
        pageSubtitle.setText("Số liệu chấm công của bạn");
        contentArea.getChildren().clear();

        VBox card = new VBox(24);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        Label title = new Label("📊 Thống kê cá nhân");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(C_TEXT));

        HBox row = new HBox(20);
        row.setMaxWidth(Double.MAX_VALUE);

        VBox s1 = statBig(FontAwesomeSolid.CALENDAR_DAY, "Tổng ngày", "...", C_BLUE);
        VBox s2 = statBig(FontAwesomeSolid.CHECK_CIRCLE, "Check-in", "...", C_GREEN);
        VBox s3 = statBig(FontAwesomeSolid.CLOCK, "Check-out", "...", C_ORANGE);
        VBox s4 = statBig(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "Đi muộn", "...", C_RED);

        HBox.setHgrow(s1, Priority.ALWAYS);
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox.setHgrow(s3, Priority.ALWAYS);
        HBox.setHgrow(s4, Priority.ALWAYS);

        row.getChildren().addAll(s1, s2, s3, s4);
        card.getChildren().addAll(title, row);
        contentArea.getChildren().add(card);

        new Thread(() -> {
            try {
                String res = http.get("/api/history");
                ObservableList<HistRow> rows = parseHistory(res);
                Platform.runLater(() -> {
                    int totalDays = rows.size(), checkins = 0, checkouts = 0, late = 0;
                    for (HistRow r : rows) {
                        if (!"—".equals(r.in)) checkins++;
                        if (!"—".equals(r.out)) checkouts++;
                        if (r.status.contains("muộn")) late++;
                    }
                    row.getChildren().clear();
                    VBox a = statBig(FontAwesomeSolid.CALENDAR_DAY, "Tổng ngày", String.valueOf(totalDays), C_BLUE);
                    VBox b = statBig(FontAwesomeSolid.CHECK_CIRCLE, "Check-in", String.valueOf(checkins), C_GREEN);
                    VBox c = statBig(FontAwesomeSolid.CLOCK, "Check-out", String.valueOf(checkouts), C_ORANGE);
                    VBox d = statBig(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "Đi muộn", String.valueOf(late), C_RED);
                    HBox.setHgrow(a, Priority.ALWAYS);
                    HBox.setHgrow(b, Priority.ALWAYS);
                    HBox.setHgrow(c, Priority.ALWAYS);
                    HBox.setHgrow(d, Priority.ALWAYS);
                    row.getChildren().addAll(a, b, c, d);
                });
            } catch (Exception ex) { }
        }).start();
    }

    private VBox statBig(FontAwesomeSolid icon, String title, String value, String color) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(26));
        card.setMinWidth(220);
        card.setPrefHeight(140);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: " + C_BG + "; -fx-background-radius: 12; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 12; -fx-border-width: 1;");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(38, 38);
        iconCircle.setMaxSize(38, 38);
        iconCircle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 19;");
        FontIcon icn = new FontIcon(icon);
        icn.setIconSize(18);
        icn.setIconColor(Color.web(color));
        iconCircle.getChildren().add(icn);

        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13.5));
        t.setTextFill(Color.web(C_MUTED));
        top.getChildren().addAll(iconCircle, t);

        Label v = new Label(value);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        v.setTextFill(Color.web(color));

        card.getChildren().addAll(top, v);
        return card;
    }

    public static class AdminRow {
        public String username, fullName, date, in, out, total, status;
        public AdminRow(String u, String f, String d, String i, String o, String t, String s) {
            username = u; fullName = f; date = d; in = i; out = o; total = t; status = s;
        }
    }

    private void showAdmin() {
        pageTitle.setText("Quản lý chấm công");
        pageSubtitle.setText("Xem chấm công của tất cả nhân viên");
        contentArea.getChildren().clear();

        VBox card = new VBox(20);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-background-radius: 16; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 16; -fx-border-width: 1; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 12, 0, 0, 4);");

        HBox hdr = new HBox(15);
        hdr.setAlignment(Pos.CENTER_LEFT);

        FontIcon icn = new FontIcon(FontAwesomeSolid.CROWN);
        icn.setIconSize(20);
        icn.setIconColor(Color.web(C_AMBER));
        Label title = new Label("  Bảng chấm công toàn công ty");
        title.setGraphic(icn);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label countLbl = new Label("⏳ Đang tải...");
        countLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        countLbl.setTextFill(Color.web(C_ACCENT));
        countLbl.setStyle("-fx-background-color: #ecfeff; -fx-background-radius: 20; -fx-padding: 8 18 8 18; -fx-border-color: #a5f3fc; -fx-border-radius: 20; -fx-border-width: 1;");

        Button refresh = new Button("  Làm mới");
        FontIcon rIcn = new FontIcon(FontAwesomeSolid.SYNC_ALT);
        rIcn.setIconSize(12);
        rIcn.setIconColor(Color.web(C_ACCENT));
        refresh.setGraphic(rIcn);
        refresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        refresh.setStyle("-fx-background-color: #ecfeff; -fx-text-fill: " + C_ACCENT + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-border-color: #a5f3fc; -fx-border-radius: 8; -fx-border-width: 1;");
        refresh.setOnAction(e -> showAdmin());

        hdr.getChildren().addAll(title, sp, countLbl, refresh);

        TextField search = new TextField();
        search.setPromptText("🔍  Tìm theo username hoặc họ tên...");
        search.setPrefHeight(44);
        search.setStyle(INPUT_STYLE);

        TableView<AdminRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(540);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AdminRow, String> c0 = new TableColumn<>("👤 Username");
        c0.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().username));
        c0.setPrefWidth(120);
        TableColumn<AdminRow, String> c1 = new TableColumn<>("📛 Họ và tên");
        c1.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().fullName));
        c1.setPrefWidth(210);
        TableColumn<AdminRow, String> c2 = new TableColumn<>("📅 Ngày");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().date));
        c2.setPrefWidth(120);
        TableColumn<AdminRow, String> c3 = new TableColumn<>("⏰ Giờ vào");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().in));
        c3.setPrefWidth(110);
        TableColumn<AdminRow, String> c4 = new TableColumn<>("🕐 Giờ ra");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().out));
        c4.setPrefWidth(110);
        TableColumn<AdminRow, String> c5 = new TableColumn<>("⏱️ Tổng giờ");
        c5.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().total));
        c5.setPrefWidth(140);
        TableColumn<AdminRow, String> c6 = new TableColumn<>("📊 Trạng thái");
        c6.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().status));
        c6.setPrefWidth(170);
        c6.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    if (item.contains("Đủ giờ")) setStyle("-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold;");
                    else if (item.contains("Chưa")) setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;");
                    else if (item.contains("muộn")) setStyle("-fx-text-fill: " + C_RED + "; -fx-font-weight: bold;");
                    else if (item.contains("Về sớm")) setStyle("-fx-text-fill: " + C_ORANGE + "; -fx-font-weight: bold;");
                }
            }
        });

        table.getColumns().addAll(c0, c1, c2, c3, c4, c5, c6);
        card.getChildren().addAll(hdr, search, table);
        contentArea.getChildren().add(card);

        new Thread(() -> {
            try {
                String res = http.get("/api/admin/all");
                ObservableList<AdminRow> rows = parseAdmin(res);
                Platform.runLater(() -> {
                    table.setItems(rows);
                    countLbl.setText("📊 " + rows.size() + " bản ghi");
                    search.textProperty().addListener((obs, old, val) -> {
                        if (val == null || val.isEmpty()) table.setItems(rows);
                        else {
                            String key = val.toLowerCase();
                            ObservableList<AdminRow> filtered = FXCollections.observableArrayList();
                            for (AdminRow r : rows)
                                if (r.username.toLowerCase().contains(key) || r.fullName.toLowerCase().contains(key))
                                    filtered.add(r);
                            table.setItems(filtered);
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> countLbl.setText("❌ " + ex.getMessage()));
            }
        }).start();
    }

    // ============================================================
    // PARSE
    // ============================================================
    private ObservableList<HistRow> parseHistory(String json) {
        ObservableList<HistRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        int aEnd = json.lastIndexOf(']');
        if (aStart < 0 || aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String d = extract(item, "date");
            String i = extract(item, "checkIn");
            String o = extract(item, "checkOut");
            if (d == null || d.isEmpty()) continue;
            if (i == null || i.isEmpty()) i = "—";
            if (o == null || o.isEmpty()) o = "—";
            String total = "—";
            String status = "Đã chấm công";
            if (!"—".equals(i) && !"—".equals(o)) {
                try {
                    String[] ip = i.split(":");
                    String[] op = o.split(":");
                    int iMin = Integer.parseInt(ip[0]) * 60 + Integer.parseInt(ip[1]);
                    int oMin = Integer.parseInt(op[0]) * 60 + Integer.parseInt(op[1]);
                    int diff = oMin - iMin;
                    total = String.format("%d giờ %02d phút", diff / 60, diff % 60);
                    if (iMin > 8 * 60) status = "⚠️ Đi muộn";
                    else if (oMin < 17 * 60) status = "⚠️ Về sớm";
                    else status = "✅ Đủ giờ";
                } catch (Exception e) { total = "—"; }
            } else if (!"—".equals(i)) {
                status = "⏳ Chưa check-out";
            } else {
                status = "❌ Chưa chấm công";
            }
            rows.add(new HistRow(d, i, o, total, status));
        }
        return rows;
    }

    private ObservableList<AdminRow> parseAdmin(String json) {
        ObservableList<AdminRow> rows = FXCollections.observableArrayList();
        if (json == null || json.isEmpty()) return rows;
        int dStart = json.indexOf("\"data\"");
        if (dStart < 0) return rows;
        int aStart = json.indexOf('[', dStart);
        int aEnd = json.lastIndexOf(']');
        if (aStart < 0 || aEnd < 0) return rows;
        String arr = json.substring(aStart + 1, aEnd);
        String[] items = arr.split("\\},\\s*\\{");
        for (String item : items) {
            item = item.replace("{", "").replace("}", "");
            String u = extract(item, "username");
            String f = extract(item, "fullName");
            String d = extract(item, "date");
            String i = extract(item, "checkIn");
            String o = extract(item, "checkOut");
            if (d == null || d.isEmpty()) continue;
            if (f == null || f.isEmpty()) f = u;
            if (i == null || i.isEmpty()) i = "—";
            if (o == null || o.isEmpty()) o = "—";
            String total = "—";
            String status = "Đã chấm công";
            if (!"—".equals(i) && !"—".equals(o)) {
                try {
                    String[] ip = i.split(":");
                    String[] op = o.split(":");
                    int iMin = Integer.parseInt(ip[0]) * 60 + Integer.parseInt(ip[1]);
                    int oMin = Integer.parseInt(op[0]) * 60 + Integer.parseInt(op[1]);
                    int diff = oMin - iMin;
                    total = String.format("%d giờ %02d phút", diff / 60, diff % 60);
                    if (iMin > 8 * 60) status = "⚠️ Đi muộn";
                    else if (oMin < 17 * 60) status = "⚠️ Về sớm";
                    else status = "✅ Đủ giờ";
                } catch (Exception e) { total = "—"; }
            } else if (!"—".equals(i)) {
                status = "⏳ Chưa check-out";
            } else {
                status = "❌ Chưa chấm công";
            }
            rows.add(new AdminRow(u, f, d, i, o, total, status));
        }
        return rows;
    }

    private String extract(String json, String key) {
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return "";
        int end = json.indexOf('"', start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    private int extractInt(String json, String key) {
        if (json == null) return 0;
        String pattern = "\"" + key + "\"";
        int i = json.indexOf(pattern);
        if (i < 0) return 0;
        int colon = json.indexOf(':', i + pattern.length());
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return 0;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (Exception e) { return 0; }
    }

    // ============================================================
    // UTILS
    // ============================================================
    private String getTodayString() {
        java.time.LocalDate now = java.time.LocalDate.now();
        String[] days = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
        String d = days[now.getDayOfWeek().getValue() - 1];
        return "📅  " + d + ", ngày " + now.getDayOfMonth() + "/" + now.getMonthValue() + "/" + now.getYear();
    }

    private String getTodayShort() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return now.getDayOfMonth() + "/" + now.getMonthValue();
    }

    private void startClock() {
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                Platform.runLater(() -> {
                    java.time.LocalTime now = java.time.LocalTime.now();
                    if (clockLabel != null)
                        clockLabel.setText(String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void doLogin() {
        new Thread(() -> {
            try {
                String body = "{\"username\":\"" + userField.getText() +
                              "\",\"password\":\"" + passField.getText() + "\"}";
                String res = http.post("/api/login", body);
                String token = JsonUtil.getString(res, "token");
                if (token != null) {
                    http.setToken(token);
                    String r = JsonUtil.getString(res, "role");
                    String name = JsonUtil.getString(res, "fullName");
                    Platform.runLater(() -> showMain(name, r));
                } else {
                    String err = JsonUtil.getString(res, "error");
                    Platform.runLater(() -> loginError.setText("❌ " + (err != null ? err : "Đăng nhập thất bại")));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> loginError.setText("❌ Lỗi kết nối: " + ex.getMessage()));
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
