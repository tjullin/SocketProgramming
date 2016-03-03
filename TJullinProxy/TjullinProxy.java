import java.io.*;
import java.net.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;

/*********tjullin的代理服务器3.0功能列表********
1.转发客户端的http请求，记录日志信息，利用socket和url进行通信
2.进行流量信息的统计
3.设置限制的网站
--------------未完成----------------------------
4.进行proxy的串联
5.定向输出日志信息
------------------------------------------------
6.代理服务器性能参数设置
**********tjullin的代理服务器3.0功能列表********/

public class TjullinProxy extends Thread
//继承自Thread的代理线程，方便多条代理服务同步进行
{
	/*********tjullin的代理服务器3.0的参数列表****/
	public static int SIZE = 4096;//输入输出缓存区域的大小
	public static int WAIT_CLIENT = 1000;//设置客户端链接的超时时间
	public static int WAIT_SERVER = 1000;//设置服务器链接的超时时间
	public static int PROXY_PORT = 9000;//设置代理服务器的端口号
	public static int PAUSE = 50;//设置代理服务器的两次链接之间的间隔
	public static int RETRIES = 5;//重新链接的最大次数
	public static double up_flow = 0;//上传的流量
	public static String up_unit = "B";//上传流量的单位
	public static double up_data = 0;//上传流量的总量(单位固定为B)
	public static double down_flow = 0;//下载的流量
	public static String down_unit = "B";//下载流量的单位
	public static double down_data = 0;//下载流量的总量(单位固定为B)
	public static int MAX_LIMIT = 100;//访问限制的最大数量
	//public static OutputStream log;//设置log的输出流(默认操作台)
	/*********tjullin的代理服务器3.0的参数列表****/	
	private Socket client = null;//与客户端交互的socket通信
	private int upload = 0;//上传的流量
	private int download = 0;//下载的流量

	//利用客户端发出的访问请求，初始化与客户端通信的socket
	public TjullinProxy ( Socket client )
	{	
		this.client = client;
		this.start();
	}
	
	/********tjullin的代理服务器3.0的日志信息打印函数**/
	void print_date ( )
	//打印日期时间
	{
		SimpleDateFormat date =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.print
				( date.format( new Date())+ " : ");
	}

	void print_request ( String url , int num )
	//打印当前的请求信息
	{
		System.out.println ("正在尝试第"+ num + "次连接服务器");
		System.out.println ("URL : " + url );
	}

	void print_fail ( String url , int num  , int pattern )
	//打印当前请求失败时的状态信息
	{
		System.out.println("第" + num + "次链接失败");
		System.out.println("URL : " + url );
		System.out.print("错误原因：链接");
		if ( pattern == 1 )
			System.out.println("链接出错");
		if ( pattern == 2 )
			System.out.println("地址没找到主机");
		if ( pattern == 3 )
			System.out.println("通信输入输出流出错");
	}

	void print_success ( String url )
	//打印请求成功的状态信息
	{
		System.out.println("链接成功!");
		System.out.println("URL : " + url );
	}
	/********tjullin的代理服务器3.0的日志信息打印函数**/

	/********tjullin的代理服务器3.0的流量记录******/
	void print_flow ( )
	//输出本次访问线程的流量统计信息
	{
		System.out.println ( "---------本次操作的流量信息--------------");
		System.out.println ( "|   上传流量："+ upload + "B           |" );
		System.out.println ( "|   下载流量："+download+ "B           |" );
		System.out.println ( "-----------------------------------------");
	}

	void print_sum ( )
	//输出代理服务器代理期间的流量统计信息
	{
		up_data += upload;
		down_data += download;
		adjust ( );
		DecimalFormat df = new DecimalFormat("0.0");
		System.out.println("-------------流量总量--------------------");
		System.out.println("|    上传流量：" + df.format(up_flow)+ " " + up_unit 
							+"    |" );
		System.out.println("|    下载流量：" + df.format(down_flow) + " " + down_unit
							+"    |" );
		System.out.println("-----------------------------------------");
	}
	
	void adjust ( )
	{
		up_flow = up_data;
		if ( up_flow >= 1024 )
		{
			up_flow /= 1024;
			up_unit = "KB";
		}
		if ( up_flow >= 1024 )
		{
			up_flow /= 1024;
			up_unit = "MB";
		}
		if ( up_flow >= 1024 )
		{
			up_flow /= 1024;
			up_unit = "GB";
		}
		down_flow = down_data;
		if ( down_flow >= 1024 )
		{
			down_flow /= 1024;
			down_unit = "KB";
		}
		if ( down_flow >= 1024 )
		{
			down_flow /= 1024;
			down_unit = "MB";
		}
		if ( down_flow >= 1024 )
		{
			down_flow /= 1024;
			down_unit = "GB";
		}
	}
	/********tjullin的代理服务器3.0的流量记录******/

	/********tjullin的代理服务器3.0的访问限制******/
	static String baidu = "www.baidu.com";
	static String sohu = "www.sohu.com";
	static String qq = "www.qq.com";
	static String sina = "www.sina.com";
	static String wangyi = "www.163.com";
	static int limit_cnt = 0;
	static String [] limit = new String[MAX_LIMIT];

	//添加访问限制
	static void add_limit ( String host  )
	{
		limit[limit_cnt++] = host;
	}
	//检查是否被限制
	boolean check ( String url )
	{
		for ( int i = 0 ; i < limit_cnt ; i++ )
			if ( url.indexOf(limit[i]) != -1 )
			{
				print_limit ( url );
				return true;
			}
		return false;
	}
	//通知客户端当前链接被限制
	void print_limit ( String url )
	{
		print_date();
		System.out.println ( "当前请求被限制，不能被访问");
		System.out.println ( "URL :" + url );
	}
	/********tjullin的代理服务器3.0的访问限制******/

	public void run ()
	{
			byte buf[] = new byte[SIZE];

			try
			{
				/*****tjullin的代理服务器3.0从客户端获取url**********/	
				client.setSoTimeout(WAIT_CLIENT);//设置客户端链接超时的时间
				//获取客户端的请求的输入流
				InputStream request = client.getInputStream();
				//响应客户端的请求的输出流
				OutputStream response = client.getOutputStream();
				//获取客户请求报文中请求资源的url
				int ch = request.read();//读取字节，如果读入结束，ch获取到-1
				String address = "";//储存url描述的地址（路径）
				while ( ch != -1 )//如果成功读取到字节
				{
					upload++;
					address += (char) ch;//将字节转化为字符
					if ( ch == 13 ) break;//读取到换行符终止读入操作
					ch = request.read();//获取新的字节
				}
				//将报文的首行GET url /HTTP..根据' '切割，获取url的部分
				String url_string = address.split(" ")[1];
				/****tjullin的代理服务器3.0从客户端获取url***********/
				

				/****tjullin的代理服务器3.0的访问限制检查************/
				if ( check(url_string )) return;
				/****tjullin的代理服务器3.0的访问限制检查************/

				/**tjullin的代理服务器3.0通过端口和主机地址访问服务器**/
				URL url = new URL(url_string);
				int port = url.getPort();//获取要访问的服务器端口号
				if ( port == -1 )
					port = url.getDefaultPort();
				Socket server = null;//创建与服务器交互的socket通信
				boolean success = false;//标记当前访问是否成功
				int retry = 0;//当前尝试链接的次数；
				while ( retry++ < RETRIES )
				{
					try
					{
						print_date();	
						print_request( url_string , retry );
						server = new Socket(url.getHost() , port );
						success = true;
						print_date();
						print_success ( url_string );
						break;
					}
					//链接失败的处理
					catch ( ConnectException ex )
					{
						print_date();
						print_fail( url_string , retry , 1 );
						//client.close();//关闭与客户端的通信
						//ex.printStackTrace();//错误报告
					}
					//请求的主机找不到的错误处理
					catch ( UnknownHostException ex )
					{
						print_date();
						print_fail( url_string , retry , 2 );
						//client.close();//关闭与客户端的通信
						//ex.printStackTrace();//错误报告
					}
					//通信间失败的错误处理
					catch ( IOException ex )
					{
						print_date();
						print_fail( url_string , retry , 3 );
						//client.close();//关闭与客户端的通信
						//ex.printStackTrace();//错误报告
					}
				}
				if ( !success ) client.close();//关闭与客户端的通信
				//如果与客户端通信没有发生错误
				if ( !client.isClosed() )
				{
					//设置服务器的链接超时时间
					server.setSoTimeout(WAIT_SERVER);
					//获取向服务器提交请求的输出流
					OutputStream ask = server.getOutputStream();
					//将从客户端获取的报文转发给服务器
					ask.write(address.getBytes());
					try
					{
						int size;//从客户端的请求读出的残留部分的大小
						//如果成功读出内容，那么转发给服务器
						while (( size = request.read(buf) ) > 0 )
						{
							//无偏离量的将大小为size的字节输出到缓存
							upload += size;	
							ask.write(buf,0,size);
							ask.flush();//将缓存的内容一次性发送给服务器
						}
					}
					//通信链接超时的错误捕捉
					catch ( SocketTimeoutException ex )
					{
					}
					//获取从服务端获取资源的输入流0
					InputStream resource = server.getInputStream();
					//将资源传回客户端
					try
					{
						int size;//从服务器获取到的资源在缓存中存储的大小
						//如果成功获取到资源，将资源传回客户端
						while (( size = resource.read(buf) ) > 0) 
						{
							download += size;
							response.write(buf,0,size);
						}
					}
					//链接超时，那么关闭同服务器的socket通信
					catch ( SocketTimeoutException ex )
					{
						server.close();
					}
					//关闭同客户端的一切联系
					request.close();
					response.close();
					client.close();
				}	
				/**tjullin的代理服务器3.0通过端口和主机地址访问服务器**/

				print_flow();
				print_sum ();
			}
			//如果发现错误，发出错误报告
			catch ( Exception e )
			{
				//e.printStackTrace();
			}			
	}
	/**********tjullin的代理服务器3.0的功能************/
	public static void main ( String [] args )
	{
		ServerSocket serverSocket;
		add_limit ( qq );
		Socket socket;
		try
		{
			/********tjullin服务器3.0的参数设置*******/
			System.out.println("tjullin服务器3.0正常启动");
			
			/********tjullin服务器3.0的参数设置*******/
			serverSocket = new ServerSocket(PROXY_PORT);
			//设置代理服务器的端口号
			while ( true )
			{
				//阻塞进程，等待客户端发出请求	
				socket = serverSocket.accept();
				new TjullinProxy(socket);
				//根据当前的客户端请求开启一个线程进行处理
			}
		}
		catch ( IOException e )
		{
			//e.printStackTrace();
		}
	}
	/**********tjullin的代理服务器3.0的功能************/	
}
