package com.ongroa.fzchat2.server;

import java.net.Socket;

public class Client {

	Socket socket;
	String nick;
	
	@Override
	public String toString() {
		return String.format("\t%s %d", nick, socket.getPort());
	}
}
