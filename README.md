# lucene-local-repo

lucene-local-repo 是对 Lucene 的操作封装，包含中文分词，适用于为桌面应用添加搜索引擎的功能。

lucene-local-repo 对于文本内容封装为 Text 对象，该对象只包含 id 和 content 两个属性，其他属性不会被加到索引中，这样使得索引内容大小最小化。

注意，Lucene 索引并不适合直接当数据库用。

### 添加依赖

本项目只有很少的几个类，就不发布到 maven central 了，请自行 clone 到本地并执行 `mvn install` 安装。

### 创建

```java
LuceneRepository luceneRepository = LuceneRepository.builder()
      .path(Paths.get("target/index"))
      .analyzer(JcsegAnalyzerBuilder.build())
      .build();
```

### 添加、更新索引内容

```java
// addContent() 添加单个内容；
// addContents() 批量添加内容
luceneRepository.addContent(new Text(1, "你好"));
// updateContent() 更新单个内容；
luceneRepository.updateContent(new Text(1, "你好呀"));
```

### 根据ID删除内容

```java
luceneRepository.deleteById(1);
```

### 搜索内容

```java
int pageSize = 10;

// 查询一个或多个关键字，每个关键字都是必须包含
LuceneSearchResult result = luceneRepository.searchByContent(pageSize, "你好", "呀");
System.out.println("结果数: " + result.getCount());

// 查询只需要调用一次，然后 result 对象可以一直持有
if (result.hasData()) {
    result.getDocuments().forEach(doc -> System.out.println(
        "id = " + doc.getField("id").numericValue()
    ));
}

// 需要翻页时只要调用 result 对象即可
result = result.nextPage();
```

```java
// 使用 Lucene 语法来进行搜索
LuceneSearchResult result = luceneRepository.search(pageSize, "你好 OR 呀");
    ...
```
