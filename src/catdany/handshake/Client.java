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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

import catdany.cryptocat.CryptoCat;
import catdany.cryptocat.api.CatCert;
import catdany.cryptocat.api.CatCipher;
import catdany.cryptocat.api.CatEncryptor;

public class Client implements Runnable
{
	public final PipedInputStream in;
	public final PipedOutputStream out;
	
	public final BufferedReader reader;
	public final PrintWriter writer;

	public final Thread thread;
	
	public CatCert certRemote;
	public CatEncryptor encryptor;
	public CatCipher cipher;
	
	public Client(PipedInputStream in, PipedOutputStream out) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		this.in = in;
		this.out = out;
		this.reader = new BufferedReader(new InputStreamReader(in));
		this.writer = new PrintWriter(new OutputStreamWriter(out), true);
		this.thread = new Thread(this, "Client");
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
					if (message.equals("Just letting you know that we're now communicating over an encrypted channel."))
					{
						sendEncrypted("Cool.");
					}
				}
				else if (read.startsWith("HANDSHAKE_CERT"))
				{
					certRemote = CatCert.fromJson(read.substring("HANDSHAKE_CERT ".length()));
					log("Received server certificate.");
					CryptoCat.printCert(certRemote, true, 0);
					SecureRandom random = SecureRandom.getInstanceStrong();
					byte[] preMasterSecret = new byte[8];
					random.nextBytes(preMasterSecret);
					cipher = new CatCipher(preMasterSecret, Handshake.cipherPadding);
					encryptor = new CatEncryptor(certRemote.publicKey, certRemote.algorithmKeys);
					byte[] encryptedPreMasterSecret = encryptor.encrypt(preMasterSecret);
					send("HANDSHAKE_PRE_MASTER_SECRET %s", DatatypeConverter.printHexBinary(encryptedPreMasterSecret));
					log("Sent master secret to server.");
				}
			}
			catch (IOException t)
			{
				//System.err.println("Exception in Client.run() on thread " + thread.getName());
				//t.printStackTrace();
			}
			catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidKeySpecException t)
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