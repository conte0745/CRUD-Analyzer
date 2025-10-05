package dev.example.crudscan.mybatis;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.UnitTestBase;
import dev.example.crudscan.model.Models.SqlMapping;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** MyBatisAnnotationScannerのテストクラス */
@DisplayName("MyBatisAnnotationScanner機能のテスト")
class MyBatisAnnotationScannerTest extends UnitTestBase {

  private MyBatisAnnotationScanner scanner;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    scanner = new MyBatisAnnotationScanner(tempDir);
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    MyBatisAnnotationScanner newScanner = new MyBatisAnnotationScanner(tempDir);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  @DisplayName("アノテーション付きマッパーからSQLマッピングが抽出されること")
  void testScan_WithAnnotatedMapper_ShouldExtractSqlMappings() throws Exception {
    // Given
    createAnnotatedMapperFile();

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotEmpty();

    // @Selectアノテーションから抽出されたマッピングを確認
    SqlMapping selectMapping =
        result.stream().filter(m -> m.mapperMethod().equals("findById")).findFirst().orElse(null);

    assertThat(selectMapping).isNotNull();
    assertThat(selectMapping.mapperClass()).contains("UserAnnotationMapper");
    assertThat(selectMapping.op()).isEqualTo("SELECT");
    assertThat(selectMapping.tables()).contains("users");
  }

  @Test
  @DisplayName("複数のアノテーションを含むマッパーから全てのマッピングが抽出されること")
  void testScan_WithMultipleAnnotations_ShouldExtractAllMappings() throws Exception {
    // Given
    createCompleteAnnotatedMapperFile();

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).hasSizeGreaterThanOrEqualTo(4); // SELECT, INSERT, UPDATE, DELETE

    // 各操作種別が含まれることを確認
    List<String> operations = result.stream().map(SqlMapping::op).distinct().toList();

    assertThat(operations).containsAnyOf("SELECT", "INSERT", "UPDATE", "DELETE");
  }

  @Test
  @DisplayName("空のディレクトリをスキャンした場合に空のリストが返されること")
  void testScan_WithEmptyDirectory_ShouldReturnEmptyList() throws Exception {
    // Given - 空のディレクトリ

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("@Mapperアノテーションのないクラスが無視されること")
  void testScan_WithNonMapperClass_ShouldIgnore() throws Exception {
    // Given
    createNonMapperFile();

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("SQLアノテーションのないマッパーで空のリストが返されること")
  void testScan_WithMapperWithoutAnnotations_ShouldReturnEmpty() throws Exception {
    // Given
    createMapperWithoutAnnotations();

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  /** アノテーション付きマッパーファイルを作成 */
  private void createAnnotatedMapperFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/mapper");
    copyTestResource(
        "mybatis/MyBatisAnnotationScannerTest/UserAnnotationMapper.java.txt",
        packageDir,
        "UserAnnotationMapper.java");
  }

  /** 完全なCRUD操作を含むアノテーション付きマッパーファイルを作成 */
  private void createCompleteAnnotatedMapperFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/mapper");
    copyTestResource(
        "mybatis/MyBatisAnnotationScannerTest/CompleteAnnotationMapper.java.txt",
        packageDir,
        "CompleteAnnotationMapper.java");
  }

  /** `@Mapper`アノテーションのないクラスファイルを作成 */
  private void createNonMapperFile() throws Exception {
    Path packageDir = tempDir.resolve("com/example/service");
    copyTestResource(
        "mybatis/MyBatisAnnotationScannerTest/UserService.java.txt",
        packageDir,
        "UserService.java");
  }

  /** SQLアノテーションのないマッパーファイルを作成 */
  private void createMapperWithoutAnnotations() throws Exception {
    Path packageDir = tempDir.resolve("com/example/mapper");
    copyTestResource(
        "mybatis/MyBatisAnnotationScannerTest/EmptyMapper.java.txt",
        packageDir,
        "EmptyMapper.java");
  }
}
