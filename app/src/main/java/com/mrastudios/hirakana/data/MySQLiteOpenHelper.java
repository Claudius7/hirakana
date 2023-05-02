package com.mrastudios.hirakana.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mrastudios.hirakana.domain.GuessableJapaneseCharacters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Locale;

/**
 * A one table and one row database that only updates its row's data if
 * a table with one row already exists but this may change in the future
 */
final class MySQLiteOpenHelper extends SQLiteOpenHelper
{
    private static MySQLiteOpenHelper instance;
    private static final String DB_NAME = "UserData777";
    private static final int DB_VERSION = 2;

    private static final String TABLE_USERDATA = "UserData";
    private static final String KEY_ID = "_id";
    private static final String COLUMN_ATTEMPTS_QUIZ = "Attempts_Quiz";
    private static final String COLUMN_ATTEMPTS_CHALLENGE = "Attempts_Challenge";
    private static final String COLUMN_JAPANESE_CHARACTERS = "JapaneseCharacters";

    private MySQLiteOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    static synchronized MySQLiteOpenHelper getInstance(Context context) {
        if(instance == null) instance = new MySQLiteOpenHelper(context);
        return instance;
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) throws SQLiteException {
        sqLiteDatabase.execSQL(String.format(Locale.ENGLISH,
                "CREATE TABLE IF NOT EXISTS %1$s( %2$s INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " %3$s INTEGER," +
                        " %4$s INTEGER," +
                        " %5$s BLOB);",
                TABLE_USERDATA, KEY_ID, COLUMN_ATTEMPTS_QUIZ,
                COLUMN_ATTEMPTS_CHALLENGE, COLUMN_JAPANESE_CHARACTERS));
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(String.format(Locale.ENGLISH,
                "CREATE TABLE IF NOT EXISTS %1$s(%2$s INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " %3$s INTEGER NOT NULL," +
                        " %4$s INTEGER NOT NULL," +
                        " %5$s BLOB NOT NULL);",
                TABLE_USERDATA, KEY_ID, COLUMN_ATTEMPTS_QUIZ,
                COLUMN_ATTEMPTS_CHALLENGE, COLUMN_JAPANESE_CHARACTERS));

        final String TABLE_OLD_USERDATA = "TableUserDatas777";
        final String COLUMN_OLD_ATTEMPTS_QUIZ = "SavedQuizAttempts";
        final String COLUMN_OLD_ATTEMPTS_CHALLENGE = "SavedChallengeAttempts";
        final String COLUMN_OLD_JAPANESE_CHARACTERS = "SavedJapaneseCharacters";

        String query = String.format(Locale.ENGLISH,
                "SELECT %1$s, %2$s, %3$s FROM %4$s;",
                COLUMN_OLD_ATTEMPTS_QUIZ,
                COLUMN_OLD_ATTEMPTS_CHALLENGE,
                COLUMN_OLD_JAPANESE_CHARACTERS,
                TABLE_OLD_USERDATA);

        try(Cursor cursor = sqLiteDatabase.rawQuery(query, new String[]{})) {
            if(!cursor.moveToFirst()) {
                Log.e(getClass().getSimpleName(), "This can't be!!! Old database is empty and is NOT UPGRADED!");
                sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_USERDATA);
                return;
            }
        }
        sqLiteDatabase.execSQL(String.format(Locale.ENGLISH,
                "INSERT INTO %1$s(%2$s, %3$s, %4$s) %5$s;",
                TABLE_USERDATA,
                COLUMN_ATTEMPTS_QUIZ,
                COLUMN_ATTEMPTS_CHALLENGE,
                COLUMN_JAPANESE_CHARACTERS,
                query));
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_OLD_USERDATA);
    }

    private long insertOrUpdate(@NonNull ContentValues contentValues) {
        if(contentValues.size() == 0) return -1;

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        sqLiteDatabase.beginTransaction();
        Cursor cursor = sqLiteDatabase.query(TABLE_USERDATA,
                contentValues.keySet().toArray(new String[0]),
                KEY_ID + " = ?",
                new String[]{"1"},
                null, null, null);

        long returnCode;
        if(cursor.moveToFirst()) {
            returnCode = sqLiteDatabase.update(TABLE_USERDATA,
                    contentValues,
                    KEY_ID + " = ?",
                    new String[]{"1"});
        } else {
            returnCode = sqLiteDatabase.insertOrThrow(TABLE_USERDATA, null, contentValues);
        }

        if(returnCode != -1) sqLiteDatabase.setTransactionSuccessful();

        cursor.close();
        sqLiteDatabase.endTransaction();
        return returnCode;
    }

    @Nullable
    private Object loadData(int cursorTypeInt, @NonNull String columnName) {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        try (Cursor cursor = sqLiteDatabase.rawQuery(
                String.format(Locale.ENGLISH, "SELECT %1$s FROM %2$s WHERE %3$s = ?",
                        columnName, TABLE_USERDATA, KEY_ID),
                new String[]{"1"}))
        {
            if(!cursor.moveToFirst()) return null;

            switch (cursorTypeInt) {
                case Cursor.FIELD_TYPE_INTEGER:
                    try {
                        return cursor.getInt(0);
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), Arrays.toString(e.getStackTrace()));
                        return cursor.getLong(0);
                    }
                case Cursor.FIELD_TYPE_FLOAT:
                    try {
                        return cursor.getFloat(0);
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), Arrays.toString(e.getStackTrace()));
                        return cursor.getDouble(0);
                    }
                case Cursor.FIELD_TYPE_STRING:
                    return cursor.getString(0);
                case Cursor.FIELD_TYPE_BLOB:
                    byte[] savedObjectAsBytes = cursor.getBlob(0);
                    try(ObjectInputStream ois = new ObjectInputStream(
                            new ByteArrayInputStream(savedObjectAsBytes)))
                    {
                        return ois.readObject();
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), Arrays.toString(e.getStackTrace()));
                    }
                default:
                    return null;
            }
        }
    }

    public int loadQuizAttempts() {
        Object obj = loadData(Cursor.FIELD_TYPE_INTEGER, COLUMN_ATTEMPTS_QUIZ);
        return obj == null ? 0 : (int) obj;
    }

    public int loadChallengeAttempts() {
        Object obj = loadData(Cursor.FIELD_TYPE_INTEGER, COLUMN_ATTEMPTS_CHALLENGE);
        return obj == null ? 0 : (int) obj;
    }

    @Nullable
    public GuessableJapaneseCharacters loadGuessableJapaneseCharacters() {
        return (GuessableJapaneseCharacters) loadData(Cursor.FIELD_TYPE_BLOB, COLUMN_JAPANESE_CHARACTERS);
    }

    public boolean saveQuizAttempts(int quizAttempts) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ATTEMPTS_QUIZ, quizAttempts);
        return insertOrUpdate(contentValues) != -1;
    }

    public boolean saveChallengeAttempts(int challengeAttempts) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ATTEMPTS_CHALLENGE, challengeAttempts);
        return insertOrUpdate(contentValues) != -1;
    }

    public boolean saveGuessableJapaneseCharacters(
            GuessableJapaneseCharacters guessableJapaneseCharacters) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(guessableJapaneseCharacters);
        byte[] savedGameAsBytes = baos.toByteArray();
        baos.close();
        oos.close();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_JAPANESE_CHARACTERS, savedGameAsBytes);
        return insertOrUpdate(contentValues) != -1;
    }
}