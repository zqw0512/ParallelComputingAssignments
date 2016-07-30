package com.qzhu;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class EchoNode {
	public class ConnectionHandler implements Runnable {
		private Socket socket;
		private int id;

		public ConnectionHandler(Socket socket, int id) {
			this.socket = socket;
			this.id = id;
		}

		@Override
		public void run() {
			InputStream is = null;
			try {
				is = socket.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				Object object;
				while ((object = (Message) ois.readObject()) != null) {
					Message message = (Message) object;
					log("/connection #"+id+", processing message"+message);
					processMessage(message);
				}

			} catch (EOFException e1) {
				//safely exit the thread when EOFException Occours
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}
	}

	private EchoNode parent;
	private boolean initMessageSent = false;
	private boolean inited;

	public void setInited(boolean inited) {
		this.inited = inited;
	}

	public boolean isInited() {
		return this.inited;
	}

	private List<EchoNode> neighbors = new ArrayList<EchoNode>();

	private String ip;

	public String getIp() {
		return this.ip;
	}

	private int port;

	public int getPort() {
		return this.port;
	}

	private boolean send;

	public boolean isSent() {
		return send;
	}

	private boolean received;

	public boolean isReceived() {
		return received;
	}

	public EchoNode(String ip, int port, boolean send, boolean received) {
		this.ip = ip;
		this.port = port;
		this.send = send;
		this.received = received;
	}

	public static EchoNode createNodeFromFile(String fileName) {
		EchoNode result = null;
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(fileName));
			String line = br.readLine();
			int i = 1;
			while (line != null) {
				if (i == 1) {
					String[] t = line.split(":");
					result = new EchoNode(t[0], Integer.parseInt(t[1]), false, false);

				} else {

					String[] k = line.split(":");
					if (k.length > 2 && "initiator".equals(k[2])) {
						// Find if initiator
						result.setInited(true);
					} else {
						// Find neighbors
						result.neighbors.add(new EchoNode(k[0], Integer.parseInt(k[1]), false, false));
					}
				}

				line = br.readLine();
				i++;

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public boolean checkReceiveFromAll() {
		boolean result = true;
		for (EchoNode n : neighbors) {
			if (parent == null || parent.port != n.port) {
				if (!n.received) {
					result = false;
				}
			}
		}
		return result;
	}

	public void startServer() {
		log("Launching server... " + ip + ":" + port);
		ServerSocket serverSocket = null;
		BufferedReader in = null;
		try {
			serverSocket = new ServerSocket(port);
			Socket clientSocket = null;
			int i = 0;
			for (;;) {

				clientSocket = serverSocket.accept();
				i++;
				log("Server Socket #" + i + " Connected");
				Runnable connectionHandler = new ConnectionHandler(clientSocket, i);
				new Thread(connectionHandler).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		} finally {
			try {
				if (in != null)
					in.close();
				if (serverSocket != null)
					serverSocket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public void log(String msg) {
		System.out.println(this.port + ":" + msg);
	}

	public boolean checkNeighborServer() {
		Message message = new Message(this.ip, this.port, "ping");
		for (EchoNode n : this.neighbors) {
			for (;;) {
				try {
					sendMessage(n, message);
					log("My neighbor " + n.port + " is online");
					break;
				} catch (Exception ex) {
					log("error in connect my neighbor" + n.ip + ":" + n.port + ", it's still offline will retry");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		log("All my neighbors are online now!");
		return true;
	}

	public void sendMessage(EchoNode n, Message message)  {
		Socket clientSocket;
		try {
			clientSocket = new Socket(n.ip, n.port);
			try {

				ObjectOutputStream objectOutput = new ObjectOutputStream(clientSocket.getOutputStream());
				objectOutput.writeObject(message);

			} catch (Exception e) {
				e.printStackTrace();
			}
			log("Message:[" + message + "] Sent to " + n.ip + ":" + n.port);

		
		}catch(ConnectException e1) {
			log("Error in connect " + n.ip + ":" + n.port);
			throw new RuntimeException(e1);

		} catch (Exception e) {
			e.printStackTrace();
			log("Error in send message" + n.ip + ":" + n.port);
;				throw new RuntimeException(e);
		}
	}

	public void sendMessageToAllNeighbors(Message message) {

		for (EchoNode n : neighbors) {
			if (parent == null || parent.port != n.port) {
				sendMessage(n, message);
				n.send = true;
			}

		}
	}

	public void processMessage(Message message) {
		EchoNode sender = findNodeBtwNeighbors(message.getFromIp(), message.getFromPort());
		if (inited) {
			sender.received = true;
		} else {
			if (parent == null) {
				parent = sender;
				Message sendMessage = new Message(this.ip, this.port, "I got my parent, check all other neighbors");
				sender.received = true;
				sendMessageToAllNeighbors(sendMessage);
			} else {
				sender.received = true;
			}
		}

	}

	public EchoNode findNodeBtwNeighbors(String ip, int port) {
		for (EchoNode n : neighbors) {
			if (n.ip.equals(ip) && n.port == port) {
				return n;
			}
		}
		log("!!!!!!Sender "+ip+":"+port+" not found!! this problem usually caused by wrong configuration, please fix this issue");
		return null;
	}

	public void start() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				startServer();
			}
		}).start();

		if (checkNeighborServer()) {
			boolean done = false;
			while (true) {
				if (inited && !initMessageSent) {
					log("Start to send message from initiator: ");
					Message initMessage = new Message(this.ip, this.port, "I'm initiator!");

					sendMessageToAllNeighbors(initMessage);
					initMessageSent = true;
					log("I'm initiator, All my neighbors are:" + neighbors);
				} else {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					log(".");
					if (checkReceiveFromAll()) {
						if (inited) {
							log("** Done **");
							done = true;
						} else {
							Message message = new Message(this.ip, this.port, "Message to my parent");
							sendMessage(parent, message);
							log("** Done **");
							done = true;
						}
					}
				}
				if (done) {
					if (this.parent != null) {
						log("My parent is" + this.parent.getPort());
					} else {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						log("** I'm the init Node, I have no parent **");
						log("**** ALL DONE! ****");
					}
					StringBuffer sb = new StringBuffer("   I have " + this.neighbors.size() + " neighbors, they are")
							.append("\n    ");
					for (EchoNode n : this.neighbors) {
						sb.append(n.getPort()).append(",");
					}
					sb.append("\n");
					log(sb.toString());
					break;
				}
			}
		}
	}

	public static void main(String[] args) {
		if (args==null || args.length<1) {
			System.out.println("please run with at least one configuration file name!");
			System.exit(0);
		}
		for (String s:args) {
			EchoNode n = createNodeFromFile(s);
			new Thread(new Runnable() {
				@Override
				public void run() {
					n.start();
				}
			}).start();
		}

		while (true) {
			//keep the server running
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
