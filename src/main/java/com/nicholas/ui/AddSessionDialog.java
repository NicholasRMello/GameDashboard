package com.nicholas.ui;

import com.nicholas.dao.SessionDao;
import com.nicholas.model.Session;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Dialog para adicionar uma sessão de jogo (data e horas jogadas).
 */
public class AddSessionDialog {
    private final SessionDao sessionDao;

    public AddSessionDialog(SessionDao sessionDao) {
        this.sessionDao = sessionDao;
    }

    /**
     * Exibe o diálogo e retorna a Session criada, ou empty se cancelado.
     * @param gameId id do jogo para associar a sessão
     */
    public Optional<Session> showAndWait(long gameId) {
        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Sessão");
        dialog.setHeaderText(null);

        ButtonType addButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField hoursField = new TextField();
        hoursField.setPromptText("Horas jogadas");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Data:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Horas:"), 0, 1);
        grid.add(hoursField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Habilitar botão apenas se horas for um número válido
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        hoursField.textProperty().addListener((obs, oldV, newV) -> {
            addButton.setDisable(!newV.matches("\\d+(\\\\.\\d+)?"));
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Session s = new Session();
                s.setGameId(gameId);
                s.setPlayDate(datePicker.getValue());
                s.setHoursPlayed(Double.parseDouble(hoursField.getText()));
                return s;
            }
            return null;
        });

        Optional<Session> result = dialog.showAndWait();
        if (result.isPresent()) {
            Session session = result.get();
            try {
                sessionDao.save(session);
                return Optional.of(session);
            } catch (SQLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Erro ao salvar sessão: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
        return Optional.empty();
    }
}
