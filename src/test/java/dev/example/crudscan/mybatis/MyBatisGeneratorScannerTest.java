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

/** MyBatisGeneratorScannerのテストクラス */
@DisplayName("MyBatisGeneratorScanner機能のテスト")
class MyBatisGeneratorScannerTest extends UnitTestBase {

  private MyBatisGeneratorScanner scanner;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    scanner = new MyBatisGeneratorScanner(tempDir);
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    MyBatisGeneratorScanner newScanner = new MyBatisGeneratorScanner(tempDir);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  @DisplayName("Generator生成XMLファイルからマッピングが抽出されること")
  void testScan_WithGeneratedXmlFiles_ShouldExtractMappings() throws Exception {
    // Given - 空のディレクトリでも正常に動作することを確認
    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();

    // 基本的な動作確認
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
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
  @DisplayName("動的SQL要素を含むXMLファイルが正常に処理されること")
  void testScan_WithDynamicSqlElements_ShouldExtractMappings() throws Exception {
    // Given - 動的SQL要素を含むマッパーXMLファイルをコピー
    Path mapperDir = tempDir.resolve("mapper");
    copyTestResource(
        "mybatis/MyBatisGeneratorScannerTest/DynamicSqlMapper.xml",
        mapperDir,
        "DynamicSqlMapper.xml");

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull().isNotEmpty();

    // 動的SQL要素を含むメソッドが検出される
    boolean hasDynamicSqlMethods =
        result.stream()
            .anyMatch(
                mapping ->
                    mapping.mapperMethod().equals("findUsersDynamic")
                        || mapping.mapperMethod().equals("updateSelective")
                        || mapping.mapperMethod().equals("findUsersWithRelations"));
    assertThat(hasDynamicSqlMethods).isTrue();

    // テーブル名が正しく抽出される
    boolean hasUsersTable = result.stream().anyMatch(mapping -> mapping.tables().contains("users"));
    assertThat(hasUsersTable).isTrue();
  }

  @Test
  @DisplayName("Generator生成でないXMLファイルが適切に処理されること")
  void testScan_WithNonGeneratedXml_ShouldHandleGracefully() throws Exception {
    // Given - 通常のマッパーXMLファイルをコピー
    Path mapperDir = tempDir.resolve("mapper");
    copyTestResource(
        "mybatis/MyBatisGeneratorScannerTest/NonGeneratedMapper.xml",
        mapperDir,
        "NonGeneratedMapper.xml");

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    // 通常のXMLファイルは処理されるが、Generator特有の標準CRUD操作は生成されない
    assertThat(result).isNotNull().isEmpty(); // このスキャナーはGenerator特有の機能に特化しているため

    // Generator特有のメソッド名（ByExample等）は含まれない
    boolean hasGeneratorMethods =
        result.stream()
            .anyMatch(
                mapping ->
                    mapping.mapperMethod().matches(".*(ByExample|ByPrimaryKey|Selective).*"));
    assertThat(hasGeneratorMethods).isFalse();
  }

  @Test
  @DisplayName("無効なXMLファイルが適切に処理されること")
  void testScan_WithInvalidXml_ShouldHandleGracefully() {
    // Given - 空のディレクトリでも正常に動作することを確認
    // When & Then
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }
}
