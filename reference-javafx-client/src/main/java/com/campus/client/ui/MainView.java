package com.campus.client.ui;

import com.campus.client.data.BookingStorage;
import com.campus.client.data.UserStorage;
import com.campus.client.model.User;
import com.campus.client.services.mcp.CampusMcpClient;
import com.campus.client.services.rag.RagService;
import com.campus.client.ui.components.Footer;
import com.campus.client.ui.components.Header;
import com.campus.client.ui.components.NavBar;
import com.campus.client.ui.pages.assistant.AssistantView;
import com.campus.client.ui.pages.booking.BHistory;
import com.campus.client.ui.pages.booking.BookingView;
import com.campus.client.ui.pages.dashboard.DashboardPage;
import com.campus.client.ui.pages.facilities.AvailableView;
import com.campus.client.ui.pages.facilities.FaciView;
import com.campus.client.ui.pages.login.LoginPage;
import com.campus.client.ui.pages.login.RegisterPage;

import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * MainView - the JavaFX application shell. Holds the header/navbar/footer
 * frame and swaps the center content between pages. App.java calls
 * getRoot(), setStatus(...), bind(mcp, rag) and refreshDiscovery() on this
 * class during startup (see App.java).
 */
public class MainView {

    private final BorderPane root = new BorderPane();
    private final Header header = new Header();
    private final NavBar navBar = new NavBar();
    private final Footer footer = new Footer();

    private final UserStorage userStorage = new UserStorage();
    private final BookingStorage bookingStorage = new BookingStorage();

    // Background worker for MCP/LLM calls triggered from any page (daemon threads so the
    // JVM can still exit even if a call is in flight when the window closes).
    private final ExecutorService worker = Executors.newCachedThreadPool(daemonThreadFactory());

    private CampusMcpClient mcp;
    private RagService rag;
    private User currentUser;

    public MainView() {
        root.setTop(header);
        root.setLeft(navBar);
        root.setBottom(footer);
        navBar.setOnDashboard(this::showDashboard);
        navBar.setOnFacilities(this::showFacilities);
        navBar.setOnHistory(this::showBookingHistory);
        navBar.setOnAssistant(this::showAssistant);
        navBar.setOnLogout(this::logout);

        showLogin();
    }

    public Parent getRoot() {
        return root;
    }

    public void setStatus(String text) {
        header.setStatus(text);
    }

    /** Called by App.java once the MCP connection (and optionally the LLM) is ready. */
    public void bind(CampusMcpClient mcp, RagService rag) {
        this.mcp = mcp;
        this.rag = rag;
    }

    /** Called by App.java after a successful connection; currently just a hook for future use. */
    public void refreshDiscovery() {
        // Discovery details are surfaced via the status line for now; individual pages
        // (FaciView, AvailableView, AssistantView, BookingView) query the server directly
        // when the student navigates to them.
    }


    private void setContent(javafx.scene.Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding:0;"
        );

        root.setCenter(scrollPane);
    }

    // --- Navigation ---

    private void showLogin() {
        navBar.setLoggedIn(false);
        LoginPage page = new LoginPage(userStorage);
        page.setOnLoginSuccess(this::onLoggedIn);
        page.setOnGoToRegister(this::showRegister);
        setContent(page);
    }

    private void showRegister() {
        navBar.setLoggedIn(false);
        RegisterPage page = new RegisterPage(userStorage);
        page.setOnRegisterSuccess(this::onLoggedIn);
        page.setOnBackToLogin(this::showLogin);
        setContent(page);
    }

    private void onLoggedIn(User user) {
        this.currentUser = user;
        navBar.setLoggedIn(true);
        showDashboard();
    }

    private void showDashboard() {
        requireLogin();
        DashboardPage page = new DashboardPage(currentUser);
        page.setOnBrowseFacilities(this::showFacilities);
        page.setOnBookingHistory(this::showBookingHistory);
        page.setOnAssistant(this::showAssistant);
        setContent(page);
    }

    private void showFacilities() {
        System.out.println("Facilities button clicked");
        requireLogin();
        if (mcp == null) {
            setStatus("Still connecting to the campus server, please wait a moment and try again.");
            return;
        }
        FaciView page = new FaciView(mcp, worker);
        page.setOnCheckAvailability(facilityText -> showAvailability(facilityText));
        page.setOnBack(this::showDashboard);
        setContent(page);
    }

    private void showAvailability(String prefillFacilityText) {
        AvailableView page = new AvailableView(mcp, worker, prefillFacilityText);
        page.setOnProceedToBooking((resourceId, facilityName) -> showBooking(resourceId, facilityName));
        page.setOnBack(this::showFacilities);
        setContent(page);
    }

    private void showBooking(String resourceId, String facilityName) {
        BookingView page = new BookingView(mcp, worker, bookingStorage,
                currentUser.getStudentId(), resourceId, facilityName);
        page.setOnBackToAvailability(this::showFacilities);
        page.setOnBookingConfirmed(this::showBookingHistory);
        setContent(page);
    }

    private void showBookingHistory() {
        requireLogin();
        BHistory page = new BHistory(bookingStorage, currentUser.getStudentId());
        page.setOnBack(this::showDashboard);
        setContent(page);
    }

    private void showAssistant() {
        requireLogin();
        AssistantView page = new AssistantView(rag, worker); // rag may be null; AssistantView handles that
        page.setOnBack(this::showDashboard);
        setContent(page);
    }

    private void logout() {
        currentUser = null;
        showLogin();
    }

    private void requireLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("Navigation attempted without a logged-in user.");
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread t = new Thread(runnable, "mcp-worker");
            t.setDaemon(true);
            return t;
        };
    }
}