package dev.example.crudscan.output;

import dev.example.crudscan.model.Models.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CRUDマトリクスをPlantUML形式でCRUD関係図を出力するライター */
public class PlantumlWriter {
  private static final Logger logger = LoggerFactory.getLogger(PlantumlWriter.class);

  /** デフォルトコンストラクタ */
  public PlantumlWriter() {
    // デフォルトコンストラクタ
  }

  /**
   * CRUDマトリクスをPlantUMLファイルに出力する
   *
   * @param out 出力先パス
   * @param links CRUDリンク一覧
   */
  public void write(Path out, List<CrudLink> links) {
    write(out, links, List.of(), List.of());
  }

  /**
   * CRUDマトリクスをPlantUMLファイルに出力する（バッチJob対応版）
   *
   * @param out 出力先パス
   * @param links CRUDリンク一覧
   * @param batchJobs バッチJob一覧
   * @param edges 呼び出し関係一覧
   */
  public void write(
      Path out, List<CrudLink> links, List<BatchJob> batchJobs, List<CallEdge> edges) {
    try {
      Files.createDirectories(out.getParent());
    } catch (IOException ignored) {
      logger.error("フォルダ作成失敗 : {}", out.getParent());
      // 出力ディレクトリ作成失敗時は意図的に無視
    }
    try (var w = Files.newBufferedWriter(out)) {
      w.write("@startuml\n");

      // Web APIの図
      if (!links.isEmpty()) {
        w.write("actor User\n");
        links.stream()
            .limit(8)
            .forEach(
                l -> {
                  try {
                    w.write(
                        "User -> "
                            + l.ep().controller()
                            + ": "
                            + l.ep().httpMethod()
                            + " "
                            + l.ep().url()
                            + "\n");
                    w.write(l.ep().controller() + " -> Service: ...\n");
                    w.write("Service -> " + l.table() + ": " + l.crud() + "\n");
                    w.write(l.table() + " --> Service: result\n");
                    w.write("Service --> " + l.ep().controller() + "\n");
                    w.write(l.ep().controller() + " --> User: 200\n");
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                });
      }

      // バッチJobの図
      if (!batchJobs.isEmpty()) {
        w.write("participant Scheduler\n");
        batchJobs.stream()
            .limit(5)
            .forEach(
                job -> {
                  try {
                    w.write(
                        "Scheduler -> " + job.className() + ": trigger " + job.jobName() + "\n");
                    w.write(job.className() + " -> Service: execute\n");

                    // このバッチJobに関連するSQL操作を探す
                    edges.stream()
                        .filter(edge -> edge.fromClass().equals(job.className()))
                        .limit(3)
                        .forEach(
                            edge -> {
                              try {
                                w.write(
                                    "Service -> " + edge.toClass() + ": " + edge.toMethod() + "\n");
                                w.write(edge.toClass() + " -> Database: SQL\n");
                                w.write("Database --> " + edge.toClass() + ": result\n");
                                w.write(edge.toClass() + " --> Service\n");
                              } catch (IOException e) {
                                throw new UncheckedIOException(e);
                              }
                            });

                    w.write("Service --> " + job.className() + ": complete\n");
                    w.write(job.className() + " --> Scheduler: finished\n");
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                });
      }

      w.write("@enduml\n");
    } catch (IOException e) {
      logger.error("PlantUML出力失敗: {}", out);
      throw new UncheckedIOException(e);
    }
  }
}
