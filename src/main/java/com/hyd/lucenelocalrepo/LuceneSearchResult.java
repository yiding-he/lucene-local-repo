package com.hyd.lucenelocalrepo;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author yiding_he
 */
public class LuceneSearchResult {

  public static LuceneSearchResult EMPTY = new LuceneSearchResult(new Page(null, null), null);

  public static class Page {

    public final TopDocs topDocs;

    public final List<Document> documents;

    public Page(TopDocs topDocs, List<Document> documents) {
      this.topDocs = topDocs;
      this.documents = documents;
    }
  }

  private final Page page;

  private final Function<ScoreDoc, Page> continueSearch;

  public LuceneSearchResult(
    Page page,
    Function<ScoreDoc, Page> continueSearch
  ) {
    this.page = page;
    this.continueSearch = continueSearch;
  }

  public boolean hasData() {
    return this.page.topDocs != null && this.page.topDocs.scoreDocs.length > 0;
  }

  public TopDocs getTopDocs() {
    return this.page.topDocs;
  }

  public List<Document> getDocuments() {
    return this.page.documents;
  }

  public LuceneSearchResult nextPage() {
    if (this.page.topDocs == null) {
      return EMPTY;
    } else {
      ScoreDoc[] scoreDocs = this.page.topDocs.scoreDocs;
      if (scoreDocs == null || scoreDocs.length == 0) {
        return EMPTY;
      } else {
        Page nextPage = continueSearch.apply(scoreDocs[scoreDocs.length - 1]);
        return new LuceneSearchResult(nextPage, continueSearch);
      }
    }
  }

  public long getCount() {
    if (this.page == null || this.page.topDocs == null) {
      return 0;
    }
    return this.page.topDocs.totalHits.value;
  }

  /**
   * 遍历所有查询结果，自动翻页
   */
  public void allForEach(Consumer<Document> consumer) {
    LuceneSearchResult result = this;
    while (result.hasData()) {
      result.getDocuments().forEach(consumer);
      result = result.nextPage();
    }
  }
}
