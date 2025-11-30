package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.MemberSimpleAdapter;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.utils.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class MemberListActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;

    // Views
    private Toolbar toolbar;
    private RecyclerView rvMembers;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private TextView tvMemberCount;
    private ProgressBar progressBar;

    // Data
    private List<Member> membersList = new ArrayList<>();
    private MemberSimpleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        firebaseManager = FirebaseManager.getInstance();

        // Get data from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadMembers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvMembers = findViewById(R.id.rvMembers);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = clubName != null ? clubName + " 부원 명단" : "부원 명단";
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberSimpleAdapter(membersList);
        rvMembers.setAdapter(adapter);
    }

    private void loadMembers() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(List<Member> members) {
                progressBar.setVisibility(View.GONE);
                membersList.clear();
                membersList.addAll(members);
                adapter.notifyDataSetChanged();

                // Update member count
                tvMemberCount.setText(membersList.size() + "명");

                // Show empty view if no members
                if (membersList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvMembers.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvMembers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberListActivity.this,
                    "부원 목록을 불러오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                layoutEmpty.setVisibility(View.VISIBLE);
                tvEmptyMessage.setText("부원 목록을 불러올 수 없습니다");
                rvMembers.setVisibility(View.GONE);
            }
        });
    }
}
