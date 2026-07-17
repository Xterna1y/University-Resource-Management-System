package com.campus.client.ui.pages.booking;

import com.campus.client.data.BookingStorage;
import com.campus.client.model.Booking;
import com.campus.client.ui.components.Theme;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * BHistory (BookingHistoryView) - same underlying logic as before
 * (BookingStorage.upcoming/past/loadCancelledForStudent, cancel with
 * confirmation). This pass only restyles the tab selector as segmented
 * buttons and adds a status pill + inline View action, to match the mockup.
 */
@SuppressWarnings("unused")
public class BHistory extends BorderPane {

    private enum Filter { UPCOMING, PAST, CANCELLED }

    private final BookingStorage bookingStorage;
    private final String studentId;

    private final TableView<Booking> bookingTable = new TableView<>();
    private final Button upcomingTabButton = new Button("Upcoming");
    private final Button pastTabButton = new Button("Past");
    private final Button cancelledTabButton = new Button("Cancelled");
    private final Button cancelBookingButton = new Button("Cancel Selected Booking");

    private Filter activeFilter = Filter.UPCOMING;
    private Runnable onBack;

    public BHistory(BookingStorage bookingStorage, String studentId) {
        this.bookingStorage = bookingStorage;
        this.studentId = studentId;

        setPadding(new Insets(20));
        setTop(buildHeader());
        setCenter(buildBody());

        refresh();
    }

    public void setOnBack(Runnable r) { this.onBack = r; }

    private VBox buildHeader() {
        Label title = new Label("Booking History");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill:" + Theme.DARK + ";");

        Label subtitle = new Label("View your past and upcoming bookings");
        subtitle.setStyle("-fx-text-fill:" + Theme.TEXT_MUTED + ";");

        Button backButton = new Button("\u2190 Back");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill:" + Theme.DARK + "; -fx-cursor: hand;");
        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });

        VBox box = new VBox(6, backButton, title, subtitle);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    private VBox buildBody() {
        HBox segmented = buildSegmentedTabs();
        buildTableColumns();
        bookingTable.setPlaceholder(new Label("No bookings to show."));

        cancelBookingButton.setStyle(Theme.secondaryButton());
        cancelBookingButton.setOnAction(e -> handleCancel());

        HBox actions = new HBox(10, cancelBookingButton);
        actions.setPadding(new Insets(10, 0, 0, 0));

        VBox box = new VBox(15, segmented, bookingTable, actions);
        return box;
    }

    /** Flat segmented-button row instead of a TabPane, matching the mockup's "Upcoming | Past | Cancelled" control. */
    private HBox buildSegmentedTabs() {
        for (Button b : new Button[]{upcomingTabButton, pastTabButton, cancelledTabButton}) {
            b.setPrefWidth(110);
        }
        upcomingTabButton.setOnAction(e -> { activeFilter = Filter.UPCOMING; refresh(); });
        pastTabButton.setOnAction(e -> { activeFilter = Filter.PAST; refresh(); });
        cancelledTabButton.setOnAction(e -> { activeFilter = Filter.CANCELLED; refresh(); });

        HBox box = new HBox(0, upcomingTabButton, pastTabButton, cancelledTabButton);
        box.setStyle("-fx-border-color:" + Theme.GREY_BORDER + "; -fx-border-radius: 6; -fx-background-radius: 6;");
        return box;
    }

    private void styleSegmentedTabs() {
        styleTabButton(upcomingTabButton, activeFilter == Filter.UPCOMING);
        styleTabButton(pastTabButton, activeFilter == Filter.PAST);
        styleTabButton(cancelledTabButton, activeFilter == Filter.CANCELLED);
    }

    private void styleTabButton(Button b, boolean active) {
        if (active) {
            b.setStyle("-fx-background-color:" + Theme.RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            b.setStyle("-fx-background-color: white; -fx-text-fill:" + Theme.DARK + "; -fx-cursor: hand;");
        }
    }

    @SuppressWarnings("unchecked")
    private void buildTableColumns() {
        TableColumn<Booking, String> facilityCol = new TableColumn<>("Facility");
        facilityCol.setCellValueFactory(new PropertyValueFactory<>("facilityName"));

        TableColumn<Booking, Object> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));

        TableColumn<Booking, Object> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<Booking, Object> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        TableColumn<Booking, Booking> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(booking.getStatus().name());
                pill.setStyle(Theme.statusPill(booking.getStatus() != Booking.Status.CANCELLED));
                setGraphic(pill);
            }
        });

        TableColumn<Booking, Booking> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            {
                viewButton.setStyle("-fx-background-color: transparent; -fx-text-fill:" + Theme.RED
                        + "; -fx-underline: true; -fx-cursor: hand;");
                viewButton.setOnAction(e -> showDetails(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                setGraphic(empty || booking == null ? null : viewButton);
            }
        });

        bookingTable.getColumns().setAll(List.of(facilityCol, dateCol, startCol, endCol, statusCol, actionCol));
    }

    /** Reloads the table for whichever segment is currently active. Same data sources as before. */
    private void refresh() {
        styleSegmentedTabs();
        List<Booking> data = switch (activeFilter) {
            case PAST -> bookingStorage.past(studentId);
            case CANCELLED -> bookingStorage.loadCancelledForStudent(studentId);
            case UPCOMING -> bookingStorage.upcoming(studentId);
        };
        bookingTable.setItems(FXCollections.observableArrayList(data));
        cancelBookingButton.setVisible(activeFilter == Filter.UPCOMING);
        cancelBookingButton.setManaged(activeFilter == Filter.UPCOMING);
    }

    private void showDetails(Booking booking) {
        if (booking == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Details");
        alert.setHeaderText(booking.getFacilityName());
        alert.setContentText(String.format(
                "Reference: %s%nDate: %s%nTime: %s - %s%nStatus: %s",
                booking.getBookingId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime(), booking.getStatus()));
        alert.showAndWait();
    }

    private void handleCancel() {
        Booking selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel booking for " + selected.getFacilityName() + " on " + selected.getBookingDate() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                bookingStorage.cancel(selected.getBookingId());
                refresh();
            }
        });
    }
}