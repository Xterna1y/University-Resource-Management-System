package com.campus.client.ui.pages.booking;

import com.campus.client.data.BookingStorage;
import com.campus.client.model.Booking;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * BHistory (BookingHistoryView) - FR09 / UC14: shows a student's booking
 * history split into Upcoming / Past / Cancelled tabs (Table 26 of the
 * design report), reading from the local text-file store (BookingStorage).
 */
public class BHistory extends BorderPane {

    private final BookingStorage bookingStorage;
    private final String studentId;

    private final TableView<Booking> bookingTable = new TableView<>();
    private final TabPane tabPane = new TabPane();
    private Runnable onBack;

    public BHistory(BookingStorage bookingStorage, String studentId) {
        this.bookingStorage = bookingStorage;
        this.studentId = studentId;

        setPadding(new Insets(20));
        setTop(buildHeader());
        setCenter(buildTabs());

        refresh();
    }

    public void setOnBack(Runnable r) { this.onBack = r; }

    private VBox buildHeader() {
        Label title = new Label("Booking History");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> { if (onBack != null) onBack.run(); });

        VBox box = new VBox(10, title, backButton);
        box.setPadding(new Insets(0, 0, 15, 0));
        return box;
    }

    @SuppressWarnings("unchecked")
    private TabPane buildTabs() {
        buildTableColumns();

        Tab upcomingTab = new Tab("Upcoming");
        Tab pastTab = new Tab("Past");
        Tab cancelledTab = new Tab("Cancelled");
        for (Tab t : List.of(upcomingTab, pastTab, cancelledTab)) {
            t.setClosable(false);
        }

        VBox upcomingBox = buildTabContent(true);
        upcomingTab.setContent(upcomingBox);
        pastTab.setContent(new Label()); // populated in refresh()
        cancelledTab.setContent(new Label());

        tabPane.getTabs().addAll(upcomingTab, pastTab, cancelledTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refresh());
        return tabPane;
    }

    private VBox buildTabContent(boolean withCancel) {
        Button viewButton = new Button("View");
        viewButton.setOnAction(e -> showDetails(bookingTable.getSelectionModel().getSelectedItem()));

        VBox box = new VBox(10, bookingTable, viewButton);
        if (withCancel) {
            Button cancelBookingButton = new Button("Cancel Booking");
            cancelBookingButton.setOnAction(e -> handleCancel());
            box.getChildren().add(cancelBookingButton);
        }
        return box;
    }

    private void buildTableColumns() {
        TableColumn<Booking, String> facilityCol = new TableColumn<>("Facility");
        facilityCol.setCellValueFactory(new PropertyValueFactory<>("facilityName"));

        TableColumn<Booking, Object> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));

        TableColumn<Booking, Object> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<Booking, Object> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        TableColumn<Booking, Object> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookingTable.getColumns().setAll(List.of(facilityCol, dateCol, startCol, endCol, statusCol));
        bookingTable.setPlaceholder(new Label("No bookings to show."));
    }

    /** Reloads the table for whichever tab is currently selected. */
    private void refresh() {
        int selected = tabPane.getSelectionModel().getSelectedIndex();
        List<Booking> data;
        if (selected == 1) {
            data = bookingStorage.past(studentId);
        } else if (selected == 2) {
            data = bookingStorage.loadCancelledForStudent(studentId);
        } else {
            data = bookingStorage.upcoming(studentId);
        }
        ObservableList<Booking> items = FXCollections.observableArrayList(data);
        bookingTable.setItems(items);
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