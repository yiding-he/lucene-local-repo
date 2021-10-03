package com.hyd.lucenelocalrepo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StandardDirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 对 Lucene 本地索引的读写操作封装
 *
 * @author yiding_he
 */
public class LuceneRepository implements Closeable {

  private final Path repositoryPath;

  private final Analyzer analyzer;      // for indexing and searching

  private IndexWriter indexWriter;      // always created and open, re-created after searching

  private IndexReader indexReader;      // created only when searching

  public static LuceneRepositoryBuilder builder() {
    return new LuceneRepositoryBuilder();
  }

  protected LuceneRepository(Path path, Analyzer analyzer) {
    this.analyzer = analyzer;
    this.repositoryPath = path;
  }

  private IndexWriter getIndexWriter() throws IOException {
    if (indexWriter == null) {
      FSDirectory directory = FSDirectory.open(repositoryPath);
      IndexWriterConfig writerConfig = new IndexWriterConfig(this.analyzer);
      this.indexWriter = new IndexWriter(directory, writerConfig);
    }
    return indexWriter;
  }

  private IndexReader openReader() throws IOException {
    closeAndClear(true);
    FSDirectory directory = FSDirectory.open(repositoryPath);
    this.indexReader = StandardDirectoryReader.open(directory);
    return this.indexReader;
  }

  private Document readSafe(IndexReader reader, int docId) {
    try {
      return reader.document(docId);
    } catch (IOException e) {
      throw new LuceneRepositoryException(e);
    }
  }

  public synchronized void addDocument(Document document) {
    try {
      getIndexWriter().addDocument(document);
    } catch (IOException e) {
      throw new LuceneRepositoryException(e);
    }
  }

  public synchronized LuceneSearchResult searchDocument(String field, String keyword, int pageSize) {

    Function<ScoreDoc, LuceneSearchResult.Page> searchMethod = (scoreDoc) -> {
      try(IndexReader indexReader = openReader()) {
        QueryBuilder queryBuilder = new QueryBuilder(analyzer);
        Query query = queryBuilder.createPhraseQuery(field, keyword);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.searchAfter(scoreDoc, query, pageSize);
        List<Document> documents = Stream.of(topDocs.scoreDocs)
          .map(sd -> readSafe(indexReader, sd.doc))
          .collect(Collectors.toList());
        return new LuceneSearchResult.Page(topDocs, documents);
      } catch (IOException e) {
        throw new LuceneRepositoryException(e);
      }
    };

    return new LuceneSearchResult(searchMethod.apply(null), searchMethod);
  }

  @Override
  public synchronized void close() {
    closeAndClear(false);
  }

  private void closeAndClear(boolean doClear) {
    Stream.of(this.indexReader, this.indexWriter).forEach(c -> {
      try {
        if (c != null) {
          c.close();
        }
      } catch (IOException e) {
        // ignore error
      }
    });

    if (doClear) {
      this.indexReader = null;
      this.indexWriter = null;
    }
  }
}
