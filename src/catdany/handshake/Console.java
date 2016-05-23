package catdany.handshake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class Console implements Runnable
{
	public final Client client;
	public final Server server;
	
	public Console(Client client, Server server)
	{
		this.client = client;
		this.server = server;
		Thread thread = new Thread(this, "Console");
		thread.start();
		System.out.println("Console ready.");
	}
	
	@Override
	public void run()
	{
		BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
		try
		{
			while (true)
			{
				String read = sysin.readLine();
				if (read.startsWith("client>server"))
				{
					if (client.cipher != null)
					{
						client.sendEncrypted(read.substring(14));
					}
					else
					{
						client.send(read.substring(14));
					}
				}
				else if (read.startsWith("server>client"))
				{
					if (server.cipher != null)
					{
						server.sendEncrypted(read.substring(143));
					}
					else
					{
						server.send(read.substring(14));
					}
				}
				else
				{
					System.out.println("Use 'client>server' or 'server>client' and then type a message your want to send.");
				}
			}
		}
		catch (IOException t)
		{
			System.err.println("Console error.");
			t.printStackTrace();
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException t)
		{
			t.printStackTrace();
		}
	}
}