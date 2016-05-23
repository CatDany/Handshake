package catdany.handshake;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

import javax.crypto.NoSuchPaddingException;

import catdany.cryptocat.api.CatCert;
import catdany.cryptocat.api.CatUtils;

public class Handshake
{
	public static CatCert serverCert;
	public static CatCert rootCert;
	public static ClientAndServer pair;
	public static Console console;
	
	public static final Charset charset = Charset.forName("Windows-1251");
	public static final byte cipherPadding = (byte)' ';
	
	public static void main(String[] args) throws Exception
	{
		CatUtils.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"));
		if (args.length > 0)
		{
			if (args[0].equals("simulate"))
			{
				rootCert = CatCert.fromJson(new File("root.json"));
				serverCert = CatCert.fromJson(new File("server.json"));
				serverCert = serverCert.getPrivateKeyCert(Files.readAllBytes(Paths.get("server.key")), "00000000");
				pair = getLinkedPair();
				pair.getClient().send("HELLO");
			}
			else if (args[0].equals("socket-server"))
			{
				System.out.println("Initializing as client...");
				serverCert = CatCert.fromJson(new File("server.json"));
				serverCert = serverCert.getPrivateKeyCert(Files.readAllBytes(Paths.get("server.key")), "00000000");
				ServerSocket socketServer = new ServerSocket(Integer.parseInt(args[1]));
				System.out.println("Waiting for client to connect...");
				Socket socketClient = socketServer.accept();
				System.out.println("Client connected.");
				pair = new ClientAndServer(null, new Server(socketClient.getInputStream(), socketClient.getOutputStream()));
			}
			else if (args[0].equals("socket-client"))
			{
				rootCert = CatCert.fromJson(new File("root.json"));
				System.out.println("Root certificate has been read from file 'root.json'");
				System.out.println("Connecting to server...");
				Socket socket = new Socket(InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
				System.out.println("Connected to server successfully.");
				pair = new ClientAndServer(new Client(socket.getInputStream(), socket.getOutputStream()), null);
			}
		}
		else
		{
			System.out.println("No arguments are specified. Use 'simulate', 'socket client [ip] [port]' or 'socket server [port]'");
		}
		console = new Console(pair.getClient(), pair.getServer());
	}
	
	/**
	 * Link a client and a server using {@link PipedInputStream} and {@link PipedOutputStream}
	 * @return
	 * @throws IOException
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static ClientAndServer getLinkedPair() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException
	{
		PipedOutputStream outClient = new PipedOutputStream();
		PipedOutputStream outServer = new PipedOutputStream();
		PipedInputStream inClient = new PipedInputStream(outServer);
		PipedInputStream inServer = new PipedInputStream(outClient);
		Client client = new Client(inClient, outClient);
		Server server = new Server(inServer, outServer);
		return new ClientAndServer(client, server);
	}
}
