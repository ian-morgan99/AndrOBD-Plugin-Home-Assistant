package com.fr3ts0n.androbd.plugin.homeassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Database helper for storing OBD data records with timestamps and sent status
 */
public class DataDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "DataDbHelper";
    
    private static final String DATABASE_NAME = "androbd_ha_data.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table name
    private static final String TABLE_DATA = "data_records";
    
    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_KEY = "key";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_SENT = "sent";
    
    // Create table SQL
    private static final String CREATE_TABLE = 
            "CREATE TABLE " + TABLE_DATA + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_KEY + " TEXT NOT NULL, " +
            COLUMN_VALUE + " TEXT NOT NULL, " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
            COLUMN_SENT + " INTEGER DEFAULT 0)";
    
    // Create index on timestamp for efficient queries
    private static final String CREATE_INDEX = 
            "CREATE INDEX idx_timestamp ON " + TABLE_DATA + "(" + COLUMN_TIMESTAMP + ")";
    
    // Create index on sent status for efficient queries
    private static final String CREATE_INDEX_SENT = 
            "CREATE INDEX idx_sent ON " + TABLE_DATA + "(" + COLUMN_SENT + ")";
    
    public DataDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_INDEX);
        db.execSQL(CREATE_INDEX_SENT);
        Log.d(TAG, "Database created");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, just drop and recreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
        onCreate(db);
        Log.d(TAG, "Database upgraded from version " + oldVersion + " to " + newVersion);
    }
    
    /**
     * Insert a new data record
     */
    public long insertRecord(DataRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY, record.getKey());
        values.put(COLUMN_VALUE, record.getValue());
        values.put(COLUMN_TIMESTAMP, record.getTimestamp());
        values.put(COLUMN_SENT, record.isSent() ? 1 : 0);
        
        long id = db.insert(TABLE_DATA, null, values);
        record.setId(id);
        
        return id;
    }
    
    /**
     * Get all unsent records
     */
    public List<DataRecord> getUnsentRecords() {
        List<DataRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_DATA,
                null,
                COLUMN_SENT + " = 0",
                null,
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        
        return records;
    }
    
    /**
     * Get records within a time range
     */
    public List<DataRecord> getRecordsByTimeRange(long startTime, long endTime) {
        List<DataRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_DATA,
                null,
                COLUMN_TIMESTAMP + " >= ? AND " + COLUMN_TIMESTAMP + " <= ?",
                new String[]{String.valueOf(startTime), String.valueOf(endTime)},
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        
        return records;
    }
    
    /**
     * Mark a record as sent
     */
    public void markAsSent(long recordId) {
        SQLiteDatabase db = getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_SENT, 1);
        
        db.update(TABLE_DATA, values, COLUMN_ID + " = ?", new String[]{String.valueOf(recordId)});
    }
    
    /**
     * Mark multiple records as sent
     */
    public void markAsSent(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return;
        }
        
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SENT, 1);
            
            for (Long id : recordIds) {
                db.update(TABLE_DATA, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            }
            
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Delete old sent records (older than specified timestamp)
     */
    public int deleteOldSentRecords(long olderThan) {
        SQLiteDatabase db = getWritableDatabase();
        
        return db.delete(
                TABLE_DATA,
                COLUMN_SENT + " = 1 AND " + COLUMN_TIMESTAMP + " < ?",
                new String[]{String.valueOf(olderThan)}
        );
    }
    
    /**
     * Get total record count
     */
    public long getRecordCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DATA, null);
        
        long count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
            cursor.close();
        }
        
        return count;
    }
    
    /**
     * Get unsent record count
     */
    public long getUnsentRecordCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_DATA + " WHERE " + COLUMN_SENT + " = 0",
                null
        );
        
        long count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
            cursor.close();
        }
        
        return count;
    }
    
    /**
     * Clear all records
     */
    public void clearAllRecords() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DATA, null, null);
        Log.d(TAG, "All records cleared");
    }
    
    /**
     * Convert cursor to DataRecord object
     */
    private DataRecord cursorToRecord(Cursor cursor) {
        DataRecord record = new DataRecord();
        
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        record.setKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY)));
        record.setValue(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE)));
        record.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
        record.setSent(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SENT)) == 1);
        
        return record;
    }
}
