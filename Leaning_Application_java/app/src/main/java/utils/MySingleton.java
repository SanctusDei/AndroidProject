package utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class MySingleton {
    private static MySingleton instance;
    private RequestQueue  requestQueue;
    private ImageLoader imageLoader;
    private static Context ctx;

    private  MySingleton(Context context) {
            ctx = context;
            requestQueue = getRequestQueue();

            // 图片加载器

            imageLoader = new ImageLoader(requestQueue,
                    new ImageLoader.ImageCache() {
                        private final LruCache<String, Bitmap>
                                cache = new LruCache<>(20); // 缓存20张图

                        /**
                         * 原理：它在内存中开辟了一块空间。当你加载一张图时，它先看内存里有没有：
                         *
                         * 有（Hit）：直接返回 Bitmap，秒开，不费流量。
                         *
                         * 没有（Miss）：去联网下载，下载完存进 cache。
                         *
                         * */

                        @Nullable
                        @Override
                        public Bitmap getBitmap(String url) {

                            return cache.get(url);

                        }

                        @Override
                        public void putBitmap(String url, Bitmap bitmap) {

                            cache.put(url, bitmap);

                        }
                    });

    }

    // 使用synchronized保证在多线程环境下不会同时创建两个instance
    public static synchronized MySingleton getInstance(Context context) {
        if (instance == null) {

            instance = new MySingleton(context); // 只有第一次使用时才创建实例

        }
        return  instance;
    }


    // 请求队列

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {

            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());

        }

        return requestQueue;
    }


    // 泛型方法:
    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    }


