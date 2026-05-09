package com.taskmanager.tests;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MERN Task Manager – Selenium Test Suite
 * 17 automated test cases  (TC-01 … TC-17)
 *
 * Language  : Java 17
 * Framework : JUnit 5 + Selenium WebDriver 4
 * Browser   : Headless Google Chrome  (markhobson/maven-chrome image)
 * Runner    : Maven Surefire Plugin
 *
 * Configure the target URL:
 *   mvn test -Dapp.url=http://localhost:3000
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskManagerTest {

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final String BASE_URL =
            System.getProperty("app.url", "http://localhost:3000");

    // One unique e-mail per test-run so TC-01 always finds a fresh account
    private static final long   TS       = System.currentTimeMillis();
    private static final String EMAIL    = "testuser_" + TS + "@mail.com";
    private static final String PASSWORD = "Test@1234";
    private static final String NAME     = "Selenium Tester";

    private static final int WAIT_SEC = 15;

    // ── Shared driver (one browser for all tests) ─────────────────────────────
    private static WebDriver       driver;
    private static WebDriverWait   wait;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeAll
    static void setUpDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless",               // no display needed on server
                "--no-sandbox",             // required inside Docker
                "--disable-dev-shm-usage",  // prevents /dev/shm overflow
                "--disable-gpu",
                "--window-size=1920,1080",
                "--remote-allow-origins=*"
        );
        driver = new ChromeDriver(options);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SEC));
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) driver.quit();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void go(String path) {
        driver.get(BASE_URL + path);
    }

    private WebElement waitFor(String cssSelector) {
        return wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssSelector)));
    }

    private WebElement waitVisible(String cssSelector) {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
    }

    private void type(String cssSelector, String text) {
        WebElement el = waitFor(cssSelector);
        el.clear();
        el.sendKeys(text);
    }

    private void click(String cssSelector) {
        WebElement el = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
        el.click();
    }

    private void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    /** Log in with the shared test account and wait for the dashboard. */
    private void loginAs(String email, String password) throws InterruptedException {
        go("/login");
        waitFor("#login-email");
        type("#login-email",    email);
        type("#login-password", password);
        click("#login-btn");
        waitFor("#logout-btn");
    }

    private void loginAsDefault() throws InterruptedException {
        loginAs(EMAIL, PASSWORD);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-01  Valid Registration
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(1)
    @DisplayName("TC-01: Valid user registration navigates to dashboard")
    void tc01_validRegistration() {
        go("/register");
        waitFor("#reg-name");

        type("#reg-name",     NAME);
        type("#reg-email",    EMAIL);
        type("#reg-password", PASSWORD);
        click("#register-btn");

        waitFor("#logout-btn");
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Should redirect to /dashboard after registration");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-02  Duplicate E-mail
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(2)
    @DisplayName("TC-02: Duplicate email shows an error message")
    void tc02_duplicateEmail() {
        go("/register");
        waitFor("#reg-name");

        type("#reg-name",     "Another User");
        type("#reg-email",    EMAIL);          // same email as TC-01
        type("#reg-password", PASSWORD);
        click("#register-btn");

        String msg = waitFor("#register-error").getText();
        assertTrue(msg.toLowerCase().contains("email"),
                "Error message should mention 'email'");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-03  Valid Login
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(3)
    @DisplayName("TC-03: Valid login redirects to dashboard")
    void tc03_validLogin() throws InterruptedException {
        loginAsDefault();
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "URL should contain /dashboard");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-04  User Name Visible in Navbar
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(4)
    @DisplayName("TC-04: Logged-in user name is displayed in the navbar")
    void tc04_userNameInNavbar() throws InterruptedException {
        loginAsDefault();
        String navText = waitFor("#user-name").getText();
        assertTrue(navText.contains(NAME),
                "Navbar should contain the user's name: " + NAME);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-05  Invalid Password
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(5)
    @DisplayName("TC-05: Wrong password shows 'Invalid credentials' error")
    void tc05_invalidPassword() {
        go("/login");
        waitFor("#login-email");

        type("#login-email",    EMAIL);
        type("#login-password", "WrongPass!999");
        click("#login-btn");

        String msg = waitFor("#login-error").getText();
        assertTrue(msg.toLowerCase().contains("invalid"),
                "Error should say 'invalid credentials'");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-06  Empty Login Fields
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(6)
    @DisplayName("TC-06: Submitting empty login form stays on /login")
    void tc06_emptyLoginFields() throws InterruptedException {
        go("/login");
        waitFor("#login-btn");

        // Clear both fields then click – HTML5 required prevents submission
        driver.findElement(By.cssSelector("#login-email")).clear();
        driver.findElement(By.cssSelector("#login-password")).clear();
        driver.findElement(By.cssSelector("#login-btn")).click();

        sleep(800);
        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should stay on /login when fields are empty");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-07  Logout
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(7)
    @DisplayName("TC-07: Logout button redirects to /login")
    void tc07_logout() throws InterruptedException {
        loginAsDefault();
        click("#logout-btn");
        waitFor("#login-form");
        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should be on /login after logout");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-08  Protected Route
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(8)
    @DisplayName("TC-08: Accessing /dashboard without auth redirects to /login")
    void tc08_protectedRoute() throws InterruptedException {
        // Clear localStorage to simulate unauthenticated state
        go("/login");
        ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
        go("/dashboard");
        sleep(1200);
        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Unauthenticated access to /dashboard should redirect to /login");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-09  Create Task – Valid Input
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(9)
    @DisplayName("TC-09: Creating a task with valid input adds it to the list")
    void tc09_createTaskValid() throws InterruptedException {
        loginAsDefault();
        click("#new-task-btn");
        waitFor("#task-title-input");

        type("#task-title-input", "Java Selenium Task");
        type("#task-desc-input",  "Created by Java TC-09");
        click("#save-task-btn");

        sleep(1000);
        List<WebElement> cards = driver.findElements(By.cssSelector(".task-card"));
        assertFalse(cards.isEmpty(), "Task list should have at least one card");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-10  Create Task – Empty Title
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(10)
    @DisplayName("TC-10: Creating a task without a title shows validation error")
    void tc10_createTaskEmptyTitle() throws InterruptedException {
        loginAsDefault();
        click("#new-task-btn");
        waitFor("#task-title-input");

        // Leave title empty, submit
        click("#save-task-btn");
        sleep(600);

        // Modal stays open – still on /dashboard
        assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Should remain on dashboard when title is empty");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-11  Edit Task Title
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(11)
    @DisplayName("TC-11: Editing a task title updates it on the card")
    void tc11_editTaskTitle() throws InterruptedException {
        loginAsDefault();
        waitFor(".edit-task-btn");

        List<WebElement> editBtns = driver.findElements(By.cssSelector(".edit-task-btn"));
        editBtns.get(0).click();
        waitFor("#task-title-input");

        type("#task-title-input", "Updated by TC-11");
        click("#save-task-btn");
        sleep(800);

        String body = driver.findElement(By.cssSelector("body")).getText();
        assertTrue(body.contains("Updated by TC-11"),
                "Updated title should appear on the task card");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-12  Change Status to In-Progress
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(12)
    @DisplayName("TC-12: Changing task status to in-progress reflects on card badge")
    void tc12_statusInProgress() throws InterruptedException {
        loginAsDefault();
        waitFor(".edit-task-btn");

        driver.findElements(By.cssSelector(".edit-task-btn")).get(0).click();
        waitFor("#task-status-input");

        new Select(driver.findElement(By.cssSelector("#task-status-input")))
                .selectByValue("in-progress");
        click("#save-task-btn");
        sleep(800);

        List<WebElement> badges = driver.findElements(By.cssSelector(".task-status"));
        boolean found = badges.stream()
                .anyMatch(b -> b.getText().contains("in-progress"));
        assertTrue(found, "At least one card should show 'in-progress' badge");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-13  Change Status to Completed
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(13)
    @DisplayName("TC-13: Changing task status to completed reflects on card badge")
    void tc13_statusCompleted() throws InterruptedException {
        loginAsDefault();
        waitFor(".edit-task-btn");

        driver.findElements(By.cssSelector(".edit-task-btn")).get(0).click();
        waitFor("#task-status-input");

        new Select(driver.findElement(By.cssSelector("#task-status-input")))
                .selectByValue("completed");
        click("#save-task-btn");
        sleep(800);

        List<WebElement> badges = driver.findElements(By.cssSelector(".task-status"));
        boolean found = badges.stream()
                .anyMatch(b -> b.getText().contains("completed"));
        assertTrue(found, "At least one card should show 'completed' badge");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-14  Delete Task
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(14)
    @DisplayName("TC-14: Deleting a task removes it from the list")
    void tc14_deleteTask() throws InterruptedException {
        loginAsDefault();
        waitFor(".task-card");

        int before = driver.findElements(By.cssSelector(".task-card")).size();
        click(".delete-task-btn");

        // Accept the JS confirm() dialog
        wait.until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert().accept();
        sleep(1000);

        int after = driver.findElements(By.cssSelector(".task-card")).size();
        assertTrue(after < before,
                "Task count should decrease after deletion");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-15  Filter by Status
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(15)
    @DisplayName("TC-15: Status filter shows only tasks matching selected status")
    void tc15_filterByStatus() throws InterruptedException {
        loginAsDefault();

        // Create a fresh pending task so the filter has something to find
        click("#new-task-btn");
        waitFor("#task-title-input");
        type("#task-title-input", "Pending Filter Task");
        click("#save-task-btn");
        sleep(600);

        // Select 'pending' in the filter dropdown
        new Select(driver.findElement(By.cssSelector("#status-filter")))
                .selectByValue("pending");
        sleep(500);

        List<WebElement> badges = driver.findElements(By.cssSelector(".task-status"));
        boolean nonPendingVisible = badges.stream()
                .anyMatch(b -> !b.getText().equals("pending"));
        assertFalse(nonPendingVisible,
                "Non-pending tasks should not be visible when 'pending' filter is active");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-16  Search by Keyword
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(16)
    @DisplayName("TC-16: Search box filters tasks by keyword")
    void tc16_searchByKeyword() throws InterruptedException {
        loginAsDefault();

        // Create a task with a unique keyword
        click("#new-task-btn");
        waitFor("#task-title-input");
        type("#task-title-input", "UniqueKeyword7Z");
        click("#save-task-btn");
        sleep(600);

        // Type keyword in search box
        type("#search-box", "UniqueKeyword7Z");
        sleep(500);

        List<WebElement> titles = driver.findElements(By.cssSelector(".task-title"));
        assertTrue(titles.stream().allMatch(t -> t.getText().contains("UniqueKeyword7Z")),
                "All visible task titles should contain the search keyword");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TC-17  Page Title
    // ══════════════════════════════════════════════════════════════════════════
    @Test
    @Order(17)
    @DisplayName("TC-17: Browser page title is 'Task Manager'")
    void tc17_pageTitle() {
        go("/");
        assertEquals("Task Manager", driver.getTitle(),
                "Page title should be 'Task Manager'");
    }
}
