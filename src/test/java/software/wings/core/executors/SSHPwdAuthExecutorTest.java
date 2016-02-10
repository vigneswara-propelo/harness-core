package software.wings.core.executors;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;

/**
 * Created by anubhaw on 2/10/16.
 */
public class SSHPwdAuthExecutorTest {
  @Test
  public void testExecute() throws Exception {
    SSHSessionConfig config = new SSHSessionConfig("localhost", 3333, "osboxes", "osboxes.org");
    SSHPwdAuthExecutor executor = new SSHPwdAuthExecutor(config);

    Thread thread1 = new Thread(() -> {
      System.out.println("Sorting numbers");
      try {
        executor.execute("seq 10000000 > numbers && sort numbers | sort | sort |wc -l && rm numbers");
        System.out.println("Sorting completed");
      } catch (Exception ex) {
        System.out.println("Sorting failed: " + ex.getStackTrace());
      }
    });

    Thread thread2 = new Thread(() -> {
      try {
        System.out.println("Stop sorting");
        executor.abort();
        if (executor.getChannel() == null) {
          System.out.println("Channel closed successfully");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("Sorting Stopped");
    });

    System.out.println("start threads");
    thread1.setName("Sorting thread");
    thread1.start();
    Thread.sleep(2000);
    thread2.setName("Abort thread");
    thread2.start();
    Thread.sleep(1000000);
  }
}