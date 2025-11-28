package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.google.android.material.button.MaterialButton;

public class ClubRecommendActivity extends AppCompatActivity {

    // Checkboxes
    private CheckBox cbChristian;
    private CheckBox cbLively;
    private CheckBox cbQuiet;
    private CheckBox cbVolunteer;
    private CheckBox cbSports;
    private CheckBox cbOutdoor;
    private CheckBox cbCareer;
    private CheckBox cbAcademic;
    private CheckBox cbArt;

    private MaterialButton btnShowResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_recommend);

        initViews();
        setupListeners();
    }

    private void initViews() {
        cbChristian = findViewById(R.id.cbChristian);
        cbLively = findViewById(R.id.cbLively);
        cbQuiet = findViewById(R.id.cbQuiet);
        cbVolunteer = findViewById(R.id.cbVolunteer);
        cbSports = findViewById(R.id.cbSports);
        cbOutdoor = findViewById(R.id.cbOutdoor);
        cbCareer = findViewById(R.id.cbCareer);
        cbAcademic = findViewById(R.id.cbAcademic);
        cbArt = findViewById(R.id.cbArt);

        btnShowResult = findViewById(R.id.btnShowResult);
    }

    private void setupListeners() {
        btnShowResult.setOnClickListener(v -> showRecommendationResult());
    }

    private void showRecommendationResult() {
        // Count selected checkboxes
        int selectedCount = 0;
        StringBuilder selectedOptions = new StringBuilder("선택한 옵션:\n");

        if (cbChristian.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 기독교 동아리\n");
        }
        if (cbLively.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 활기찬 분위기\n");
        }
        if (cbQuiet.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 조용한 분위기\n");
        }
        if (cbVolunteer.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 봉사활동\n");
        }
        if (cbSports.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 운동 관련\n");
        }
        if (cbOutdoor.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 외부 활동\n");
        }
        if (cbCareer.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 취업/스펙\n");
        }
        if (cbAcademic.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 학술/연구\n");
        }
        if (cbArt.isChecked()) {
            selectedCount++;
            selectedOptions.append("- 예술/문화\n");
        }

        if (selectedCount == 0) {
            Toast.makeText(this, "최소 1개 이상 선택해주세요", Toast.LENGTH_SHORT).show();
        } else {
            // TODO: 추천 로직 구현 예정
            Toast.makeText(this, selectedCount + "개 선택됨\n추천 기능은 곧 추가됩니다", Toast.LENGTH_LONG).show();
        }
    }
}
