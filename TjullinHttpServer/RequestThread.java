import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

/************tjullin服务器线程的基本功能***********/
/*
1.响应get请求,返回要获取的本地文件
2.响应head请求,返回询问文件的基本信息
3.在文件找不到的情况下，返回failHTML告知错误信息
4.支持文件类型----.gih
		      ----.html
			  ----.txt
			  ----.jpeg
*/  
/************tjullib服务器线程的基本功能***********/

public class RequestThread implements Runnable
{
	/********tjullin服务器线程的参数***************/
	private File directory;//允许请求资源的根目录
	private String defaultFileName;//默认请求资源的名称
	private String failHtml;//失败后传输的html文件
	/********tjullin服务器线程的参数***************/
	
	/*******tjullin服务器的线程池******************/
	private static List pool = new LinkedList();

	public static void addThread ( Socket request )
	//将新的请求线程放入线程池当中
	{
		/*利用synchronized关键字保证添加过程只有一个进程执行该代码块，
		防止冲突*/	
		synchronized ( pool )
		{
			//将当前线程添加到线程池
			pool.add(pool.size(),request);
			//唤醒所有正在等待的线程
			pool.notifyAll();
		}
	}
	/*******tjullin服务器的线程池******************/
   
	/*******tjullin服务器的日志信息打印************/
	private void print_date ( )
	//打印日志信息的具体时间
	{
		SimpleDateFormat date =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.print
			( date.format( new Date()) + " : " );
	}

	private void print_method ( String method )
	//打印成功获取到请求的信息
	{
		print_date();
		System.out.println ("获取到来自浏览器的"+method+"请求");
	}
	
	private void print_success ( String fileName )
	//成功索引到文件后打印信息
	{
		print_date();
		System.out.println("成功索引到目标文件："+fileName );
	}
	
	private void print_process ( )
	//告知文件传输的信息打印
	{
		print_date();
		System.out.println("文件正在传输中.........");
	}	
	
	private void print_finish ( String fileName )
	//告知文件传输结束的信息打印
	{
		print_date();
		System.out.println( fileName + " 传输完毕！");
	}
	
	private void print_fail ( String fileName )
	//告知文件没有找到的信息打印
	{
		print_date();
		System.out.println( fileName + "没有找到！");
	}
	/*******tjullin服务器的日志信息打印************/


	public RequestThread ( File directory , String name )
	//用请求的资源的路径和名称初始化当前线程
	{
		/*如果要求获取的文件路径是一个文件而并非文件夹，那么
		告知服务器路径非法*/
		if ( directory.isFile())
				throw new IllegalArgumentException();//抛出参数非法异常
		this.directory = directory;//初始化获取资源的路径
		if ( name != null )
			defaultFileName = name;//初始化要获取资源的名称（包含后缀）
		makeFailHtml();
	}
	
	/******tjullin服务器的HTTP报文编辑************/

	public static String getType ( String name )
	//获取对应文件类型的标准HTTP报文的文件类型字符串
	{
		if ( name.endsWith(".html") || name.endsWith(".htm") )
			return "text/html";
		else if ( name.endsWith(".txt") || name.endsWith(".java"))
			return "text/plain";
		else if ( name.endsWith(".gif"))
			return "image/gif";
		else if ( name.endsWith(".class"))
			return "application/octet-stream";
		else if ( name.endsWith(".jpg") || name.endsWith(".jpeg"))
			return "image/jpeg";
		else if ( name.endsWith(".png") )
			return "image/png";
		else return "text/plain";
	}

	public String get_Header ( int length , String type )
	//获取返回到浏览器的GET请求的响应报文的报头
	{
		String ret = "HTTP/1.0 200 OK\r\n";
		Date now = new Date();
		ret += "Date: " + now + "\r\n";
		ret += "Server: tjullin 1.0\r\n";
		if ( length >= 0 )
			ret += "Content-length: " + length + "\r\n";
		ret += "Content-Type: " + type + "\r\n\r\n";
		return ret;
	}
	
	public String head_Header ( int length , String type )
	{
		String ret = "HTTP/1.1 200 OK\r\n";
		Date now = new Date();
		ret += "Date: " + now + "\r\n";
		ret += "Server: tjullin 1.0\r\n";
		if ( length >= 0 )
			ret += "Content-length: " + length + "\r\n";
		ret += "Content-Type: " + type + "\r\n\r\n";
		return ret;
	}

