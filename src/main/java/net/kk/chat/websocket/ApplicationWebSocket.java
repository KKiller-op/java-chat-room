package net.kk.chat.websocket;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kk.chat.entity.Message;
import net.kk.chat.utils.MessageUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author jackcooper
 * @create 2017-12-28 13:04
 */
@ServerEndpoint(value = "/websocket")
@Component
public class ApplicationWebSocket{
    private static AtomicInteger onlineCount = new AtomicInteger(0);
    private static Map<String,ApplicationWebSocket> webSocket = new ConcurrentHashMap<>(); //存储在线人数
    private Session session;
    private String sendName;
    @OnOpen
    public void onOpen(Session session){
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        this.session = session;
    }
    @OnMessage
    public void onMessage(String text, Session session){
        try {
            ObjectMapper mapper = new ObjectMapper();
            Message message = mapper.readValue(text, Message.class);
            String type = message.getType();
            String sendName = message.getSendName();
            if ("setting".equals(type)){
                setting(sendName);
            }else if("在线群聊".equals(message.getReceiveName())){
                sendMessageAll(sendName, message);
            } else{
                String toName = message.getReceiveName();
                sendMessage(toName,sendName,message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @OnClose
    public void onClose(){
        System.out.println(sendName + "退出了聊天室!");
        ApplicationWebSocket.webSocket.remove(sendName);
        subOnlineCount();
    }
    public Set<String> getNames(){
        return webSocket.keySet();
    }
    public void sendMessageAll(String jsonMessage) throws IOException {
        Set<String> names = getNames();
        for(String name : names){
            ApplicationWebSocket.webSocket.get(name).session.getBasicRemote().sendText(jsonMessage);
        }
    }
    public void sendMessageAll(String sendName, Message message) throws IOException {
        Set<String> names = getNames();
        message.setText(message.getText().replaceAll("\n","<br>"));
        message.setCreateDate(DateUtil.format(new Date(), "MM-dd hh:mm"));
        message.setUrl("/img/" + sendName + ".jpg");
        String jsonMessage = JSON.toJSONString(message);
        sendMessageAll(jsonMessage);
    }
    public void sendMessage(String toName, String sendName, Message message) throws IOException {
        message.setText(message.getText().replaceAll("\n","<br>"));
        message.setCreateDate(DateUtil.format(new Date(), "MM-dd hh:mm"));
        message.setUrl("/img/" + sendName + ".jpg");
        String jsonMessage = JSON.toJSONString(message);
        if(!toName.equals(sendName)){
            ApplicationWebSocket.webSocket.get(sendName).session.getBasicRemote().sendText(jsonMessage);
        }
        ApplicationWebSocket.webSocket.get(toName).session.getBasicRemote().sendText(jsonMessage);
    }
    public void setting(String sendName) throws IOException {
        this.sendName = sendName;
        ApplicationWebSocket.webSocket.put(sendName, this);
        String json = MessageUtil.getMessageJSON(getNames());
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
