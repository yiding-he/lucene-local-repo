package com.hyd.lucenelocalrepo;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yiding_he
 */
class LuceneRepositoryTest {

  @Test
  public void testCreateRepository() throws Exception {
    LuceneRepository repository = LuceneRepository.builder()
      .path(Paths.get("target/index"))
      .analyzer(JcsegAnalyzerBuilder.build())
      .build();

    repository.close();
  }

  @Test
  public void testQuery() throws Exception {
    LuceneRepository repository = LuceneRepository.builder()
      .path(Paths.get("target/index"))
      .analyzer(JcsegAnalyzerBuilder.build())
      .build();

    Document document = new Document();
    document.add(new LongPoint("id", 1L));
    document.add(new TextField("content", "张三是一个好人", Field.Store.YES));
    repository.addDocument(document);

    LuceneSearchResult result = repository.searchDocument("content", "好人", 10);
    while (result.hasData()) {
      result.getDocuments().forEach(doc -> System.out.println(doc.getField("content").stringValue()));
      result = result.nextPage();
    }
  }
}