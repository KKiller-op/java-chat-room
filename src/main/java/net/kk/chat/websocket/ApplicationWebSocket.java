package net.kk.chat.websocket;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kk.chat.entity.Message;
import net.kk.chat.utils.MessageUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author KK
 * @create 2020-08-16
 */
@ServerEndpoint(value = "/websocket")
@Component
public class ApplicationWebSocket {
    private static AtomicInteger onlineCount = new AtomicInteger(0);
    private static Map<String, ApplicationWebSocket> webSocket = new ConcurrentHashMap<>(); //存储在线人数
    private Session session;
    private String sendName;

    @OnOpen
    public void onOpen(Session session) {
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        this.session = session;
        String json = MessageUtil.getMessNames(getNames());
        //有人上线 重新把聊天列表推送给客户端
        sendMessNames();
    }

    @OnMessage
    public void onMessage(String text) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Message message = mapper.readValue(text, Message.class);
            String type = message.getType();
            String sendName = message.getSendName();
            if (Objects.equals("setting", type) && !Objects.equals(null, sendName)) {
                setting(sendName);
            } else if (Objects.equals("在线群聊", message.getReceiveName())) {
                sendMessageAll(sendName, message);
            } else {
                String toName = message.getReceiveName();
                sendMessage(toName, sendName, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose() {
        System.out.println(sendName + "退出了聊天室!");
        ApplicationWebSocket.webSocket.remove(sendName);
        subOnlineCount();
        //重新把聊天列表推送给客户端
        sendMessNames();
    }

    public Set<String> getNames() {
        return webSocket.keySet();
    }

    public void sendMessageAll(String jsonMessage)  {
        try {
            Set<String> names = getNames();
            for (String name : names) {
                ApplicationWebSocket.webSocket.get(name).session.getBasicRemote().sendText(jsonMessage);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void sendMessageAll(String sendName, Message message) {
        Set<String> names = getNames();
        message.setText(message.getText().replaceAll("\n", "<br>"));
        String jsonMessage = JSON.toJSONString(message);
        sendMessageAll(jsonMessage);
    }

    public void sendMessage(String toName, String sendName, Message message) throws IOException {
        message.setText(message.getText().replaceAll("\n", "<br>"));
        String jsonMessage = JSON.toJSONString(message);
        ApplicationWebSocket.webSocket.get(toName).session.getBasicRemote().sendText(jsonMessage);
    }

    public void setting(String sendName) {
        this.sendName = sendName;
        ApplicationWebSocket.webSocket.put(sendName, this);
        sendMessNames();
    }
    public void sendMessNames(){
        String json = MessageUtil.getMessNames(getNames());
        sendMessageAll(json);
    }
    public static synchronized int getOnlineCount() {
        return onlineCount.get();
    }

    public static synchronized void addOnlineCount() {
        ApplicationWebSocket.onlineCount.getAndIncrement();
    }

    public static synchronized void subOnlineCount() {
        ApplicationWebSocket.onlineCount.getAndDecrement();
    }
}
