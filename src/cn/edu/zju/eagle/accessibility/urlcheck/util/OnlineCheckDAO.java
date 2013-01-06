package cn.edu.zju.eagle.accessibility.urlcheck.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.log4j.Logger;

public class OnlineCheckDAO {
	private Connection con;
	private PreparedStatement ps;
	
	private static Logger log = Logger.getLogger(OnlineCheckDAO.class);
	
	public void update(String html,int checkId){
		try{
			String sql = "update onlinecheck set html=?,time=? where id=?";
			con = DBUtils.getConnection();
			ps = con.prepareStatement(sql);
			ps.setString(1, html);
			ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			ps.setInt(3, checkId);
			ps.executeUpdate();
		}
		catch(Exception e){
			log.warn(e.getMessage());
		}
		finally{
			DBUtils.free(ps, con);
		}
	}
}
