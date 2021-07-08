/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.mbtiles;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqlHelper {

    public static Connection getConnection(File mbtilesFile) {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + mbtilesFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Connection to Mbtiles database could not be established.", e);
        }
    }

    public static ResultSet executeQuery(Connection connection, String sql) {
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(sql);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Query execution failed: %s", sql), e);
        }
    }

    public static void execute(Connection connection, String sql) {
        try {
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Statement execution failed: %s", sql), e);
        }
    }

    public static void addMetadata(Connection connection, String name, Object value) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO metadata (name,value) VALUES(?,?)");
            statement.setString(1, name);
            if (value instanceof String)
                statement.setString(2, (String) value);
            else if (value instanceof Integer)
                statement.setInt(2, (int) value);
            else if (value instanceof Long)
                statement.setLong(2, (long) value);
            else if (value instanceof Float)
                statement.setFloat(2, (float) value);
            else if (value instanceof Double)
                statement.setDouble(2, (double) value);
            else
                statement.setString(2, value.toString());
            statement.execute();
            statement.close();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not add metadata: %s=%s", name, value.toString()), e);
        }
    }
}
