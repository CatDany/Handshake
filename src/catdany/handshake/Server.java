package catdany.handshake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

import catdany.cryptocat.api.CatCert;
import catdany.cryptocat.api.CatCipher;
import catdany.cryptocat.api.CatDecryptor;

public class Server implements Runnable
{
	public final PipedInputStream in;
	public final PipedOutputStream out;
	
	public final BufferedReader reader;
	public final PrintWriter writer;

	public final Thread thread;
	
	public CatCert certLocal;
	public CatDecryptor decryptor;
	public CatCipher cipher;
	
	public Server(PipedInputStream in, PipedOutputStream out) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		this.certLocal = Handshake.serverCert;
		this.decryptor = new CatDecryptor(certLocal.privateKey, certLocal.algorithmKeys);
		this.in = in;
		this.out = out;
		this.reader = new BufferedReader(new InputStreamReader(in));
		this.writer = new PrintWriter(new OutputStreamWriter(out), true);
		this.thread = new Thread(this, "Server");
		thread.start();
	}
	
	public void run()
	{
		while (true)
		{
			try
			{
				String read = reader.readLine();
				if (read.startsWith("ENCRYPTED"))
				{
					byte[] encryptedMessage = DatatypeConverter.parseHexBinary(read.substring("ENCRYPTED ".length()));
					String message = new String(cipher.decrypt(encryptedMessage), Handshake.charset);
					log("Received encrypted message: %s", message);
				}
				else if (read.equals("HELLO"))
				{
					log("Received HELLO message from client. Sending public certificate.");
					send("HANDSHAKE_CERT %s", certLocal);
				}
				else if (read.startsWith("HANDSHAKE_PRE_MASTER_SECRET"))
				{
					byte[] encryptedKey = DatatypeConverter.parseHexBinary(read.substring("HANDSHAKE_PRE_MASTER_SECRET ".length()));
					byte[] key = decryptor.decrypt(encryptedKey);
					cipher = new CatCipher(key, Handshake.cipherPadding);
					sendEncrypted("Just letting you know that we're now communicating over an encrypted channel.");
					log("Received master secret from client.");
				}
			}
			catch (IOException t)
			{
				//System.err.println("Exception in Client.run() on thread " + thread.getName());
				//t.printStackTrace();
			}
			catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException t)
			{
				t.printStackTrace();
			}
		}
	}
	
	public void send(String format, Object... args)
	{
		String s = String.format(format, args);
		log("Sent: %s", s);
		writer.println(s);
	}
	
	//TODO:sign data coming from server
	public void sendEncrypted(String format, Object... args) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException
	{
		String s = String.format(format, args);
		log("Sent (Encrypted): %s", s);
		send("ENCRYPTED %s", DatatypeConverter.printHexBinary(cipher.encrypt(s.getBytes(Handshake.charset))));
	}
	
	public void log(String format, Object... args)
	{
		System.out.println(String.format("[%s] %s", thread.getName(), String.format(format, args)));
	}
}