
# React x Spring

Web フロントエンド(TypeScript) と BFF(Spring Boot) をタイプセーフに繋ぐ 2022

# はじめに

本記事の目的は、Web フロントエンド(TypeScript) と BFF(Spring Boot) をタイプセーフに繋ぐ方法を紹介することです。

以前、以下の記事を書きました。

https://qiita.com/chibato/items/e4a748db12409b40c02f

この記事からの変更点は、次の２点です。
* OpenAPI Spec を出力する [Springfox](https://github.com/springfox/springfox) を [springdoc-openapi](https://springdoc.org/) に変更しています。Springfox はメンテナンスされなくなっているようです。
* 生成するコードが Angular 用のクライアントでしたが、より汎用的な fetch のクライアントに変更しています。サンプルコードも React に変更しています。

# 作ったもの

https://github.com/chibat/react-spring

# サンプルアプリの起動

## 前提条件
以下がインストールされている必要があります。
* JDK: 17+
* Node.js: 16+ 

サンプルアプリのレポジトリを fork し Codespaces や VS Code の Remote Container で開けばこの環境になります。

## ローカル環境でのアプリケーションの起動

まずはサンプルアプリのレポジトリを clone します。
```
$ git clone https://github.com/chibat/react-spring.git
$ cd react-spring
```

任意の IDE で Spring Boot アプリケーションの起動を実行します。

Gradle から実行する場合は、以下のコマンドになります。
```
./gradlew bootRun
```

次に開発用フロントエンドサーバの起動

```
$ cd frontend
$ npm install
$ npm start
```

Web ブラウザで以下の URL にアクセスします。

http://localhost:3000/

以下のような画面が表示され、二つの数字を入力し、「=」ボタンをクリックすると足し算の計算結果が表示されます。


## 本番用ビルドと起動

```
$ ./gradlew bootJar
$ java -jar build/libs/react-spring-0.0.1-SNAPSHOT.jar
```
# コードの解説

## BFF(Spring Boot) のコード

### src/main/java/app/model/Request.java

以下は、フロンエンドからのリクエストを受け取るクラスです。Java 16 で追加された `record` を利用しています。二つの整数を格納します。
```java
package app.model;

public record Request(int arg1, int arg2) {
}
```

### src/main/java/app/model/Response.java

以下は、フロントエンドへのレスポンスを格納クラスです。同様に `record` を利用しています。
```java
package app.model;

public record Response(int result) {
}
```

### src/main/java/app/AppController.java

以下は、リクエストを受け取りレスポンするコントローラクラスです。ここで足し算の計算をしています。
```java
package app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.model.Request;
import app.model.Response;

@RestController
public class AppController {

  @PostMapping("/add")
  public ResponseEntity<Response> add(@RequestBody Request request) {
    var result = request.arg1() + request.arg2();
    var response = new Response(result);
    return ResponseEntity.ok(response);
  }
}
```

メソッド名の `add` は、OpenAPI Spec の `operationId` として扱われ、生成される TypeScript のコードのメソッド名も `add` になります。

### src/test/java/app/OpenApiDocsGenerator.java

以下は、OpenAPI Spec のファイルを生成するテストクラスです。
build ディレクトリ配下に openapi.json という名前で spec ファイルの生成を行います。

```java
package app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = Application.class)
public class OpenApiDocsGenerator {
  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
  }

  @Test
  public void convertSwaggerToAsciiDoc() throws Exception {
    this.mockMvc.perform(get("/v3/api-docs").accept("application/json;charset=UTF-8"))
        .andDo(new Handler()).andExpect(status().isOk());
  }

  public static class Handler implements ResultHandler {

    private final String outputDir = "build";
    private final String fileName = "openapi.json";

    @Override
    public void handle(MvcResult result) throws Exception {
      MockHttpServletResponse response = result.getResponse();
      String swaggerJson = response.getContentAsString();
      Files.createDirectories(Paths.get(outputDir));
      try (BufferedWriter writer =
          Files.newBufferedWriter(Paths.get(outputDir, fileName), StandardCharsets.UTF_8)) {
        writer.write(swaggerJson);
      }
    }
  }
}
```

springdoc-openapi は、OpenAPI Spec のファイルを出力するための機能を gradle plugin, maven plugin で提供しています。
今回このプラグインを利用しなかった理由は二つあります。
一つは、このプラグインを利用するとアプリケーションを起動させるためビルド時にポートを listen してしまいます。この挙動は都合が悪いケースがあると思います。
もう一つは実行時に不要な jar の依存が増える点です。

### build.gradle

ビルドは Gradle を利用しています。

```groovy
plugins {
  id 'org.springframework.boot' version '2.7.2'
  id 'io.spring.dependency-management' version '1.0.12.RELEASE'
  id 'java'
  id 'org.openapi.generator' version '6.0.0' // (1)
  id "com.github.node-gradle.node" version "3.4.0" // (2)
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

repositories {
  mavenCentral()
}

dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-web'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'

  // https://springdoc.org/#spring-webmvc-support
  // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-webmvc-core
  testImplementation 'org.springdoc:springdoc-openapi-webmvc-core:1.6.9' // (3)
}

tasks.named('test') {
  useJUnitPlatform()
}

// ここから追加
def OPEN_API_GENERATE_DIR = "${project.rootDir}/frontend/src/generated"
def OPEN_API_DOCS = "$buildDir/openapi.json"
def FRONTEND_DIR = "${project.rootDir}/frontend"

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin#configuration
openApiGenerate {
  generatorName = "typescript-fetch"
  inputSpec = OPEN_API_DOCS.toString()
  outputDir = OPEN_API_GENERATE_DIR.toString()
  configOptions = [
    // https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/typescript-fetch.md
    useSingleRequestParameter: "false"
  ]
}

task generateOpenApiDocs(type: Test, dependsOn: testClasses) {
    useJUnitPlatform()
    inputs.files fileTree("${project.rootDir}/src/main/java")
    outputs.file file(OPEN_API_DOCS)
    filter {
        includeTestsMatching "app.OpenApiDocsGenerator"
    }
}

node {
  download = true
  version = "16.14.2"
  nodeProjectDir = file(FRONTEND_DIR)
}

task compileFrontend(type: NpmTask) {
  //inputs.files fileTree("${FRONTEND_DIR}/src")
  inputs.files fileTree("${FRONTEND_DIR}")
  outputs.dir file("${project.rootDir}/build/resources/main/static")
  args = ['run', 'build']
}

tasks.openApiGenerate.dependsOn([generateOpenApiDocs, cleanOpenApiGenerate])
compileFrontend.dependsOn([npm_ci, tasks.openApiGenerate])
bootJar.dependsOn compileFrontend
```

## 自動生成されたAPIクライアント(TypeScript)のコード

ディレクトリ `frontend/src/generated` 配下に生成されます。

```
$ ls -R
.:
apis  index.ts  models  runtime.ts

./apis:
AppControllerApi.ts  index.ts

./models:
index.ts  Request.ts  Response.ts
```

## フロントエンド(TypeScript)のコード

### frontend/src/App.tsx

```tsx
import { useState } from 'react';
import { AppControllerApi, Configuration } from './generated';

// 自動生成されたクラスのインスタンスを生成
const api = new AppControllerApi(new Configuration({ basePath: window.location.origin }));

export default function App() {

  const [arg1, setArg1] = useState<string>("");
  const [arg2, setArg2] = useState<string>("");
  const [result, setResult] = useState<number>();

  function add() {
    if (!arg1 || !arg2) {
      return;
    }

    // 自動生成されたコードを利用し、BFF にリクエスト
    api.add({ arg1: Number(arg1), arg2: Number(arg2) }).then(response => {
      setResult(response.result);
    });
  };

  return (
    <>
      <input type="text" value={arg1} onChange={(e) => setArg1(e.target.value)} autoFocus />
      +
      <input type="text" value={arg2} onChange={(e) => setArg2(e.target.value)} />
      <input type="button" value=" = " onClick={add} />
      <span>{result}</span>
    </>
  );
}
```

### frontend/package.json

```jsonc
{
    :
    :
  "scripts": {

    // 開発用フロンエンドサーバの起動の前に APIクライアントのコード生成をしています
    "start": "cd .. && ./gradlew openApiGenerate && cd - && react-scripts start",

    // 本番用のビルドタスク。BUILD_PATH で APIクライアントのコードを出力するディレクトリを指定しています
    "build": "BUILD_PATH='../build/resources/main/static' react-scripts build"
  },
    :
    :
  // 開発用フロンエンドサーバへのリクエストを BFF 側にフォワードする設定です
  "proxy": "http://localhost:8080"
}
```

# おわりに

Hilla framework

