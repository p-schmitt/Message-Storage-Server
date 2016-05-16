/* Program: Message Server
 * This: messageserver.java
 * Date: 05/12/2016
 * Author: P. Schmitt, R. Hill
 * Purpose: The server side of the message program
 */

package messageserver;
import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.sql.*;

//====================class MessageServer===========================
//Server listens for commands from the client and responds accordingly
public class MessageServer 
{

    public static void main(String[] args) throws Exception 
    {
        ServerSocket  serverSocket = new ServerSocket(4444); 
        String info = "Started server on port ServerSocket[addr=" 
                + serverSocket.getLocalSocketAddress()+",localport="
                + serverSocket.getLocalPort() + "}";
        System.out.println(info);
        String clientSentence;
        String modSentence;
        Socket clientSocket = serverSocket.accept(); 
        do
        {
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
        
        clientSentence = inFromClient.readLine();
        String[] data = clientSentence.split(":");
        
        if(clientSentence==null)
        {
            break;
        }
        else if(data[0].equals("add"))
        {
            if (countRows(getConnection(), "Users", "username", data[1]) == 1)
                    {
                        outToClient.writeBytes("Username already in database. "
                                + "Please try another username!\n");
                    }
                    else
                    {
                        insert(data[1], hashPassword(data[2]));
                        modSentence = "User: " + data[1] + " has been added to "
                                + "the database, along with password.";
                        outToClient.writeBytes(modSentence + '\n');
                    }
        }
        else if(data[0].equals("login"))
        {
            String username = data[1];
            String password = data[2];
            Connection conn = getConnection();
            String select = "select * from users where username = '" + username+"'";
            PreparedStatement stmt = conn.prepareStatement(select);
            ResultSet rs = stmt.executeQuery();
            
            int count = 0;
            if(rs.next())
            {

                    if(MessageDigest.isEqual(rs.getBytes("pwd"), hashPassword(password)))
                    {
                        modSentence = "good";
                        outToClient.writeBytes(modSentence+"\n");
                        count++;
                    }
                    else
                    {
                        modSentence = "bad";
                        outToClient.writeBytes(modSentence+"\n");
                    }
            }
            else
            {
                outToClient.writeBytes("Username not found\n");
            }
        }
        else if(data[0].equals("insert"))
        {
            Connection conn = getConnection();
            String insert = "insert into messages values (?,?)";
            PreparedStatement stmt = conn.prepareStatement(insert);
            stmt.setString(1, data[1]);
            stmt.setString(2, data[2]);
            stmt.executeUpdate();
            modSentence ="Message: " +  data[1] + " has been saved to the database";
            outToClient.writeBytes(modSentence + '\n');
        }        
        else if(data[0].equals("id"))
        {
            Connection conn = getConnection();
            String select = "select * from messages where message_id = " 
                    + Integer.parseInt(data[1])+ " and username = '"+ data[2]+"'";
            PreparedStatement stmt = conn.prepareStatement(select);
            ResultSet rs = stmt.executeQuery();
            if(rs.next())
            {
                String result = rs.getString("message_content");
                System.out.println(result);
                outToClient.writeBytes(result+"\n");
            }
            else
            {
                outToClient.writeBytes("Message ID not found\n");
            }
        }
        else if(data[0].equalsIgnoreCase("string"))
        {
            Connection conn = getConnection();
            String result="";
            String select = "select * from messages where message_content like '%" 
                    + data[1]+"%'"+ " and username = '"+ data[2]+"'";
            PreparedStatement stmt = conn.prepareStatement(select);
            ResultSet rs = stmt.executeQuery();
            while(rs.next())
            {
                    result += rs.getString("message_content") +" \t";
                    System.out.println(result);
            }
            outToClient.writeBytes(result+"\n");
        }
        }while(!clientSentence.contains("QUIT"));
              
        clientSocket.close();
    }
    
    //===================getConnection============================
    //Returns a connection based on the local instance of my database
    public static Connection getConnection() throws SQLException, ClassNotFoundException
    {
      String url =  "jdbc:sqlserver://localhost:1433;databaseName=MessageStorage;integratedSecurity=true";
      Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      return DriverManager.getConnection(url);
    }
    
    //=====================hashPassword==========================
    //Takes a string as input and returns a SHA-256 hash value
    public static byte[] hashPassword(String password)
   {
       byte[] digest;
       try
       {
           MessageDigest sha = MessageDigest.getInstance("SHA-256");
           sha.update(password.getBytes());
           digest = sha.digest();
           return digest;
       }
       catch(Exception e)
       {
           System.out.println("Failed to hash password.");
       }
       return null;
   }
    
    //==================countRows============================
    //takes a target username and checks if it already exists in the database
    public static int countRows(Connection con, String table, String column, 
            String target) throws Exception
    {
        Statement stmt = null;
        ResultSet rs = null;
        int rowCount = -1;
        
        try
        {
            stmt = con.createStatement();
            rs = stmt.executeQuery("select count(*) from " + table + " where " + 
                    column + " = '" + target + "' ");
            rs.next();
            rowCount = rs.getInt(1);
        }
        finally
        {
            
        }
        return rowCount;
    }
    
    //=======================insert================================
    //inserts a new row into the users table
    public static void insert(String uid, byte[] hashPwd) throws 
            SQLException, Exception
    {
        Connection con = getConnection();
        
        String insertString = "insert into Users values(?,?)";
        PreparedStatement stmt = con.prepareStatement(insertString);
        
        stmt.setString(1, uid);
        stmt.setBytes(2, hashPwd);
        stmt.executeUpdate();
        stmt.close();
        con.close();
    }
}
