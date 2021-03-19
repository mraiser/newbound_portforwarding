package com.newbound.robot.published;

import java.io.*;
import java.util.*;
import com.newbound.robot.BotBase;
import org.json.*;
import com.newbound.p2p.*;
import java.net.*;
import com.newbound.robot.*;

public class PortForwarding extends BotBase
{
	public static void main(String[] args) 
	{
		try 
		{
			new PortForwarding().start();
		} 
		catch (Exception x) 
		{
			x.printStackTrace();
		}
	}


	public String getServiceName() 
	{
		return "portforwarding";
	}


	public Object handleCommand(String cmd, Hashtable params) throws Exception
	{
		if (cmd.equals("portforwarding") || cmd.startsWith("portforwarding/")) return handlePortforwarding((String)params.get("id"), (String)params.get("stream"), (String)params.get("sessionid"));
		else if (cmd.equals("list") || cmd.startsWith("list/")) return handleList();
		else if (cmd.equals("addlocal") || cmd.startsWith("addlocal/")) return handleAddlocal((String)params.get("ipaddr"), (String)params.get("port"), (String)params.get("name"));
		else if (cmd.equals("deletelocal") || cmd.startsWith("deletelocal/")) return handleDeletelocal((String)params.get("id"));
		else if (cmd.equals("addremote") || cmd.startsWith("addremote/")) return handleAddremote((String)params.get("peer"), (String)params.get("id"), (String)params.get("name"), (String)params.get("port"), (String)params.get("ipaddr"), (String)params.get("peername"));
		else if (cmd.equals("deleteremote") || cmd.startsWith("deleteremote/")) return handleDeleteremote((String)params.get("id"));

		throw new Exception("UNKNOWN COMMAND: "+cmd);
	}

	public String getIndexFileName()
	{
		return "index.html";
	}


	public JSONObject getCommands() throws Exception
	{
		JSONObject commands = new JSONObject();
		JSONObject cmd;
		
		cmd = new JSONObject();
		commands.put("portforwarding", cmd);
		cmd.put("desc", "Begin portforwarding the given local share via the given stream with the requesting peer.");
		cmd.put("parameters", new JSONArray("[\"id\",\"stream\",\"sessionid\"]"));
		cmd.put("groups", "trusted,portforward");
		
		cmd = new JSONObject();
		commands.put("list", cmd);
		cmd.put("desc", "Returns a list of local and remote shared services.");
		cmd.put("parameters", new JSONArray());
		cmd.put("groups", "trusted,portforward");
		
		cmd = new JSONObject();
		commands.put("addlocal", cmd);
		cmd.put("desc", "Share a local service.");
		cmd.put("parameters", new JSONArray("[\"ipaddr\",\"port\",\"name\"]"));
		
		cmd = new JSONObject();
		commands.put("deletelocal", cmd);
		cmd.put("desc", "Stop sharing a local service.");
		cmd.put("parameters", new JSONArray("[\"id\"]"));
		
		cmd = new JSONObject();
		commands.put("addremote", cmd);
		cmd.put("desc", "Start sharing a remote service.");
		cmd.put("parameters", new JSONArray("[\"peer\",\"id\",\"name\",\"port\",\"ipaddr\",\"peername\"]"));
		
		cmd = new JSONObject();
		commands.put("deleteremote", cmd);
		cmd.put("desc", "Stop sharing a remote service.");
		cmd.put("parameters", new JSONArray("[\"id\"]"));
		
		return commands;
	}
	protected int getDefaultPortNum()
	{
		return 5773;
	}

	public Object handlePortforwarding(String id, String stream, String sessionid) throws Exception
	{
		PeerBot pb = PeerBot.getPeerBot();
		P2PPeer peer = pb.getPeer(sessionid, true, true);
		P2PConnection con = peer.getStream(Long.parseLong(stream));
		
		JSONObject fwd = LOCALPORTS.getJSONObject(id);
		newLocalForward(fwd, con);
		
		return "OK";
	}

	public Object handleList() throws Exception
	{
		JSONObject result = newResponse();
		result.put("local", LOCALPORTS);
		result.put("remote", REMOTEPORTS);
		return result;
	}

	public Object handleAddlocal(String ipaddr, String port, String name) throws Exception
	{
		JSONObject fwd = new JSONObject();
		fwd.put("name", name);
		fwd.put("ipaddr", ipaddr);
		fwd.put("port", Long.parseLong(port));
		String key = UUID.randomUUID().toString();
		
		File f = new File(getRootDir(), "localports.properties");
		Properties p = loadProperties(f);
		if (p == null) p = new Properties();
		p.put(key, fwd.toString());
		storeProperties(p, f);
		LOCALPORTS.put(key, fwd);
		
		JSONObject o = newResponse();
		o.put("data", fwd);
		o.put("key", key);
		
		return o;
	}

	public Object handleDeletelocal(String id) throws Exception
	{
		File f = new File(getRootDir(), "localports.properties");
		Properties p = loadProperties(f);
		if (p != null)
		{
		  p.remove(id);
		  storeProperties(p, f);
		  LOCALPORTS.remove(id);
		}
		
		JSONObject o = newResponse();
		
		return o;
	}

	public Object handleAddremote(String peer, String id, String name, String port, String ipaddr, String peername) throws Exception
	{
		JSONObject fwd = new JSONObject();
		fwd.put("name", name);
		fwd.put("port", port);
		fwd.put("peer", peer);
		fwd.put("ipaddr", ipaddr);
		fwd.put("peername", peername);
		
		startPortForwardingRemote(id, fwd);
		
		File f = new File(getRootDir(), "remoteports.properties");
		Properties p = loadProperties(f);
		if (p == null) p = new Properties();
		p.put(id, fwd.toString());
		storeProperties(p, f);
		
		JSONObject o = newResponse();
		o.put("data", fwd);
		o.put("key", id);
		
		return o;
		
	}

