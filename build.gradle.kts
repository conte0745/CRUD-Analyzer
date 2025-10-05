plugins {
    java
    application
    jacoco
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
  
  // テスト依存関係
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testImplementation("org.mockito:mockito-core:5.6.0")
  testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
  testImplementation("org.assertj:assertj-core:3.24.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // テストを単一スレッドで実行（モノレル実行）
    maxParallelForks = 1
    // JVMごとのテスト実行も単一に制限
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    
    // テスト出力を表示
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    
    // デバッグログを有効化
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
    systemProperty("org.slf4j.simpleLogger.log.dev.example.crudscan", "DEBUG")
    
    // JaCoCoカバレッジ測定を有効化
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCoカバレッジレポート設定
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
}

// カバレッジ検証設定
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.85".toBigDecimal() // 85%以上のカバレッジを要求
            }
        }
    }
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

// JavaDoc設定
tasks.javadoc {
    options {
        encoding = "UTF-8"
        (this as StandardJavadocDocletOptions).apply {
            // 概要ページの生成を有効化
            overview = "src/main/java/overview.html"
            windowTitle = "CRUD Analyzer API Documentation"
            docTitle = "CRUD Analyzer"
            header = "<b>CRUD Analyzer</b>"
            bottom = "Copyright © 2024 CRUD Analyzer Team. All rights reserved."
            
            // 詳細なドキュメント生成オプション
            addBooleanOption("html5", true)
            addStringOption("Xdoclint:none", "-quiet")
            links("https://docs.oracle.com/en/java/javase/21/docs/api/")
            charset("UTF-8")
        }
    }
}

// 品質チェックタスクの定義
tasks.register("qualityCheck") {
    description = "全ての品質チェックを実行"
    group = "verification"
    
    dependsOn(
        tasks.test,
        tasks.jacocoTestReport,
        tasks.jacocoTestCoverageVerification,
        tasks.spotlessApply,
        tasks.javadoc
    )
}

// CIで使用するタスク
tasks.register("ci") {
    description = "CI環境で実行する全てのチェック"
    group = "build"
    
    dependsOn(
        tasks.clean,
        tasks.compileJava,
        tasks.compileTestJava,
        tasks.named("qualityCheck"),
        tasks.shadowJar
    )
}
