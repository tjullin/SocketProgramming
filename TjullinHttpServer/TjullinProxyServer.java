import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;


/*********tjullin代理服务器3.0功能列表*****
1.利用socket和url进行代理服务器和服务器以及客户端间的通信
2.记录日志信息
3.进行流量信息统计
4.限制访问
/*********tjullin代理服务器3.0功能列表*****/


public class TjullinProxyServer extends Thread {
    
	/**********tjullin代理服务器3.0参数列表*******/
	public static int SIZE = 4096;//读取报文的缓存的大小
	public static int PROXY_PORT = 9000;//代理服务器的端口号
	public static int RETRIES = 5;//访问失败的最大次数
	public static int LIMIT = 100;//访问权限限制的最大数量
	public static int SERVER_WAIT = 5000;//访问服务器的最长链接时间
	public static int CLIENT_WAIT = 1000;//链接客户端的最长链接时间
	public static int PAUSE = 50;//重新尝试链接的等待时间
	public static double up_flow = 0;//上传流量总量（单位调整）
	public static double up_data = 0;//上传流量总量
	public static double down_flow = 0;//下载流量总量（单位调整）
	public static double down_data = 0;//下载流量总量
	public static String up_unit;//上传流量的单位
	public static String down_unit;//下载流量的单位
	/**********tjullin代理服务器3.0参数列表*******/	
	
	/**********tjullin代理服务器3.0与服务器和客户端通信**/
	private Socket client = null;
	private Socket server = null;
	/**********tjullin代理服务器3.0与服务器和客户端通信**/
	
	/**********tjullin代理服务器3.0与服务器日志信息打印**/
	private void print_date ( )
	{
		SimpleDateFormat date =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.print
				( date.format( new Date() ) + " : " );
	}	
	
	private void print_get ()
	{
		print_date();
		System.out.println("客户端发出请求........");
	}
	
	private void print_request ( String host , int port , int num )
	{
		print_date();
		System.out.println( "尝试第"+num+"次连接主机："
						+host+"的"+port+"号端口");
	}

	private void print_success ( String host , int port )
	{
		print_date();
		System.out.println( "成功链接"+host+"的"+port+"端口");
	}
	
	private void print_fail (String host , int port ,int num ,int type  )
	{	
		print_date();
		System.out.println("第"+num+"次链接失败");
		System.out.println("失败原因：");
		if ( type == 1 )
			System.out.println("链接过程出错");
		if ( type == 2 )
			System.out.println("主机地址未找到");
	}
	//打印通信超时的情况
	void print_timeout ( String url )
	{
		print_date();
		System.out.println("通信超时");
		System.out.println("URL : " + url );
	}
	//打印传输传输成功
	void print_finish ( String url )
	{
		print_date();
		System.out.println("传输完毕");
		System.out.println("URL : " + url );
		print_flow();
		print_sum();
	}

	/**********tjullin代理服务器3.0与服务器日志信息打印**/

	/**********tjullin代理服务器3.0与服务器的访问权限限制*/
	//记录所有需要限制的主机的信息
	static private String [] limit = new String[LIMIT];
	static private int limit_cnt = 0;
	public static void add_limit ( String url )
	//用来添加新的限制
	{
		limit[limit_cnt++] = url;
	}

	private boolean check ( String url )
	//用来检查当前请求的url是否要被限制
	{
		for ( int i = 0 ; i < limit_cnt ; i++ )
			if ( url.indexOf(limit[i]) != -1 )
			{
				print_limit( url );
				return true;
			}
		return false;
	}
	//打印被限制的情况
	void print_limit ( String url )
	{
		print_date();
		System.out.println( "访问被限制");
		System.out.println( "URL : " + url );
	}

	/**********tjullin代理服务器3.0与服务器的访问权限限制*/

	/**********tjullin代理服务器3.0的流量统计*************/
	private int upload = 0;
	private int download = 0;

	private void print_flow ( )
	{
		System.out.println("本次上传流量："+upload 
						  +"B 下载流量："+download + "B");
	}

	private void print_sum ( )
	{
		adjust();
		DecimalFormat df = new DecimalFormat("0.0");
		System.out.println("----------流量总计-----------------");
		System.out.println
			("| 上传流量：" + df.format(up_flow) + " " + up_unit+ "  |" );
		System.out.println
			("| 下载流量：" + df.format(down_flow) +" "+ down_unit + "  |");
		System.out.println("-----------------------------------");
	}

	private void adjust ( )
	{
		up_data += upload;
		up_flow = up_data;
		down_data += download;
		down_flow = down_data;
		int temp = 0;
		while ( up_flow >= 1024 && temp < 4 )
		{
			up_flow /= 1024;
			temp++;
		}
		switch ( temp )
		{
			case 0 :
				up_unit = "B";
				break;
			case 1 :
				up_unit = "KB";
				break;
			case 2 :
				up_unit = "MB";
				break;
			case 3 :
				up_unit = "GB";
				break;
			default:
				break;
		}
		temp = 0;
		while ( down_flow >= 1024 && temp < 4 )
		{
			down_flow /= 1024;
			temp++;
		}
		switch ( temp )
		{
			case 0 :
				down_unit = "B";
				break;
			case 1 :
				down_unit = "KB";
				break;
			case 2 :
				down_unit = "MB";
				break;
			case 3 :
				down_unit = "GB";
				break;
			default:
				break;
		}
	}
	/**********tjullin代理服务器3.0的流量统计*************/