	public Object handleDeleteremote(String id) throws Exception
	{
		File f = new File(getRootDir(), "remoteports.properties");
		Properties p = loadProperties(f);
		if (p == null) p = new Properties();
		p.remove(id);
		storeProperties(p, f);
		
		REMOTEPORTS.remove(id);
		((ServerSocket)THREADS.get(id)).close();
		
		return "OK";
	}



private JSONObject LOCALPORTS = new JSONObject();
private JSONObject REMOTEPORTS = new JSONObject();
private Hashtable THREADS = new Hashtable();

public void init() throws Exception
{
  super.init();
  RUNNING = true;
  
  Properties p = loadProperties(new File(getRootDir(), "localports.properties"));
  if (p != null)
  {
    Enumeration e = p.keys();
    while (e.hasMoreElements())
    {
      String key = (String)e.nextElement();
      String val = p.getProperty(key);
      JSONObject fwd = new JSONObject(val);
      startPortForwardingLocal(key, fwd);
    }
  }
  
  p = loadProperties(new File(getRootDir(), "remoteports.properties"));
  if (p != null)
  {
    Enumeration e = p.keys();
    while (e.hasMoreElements())
    {
      String key = (String)e.nextElement();
      String val = p.getProperty(key);
      JSONObject fwd = new JSONObject(val);
      startPortForwardingRemote(key, fwd);
    }
  }
}

private void startPortForwardingLocal(String key, JSONObject fwd) throws Exception
{
  LOCALPORTS.put(key, fwd);
}

private void startPortForwardingRemote(final String key, final JSONObject fwd) throws Exception
{
  REMOTEPORTS.put(key, fwd);

  int port = fwd.getInt("port");
  final ServerSocket ss = new ServerSocket(port);
  fwd.put("connected", true);
  
  Runnable r = new Runnable()
  {
    public void run()
    {
      System.out.println("Start forwarding remote "+key);
      while (RUNNING) try
      {
    	if (!REMOTEPORTS.has(key) || REMOTEPORTS.get(key) == null) break;
        Socket s = ss.accept();
        newRemoteForward(key, fwd, s);
      }
      catch (Exception x) { x.printStackTrace(); }
      System.out.println("Done forwarding remote "+key);

      try 
      { 
    	  ss.close(); 
          fwd.put("connected", false);
      } 
      catch (Exception x) { x.printStackTrace(); }
    }
  };
    
  Thread t = new Thread(r);
  t.start();  
  THREADS.put(key, ss);
}

protected void newLocalForward(final JSONObject fwd, final P2PConnection con) throws Exception
{
	String ipaddr = fwd.getString("ipaddr");
	int port = fwd.getInt("port");
	final Socket s = new Socket(ipaddr, port);
	
    Runnable rr = new Runnable()
    {
      public void run()
      {
        try
        {
        	startListeningToPeer(s, con);
        	listenToLocalPort(s, con);
        }
        catch (Exception x) { x.printStackTrace(); }
      }
    };
    new Thread(rr).start();

}

protected void newRemoteForward(final String key, final JSONObject fwd, final Socket s) 
{
    Runnable rr = new Runnable()
    {
      public void run()
      {
        try
        {
        	String id = fwd.getString("peer");
        	PeerBot pb = PeerBot.getPeerBot();
        	P2PPeer peer = pb.getPeer(id, true, true);
        	final P2PConnection con = peer.newStream();
        	
        	Hashtable h = new Hashtable();
        	h.put("stream", ""+con.getID());
        	h.put("id", key);
        	JSONObject jo = peer.sendCommand("portforwarding", "portforwarding", h);
        	
        	startListeningToPeer(s, con);
        	listenToLocalPort(s, con);
        }
        catch (Exception x) { x.printStackTrace(); }
      }
    };
    new Thread(rr).start();

}

protected void listenToLocalPort(Socket s, P2PConnection con) throws IOException
{
  try
  {
    InputStream is = s.getInputStream();
    OutputStream os = con.getOutputStream();
    byte[] buf = new byte[4096];
	while (con.isConnected())
	{
		int i = is.read(buf);
		if (i == -1) break;
		os.write(buf, 0, i);
        os.flush();
	}
  }
  finally
  {
    if (con.isConnected()) try { con.close(); } catch (Exception xx) {}
    if (s.isConnected()) try { s.close(); } catch (Exception xx) {}
  }
}

protected void startListeningToPeer(final Socket s, final P2PConnection con)
{
	Runnable rrr = new Runnable()
	{
		public void run()
		{
			try
			{
            	OutputStream os = s.getOutputStream();
            	InputStream is = con.getInputStream();
                byte[] buf = new byte[4096];
            	while (con.isConnected())
            	{
            		int i = is.read(buf);
            		if (i == -1) break;
            		os.write(buf, 0, i);
                    os.flush();
            	}
			}
			catch (Exception x) 
            { 
              x.printStackTrace(); 
            }
            finally
            {
		      if (con.isConnected()) try { con.close(); } catch (Exception xx) {}
		      if (s.isConnected()) try { s.close(); } catch (Exception xx) {}
            }
		}
	};
	new Thread(rrr).start();

}

protected String handleShutdown(Hashtable params) throws Exception 
{
  Enumeration e = THREADS.elements();
  while (e.hasMoreElements())
  {
    ServerSocket t = (ServerSocket)e.nextElement();
    try { t.close(); } catch (Exception x) {}
  }
  
  return super.handleShutdown(params);
}

}

