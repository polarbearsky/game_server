package com.netty.game.server.schedulers;

import java.util.concurrent.TimeUnit;

public abstract class JobContext {
	public abstract long getDelay();
	public abstract long getPeriod();
	public abstract TimeUnit getTimeUnit();
}
