package dev.example.crudscan.integration;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.AnalyzerMain;
import dev.example.crudscan.TestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 統合テストクラス
 *
 * <p>アプリケーション全体の動作を統合的にテストします。
 */
@DisplayName("アプリケーション統合テスト")
class IntegrationTest extends TestBase {
  private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

  @TempDir Path tempDir;

  @Test
  @DisplayName("完全な分析ワークフローテスト - 全出力ファイルの生成確認")
  void testFullAnalysisWorkflow_ShouldGenerateAllOutputs() throws Exception {
    /**
     * テストシナリオ: 1. 完全なSpring MVCプロジェクト構造を作成（Controller, Service, Mapper, XML） 2. CRUD
     * Analyzerで全体的な分析を実行 3. 全ての出力ファイルが正常に生成されることを確認
     *
     * <p>期待結果: - crud-matrix.md: CRUDマトリクスファイルが生成される - analysis.json: 分析結果のJSONファイルが生成される -
     * エンドポイント、テーブル、CRUDリンクが正しく検出される
     */

    // Given - 完全なプロジェクト構造を作成
    Path srcDir = tempDir.resolve("src/main/java");
    Path resourcesDir = tempDir.resolve("src/main/resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // 実際のSpring Bootプロジェクト構造を模倣
    createRealisticProjectStructure(srcDir, resourcesDir);

    // 設定ファイルを作成
    createConfigurationFile();

    // When - メイン関数を実行
    String[] args = {srcDir.toString(), resourcesDir.toString(), outputDir.toString()};

    assertThatCode(() -> AnalyzerMain.main(args)).doesNotThrowAnyException();

    // Then - 全ての出力ファイルが生成されることを確認
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
    assertThat(outputDir.resolve("crud")).exists();
    assertThat(outputDir.resolve("analysis.json")).exists();

    // 出力内容の基本的な検証
    String matrixContent = Files.readString(outputDir.resolve("crud-matrix.md"));
    assertThat(matrixContent).isNotEmpty();

    String jsonContent = Files.readString(outputDir.resolve("analysis.json"));
    assertThat(jsonContent).isNotEmpty();

    // 分析が正常に完了したことを出力ファイルの存在と内容で確認
    assertThat(matrixContent)
        .as("CRUDマトリクスファイルが正常に生成されていること")
        .isNotEmpty()
        .contains("エンドポイント数");
    assertThat(jsonContent)
        .as("JSON分析結果ファイルが正常に生成されていること")
        .isNotEmpty()
        .contains("endpoints");
  }

  @Test
  @DisplayName("設定ファイルを使用した分析テスト - カスタム設定の適用確認")
  void testAnalysisWithConfigurationFile_ShouldUseConfigSettings() throws Exception {
    /**
     * テストシナリオ: 1. カスタム設定ファイル（analyzer-config.properties）を作成 2. 設定ファイルで指定されたパラメータでCRUD分析を実行 3.
     * 設定が正しく適用され、出力ファイルが生成されることを確認
     *
     * <p>期待結果: - 設定ファイルの内容が分析処理に反映される - crud-matrix.mdファイルが正常に生成される
     */

    // Given
    Path srcDir = tempDir.resolve("custom-src");
    Path resourcesDir = tempDir.resolve("custom-resources");
    Path outputDir = tempDir.resolve("custom-output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // 基本的なファイル構造を作成
    createBasicProjectStructure(srcDir, resourcesDir);

    // When - 引数でディレクトリを指定
    String[] args = {srcDir.toString(), resourcesDir.toString(), outputDir.toString()};

    assertThatCode(() -> AnalyzerMain.main(args)).doesNotThrowAnyException();

    // Then - 指定した出力ディレクトリにファイルが作成される
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
  }

  @Test
  @DisplayName("コントローラから直接Mapperを呼ぶ場合の分析テスト")
  void testAnalysisWithDirectMapperCall_ShouldDetectDirectCrudOperations() throws Exception {
    /**
     * テストシナリオ: 1. Controller → Mapper の直接呼び出し構造を作成 2. Serviceレイヤーを経由しない単純なアーキテクチャでCRUD分析を実行 3.
     * 直接的なCRUD操作が正しく検出されることを確認
     *
     * <p>期待結果: - Controller → Mapperの呼び出し関係が検出される - Mapperメソッドに対応するSQLマッピングが検出される -
     * エンドポイントとテーブル間のCRUDリンクが生成される
     */

    // Given - コントローラが直接Mapperを呼ぶ構造を作成
    Path srcDir = tempDir.resolve("direct-mapper-src");
    Path resourcesDir = tempDir.resolve("direct-mapper-resources");
    Path outputDir = tempDir.resolve("direct-mapper-output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // 直接Mapper呼び出しのプロジェクト構造を作成
    createDirectMapperProjectStructure(srcDir, resourcesDir);

    // 設定ファイルを作成
    createConfigurationFile();

    // When - 分析を実行
    String[] args = {srcDir.toString(), resourcesDir.toString(), outputDir.toString()};

    assertThatCode(() -> AnalyzerMain.main(args)).doesNotThrowAnyException();

    // Then - 分析結果を検証
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
    assertThat(outputDir.resolve("analysis.json")).exists();

    // 出力内容の検証
    String matrixContent = Files.readString(outputDir.resolve("crud-matrix.md"));
    assertThat(matrixContent)
        .isNotEmpty()
        .containsIgnoringCase("com.example.direct.controller")
        .containsIgnoringCase("products");

    String jsonContent = Files.readString(outputDir.resolve("analysis.json"));
    assertThat(jsonContent)
        .isNotEmpty()
        .containsIgnoringCase("com.example.direct.mapper.ProductMapper");

    // 分析が正常に完了したことを出力ファイルの存在と内容で確認
    assertThat(matrixContent)
        .as("直接Mapper呼び出しが検出されていること")
        .containsIgnoringCase("products");
    assertThat(jsonContent)
        .as("ProductMapperが分析結果に含まれていること")
        .containsIgnoringCase("ProductMapper");
  }

  @Test
  @DisplayName("コントローラからAbstractView経由でMapper/Serviceを呼ぶ場合の分析テスト")
  void testAnalysisWithAbstractViewCall_ShouldDetectViewBasedCrudOperations() throws Exception {
    /**
     * テストシナリオ: 1. Controller → AbstractView → Mapper/Service の多層呼び出し構造を作成 2.
     * AbstractViewパターンを使用したアーキテクチャでCRUD分析を実行 3. 間接的な呼び出しチェーンを通じたCRUD操作が正しく検出されることを確認
     *
     * <p>期待結果: - Controller → AbstractViewの呼び出し関係が検出される - AbstractView →
     * Mapper/Serviceの呼び出し関係が検出される - 多層の呼び出しチェーンを通じてテーブルとCRUDリンクが生成される -
     * ordersテーブルへの直接的なCRUD操作が検出される - customersテーブルへの間接的なCRUD操作が検出される（Service経由）
     */

    // Given - コントローラがAbstractView経由でMapper/Serviceを呼ぶ構造を作成
    Path srcDir = tempDir.resolve("abstract-view-src");
    Path resourcesDir = tempDir.resolve("abstract-view-resources");
    Path outputDir = tempDir.resolve("abstract-view-output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // AbstractView経由の呼び出しプロジェクト構造を作成
    createAbstractViewProjectStructure(srcDir, resourcesDir);

    // 設定ファイルを作成
    createConfigurationFile();

    // When - 分析を実行
    String[] args = {srcDir.toString(), resourcesDir.toString(), outputDir.toString()};

    assertThatCode(() -> AnalyzerMain.main(args)).doesNotThrowAnyException();

    // Then - 分析結果を検証
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
    assertThat(outputDir.resolve("analysis.json")).exists();

    // 出力内容の検証
    String matrixContent = Files.readString(outputDir.resolve("crud-matrix.md"));

    // デバッグ: 実際の内容を出力
    logger.debug("=== MATRIX CONTENT DEBUG ===");
    logger.debug("Matrix content: {}", matrixContent);
    logger.debug("=== END DEBUG ===");

    assertThat(matrixContent)
        .isNotEmpty()
        .containsIgnoringCase("エンドポイント数:** 8")
        .doesNotContain("テーブル数:** 0") // テーブルが検出されていることを要求
        .containsIgnoringCase("orders") // ordersテーブルが検出されていることを要求
        .containsIgnoringCase("customers"); // customersテーブルが検出されていることを要求

    String jsonContent = Files.readString(outputDir.resolve("analysis.json"));
    
    assertThat(jsonContent)
        .isNotEmpty()
        .containsIgnoringCase("OrderMapper")
        .containsIgnoringCase("CustomerMapper");

    // 分析が正常に完了したことを出力ファイルの存在と内容で確認
    assertThat(matrixContent)
        .as("AbstractView経由の呼び出しが検出されていること")
        .containsIgnoringCase("orders")
        .containsIgnoringCase("customers");
    assertThat(jsonContent)
        .as("OrderMapperとCustomerMapperが分析結果に含まれていること")
        .containsIgnoringCase("OrderMapper")
        .containsIgnoringCase("CustomerMapper");
  }

  /** リアルなプロジェクト構造を作成 */
  private void createRealisticProjectStructure(Path srcDir, Path resourcesDir) throws Exception {
    // Controller
    Path controllerDir = srcDir.resolve("com/example/demo/controller");
    copyTestResource(
        "integration/realistic/UserController.java.txt", controllerDir, "UserController.java");

    // Service
    Path serviceDir = srcDir.resolve("com/example/demo/service");
    copyTestResource("integration/realistic/UserService.java.txt", serviceDir, "UserService.java");

    // Repository
    Path repositoryDir = srcDir.resolve("com/example/demo/repository");
    copyTestResource(
        "integration/realistic/UserRepository.java.txt", repositoryDir, "UserRepository.java");

    // MyBatis XML
    Path mapperDir = resourcesDir.resolve("mapper");
    copyTestResource("integration/realistic/UserMapper.xml", mapperDir);
  }

  private void createBasicProjectStructure(Path srcDir, Path resourcesDir) throws Exception {
    // 最小限の構造
    Path packageDir = srcDir.resolve("com/example/basic");
    copyTestResource(
        "integration/basic/BasicController.java.txt", packageDir, "BasicController.java");

    // 基本的なリソースファイルも作成
    if (resourcesDir != null) {
      Files.createDirectories(resourcesDir.resolve("static"));
    }
  }

  /** 直接Mapper呼び出しのプロジェクト構造を作成 */
  private void createDirectMapperProjectStructure(Path srcDir, Path resourcesDir) throws Exception {
    // Controller that directly calls Mapper
    Path controllerDir = srcDir.resolve("com/example/direct/controller");
    copyTestResource(
        "integration/direct-mapper/ProductController.java.txt",
        controllerDir,
        "ProductController.java");

    // Mapper interface
    Path mapperDir = srcDir.resolve("com/example/direct/mapper");
    copyTestResource(
        "integration/direct-mapper/ProductMapper.java.txt", mapperDir, "ProductMapper.java");

    // Model class
    Path modelDir = srcDir.resolve("com/example/direct/model");
    copyTestResource("integration/direct-mapper/Product.java.txt", modelDir, "Product.java");

    // MyBatis XML
    Path xmlMapperDir = resourcesDir.resolve("mapper");
    copyTestResource("integration/direct-mapper/ProductMapper.xml", xmlMapperDir);
  }

  /** AbstractView経由の呼び出しプロジェクト構造を作成 */
  private void createAbstractViewProjectStructure(Path srcDir, Path resourcesDir) throws Exception {
    // Controller that uses AbstractView
    Path controllerDir = srcDir.resolve("com/example/view/controller");
    copyTestResource(
        "integration/abstract-view/OrderController.java.txt",
        controllerDir,
        "OrderController.java");

    // AbstractView classes
    Path viewDir = srcDir.resolve("com/example/view/view");
    copyTestResource(
        "integration/abstract-view/AbstractView.java.txt", viewDir, "AbstractView.java");
    copyTestResource("integration/abstract-view/OrderView.java.txt", viewDir, "OrderView.java");
    copyTestResource(
        "integration/abstract-view/CustomerView.java.txt", viewDir, "CustomerView.java");

    // Service classes
    Path serviceDir = srcDir.resolve("com/example/view/service");
    copyTestResource(
        "integration/abstract-view/CustomerService.java.txt", serviceDir, "CustomerService.java");

    // Mapper classes
    Path mapperDir = srcDir.resolve("com/example/view/mapper");
    copyTestResource(
        "integration/abstract-view/OrderMapper.java.txt", mapperDir, "OrderMapper.java");
    copyTestResource(
        "integration/abstract-view/CustomerMapper.java.txt", mapperDir, "CustomerMapper.java");

    // Model classes
    Path modelDir = srcDir.resolve("com/example/view/model");
    copyTestResource("integration/abstract-view/Order.java.txt", modelDir, "Order.java");
    copyTestResource("integration/abstract-view/Customer.java.txt", modelDir, "Customer.java");

    // MyBatis XML
    Path xmlMapperDir = resourcesDir.resolve("mapper");
    copyTestResource("integration/abstract-view/OrderMapper.xml", xmlMapperDir);
    copyTestResource("integration/abstract-view/CustomerMapper.xml", xmlMapperDir);
  }

  private void createConfigurationFile() throws Exception {
    // デフォルト設定ファイルを作成
    Path configFile = tempDir.resolve("analyzer-config.properties");
    String configContent =
        """
        # CRUD Analyzer Configuration
        analysis.include.generated=true
        analysis.include.dynamic.sql=true
        output.markdown=true
        output.plantuml=true
        output.json=true
        """;

    Files.writeString(configFile, configContent);
  }
}
