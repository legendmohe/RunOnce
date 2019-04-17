package com.legendmohe;

/**
 * Created by hexinyu on 2019/4/17.
 */
public class LifeCycle {

    public void addObserver(LifeCycleObserver observer) {

    }

    public interface LifeCycleObserver {
        void onDestroy(LifeCycle cycle);
    }
}
