package dev.example.crudscan.output;

import dev.example.crudscan.model.Models.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** CRUD解析結果をJSON形式で出力するライター */
public class JsonWriter {

  /** JSON配列の終了文字列 */
  private static final String JSON_ARRAY_END = "  ],\n";

  /** デフォルトコンストラクタ */
  public JsonWriter() {
    // デフォルトコンストラクタ
  }

  /**
   * 解析結果をJSONファイルに出力する
   *
   * @param out 出力先パス
   * @param eps エンドポイント一覧
   * @param edges 呼び出し関係一覧
   * @param sqls SQLマッピング一覧
   * @param links CRUDリンク一覧
   */
  public void write(
      Path out,
      List<Endpoint> eps,
      List<CallEdge> edges,
      List<SqlMapping> sqls,
      List<CrudLink> links) {
    write(out, eps, edges, sqls, links, List.of());
  }

  /**
   * 解析結果をJSONファイルに出力する（バッチJob対応版）
   *
   * @param out 出力先パス
   * @param eps エンドポイント一覧
   * @param edges 呼び出し関係一覧
   * @param sqls SQLマッピング一覧
   * @param links CRUDリンク一覧
   * @param batchJobs バッチJob一覧
   */
  public void write(
      Path out,
      List<Endpoint> eps,
      List<CallEdge> edges,
      List<SqlMapping> sqls,
      List<CrudLink> links,
      List<BatchJob> batchJobs) {
    try {
      Files.createDirectories(out.getParent());
    } catch (IOException ignored) {
      // 出力ディレクトリ作成失敗時は意図的に無視
    }
    try (var w = Files.newBufferedWriter(out)) {
      w.write("{\n");
      writeEndpoints(w, eps);
      writeEdges(w, edges);
      writeSqlMappings(w, sqls);
      writeCrudLinks(w, links);
      writeBatchJobs(w, batchJobs);
      w.write("}\n");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** エンドポイント一覧をJSONに出力 */
  private void writeEndpoints(BufferedWriter w, List<Endpoint> eps) throws IOException {
    w.write("  \"endpoints\": [\n");
    for (int i = 0; i < eps.size(); i++) {
      var e = eps.get(i);
      w.write(
          String.format(
              "    {\"method\":\"%s\",\"url\":\"%s\",\"controller\":\"%s\",\"func\":\"%s\"}%s%n",
              e.httpMethod(),
              e.url(),
              e.controller(),
              e.method(),
              (i < eps.size() - 1) ? "," : ""));
    }
    w.write(JSON_ARRAY_END);
  }

  /** 呼び出し関係一覧をJSONに出力 */
  private void writeEdges(BufferedWriter w, List<CallEdge> edges) throws IOException {
    w.write("  \"edges\": [\n");
    for (int i = 0; i < edges.size(); i++) {
      var e = edges.get(i);
      w.write(
          String.format(
              "    {\"fromClass\":\"%s\",\"fromMethod\":\"%s\",\"toClass\":\"%s\",\"toMethod\":\"%s\"}%s%n",
              e.fromClass(),
              e.fromMethod(),
              e.toClass(),
              e.toMethod(),
              (i < edges.size() - 1) ? "," : ""));
    }
    w.write(JSON_ARRAY_END);
  }

  /** SQLマッピング一覧をJSONに出力 */
  private void writeSqlMappings(BufferedWriter w, List<SqlMapping> sqls) throws IOException {
    w.write("  \"sql\": [\n");
    for (int i = 0; i < sqls.size(); i++) {
      var s = sqls.get(i);
      w.write(
          String.format(
              "    {\"mapper\":\"%s\",\"method\":\"%s\",\"op\":\"%s\",\"tables\":%s}%s%n",
              s.mapperClass(),
              s.mapperMethod(),
              s.op(),
              toJsonArray(s.tables()),
              (i < sqls.size() - 1) ? "," : ""));
    }
    w.write(JSON_ARRAY_END);
  }

  /** CRUDリンク一覧をJSONに出力 */
  private void writeCrudLinks(BufferedWriter w, List<CrudLink> links) throws IOException {
    w.write("  \"links\": [\n");
    for (int i = 0; i < links.size(); i++) {
      var l = links.get(i);
      w.write(
          String.format(
              "    {\"url\":\"%s\",\"http\":\"%s\",\"table\":\"%s\",\"crud\":\"%s\"}%s%n",
              l.ep().url(),
              l.ep().httpMethod(),
              l.table(),
              l.crud(),
              (i < links.size() - 1) ? "," : ""));
    }
    w.write(JSON_ARRAY_END);
  }

  /** バッチJob一覧をJSONに出力 */
  private void writeBatchJobs(BufferedWriter w, List<BatchJob> batchJobs) throws IOException {
    w.write("  \"batchJobs\": [\n");
    for (int i = 0; i < batchJobs.size(); i++) {
      var job = batchJobs.get(i);
      w.write(
          String.format(
              "    {\"className\":\"%s\",\"jobName\":\"%s\",\"packageName\":\"%s\"}%s%n",
              job.className(),
              job.jobName(),
              job.packageName(),
              (i < batchJobs.size() - 1) ? "," : ""));
    }
    w.write("  ]\n");
  }

  /**
   * 文字列リストをJSON配列文字列に変換
   *
   * @param list 対象リスト
   * @return JSON配列文字列
   */
  private String toJsonArray(List<String> list) {
    var sb = new StringBuilder("[");
    for (int i = 0; i < list.size(); i++) {
      sb.append('\"').append(list.get(i)).append('\"');
      if (i < list.size() - 1) sb.append(',');
    }
    sb.append(']');
    return sb.toString();
  }
}
