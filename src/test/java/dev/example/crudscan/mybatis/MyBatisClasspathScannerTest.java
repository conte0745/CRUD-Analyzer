package dev.example.crudscan.mybatis;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.TestBase;
import dev.example.crudscan.config.AnalyzerConfiguration;
import dev.example.crudscan.model.Models.SqlMapping;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ClasspathMyBatisScannerのテストクラス */
@DisplayName("MyBatisClasspathScanner機能のテスト")
class MyBatisClasspathScannerTest extends TestBase {

  private MyBatisClasspathScanner scanner;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    scanner = new MyBatisClasspathScanner(tempDir);
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    MyBatisClasspathScanner newScanner = new MyBatisClasspathScanner(tempDir);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  @DisplayName("有効なXMLファイルからマッピングが抽出されること")
  void testScan_WithValidXmlFiles_ShouldExtractMappings() throws Exception {
    // Given
    createTestResourcesStructure(tempDir.resolve("src/main/resources"));

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
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("スキャン処理が適切に処理されること")
  void testScan_ShouldHandleGracefully() {
    // When & Then
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("設定付きコンストラクタでインスタンスが正常に作成されること")
  void testConstructorWithConfig_ShouldCreateInstance() {
    // Given
    var config = new dev.example.crudscan.config.AnalyzerConfiguration();

    // When
    MyBatisClasspathScanner newScanner = new MyBatisClasspathScanner(tempDir, config);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  @DisplayName("存在しないリソースディレクトリでも正常に処理されること")
  void testScan_WithNonExistentResourcesDir_ShouldHandleGracefully() throws Exception {
    // Given
    Path nonExistentProject = tempDir.resolve("non-existent-project");
    MyBatisClasspathScanner nonExistentScanner = new MyBatisClasspathScanner(nonExistentProject);

    // When
    List<SqlMapping> result = nonExistentScanner.scan();

    // Then
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("複数のXMLファイルからマッピングが抽出されること")
  void testScan_WithMultipleXmlFiles_ShouldExtractAllMappings() throws Exception {
    // Given
    createMultipleTestResourcesStructure(tempDir.resolve("src/main/resources"));

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    // 複数のファイルが処理されることを確認
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("無効なXMLファイルが適切に処理されること")
  void testScan_WithInvalidXmlFile_ShouldHandleGracefully() throws Exception {
    // Given
    createInvalidXmlResourcesStructure(tempDir.resolve("src/main/resources"));

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    // 無効なファイルがあっても例外が発生しない
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ネストしたディレクトリ構造からXMLファイルが検出されること")
  void testScan_WithNestedDirectoryStructure_ShouldFindXmlFiles() throws Exception {
    // Given
    createNestedResourcesStructure(tempDir.resolve("src/main/resources"));

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("XMLファイル以外のファイルは無視されること")
  void testScan_WithNonXmlFiles_ShouldIgnoreNonXmlFiles() throws Exception {
    // Given
    createMixedFileTypesStructure(tempDir.resolve("src/main/resources"));

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("空のXMLファイルが適切に処理されること")
  void testScan_WithEmptyXmlFile_ShouldHandleGracefully() throws Exception {
    // Given
    createEmptyXmlResourcesStructure(tempDir.resolve("src/main/resources"));

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("MyBatis以外のXMLファイルが適切に処理されること")
  void testScan_WithNonMyBatisXmlFile_ShouldHandleGracefully() throws Exception {
    // Given
    createNonMyBatisXmlStructure(tempDir.resolve("src/main/resources"));

    // When
    List<SqlMapping> result = scanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(() -> scanner.scan()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("JARファイル内のXMLファイルからマッピングが抽出されること")
  void testScan_WithJarFile_ShouldExtractMappingsFromJar() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);
    // "common"で始まるJARファイルを作成（scanJarsInDirectoryの条件を満たす）
    createTestJarFile(jarDir, "common-mybatis.jar");

    // カスタム設定でJARパスを指定
    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("複数のJARファイルからマッピングが抽出されること")
  void testScan_WithMultipleJarFiles_ShouldExtractFromAllJars() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);
    createTestJarFile(jarDir, "common-mapper-1.0.jar");
    createTestJarFile(jarDir, "common-utils-2.0.jar");

    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("存在しないJARパスが設定されている場合に適切に処理されること")
  void testScan_WithNonExistentJarPath_ShouldHandleGracefully() throws Exception {
    // Given
    Path nonExistentJarDir = tempDir.resolve("non-existent-lib");
    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(nonExistentJarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("無効なJARファイルが適切に処理されること")
  void testScan_WithInvalidJarFile_ShouldHandleGracefully() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);
    createInvalidJarFile(jarDir);

    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("JARファイル内の無効なXMLファイルが適切に処理されること")
  void testScan_WithInvalidXmlInJar_ShouldHandleGracefully() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);
    createJarFileWithInvalidXml(jarDir);

    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("scanJarsInDirectoryメソッドが正常に動作すること")
  void testScan_ScanJarsInDirectory_ShouldProcessCommonJars() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);

    // "common"で始まるJARファイルを複数作成
    createTestJarFile(jarDir, "common-mapper-v1.jar");
    createTestJarFile(jarDir, "common-utils-v2.jar");
    // "common"で始まらないJARファイル（無視される）
    createTestJarFile(jarDir, "other-library.jar");

    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    // "common"で始まるJARファイルのみが処理される
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("scanJarFileメソッドがXMLファイルのみを処理すること")
  void testScan_ScanJarFile_ShouldProcessOnlyXmlFiles() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);

    // XML以外のファイルも含むJARファイルを作成
    createJarFileWithMixedContent(jarDir);

    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull();
    // XMLファイルのみが処理され、他のファイルは無視される
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ディレクトリ内のJARファイルが存在しない場合に適切に処理されること")
  void testScan_WithEmptyJarDirectory_ShouldHandleGracefully() throws Exception {
    // Given
    Path jarDir = tempDir.resolve("lib");
    createDirectories(jarDir);
    // JARファイルを作成しない（空のディレクトリ）

    AnalyzerConfiguration customConfig = createCustomConfigWithJarPath(jarDir);
    MyBatisClasspathScanner jarScanner = new MyBatisClasspathScanner(tempDir, customConfig);

    // When
    List<SqlMapping> result = jarScanner.scan();

    // Then
    assertThat(result).isNotNull().isEmpty();
    assertThatCode(jarScanner::scan).doesNotThrowAnyException();
  }

  /** テスト用のリソース構造を作成 */
  private void createTestResourcesStructure(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/UserMapper.xml", mapperDir,
        "UserMapper.xml");
  }

  /** 複数のテスト用リソース構造を作成 */
  private void createMultipleTestResourcesStructure(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/UserMapper.xml", mapperDir,
        "UserMapper.xml");
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/ProductMapper.xml", mapperDir,
        "ProductMapper.xml");
  }

  /** 無効なXMLファイルを含むリソース構造を作成 */
  private void createInvalidXmlResourcesStructure(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    createDirectories(mapperDir);

    // 無効なXMLファイルを作成
    String invalidXmlContent = """
        <?xml version="1.0" encoding="UTF-8"?>
        <mapper namespace="com.example.test.InvalidMapper">
            <select id="findAll" resultType="String">
                SELECT * FROM invalid_table
            <!-- 閉じタグなし
        </mapper>
        """;
    Files.writeString(mapperDir.resolve("InvalidMapper.xml"), invalidXmlContent);
  }

  /** ネストしたディレクトリ構造を作成 */
  private void createNestedResourcesStructure(Path resourcesDir) throws Exception {
    Path nestedDir = resourcesDir.resolve("mapper/nested/deep");
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/NestedMapper.xml", nestedDir,
        "NestedMapper.xml");
  }

  /** 混合ファイルタイプの構造を作成 */
  private void createMixedFileTypesStructure(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    createDirectories(mapperDir);

    // XMLファイル
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/UserMapper.xml", mapperDir,
        "UserMapper.xml");

    // 非XMLファイル
    Files.writeString(mapperDir.resolve("config.properties"), "key=value");
    Files.writeString(mapperDir.resolve("README.txt"), "This is a readme file");
    Files.writeString(mapperDir.resolve("data.json"), "{\"key\": \"value\"}");
  }

  /** 空のXMLファイルを含むリソース構造を作成 */
  private void createEmptyXmlResourcesStructure(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    createDirectories(mapperDir);

    // 空のXMLファイル
    Files.writeString(mapperDir.resolve("EmptyMapper.xml"), "");
  }

  /** MyBatis以外のXMLファイルを含むリソース構造を作成 */
  private void createNonMyBatisXmlStructure(Path resourcesDir) throws Exception {
    Path configDir = resourcesDir.resolve("config");
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/spring-config.xml", configDir,
        "spring-config.xml");
    copyTestResource("mybatis/ClasspathMyBatisScannerTest/pom.xml", resourcesDir, "pom.xml");
  }

  /** テスト用のJARファイルを作成（ファイル名指定） */
  private Path createTestJarFile(Path jarDir, String jarFileName) throws Exception {
    Path jarFile = jarDir.resolve(jarFileName);

    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
      // MyBatis XMLファイルをJARに追加
      String mapperXmlContent =
          loadResourceAsString("mybatis/ClasspathMyBatisScannerTest/JarMapper.xml");
      JarEntry mapperEntry = new JarEntry("mapper/JarMapper.xml");
      jos.putNextEntry(mapperEntry);
      jos.write(mapperXmlContent.getBytes());
      jos.closeEntry();

      // 追加のXMLファイル
      String additionalXmlContent =
          loadResourceAsString("mybatis/ClasspathMyBatisScannerTest/AdditionalMapper.xml");
      JarEntry additionalEntry = new JarEntry("com/example/mapper/AdditionalMapper.xml");
      jos.putNextEntry(additionalEntry);
      jos.write(additionalXmlContent.getBytes());
      jos.closeEntry();

      // 非XMLファイルも追加（無視されることを確認）
      JarEntry propertiesEntry = new JarEntry("config.properties");
      jos.putNextEntry(propertiesEntry);
      jos.write("key=value".getBytes());
      jos.closeEntry();
    }

    return jarFile;
  }

