# Getting started

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- Google Chrome (only needed to run the included demo test)

---

## Installation

### Option A — Build from source (recommended)

Clone the repository and install to your local Maven cache:

```bash
git clone https://github.com/your-org/selenium-reporter.git
cd selenium-reporter
mvn clean install -DskipTests
```

### Option B — Install a pre-built JAR

If you received a `selenium-reporter-1.0.0.jar` file:

```bash
mvn install:install-file \
  -Dfile=selenium-reporter-1.0.0.jar \
  -DgroupId=com.empresa \
  -DartifactId=selenium-reporter \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

---

## Add the dependency

In your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.empresa</groupId>
    <artifactId>selenium-reporter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Minimal working example — Cucumber

### 1. Register the plugin

In your JUnit 5 runner class:

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
    key   = PLUGIN_PROPERTY_NAME,
    value = "com.empresa.reporter.listener.CucumberReportPlugin"
)
public class RunnerTest {}
```

Or via `src/test/resources/junit-platform.properties`:

```properties
cucumber.plugin=com.empresa.reporter.listener.CucumberReportPlugin
```

### 2. Wire the driver in hooks

```java
public class Hooks {

    private WebDriver driver;

    @Before
    public void setUp() {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new");
        driver = new ChromeDriver(opts);
        ReportContext.setDriver(driver);   // gives the reporter access to the driver
    }

    @After
    public void tearDown() {
        ReportContext.removeDriver();      // must come BEFORE driver.quit()
        driver.quit();
    }
}
```

> **Order matters.** Call `ReportContext.removeDriver()` before `driver.quit()`.
> The reporter captures screenshots at step-failure time (before `@After` runs),
> so removing the driver from context before quitting prevents accidental
> screenshot attempts on a closed session.

### 3. Configure the report (optional)

In a `@BeforeAll` static setup or a dedicated configuration hook:

```java
@BeforeAll
public static void configureReporter() {
    ReportContext.setConfig(ReporterConfig.builder()
        .projectName("My App — Acceptance Tests")
        .outputDir("./test-reports")
        .captureScreenshotOnFailure(true)
        .build());
}
```

If you skip this step the library uses sensible defaults (see [configuration.md](configuration.md)).

---

## Minimal working example — TestNG

```java
@Listeners(TestNGReportListener.class)
public class LoginTest {

    private WebDriver driver;

    @BeforeClass
    public static void configureReporter() {
        ReportContext.setConfig(ReporterConfig.builder()
            .projectName("My App")
            .outputDir("./test-reports")
            .captureScreenshotOnFailure(true)
            .build());
    }

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        ReportContext.setDriver(driver);
    }

    @AfterMethod
    public void tearDown() {
        ReportContext.removeDriver();
        driver.quit();
    }

    @Test
    public void testLoginSuccess() {
        driver.get("https://example.com/login");
        // ... interactions and assertions ...
    }
}
```

---

## Running tests and finding the report

```bash
# With the included demo profile (requires Chrome):
mvn test -Prun-selenium-tests

# With your own project:
mvn test
```

The report is written to the configured `outputDir`:

```
./test-reports/
├── report_20260418_143215.html   ← open this in any browser
└── history/
    └── run_1713450735000.json    ← history entry for this run
```

Open `report_*.html` directly — it is fully self-contained with all charts,
screenshots, and styles embedded. No web server needed.
