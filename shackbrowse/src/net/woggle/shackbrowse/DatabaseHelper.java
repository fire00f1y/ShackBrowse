package net.woggle.shackbrowse;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

  public static final String TABLE_NOTES = "notes";
  public static final String COLUMN_NUNIQUE = "n_id";
  public static final String COLUMN_NTYPE = "n_type";
  public static final String COLUMN_NPOSTID = "n_postid";
  public static final String COLUMN_NBODY = "n_body";
  public static final String COLUMN_NAUTHOR = "n_author";
  public static final String COLUMN_NTIME = "n_time";
  public static final String COLUMN_NKW = "n_keyword";

  public static final String TABLE_POSTQUEUE = "postqueue";
  public static final String COLUMN_PID = "p_id";
  public static final String COLUMN_PTEXT = "p_text";
  public static final String COLUMN_PREPLYTO = "p_parentid";
  public static final String COLUMN_PFINALID = "p_finalid";
  public static final String COLUMN_PISMESSAGE = "p_ismessage";
  public static final String COLUMN_PISNEWS = "p_isnews";
  public static final String COLUMN_PSUBJECT = "p_subject";
  public static final String COLUMN_PRECIPIENT = "p_recipient";
  public static final String COLUMN_PFINALIZEDTIME = "p_finalizedtime";

  private static final String DATABASE_NAME = "shkbrs3.db";
  private static final int DATABASE_VERSION = 9;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table "
      + TABLE_NOTES + "("
          + COLUMN_NUNIQUE + " integer primary key,"
          + COLUMN_NPOSTID + " integer,"
          + COLUMN_NTYPE + " text not null,"
          + COLUMN_NBODY + " text not null,"
          + COLUMN_NAUTHOR + " text not null, "
          + COLUMN_NTIME + " integer, "
          + COLUMN_NKW + " text not null);";
  private static final String DATABASE_CREATE2 =  "create table "
      + TABLE_POSTQUEUE + "(" + COLUMN_PID
      + " integer primary key autoincrement, " + COLUMN_PTEXT
      + " text not null, " + COLUMN_PREPLYTO + " integer, " + COLUMN_PFINALID + " integer, " + COLUMN_PISMESSAGE + " integer, " + COLUMN_PISNEWS + " integer, " + COLUMN_PSUBJECT + " text, " + COLUMN_PRECIPIENT + " text, " + COLUMN_PFINALIZEDTIME + " integer);";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
    database.execSQL(DATABASE_CREATE2);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(DatabaseHelper.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    if (((oldVersion == 5) || (oldVersion == 6) || (oldVersion == 7)) && (newVersion == 8))
    {
    	// only postqueue needs update
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTQUEUE);
    	db.execSQL(DATABASE_CREATE2);
    }
    else
    {
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTQUEUE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
	    onCreate(db);
    }
  }

} 