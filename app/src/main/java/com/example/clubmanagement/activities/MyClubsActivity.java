package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.User;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MyClubsActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private ImageView ivBack;
    private LinearLayout llCentralClubContainer;
    private LinearLayout llGeneralClubsContainer;
    private TextView tvNoGeneralClubs;
    private ProgressBar progressBar;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_my_clubs);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupListeners();
        loadMyClubs();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        llCentralClubContainer = findViewById(R.id.llCentralClubContainer);
        llGeneralClubsContainer = findViewById(R.id.llGeneralClubsContainer);
        tvNoGeneralClubs = findViewById(R.id.tvNoGeneralClubs);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void loadMyClubs() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                progressBar.setVisibility(View.GONE);
                currentUser = user;

                if (user != null) {
                    displayCentralClub(user);
                    displayGeneralClubs(user);
                } else {
                    Toast.makeText(MyClubsActivity.this, "사용자 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MyClubsActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCentralClub(User user) {
        llCentralClubContainer.removeAllViews();

        if (user.hasJoinedCentralClub()) {
            String clubName = user.getCentralClubName();
            String clubId = user.getCentralClubId();
            addClubAccordion(clubName, clubId, "중앙 동아리",
                "메인 화면에서 관리되는 동아리입니다. 하나만 가입할 수 있습니다.",
                llCentralClubContainer, true);
        } else {
            TextView emptyView = new TextView(this);
            emptyView.setText("가입한 중앙 동아리가 없습니다.");
            emptyView.setTextSize(14);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.setPadding(0, 16, 0, 16);
            llCentralClubContainer.addView(emptyView);
        }
    }

    private void displayGeneralClubs(User user) {
        llGeneralClubsContainer.removeAllViews();

        List<String> clubIds = user.getGeneralClubIds();
        List<String> clubNames = user.getGeneralClubNames();

        if (clubIds.isEmpty()) {
            tvNoGeneralClubs.setVisibility(View.VISIBLE);
        } else {
            tvNoGeneralClubs.setVisibility(View.GONE);

            for (int i = 0; i < clubIds.size(); i++) {
                String clubId = clubIds.get(i);
                String clubName = (i < clubNames.size()) ? clubNames.get(i) : "동아리";

                addClubAccordion(clubName, clubId, "일반 동아리",
                    "중앙동아리 외에 추가로 가입한 동아리입니다. 여러 개 가입 가능합니다.",
                    llGeneralClubsContainer, false);
            }
        }
    }

    private void addClubAccordion(String clubName, String clubId, String clubType,
                                  String description, LinearLayout container, boolean isCentral) {
        View accordionView = LayoutInflater.from(this)
            .inflate(R.layout.item_my_club_accordion, container, false);

        // Find views
        LinearLayout accordionHeader = accordionView.findViewById(R.id.accordionHeader);
        LinearLayout accordionContent = accordionView.findViewById(R.id.accordionContent);
        View divider = accordionView.findViewById(R.id.divider);
        TextView tvClubName = accordionView.findViewById(R.id.tvClubName);
        TextView tvClubType = accordionView.findViewById(R.id.tvClubType);
        TextView tvClubDescription = accordionView.findViewById(R.id.tvClubDescription);
        ImageView ivExpandIcon = accordionView.findViewById(R.id.ivExpandIcon);
        MaterialButton btnGoToClub = accordionView.findViewById(R.id.btnGoToClub);

        // Set data
        tvClubName.setText(clubName);
        tvClubType.setText(clubType);
        tvClubDescription.setText(description);

        // Set click listener for accordion expansion
        accordionHeader.setOnClickListener(v -> {
            boolean isExpanded = accordionContent.getVisibility() == View.VISIBLE;

            if (isExpanded) {
                // Collapse
                accordionContent.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                ivExpandIcon.setRotation(0);
            } else {
                // Expand
                accordionContent.setVisibility(View.VISIBLE);
                divider.setVisibility(View.VISIBLE);
                ivExpandIcon.setRotation(180);
            }
        });

        // Set click listener for "Go to Club" button
        btnGoToClub.setOnClickListener(v -> {
            Intent intent = new Intent(MyClubsActivity.this, ClubMainActivity.class);
            intent.putExtra("club_name", clubName);
            intent.putExtra("club_id", clubId);
            startActivity(intent);
        });

        // Add to container
        container.addView(accordionView);
    }
}
