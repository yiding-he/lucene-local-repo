package com.hyd.lucenelocalrepo;

/**
 * @author yiding_he
 */
public class Text {

  private long id;

  private String content;

  public Text() {
  }

  public Text(long id, String content) {
    this.id = id;
    this.content = content;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
