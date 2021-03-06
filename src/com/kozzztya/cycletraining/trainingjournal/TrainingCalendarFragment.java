package com.kozzztya.cycletraining.trainingjournal;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;

import com.kozzztya.cycletraining.R;
import com.kozzztya.cycletraining.db.DatabaseProvider;
import com.kozzztya.cycletraining.db.Trainings;
import com.kozzztya.cycletraining.utils.DateUtils;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;
import com.roomorama.caldroid.CaldroidListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import hirondelle.date4j.DateTime;

public class TrainingCalendarFragment extends CaldroidFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_TRAININGS = 0;

    /**
     * Training day is done only if all trainings are done
     */
    private static final String COLUMN_DAY_DONE = "min(" + Trainings.IS_DONE + ")";

    private static final String[] PROJECTION_TRAININGS = new String[]{
            Trainings._ID,
            Trainings.DATE,
            COLUMN_DAY_DONE
    };

    private TrainingCalendarCallbacks mCallbacks;

    public TrainingCalendarFragment() {
    }

    /**
     * Initializes the fragment's arguments, and returns the new instance to the client.
     *
     * @param firstDayOfWeek The first day of the week, which is displayed on the calendar.
     */
    public static Fragment newInstance(int firstDayOfWeek) {
        Bundle bundle = new Bundle();
        bundle.putInt(CaldroidFragment.START_DAY_OF_WEEK, firstDayOfWeek);

        TrainingCalendarFragment fragment = new TrainingCalendarFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getCaldroidListener() == null)
            setCaldroidListener(new CaldroidListener() {
                @Override
                public void onSelectDate(Date date, View view) {
                    mCallbacks.onSelectCalendarDate(date.getTime());
                }
            });

        getLoaderManager().initLoader(LOADER_TRAININGS, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.action_calendar);
    }

    /**
     * After calendar date change load new data
     */
    @Override
    public void setCalendarDateTime(DateTime dateTime) {
        super.setCalendarDateTime(dateTime);
        getLoaderManager().restartLoader(LOADER_TRAININGS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CaldroidGridAdapter datesAdapter = getNewDatesGridAdapter(month, year);
        ArrayList<DateTime> datetimeList = datesAdapter.getDatetimeList();

        String dateFrom = datetimeList.get(0).format("'YYYY-MM-DD'");
        String dateTo = datetimeList.get(datetimeList.size() - 1).format("'YYYY-MM-DD'");

        String selection = Trainings.DATE + " >= " + dateFrom + " AND " +
                Trainings.DATE + " <= " + dateTo + ") GROUP BY (" + Trainings.DATE;
        return new CursorLoader(getActivity(), DatabaseProvider.TRAININGS_URI, PROJECTION_TRAININGS, selection, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        markTrainingCalendar(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void markTrainingCalendar(Cursor cursor) {
        HashMap<Date, Integer> mBackgroundForDateMap = new HashMap<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Date date = DateUtils.safeParse(cursor.getString(cursor.getColumnIndex(Trainings.DATE)));
                boolean isDone = cursor.getInt(cursor.getColumnIndex(COLUMN_DAY_DONE)) != 0;
                switch (DateUtils.getTrainingStatus(new java.sql.Date(date.getTime()), isDone)) {
                    case DateUtils.STATUS_DONE:
                        mBackgroundForDateMap.put(date, R.color.green);
                        break;
                    case DateUtils.STATUS_IN_PLANS:
                        mBackgroundForDateMap.put(date, R.color.light_gray);
                        break;
                    case DateUtils.STATUS_MISSED:
                        mBackgroundForDateMap.put(date, R.color.red);
                        break;
                }
            }
            while (cursor.moveToNext());
        }
        setBackgroundResourceForDates(mBackgroundForDateMap);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (TrainingCalendarCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + TrainingCalendarCallbacks.class.getSimpleName());
        }
    }

    public interface TrainingCalendarCallbacks {
        public void onSelectCalendarDate(long trainingDay);
    }
}
