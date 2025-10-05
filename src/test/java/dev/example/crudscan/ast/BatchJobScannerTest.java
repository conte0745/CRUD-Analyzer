package dev.example.crudscan.ast;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.TestBase;
import dev.example.crudscan.model.Models.BatchJob;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** BatchJobScannerのテストクラス */
@DisplayName("BatchJobScanner機能のテスト")
class BatchJobScannerTest extends TestBase {

  private BatchJobScanner scanner;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    scanner = new BatchJobScanner(tempDir);
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    BatchJobScanner newScanner = new BatchJobScanner(tempDir);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  @DisplayName("存在しないディレクトリをスキャンした場合に空のリストが返されること")
  void testScan_WithNonExistentDirectory_ShouldReturnEmptyList() {
    // Given
    Path nonExistentDir = tempDir.resolve("non-existent");
    BatchJobScanner nonExistentScanner = new BatchJobScanner(nonExistentDir);

    // When
    List<BatchJob> result = nonExistentScanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("空のディレクトリをスキャンした場合に空のリストが返されること")
  void testScan_WithEmptyDirectory_ShouldReturnEmptyList() {
    // Given - 空のディレクトリ

    // When
    List<BatchJob> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("@Configuration付きバッチJobクラスが正常に検出されること")
  void testScan_WithBatchJobClass_ShouldDetectJob() throws Exception {
    // Given
    createBatchJobFile();

    // When
    List<BatchJob> result = scanner.scan();

    // Then
    assertThat(result).hasSize(1);
    BatchJob job = result.get(0);
    assertThat(job.className()).isEqualTo("com.example.batch.UserDataProcessJob");
    assertThat(job.jobName()).isEqualTo("UserDataProcessJob");
    assertThat(job.packageName()).isEqualTo("com.example.batch");
  }

  @Test
  @DisplayName("複数のバッチJobクラスが正常に検出されること")
  void testScan_WithMultipleBatchJobClasses_ShouldDetectAllJobs() throws Exception {
    // Given
    createMultipleBatchJobFiles();

    // When
    List<BatchJob> result = scanner.scan();

    // Then
    assertThat(result).hasSize(2);

    List<String> jobNames = result.stream().map(BatchJob::jobName).toList();
    assertThat(jobNames).containsExactlyInAnyOrder("DataImportJob", "ReportGenerationJob");

    List<String> classNames = result.stream().map(BatchJob::className).toList();
    assertThat(classNames).containsExactlyInAnyOrder("com.example.batch.DataImportJob",
        "com.example.batch.ReportGenerationJob");
  }

  @Test
  @DisplayName("@ConfigurationアノテーションがあってもJobで終わらない場合は検出されないこと")
  void testScan_WithNonJobConfiguration_ShouldNotDetect() throws Exception {
    // Given
    createNonJobConfigurationFile();

    // When
    List<BatchJob> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("インターフェースは検出されないこと")
  void testScan_WithInterface_ShouldNotDetect() throws Exception {
    // Given
    createInterfaceFile();

    // When
    List<BatchJob> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("パッケージ宣言がないクラスも正常に処理されること")
  void testScan_WithNoPackageDeclaration_ShouldHandleGracefully() throws Exception {
    // Given
    createNoPackageJobFile();

    // When
    List<BatchJob> result = scanner.scan();

    // Then
    assertThat(result).hasSize(1);
    BatchJob job = result.get(0);
    assertThat(job.className()).isEqualTo("SimpleJob");
    assertThat(job.jobName()).isEqualTo("SimpleJob");
    assertThat(job.packageName()).isEmpty();
  }

  @Test
  @DisplayName("不正なJavaファイルが適切に処理されること")
  void testScan_WithInvalidJavaFile_ShouldHandleGracefully() throws Exception {
    // Given
    createInvalidJavaFile();

    // When & Then
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  /** バッチJobファイルを作成 */
  private void createBatchJobFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/batch");
    createDirectories(packageDir);

    String content = """
        package com.example.batch;

        import org.springframework.context.annotation.Configuration;

        @Configuration("UserDataProcessJob")
        public class UserDataProcessJob {
            // バッチJob実装
        }
        """;

    Files.writeString(packageDir.resolve("UserDataProcessJob.java"), content);
  }

  /** 複数のバッチJobファイルを作成 */
  private void createMultipleBatchJobFiles() throws Exception {
    Path packageDir = tempDir.resolve("com/example/batch");
    createDirectories(packageDir);

    String content1 = """
        package com.example.batch;

        import org.springframework.context.annotation.Configuration;

        @Configuration("DataImportJob")
        public class DataImportJob {
            // データインポートJob
        }
        """;

    String content2 = """
        package com.example.batch;

        import org.springframework.context.annotation.Configuration;

        @Configuration("ReportGenerationJob")
        public class ReportGenerationJob {
            // レポート生成Job
        }
        """;

    Files.writeString(packageDir.resolve("DataImportJob.java"), content1);
    Files.writeString(packageDir.resolve("ReportGenerationJob.java"), content2);
  }

  /** Job以外の@Configurationファイルを作成 */
  private void createNonJobConfigurationFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/config");
    createDirectories(packageDir);

    String content = """
        package com.example.config;

        import org.springframework.context.annotation.Configuration;

        @Configuration("DatabaseConfig")
        public class DatabaseConfig {
            // 設定クラス
        }
        """;

    Files.writeString(packageDir.resolve("DatabaseConfig.java"), content);
  }

  /** インターフェースファイルを作成 */
  private void createInterfaceFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/batch");
    createDirectories(packageDir);

    String content = """
        package com.example.batch;

        import org.springframework.context.annotation.Configuration;

        @Configuration("BatchJobInterface")
        public interface BatchJobInterface {
            // インターフェース
        }
        """;

    Files.writeString(packageDir.resolve("BatchJobInterface.java"), content);
  }

  /** パッケージ宣言なしのJobファイルを作成 */
  private void createNoPackageJobFile() throws Exception {
    String content = """
        import org.springframework.context.annotation.Configuration;

        @Configuration("SimpleJob")
        public class SimpleJob {
            // シンプルなJob
        }
        """;

    Files.writeString(tempDir.resolve("SimpleJob.java"), content);
  }

  /** 不正なJavaファイルを作成 */
  private void createInvalidJavaFile() throws Exception {
    String content = """
        This is not valid Java code
        @Configuration("InvalidJob")
        public class InvalidJob {
            // 不正な構文
        """;

    Files.writeString(tempDir.resolve("InvalidJob.java"), content);
  }
}