	public void makeFailHtml ( )
	{
		failHtml = "<HTML>\r\n";
		failHtml += "<HEAD><TITLE>File Not Found</TITLE></HRAD\r\n";
		failHtml += "<BODY>\r\n";
		failHtml += "<H1>HTTP Error 404: File Not Found</H1>";
		failHtml += "</BODY></HTML>\r\n";
	}
	/******tjullin服务器的HTTP报文编辑************/

	
	public void run ( )
	//执行当前线程，响应请求
	{
		//设置要访求资源的目录
		String root = directory.getPath();
		while ( true )
		{
			Socket connection;
			//服务器与客户端（代理服务器）通信的socket
			/***********tjullin的线程池获取socket***************/
			synchronized ( pool )
			{
				while ( pool.isEmpty() )
				/*
				1.如果线程池为空，那么当前线程等待
				2.如果当前线程被打断，那么判断打断的线程是否导致线程池非空
				3.如果线程池非空，那么跳出循环，利用线程池中第一个socket
				初始化socket通信
				4.如果线程池依旧为空，那么继续等待，直到线程池非空
				*/
				{
					try
					{
						//使当前线程等待....
						pool.wait();
					}
					//捕捉当前线程被打断的异常，防止程序崩溃
					catch ( InterruptedException e )
					{
					}
				}
				//获取到线程池的第一个socket初始化socket通信
				connection = (Socket)pool.remove(0);
			}
			/***********tjullin的线程池获取socket***************/
			try
			{
				
				//客户端（代理服务器）向服务器发出请求的输入流
				Reader client = new InputStreamReader(
					new BufferedInputStream(
						connection.getInputStream()),"ASCII");
				//响应客户端（代理服务器请求）请求的输出流
				OutputStream out = new BufferedOutputStream(
					connection.getOutputStream());
				Writer server = new OutputStreamWriter(out);
				//存储HTTP行串
				StringBuffer request = new StringBuffer(80);

				String fileName;//设置要返回资源的名称
				String contentType;//设置要返回内容的文件类型
				/********tjullin的服务器线程获取客户端请求信息*****/
				while ( true )
				//读取报文的第一行
				{
					int c = client.read();
					if ( c == '\t' || c == '\n' || c == -1 )
						break;
					request.append((char)c);
				}
				String get = request.toString();
				//获取报头第一行的字符串
				StringTokenizer collection = new StringTokenizer(get);
				/*切割字符串的应用类，默认的分隔符是空格，制表符
				回车符和换行符*/
				String method = collection.nextToken();
				print_method ( method );//告知客户端请求的方式
				//获取请求的方式：GET,POST,HEAD
				String version = "";//记录支持HTTP协议的版本
				/*******tjullin的服务器线程获取客户端请求信息*****/

				/*******tjullin的服务器线程处理GET请求************/
				if ( method.indexOf("GET") != -1 )
				{
					//获取请求资源的文件名称
					fileName = collection.nextToken();
					/*如果请求资源是个目录，那么改为请求
						在这个目录中的默认文件*/
					if ( fileName.endsWith("/")	)
						fileName += defaultFileName;
					/*获取所请求资源的文件类型的标准
					HTTP报文格式字符串*/
					contentType = getType(fileName);
					//获取报文支持的HTTP协议的版本
					if ( collection.hasMoreTokens() )
						version = collection.nextToken();
					//获取请求文件的全路径
					File file =
						new File ( directory , fileName );
					//如果文件可读，且路径能够匹配上
					if ( file.canRead()&&
						file.getCanonicalPath().startsWith(root))
					{
						//打印成功索引到的文件信息
						print_success (fileName );
						//打开索引到的目标文件
						print_process();
						DataInputStream fileStream = new DataInputStream(
							new BufferedInputStream(
								new FileInputStream(file)));
						//存储二进制文件内容的目标文件内容
						byte[] data = new byte[(int)file.length()];
						//将文件内容读到data数组
						fileStream.readFully(data);
						//关闭文件
						fileStream.close();
						//如果请求的报文是遵守HTTP协议的
						if ( version.startsWith("HTTP"))
						{
							//向客户端发出报文的报头部分
							server.write( 
								get_Header( data.length,contentType));
							server.flush();
						}
						//发送报文的主体文件内容部分
						out.write(data);
						out.flush();
						print_finish( fileName);
					}
					//如果文件没有找到
					else 
					{
						print_fail( fileName);
						//如果请求报文遵守HTTP协议
						if ( version.startsWith("HTTP"))
						{
							//发送回客户端HTTP的报头部分
							server.write(get_Header(-1,"text/html"));
							server.flush();
						}
						//传送告知错误的html文件
						server.write(failHtml);
						server.flush();
					}
				}
				/*************tjullin的服务器线程处理GET请求***********/
				
				/*******tjullin的服务器线程处理HEAD请求************/
				if ( method.indexOf("GET") != -1 )
				{
					//获取请求资源的文件名称
					fileName = collection.nextToken();
					/*如果请求资源是个目录，那么改为请求
						在这个目录中的默认文件*/
					if ( fileName.endsWith("/")	)
						fileName += defaultFileName;
					/*获取所请求资源的文件类型的标准
					HTTP报文格式字符串*/
					contentType = getType(fileName);
					//获取报文支持的HTTP协议的版本
					if ( collection.hasMoreTokens() )
						version = collection.nextToken();
					//获取请求文件的全路径
					File file =
						new File ( directory , fileName );
					//如果文件可读，且路径能够匹配上
					if ( file.canRead()&&
						file.getCanonicalPath().startsWith(root))
					{
						//打印成功索引到的文件信息
						print_success (fileName );
						print_process();
						//打开索引到的目标文件
						DataInputStream fileStream = new DataInputStream(
							new BufferedInputStream(
								new FileInputStream(file)));
						//存储二进制文件内容的目标文件内容
						byte[] data = new byte[(int)file.length()];
						//将文件内容读到data数组
						fileStream.readFully(data);
						//关闭文件
						fileStream.close();
						//如果请求的报文是遵守HTTP协议的
						if ( version.startsWith("HTTP"))
						{
							//向客户端发出报文的报头部分
							server.write( 
								head_Header( data.length,contentType));
							server.flush();
						}
						print_finish(fileName);
					//HEAD请求不需要发送主体部分,只发送报头即可
					/*	//发送报文的主体文件内容部分
						out.write(data);
						out.flush();
					*/
					}
					//如果文件没有找到
					else 
					{
						//如果请求报文遵守HTTP协议
						if ( version.startsWith("HTTP"))
						{
							print_fail( fileName );
							//发送回客户端HTTP的报头部分
							server.write(head_Header(-1,"text/html"));
							server.flush();
						}
						//传送告知错误的html文件
						server.write(failHtml);
						server.flush();
					}
				}
				/*****tjullin的服务器处理head请求****************/
			}
			catch ( Exception e )
			{
			}
			//一定要执行的清理工作，放在finally代码块当中
			finally 
			{
				try
				{
					connection.close();
				}
				catch ( IOException e2 )
				{
				}
			}
		}
	}
}
