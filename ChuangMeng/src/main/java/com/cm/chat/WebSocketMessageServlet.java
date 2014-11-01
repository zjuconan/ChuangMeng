package com.cm.chat;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.apache.logging.log4j.Logger;

import com.cm.logging.CmLogger;

@ServerEndpoint(value="/chat")
public class WebSocketMessageServlet {
	public Logger logger = CmLogger.getLogger();
	public static int ONLINE_USER_COUNT=1;
	public static Map<String, Session> SESSION_MAP= new Hashtable<String, Session>();
	public String getUser(HttpServletRequest request){
		return (String) request.getSession().getAttribute("user");	
	}
	
	@OnOpen
	public void start(Session session){
		SESSION_MAP.put(session.getId(), session);
		logger.info(session.getId()+" join");
	}
	@OnMessage
	public void process(Session session, String message){
		logger.info(session.getId()+" say:"+message);
		broadcast(session.getId()+" say:"+message);
	}
	@OnClose
	public void end(Session session){
		logger.info(session.getId()+" leave");
		SESSION_MAP.remove(session.getId());
	}
	@OnError
	public void error(Session session, Throwable throwable){
		logger.error(session.getId()+" throw "+throwable);
		end(session);
	}
	void broadcast(String message){
        RemoteEndpoint.Basic remote = null;
        for(String key: SESSION_MAP.keySet()){
            remote = SESSION_MAP.get(key).getBasicRemote();
            try {
                remote.sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
