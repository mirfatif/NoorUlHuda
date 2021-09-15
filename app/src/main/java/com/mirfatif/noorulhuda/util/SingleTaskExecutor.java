package com.mirfatif.noorulhuda.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SingleTaskExecutor extends ScheduledThreadPoolExecutor {

  public SingleTaskExecutor() {
    super(1);
  }

  public int getPendingTasks() {
    return getQueue().size();
  }
}
