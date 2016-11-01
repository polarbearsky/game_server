package com.netty.game.server.schedulers;

import java.util.concurrent.TimeUnit;

public class CommonJobContext extends JobContext {

	private long delay;
	private long period;
	private TimeUnit timeUnit;
	public CommonJobContext(long delay, long period, TimeUnit timeUnit){
		this.delay = delay;
		this.period = period;
		this.timeUnit = timeUnit;
	}
	
	@Override
	public long getDelay() {
		return this.delay;
	}

	@Override
	public long getPeriod() {
		return this.period;
	}

	@Override
	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}

}
