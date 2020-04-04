package com.example.httpurlconnectiontest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private SendUrlTask sendUrlTask;
    private Button button;
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUrlTask= new SendUrlTask("https://www.baidu.com");
                sendUrlTask.execute();
            }
        });

    }


    private class SendUrlTask extends AsyncTask<Void,Void,String> {
        //网络请求多为耗时操作,在这用异步类进行操作,完成后自动销毁防止内存溢出
        String url;
        public SendUrlTask(String url) {
            super();
            this.url = url;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return NetUrlis.SendUrlRequest(url);
            //调用工具类发起网络请求
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            sendUrlTask.cancel(true);
            //执行完成后toast结果,并销毁
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sendUrlTask != null) {
            sendUrlTask.cancel(true);
        }
    }
}

