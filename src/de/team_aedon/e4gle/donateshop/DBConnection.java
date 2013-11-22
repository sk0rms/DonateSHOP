package de.team_aedon.e4gle.donateshop;

import java.sql.*;
import java.util.*;

public class DBConnection {

	private Connection con;
	private Statement st;
	private ResultSet rs;
	private String servername;
	
	public DBConnection(String url, String user, String pw, String servername) throws Exception{
		Class.forName("com.mysql.jdbc.Driver");
		try {
			this.con = DriverManager.getConnection(url,user,pw);
		} catch(Exception e){
			throw new Exception("Konnte keine Verbindung zur Datenbank aufbauen!");
		}
		this.st = con.createStatement();
		this.servername = servername;
	}
	
	public void createDefaultTables() throws Exception{
		List<String> tables = Arrays.asList("CREATE TABLE IF NOT EXISTS Spieler(ID INT PRIMARY KEY AUTO_INCREMENT, Name VARCHAR(25), VotePoints INT NOT NULL DEFAULT '0') ENGINE=InnoDB", "CREATE TABLE IF NOT EXISTS Pakete(ID INT PRIMARY KEY AUTO_INCREMENT, Name VARCHAR(25)) ENGINE=InnoDB", "CREATE TABLE IF NOT EXISTS SpielerPakete(ID INT PRIMARY KEY AUTO_INCREMENT, SpielerID INT, PaketID INT, ExpirationDate BIGINT, FOREIGN KEY(SpielerId) REFERENCES Spieler(ID) ON DELETE CASCADE, FOREIGN KEY(PaketID) REFERENCES Pakete(ID) ON DELETE CASCADE) ENGINE=InnoDB");
		for(String table : tables){
		this.st.executeUpdate(table);
		}
		String sql = "CREATE TABLE IF NOT EXISTS "+this.servername+"(SPid INT, Erhalten TINYINT NOT NULL DEFAULT '0', FOREIGN KEY(SPid) REFERENCES SpielerPakete(ID) ON DELETE CASCADE) ENGINE=InnoDB";
		this.st.executeUpdate(sql);
	}
		
