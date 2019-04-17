# RunOnce

实现“只运行一次”逻辑的工具类

# 使用例子

``` java
public class Main {

    public static void main(String[] args) {
        // 下面演示的是利用RunOnce工具，实现在TestContext指定的生命周期内，只运行一些逻辑一次

        TestContext context = new TestContext();
        System.out.println("====new context====");

        // 下面只会打印一次"abc running"
        runOnce("abc", context);
        runOnce("abc", context);

        // 回收RunOnce对context的引用
        context.triggerDestroy();

        // 已经destroy，不会打印
        runOnce("abc", context);
        runOnce("bcd", context);

        // 重新开始
        context.reset();
        System.out.println("====reset context====");

        // 下面只会各打印一次"abc running"、"bcd running"
        runOnce("abc", context);
        runOnce("bcd", context);
        runOnce("abc", context);
        runOnce("bcd", context);
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
        private RunOnce.Binder mBinder = new RunOnce.Binder();

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
        public boolean isDestroy() {
            return mIsDestroy;
        }

        public void reset() {
            mIsDestroy = false;
        }
    }

}
```
