package com.hyd.lucenelocalrepo;

import org.apache.lucene.analysis.Analyzer;

import java.nio.file.Path;

/**
 * @author yiding_he
 */
public class LuceneRepositoryBuilder {

  private Path path;

  private Analyzer analyzer;

  public LuceneRepositoryBuilder path(Path path) {
    this.path = path;
    return this;
  }

  public LuceneRepositoryBuilder analyzer(Analyzer analyzer) {
    this.analyzer = analyzer;
    return this;
  }

  public LuceneRepository build() {
    return new LuceneRepository(path, analyzer);
  }
}
