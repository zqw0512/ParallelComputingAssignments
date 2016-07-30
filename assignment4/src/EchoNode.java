import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EchoNode {
	public class ConnectionHandler implements Runnable{
		private Socket socket;
		private int id;
		public ConnectionHandler(Socket socket,int id){
			this.socket=socket;
			this.id=id;
		}
		@Override 
		public void run() {
			BufferedReader in =null;
			try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String message = null;
			while ((message = in.readLine())!=null) {
				// read the text from client
				log(" socket #"+id+" Read '" + message + "'");
				if ("ping".equals(message)) {
					//log("   it's a ping message, skip...");

				}else {
					doIt(analizMessage(message));
				}

			}
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
	}
	private EchoNode parent;
	private boolean sendIniMessFlag = false;
	private boolean iniFlag;

	public void setIniFlag(boolean iniFlag) {
		this.iniFlag = iniFlag;
	}

	public boolean isInit() {
		return this.iniFlag;
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

	private boolean receive;

	public boolean isReceived() {
		return receive;
	}

	public EchoNode(String ip, int port, boolean send, boolean receive) {
		this.ip = ip;
		this.port = port;
		this.send = send;
		this.receive = receive;
	}

	public static EchoNode createNodeFromFile(String fileName) {
		EchoNode result = null;
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(fileName));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			int i = 1;
			while (line != null) {
				if (i == 1) {
					String[] t = line.split(":");
					result = new EchoNode(t[0], Integer.parseInt(t[1]), false, false);

				} else {

					String[] k = line.split(":");
					if (k.length>2 &&  "initiator".equals(k[2])) {
						// Find if initiator
						result.setIniFlag(true);
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
			if (parent==null || parent.port != n.port) {
				if (!n.receive) {
					result = false;
				}
			}
		}
		return result;
	}

	public Map<String, String> analizMessage(String message) {
		String[] ms = message.split("&");
		String[] msIam = ms[1].split("=");
		String[] mx = msIam[1].split(":");
		Map<String, String> result = new HashMap<String, String>();
		result.put("ip", mx[0]);
		result.put("port", mx[1]);

		return result;
	}

	public void startServer() {
		log("Launching server... " + ip + ":" + port);
		ServerSocket serverSocket = null;
		BufferedReader in = null;
		try {
			serverSocket = new ServerSocket(port);
			Socket clientSocket = null;
			int i=0;
			for (;;) {
					
				clientSocket = serverSocket.accept();
				i++;
				log("Server Socket #"+i+" Connected");
				Runnable connectionHandler = new ConnectionHandler(clientSocket,i);
				new Thread(connectionHandler).start();
			}

		} catch (Exception e) {
			e.printStackTrace();
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
		System.out.println(this.port+":"+msg);
	}
	public boolean checkNeighborServer() {
		for (EchoNode n : this.neighbors) {
			for (;;) {
				try {
					sendMessage(n,"ping");
					log("My neighbor "+n.port+" is online");
					break;
				} catch (Exception ex) {
					log("error in connect " + n.ip + ":" + n.port + ", it's still offline will retry");
				}
			}
		}
		log("All my neighbors are online now!");
		return true;
	}

	public void sendMessage(EchoNode n, String msg) {
		Socket clientSocket;
		try {
			clientSocket = new Socket(n.ip, n.port);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			outToServer.writeBytes(msg + '\n');
			clientSocket.setSoTimeout(5000);
			clientSocket.close();
			outToServer.close();
			log("Message:["+msg+"] Sent to " + n.ip + ":" + n.port);

		} catch (Exception exc) {
			if (!"ping".equals(msg)) {
			exc.printStackTrace();
			log("Error in send message" + n.ip + ":" + n.port);
			}
		}
	}

	public void sendMssToAllNeighbors(String message) {

		for (EchoNode n : neighbors) {
			if (parent==null || parent.port != n.port) {
				sendMessage(n, message);
				n.send = true;
			}

		}
	}

	public void doIt(Map<String, String> ms) {
		EchoNode sender = findNodeBtwNeighbors(ms.get("ip"), Integer.parseInt(ms.get("port")));
		if (iniFlag) {
			sender.receive = true;
		} else {
			if (parent==null) {
				parent = sender;
				String sendMss = "&Iam=" + this.ip + ":" + this.port;
				sender.receive = true;
				sendMssToAllNeighbors(sendMss);
			} else {
				sender.receive = true;
			}
		}

	}

	public EchoNode findNodeBtwNeighbors(String ip, int port) {
		for (EchoNode n : neighbors) {
			if (n.ip.equals( ip) && n.port == port) {
				return n;

			}
		}
		return null;
	}

	public void start() {
		
		new Thread(new Runnable() {
			@Override public void run() {
				startServer();
			}
		}).start();
		
		
		if (checkNeighborServer()) {
			boolean done = false;
			while (true) {
				if (iniFlag && !sendIniMessFlag) {
					log("Start to send message from initiator: ");
					String initMessage = "&Iam=" + ip + ":" + port;
					sendMssToAllNeighbors(initMessage);
					sendIniMessFlag = true;
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
						if (iniFlag) {
							log("** Done **");
							done = true;
						} else {
							String message = "&Iam=" + ip + ":" + port;
							sendMessage(parent, message);
							log("** Done **");
							done = true;
						}
					}
				}
				if (done) {
					if (this.parent!=null) {
						log("My parent is"+this.parent.getPort());
					}else {
						log("I'm the init Node, I have no parent");
					}
					StringBuffer sb = new StringBuffer("Found "+this.neighbors.size()+"neighbors, they are").append("\n    ");
					for (EchoNode n:this.neighbors){
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
//		String filename = "configuration.conf";
//		log("Start...");
//		// Find Iam,Initiator,Neighbors
//		EchoNode self = createNodeFromFile(filename);
//		log("I am " + self.getIp() + ":" + self.getPort() + " \niniFlag is: " + self.isInit()
//				+ " \nAll my neighbors are:");
//		for (EchoNode n : self.neighbors) {
//			log("  " + n.ip + ":" + n.port);
//		}
//		self.start();

		EchoNode n1 =createNodeFromFile("node1.conf");
		EchoNode n2 =createNodeFromFile("node2.conf");
		EchoNode n3 =createNodeFromFile("node3.conf");
		
		new Thread(new Runnable() {
			@Override public void run() {
				n1.start();
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override public void run() {
				n2.start();
			}
		}).start();
		
		new Thread(new Runnable() {
			@Override public void run() {
				n3.start();
			}
		}).start();
		
		while(true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		//n1.sendMessage(n2, "&Iam=" + n1.ip + ":" + n1.port);
				
	}

}
