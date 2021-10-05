package com.hyd.lucenelocalrepo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yiding_he
 */
class LuceneRepositoryTest {

  private static List<String> lines;

  private LuceneRepository luceneRepository;

  @BeforeAll
  public static void beforeAll() throws Exception {
    URL resource = LuceneRepositoryTest.class.getResource("/sample.txt");
    if (resource == null) {
      throw new IllegalStateException("Cannot read sample.txt");
    }
    lines = Files.lines(Paths.get(resource.toURI())).collect(Collectors.toList());
  }

  @BeforeEach
  public void BeforeEach() throws Exception {
    FileUtils.deleteDirectory(new File("target/index"));
    luceneRepository = LuceneRepository.builder()
      .path(Paths.get("target/index"))
      .analyzer(JcsegAnalyzerBuilder.build())
      .build();

    List<Text> textList = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      Text text = new Text(i, line);
      textList.add(text);
    }

    luceneRepository.addContents(textList);
  }

  @AfterEach
  public void afterEach() {
    if (luceneRepository != null) {
      luceneRepository.close();
    }
  }

  private void outputDocument(Document doc) {
    if (doc == null) {
      System.out.println("(doc is null)");
      return;
    }
    IndexableField idField = doc.getField("id");
    if (idField == null) {
      System.out.println("(field 'id' not found)");
      return;
    }
    int id = idField.numericValue().intValue();
    System.out.println("id = " + id);
    System.out.println("content = " + StringUtils.abbreviate(lines.get(id), 30));
  }

  //////////////////////////////////////////////////////////////

  @Test
  public void testQuery() throws Exception {
    LuceneSearchResult result = luceneRepository.searchByContent(10, "中国", "评级"); // 每页10条
    System.out.println("count = " + result.getCount());
    result.allForEach(this::outputDocument);
  }

  @Test
  public void testQueryMultiPage() throws Exception {
    LuceneSearchResult result = luceneRepository.searchByContent(10, "中国"); // 每页10条
    System.out.println("count = " + result.getCount());
    result.allForEach(this::outputDocument);
  }

  @Test
  public void testQueryPhrase() throws Exception {
    LuceneSearchResult result = luceneRepository.search(10, "中国 +评级"); // 每页10条
    System.out.println("count = " + result.getCount());
    result.allForEach(this::outputDocument);
  }

  @Test
  public void testFindById() throws Exception {
    Document doc = luceneRepository.findById(1);
    outputDocument(doc);
  }

  @Test
  public void testDeleteById() throws Exception {
    luceneRepository.deleteById(1);

    LuceneSearchResult result = luceneRepository.searchByContent(10, "瑞银");
    result.allForEach(this::outputDocument);
  }
}