package app.gavin.carpathrecorder;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Gavin on 2016/2/6.
 */
public class LocationDatabase {
    static final String mDataBaseName = "/data/data/app.gavin.carpathrecorder/databases/car_path_recorder.db";
    static final String mTableName = "location";
    static final String TAG = "LocationDatabase";

    SQLiteDatabase mDatabase = null;
    Boolean mDbLock = true;

    public LocationDatabase() {
        //打开数据库
        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDataBaseName, null);
        if(mDatabase != null) {
            //如果数据表不存在，就创建
            String sql = "create table if not exists " + mTableName + "(" +
                    "id integer primary key autoincrement NOT NULL," +
                    "uploadedFlag integer NOT NULL DEFAULT 0," +
                    "locationType integer NOT NULL DEFAULT 0," +
                    "provider text," +
                    "latitude double," +
                    "longitude double," +
                    "altitude double," +
                    "accuracy float," +
                    "gpsTime text," +
                    "satellites integer," +
                    "speed float," +
                    "bearing float" +
                    ")";
            mDatabase.execSQL(sql);
        }
    }

    public static String cursorToJson(Cursor crs)
    {
        JSONArray arr = new JSONArray();
        crs.moveToFirst();
        while (!crs.isAfterLast()) {
            int nColumns = crs.getColumnCount();
            JSONObject row = new JSONObject();
            for (int i = 0 ; i < nColumns ; i++) {
                String colName = crs.getColumnName(i);
                //滤除不需要的Col
                if(colName == null || colName.equals("id") || colName.equals("uploadedFlag"))
                    continue;

                try {
                    switch (crs.getType(i)) {
                        case Cursor.FIELD_TYPE_FLOAT  : row.put(colName, crs.getDouble(i))         ; break;
                        case Cursor.FIELD_TYPE_INTEGER: row.put(colName, crs.getLong(i))           ; break;
                        case Cursor.FIELD_TYPE_NULL   : row.put(colName, null)                     ; break;
                        case Cursor.FIELD_TYPE_STRING : row.put(colName, crs.getString(i))         ; break;
                    }
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
            arr.put(row);
            if (!crs.moveToNext())
                break;
        }
        return arr.toString();
    }

    public boolean getDatabaseStatus(){
        return mDatabase != null;
    }

    public void close(){
        if(mDatabase != null)
            mDatabase.close();
    }

    public long addLocationPoint(ContentValues values){
        long result = -1;
        if(values == null || mDatabase == null)
            return result;

        synchronized (mDbLock){
            result = mDatabase.insert(mTableName, null, values);
        }

        return result;
    }

    public Cursor queryNotUploadedRecord(int limit){
        Cursor c = null;
        if(mDatabase == null)
            return c;
        String sql = "select * from " + mTableName + " where uploadedFlag = 0 order by id limit " + limit;
        synchronized (mDbLock){
            c = mDatabase.rawQuery(sql, null);
        }
        return c;
    }

    public int updateUploadStatus(Cursor c){
        int lastID = -1;
        if(mDatabase == null)
            return lastID;
        if(c.getCount() <= 0)
            return lastID;
        int idIndex = c.getColumnIndex("id");
        synchronized (mDbLock) {
            String ids = "(";
            c.moveToFirst();
            do{
                lastID = c.getInt(idIndex);
                ids += lastID + ",";
            }while (c.moveToNext());
            ids = ids.substring(0, ids.length() - 1);
            ids += ")";
            String sql = "update " + mTableName + " set uploadedFlag=1 where id in " + ids;
            mDatabase.execSQL(sql);
        }
        return lastID;
    }

    public Cursor getLocationRecord(int limit){
        Cursor c = null;
        if(mDatabase == null)
            return c;
        String sql = "select * from " + mTableName + " limit " + limit;
        synchronized (mDbLock){
            c = mDatabase.rawQuery(sql, null);
        }
        return c;
    }
}

