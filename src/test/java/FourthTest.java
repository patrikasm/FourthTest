/************************************************************************************************
 Task:
    Create two automated tests, task conditions:
        - A new user must be created before the tests can be run automatically.
        - Both tests must log in with the same created user.
        - The tests and the user creation must be executed in separate webdriver sessions.
        - Use the annotations of the Unit Tests to invoke and close the webdriver sessions.
        - Run the tests via a Jenkins job with a cron scheduler.

    User development workflow:
        1. Open the website https://demowebshop.tricentis.com/
        2. Click 'Log in'
        3. Click 'Register' under 'FourthTest Customer'
        4. Fill in the fields on the registration form
        5. Click 'Register'
        6. Click 'Continue'

    Test scenarios:
        1.  Open the website https://demowebshop.tricentis.com/
        2.  Click 'Log in'
        3.  Fill in 'Email:', 'Password:' and click 'Log in'
        4.  Select 'Digital downloads' from the side menu
        5.  Add items to your basket by reading the text file
            (for the first test read from data1.txt, for the second test read from data2.txt)
        6.  Open 'Shopping cart'
        7.  Click on the 'I agree' checkbox and the 'Checkout' button
        8.  In 'Billing Address' select an existing address or fill in the fields for a new address,
            click 'Continue'
        9.  For 'Payment Method' click 'Continue'
        10. For 'Payment Information' click 'Continue'
        11. For 'Confirm Order' click 'Confirm'
        12. Confirm that order has been placed

************************************************************************************************/

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class FourthTest {
    private WebDriver driver;
    private static WebDriver staticDriver;
    private static String userEmail;
    private static final String USER_PASSWORD_123 = "Password123";
    private static final Logger LOGGER = Logger.getLogger(FourthTest.class.getName());
    private static final Random random = new Random();

    @BeforeAll
    public static void setupTestAndCreateUser() {
        System.setProperty("webdriver.chrome.driver", "/Users/patrikasm/Downloads/chromedriver-mac-x64/chromedriver");
        staticDriver = new ChromeDriver();
        staticDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        boolean userCreated = false;
        do {
            try {
                createUser();
                userCreated = true;
            } catch (Exception e) {
                LOGGER.info("Exception occurred while creating user. Retrying...");
            }
        } while (!userCreated);
        staticDriver.quit();
    }

    @BeforeEach
    public void setDriverAndLogin() {
        System.setProperty("webdriver.chrome.driver", "/Users/patrikasm/Downloads/chromedriver-mac-x64/chromedriver");
        driver = new ChromeDriver();
        loginUser();
    }

    @Test
    void testScenario1() {
        navigateAndAddItems("data1.txt");
        Assertions.assertEquals("Your order has been successfully processed!", completeCheckoutProcess());
    }

    @Test
    void testScenario2() {
        navigateAndAddItems("data2.txt");
        Assertions.assertEquals("Your order has been successfully processed!", completeCheckoutProcess());
    }

    @AfterEach
    public void completeCheckoutAndTearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    public static void createUser() {
        staticDriver.get("https://demowebshop.tricentis.com/");
        staticDriver.findElement(By.xpath("//a[contains(text(),'Register')]")).click();

        int randomInt = random.nextInt(10000) + 1;
        userEmail = "test.user." + randomInt + "@example.com";
        staticDriver.findElement(By.id("gender-male")).click();
        staticDriver.findElement(By.id("FirstName")).sendKeys("Test");
        staticDriver.findElement(By.id("LastName")).sendKeys("User");
        staticDriver.findElement(By.id("Email")).sendKeys(userEmail);
        staticDriver.findElement(By.id("Password")).sendKeys(USER_PASSWORD_123);
        staticDriver.findElement(By.id("ConfirmPassword")).sendKeys(USER_PASSWORD_123);
        staticDriver.findElement(By.id("register-button")).click();
        staticDriver.findElement(By.xpath("//input[@value='Continue']")).click();
    }

    private void loginUser() {
        driver.get("https://demowebshop.tricentis.com/login");
        driver.findElement(By.id("Email")).sendKeys(userEmail);
        driver.findElement(By.id("Password")).sendKeys(USER_PASSWORD_123);
        driver.findElement(By.xpath("//input[@value='Log in']")).click();
    }

    public int extractQty(String elementString) {
        StringBuilder qtyBuilder = new StringBuilder();
        for (int i = 0; i < elementString.length(); i++) {
            char c = elementString.charAt(i);
            if (Character.isDigit(c)) {
                qtyBuilder.append(c);
            }
        }
        return Integer.parseInt(qtyBuilder.toString());
    }

    private void navigateAndAddItems(String dataFileName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement digitalDownloads = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Digital downloads')]")));
        digitalDownloads.click();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/data/" + dataFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                WebElement cartQtyElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[@class='cart-qty']")));
                int qtyInt = extractQty(cartQtyElement.getText());
                List<WebElement> product = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("//h2[@class='product-title']/a[text()='" + line + "']/parent::*/parent::*/div[@class='add-info']/div[@class='buttons']/input[@value='Add to cart']")));
                product.get(0).click();
                qtyInt++;
                wait.until(ExpectedConditions.textToBe(By.xpath("//span[@class='cart-qty']"), "(" + qtyInt + ")"));
            }
        } catch (IOException e) {
            LOGGER.info("Exception occurred while reading data file");
        }
    }

    private String completeCheckoutProcess() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement shoppingCart = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[contains(text(),'Shopping cart')]")));
        shoppingCart.click();

        driver.findElement(By.id("termsofservice")).click();
        WebElement checkoutButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("checkout")));
        checkoutButton.click();

        try {
            driver.findElement(By.id("billing-address-select"));
        } catch (Exception e) {
            driver.findElement(By.id("BillingNewAddress_CountryId")).sendKeys("Lithuania");
            driver.findElement(By.id("BillingNewAddress_City")).sendKeys("Vilnius");
            driver.findElement(By.id("BillingNewAddress_Address1")).sendKeys("Gedimino pr. 1");
            driver.findElement(By.id("BillingNewAddress_ZipPostalCode")).sendKeys("LT-01103");
            driver.findElement(By.id("BillingNewAddress_PhoneNumber")).sendKeys("+37060000000");
        }

        WebElement continueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@class='button-1 new-address-next-step-button']")));
        continueButton.click();
        WebElement paymentMethodContinueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@class='button-1 payment-method-next-step-button']")));
        paymentMethodContinueButton.click();

        WebElement paymentInformationContinueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@class='button-1 payment-info-next-step-button']")));
        paymentInformationContinueButton.click();

        WebElement confirmOrderButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@class='button-1 confirm-order-next-step-button']")));
        confirmOrderButton.click();

        WebElement orderConfirmation = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[@class='title']")));

        return orderConfirmation.getText();

    }
}