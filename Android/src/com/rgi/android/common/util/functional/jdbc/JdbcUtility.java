package com.rgi.android.common.util.functional.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author Luke Lambert
 *
 */
public class JdbcUtility
{
    /**
     * Returns {@Link ArrayList} of the type of the input consisting of the results of applying the
     *  operations in {@Link ResultSetMapper} on the given {@Link ResultSet}
     *
     * @param resultSet
     *      The result set to perform the operations on
     * @param resultSetMapper
     *      the definition of the operations performed on a {@Link ResultSet}
     * @return
     *      an ArrayList of the type of input to the operation
     * @throws SQLException
     *      throws if an SQLException occurs
     */
    public static <T> ArrayList<T> map(final ResultSet resultSet, final ResultSetMapper<T> resultSetMapper) throws SQLException
    {
        if(resultSet == null || resultSet.isClosed())
        {
            throw new IllegalArgumentException("Result set may not be null or close");
        }

        if(resultSetMapper == null)
        {
            throw new IllegalArgumentException("Result set mapper may not be null");
        }

        final ArrayList<T> results = new ArrayList<T>();

        while(resultSet.next())
        {
            results.add(resultSetMapper.apply(resultSet));
        }

        return results;
    }
}
