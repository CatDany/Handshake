package catdany.handshake;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
		rootCert = CatCert.fromJson(new File("root.json"));
		serverCert = CatCert.fromJson(new File("server.json"));
		serverCert = serverCert.getPrivateKeyCert(Files.readAllBytes(Paths.get("server.key")), "00000000");
		
		pair = getLinkedPair();
		pair.getClient().send("HELLO");
		console = new Console(pair.getClient(), pair.getServer());
	}
	
	/**
	 * Link a client and a server
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
