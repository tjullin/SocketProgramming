import java.net.*;
import java.util.*;

/***********tjullin的udp-ping客户端*****/
/***********tjullin的udp-ping客户端*****/
public class TjullinUDPClient extends TjullinPing implements Runnable
{
	/*******tjullin的udp-ping客户端的参数**/
	String remoteHost;//目的主机的ip地址
	int remotePort;//目的主机的端口
	int numReplies = 0;//收到的回复数量 
	static public int NUM_PINGS = 10;//设置发出ping信息的数量
	static public int WAIT_REPLY = 1000;//发送的最大等待时间（ms）
	static public int WAIT_SEND = 5000;//接收回复的最大等待时间（ms）
	static boolean[] replies = new boolean[NUM_PINGS];
	//标记发送的ping-信息是否收到回复
	static long[] rtt = new long[NUM_PINGS];
	//记录每一条ping-信息的往返时延
	/*******tjullin的udp-ping客户端的参数**/
	
	//初始化客户端的必要参数
	public TjullinUDPClient ( String host , int port )
	{
		remoteHost = host;
		remotePort = port;
	}
	
	/******tjullin的udp-ping客户端的回复信息分析*/
	private void handleReply ( String reply )
	{
		System.out.println("开始分析回复信息！");
		String[] tmp = reply.split(" ");
		//以空格为间隔把回复划分为字符串数组
		int pingNumber = Integer.parseInt(tmp[1]);//获取收到的ping信息的号
		long then = Long.parseLong(tmp[2]);//获取收到的ping信息的发送时间
		replies[pingNumber] =true;//设置收到的回复的状态为已收到
		Date now = new Date();
		//计算往返时延
		rtt[pingNumber] = now.getTime() - then;
		numReplies++;
	}

	/******tjullin的udp-ping客户端的回复信息分析*/

	public void run ( )
	{
		//创建发送UDP-ping信息的socket
		createSocket();
		try
		{
			socket.setSoTimeout( WAIT_REPLY );
			//设置socket发送UDP-信息的最大时间
		}
		catch (SocketException e )
		{
			System.out.println("设置最大发送时间失败!!");
		}

		for ( int i = 0 ; i < NUM_PINGS ; i++ )
		{
			/*********tjullin的udp-ping客户端发送信息的构造*****/
			Date now = new Date();//获取当前的系统时间
			//填写要发送的信息
			String message = "PING " + i + " " + now.getTime() + " ";
			//标记当前发送信息的接收状态为未接收
			replies[i] = false;
			rtt[i] = 1000000;
			PingMessage ping = null;
			//构造ping信息
			try
			{
				ping = new PingMessage
					(InetAddress.getByName(remoteHost),remotePort,message);
			}
			catch ( UnknownHostException e )
			{
				System.out.println("找不到要发送的目的主机");
			}
			/*********tjullin的udp-ping客户端发送信息的构造*****/
		    System.out.println("正在发送");
			/*********tjullin的udp-ping客户端信息发送到服务器*****/
			sendPing(ping);
			/*********tjullin的udp-ping客户端信息发送到服务器*****/
			System.out.println("成功发送信息！");
			/*********tjullin的udp-ping客户端分析回复信息*********/
			try
			{
					System.out.println("开始接收...");
					PingMessage reply = receivePing();//接收服务器的回复信息
					System.out.println("接收完毕...");
					handleReply(reply.getContent());
			}
			//如果超过了最大时限依旧没有回复，那么等待处理...
			catch ( SocketTimeoutException e )
			{
				System.out.println("dsadasdas");
			}
		}
		/*********tjullin的udp-ping客户端分析回复信息*********/
			
		try
		{
			socket.setSoTimeout(WAIT_SEND);
		}
		catch ( SocketException e )
		{
			System.out.println("设置等待回复的最大时限失败！");
		}
		//如果发出的请求没有全部收到的话
		while ( numReplies < NUM_PINGS )
		{
			try
			{
				//System.out.println("开始接收！");
				PingMessage reply = receivePing();
				handleReply(reply.getContent());
			}
			catch ( SocketTimeoutException e )
			{
				numReplies = NUM_PINGS;
			}
		}
			
		for (int j = 0; j < NUM_PINGS; j++) 
		{
			System.out.println(
					"PING " + j + ": " + replies[j] + " RTT: "
					+((rtt[j] > 0) ? Long.toString(rtt[j]):"< 1")
					+" ms");
		}
	}	


	public static void main ( String args[] )
	{
		String host = "127.0.0.1";
		int port = 9876;

		System.out.println("链接主机"+host+"端口号："+port);

		TjullinUDPClient client = new TjullinUDPClient(host,port);
		client.run();
	}
}
