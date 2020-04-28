package com.lagou.edu.dao.impl;

import com.lagou.edu.pojo.Account;
import com.lagou.edu.dao.AccountDao;
import com.lagou.edu.utils.ConnectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 应癫
 */
@Repository("accountDao")
public class JdbcTemplateDaoImpl implements AccountDao {
    @Autowired
    private ConnectionUtils connectionUtils;
    public void setConnectionUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }
    @Override
    public Account queryAccountByCardNo(String cardNo) throws Exception {

        String sql = "select * from account where cardNo=?";
        Connection con = connectionUtils.getCurrentThreadConn();
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setString(1,cardNo);
        ResultSet resultSet = preparedStatement.executeQuery();
        Account account = new Account();
        while(resultSet.next()) {
            account.setCardNo(resultSet.getString("cardNo"));
            account.setName(resultSet.getString("name"));
            account.setMoney(resultSet.getInt("money"));
        }
        resultSet.close();
        preparedStatement.close();
        //con.close();
        return account;
    }

    @Override
    public int updateAccountByCardNo(Account account) throws Exception {
        String sql = "update account set money=? where cardNo=?";
        Connection con = (Connection) connectionUtils.getCurrentThreadConn();
        PreparedStatement preparedStatement = con.prepareStatement(sql);
        preparedStatement.setInt(1,account.getMoney());
        preparedStatement.setString(2,account.getCardNo());
        int i = preparedStatement.executeUpdate();
        preparedStatement.close();
       // con.close();
        return i;
    }
}
