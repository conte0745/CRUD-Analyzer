package dev.example.crudscan.output;

import dev.example.crudscan.model.Models.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRUDマトリクスをMarkdown形式で出力するライター
 *
 * <p>このクラスは、CRUD解析の結果をMarkdown形式のテーブルとして出力し、 視覚的に分かりやすいCRUDマトリクスを生成します。
 *
 * <h2>出力形式</h2>
 *
 * <ul>
 *   <li>全体のCRUDマトリクス（すべてのエンドポイント）
 *   <li>パッケージ別のCRUDマトリクス（パッケージごとに分割）
 *   <li>Markdownテーブル形式（GitHubやDocusaurus等で表示可能）
 * </ul>
 *
 * <h2>マトリクス構造</h2>
 *
 * <pre>
 * | URL | HTTP | table1 | table2 | table3 |
 * |-----|------|--------|--------|--------|
 * | /users | GET | S | | |
 * | /users | POST | I | | |
 * | /books | GET | | S | S |
 * </pre>
 *
 * <h2>CRUD記号</h2>
 *
 * <ul>
 *   <li>C (Create) - INSERT操作
 *   <li>R (Read) - SELECT操作
 *   <li>U (Update) - UPDATE操作
 *   <li>D (Delete) - DELETE操作
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * MarkdownWriter writer = new MarkdownWriter();
 *
 * // 全体のCRUDマトリクス出力
 * writer.writeMatrix(Paths.get("crud-matrix.md"), crudLinks);
 *
 * // パッケージ別CRUDマトリクス出力
 * writer.writeMatrixByPackage(Paths.get("output"), crudLinks, endpoints);
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class MarkdownWriter {
  private static final Logger logger = LoggerFactory.getLogger(MarkdownWriter.class);

  /** デフォルトコンストラクタ */
  public MarkdownWriter() {
    // デフォルトコンストラクタ
  }

  /**
   * CRUDマトリクスをMarkdownファイルに出力する
   *
   * <p>すべてのCRUDリンクを統合したマトリクスを生成し、 指定されたパスにMarkdownファイルとして出力します。
   *
   * @param out 出力先パス（.mdファイル）
   * @param links CRUDリンク一覧
   */
  public void writeMatrix(Path out, List<CrudLink> links) {
    try {
      Files.createDirectories(out.getParent());
    } catch (IOException ignored) {
      logger.error("出力ディレクトリ作成失敗: {}", out.getParent());
    }
    var eps = links.stream().map(CrudLink::ep).distinct().toList();
    var tables = links.stream().map(CrudLink::table).distinct().sorted().toList();
    try (var w = Files.newBufferedWriter(out)) {
      w.write("| URL | HTTP | " + String.join(" | ", tables) + " |\n");
      w.write("|---|---|" + "---|".repeat(tables.size()) + "\n");
      for (Endpoint ep : eps) {
        var row = new ArrayList<String>();
        row.add(ep.url());
        row.add(ep.httpMethod());
        for (String t : tables) {
          var cell =
              links.stream()
                  .filter(l -> l.ep().equals(ep) && l.table().equals(t))
                  .map(CrudLink::crud)
                  .sorted()
                  .distinct()
                  .collect(Collectors.joining(""));
          row.add(cell);
        }
        w.write("| " + String.join(" | ", row) + " |\n");
      }
    } catch (IOException e) {
      logger.error("CRUDマトリクス出力失敗: {}", out);
      throw new UncheckedIOException(e);
    }
  }

  /**
   * CRUDマトリクスのインデックスファイルを出力する
   *
   * <p>パッケージ別CRUDマトリクスへの参照リンク集を生成し、見出しや作成日時を含むインデックスファイルを出力します。
   *
   * @param out 出力先パス（.mdファイル）
   * @param links CRUDリンク一覧
   * @param endpoints 全エンドポイント一覧
   */
  public void writeIndexMatrix(Path out, List<CrudLink> links, List<Endpoint> endpoints) {
    writeIndexMatrix(out, links, endpoints, List.of());
  }

  /**
   * CRUDマトリクスのインデックスファイルを出力する（バッチJob対応版）
   *
   * <p>パッケージ別CRUDマトリクスへの参照リンク集を生成し、バッチJob情報も含めて出力します。
   *
   * @param out 出力先パス（.mdファイル）
   * @param links CRUDリンク一覧
   * @param endpoints 全エンドポイント一覧
   * @param batchJobs バッチJob一覧
   */
  public void writeIndexMatrix(
      Path out, List<CrudLink> links, List<Endpoint> endpoints, List<BatchJob> batchJobs) {
    try {
      Files.createDirectories(out.getParent());
      try (var w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
        // ヘッダー情報
        w.write("# CRUD Matrix Index\n\n");
        w.write(
            "**Generated:** "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + "\n\n");

        // 統計情報
        Map<String, List<Endpoint>> packageGroups =
            endpoints.stream().collect(Collectors.groupingBy(Endpoint::packageName));

        Set<String> tables = links.stream().map(CrudLink::table).collect(Collectors.toSet());

        w.write("## 📊 統計情報\n\n");
        w.write("- **パッケージ数:** " + packageGroups.size() + "\n");
        w.write("- **エンドポイント数:** " + endpoints.size() + "\n");
        w.write("- **バッチJob数:** " + batchJobs.size() + "\n");
        w.write("- **テーブル数:** " + tables.size() + "\n");
        w.write("- **CRUDリンク数:** " + links.size() + "\n\n");

        // パッケージ別リンク
        w.write("## 📋 パッケージ別CRUDマトリクス\n\n");

        for (Map.Entry<String, List<Endpoint>> entry : packageGroups.entrySet()) {
          String packageName = entry.getKey();
          List<Endpoint> packageEndpoints = entry.getValue();

          // このパッケージのCRUDリンクをフィルタ
          List<CrudLink> packageLinks =
              links.stream().filter(link -> packageEndpoints.contains(link.ep())).toList();

          if (!packageLinks.isEmpty()) {
            // パッケージ情報
            String fileName = packageName.replace(".", "-") + "-crud-matrix.md";
            Set<String> packageTables =
                packageLinks.stream().map(CrudLink::table).collect(Collectors.toSet());

            w.write("### 📦 " + packageName + "\n\n");
            w.write("- **ファイル:** [" + fileName + "](crud/packages/" + fileName + ")\n");
            w.write("- **エンドポイント数:** " + packageEndpoints.size() + "\n");
            w.write("- **対象テーブル:** " + String.join(", ", packageTables) + "\n");
            w.write("- **CRUDリンク数:** " + packageLinks.size() + "\n\n");
          }
        }

        // バッチJob情報
        if (!batchJobs.isEmpty()) {
          w.write("## 🔄 バッチJob一覧\n\n");
          Map<String, List<BatchJob>> jobsByPackage =
              batchJobs.stream().collect(Collectors.groupingBy(BatchJob::packageName));

          for (Map.Entry<String, List<BatchJob>> entry : jobsByPackage.entrySet()) {
            String packageName = entry.getKey();
            List<BatchJob> packageJobs = entry.getValue();

            w.write("### 📦 " + packageName + "\n\n");
            for (BatchJob job : packageJobs) {
              w.write("- **" + job.jobName() + "** (`" + job.className() + "`)\n");
            }
            w.write("\n");
          }
        }

        // フッター
        w.write("---\n\n");
        w.write("*このファイルは CRUD Analyzer によって自動生成されました。*\n");
      }
      logger.info("CRUDマトリクスインデックスを出力しました: {}", out);
    } catch (IOException ex) {
      logger.error("CRUDマトリクスインデックスの出力に失敗しました: {}", out, ex);
    }
  }

  /**
   * パッケージ別にCRUDマトリクスを出力する
   *
   * <p>エンドポイントをパッケージごとにグループ化し、 各パッケージ専用のCRUDマトリクスを個別のファイルとして出力します。
   *
   * <p>出力ファイルは {@code crud/} ディレクトリ内に、 パッケージ名をベースとしたファイル名で保存されます。
   *
   * @param baseDir 出力ベースディレクトリ
   * @param links CRUDリンク一覧
   * @param endpoints 全エンドポイント一覧
   */
  public void writeMatrixByPackage(Path baseDir, List<CrudLink> links, List<Endpoint> endpoints) {
    // パッケージ別にエンドポイントをグループ化
    Map<String, List<Endpoint>> packageGroups =
        endpoints.stream().collect(Collectors.groupingBy(Endpoint::packageName));

    for (Map.Entry<String, List<Endpoint>> entry : packageGroups.entrySet()) {
      String packageName = entry.getKey();
      List<Endpoint> packageEndpoints = entry.getValue();

      // このパッケージのエンドポイントに関連するCRUDリンクのみフィルタ
      List<CrudLink> packageLinks =
          links.stream().filter(link -> packageEndpoints.contains(link.ep())).toList();

      if (!packageLinks.isEmpty()) {
        // パッケージ名をファイル名に変換（ドットをハイフンに）
        String fileName = packageName.replace(".", "-") + "-crud-matrix.md";
        Path outputPath = baseDir.resolve("crud/packages").resolve(fileName);

        writeMatrixWithHeader(outputPath, packageLinks, packageName);
      }
    }
  }

  /**
   * ヘッダー付きでCRUDマトリクスを出力する
   *
   * <p>指定されたCRUDリンクから、パッケージ名とタイムスタンプを含む ヘッダー付きのMarkdownファイルを生成します。
   *
   * @param out 出力先パス
   * @param links 対象のCRUDリンク一覧
   * @param packageName パッケージ名（ヘッダーに表示）
   */
  private void writeMatrixWithHeader(Path out, List<CrudLink> links, String packageName) {
    try {
      Files.createDirectories(out.getParent());
    } catch (IOException ignored) {
      logger.error("出力ディレクトリ作成失敗: {}", out.getParent());
    }

    var eps = links.stream().map(CrudLink::ep).distinct().toList();
    var tables = links.stream().map(CrudLink::table).distinct().sorted().toList();

    try (var w = Files.newBufferedWriter(out)) {
      // ヘッダー情報を追加
      w.write("# CRUD Matrix - " + packageName + "\n\n");
      w.write("Generated at: " + java.time.LocalDateTime.now() + "\n\n");
      w.write("## Endpoints: " + eps.size() + ", Tables: " + tables.size() + "\n\n");

      // CRUDマトリクステーブル
      w.write("| URL | HTTP | " + String.join(" | ", tables) + " |\n");
      w.write("|---|---|" + "---|".repeat(tables.size()) + "\n");

      for (Endpoint ep : eps) {
        var row = new ArrayList<String>();
        row.add(ep.url());
        row.add(ep.httpMethod());
        for (String t : tables) {
          var cell =
              links.stream()
                  .filter(l -> l.ep().equals(ep) && l.table().equals(t))
                  .map(CrudLink::crud)
                  .sorted()
                  .distinct()
                  .collect(Collectors.joining(""));
          row.add(cell);
        }
        w.write("| " + String.join(" | ", row) + " |\n");
      }
    } catch (IOException e) {
      logger.error("CRUDマトリクス出力失敗: {}", out);
      throw new UncheckedIOException(e);
    }
  }
}
