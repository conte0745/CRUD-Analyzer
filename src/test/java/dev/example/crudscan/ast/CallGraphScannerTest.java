package dev.example.crudscan.ast;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.TestBase;
import dev.example.crudscan.model.Models.CallEdge;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** CallGraphScannerのテストクラス */
@DisplayName("CallGraphScanner機能のテスト")
class CallGraphScannerTest extends TestBase {
  private CallGraphScanner scanner;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    scanner = new CallGraphScanner(tempDir);
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    CallGraphScanner newScanner = new CallGraphScanner(tempDir);

    // Then
    assertThat(newScanner).isNotNull();
  }

  @Test
  @DisplayName("存在しないディレクトリをスキャンした場合に空のリストが返されること")
  void testScan_WithNonExistentDirectory_ShouldReturnEmptyList() {
    // Given
    Path nonExistentDir = tempDir.resolve("non-existent");
    CallGraphScanner nonExistentScanner = new CallGraphScanner(nonExistentDir);

    // When
    List<CallEdge> result = nonExistentScanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("空のディレクトリをスキャンした場合に空のリストが返されること")
  void testScan_WithEmptyDirectory_ShouldReturnEmptyList() {
    // Given - 空のディレクトリ

    // When
    List<CallEdge> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Service-Repository間の呼び出し関係が正常に検出されること")
  void testScan_WithServiceRepositoryCall_ShouldDetectCallEdge() throws Exception {
    // Given
    createServiceRepositoryFiles();

    // When
    List<CallEdge> result = scanner.scan();

    // Then
    assertThat(result).isNotEmpty();

    // UserService -> UserRepository の呼び出し関係を確認
    boolean hasServiceToRepositoryCall = result.stream()
        .anyMatch(edge -> edge.fromClass().contains("UserService")
            && edge.toClass().contains("UserRepository") && edge.fromMethod().equals("findUser")
            && edge.toMethod().equals("findById"));

    assertThat(hasServiceToRepositoryCall).isTrue();
  }

  @Test
  @DisplayName("複数のメソッド呼び出し関係が正常に検出されること")
  void testScan_WithMultipleMethodCalls_ShouldDetectAllCallEdges() throws Exception {
    // Given
    createComplexServiceFiles();

    // When
    List<CallEdge> result = scanner.scan();

    // Then
    assertThat(result).hasSizeGreaterThan(1);

    // 複数の呼び出し関係が検出されることを確認
    List<String> fromMethods = result.stream().map(CallEdge::fromMethod).distinct().toList();
    assertThat(fromMethods).hasSizeGreaterThan(1);
  }

  @Test
  @DisplayName("対象外のクラス（非Service/Repository/Mapper/Dao）は処理されないこと")
  void testScan_WithNonTargetClass_ShouldNotProcess() throws Exception {
    // Given
    createNonTargetClassFile();

    // When
    List<CallEdge> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("不正なJavaファイルが適切に処理されること")
  void testScan_WithInvalidJavaFile_ShouldHandleGracefully() throws Exception {
    // Given
    createInvalidJavaFile();

    // When
    List<CallEdge> result = scanner.scan();

    // Then
    // 不正なファイルは処理されないが、例外は投げられない
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("メソッド呼び出しがないクラスでも正常に処理されること")
  void testScan_WithNoMethodCalls_ShouldHandleGracefully() throws Exception {
    // Given
    createServiceWithoutCalls();

    // When
    List<CallEdge> result = scanner.scan();

    // Then
    assertThat(result).isEmpty();
  }

  /** Service-Repository間の呼び出しファイルを作成 */
  private void createServiceRepositoryFiles() throws Exception {
    Path serviceDir = tempDir.resolve("com/example/service");
    Path repositoryDir = tempDir.resolve("com/example/repository");
    createDirectories(serviceDir);
    createDirectories(repositoryDir);

    // UserService
    String serviceContent = """
        package com.example.service;

        import com.example.repository.UserRepository;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Service;

        @Service
        public class UserService {

            @Autowired
            private UserRepository userRepository;

            public User findUser(Long id) {
                return userRepository.findById(id);
            }
        }
        """;

    // UserRepository
    String repositoryContent = """
        package com.example.repository;

        import org.springframework.stereotype.Repository;

        @Repository
        public class UserRepository {

            public User findById(Long id) {
                // データベースアクセス処理
                return new User();
            }
        }
        """;

    Files.writeString(serviceDir.resolve("UserService.java"), serviceContent);
    Files.writeString(repositoryDir.resolve("UserRepository.java"), repositoryContent);
  }

  /** 複数のメソッド呼び出しを含むServiceファイルを作成 */
  private void createComplexServiceFiles() throws Exception {
    Path serviceDir = tempDir.resolve("com/example/service");
    Path repositoryDir = tempDir.resolve("com/example/repository");
    createDirectories(serviceDir);
    createDirectories(repositoryDir);

    String serviceContent = """
        package com.example.service;

        import com.example.repository.UserRepository;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Service;

        @Service
        public class UserService {

            @Autowired
            private UserRepository userRepository;

            public User findUser(Long id) {
                return userRepository.findById(id);
            }

            public void saveUser(User user) {
                userRepository.save(user);
            }

            public void deleteUser(Long id) {
                userRepository.deleteById(id);
            }
        }
        """;

    String repositoryContent = """
        package com.example.repository;

        import org.springframework.stereotype.Repository;

        @Repository
        public class UserRepository {

            public User findById(Long id) {
                return new User();
            }

            public void save(User user) {
                // 保存処理
            }

            public void deleteById(Long id) {
                // 削除処理
            }
        }
        """;

    Files.writeString(serviceDir.resolve("UserService.java"), serviceContent);
    Files.writeString(repositoryDir.resolve("UserRepository.java"), repositoryContent);
  }

  /** 対象外のクラスファイルを作成 */
  private void createNonTargetClassFile() throws Exception {
    Path controllerDir = tempDir.resolve("com/example/controller");
    createDirectories(controllerDir);

    String content = """
        package com.example.controller;

        import org.springframework.stereotype.Controller;

        @Controller
        public class UserController {

            public String index() {
                return "index";
            }
        }
        """;

    Files.writeString(controllerDir.resolve("UserController.java"), content);
  }

  /** メソッド呼び出しがないServiceファイルを作成 */
  private void createServiceWithoutCalls() throws Exception {
    Path serviceDir = tempDir.resolve("com/example/service");
    createDirectories(serviceDir);

    String content = """
        package com.example.service;

        import org.springframework.stereotype.Service;

        @Service
        public class SimpleService {

            public String getMessage() {
                return "Hello World";
            }
        }
        """;

    Files.writeString(serviceDir.resolve("SimpleService.java"), content);
  }

  /** 不正なJavaファイルを作成 */
  private void createInvalidJavaFile() throws Exception {
    String content = """
        This is not valid Java code
        public class InvalidService {
            // 不正な構文
        """;

    Files.writeString(tempDir.resolve("InvalidService.java"), content);
  }
}
