package com.netty.game.server.util;

import java.util.Random;

public class RandomUtil {
	private static Random r = new Random();
	public static boolean randomPersent(int persent){
		return r.nextInt(100) < persent;
	}
	
	public static boolean random(double rate){
		return Math.random() < rate;
	}
	
	/**
	 * Select an <code>int</code> value between <code>min</code> and <code>max</code> by random. 
	 * Both <code>min</code> and <code>max</code> might be selected.
	 * @param min
	 * @param max
	 * @return
	 */
	public static int avgRandom(int min, int max){
		if(min > max){
			int temp = max;
			max = min;
			min = temp;
		}
		int rNum = r.nextInt(max - min + 1);
		return rNum + min;
	}
	
	/**
	 * Select an <code>double</code> value between <code>min</code> and <code>max</code> by random. 
	 * Both <code>min</code> and <code>max</code> might be selected.
	 * @param min
	 * @param max
	 * @return
	 */
	public static double avgRandom(double min, double max){
		if(min > max){
			double temp = max;
			max = min;
			min = temp;
		}
		double rNum = r.nextDouble() * (max - min);
		return rNum + min;
	}

}
