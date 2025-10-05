# テスト実装ガイド

CRUD Analyzerプロジェクトでのテスト実装の標準化ガイド

## 📋 目次

1. [基本構造](#基本構造)
2. [命名規則](#命名規則)
3. [テストメソッドの構造](#テストメソッドの構造)
4. [アノテーション](#アノテーション)
5. [テストデータ管理](#テストデータ管理)
6. [パッケージ別戦略](#パッケージ別戦略)
7. [リソース管理](#リソース管理)
8. [モック使用](#モック使用)
9. [品質維持](#品質維持)
10. [まとめ](#まとめ)

## 🏗️ 基本構造

### テストクラスの基本構造

```java
/** {TargetClass}のテストクラス */
@DisplayName("{TargetClass}機能のテスト")
class {TargetClass}Test extends TestBase {

  private {TargetClass} target;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    target = new {TargetClass}(tempDir);
  }

  @Test
  @DisplayName("正常系のテストケース説明")
  void testMethod_WithValidInput_ShouldReturnExpectedResult() {
    // Given
    // テストデータの準備

    // When
    // テスト対象メソッドの実行

    // Then
    // 結果の検証
  }
}
```

### テスト継承階層

- **TestBase**: すべてのユニットテストの基底クラス
  - 共通のテストユーティリティメソッドを提供
  - テストリソースの管理機能
  - ファイル操作のヘルパーメソッド

## 📝 命名規則

### クラス名
- **パターン**: `{TargetClass}Test`
- **例**: `SqlClassifierTest`, `MarkdownWriterTest`

### テストメソッド名
- **パターン**: `test{MethodName}_{Condition}_{ExpectedResult}`
- **例**:
  - `testClassify_WithValidSql_ShouldReturnCorrectInfo`
  - `testScan_WithEmptyDirectory_ShouldReturnEmptyList`
  - `testWrite_WithNullInput_ShouldThrowException`

## 🔧 テストメソッドの構造

### Given-When-Then パターンの徹底

```java
@Test
@DisplayName("有効なSQL文で正しい分類情報が返されること")
void testClassify_WithValidSql_ShouldReturnCorrectInfo() {
  // Given - テストデータの準備
  String sql = "SELECT * FROM users WHERE id = 1";

  // When - テスト対象メソッドの実行
  SqlClassifier.Info result = classifier.classify(sql);

  // Then - 結果の検証
  assertThat(result).isNotNull();
  assertThat(result.op()).isEqualTo("SELECT");
  assertThat(result.getAllTables()).contains("users");
}
```

### テストケースの分類

1. **正常系テスト**
   - 期待される入力での動作確認
   - 境界値での動作確認

2. **異常系テスト**
   - 不正な入力での例外処理確認
   - null入力での動作確認

3. **エッジケーステスト**
   - 空の入力での動作確認
   - 大量データでの動作確認

## 🏷️ アノテーション

### 必須アノテーション

```java
// クラスレベル
@DisplayName("機能の概要を日本語で説明")
class ExampleTest {

  // メソッドレベル
  @Test
  @DisplayName("テストケースの具体的な内容を日本語で説明")
  void testMethod() {
    // テスト実装
  }
}
```

## 📁 テストデータ管理

### テストリソースファイルの配置

```
src/test/resources/
├── mybatis/
│   ├── MyBatisXmlScannerTest/
│   │   ├── UserMapper.xml
│   │   ├── CompleteMapper.xml
│   │   └── ComplexJoinMapper.xml
│   └── MyBatisAnnotationScannerTest/
│       ├── UserAnnotationMapper.java.txt
│       └── CompleteAnnotationMapper.java.txt
└── sql/
    ├── valid-queries.sql
    └── invalid-queries.sql
```

### テストデータの作成パターン

```java
/** テスト用のXMLファイルを作成 */
private void createTestXmlFile() throws Exception {
  Path mapperDir = tempDir.resolve("mapper");
  copyTestResource(
      "mybatis/MyBatisXmlScannerTest/UserMapper.xml",
      mapperDir,
      "UserMapper.xml");
}

/** テスト用のモデルオブジェクトを作成 */
private List<Endpoint> createTestEndpoints() {
  return List.of(
      new Endpoint("GET", "/api/users", "UserController", "getUsers", "com.example.controller"),
      new Endpoint("POST", "/api/users", "UserController", "createUser", "com.example.controller")
  );
}
```

## 📦 パッケージ別戦略

### AST（抽象構文木）パッケージ

```java
@DisplayName("CallGraphScanner機能のテスト")
class CallGraphScannerTest extends TestBase {

  @Test
  @DisplayName("Javaファイルからメソッド呼び出し関係が正しく抽出されること")
  void testScan_WithJavaFiles_ShouldExtractCallRelations() {
    // Javaファイルを作成してスキャン結果を検証
  }
}
```

### MyBatisパッケージ

```java
@DisplayName("MyBatisXmlScanner機能のテスト")
class MyBatisXmlScannerTest extends TestBase {

  @Test
  @DisplayName("複雑なSQLクエリからテーブル名が正しく抽出されること")
  void testScan_WithComplexQueries_ShouldExtractTables() {
    // XMLファイルを作成してSQL解析結果を検証
  }
}
```

### 出力パッケージ

```java
@DisplayName("MarkdownWriter機能のテスト")
class MarkdownWriterTest {

  @Test
  @DisplayName("CRUDマトリクスが正しいMarkdown形式で出力されること")
  void testWriteMatrix_WithValidData_ShouldCreateCorrectMarkdownTable() {
    // 出力ファイルの内容を検証
  }
}
```

## 📂 リソース管理

### リソースファイルの命名規則

- **パターン**: `{TestClass}/{TestScenario}.{extension}`
- **例**:
  - `MyBatisXmlScannerTest/UserMapper.xml`
  - `MyBatisAnnotationScannerTest/CompleteAnnotationMapper.java.txt`

### リソースコピーの標準化

```java
/** TestBaseで提供されるヘルパーメソッド */
protected void copyTestResource(String resourcePath, Path targetDir, String fileName)
    throws Exception {
  // リソースファイルを一時ディレクトリにコピー
}

protected Path getTestResourcePath(String resourcePath) {
  // テストリソースのパスを取得
}
```

## 🎭 モック使用

### 外部依存の分離

```java
@Mock
private ExternalService externalService;

@Test
@DisplayName("外部サービスの呼び出しが正しく処理されること")
void testMethod_WithExternalService_ShouldHandleCorrectly() {
  // Given
  when(externalService.call()).thenReturn("expected_result");

  // When
  String result = target.processWithExternalService();

  // Then
  assertThat(result).isEqualTo("processed_expected_result");
  verify(externalService).call();
}
```

## ⚡ 品質維持

### SonarQubeルールの遵守

```java
// 推奨: アサーションチェーンの使用
assertThat(result)
    .isNotNull()
    .hasSize(3)
    .contains("expected");

// 推奨: パラメータ化テストの使用（類似テストが4つ以上ある場合）
@ParameterizedTest
@CsvSource({
    "SELECT, users, true",
    "INSERT, products, false",
    "UPDATE, orders, true"
})
void testClassify_WithVariousInputs(String operation, String table, boolean expected) {
    // テスト実装
}
```

### テストの保守性

- **DRY原則**: 重複するテストコードを避ける
- **単一責任**: 1つのテストメソッドは1つの機能をテスト
- **可読性**: テストの意図が明確に分かる命名とコメント

## 🎯 まとめ

### 品質指標

- **カバレッジ**: 85%以上
- **テストケース**: 正常系・異常系・エッジケース
- **SonarQube**: ルール遵守

### このガイドの効果

1. **一貫性**: プロジェクト全体で統一されたテストコード
2. **保守性**: 理解しやすく修正しやすいテスト
3. **信頼性**: 高品質で安定したテストスイート
4. **効率性**: 開発者が迷わずテストを書ける環境

テスト実装時は、このガイドを参考にして品質の高いテストコードを作成してください。
