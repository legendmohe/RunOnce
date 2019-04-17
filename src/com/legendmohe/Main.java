package com.legendmohe;

public class Main {

    public static void main(String[] args) {
        // 下面演示的是利用RunOnce工具，实现在TestContext指定的生命周期内，只运行一些逻辑一次

        TestContext context = new TestContext();
        System.out.println("====new context====");

        // 下面只会打印一次"abc running"
        runOnce("abc", context);
        runOnce("abc", context);

        // 重新开始
        context.reset();
        System.out.println("====reset context====");

        runOnce("abc", context);

        // 回收RunOnce对context的引用
        context.triggerDestroy();

        // 已经destroy，不会打印
        runOnce("abc", context);
        runOnce("bcd", context);

        // 如果不重新创建context，那么要reset一下才能重新开始
        context.reset();
        System.out.println("====reset context====");

        // 下面只会各打印一次"abc running"、"bcd running"
        runOnce("abc", context);
        runOnce("bcd", context);
        runOnce("abc", context);
        runOnce("bcd", context);

        // LifeCycle演示
        testLifeCycle();
    }

    private static void testLifeCycle() {
        // 演示如何结合LifeCycle使用RunOnce
        LifeCycle lifeCycle = new LifeCycle();
        RunOnce.from(LifeCycleWrapper.wrap(lifeCycle)).run("lifecycle", new Runnable() {
            @Override
            public void run() {
                System.out.println("lifecycle running");
            }
        });
    }

    /*
    执行体
     */
    private static void runOnce(String tag, TestContext context) {
        RunOnce.from(context).run(tag, new Runnable() {
            @Override
            public void run() {
                System.out.println(tag + " running");
            }
        });
    }

    /**
     * RunOnce的Context实现
     */
    private static class TestContext implements RunOnce.Context {

        // 用于通知上下文的结束事件，即onDestroy发生
        private RunOnce.Binder mBinder = new RunOnce.DefaultBinder();

        private boolean mIsDestroy;

        public void triggerDestroy() {
            mIsDestroy = true;
            mBinder.notifyDestroy();
        }

        @Override
        public RunOnce.Binder getRunOnceBinder() {
            return mBinder;
        }

        @Override
        public boolean isRunOnceContextDestroy() {
            return mIsDestroy;
        }

        public void reset() {
            mBinder.notifyDestroy();
            mIsDestroy = false;
        }
    }

    private static class LifeCycleWrapper implements RunOnce.ContextProvider {

        private LifeCycle mLifeCycle;

        private RunOnce.Context mRunOnceContext;

        private RunOnce.Binder mBinder;

        private boolean mIsDestroy;

        public static LifeCycleWrapper wrap(LifeCycle cycle) {
            return new LifeCycleWrapper(cycle);
        }

        private LifeCycleWrapper(LifeCycle cycle) {
            mLifeCycle = cycle;

            mBinder = new RunOnce.DefaultBinder();

            mLifeCycle.addObserver(new LifeCycle.LifeCycleObserver() {
                @Override
                public void onDestroy(LifeCycle cycle) {
                    mBinder.notifyDestroy();
                    mIsDestroy = true;
                }
            });
            mRunOnceContext = new RunOnce.Context() {
                @Override
                public RunOnce.Binder getRunOnceBinder() {
                    return mBinder;
                }

                @Override
                public boolean isRunOnceContextDestroy() {
                    return mIsDestroy;
                }
            };
        }

        /**
         * 返回同一个lifecycle对象，使RunOnce返回同一个RunOnce对象。
         *
         * @return
         */
        @Override
        public Object provideKey() {
            return mLifeCycle;
        }

        @Override
        public RunOnce.Context provideContext() {
            return mRunOnceContext;
        }
    }
}
