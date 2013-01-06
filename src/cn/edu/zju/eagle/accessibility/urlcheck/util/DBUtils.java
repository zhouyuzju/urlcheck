package cn.edu.zju.eagle.accessibility.urlcheck.util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;

public final class DBUtils{

	private static DataSource myDataSource = null;

	private DBUtils() {
	}

	static {
		try {
			Properties prop = new Properties();
			InputStream is = DBUtils.class.getClassLoader()
					.getResourceAsStream("dbcp.properties");
			prop.load(is);
			myDataSource = BasicDataSourceFactory.createDataSource(prop);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static DataSource getDataSource() {
		return myDataSource;
	}

	public static Connection getConnection() throws SQLException {
		return myDataSource.getConnection();
	}

	public static void free(ResultSet rs, Statement st, Connection conn) {
		try {
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (st != null)
					st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				if (conn != null)
					try {
						conn.close();
						// myDataSource.free(conn);
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		}
	}
	
	//added from zhouyu
	public static void free(Statement st, Connection conn) 
	{
		try 
		{
			if (st != null)
				st.close();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			if (conn != null)
			{
				try 
				{
					conn.close();
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	/*public static void main(String[] args) throws SQLException, UnsupportedEncodingException{
		System.out.println(DBUtils.getDataSource());
		Connection con = DBUtils.getConnection();
		PreparedStatement ps = con.prepareStatement("select id,cache,title from pages where id = 21270");
		ResultSet rs=ps.executeQuery();
		while(rs.next()){
			//Blob myblob = rs.getBlob("cache");
			byte[] myblob =rs.getBytes("cache");
			String out = new String(myblob, "utf-8");
			System.out.println("网页编号=" + rs.getInt("id"));
			//System.out.println("title="+rs.getString("title"));
			System.out.println("Blob 网页" +out);
		}
		DBUtils.free(rs, ps, con);
	}
	*/
}