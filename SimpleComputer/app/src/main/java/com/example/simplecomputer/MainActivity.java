package com.example.simplecomputer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.jar.JarException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LifeCycleTest";
    private EditText input1, input2;
    private Button btnAdd, btnUpload;
    private TextView txtResult, txtUserUserName;

    private boolean isAdded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//      绑定布局文件
        setContentView(R.layout.activity_main);
//        初始化控件

        input1 = findViewById(R.id.inputNum1);
        input2 = findViewById(R.id.inputNum2);
        btnAdd = findViewById(R.id.buttonAdd);
        btnUpload = findViewById(R.id.btnUpLoad);
        txtResult = findViewById(R.id.txtResult);
        txtUserUserName = findViewById(R.id.txtUserName);

//        1.获取相同的SharedPreferences对象
        SharedPreferences sp = getSharedPreferences("MyData", MODE_PRIVATE);

//        2.读取数据
        float lastResult = sp.getFloat("last_result", 0.0f);
        String lastUserName = sp.getString("last_user_name", "");
//        3.显示到界面
        if (lastResult != 0.0f) {
            txtResult.setText("欢迎回来!" + lastUserName + "上次的计算结果是" + lastResult);
        }

//        设置点击事件
        btnAdd.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {
                calculateSum();

            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {




            @Override
            public void onClick(View view) {

                testPublicPost();
            }
        });

    }

    private void testPublicPost(){
//        1.创建客户端
        OkHttpClient client = new OkHttpClient();

//        2. 构建要发送的数据
        String myName = txtUserUserName.getText().toString();
        String myResult = txtResult.getText().toString();


        RequestBody formBody = new FormBody.Builder()
                .add("user", myName)
                .add("Result", myResult)
                .build();

//        3. 创建请求(指向公开测试接口)
        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(formBody)
                .build();
//        4. 发起异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                如果断网了,会执行这里的代码
                runOnUiThread(()->
                        Toast.makeText(MainActivity.this, "网络连接失败", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && isAdded) {
                    isAdded = false;
//                    服务器返回的JSON字符串
                    final  String responseData = response.body().string();

                    try {
//                        1. 将整串字符串转为 JSON 对象
                        JSONObject jsonRoot = new JSONObject(responseData);

//                        2. 提取 'origin'(提取ip地址)
                        String myIp = jsonRoot.getString("origin");

//                        3. 提取嵌套在 "form" 里的 "user”
                        JSONObject formObject = jsonRoot.getJSONObject("form");
                        String userName = formObject.getString("user");

                        // 回到主线程更新 UI
                        runOnUiThread(() -> {
                            txtResult.setText("解析成功！用户：" + userName + "\n来源IP：" + myIp);
                            Toast.makeText(MainActivity.this, "数据已同步至云端", Toast.LENGTH_SHORT).show();
                        });
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "解析失败: " + e.getMessage());
                    }


                    // 回到主线程更新 UI

                } else {

                    Log.d("FuckYou","测试");
                    runOnUiThread(()-> {
                        Toast.makeText(MainActivity.this, "此次结果已经向服务器传输过了，请勿重复传输",Toast.LENGTH_SHORT).show();
                    });

                }
            }
        });




    }


    // 计算两数相加

    private void calculateSum() {
        String s1 = input1.getText().toString().trim();
        String s2 = input2.getText().toString().trim();


        if (s1.isEmpty() || s2.isEmpty() ) {
            Toast.makeText(this, "请输入完整的数字",Toast.LENGTH_SHORT).show();
            return;
        }

    try {

        double sum = getAdd(s1, s2);
        isAdded = true;

//        1. 获取SharePreferences 对象(文件名叫 "MYData", 模式为私有)
        SharedPreferences sp = getSharedPreferences("MyData", MODE_PRIVATE);
//        2. 开启编译器
        SharedPreferences.Editor editor = sp.edit();
//        3. 存入数据 (类似 Intent , 也是键值对)
        editor.putFloat("last_result", (float) sum);
        editor.putString("last_user_name", txtUserUserName.getText().toString());
//        4. 提交
        editor.apply();


        txtResult.setText("结果:"+ sum);
        String userName = txtUserUserName.getText().toString();
//        核心跳转代码
//        1.创建意图:从当前界面(this)跳转到SecondActivity.Class
        Intent intent = new Intent(this, SecondActivity.class);
//        2.存储数据
        intent.putExtra("AddResult", sum);

        intent.putExtra("UserName", userName);
//        3.跳转
        startActivity(intent);

    } catch (NumberFormatException e) {
//        处理非法输入
        txtResult.setText("格式输入错误");
    }
    }

    private double getAdd(String s1, String s2)
    {
        double num1 = Double.parseDouble(s1);
        double num2 = Double.parseDouble(s2);
        double sum = num1 + num2;
        return sum;

    }
}