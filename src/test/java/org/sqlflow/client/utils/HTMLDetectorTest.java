package org.sqlflow.client.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HTMLDetectorTest {
  @Test
  public void testIsHTML() {
    assert HTMLDetector.validate("<html>yes</html>");
    assert HTMLDetector.validate("<image/>");
    assert HTMLDetector.validate("<html>it's a BUG</htlm>");
    assert !HTMLDetector.validate("<html no</html>");
    assert !HTMLDetector.validate("no");
    assert !HTMLDetector.validate("<html>no");
  }
}