	public boolean playerExists(String name) throws Exception{
		String query = "Select ID From Spieler s WHERE s.Name = '"+name+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return true;
		} else {
			return false;
		}	
	}
	
	public boolean paketExists(String name) throws Exception{
		String query = "Select ID From Paket p WHERE p.Name = '"+name+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return true;
		} else {
			return false;
		}	
	}
	
	public boolean addPlayerIfNotExists(String name) throws Exception{
		String query = "SELECT ID FROM Spieler s WHERE s.Name = '"+name+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return true;
		} else {
			query = "INSERT INTO Spieler(Name) Values('"+name+"')";
			this.st.executeUpdate(query);
			return true;
		}
	}
	
	public List<Integer> getPlayerPakete(String player) throws Exception{
		List<Integer> pakete = new ArrayList<Integer>();
		String query = "Select sp.ID FROM Spieler s, Pakete p, SpielerPakete sp WHERE(sp.SpielerID = s.ID AND sp.PaketID = p.ID AND s.Name = '"+player+"')";
		this.rs = this.st.executeQuery(query);
		while(rs.next()){
			pakete.add(rs.getInt(1));
		}
		return pakete;
	}
	
	public List<String> giveUngivenPlayerPakete(String player) throws Exception{
		List<String> pakete = new ArrayList<String>();
		List<Integer> SPids = new ArrayList<Integer>();
		String query = "Select sp.ID, p.Name FROM Spieler s, Pakete p, SpielerPakete sp, "+this.servername+" server WHERE(s.ID = sp.SpielerID AND sp.PaketID = p.ID AND s.Name = '"+player+"' AND sp.ID = server.SPid AND server.Erhalten = 0)";
		this.rs = this.st.executeQuery(query);
		while(rs.next()){
			SPids.add(rs.getInt(1));
			pakete.add(rs.getString(2));
		}
		for(int i : SPids){
			query = "UPDATE "+this.servername+" SET Erhalten = 1 WHERE SPid = "+i;
			this.st.executeUpdate(query);
		}
		return pakete;
	}
	
	public boolean addPaketIfNotExists(String name) throws Exception{
		String query = "SELECT ID FROM Pakete p WHERE p.Name = '"+name+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return true;
		} else {
			query = "INSERT INTO Pakete(Name) Values('"+name+"')";
			this.st.executeUpdate(query);
			return true;
		}
	}
	
	public boolean addPaketToPlayer(String spieler, String paket, Long expiration) throws Exception{
		addPlayerIfNotExists(spieler);
		int SpielerID = getPlayerID(spieler);
		int PaketID = getPaketID(paket);
		if(expiration==-1L){
			String query = "INSERT INTO SpielerPakete(SpielerID, PaketID, ExpirationDate) Values('"+SpielerID+"', '"+PaketID+"', NULL)";
			this.st.executeUpdate(query);
		} else {
			String query = "INSERT INTO SpielerPakete(SpielerID, PaketID, ExpirationDate) Values('"+SpielerID+"', '"+PaketID+"', '"+expiration+"')";
			this.st.executeUpdate(query);
		}
		return true;
	}
	
	public int getPlayerID(String name) throws Exception{
		String query = "SELECT ID FROM Spieler s WHERE s.Name = '"+name+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			int SpielerID = rs.getInt(1);
			return SpielerID;
		} else {
			throw new NullPointerException("Der Spieler '"+name+"' existiert nicht!");
		}
	}
	
	public int getPaketID(String name) throws Exception{
		String query = "SELECT ID FROM Pakete p WHERE p.Name = '"+name+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			int PaketID = rs.getInt(1);
			return PaketID;
		} else {
			throw new NullPointerException("Das Paket '"+name+"' existiert nicht!");
		}
	}
	
	public long getExpirationDateInMS(int SPid) throws Exception{
		String query = "SELECT ExpirationDate FROM SpielerPakete sp WHERE sp.ID = "+SPid;
		this.rs =this.st.executeQuery(query);
		if(rs.next()){
			long variable = rs.getLong(1);
			if(variable==0L){
				throw new Exception("Das Paket (ID='"+SPid+"') kann nicht ablaufen!");
			} else {
				return variable;
			}
		} else {
			throw new NullPointerException("Diese Paket-ID ("+SPid+") existiert nicht!");
		}
	}
	
	public boolean isPaketExpired(int SPid) throws Exception{
		Long time = System.currentTimeMillis();
		String query = "SELECT ExpirationDate FROM SpielerPakete sp WHERE sp.ID = "+SPid;
		this.rs =this.st.executeQuery(query);
		if(rs.next()){
			Long variable = rs.getLong(1);
			if(variable==0L){
				throw new Exception("Das Paket (ID='"+SPid+"') kann nicht ablaufen!");
			} else if(variable<time){
				return true;	
			} else {
				return false;
			}
		} else {
			throw new NullPointerException("Diese Paket-ID ("+SPid+") existiert nicht!");
		}
	}
	
	public boolean paketExpireable(int SPid) throws Exception {
		String query = "SELECT ExpirationDate FROM SpielerPakete sp WHERE sp.ID = "+SPid;
		this.rs =this.st.executeQuery(query);
		if(rs.next()){
			Long variable = rs.getLong(1);
			if(variable==0){
				return false;
			} else {
				return true;
			}
		} else {
			throw new NullPointerException("Diese Paket-ID ("+SPid+") existiert nicht!");
		}
	}
	
	public boolean addPaketeToServerTableIfNotExists() throws Exception{
		String query = "SELECT sp.ID FROM SpielerPakete sp";
		List<Integer> SPids = new ArrayList<Integer>();
		this.rs = this.st.executeQuery(query);
		while(rs.next()){
			SPids.add(rs.getInt(1));
		}
		for(int i : SPids){
			query = "Select server.SPid FROM SpielerPakete sp, "+this.servername+" server WHERE sp.ID = server.spID AND sp.ID = "+i;
			this.rs = this.st.executeQuery(query);
			if(!rs.next()){
				String sql = "INSERT INTO "+this.servername+"(SPid) Values("+i+")";
				this.st.executeUpdate(sql);
			}
		}
		return true;
	}
	
	public List<String> getExpiredPlayerPakets(String player) throws Exception{
		List<Integer> SPid = getPlayerPakete(player);
		List<Integer> paketids = new ArrayList<Integer>();
		for(int id : SPid){
			if(paketExpireable(id)){
				if(isPaketExpired(id) && !paketMarkedAsExpired(id)){
					paketids.add(id);
					setPaketAsExpired(id);
				}
			}
		}
		List<String> pakete = new ArrayList<String>();
		for(int i : paketids){
			pakete.add(getPaketNameBySPid(i));
		}
		return pakete;
	}
	
	public String getPaketNameBySPid(int id) throws Exception{
		String query = "Select p.Name FROM SpielerPakete sp, Pakete p WHERE sp.ID = "+id+" AND sp.PaketID = p.ID";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return rs.getString(1);
		} else {
			throw new NullPointerException("Diese SP-ID existiert nicht!");
		}
	}
	
	public boolean setPaketAsExpired(int SPid) throws Exception{
		String query = "UPDATE "+this.servername+" SET Erhalten = 2 WHERE SPid = "+SPid;
		this.st.executeUpdate(query);
		return true;
	}
	
	public boolean paketMarkedAsExpired(int SPid) throws Exception{
		String query = "Select server.SPid FROM "+this.servername+" server WHERE server.Erhalten = 2 AND SPid = "+SPid;
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return true;
		}else {
			return false;
		}
	}
	
	public boolean playerHasBoughtPakets(String player) throws Exception{
		String query = "Select sp.ID FROM Spieler s, SpielerPakete sp WHERE s.ID = sp.SpielerID AND s.Name = '"+player+"'";
		this.rs = this.st.executeQuery(query);
		if(rs.next()){
			return true;
		}else {
			return false;
		}
	}
	
	public boolean hasAnUnexpiredPaketOf(String spieler, String paket) throws Exception{
		String query = "Select sp.ID FROM Spieler s, SpielerPakete sp, Pakete p WHERE s.ID = sp.SpielerID AND p.ID = sp.PaketID AND p.Name = '"+paket+"' AND s.Name = '"+spieler+"'";
		this.rs = this.st.executeQuery(query);
		List<Integer> i = new ArrayList<Integer>();
		while(rs.next()){
			i.add(rs.getInt(1));
		}
		for(int p : i){
			if(paketExpireable(p)){
				if(!isPaketExpired(p)){
					return true;
				}
			}
		}
		return false;
	}
}
