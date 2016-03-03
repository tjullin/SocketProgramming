import java.net.*;

/***********tjullin的UDP测试服务器功能列表************/

/***********tjullin的UDP测试服务器功能列表************/

public class TjullinUDPServer
{
	/*******tjullin的UDP测试服务器的参数列表********/
	public static int port = 9876;//获取UDP信息的端口
	public static int SIZE = 4096;//发送和接收数据报的最大大小
	/*******tjullin的UDP测试服务器的参数列表********/


	public static void main ( String args[] )
			throws Exception
	{
		//初始化获取数据包的socket,要指明获取数据的端口
		DatagramSocket socket = new DatagramSocket(port);
		byte[] receiveData = new byte[SIZE];//收到的数据
		byte[] sendData = new byte[SIZE];//发送的数据
		//记录收到的数据包发送方的ip地址
		InetAddress IPAddress;
		//记录收到的数据包发送方的端口
		int sendPort;
		
		while ( true )
		{
			/****tjullin的UDP测试服务器收取UDP数据*******/
			//收到的数据包
			DatagramPacket receivePacket = 
				new DatagramPacket(
					receiveData,receiveData.length);
			socket.receive(receivePacket);//阻塞线程，知道收到数据包
			String content = new String ( receivePacket.getData());
			//将获取的UDP报文中的信息打印到控制台
			if ( content != null && !content.equals(""))
				System.out.println(content);
			//获取收到的数据包发送方的ip地址
			IPAddress = receivePacket.getAddress();
			//获取收到的数据包发送方的端口
			sendPort = receivePacket.getPort();
			//要发送回客户端的信息
			sendData = content.getBytes();
			/****tjullin的UDP测试服务器收取UDP数据*******/
			System.out.println("获取成功！");
			/****tjullin的UDP测试服务器回复UDP数据*******/
			DatagramPacket sendPacket = new DatagramPacket(
			//利用获取的信息填写发送的数包
				sendData,sendData.length,IPAddress,sendPort);
			socket.send(sendPacket);
			System.out.println("发送成功");
			/****tjullin的UDP测试服务器恢复UDP数据*******/
		}
	}
}
