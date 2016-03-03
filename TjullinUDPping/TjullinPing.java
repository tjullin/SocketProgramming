import java.io.*;
import java.net.*;

/*******tjullin的UDP-Ping程序的功能*******/
/*
1.类似于Ping指令，用来检查网络是否连通
*/
/*******tjullin的UDP-Ping程序的功能*******/

public class TjullinPing
{

	/***tjullin的UDP-Ping程序的参数***/
	static public int SIZE = 4096;//设置ping信息的最大长度
	static public int FLAG = 1;//标记当前ping程序是接收/发送状态
	/***tjullin的UDP-Ping程序的参数***/	
	DatagramSocket socket;//UDP协议的socket通信，只能接收/发送报文
	
	//重载的用来发送UDP信息的socket通信
	public void createSocket()
	{
		try
		{
			socket = new DatagramSocket();
		}
		catch ( SocketException e )
		{
			System.out.println("创建发送UDP的socket通信失败");
		}
	}

	//重载的用来接收UDP信息的socket,必须设置端口
	public void createSocket ( int port )
	{
		try
		{
			socket = new DatagramSocket(port);
		}
		catch ( SocketException e )
		{
			System.out.println("创建接收UDP的socket通信失败");
		}
	}

	//发送一个UDP-Ping信息(自定义的一个类)
	public void sendPing ( PingMessage ping )
	{
		/********tjullin的UDP-ping程序创建发送的UDP数据包*****/
		InetAddress host = ping.getHost();//获取发送目的主机的IP地址
		int port = ping.getPort();//获取目的主机负责接收ping信息的端口
		String message = ping.getContent();//获取要发送内容的主体

		try
		{
			//创建一个用来发送信息的数据包
			DatagramPacket packet =
				new DatagramPacket(
					message.getBytes(),message.length(),host,port);
			socket.send( packet );
			System.out.println("已经发送ping信息到"+host+":"+port);
		}
		catch ( IOException e )
		{
			System.out.println("UDP-ping信息发送失败");
		}
		/********tjullin的UDP-ping程序创建发送的UDP数据包*****/
	}

	//接收一个UDP-ping信息
	public PingMessage receivePing() throws SocketTimeoutException
	{
		/******tjullin的UDP-ping程序创建发送的UDP数据包***/
		byte buf[] = new byte[SIZE];//收取信息的缓存区
		DatagramPacket packet =//设置接收内容放置的缓存区 
			new DatagramPacket(
				buf , SIZE );
		PingMessage reply = null;//接收到的ping信息

		try
		{
			socket.receive(packet);//阻塞线程等待接收到数据包
			System.out.println(
				"收到来自 "+packet.getAddress() + ":" + packet.getPort()
				+" 的UDP-ping信息");
			//将byte转换成字符串
			String content = new String ( packet.getData());
			//构造ping信息
			reply = new PingMessage(
						packet.getAddress(),
						packet.getPort(),
						content);
		}
		catch ( SocketTimeoutException e )
		{
			throw e;
		}
		catch ( IOException e )
		{
			System.out.println("接收UDP-信息失败");
		}
		/******tjullin的UDP-ping程序创建发送的UDP数据包***/
		return reply;
	}
}
