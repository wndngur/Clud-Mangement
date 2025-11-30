package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClubInfoEditActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private Club currentClub;
    private String clubId;
    private String clubName;

    // UI Components
    private Toolbar toolbar;
    private TextInputEditText etPurpose;
    private TextInputEditText etProfessor;
    private TextInputEditText etDepartment;
    private TextInputEditText etLocation;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    // Monthly schedule EditTexts
    private TextInputEditText[] etMonths = new TextInputEditText[13]; // Index 1-12 for months

    // Keyword UI Components
    private CheckBox cbChristian;
    private RadioGroup rgAtmosphere;
    private RadioButton rbLively;
    private RadioButton rbQuiet;
    private CheckBox cbVolunteer;
    private CheckBox cbSports;
    private CheckBox cbOutdoor;
    private CheckBox cbCareer;
    private CheckBox cbAcademic;
    private CheckBox cbArt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_info_edit);

        firebaseManager = FirebaseManager.getInstance();

        // Get club info from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadClubInfo();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etPurpose = findViewById(R.id.etPurpose);
        etProfessor = findViewById(R.id.etProfessor);
        etDepartment = findViewById(R.id.etDepartment);
        etLocation = findViewById(R.id.etLocation);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Monthly schedule EditTexts
        etMonths[1] = findViewById(R.id.etMonth1);
        etMonths[2] = findViewById(R.id.etMonth2);
        etMonths[3] = findViewById(R.id.etMonth3);
        etMonths[4] = findViewById(R.id.etMonth4);
        etMonths[5] = findViewById(R.id.etMonth5);
        etMonths[6] = findViewById(R.id.etMonth6);
        etMonths[7] = findViewById(R.id.etMonth7);
        etMonths[8] = findViewById(R.id.etMonth8);
        etMonths[9] = findViewById(R.id.etMonth9);
        etMonths[10] = findViewById(R.id.etMonth10);
        etMonths[11] = findViewById(R.id.etMonth11);
        etMonths[12] = findViewById(R.id.etMonth12);

        // Keyword views
        cbChristian = findViewById(R.id.cbChristian);
        rgAtmosphere = findViewById(R.id.rgAtmosphere);
        rbLively = findViewById(R.id.rbLively);
        rbQuiet = findViewById(R.id.rbQuiet);
        cbVolunteer = findViewById(R.id.cbVolunteer);
        cbSports = findViewById(R.id.cbSports);
        cbOutdoor = findViewById(R.id.cbOutdoor);
        cbCareer = findViewById(R.id.cbCareer);
        cbAcademic = findViewById(R.id.cbAcademic);
        cbArt = findViewById(R.id.cbArt);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("동아리 정보 수정");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadClubInfo() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                progressBar.setVisibility(View.GONE);

                if (club != null) {
                    currentClub = club;
                    displayClubInfo();
                } else {
                    // Club doesn't exist yet, create empty one
                    currentClub = new Club(clubId, clubName != null ? clubName : "동아리");
                    displayClubInfo();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubInfoEditActivity.this, "동아리 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayClubInfo() {
        if (currentClub == null) return;

        if (currentClub.getPurpose() != null) {
            etPurpose.setText(currentClub.getPurpose());
        }

        if (currentClub.getProfessor() != null) {
            etProfessor.setText(currentClub.getProfessor());
        }

        // Monthly schedule
        for (int month = 1; month <= 12; month++) {
            String schedule = currentClub.getScheduleForMonth(month);
            if (schedule != null && !schedule.isEmpty()) {
                etMonths[month].setText(schedule);
            }
        }

        if (currentClub.getDepartment() != null) {
            etDepartment.setText(currentClub.getDepartment());
        }

        if (currentClub.getLocation() != null) {
            etLocation.setText(currentClub.getLocation());
        }

        // Keywords
        cbChristian.setChecked(currentClub.isChristian());

        String atmosphere = currentClub.getAtmosphere();
        if ("lively".equals(atmosphere)) {
            rbLively.setChecked(true);
        } else if ("quiet".equals(atmosphere)) {
            rbQuiet.setChecked(true);
        }

        List<String> activityTypes = currentClub.getActivityTypes();
        if (activityTypes != null) {
            cbVolunteer.setChecked(activityTypes.contains("volunteer"));
            cbSports.setChecked(activityTypes.contains("sports"));
            cbOutdoor.setChecked(activityTypes.contains("outdoor"));
        }

        List<String> purposes = currentClub.getPurposes();
        if (purposes != null) {
            cbCareer.setChecked(purposes.contains("career"));
            cbAcademic.setChecked(purposes.contains("academic"));
            cbArt.setChecked(purposes.contains("art"));
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveClubInfo());

        // 활동 유형 - 최대 2개 선택 제한
        CheckBox[] activityCheckboxes = {cbVolunteer, cbSports, cbOutdoor};
        for (CheckBox cb : activityCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && countChecked(activityCheckboxes) > 2) {
                    buttonView.setChecked(false);
                    Toast.makeText(this, "활동 유형은 최대 2개까지 선택 가능합니다", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 목적 - 최대 2개 선택 제한
        CheckBox[] purposeCheckboxes = {cbCareer, cbAcademic, cbArt};
        for (CheckBox cb : purposeCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && countChecked(purposeCheckboxes) > 2) {
                    buttonView.setChecked(false);
                    Toast.makeText(this, "목적은 최대 2개까지 선택 가능합니다", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private int countChecked(CheckBox[] checkboxes) {
        int count = 0;
        for (CheckBox cb : checkboxes) {
            if (cb.isChecked()) count++;
        }
        return count;
    }

    private void saveClubInfo() {
        // Get values from EditText fields
        String purpose = etPurpose.getText() != null ? etPurpose.getText().toString().trim() : "";
        String professor = etProfessor.getText() != null ? etProfessor.getText().toString().trim() : "";
        String department = etDepartment.getText() != null ? etDepartment.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

        // Update club object
        if (currentClub == null) {
            currentClub = new Club(clubId, clubName != null ? clubName : "동아리");
        }

        currentClub.setPurpose(purpose);
        currentClub.setProfessor(professor);
        currentClub.setDepartment(department);
        currentClub.setLocation(location);

        // Monthly schedule
        Map<String, String> monthlySchedule = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            if (etMonths[month] != null && etMonths[month].getText() != null) {
                String schedule = etMonths[month].getText().toString().trim();
                if (!schedule.isEmpty()) {
                    monthlySchedule.put(String.valueOf(month), schedule);
                }
            }
        }
        currentClub.setMonthlySchedule(monthlySchedule);

        // Also update the legacy schedule field with combined monthly schedules
        currentClub.setSchedule(currentClub.getMonthlyScheduleAsString());

        // Keywords
        currentClub.setChristian(cbChristian.isChecked());

        // Atmosphere
        if (rbLively.isChecked()) {
            currentClub.setAtmosphere("lively");
        } else if (rbQuiet.isChecked()) {
            currentClub.setAtmosphere("quiet");
        } else {
            currentClub.setAtmosphere(null);
        }

        // Activity types
        List<String> activityTypes = new ArrayList<>();
        if (cbVolunteer.isChecked()) activityTypes.add("volunteer");
        if (cbSports.isChecked()) activityTypes.add("sports");
        if (cbOutdoor.isChecked()) activityTypes.add("outdoor");
        currentClub.setActivityTypes(activityTypes);

        // Purposes
        List<String> purposes = new ArrayList<>();
        if (cbCareer.isChecked()) purposes.add("career");
        if (cbAcademic.isChecked()) purposes.add("academic");
        if (cbArt.isChecked()) purposes.add("art");
        currentClub.setPurposes(purposes);

        // Save to Firebase
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        firebaseManager.saveClub(currentClub, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(ClubInfoEditActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSave.setEnabled(true);
                Toast.makeText(ClubInfoEditActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
