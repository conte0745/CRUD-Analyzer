package dev.example.crudscan.ast;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.UnitTestBase;
import dev.example.crudscan.ast.ControllerScanner.ScanResult;
import dev.example.crudscan.model.Models.Endpoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ControllerScannerのテストクラス */
@DisplayName("ControllerScanner機能のテスト")
class ControllerScannerTest extends UnitTestBase {

  private ControllerScanner scanner;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    scanner = new ControllerScanner(tempDir);
  }

  @Test
  void testConstructor_ShouldCreateInstance() {
    // When
    ControllerScanner newScanner = new ControllerScanner(tempDir);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  void testScan_WithValidController_ShouldExtractEndpoints() throws Exception {
    // Given
    createTestControllerFile();

    // When
    ScanResult result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.endpoints()).isNotEmpty();

    List<Endpoint> endpoints = result.endpoints();
    assertThat(endpoints).hasSizeGreaterThanOrEqualTo(1);

    // エンドポイントが正しく抽出されることを確認
    boolean hasGetEndpoint =
        endpoints.stream()
            .anyMatch(e -> e.url().contains("/hello") && e.httpMethod().equals("GET"));
    assertThat(hasGetEndpoint).isTrue();
  }

  @Test
  void testScan_WithEmptyDirectory_ShouldReturnEmptyResult() throws Exception {
    // Given - 空のディレクトリ

    // When
    ScanResult result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.endpoints()).isEmpty();
    assertThat(result.calls()).isEmpty();
  }

  @Test
  void testScan_WithNonControllerClass_ShouldIgnore() throws Exception {
    // Given
    createNonControllerFile();

    // When
    ScanResult result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.endpoints()).isEmpty();
  }

  /** テスト用のコントローラーファイルを作成 */
  private void createTestControllerFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/test");
    Files.createDirectories(packageDir);

    String controllerContent =
        """
        package com.example.test;

        import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/test")
        public class TestController {

            @GetMapping("/hello")
            public String hello() {
                return "Hello, World!";
            }

            @PostMapping("/create")
            public String create(@RequestBody String data) {
                return "Created: " + data;
            }
        }
        """;

    Files.writeString(packageDir.resolve("TestController.java"), controllerContent);
  }

  /** コントローラーではないクラスファイルを作成 */
  private void createNonControllerFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/test");
    Files.createDirectories(packageDir);

    String serviceContent =
        """
        package com.example.test;

        import org.springframework.stereotype.Service;

        @Service
        public class TestService {

            public String process() {
                return "Processed";
            }
        }
        """;

    Files.writeString(packageDir.resolve("TestService.java"), serviceContent);
  }
}
