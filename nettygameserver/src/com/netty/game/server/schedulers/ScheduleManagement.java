package com.netty.game.server.schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class ScheduleManagement {
	private static final ScheduleManagement instance = new ScheduleManagement();
	private ScheduledExecutorService scheduler;

	public static ScheduleManagement getInstance(){ 		
		return instance;
	}
	
	private ScheduleManagement(){
		/*
		 * 一台虚拟机仅启动一个线程,绑定多个任务需要按顺序执行,
		 * 当任务执行时间较长时,延迟较大,
		 * 如果实时要求高,可考虑开一个线程池;
		 */
		scheduler = Executors.newScheduledThreadPool(5);
	}
	
	/*
	 * 周期任务 或 单次任务
	 */
	public Future<?> register(Runnable job, JobContext jobContext){
		long period = jobContext.getPeriod();
		Future<?> futrue;
		if(period > 0)
		{
			futrue = scheduler.scheduleAtFixedRate(job
				, jobContext.getDelay(), jobContext.getPeriod(), jobContext.getTimeUnit());
		}else{
			futrue = scheduler.schedule(job, jobContext.getDelay(), jobContext.getTimeUnit());
		}
		return futrue;
	}
	
	public void shutdown()
	{
		if (!scheduler.isShutdown()) {
			scheduler.shutdown();
		}
	}
}
