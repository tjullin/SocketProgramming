import java.net.*;

public class PingMessage
//单条ping信息的构造类
{
	private InetAddress address;//目的IP地址
	private int port = 0;//目的主机的端口号
	private String message;//传递的信息

	public PingMessage(
		InetAddress address , int port , String message )
	{
		this.address = address;
		this.port = port;
		this.message = message;
	}

	//一堆接口函数
	public InetAddress getHost()
	{
		return address;
	}

	public String getContent()
	{
		return message;
	}

	public int getPort()
	{
		return port;
	}
}
