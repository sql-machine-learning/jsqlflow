package org.sqlflow.client;

import java.util.List;

public class MessageHandler2020 implements MessageHandler {
  private final String myName = this.getClass().getSimpleName();

  @Override
  public void handleHTML(String html) {
    tag();
    System.out.println(html);
  }

  @Override
  public void handleText(String text) {
    tag();
    System.out.println(text);
  }

  @Override
  public void handleEOE() {
    tag();
  }

  @Override
  public void handleHeader(List<String> columnNames) {
    tag();
    columnNames.forEach(
        col -> {
          System.out.print("\t" + col);
        });
    System.out.println();
  }

  @Override
  public void handleRows(List<com.google.protobuf.Any> rows) {
    tag();
    rows.forEach(
        row -> {
          System.out.println("*");
        });
  }

  private void tag() {
    String lastFunctionName = Thread.currentThread().getStackTrace()[2].getMethodName();
    System.out.println("[" + myName + "::" + lastFunctionName + "]");
  }
}
