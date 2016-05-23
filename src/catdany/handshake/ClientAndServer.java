package catdany.handshake;

public class ClientAndServer
{
	private final Client client;
	private final Server server;
	
	public ClientAndServer(Client client, Server server)
	{
		this.client = client;
		this.server = server;
	}
	
	public Client getClient()
	{
		return client;
	}
	
	public Server getServer()
	{
		return server;
	}
}