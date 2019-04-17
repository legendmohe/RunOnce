package com.legendmohe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现“只运行一次”逻辑的工具类
 * <p>
 * Created by hexinyu on 2019/4/17.
 */
public class RunOnce {

    private static Map<Context, RunOnce> gContextMap = new ConcurrentHashMap<>();

    /**
     * 绑定当前context
     *
     * @param context
     * @return
     */
    public synchronized static RunOnce from(Context context) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        // 如果已经Destroy了，就不绑定，直接返回空实现
        if (context.isDestroy()) {
            return EMPTY_RUN_ONCE;
        }
        RunOnce runOnce = gContextMap.get(context);
        if (runOnce == null) {
            // 注册destroy监听
            final Binder runOnceBinder = context.getRunOnceBinder();
            runOnceBinder.setListener(new Binder.Listener() {
                @Override
                public void onDestroy() {
                    //destroy时清理资源，避免泄漏
                    gContextMap.remove(context);
                    runOnceBinder.mListener = null;
                    runOnceBinder.onDestroy();
                }
            });
            runOnce = new RunOnce(runOnceBinder);
            gContextMap.put(context, runOnce);
        }
        return runOnce;
    }

    /*
    空实现
     */
    private static RunOnce EMPTY_RUN_ONCE = new RunOnce(null) {
        @Override
        void run(String tag, Runnable runOnce) {
            //忽略传进来的runnable，不执行任何代码
        }
    };

    //////////////////////////////////////////////////////////////////////

    // Keep reference
    private Binder mRunOnceBinder;

    public RunOnce(Binder runOnceBinder) {
        mRunOnceBinder = runOnceBinder;
    }

    /**
     * 在Context destroy之前，相同tag的runnable只会执行一次
     *
     * @param tag
     * @param runOnce
     */
    void run(String tag, Runnable runOnce) {
        if (!mRunOnceBinder.checkHasRun(tag)) {
            if (runOnce != null) {
                runOnce.run();
            }
        }
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * 实现生命周期绑定和判定是否执行过的逻辑
     * 外部要调用notifyDestroy来触发清理逻辑
     */
    public static abstract class Binder {

        private Binder.Listener mListener;

        private void setListener(Binder.Listener listener) {
            mListener = listener;
        }

        public void notifyDestroy() {
            if (mListener != null) {
                mListener.onDestroy();
            }
        }

        /**
         * 清理自身资源
         */
        protected abstract void onDestroy();

        /**
         * 判断一个tag是否执行过
         *
         * @param tag
         * @return
         */
        protected abstract boolean checkHasRun(String tag);

        private interface Listener {
            void onDestroy();
        }
    }

    /**
     * 实现destroy前只执行一次逻辑
     */
    public static class DefaultBinder extends Binder {
        private Map<String, Boolean> mHasRunMap = new ConcurrentHashMap<>();

        @Override
        protected boolean checkHasRun(String tag) {
            if (mHasRunMap.containsKey(tag)) {
                return true;
            }
            mHasRunMap.put(tag, true);
            return false;
        }

        @Override
        public void onDestroy() {
            mHasRunMap.clear();
        }
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * 运行次数计算的上下文。内部要维护一个Binder用于发送destroy通知
     */
    public interface Context {
        /**
         * 当调用Binder.notifyDestroy后，内部runnable计数会被清空，RunOnce会删除对这个context
         * 的引用。
         *
         * @return
         */
        Binder getRunOnceBinder();

        /**
         * 如果返回true，则代表当前Context已经销毁，不能再发送Runnable，也不会绑定到Runnable（放在map里）
         *
         * @return
         */
        boolean isDestroy();
    }
}
