package org.sqlflow.client;

import java.util.List;

public class MessageHandlerExample implements MessageHandler {
  @Override
  public void handleHTML(String html) {
    System.out.println(html);
  }

  @Override
  public void handleText(String text) {
    System.out.println(text);
  }

  @Override
  public void handleHeader(List<String> columnNames) {
    columnNames.forEach(
        col -> {
          System.out.print("\t" + col);
        });
    System.out.println();
  }

  @Override
  public void handleRow(List<com.google.protobuf.Any> row) {
    row.forEach(System.out::println);
  }
}
