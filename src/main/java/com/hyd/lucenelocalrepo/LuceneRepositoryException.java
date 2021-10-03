package com.hyd.lucenelocalrepo;

/**
 * @author yiding_he
 */
public class LuceneRepositoryException extends RuntimeException {

  public LuceneRepositoryException() {
  }

  public LuceneRepositoryException(String message) {
    super(message);
  }

  public LuceneRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public LuceneRepositoryException(Throwable cause) {
    super(cause);
  }
}
