package com.hyd.lucenelocalrepo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StandardDirectoryReader;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
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

  @FunctionalInterface
  private interface IndexWriterTask {

    void run(IndexWriter indexWriter) throws IOException;
  }

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

  /**
   * 对索引进行更新操作，并且自动提交或回滚
   */
  private void withIndexWriter(IndexWriterTask task) {
    try {
      IndexWriter indexWriter = getIndexWriter();
      try {
        task.run(indexWriter);
        indexWriter.commit();
      } catch (IOException e) {
        indexWriter.rollback();
        throw e;
      }
    } catch (IOException e) {
      throw new LuceneRepositoryException(e);
    }
  }

  /**
   * 批量添加内容。如果有多个内容要添加，请尽量使用批量方式
   */
  public synchronized void addContents(Collection<Text> textCollection) {
    List<Document> documents = textCollection.stream().map(this::convert).collect(Collectors.toList());
    withIndexWriter(indexWriter -> {
      for (Document document : documents) {
        indexWriter.addDocument(document);
      }
    });
  }

  /**
   * 添加单个内容
   */
  public synchronized void addContent(Text text) {
    Document document = convert(text);
    withIndexWriter(indexWriter -> indexWriter.addDocument(document));
  }

  /**
   * 更新单个内容
   */
  public synchronized void updateContent(Text text) {
    withIndexWriter(indexWriter -> {
      Query query = LongPoint.newExactQuery("_id", text.getId());
      indexWriter.deleteDocuments(query);
      Document document = convert(text);
      indexWriter.addDocument(document);
    });
  }

  private Document convert(Text text) {
    Document document = new Document();
    document.add(new LongPoint("_id", text.getId()));
    document.add(new StoredField("id", text.getId()));
    document.add(new TextField("content", text.getContent(), Field.Store.NO));
    return document;
  }

  /**
   * 使用 Lucene 提供的查询语法执行查询。语法参考文档
   * http://lucene.apache.org/core/8_10_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description
   *
   * @param pageSize 分页大小
   * @param phrase   查询语句
   *
   * @return 查询结果
   */
  public synchronized LuceneSearchResult search(int pageSize, String phrase) {
    Function<ScoreDoc, LuceneSearchResult.Page> searchMethod = (scoreDoc) -> {
      try (IndexReader indexReader = openReader()) {
        StandardQueryParser queryParser = new StandardQueryParser(analyzer);
        Query query = queryParser.parse(phrase, "content");
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.searchAfter(scoreDoc, query, pageSize);
        List<Document> documents = Stream.of(topDocs.scoreDocs)
          .map(sd -> readSafe(indexReader, sd.doc))
          .collect(Collectors.toList());
        return new LuceneSearchResult.Page(topDocs, documents);
      } catch (Exception e) {
        throw new LuceneRepositoryException(e);
      }
    };

    return new LuceneSearchResult(searchMethod.apply(null), searchMethod);

  }

  /**
   * 使用一个或多个关键字执行查询
   *
   * @param pageSize 分页大小
   * @param keywords 内容关键字
   *
   * @return 查询结果
   */
  public synchronized LuceneSearchResult searchByContent(int pageSize, String... keywords) {
    Function<ScoreDoc, LuceneSearchResult.Page> searchMethod = (scoreDoc) -> {
      try (IndexReader indexReader = openReader()) {
        QueryBuilder queryBuilder = new QueryBuilder(analyzer);
        BooleanQuery.Builder rootBuilder = new BooleanQuery.Builder();
        for (String keyword : keywords) {
          Query query = queryBuilder.createPhraseQuery("content", keyword);
          rootBuilder.add(query, BooleanClause.Occur.MUST);
        }
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.searchAfter(scoreDoc, rootBuilder.build(), pageSize);
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

  public Document findById(int id) {
    try (IndexReader indexReader = openReader()) {
      IndexSearcher searcher = new IndexSearcher(indexReader);
      Query query = LongPoint.newExactQuery("_id", id);
      TopDocs topDocs = searcher.search(query, 1);
      if (topDocs.scoreDocs.length > 0) {
        return indexReader.document(topDocs.scoreDocs[0].doc);
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new LuceneRepositoryException(e);
    }
  }

  public void deleteById(long id) {
    withIndexWriter(indexWriter -> {
      Query query = LongPoint.newExactQuery("_id", id);
      indexWriter.deleteDocuments(query);
    });
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
