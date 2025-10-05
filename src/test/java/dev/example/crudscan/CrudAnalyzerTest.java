package dev.example.crudscan;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** CrudAnalyzerのテストクラス */
@DisplayName("CrudAnalyzer機能のテスト")
class CrudAnalyzerTest {

  private CrudAnalyzer analyzer;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    analyzer = new CrudAnalyzer();
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    CrudAnalyzer newAnalyzer = new CrudAnalyzer();

    // Then
    assertThat(newAnalyzer).isNotNull();
  }

  @Test
  @DisplayName("有効なディレクトリで解析が正常に完了すること")
  void testAnalyze_WithValidDirectories_ShouldCompleteSuccessfully() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // When & Then
    assertThatCode(() -> analyzer.analyze(srcDir, resourcesDir, outputDir))
        .doesNotThrowAnyException();
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // When & Then
    assertThatCode(() -> analyzer.analyze(srcDir, resourcesDir, outputDir))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("テスト用Javaファイルが正常に処理されること")
  void testAnalyze_WithTestJavaFile_ShouldProcessSuccessfully() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // テスト用のJavaファイルを作成
    createTestControllerFile(srcDir);

    // When & Then
    assertThatCode(() -> analyzer.analyze(srcDir, resourcesDir, outputDir))
        .doesNotThrowAnyException();

    // 出力ファイルが作成されることを確認
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
  }

  @Test
  @DisplayName("テスト用XMLファイルが正常に処理されること")
  void testAnalyze_WithTestXmlFile_ShouldProcessSuccessfully() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // テスト用のXMLファイルを作成
    createTestXmlFile(resourcesDir);

    // When & Then
    assertThatCode(() -> analyzer.analyze(srcDir, resourcesDir, outputDir))
        .doesNotThrowAnyException();
  }

  /** テスト用のコントローラーファイルを作成 */
  private void createTestControllerFile(Path srcDir) throws Exception {
    Path packageDir = srcDir.resolve("com/example/test");
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

  /** テスト用のXMLファイルを作成 */
  private void createTestXmlFile(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    Files.createDirectories(mapperDir);

    String xmlContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
        <mapper namespace="com.example.test.TestMapper">
            <select id="findAll" resultType="String">
                SELECT name FROM test_table
            </select>
        </mapper>
        """;

    Files.writeString(mapperDir.resolve("TestMapper.xml"), xmlContent);
  }

  @Test
  @DisplayName("空のソースディレクトリでも正常に処理されること")
  void testAnalyze_WithEmptySourceDir_ShouldHandleGracefully() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // When & Then
    assertThatCode(() -> analyzer.analyze(srcDir, resourcesDir, outputDir))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("複合的なプロジェクト構造が正常に処理されること")
  void testAnalyze_WithComplexProjectStructure_ShouldProcessSuccessfully() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // 複数のファイルを作成
    createTestControllerFile(srcDir);
    createTestServiceFile(srcDir);
    createTestXmlFile(resourcesDir);
    createTestBatchJobFile(srcDir);

    // When & Then
    assertThatCode(() -> analyzer.analyze(srcDir, resourcesDir, outputDir))
        .doesNotThrowAnyException();

    // 出力ファイルが作成されることを確認
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
    // CRUDリンクが生成されない場合、JSONとPUMLファイルは作成されない可能性がある
  }

  /** テスト用のサービスファイルを作成 */
  private void createTestServiceFile(Path srcDir) throws Exception {
    Path packageDir = srcDir.resolve("com/example/test");
    Files.createDirectories(packageDir);

    String serviceContent =
        """
        package com.example.test;

        import org.springframework.stereotype.Service;

        @Service
        public class TestService {

            public String processData(String data) {
                return "Processed: " + data;
            }
        }
        """;

    Files.writeString(packageDir.resolve("TestService.java"), serviceContent);
  }

  /** テスト用のバッチJobファイルを作成 */
  private void createTestBatchJobFile(Path srcDir) throws Exception {
    Path packageDir = srcDir.resolve("com/example/batch");
    Files.createDirectories(packageDir);

    String batchJobContent =
        """
        package com.example.batch;

        import org.springframework.context.annotation.Configuration;

        @Configuration("TestDataProcessJob")
        public class TestDataProcessJob {

            public void execute() {
                // バッチ処理
            }
        }
        """;

    Files.writeString(packageDir.resolve("TestDataProcessJob.java"), batchJobContent);
  }
}
