package dev.example.crudscan;

import static org.mockito.Mockito.*;

import dev.example.crudscan.config.AnalyzerConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** ユニットテストの基底クラス テストリソースファイルの取得機能とモック作成機能を提供 */
public class UnitTestBase {

  /**
   * test/resources配下のファイルを指定されたディレクトリにコピーする
   *
   * @param resourcePath resources配下の相対パス（例: "mybatis/MyBatisXmlScannerTest/UserMapper.xml"）
   * @param targetDir コピー先のディレクトリ
   * @param fileName コピー先のファイル名
   * @throws IOException ファイル操作でエラーが発生した場合
   */
  protected void copyTestResource(String resourcePath, Path targetDir, String fileName)
      throws IOException {
    createDirectories(targetDir);
    Path sourceFile = Path.of("src/test/resources/" + resourcePath);
    Path targetFile = targetDir.resolve(fileName);
    Files.copy(sourceFile, targetFile);
  }

  /**
   * ディレクトリを作成する
   *
   * @param dir 作成するディレクトリのパス
   * @throws IOException ディレクトリ作成でエラーが発生した場合
   */
  protected void createDirectories(Path dir) throws IOException {
    Files.createDirectories(dir);
  }

  /**
   * test/resources配下のファイルを指定されたディレクトリにコピーする（ファイル名はそのまま）
   *
   * @param resourcePath resources配下の相対パス（例: "mybatis/MyBatisXmlScannerTest/UserMapper.xml"）
   * @param targetDir コピー先のディレクトリ
   * @throws IOException ファイル操作でエラーが発生した場合
   */
  protected void copyTestResource(String resourcePath, Path targetDir) throws IOException {
    Path sourceFile = Path.of("src/test/resources/" + resourcePath);
    String fileName = sourceFile.getFileName().toString();
    copyTestResource(resourcePath, targetDir, fileName);
  }

  /**
   * test/resources配下のファイルパスを取得する
   *
   * @param resourcePath resources配下の相対パス（例: "mybatis/MyBatisXmlScannerTest/UserMapper.xml"）
   * @return ファイルの絶対パス
   */
  protected Path getTestResourcePath(String resourcePath) {
    return Path.of("src/test/resources/" + resourcePath);
  }

  /**
   * テスト用の無効なXMLファイルを作成する
   *
   * @param targetDir 作成先のディレクトリ
   * @param fileName ファイル名
   * @throws IOException ファイル操作でエラーが発生した場合
   */
  protected void createInvalidXmlFile(Path targetDir, String fileName) throws IOException {
    Files.createDirectories(targetDir);
    String invalidXmlContent = "This is not valid XML content";
    Files.writeString(targetDir.resolve(fileName), invalidXmlContent);
  }

  /**
   * JARパスを含むAnalyzerConfigurationのモックを作成する
   *
   * @param tempDir テンポラリディレクトリ
   * @param jarDir JARファイルディレクトリ
   * @return モック化されたAnalyzerConfiguration
   */
  protected AnalyzerConfiguration createMockAnalyzerConfigurationWithJarPath(
      Path tempDir, Path jarDir) {
    AnalyzerConfiguration mockConfig = mock(AnalyzerConfiguration.class);

    // JARパスを返すようにモック設定
    when(mockConfig.getJarPaths()).thenReturn(List.of(jarDir));

    // その他の設定値もモック
    when(mockConfig.getSourceDirectory()).thenReturn(tempDir.resolve("src/main/java"));
    when(mockConfig.getResourcesDirectory()).thenReturn(tempDir.resolve("src/main/resources"));
    when(mockConfig.getOutputDirectory()).thenReturn(tempDir.resolve("output"));

    return mockConfig;
  }

  /**
   * 基本的なAnalyzerConfigurationのモックを作成する
   *
   * @param tempDir テンポラリディレクトリ
   * @return モック化されたAnalyzerConfiguration
   */
  protected AnalyzerConfiguration createMockAnalyzerConfiguration(Path tempDir) {
    AnalyzerConfiguration mockConfig = mock(AnalyzerConfiguration.class);

    // 空のJARパスを返すようにモック設定
    when(mockConfig.getJarPaths()).thenReturn(List.of());

    // その他の設定値もモック
    when(mockConfig.getSourceDirectory()).thenReturn(tempDir.resolve("src/main/java"));
    when(mockConfig.getResourcesDirectory()).thenReturn(tempDir.resolve("src/main/resources"));
    when(mockConfig.getOutputDirectory()).thenReturn(tempDir.resolve("output"));

    return mockConfig;
  }
}