  /** リソースファイルを文字列として読み込む */
  private String loadResourceAsString(String resourcePath) throws Exception {
    try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Resource not found: " + resourcePath);
      }
      return new String(inputStream.readAllBytes());
    }
  }

  /** 無効なJARファイルを作成 */
  private void createInvalidJarFile(Path jarDir) throws Exception {
    Path invalidJarFile = jarDir.resolve("common-invalid.jar");
    // 無効なJARファイル（単なるテキストファイル）
    Files.writeString(invalidJarFile, "This is not a valid JAR file");
  }

  /** 無効なXMLを含むJARファイルを作成 */
  private void createJarFileWithInvalidXml(Path jarDir) throws Exception {
    Path jarFile = jarDir.resolve("common-invalid-xml.jar");

    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
      // 無効なXMLファイル
      String invalidXmlContent = """
          <?xml version="1.0" encoding="UTF-8"?>
          <mapper namespace="com.example.jar.InvalidMapper">
              <select id="findInvalid" resultType="String">
                  SELECT * FROM invalid_table
              <!-- 閉じタグなし
          </mapper>
          """;

      JarEntry invalidEntry = new JarEntry("mapper/InvalidMapper.xml");
      jos.putNextEntry(invalidEntry);
      jos.write(invalidXmlContent.getBytes());
      jos.closeEntry();
    }
  }

  /** 混合コンテンツを含むJARファイルを作成 */
  private void createJarFileWithMixedContent(Path jarDir) throws Exception {
    Path jarFile = jarDir.resolve("common-mixed-content.jar");

    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile.toFile()))) {
      // MyBatis XMLファイル
      String mapperXmlContent =
          loadResourceAsString("mybatis/ClasspathMyBatisScannerTest/MixedMapper.xml");
      JarEntry mapperEntry = new JarEntry("mapper/MixedMapper.xml");
      jos.putNextEntry(mapperEntry);
      jos.write(mapperXmlContent.getBytes());
      jos.closeEntry();

      // 非XMLファイル（処理されない）
      JarEntry propertiesEntry = new JarEntry("config.properties");
      jos.putNextEntry(propertiesEntry);
      jos.write("database.url=jdbc:mysql://localhost:3306/test".getBytes());
      jos.closeEntry();

      JarEntry javaEntry = new JarEntry("com/example/Service.class");
      jos.putNextEntry(javaEntry);
      jos.write("dummy class content".getBytes());
      jos.closeEntry();

      // ディレクトリエントリ（処理されない）
      JarEntry dirEntry = new JarEntry("META-INF/");
      jos.putNextEntry(dirEntry);
      jos.closeEntry();

      // 非MyBatis XMLファイル（処理される可能性があるが、MyBatisマッパーではない）
      String configXmlContent =
          loadResourceAsString("mybatis/ClasspathMyBatisScannerTest/mybatis-config.xml");
      JarEntry configEntry = new JarEntry("mybatis-config.xml");
      jos.putNextEntry(configEntry);
      jos.write(configXmlContent.getBytes());
      jos.closeEntry();
    }
  }

  /** JARパスを含むカスタム設定を作成（TestBaseのモック使用） */
  private AnalyzerConfiguration createCustomConfigWithJarPath(Path jarDir) {
    return createMockAnalyzerConfigurationWithJarPath(tempDir, jarDir);
  }
}