    public TjullinProxyServer (Socket socket) {
	//初始化和客户端通信的socket
        this.client = socket;
		//开启当前线程,调用run函数
        this.start();
    }


    public void run() {
	//代理访问服务器的主要功能函数：
	//	1.接收客户端的请求，获取要访问的主机地址和端口号
	//  2.验证请求是否有访问权限
	//  3.在权限足够条件下，向服务器转发报文，根据url获取资源
	//  4.将获取到的资源转发给客户端
	//  5.更新流量统计信息
        byte bytes[] = new byte[SIZE];
		//代理服务器与客户端以及服务器交互信息的缓存区
		
        try {
			/****1.tjullin的代理服务器3.0接收客户端的请求******/	
            client.setSoTimeout(CLIENT_WAIT);//设置链接超时的最大时间
			//与客户端交互中，获取请求的输入流
            InputStream request = client.getInputStream();
			//与客户端交互中，响应请求返回资源的输出流
            OutputStream response = client.getOutputStream();
            int ch = request.read();//获取请求的下一个字节，请求完毕返回-1
            String address = "";//记录GET报文的首行内容
            while (ch != -1) {//将第一行全部读入
                address += (char) ch;//将字节形式的值转化为字符
				upload++;
                if (ch == 13) {//见到换行符跳出循环
                    break;
                }
               ch = request.read();//读入请求下一个字节，请求完毕返回-1
            }
			//GET报文的首行为"GET http://.... /HTTP 1.1.. "格式
			//通过这行内容获取到url方便获取到主机地址和端口号
            String u = address.split(" ")[1];
			//利用split将以" "为基准分割成字符串的数组
            URL url = new URL(u);//初始化url用来获取请求的服务器的主机地址
            int port = url.getPort();//通过url获取端口号
			String host = url.getHost();//通过url获取要链接的主机号
            if (port == -1) {
                port = url.getDefaultPort();
				//如果不存在端口号，使用默认端口号80
            }
			print_get();
            /***1.tjullin的代理服务器3.0接收客户端请求*****/

			/***2.tjullin的代理服务器3.0访问权限验证*******/
			if ( check(address) ) return;			
			/***2.tjullin的代理服务器3.0访问权限验证*******/
			/***3.tjullin的代理服务器3.0转发报文到服务器***/
			int retry = 0;//当前尝试链接的次数
			boolean success = false;//记录当前链接是否成功的标志位
			while ( retry++ < RETRIES )
			{
			//	System.out.println("YES");
            	try {
					print_request(host,port,retry);
                	server = new Socket(host, port);
					//尝试与服务器建立通信
					//成功，则修改修改标志位
					//失败，则跳转到异常处理
					success = true;
					print_success( host, port);
					break;
            	}catch(ConnectException ex){
                	//client.close();
                	//ex.printStackTracea();
					print_fail ( host , port , retry , 1 ); 
            	}
            	catch(UnknownHostException ex){
                	//client.close();
                	//ex.printStackTrace();
					print_fail ( host , port , retry , 2 );
				}
             	catch (IOException ex) {
                	//client.close();
                	//ex.printStackTrace();
            	}
			}
			if ( !success ) client.close();
			//如果连接不成功，断开与客户端之间的链接
            if (!client.isClosed()) {//如果链接成功
                server.setSoTimeout(SERVER_WAIT);
				//设置代理服务器和服务器之间链接的超时时限
                OutputStream ask = server.getOutputStream();
				//获取向服务器发出请求的输出流
				//转发报文
                ask.write(address.getBytes());
                try{
                    while ((ch = request.read(bytes)) > 0) {
                        ask.write(bytes, 0, ch);
                        ask.flush();
						upload += ch;//更新上传流量
                    }
                } catch (SocketTimeoutException ex) {
                }
			/***3.tjullin的代理服务器向服务器转发报文*****/
			/***4.tjullin的代理服务器向客户端转发资源*****/
                InputStream resource = server.getInputStream();
				//获取到从服务器获取资源报文的输入流
				//将报文转发到客户端
                try {
                    while ((ch = resource.read(bytes)) > 0) {
                        response.write(bytes, 0, ch);
						download += ch;//更新下载流量
                    }
				//如果出现超时，断开与服务器的链接
                } catch (SocketTimeoutException ex) {
					//print_timeout( u );
                    //server.close();
                }
				print_finish ( u );
				//将与客户端的通信关闭
                response.close();
                request.close();
                client.close();
                server.close();
            }
			/***4.tjullin的代理服务器向客户端转发资源*****/
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServerSocket serverSocket;
        Socket socket;
        try {
			add_limit( "www.qq.com");	
			System.out.println("tjullin的代理服务器,version=3.0");
            serverSocket = new ServerSocket(PROXY_PORT);
            while (true) {
                socket = serverSocket.accept();
                new TjullinProxyServer(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
