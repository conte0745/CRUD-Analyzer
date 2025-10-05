plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("com.diffplug.spotless") version "6.25.0"
}

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories { mavenCentral() }

dependencies {
  implementation("com.github.javaparser:javaparser-core:3.26.2")
  implementation("com.github.jsqlparser:jsqlparser:5.3")
  implementation("org.slf4j:slf4j-api:2.0.13")
  runtimeOnly("ch.qos.logback:logback-classic:1.5.7")
}

tasks.test {
    useJUnitPlatform()
    // テストを単一スレッドで実行（モノレル実行）
    maxParallelForks = 1
    // JVMごとのテスト実行も単一に制限
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}

application { mainClass.set("dev.example.crudscan.AnalyzerMain") }

spotless {
    java {
        googleJavaFormat()
        importOrder() // インポート順の整理
        removeUnusedImports() // 未使用インポートの削除
        trimTrailingWhitespace() // 行末の空白削除
        endWithNewline() // ファイル末尾に改行を追加
        target("src/**/*.java")
    }
}

tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }

tasks.shadowJar {
  archiveBaseName.set("crud-analyzer")
  archiveClassifier.set("all")  
}
