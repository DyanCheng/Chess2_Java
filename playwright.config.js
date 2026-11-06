const { defineConfig, devices } = require("@playwright/test");
const localChromium = process.platform === "win32"
  ? "C:\\Users\\duyan\\AppData\\Local\\ms-playwright\\chromium-1200\\chrome-win64\\chrome.exe"
  : undefined;

module.exports = defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  retries: 0,
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:8088",
    trace: "retain-on-failure",
    ...(localChromium ? { launchOptions: { executablePath: localChromium } } : {})
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] } }
  ],
  webServer: {
    command: "mvn -f backend/pom.xml \"-Djava.version=17\" \"-Dspring-boot.run.arguments=--server.port=8088\" spring-boot:run",
    url: "http://127.0.0.1:8088/actuator/health",
    timeout: 90_000,
    reuseExistingServer: false,
    env: {
      ...process.env,
      JWT_SECRET: "VGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWZvci1DaGVzczItSmF2YS1KV1Q=",
      SPRING_DATASOURCE_URL: "jdbc:h2:mem:chess2e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      SPRING_DATASOURCE_USERNAME: "sa",
      SPRING_DATASOURCE_PASSWORD: "",
      SPRING_FLYWAY_ENABLED: "false",
      SPRING_JPA_HIBERNATE_DDL_AUTO: "create-drop",
      SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA: "PUBLIC"
    }
  }
});

