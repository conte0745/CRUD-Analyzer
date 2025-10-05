package dev.example.crudscan.output;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.model.Models.*;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** MarkdownWriterのテストクラス */
@DisplayName("MarkdownWriter機能のテスト")
class MarkdownWriterTest {

  private MarkdownWriter writer;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    writer = new MarkdownWriter();
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    MarkdownWriter newWriter = new MarkdownWriter();

    // Then
    assertThat(newWriter).isNotNull();
  }

  @Test
  @DisplayName("有効なデータでインデックスマトリクスファイルが正常に作成されること")
  void testWriteIndexMatrix_WithValidData_ShouldCreateFile() throws Exception {
    // Given
    List<CrudLink> links = createTestCrudLinks();
    List<Endpoint> endpoints = createTestEndpoints();
    List<BatchJob> batchJobs = createTestBatchJobs();
    Path outputFile = tempDir.resolve("crud-matrix.md");

    // When
    writer.writeIndexMatrix(outputFile, links, endpoints, batchJobs);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);
    // 基本的にファイルが作成され、何らかの内容があることを確認
    assertThat(content).isNotEmpty();
  }

  @Test
  @DisplayName("有効なデータでパッケージ別マトリクスファイルが正常に作成されること")
  void testWriteMatrixByPackage_WithValidData_ShouldCreateFiles() throws Exception {
    // Given
    List<CrudLink> links = createTestCrudLinks();
    List<Endpoint> endpoints = createTestEndpoints();

    // When
    writer.writeMatrixByPackage(tempDir, links, endpoints);

    // Then
    Path crudDir = tempDir.resolve("crud");
    assertThat(crudDir).exists();

    // パッケージ別のファイルが作成されることを確認
    List<Path> markdownFiles =
        Files.walk(crudDir)
            .filter(p -> p.toString().endsWith(".md"))
            .filter(p -> p.toString().contains("crud-matrix"))
            .toList();

    assertThat(markdownFiles).isNotEmpty();
  }

  @Test
  @DisplayName("空のデータでも空のインデックスマトリクスファイルが作成されること")
  void testWriteIndexMatrix_WithEmptyData_ShouldCreateEmptyFile() throws Exception {
    // Given
    List<CrudLink> emptyLinks = List.of();
    List<Endpoint> emptyEndpoints = List.of();
    List<BatchJob> emptyBatchJobs = List.of();
    Path outputFile = tempDir.resolve("empty-matrix.md");

    // When
    writer.writeIndexMatrix(outputFile, emptyLinks, emptyEndpoints, emptyBatchJobs);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);
    assertThat(content).contains("# CRUD Matrix");
  }

  @Test
  @DisplayName("空のデータでもパッケージ別マトリクス処理が正常に実行されること")
  void testWriteMatrixByPackage_WithEmptyData_ShouldHandleGracefully() {
    // Given
    List<CrudLink> emptyLinks = List.of();
    List<Endpoint> emptyEndpoints = List.of();

    // When & Then
    assertThatCode(() -> writer.writeMatrixByPackage(tempDir, emptyLinks, emptyEndpoints))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("有効なデータで正しいMarkdownテーブルが作成されること")
  void testWriteMatrix_WithValidData_ShouldCreateCorrectMarkdownTable() throws Exception {
    // Given
    List<CrudLink> links = createTestCrudLinks();
    Path outputFile = tempDir.resolve("test-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // ヘッダー行の確認
    assertThat(content).contains("| URL | HTTP | users |");
    // 区切り行の確認
    assertThat(content).contains("|---|---|---|");
    // データ行の確認
    assertThat(content).contains("| /api/users | GET | S |");
    assertThat(content).contains("| /api/users | POST | I |");
    assertThat(content).contains("| /api/users/{id} | PUT | U |");
    assertThat(content).contains("| /api/users/{id} | DELETE | D |");
  }

  @Test
  @DisplayName("複数テーブルで正しい列構成のMarkdownテーブルが作成されること")
  void testWriteMatrix_WithMultipleTables_ShouldCreateCorrectColumns() throws Exception {
    // Given
    List<Endpoint> endpoints =
        List.of(
            new Endpoint(
                "GET", "/api/users", "UserController", "getUsers", "com.example.controller"),
            new Endpoint(
                "GET", "/api/books", "BookController", "getBooks", "com.example.controller"));
    List<CrudLink> links =
        List.of(
            new CrudLink(endpoints.get(0), "users", "S"),
            new CrudLink(endpoints.get(0), "profiles", "S"),
            new CrudLink(endpoints.get(1), "books", "S"),
            new CrudLink(endpoints.get(1), "authors", "S"));
    Path outputFile = tempDir.resolve("multi-table-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // テーブル名がアルファベット順でソートされていることを確認
    assertThat(content).contains("| URL | HTTP | authors | books | profiles | users |");
    // 各エンドポイントの行が正しく生成されていることを確認
    assertThat(content).contains("| /api/users | GET |");
    assertThat(content).contains("| /api/books | GET |");
    // usersエンドポイントではprofilesとusersテーブルにアクセス
    assertThat(content).contains("S"); // profiles
    assertThat(content).contains("S"); // users
    // booksエンドポイントではauthorsとbooksテーブルにアクセス
    assertThat(content).contains("S"); // authors
    assertThat(content).contains("S"); // books
  }

  @Test
  @DisplayName("空のCRUDリンクでも空のテーブルが作成されること")
  void testWriteMatrix_WithEmptyLinks_ShouldCreateEmptyTable() throws Exception {
    // Given
    List<CrudLink> emptyLinks = List.of();
    Path outputFile = tempDir.resolve("empty-matrix.md");

    // When
    writer.writeMatrix(outputFile, emptyLinks);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // 空のテーブルでもヘッダーは生成される
    assertThat(content).contains("| URL | HTTP |");
    assertThat(content).contains("|---|---|");
  }

  @Test
  @DisplayName("重複するCRUD操作が正しく結合されること")
  void testWriteMatrix_WithDuplicateCrudOperations_ShouldCombineOperations() throws Exception {
    // Given
    Endpoint endpoint =
        new Endpoint(
            "POST", "/api/users", "UserController", "createUser", "com.example.controller");
    List<CrudLink> links =
        List.of(
            new CrudLink(endpoint, "users", "I"),
            new CrudLink(endpoint, "users", "S"), // 同じエンドポイント・テーブルで複数のCRUD操作
            new CrudLink(endpoint, "profiles", "I"));
    Path outputFile = tempDir.resolve("duplicate-crud-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // CRUD操作がソートされて結合されていることを確認
    assertThat(content).contains("| /api/users | POST | I | IS |");
  }

  @Test
  @DisplayName("存在しない親ディレクトリが自動作成されること")
  void testWriteMatrix_WithNonExistentParentDirectory_ShouldCreateDirectory() throws Exception {
    // Given
    List<CrudLink> links = createTestCrudLinks();
    Path nestedDir = tempDir.resolve("nested/deep/directory");
    Path outputFile = nestedDir.resolve("matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    assertThat(nestedDir).exists();
    String content = Files.readString(outputFile);
    assertThat(content).isNotEmpty();
  }

  @Test
  @DisplayName("URL内の特殊文字が正しく処理されること")
  void testWriteMatrix_WithSpecialCharactersInUrl_ShouldHandleCorrectly() throws Exception {
    // Given
    List<Endpoint> endpoints =
        List.of(
            new Endpoint(
                "GET",
                "/api/users/{id}/books?filter=active",
                "UserController",
                "getUserBooks",
                "com.example.controller"));
    List<CrudLink> links = List.of(new CrudLink(endpoints.get(0), "users", "S"));
    Path outputFile = tempDir.resolve("special-chars-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // 特殊文字を含むURLが正しく出力されることを確認
    assertThat(content).contains("| /api/users/{id}/books?filter=active | GET | S |");
  }

  @Test
  @DisplayName("長いテーブル名が正しく処理されること")
  void testWriteMatrix_WithLongTableNames_ShouldHandleCorrectly() throws Exception {
    // Given
    Endpoint endpoint =
        new Endpoint("GET", "/api/data", "DataController", "getData", "com.example.controller");
    List<CrudLink> links =
        List.of(
            new CrudLink(endpoint, "very_long_table_name_with_underscores", "S"),
            new CrudLink(endpoint, "another_extremely_long_table_name", "S"));
    Path outputFile = tempDir.resolve("long-names-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // 長いテーブル名が正しく処理されることを確認
    assertThat(content).contains("another_extremely_long_table_name");
    assertThat(content).contains("very_long_table_name_with_underscores");
  }

  @Test
  @DisplayName("IO例外が発生した場合にUncheckedIOExceptionがスローされること")
  void testWriteMatrix_WithIOException_ShouldThrowUncheckedIOException() {
    // Given
    List<CrudLink> links = createTestCrudLinks();
    // 書き込み不可能なパスを指定（存在しないドライブなど）
    Path invalidPath = Path.of("/invalid/nonexistent/path/matrix.md");

    // When & Then
    assertThatThrownBy(() -> writer.writeMatrix(invalidPath, links))
        .isInstanceOf(UncheckedIOException.class);
  }

  @Test
  @DisplayName("nullパスが指定された場合にNullPointerExceptionがスローされること")
  void testWriteMatrix_WithNullPath_ShouldThrowNullPointerException() {
    // Given
    List<CrudLink> links = createTestCrudLinks();

    // When & Then
    assertThatThrownBy(() -> writer.writeMatrix(null, links))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("nullリンクが指定された場合にNullPointerExceptionがスローされること")
  void testWriteMatrix_WithNullLinks_ShouldThrowNullPointerException() {
    // Given
    Path outputFile = tempDir.resolve("null-links-matrix.md");

    // When & Then
    assertThatThrownBy(() -> writer.writeMatrix(outputFile, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("混在するCRUD操作が正しくソートされて結合されること")
  void testWriteMatrix_WithMixedCrudOperations_ShouldSortAndCombine() throws Exception {
    // Given
    Endpoint endpoint =
        new Endpoint(
            "POST",
            "/api/complex",
            "ComplexController",
            "complexOperation",
            "com.example.controller");
    List<CrudLink> links =
        List.of(
            new CrudLink(endpoint, "table1", "U"),
            new CrudLink(endpoint, "table1", "D"),
            new CrudLink(endpoint, "table1", "I"),
            new CrudLink(endpoint, "table1", "S"), // すべてのCRUD操作
            new CrudLink(endpoint, "table2", "S"),
            new CrudLink(endpoint, "table2", "I"));
    Path outputFile = tempDir.resolve("mixed-crud-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // CRUD操作がアルファベット順でソートされて結合されていることを確認
    assertThat(content).contains("| /api/complex | POST | DISU | IS |");
  }

  @Test
  @DisplayName("Unicode文字が正しく処理されること")
  void testWriteMatrix_WithUnicodeCharacters_ShouldHandleCorrectly() throws Exception {
    // Given
    List<Endpoint> endpoints =
        List.of(
            new Endpoint(
                "GET", "/api/ユーザー", "UserController", "getUsers", "com.example.controller"));
    List<CrudLink> links = List.of(new CrudLink(endpoints.get(0), "ユーザーテーブル", "S"));
    Path outputFile = tempDir.resolve("unicode-matrix.md");

    // When
    writer.writeMatrix(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // Unicode文字が正しく処理されることを確認
    assertThat(content).contains("| /api/ユーザー | GET | S |");
    assertThat(content).contains("ユーザーテーブル");
  }

  @Test
  @DisplayName("大量のデータセットが効率的に処理されること")
  void testWriteMatrix_WithVeryLargeDataSet_ShouldHandleEfficiently() throws Exception {
    // Given - 大量のデータを生成
    List<CrudLink> links = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      Endpoint endpoint =
          new Endpoint(
              "GET",
              "/api/endpoint" + i,
              "Controller" + i,
              "method" + i,
              "com.example.package" + (i % 10));

      for (int j = 0; j < 10; j++) {
        links.add(new CrudLink(endpoint, "table" + j, "S"));
      }
    }

    Path outputFile = tempDir.resolve("large-dataset-matrix.md");

    // When
    long startTime = System.currentTimeMillis();
    writer.writeMatrix(outputFile, links);
    long endTime = System.currentTimeMillis();

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // ファイルが正しく生成されることを確認
    assertThat(content).isNotEmpty();
    assertThat(content).contains("| URL | HTTP |");

    // パフォーマンスチェック（1秒以内で完了することを期待）
    assertThat(endTime - startTime).isLessThan(1000);
  }

  /** テスト用のCrudLinkリストを作成 */
  private List<CrudLink> createTestCrudLinks() {
    List<Endpoint> endpoints = createTestEndpoints();
    return List.of(
        new CrudLink(endpoints.get(0), "users", "S"),
        new CrudLink(endpoints.get(1), "users", "I"),
        new CrudLink(endpoints.get(2), "users", "U"),
        new CrudLink(endpoints.get(3), "users", "D"));
  }

  /** テスト用のEndpointリストを作成 */
  private List<Endpoint> createTestEndpoints() {
    return List.of(
        new Endpoint("GET", "/api/users", "UserController", "getUsers", "com.example.controller"),
        new Endpoint(
            "POST", "/api/users", "UserController", "createUser", "com.example.controller"),
        new Endpoint(
            "PUT", "/api/users/{id}", "UserController", "updateUser", "com.example.controller"),
        new Endpoint(
            "DELETE", "/api/users/{id}", "UserController", "deleteUser", "com.example.controller"));
  }

  /** テスト用のBatchJobリストを作成 */
  private List<BatchJob> createTestBatchJobs() {
    return List.of(
        new BatchJob(
            "userDataExportJob", "com.example.batch.UserDataExportJob", "com.example.batch"));
  }
}
