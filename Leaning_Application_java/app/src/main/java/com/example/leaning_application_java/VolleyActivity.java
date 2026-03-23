    package com.example.leaning_application_java;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;

import utils.MySingleton;

    public class VolleyActivity extends AppCompatActivity {
        private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_volley);

        textView = findViewById(R.id.text_display);
        String url = "https://jsonplaceholder.typicode.com/todos/1";

        // 创建一个 JSON对象请求
        // 向网站发送GET请求，获取Response数据

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null, response -> {

                    try {
                        // 解析返回的标题字符串
                        String title = response.getString("title");
                        textView.setText("获取到的标题: " + title);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

        }, error -> {

                    textView.setText("获取数据失败");
        });

    // 给请求设置一个标签, 方便Activity 销毁时取消
        jsonObjectRequest.setTag("json_request");
    // 通过单例把请求发出去
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

    }

    @Override
    protected void onStop() {
        super.onStop();
        // 当Activity 不可见时, 取消所有标记的请求，防止内存显露
        MySingleton.getInstance(this).getRequestQueue().cancelAll("json_request");
    }

}