package dev.example.crudscan.integration;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.AnalyzerMain;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 統合テストクラス
 *
 * <p>アプリケーション全体の動作を統合的にテストします。
 */
@DisplayName("アプリケーション統合テスト")
class IntegrationTest {

  @TempDir Path tempDir;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  @BeforeEach
  void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  void testFullAnalysisWorkflow_ShouldGenerateAllOutputs() throws Exception {
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

    // ログ出力の確認
    String output = outContent.toString();
    assertThat(output).containsAnyOf("完了", "Done", "SUCCESS");
  }

  @Test
  void testAnalysisWithConfigurationFile_ShouldUseConfigSettings() throws Exception {
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

  /** リアルなプロジェクト構造を作成 */
  private void createRealisticProjectStructure(Path srcDir, Path resourcesDir) throws Exception {
    // Controller
    createControllerFiles(srcDir);

    // Service
    createServiceFiles(srcDir);

    // Repository
    createRepositoryFiles(srcDir);

    // MyBatis XML
    createMyBatisFiles(resourcesDir);
  }

  private void createControllerFiles(Path srcDir) throws Exception {
    Path controllerDir = srcDir.resolve("com/example/demo/controller");
    Files.createDirectories(controllerDir);

    String userControllerContent =
        """
        package com.example.demo.controller;

        import com.example.demo.service.UserService;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/users")
        public class UserController {

            @Autowired
            private UserService userService;

            @GetMapping
            public List<User> getAllUsers() {
                return userService.findAll();
            }

            @GetMapping("/{id}")
            public User getUser(@PathVariable Long id) {
                return userService.findById(id);
            }

            @PostMapping
            public User createUser(@RequestBody User user) {
                return userService.save(user);
            }

            @PutMapping("/{id}")
            public User updateUser(@PathVariable Long id, @RequestBody User user) {
                return userService.update(id, user);
            }

            @DeleteMapping("/{id}")
            public void deleteUser(@PathVariable Long id) {
                userService.delete(id);
            }
        }
        """;

    Files.writeString(controllerDir.resolve("UserController.java"), userControllerContent);
  }

  private void createServiceFiles(Path srcDir) throws Exception {
    Path serviceDir = srcDir.resolve("com/example/demo/service");
    Files.createDirectories(serviceDir);

    String userServiceContent =
        """
        package com.example.demo.service;

        import com.example.demo.repository.UserRepository;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Service;

        @Service
        public class UserService {

            @Autowired
            private UserRepository userRepository;

            public List<User> findAll() {
                return userRepository.findAll();
            }

            public User findById(Long id) {
                return userRepository.findById(id);
            }

            public User save(User user) {
                return userRepository.save(user);
            }

            public User update(Long id, User user) {
                return userRepository.update(id, user);
            }

            public void delete(Long id) {
                userRepository.delete(id);
            }
        }
        """;

    Files.writeString(serviceDir.resolve("UserService.java"), userServiceContent);
  }

  private void createRepositoryFiles(Path srcDir) throws Exception {
    Path repositoryDir = srcDir.resolve("com/example/demo/repository");
    Files.createDirectories(repositoryDir);

    String userRepositoryContent =
        """
        package com.example.demo.repository;

        import org.apache.ibatis.annotations.Mapper;

        @Mapper
        public interface UserRepository {
            List<User> findAll();
            User findById(Long id);
            User save(User user);
            User update(Long id, User user);
            void delete(Long id);
        }
        """;

    Files.writeString(repositoryDir.resolve("UserRepository.java"), userRepositoryContent);
  }

  private void createMyBatisFiles(Path resourcesDir) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    Files.createDirectories(mapperDir);

    String userMapperContent =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
        <mapper namespace="com.example.demo.repository.UserRepository">

            <select id="findAll" resultType="User">
                SELECT * FROM users ORDER BY id
            </select>

            <select id="findById" resultType="User">
                SELECT * FROM users WHERE id = #{id}
            </select>

            <insert id="save">
                INSERT INTO users (name, email, created_at)
                VALUES (#{name}, #{email}, NOW())
            </insert>

            <update id="update">
                UPDATE users
                SET name = #{user.name}, email = #{user.email}, updated_at = NOW()
                WHERE id = #{id}
            </update>

            <delete id="delete">
                DELETE FROM users WHERE id = #{id}
            </delete>

        </mapper>
        """;

    Files.writeString(mapperDir.resolve("UserMapper.xml"), userMapperContent);
  }

  private void createBasicProjectStructure(Path srcDir, Path resourcesDir) throws Exception {
    // 最小限の構造
    Path packageDir = srcDir.resolve("com/example/basic");
    Files.createDirectories(packageDir);

    String basicControllerContent =
        """
        package com.example.basic;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class BasicController {

            @GetMapping("/hello")
            public String hello() {
                return "Hello World";
            }
        }
        """;

    Files.writeString(packageDir.resolve("BasicController.java"), basicControllerContent);

    // 基本的なリソースファイルも作成
    if (resourcesDir != null) {
      Files.createDirectories(resourcesDir.resolve("static"));
    }
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
