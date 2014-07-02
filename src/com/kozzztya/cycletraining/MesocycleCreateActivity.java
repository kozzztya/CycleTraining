package com.kozzztya.cycletraining;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.kozzztya.cycletraining.db.DBHelper;
import com.kozzztya.cycletraining.db.entities.*;
import com.kozzztya.cycletraining.db.helpers.*;
import com.kozzztya.cycletraining.utils.MyDateUtils;
import com.kozzztya.cycletraining.utils.RMCalc;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import static android.view.View.OnClickListener;

public class MesocycleCreateActivity extends DrawerActivity implements OnClickListener {

    private Spinner spinnerExercise;
    private Spinner spinnerProgram;
    private Button buttonCreate;
    private Button buttonDate;
    private EditText editTextWeight;
    private EditText editTextReps;

    private Date beginDate;
    private long newMesocycleId;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.mesocycle_create);

        spinnerExercise = (Spinner) findViewById(R.id.spinnerExercise);
        spinnerProgram = (Spinner) findViewById(R.id.spinnerProgram);
        buttonCreate = (Button) findViewById(R.id.buttonCreateProgram);
        buttonDate = (Button) findViewById(R.id.buttonDate);
        editTextWeight = (EditText) findViewById(R.id.editTextWeight);
        editTextReps = (EditText) findViewById(R.id.editTextReps);

        buttonCreate.setOnClickListener(this);
        buttonDate.setOnClickListener(this);

        fillSpinners();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonDate:
                showCalendarDialog();
                break;
            case R.id.buttonCreateProgram:
                newMesocycle();
                //Show new mesocycle
                Intent intent = new Intent(this, MesocycleShowActivity.class);
                intent.putExtra("mesocycleId", newMesocycleId);
                startActivity(intent);
                break;
        }
    }

    private void fillSpinners() {
        ExercisesHelper exercisesHelper = new ExercisesHelper(this);
        ProgramsHelper programsHelper = new ProgramsHelper(this);

        List<Exercise> exercises = exercisesHelper.select(null, null, null, null);
        ArrayAdapter<Exercise> exerciseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, exercises);
        exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerExercise.setAdapter(exerciseAdapter);

        List<Program> programs = programsHelper.select(null, null, null, null);
        ArrayAdapter<Program> programAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, programs);
        programAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerProgram.setAdapter(programAdapter);
    }

    private void showCalendarDialog() {
        final CaldroidFragment dialogCaldroidFragment = new CaldroidFragment();
        Bundle bundle = new Bundle();
        bundle.putString(CaldroidFragment.DIALOG_TITLE, getString(R.string.date_dialog_title));
        bundle.putInt(CaldroidFragment.START_DAY_OF_WEEK, Preferences.getFirstDayOfWeek(this));
        dialogCaldroidFragment.setArguments(bundle);
        dialogCaldroidFragment.show(getSupportFragmentManager(), "CALDROID_DIALOG_FRAGMENT");

        dialogCaldroidFragment.setCaldroidListener(new CaldroidListener() {
            @Override
            public void onSelectDate(java.util.Date date, View view) {
                //Show chosen date on buttonDate
                beginDate = new Date(date.getTime());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                String dayOfWeekName = MyDateUtils.getDayOfWeekName(beginDate, getApplicationContext());
                buttonDate.setText(dayOfWeekName + ", " + dateFormat.format(date));

                dialogCaldroidFragment.dismiss();
            }
        });
    }

    private void newMesocycle() {
        TrainingJournalHelper trainingJournalHelper = new TrainingJournalHelper(this);
        MesocyclesHelper mesocyclesHelper = new MesocyclesHelper(this);
        TrainingsHelper trainingsHelper = new TrainingsHelper(this);
        SetsHelper setsHelper = new SetsHelper(this);

        //Get chosen program data
        long programMesocycleId = ((Program) spinnerProgram.getSelectedItem()).getMesocycle();
        Mesocycle mesocycle = mesocyclesHelper.getEntity(programMesocycleId);
        List<Training> trainings = trainingsHelper.select(TrainingsHelper.COLUMN_MESOCYCLE + " = " + programMesocycleId, null, null, null);
        List<SetView> sets = setsHelper.selectView(SetsHelper.COLUMN_MESOCYCLE + " = " + programMesocycleId, null, null, null);

        //TODO validate input
        //Insert mesocycle data from input
        float weight = Float.valueOf(editTextWeight.getText().toString());
        int reps = Integer.valueOf(editTextReps.getText().toString());
        float rm = RMCalc.maxRM(weight, reps);
        long exerciseId = ((Exercise) spinnerExercise.getSelectedItem()).getId();
        mesocycle.setRm(rm);
        mesocycle.setExercise(exerciseId);
        newMesocycleId = mesocyclesHelper.insert(mesocycle);

        //Generate trainings and sets data by chosen program and RM
        SQLiteDatabase db = DBHelper.getInstance(this).getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < trainings.size(); i++) {
                Training t = trainings.get(i);
                long oldTrainingId = t.getId();
                Training newTraining = new Training();
                newTraining.setMesocycle(newMesocycleId);
                //Generate training date
                long trainingDate = MyDateUtils.calcTrainingDate(i, mesocycle.getTrainingsInWeek(), beginDate);
                newTraining.setDate(new Date(trainingDate));
                long newTrainingId = trainingsHelper.insert(newTraining);
                for (Set s : sets) {
                    if (s.getTraining() == oldTrainingId) {
                        Set newSet = new Set();
                        newSet.setReps(s.getReps());
                        newSet.setWeight(s.getWeight() * rm);
                        newSet.setTraining(newTrainingId);
                        setsHelper.insert(newSet);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        //Add data to training journal
        long programId = ((Program) spinnerProgram.getSelectedItem()).getId();
        TrainingJournal tj = new TrainingJournal();
        tj.setProgram(programId);
        tj.setMesocycle(newMesocycleId);
        tj.setBeginDate(beginDate);
        trainingJournalHelper.insert(tj);
    }

}



















