package com.kozzztya.cycletraining.db.datasources;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.kozzztya.cycletraining.db.DBHelper;
import com.kozzztya.cycletraining.db.entities.Training;
import com.kozzztya.cycletraining.db.entities.TrainingView;
import com.kozzztya.cycletraining.utils.DateUtils;

public class TrainingsDS extends DataSourceView<Training, TrainingView> {

    public static final String TABLE_NAME = "trainings";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_MESOCYCLE = "mesocycle";
    public static final String COLUMN_COMMENT = "comment";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_DONE = "done";

    public static final String VIEW_NAME = "trainings_view";
    public static final String COLUMN_EXERCISE = TrainingJournalDS.COLUMN_EXERCISE;

    private static final String CREATE_TABLE = "create table "
            + TABLE_NAME
            + " (_id integer primary key autoincrement, "
            + COLUMN_DATE + " date, "
            + COLUMN_MESOCYCLE + " integer, "
            + COLUMN_COMMENT + " text, "
            + COLUMN_DONE + " integer default 0, "
            + COLUMN_PRIORITY + " integer default 100" //Set max priority to add new training into end of order
            + ");";

    private static final String CREATE_VIEW = "CREATE VIEW " + VIEW_NAME + " AS " +
            "SELECT e." + ExercisesDS.COLUMN_NAME + " " + COLUMN_EXERCISE + ", t.* " +
            "FROM " + TrainingJournalDS.TABLE_NAME + " tj, " + MesocyclesDS.TABLE_NAME + " m, " +
            TABLE_NAME + " t, " + ExercisesDS.TABLE_NAME + " e " +
            "WHERE m." + MesocyclesDS.COLUMN_ACTIVE + " = 1 AND tj." + COLUMN_MESOCYCLE + " = m._id AND tj." +
            COLUMN_EXERCISE + " = e._id AND t." + COLUMN_MESOCYCLE + " = m._id;";

    private static final String CREATE_TRIGGER_DELETE = "CREATE TRIGGER delete_training " +
            "BEFORE DELETE ON " + TABLE_NAME + " " +
            "FOR EACH ROW BEGIN " +
            " DELETE FROM " + SetsDS.TABLE_NAME + " WHERE " +
            SetsDS.COLUMN_TRAINING + " = old._id; END ";

    public TrainingsDS(DBHelper dbHelper) {
        super(dbHelper);
    }

    public static void onCreate(SQLiteDatabase database) {
        Log.v(DBHelper.LOG_TAG, CREATE_TABLE);
        database.execSQL(CREATE_TABLE);
        Log.v(DBHelper.LOG_TAG, CREATE_VIEW);
        database.execSQL(CREATE_VIEW);
        database.execSQL(CREATE_TRIGGER_DELETE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.v(DBHelper.LOG_TAG, "Upgrading table " + TABLE_NAME + " from version "
                + oldVersion + " to " + newVersion);
//        TODO safe delete
//        database.execSQL("DELETE FROM " + TABLE_NAME);
//        database.execSQL("DROP VIEW IF EXISTS " + VIEW_NAME);
//        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//        onCreate(database);
    }

    @Override
    public ContentValues getContentValues(Training training) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, String.valueOf(training.getDate()));
        values.put(COLUMN_MESOCYCLE, training.getMesocycle());
        values.put(COLUMN_COMMENT, training.getComment());
        values.put(COLUMN_DONE, training.isDone());
        values.put(COLUMN_PRIORITY, training.getPriority());
        return values;
    }

    @Override
    public Training entityFromCursor(Cursor cursor) {
        return new Training(
                cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                DateUtils.safeParse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE))),
                cursor.getLong(cursor.getColumnIndex(COLUMN_MESOCYCLE)),
                cursor.getString(cursor.getColumnIndex(COLUMN_COMMENT)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_DONE)) > 0,
                cursor.getInt(cursor.getColumnIndex(COLUMN_PRIORITY))
        );
    }

    @Override
    public TrainingView entityViewFromCursor(Cursor cursor) {
        return new TrainingView(
                cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
                DateUtils.safeParse(cursor.getString(cursor.getColumnIndex(COLUMN_DATE))),
                cursor.getLong(cursor.getColumnIndex(COLUMN_MESOCYCLE)),
                cursor.getString(cursor.getColumnIndex(COLUMN_COMMENT)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_DONE)) > 0,
                cursor.getInt(cursor.getColumnIndex(COLUMN_PRIORITY)),
                cursor.getString(cursor.getColumnIndex(COLUMN_EXERCISE))
        );
    }

    @Override
    public String[] getColumns() {
        return new String[]{COLUMN_ID, COLUMN_DATE, COLUMN_MESOCYCLE, COLUMN_COMMENT, COLUMN_DONE, COLUMN_PRIORITY};
    }

    @Override
    public String[] getViewColumns() {
        return new String[]{COLUMN_EXERCISE, COLUMN_ID, COLUMN_DATE, COLUMN_MESOCYCLE, COLUMN_COMMENT, COLUMN_PRIORITY, COLUMN_DONE};
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getViewName() {
        return VIEW_NAME;
    }

}