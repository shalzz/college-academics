package com.shalzz.attendance;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class MigrationTest {

    private File newFile;
    private File upgradedFile;

    @Before
    public void setup() throws IOException {
        File baseDir = new File("build/tmp/migration");
        FileUtils.cleanDirectory(baseDir);
        newFile = new File(baseDir, "new.db");
        upgradedFile = new File(baseDir, "upgraded.db");
        File firstDbFile = new File("src/test/assets", "origin.db");
        FileUtils.copyFile(firstDbFile, upgradedFile);
    }

    @Test
    public void upgrade_should_be_the_same_as_create() throws Exception {
        Context context = RuntimeEnvironment.application;
        DatabaseHandler helper = new DatabaseHandler(context);

        SQLiteDatabase newDb = SQLiteDatabase.openOrCreateDatabase(newFile, null);
        SQLiteDatabase upgradedDb = SQLiteDatabase.openDatabase(
                upgradedFile.getAbsolutePath(),
                null,
                SQLiteDatabase.OPEN_READWRITE
        );

        helper.onCreate(newDb);
        helper.onUpgrade(upgradedDb, 9, DatabaseHandler.DATABASE_VERSION); // we are starting
        // migration testing from version 9

        Set<String> newSchema = extractSchema(newFile.getAbsolutePath());
        Set<String> upgradedSchema = extractSchema(upgradedFile.getAbsolutePath());

        assertThat(upgradedSchema).isEqualTo(newSchema);
    }

    private Set<String> extractSchema(String url) throws Exception {

        final Set<String> schema = new TreeSet<>();

        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + url);

            ResultSet tables = conn.getMetaData().getTables(null, null, null, null);
            while (tables.next()) {

                String tableName = tables.getString("TABLE_NAME");
                String tableType = tables.getString("TABLE_TYPE");
                schema.add(tableType + " " + tableName);

                ResultSet columns = conn.getMetaData().getColumns(null, null, tableName, null);
                while (columns.next()) {

                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    String columnNullable = columns.getString("IS_NULLABLE");
                    String columnDefault = columns.getString("COLUMN_DEF");
                    schema.add("TABLE " + tableName +
                            " COLUMN " + columnName + " " + columnType +
                            " NULLABLE=" + columnNullable +
                            " DEFAULT=" + columnDefault);
                }
                columns.close();
            }

            tables.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return schema;
    }
}