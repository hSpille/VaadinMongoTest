package com.example.vaadinfilelist.singleton;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.vaadinfilelist.BroadCasterListenerInt;

public class Broadcaster implements Serializable {

  private static final long serialVersionUID = -7169865601153323942L;
  static ExecutorService executorService =
      Executors.newSingleThreadExecutor();

  private static LinkedList<BroadCasterListenerInt> listeners =
      new LinkedList<BroadCasterListenerInt>();

  public static synchronized void register(
    BroadCasterListenerInt listener) {
    listeners.add(listener);
  }

  public static synchronized void unregister(
    BroadCasterListenerInt listener) {
    listeners.remove(listener);
  }

  public static synchronized void broadcast(
    final String message) {
    for (final BroadCasterListenerInt listener : listeners)
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          listener.receiveBroadcast(message);
        }
      });
  }
}
