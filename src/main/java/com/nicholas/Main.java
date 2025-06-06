package com.nicholas;

import com.nicholas.dao.GameGoalDao;
import com.nicholas.dao.SessionDao;
import com.nicholas.model.GameGoal;
import com.nicholas.model.Session;
import com.nicholas.service.RawgClient;
import com.nicholas.ui.AddSessionDialog;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.data.category.DefaultCategoryDataset;
import javafx.scene.image.Image;
import javafx.scene.control.TextArea;
import java.sql.SQLException;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class Main extends Application {
    private BorderPane root;
    private GameGoalDao dao;
    private SessionDao sessionDao;
    private ObservableList<GameGoal> goals;
    private ListView<GameGoal> listView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws SQLException {
        root = new BorderPane();

        dao = new GameGoalDao();
        sessionDao = new SessionDao();
        goals = FXCollections.observableArrayList(dao.listAll());

        listView = new ListView<>(goals);
        listView.getStyleClass().add("dark-list");
        listView.setFixedCellSize(60);
        listView.setCellFactory(lv -> new GoalCell(sessionDao));
        enableDragDrop(listView);
        listView.setPrefWidth(320); // or bigger, depends what you wants


        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldGame, newGame) -> {
            if (newGame != null) {
                Node chart = createProgressChart(newGame);
                BorderPane.setMargin(chart, new Insets(10));

                ScrollPane scrollPane = new ScrollPane(createDetailPane(newGame));
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setStyle("-fx-background: #121212;");
                scrollPane.setPadding(new Insets(10));

                VBox.setVgrow(scrollPane, Priority.ALWAYS);

                root.setRight(scrollPane);
            } else {
                root.setCenter(createChart());
                root.setRight(null);
            }
        });

        ChartViewer chartPane = createChart();
        HBox controls = createControls();
        HBox summary = createSummary();
        summary.getStyleClass().add("summary");

        root.setTop(summary);
        BorderPane.setMargin(summary, new Insets(10));
        root.setLeft(listView);
        root.setCenter(chartPane);
        root.setBottom(controls);

        BorderPane.setMargin(listView, new Insets(10));
        BorderPane.setMargin(chartPane, new Insets(10));
        BorderPane.setMargin(controls, new Insets(10));

        // WRAP root IN A ScrollPane to prevent any general clipping
        ScrollPane rootScroll = new ScrollPane(root);
        rootScroll.setFitToWidth(true);
        rootScroll.setFitToHeight(true);
        rootScroll.setStyle("-fx-background: #121212;");

        Scene scene = new Scene(rootScroll, 1000, 700); // larger initial size

        scene.getStylesheets().add(getClass()
                .getResource("/style.css")
                .toExternalForm());

        stage.setTitle("Game Dashboard");
        stage.setScene(scene);
        stage.show();
    }


    private HBox createSummary() {
        int totalGames = goals.size();

        double totalEstimated = goals.stream()
                .mapToDouble(GameGoal::getEstimatedHours)
                .sum();

        double totalPlayed = goals.stream()
                .mapToDouble(g -> {
                    try {
                        return sessionDao.listByGame(g.getId())
                                .stream()
                                .mapToDouble(Session::getHoursPlayed)
                                .sum();
                    } catch (SQLException e) {
                        return 0;
                    }
                })
                .sum();

        double totalRemainingDays = goals.stream()
                .mapToDouble(g -> {
                    double played = 0;
                    try {
                        played = sessionDao.listByGame(g.getId())
                                .stream()
                                .mapToDouble(Session::getHoursPlayed)
                                .sum();
                    } catch (SQLException ignored) {}
                    double remH = Math.max(0, g.getEstimatedHours() - played);
                    return g.getHoursPerDay() > 0 ? remH / g.getHoursPerDay() : 0;
                })
                .sum();

        VBox gamesBox = new VBox(
                new Label("Games:"), new Label(String.valueOf(totalGames))
        );
        VBox estBox = new VBox(
                new Label("Estimaded:"), new Label(String.format("%.1f h", totalEstimated))
        );
        VBox playBox = new VBox(
                new Label("Played:"), new Label(String.format("%.1f h", totalPlayed))
        );
        VBox daysBox = new VBox(
                new Label("Remaining Days:"), new Label(String.format("%.1f", totalRemainingDays))
        );

        // Aplica classes CSS
        for (VBox box : List.of(gamesBox, estBox, playBox, daysBox)) {
            box.setAlignment(Pos.CENTER);
            box.getChildren().get(0).getStyleClass().add("stat-label");
            box.getChildren().get(1).getStyleClass().add("stat-value");
        }

        HBox summaryBox = new HBox(40, gamesBox, estBox, playBox, daysBox);
        summaryBox.setAlignment(Pos.CENTER);
        summaryBox.getStyleClass().add("top-bar");

        return summaryBox;
    }



    private HBox createControls() {
        FontIcon plus = new FontIcon(FontAwesome.PLUS_CIRCLE);
        plus.setIconSize(16);
        plus.setIconColor(Color.web("#e0e0e0"));
        Button addBtn = new Button("Add Goal", plus);
        addBtn.setOnAction(e -> {
            try {
                // 1) Título
                TextInputDialog titleDlg = new TextInputDialog();
                titleDlg.setTitle("New Game");
                titleDlg.setHeaderText(null);
                titleDlg.setContentText("Game Title:");
                Optional<String> optTitle = titleDlg.showAndWait();
                if (optTitle.isEmpty() || optTitle.get().isBlank()) return;
                String title = optTitle.get();

                GameGoal g = new GameGoal();
                g.setTitle(title);

                Optional<RawgClient.GameDetails> detOpt = RawgClient.fetchGameDetails(title);
                detOpt.ifPresent(det -> {
                    g.setRawgId(det.id());
                    g.setReleased(LocalDate.parse(det.released()));
                    g.setRating(det.rating());
                    g.setGenres(det.genres());
                    g.setDescription(det.description());
                    g.setImageUrl(det.backgroundImage());
                });
                dao.save(g);
                refreshDashboard();


                // 1a) Estimated RAWG
                Optional<RawgClient.GameInfo> info = RawgClient.fetchGameInfo(title);
                double defaultEst = info.map(RawgClient.GameInfo::playtime).orElse(0.0);
                String imageUrl = info.map(RawgClient.GameInfo::imageUrl).orElse("");


                // 2) Estimated hours
                TextInputDialog hoursDlg = new TextInputDialog(String.valueOf(defaultEst));
                hoursDlg.setTitle("Estimated Hours");
                hoursDlg.setHeaderText(null);
                hoursDlg.setContentText("Estimated hours to finish:");
                double est = Double.parseDouble(hoursDlg.showAndWait().orElse("0"));

                // 3) hours per day
                TextInputDialog perDayDlg = new TextInputDialog("1");
                perDayDlg.setTitle("Game Setup");
                perDayDlg.setHeaderText(null);
                perDayDlg.setContentText("Hours a day that you play:");
                double perDay = Double.parseDouble(perDayDlg.showAndWait().orElse("1"));

                // 4) days per week
                TextInputDialog perWeekDlg = new TextInputDialog("5");
                perWeekDlg.setTitle("Game Setup");
                perWeekDlg.setHeaderText(null);
                perWeekDlg.setContentText("Days a week that you play:");
                int days = Integer.parseInt(perWeekDlg.showAndWait().orElse("5"));

                // create and save the goal
                g.setTitle(title);
                g.setEstimatedHours(est);
                g.setHoursPerDay(perDay);
                g.setDaysPerWeek(days);
                g.setOrderIndex(goals.size());
                g.setImageUrl(imageUrl);
                dao.save(g);

                goals.add(g);
                refreshDashboard();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        // 🗑 Remove
        FontIcon trash = new FontIcon(FontAwesome.TRASH);
        trash.setIconSize(16);
        trash.setIconColor(Color.web("#e0e0e0"));
        Button removeBtn = new Button("Remove Selected", trash);
        removeBtn.setOnAction(e -> {
            GameGoal selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    dao.delete(selected.getId());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR,
                            "Failed to remove target: " + ex.getMessage(),
                            ButtonType.OK).showAndWait();
                    return;
                }
                goals.remove(selected);
                refreshDashboard();
            }
        });

        // ⏱ add session
        FontIcon clock = new FontIcon(FontAwesome.CLOCK_O);
        clock.setIconSize(16);
        clock.setIconColor(Color.web("#e0e0e0"));
        Button sessionBtn = new Button("Add Session", clock);
        sessionBtn.setOnAction(e -> {
            GameGoal selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                AddSessionDialog dlg = new AddSessionDialog(sessionDao);
                Optional<Session> opt = dlg.showAndWait(selected.getId());
                opt.ifPresent(s -> refreshDashboard());
            }
        });

        Button allBtn = new Button("Show All");
        allBtn.setOnAction(e -> {
            listView.getSelectionModel().clearSelection();
            root.setCenter(createChart());
            BorderPane.setMargin(root.getCenter(), new Insets(10));
        });


        HBox box = new HBox(10, addBtn, removeBtn, sessionBtn, allBtn);
        box.setPadding(new Insets(5));
        return box;
    }

    private ChartViewer createChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (GameGoal g : goals) {
            double played = 0;
            try {
                played = sessionDao.listByGame(g.getId())
                        .stream()
                        .mapToDouble(Session::getHoursPlayed)
                        .sum();
            } catch (SQLException e) {
                e.printStackTrace();
                // If you want, show a simple alert to users
            }
            double remaining = Math.max(0, g.getEstimatedHours() - played);
            double daysNeeded = g.getHoursPerDay() > 0
                    ? remaining / g.getHoursPerDay()
                    : 0;
            dataset.addValue(daysNeeded, "Remaining Days", g.getTitle());
        }

        // Cria o chart
        JFreeChart chart = ChartFactory.createBarChart(
                "Days to Finish Games",
                "Game",
                "Days",
                dataset
        );

        // dark style
        chart.setBackgroundPaint(java.awt.Color.decode("#121212"));
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(java.awt.Color.decode("#1e1e1e"));
        plot.setOutlineVisible(false);
        plot.setDomainGridlinePaint(java.awt.Color.GRAY);
        plot.setRangeGridlinePaint(java.awt.Color.GRAY);

        org.jfree.chart.renderer.category.BarRenderer renderer =
                (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setSeriesPaint(0, java.awt.Color.decode("#ff6161"));
        renderer.setShadowVisible(false);
        renderer.setItemMargin(0.2);

        chart.getTitle().setPaint(java.awt.Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        plot.getDomainAxis().setTickLabelPaint(java.awt.Color.WHITE);
        plot.getDomainAxis().setLabelPaint(java.awt.Color.WHITE);
        plot.getRangeAxis().setTickLabelPaint(java.awt.Color.WHITE);
        plot.getRangeAxis().setLabelPaint(java.awt.Color.WHITE);

        ChartViewer cv = new ChartViewer(chart);
        cv.getStyleClass().add("chart-viewer");

        return new ChartViewer(chart);
    }

    private ChartViewer createProgressChart(GameGoal game) {
        // 1) Assemble the time series with date and time per session
        TimeSeries series = new TimeSeries("Hours Played");
        try {
            List<Session> sessions = sessionDao.listByGame(game.getId())
                    .stream()
                    .sorted(Comparator.comparing(Session::getPlayDate))
                    .collect(Collectors.toList());
            for (Session s : sessions) {
                // converte LocalDate em java.util.Date via java.sql.Date
                System.out.println("Session in: " + s.getPlayDate() + ", hours: " + s.getHoursPlayed());
                Day day = new Day(java.sql.Date.valueOf(s.getPlayDate()));
                series.addOrUpdate(day, s.getHoursPlayed());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        // 2) Create the line chart
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                game.getTitle() + "  —  Hours Progress",
                "Data",
                "Hours",
                dataset,
                false,  // sem legenda
                true,
                false
        );

        chart.setBackgroundPaint(java.awt.Color.decode("#121212"));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(java.awt.Color.decode("#1e1e1e"));
        plot.setDomainGridlinePaint(java.awt.Color.GRAY);
        plot.setRangeGridlinePaint(java.awt.Color.GRAY);

        // 1) eixo de data com formatação dia/mês
        DateAxis domain = (DateAxis) plot.getDomainAxis();
        domain.setDateFormatOverride(new SimpleDateFormat("dd/MM"));

// 2) eixo de horas com ticks inteiros
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, java.awt.Color.decode("#ff6161"));
        plot.setRenderer(renderer);

        chart.getTitle().setPaint(java.awt.Color.WHITE);
        chart.getTitle().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));

        plot.getDomainAxis().setTickLabelPaint(java.awt.Color.WHITE);
        plot.getDomainAxis().setLabelPaint(java.awt.Color.WHITE);
        plot.getRangeAxis().setTickLabelPaint(java.awt.Color.WHITE);
        plot.getRangeAxis().setLabelPaint(java.awt.Color.WHITE);

        ChartViewer cv = new ChartViewer(chart);
        cv.getStyleClass().add("chart-viewer");

        // 4) Retorna o viewer
        return cv;
    }

    private VBox createDetailPane(GameGoal game) {
        VBox pane = new VBox(15); // spacing between sections
        pane.setFillWidth(true);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("detail-pane");

        // calculation of hours played and remaining
        double played = 0;
        try {
            played = sessionDao.listByGame(game.getId())
                    .stream()
                    .mapToDouble(Session::getHoursPlayed)
                    .sum();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        double remaining = Math.max(0, game.getEstimatedHours() - played);

        // TOPO: imagem + título + info
        ImageView cover = new ImageView(new Image(game.getImageUrl(), 100, 100, true, true));
        Label title = new Label(game.getTitle());
        title.getStyleClass().add("detail-title");

        Label info = new Label(String.format(
                "Estimated: %.1f h\nPlayed:  %.1f h\nRemaining: %.1f h",
                game.getEstimatedHours(), played, remaining));
        info.getStyleClass().add("detail-info");

        VBox titleBox = new VBox(title, info);
        titleBox.setSpacing(5);

        HBox topInfo = new HBox(15, cover, titleBox);
        topInfo.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().add(topInfo);


        // GRÁFICO
        ChartViewer progressChart = createProgressChart(game);
        if (progressChart != null) {
            progressChart.setPrefHeight(250);
            progressChart.setPrefWidth(600); // largura fixa razoável
            VBox chartBox = new VBox(progressChart);
            chartBox.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 10;");
            pane.getChildren().add(chartBox);
        }

        // GOAL (Release + Rating)
        Label meta = new Label(
                String.format("Release: %s   •   Rating: %.1f/5.0",
                        game.getReleased(), game.getRating()));
        meta.getStyleClass().add("detail-meta");
        pane.getChildren().add(meta);

        // Genres
        Label genres = new Label(game.getGenres());
        genres.getStyleClass().add("detail-genres");
        pane.getChildren().add(genres);

        // Description
        TextArea desc = new TextArea(game.getDescription());
        desc.setPrefWidth(Region.USE_COMPUTED_SIZE);
        desc.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(desc, Priority.ALWAYS);
        desc.setWrapText(true);
        desc.setEditable(false);
        desc.setPrefRowCount(6);
        desc.getStyleClass().add("detail-desc");
        pane.getChildren().add(desc);

        return pane;
    }






    // In the future will be working this function of Drag and Drop effects (need more things to do)
    private void enableDragDrop(ListView<GameGoal> list) {
        list.setOnDragDetected(e -> {
            Dragboard db = list.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(list.getSelectionModel().getSelectedIndex()));
            db.setContent(content);
            e.consume();
        });
        list.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (e.getGestureSource() == list && db.hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        list.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int from = Integer.parseInt(db.getString());
                int to = list.getSelectionModel().getSelectedIndex();
                Collections.swap(goals, from, to);
                try {
                    for (int i = 0; i < goals.size(); i++) {
                        GameGoal g = goals.get(i);
                        g.setOrderIndex(i);
                        dao.save(g);
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    // Refresh List, Chart and force re-render of ListView
    private void refreshDashboard() {
        listView.refresh();

        root.setTop(createSummary());

        // Limpa o centro
        root.setCenter(null);

        ChartViewer chartPane = createChart();

        StackPane centerWrapper = new StackPane(chartPane);
        centerWrapper.setPadding(new Insets(10));
        StackPane.setMargin(chartPane, new Insets(10));
        centerWrapper.setStyle("-fx-background-color: #121212;");

        root.setCenter(centerWrapper);
    }



    // Custom cell showing title, estimate and progress
    private static class GoalCell extends ListCell<GameGoal> {
        private final SessionDao sessionDao;

        public GoalCell(SessionDao sessionDao) {
            this.sessionDao = sessionDao;
        }

        @Override
        protected void updateItem(GameGoal item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                double played = 0;
                try {
                    played = sessionDao.listByGame(item.getId())
                            .stream()
                            .mapToDouble(Session::getHoursPlayed)
                            .sum();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                double pct = item.getEstimatedHours() > 0
                        ? (played / item.getEstimatedHours() * 100)
                        : 0;

                ImageView iv = new ImageView();
                iv.setFitWidth(50);
                iv.setFitHeight(50);
                iv.setPreserveRatio(true);
                if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
                    Image img = new Image(item.getImageUrl(), true);
                    iv.setImage(img);
                }

                Label lbl = new Label(String.format("%s: %.1f/%.1f h (%.0f%%)",
                        item.getTitle(), played, item.getEstimatedHours(), pct));
                lbl.setTextFill(javafx.scene.paint.Color.web("#e0e0e0"));
                lbl.setWrapText(true);
                lbl.setMaxWidth(Double.MAX_VALUE); // <-- relevant
                HBox.setHgrow(lbl, Priority.ALWAYS); // <-- revelant

                HBox box = new HBox(10, iv, lbl);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPrefWidth(USE_COMPUTED_SIZE); // <-- ensures that HBox fits
                box.setMaxWidth(Double.MAX_VALUE);
                setGraphic(box);
                setText(null);

                String tip = String.format("Game: %s%nHours Played: %.1f%nEstimated: %.1f%nProgress: %.0f%%",
                        item.getTitle(), played, item.getEstimatedHours(), pct);
                Tooltip.install(box, new Tooltip(tip));
            }
        }
        /**
         * Updates the entire dashboard (list and chart) after changes.
         */
    }
}




