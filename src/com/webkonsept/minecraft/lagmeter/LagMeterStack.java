package com.webkonsept.minecraft.lagmeter;

import java.util.LinkedList;

public class LagMeterStack {
	private int maxSize = 0;
	private LinkedList<Float> stack = new LinkedList<Float>(); 
	
	LagMeterStack(int maxSize){
		this.maxSize = maxSize;
	}
	LagMeterStack(){
		this.maxSize = 10;
	}
	public void setMaxSize(int maxSize){
		this.maxSize = maxSize;
	}
	public int getMaxSize(){
		return this.maxSize;
	}
	public int size(){
		return stack.size();
	}
	public void add(Float item){
		if (item != null && item <= 20){
			stack.add(item);
			if (stack.size() > maxSize){
				stack.poll();
			}
		}
	}
	public float getAverage(){
		float total = 0f;
		for (Float f : stack){
			if (f != null){
				total += f;
			}
		}
		return total / stack.size();
	}
}
