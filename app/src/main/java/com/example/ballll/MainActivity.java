package com.example.ballll;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    TextView tv_server_addr;
    EditText et_server_addr, et_message;
    Button btn_send;
    String addr, msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_server_addr = findViewById(R.id.tv_server_addr);
        et_server_addr = findViewById(R.id.et_server_addr);
        et_message = findViewById(R.id.et_message);
        btn_send = findViewById(R.id.btn_send);
    }

    public void sart_server(View view) {
        Context context = getApplicationContext();
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        tv_server_addr.setText("Адрес сервера: " + ip);
        // Запускаем сервер
        Server server = new Server();
        Kryo kryo = server.getKryo();
        kryo.register(SomeRequest.class);
        kryo.register(SomeResponse.class);
        server.start();
        try {
            server.bind(54555, 54777);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        server.addListener(new Listener() {
            public void received(Connection connection, Object object) {
                if (object instanceof SomeRequest) {
                    SomeRequest request = (SomeRequest) object;
                    System.out.println(request.text);
                    Toast.makeText(getApplicationContext(), "" + request.text, Toast.LENGTH_SHORT).show();
                    SomeResponse response = new SomeResponse();
                    response.text = "Thanks";
                    connection.sendTCP(response);
                }
            }
        });
    }

    public void send_message(View view) {
        msg = et_message.getText().toString();
        addr = et_server_addr.getText().toString();
        class SrvThread extends Thread {
            @Override
            public void run() {
                Client client = new Client();
                Kryo kryo = client.getKryo();
                kryo.register(SomeRequest.class);
                kryo.register(SomeResponse.class);
                client.start();
                try {
                    client.connect(5000, addr, 54555, 54777);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                SomeRequest request = new SomeRequest();
                request.text = msg;
                client.sendTCP(request);
                client.addListener(new Listener() {
                    public void received(Connection connection, Object object) {
                        if (object instanceof SomeResponse) {
                            SomeResponse response = (SomeResponse) object;
                            System.out.println("!!!"+response.text);
                        }
                    }
                });
            }
        }
        SrvThread t = new SrvThread();
        t.start();
    }

    public class SomeRequest {
        public String text;
    }

    public class SomeResponse {
        public String text;
    }
}