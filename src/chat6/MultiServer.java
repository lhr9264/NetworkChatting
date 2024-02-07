package chat6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer {
	
	//멤버변수
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	//클라이언트 정보를 저장하기 위한 Map 컬렉션 생성
	Map<String,PrintWriter> clientMap;
	
	//생성자
	public MultiServer() {
		/* 클라이언트의 이름과 접속시 생성한 출력스트림을 저장할
		HashMap 인스턴스 생성 */
		clientMap = new HashMap<String, PrintWriter>();
		/* HashMap 동기화 설정. 쓰레드가 사용자 정보에 동시접근하는
		것을 차단한다. */
		Collections.synchronizedMap(clientMap);
	}
	//채팅 서버 초기화
	public void init() {
		
		try {
			//서버소켓 생성
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			/* 1명의 클라이언트가 접속할때마다 허용해주고 동시에
			쓰레드를 생성한다. */
			while(true) {
				socket = serverSocket.accept();
				/* 클라이언트 1명당 하나의 쓰레드가 생성되어 메세지
				전송 및 수신을 담당한다. */
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/* 인스턴스 생성 후 초기화 메서드를 호출한다. */
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
	}
	
	/* 접속된 모든 클라이언트 측으로 서버의 메세지를 Echo 해주는
	역할을 수행한다.(이전 단계에서는 보낸 사람에게만 Echo 되었다) */
	public void sendAllMsg(String name, String msg)
	{
		/* Map에 저장된 클라이언트의 key를 얻어온다. key에는
		대화명이 저장되어 있다. */
		Iterator<String> it = clientMap.keySet().iterator();
		
		//앞에서 얻어온 대화면(key값)의 갯수만큼 반복한다.
		while(it.hasNext()) {
			try {
				//각 클라이언트의 PrintWriter 인스턴스를 추출한다.
				PrintWriter it_out = (PrintWriter)
				clientMap.get(it.next());
				/*
				클라이언트에게 메세지를 전달할때 매개변수로 name이
				있는 경우와 없는경우를 구분해서 전달한다.
				 */
				if(name.equals("")) {
					/* 입장 혹은 퇴장에서 사용되는 부분 */
					it_out.println(msg);
				}
				else {
					/* 메세지를 보낼때 사용되는 부분 */
					it_out.println("["+ name +"]"+ msg);
				}
			}
			catch(Exception e ) {
				System.out.println("예외:"+e);
			}
		}
	}
	class MultiServerT extends Thread {
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(),
				true);
				in = new BufferedReader(new
				InputStreamReader(this.socket.getInputStream()));
			}
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
		}
		
		@Override
		public void run() {
			String name = "";
			String s = "";
			
			try {
				/* 클라이언트가 보내는 최초메세지는 대화명이므로
				접속에 대한 부분을 출력하고 Echo 한다. */
				//클라이언트의 이름을 읽어온다.
				name = in.readLine();
				/* 방금 접속한 클라이언트를 제외한 나머지에게 입장을
				알린다. */
				sendAllMsg("", name + "님이 입장하셨습니다.");
				//현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				//접속자의 이름을 서버의 콘솔에 띄어주고...
				System.out.println(name + " 접속");
				//HashMap에 저장된 클라이언트의 수를 확인할 수 있다.
				System.out.println("현재 접속자 수는"
				+clientMap.size()+ "명 입니다.");
				
				/* 두번째 입력부터는 메세지이므로 모든 클라이언트에게
				Echo 한다. */
				while(in != null) {
					s = in.readLine();
					if(s == null)
						break;
					//서버의 콘솔에 출력되고..
					System.out.println(name +" >> "+ s);
					//모든 클라이언트에게 Echo 한다.
					sendAllMsg(name, s);
				}
			}
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
			finally {
				/*
				클라이언트가 접속을 종료하면 Socket예외가 발생하게
				되어 finally절로 진입하게 된다. 이때 "대화명"을 통해
				Map에서 클라이언트의 정보를 제거한다. 
				 */
				System.out.println(Thread.currentThread().getName()
						+" 종료");
				
				try {
					in.close();
					out.close();
					socket.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
