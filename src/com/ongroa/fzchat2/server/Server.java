package com.ongroa.fzchat2.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

public class Server {

	private static final int SERVER_PORT = 2000;

	public static final String CMD_MSG = "/MSG";
	public static final String CMD_TEXT = "/TEXT";
	public static final String CMD_NICK = "/NICK";
	public static final String CMD_NICKS = "/NICKS";
	public static final String CMD_BYE = "/BYE";
	public static final String INFO = "/INFO";
	public static final String INFO_USED_NICK = "USED_NICK";
	public static final String INFO_NICK_NOT_SET = "NICK_NOT_SET";
	public static final String INFO_INVALID_NICK = "INVALID_NICK";

	ServerSocket serverSocket;
	Vector<Client> clients;
	boolean listening;

	Server() throws IOException {
		listening = true;
		clients = new Vector<Client>();
	}

	public ArrayList<String> getNicks() {
		ArrayList<String> ret = new ArrayList<String>();
		for (Client client : clients) {
			if (client.nick != null) {
				ret.add(client.nick);
			}
		}
		return ret;
	}

	public static void main(String[] args) throws IOException {
		Server main = new Server();
		main.start();
	}

	private void start() throws IOException {
		Thread listening = new Listening();
		listening.start();
	}

	class Listening extends Thread {

		public Listening() {
			super();
		}

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(SERVER_PORT);
				while (listening) {
					Socket clientSocket = serverSocket.accept();
					new ClientThread(clientSocket).start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class ClientThread extends Thread {

		Client client;

		public ClientThread(Socket socket) {
			super();
			client = new Client();
			client.socket = socket;
			clients.add(client);
			System.out.format("Client %s %s %d is connected.\n", 
					socket.getInetAddress().getHostName(),
					socket.getInetAddress().getHostAddress(),
					socket.getPort());
			printClients();
		}

		@Override
		public void run() {
			String inputLine;
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						client.socket.getInputStream()));
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.toUpperCase().startsWith(CMD_BYE)) {
						break;
					} else if (inputLine.toUpperCase().startsWith(CMD_NICKS)) {
						handleNicks();
					} else if (inputLine.toUpperCase().startsWith(CMD_NICK)) {
						handleNick(inputLine);
					} else if (! inputLine.startsWith("/")) {
						if (client.nick != null) {
							broadcastMessage(CMD_TEXT, client, inputLine);
						} else {
							sendMessage(client, INFO, INFO_NICK_NOT_SET);

						}
					}
				}
				in.close();
				client.socket.close();
				clients.remove(client);
				System.out.format("Socket %d closed\n",client.socket.getPort());
				printClients();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void handleNicks() {
			String nicks = "";
			ArrayList<String> nickList = getNicks();
			for (String nick : nickList) {
				nicks += nick + " ";
			}
			sendMessage(client, CMD_NICKS, nicks);

		}

		private void handleNick(String inputLine) {
			String nick = getNick(inputLine);
			if (nick.equals("")) {
				sendMessage(client, INFO, INFO_INVALID_NICK);
			} else {
				if (! getNicks().contains(nick)) {
					broadcastMessage(CMD_NICK, client, nick);
					client.nick = nick;
					printClients();
				} else {
					sendMessage(client, INFO, 
							String.format("%s %s", INFO_USED_NICK, nick));
				}
			}
		}

		private String getNick(String inputLine) {
			String[] line = inputLine.split(" ");
			if (line.length > 1) {
				return line[1];
			}
			return "";
		}

		private void sendMessage(Client client, String cmd, String line) {
			try {
				PrintWriter out = new PrintWriter(
						client.socket.getOutputStream(), true);
				out.format("%s %s\n", cmd, line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void printClients() {
		System.out.println("Clients:");;
		for (Client client : clients) {
			System.out.println(client);
		}
		System.out.println();
	}

	private void broadcastMessage(String cmd, Client c, String inputLine) {
		for (Client client : clients) {
			try {
				PrintWriter out = new PrintWriter(
						client.socket.getOutputStream(), true);
				out.format("%s %s %s\n", cmd, c.nick, inputLine);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


}
