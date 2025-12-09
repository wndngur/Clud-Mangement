package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.MemberAdapter;
import com.example.clubmanagement.adapters.WithdrawalRequestAdapter;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.models.WithdrawalRequest;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MemberManagementActivity extends BaseActivity {

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;

    // Views
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private LinearLayout layoutMembers;
    private LinearLayout layoutJoinRequests;
    private LinearLayout layoutLeaveRequests;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;

    // RecyclerViews
    private RecyclerView rvMembers;
    private RecyclerView rvJoinRequests;
    private RecyclerView rvLeaveRequests;

    // Adapters
    private MemberAdapter membersAdapter;
    private MemberAdapter joinRequestsAdapter;
    private WithdrawalRequestAdapter withdrawalRequestAdapter;

    // Data
    private List<Member> membersList = new ArrayList<>();
    private List<Member> joinRequestsList = new ArrayList<>();
    private List<WithdrawalRequest> withdrawalRequestsList = new ArrayList<>();

    private int currentTab = 0; // 0: members, 1: join requests, 2: leave requests

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_management);

        firebaseManager = FirebaseManager.getInstance();

        // Get data from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        android.util.Log.d("MemberManagement", "onCreate - clubId: " + clubId + ", clubName: " + clubName);

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupBottomNavigation();
        loadMembers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        layoutMembers = findViewById(R.id.layoutMembers);
        layoutJoinRequests = findViewById(R.id.layoutJoinRequests);
        layoutLeaveRequests = findViewById(R.id.layoutLeaveRequests);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        progressBar = findViewById(R.id.progressBar);

        rvMembers = findViewById(R.id.rvMembers);
        rvJoinRequests = findViewById(R.id.rvJoinRequests);
        rvLeaveRequests = findViewById(R.id.rvLeaveRequests);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("명단 관리");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // Members RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        membersAdapter = new MemberAdapter(membersList, MemberAdapter.TYPE_MEMBERS, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onGrantAdmin(Member member) {
                showGrantAdminDialog(member);
            }

            @Override
            public void onRevokeAdmin(Member member) {
                showRevokeAdminDialog(member);
            }

            @Override
            public void onExpelMember(Member member) {
                showExpelMemberDialog(member);
            }

            @Override
            public void onApprove(Member member) {
                // Not used for members
            }

            @Override
            public void onReject(Member member) {
                // Not used for members
            }

            @Override
            public void onSetRole(Member member) {
                showSetRoleDialog(member);
            }
        });
        rvMembers.setAdapter(membersAdapter);

        // Join Requests RecyclerView
        rvJoinRequests.setLayoutManager(new LinearLayoutManager(this));
        joinRequestsAdapter = new MemberAdapter(joinRequestsList, MemberAdapter.TYPE_JOIN_REQUESTS, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onGrantAdmin(Member member) {
                // Not used for join requests
            }

            @Override
            public void onRevokeAdmin(Member member) {
                // Not used for join requests
            }

            @Override
            public void onExpelMember(Member member) {
                // Not used for join requests
            }

            @Override
            public void onApprove(Member member) {
                approveJoinRequest(member);
            }

            @Override
            public void onReject(Member member) {
                rejectJoinRequest(member);
            }

            @Override
            public void onSetRole(Member member) {
                // Not used for join requests
            }
        });
        rvJoinRequests.setAdapter(joinRequestsAdapter);

        // Leave Requests RecyclerView (Withdrawal Requests with Accordion)
        rvLeaveRequests.setLayoutManager(new LinearLayoutManager(this));
        withdrawalRequestAdapter = new WithdrawalRequestAdapter(withdrawalRequestsList, new WithdrawalRequestAdapter.OnWithdrawalActionListener() {
            @Override
            public void onApprove(WithdrawalRequest request) {
                approveWithdrawalRequest(request);
            }

            @Override
            public void onReject(WithdrawalRequest request) {
                rejectWithdrawalRequest(request);
            }
        });
        rvLeaveRequests.setAdapter(withdrawalRequestAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_members) {
                currentTab = 0;
                showMembersTab();
                return true;
            } else if (itemId == R.id.nav_join_requests) {
                currentTab = 1;
                showJoinRequestsTab();
                return true;
            } else if (itemId == R.id.nav_leave_requests) {
                currentTab = 2;
                showLeaveRequestsTab();
                return true;
            }
            return false;
        });
    }

    private void showMembersTab() {
        layoutMembers.setVisibility(View.VISIBLE);
        layoutJoinRequests.setVisibility(View.GONE);
        layoutLeaveRequests.setVisibility(View.GONE);

        if (membersList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("동아리원이 없습니다");
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showJoinRequestsTab() {
        layoutMembers.setVisibility(View.GONE);
        layoutJoinRequests.setVisibility(View.VISIBLE);
        layoutLeaveRequests.setVisibility(View.GONE);

        if (joinRequestsList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("가입 요청이 없습니다");
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showLeaveRequestsTab() {
        layoutMembers.setVisibility(View.GONE);
        layoutJoinRequests.setVisibility(View.GONE);
        layoutLeaveRequests.setVisibility(View.VISIBLE);

        if (withdrawalRequestsList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("탈퇴 요청이 없습니다");
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void loadMembers() {
        progressBar.setVisibility(View.VISIBLE);

        // Load club members
        firebaseManager.getClubMembers(clubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(List<Member> members) {
                membersList.clear();
                membersList.addAll(members);
                membersAdapter.notifyDataSetChanged();

                loadJoinRequests();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    "멤버 목록을 불러오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                loadJoinRequests();
            }
        });
    }

    private void loadJoinRequests() {
        android.util.Log.d("MemberManagement", "loadJoinRequests - clubId: " + clubId);
        firebaseManager.getJoinRequests(clubId, new FirebaseManager.MembersCallback() {
            @Override
            public void onSuccess(List<Member> members) {
                android.util.Log.d("MemberManagement", "loadJoinRequests onSuccess - count: " + members.size());
                for (Member m : members) {
                    android.util.Log.d("MemberManagement", "Join request: " + m.getName() + ", userId: " + m.getUserId() + ", appId: " + m.getApplicationId());
                }
                joinRequestsList.clear();
                joinRequestsList.addAll(members);
                joinRequestsAdapter.notifyDataSetChanged();

                loadLeaveRequests();
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("MemberManagement", "loadJoinRequests onFailure: " + e.getMessage());
                Toast.makeText(MemberManagementActivity.this,
                    "가입 신청 목록을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show();
                loadLeaveRequests();
            }
        });
    }

    private void loadLeaveRequests() {
        firebaseManager.getWithdrawalRequests(clubId, new FirebaseManager.WithdrawalRequestListCallback() {
            @Override
            public void onSuccess(List<WithdrawalRequest> requests) {
                progressBar.setVisibility(View.GONE);
                withdrawalRequestsList.clear();
                withdrawalRequestsList.addAll(requests);
                withdrawalRequestAdapter.updateData(withdrawalRequestsList);

                // Show appropriate tab
                updateCurrentTabView();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    "탈퇴 요청 목록을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show();
                updateCurrentTabView();
            }
        });
    }

    private void updateCurrentTabView() {
        switch (currentTab) {
            case 0:
                showMembersTab();
                break;
            case 1:
                showJoinRequestsTab();
                break;
            case 2:
                showLeaveRequestsTab();
                break;
        }
    }

    // Admin grant dialog
    private void showGrantAdminDialog(Member member) {
        new AlertDialog.Builder(this)
            .setTitle("관리자 권한 부여")
            .setMessage(member.getName() + "님에게 동아리 관리자 권한을 부여하시겠습니까?")
            .setPositiveButton("부여", (dialog, which) -> grantAdminPermission(member))
            .setNegativeButton("취소", null)
            .show();
    }

    private void grantAdminPermission(Member member) {
        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.setMemberAdminPermission(clubId, member.getUserId(), true, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    member.getName() + "님에게 관리자 권한이 부여되었습니다", Toast.LENGTH_SHORT).show();
                member.setAdmin(true);
                membersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    "권한 부여에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Admin revoke dialog
    private void showRevokeAdminDialog(Member member) {
        new AlertDialog.Builder(this)
            .setTitle("관리자 권한 해제")
            .setMessage(member.getName() + "님의 동아리 관리자 권한을 해제하시겠습니까?")
            .setPositiveButton("해제", (dialog, which) -> revokeAdminPermission(member))
            .setNegativeButton("취소", null)
            .show();
    }

    private void revokeAdminPermission(Member member) {
        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.setMemberAdminPermission(clubId, member.getUserId(), false, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    member.getName() + "님의 관리자 권한이 해제되었습니다", Toast.LENGTH_SHORT).show();
                member.setAdmin(false);
                membersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    "권한 해제에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Expel member dialog (with reason input)
    private void showExpelMemberDialog(Member member) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expulsion_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etExpulsionReason);

        new AlertDialog.Builder(this)
            .setTitle("동아리원 퇴출")
            .setView(dialogView)
            .setPositiveButton("퇴출", (dialog, which) -> {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "퇴출 사유를 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                expelMemberWithReason(member, reason);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void expelMemberWithReason(Member member, String reason) {
        progressBar.setVisibility(View.VISIBLE);

        // Get club name for the expulsion record
        String clubDisplayName = clubName != null ? clubName : clubId;

        firebaseManager.expelMemberWithReason(clubId, clubDisplayName, member.getUserId(), reason, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    member.getName() + "님이 퇴출되었습니다", Toast.LENGTH_SHORT).show();
                membersList.remove(member);
                membersAdapter.notifyDataSetChanged();
                updateCurrentTabView();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    "퇴출에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Join request approve/reject
    private void approveJoinRequest(Member member) {
        progressBar.setVisibility(View.VISIBLE);

        // membershipApplications에서 온 신청인지 확인
        if (member.getApplicationId() != null && !member.getApplicationId().isEmpty()) {
            // membershipApplications 사용
            firebaseManager.approveMembershipApplication(clubId, member.getApplicationId(), new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MemberManagementActivity.this,
                        member.getName() + "님의 가입이 승인되었습니다", Toast.LENGTH_SHORT).show();
                    joinRequestsList.remove(member);
                    joinRequestsAdapter.notifyDataSetChanged();

                    // Add to members list
                    member.setAdmin(false);
                    membersList.add(member);
                    membersAdapter.notifyDataSetChanged();

                    updateCurrentTabView();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MemberManagementActivity.this,
                        "승인에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 기존 join_requests 사용
            firebaseManager.approveJoinRequest(clubId, member.getUserId(), new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MemberManagementActivity.this,
                        member.getName() + "님의 가입이 승인되었습니다", Toast.LENGTH_SHORT).show();
                    joinRequestsList.remove(member);
                    joinRequestsAdapter.notifyDataSetChanged();

                    // Add to members list
                    member.setAdmin(false);
                    membersList.add(member);
                    membersAdapter.notifyDataSetChanged();

                    updateCurrentTabView();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MemberManagementActivity.this,
                        "승인에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void rejectJoinRequest(Member member) {
        new AlertDialog.Builder(this)
            .setTitle("가입 거절")
            .setMessage(member.getName() + "님의 가입 신청을 거절하시겠습니까?")
            .setPositiveButton("거절", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);

                // membershipApplications에서 온 신청인지 확인
                if (member.getApplicationId() != null && !member.getApplicationId().isEmpty()) {
                    // membershipApplications 사용
                    firebaseManager.rejectMembershipApplication(clubId, member.getApplicationId(), "관리자 거절", new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MemberManagementActivity.this,
                                member.getName() + "님의 가입이 거절되었습니다", Toast.LENGTH_SHORT).show();
                            joinRequestsList.remove(member);
                            joinRequestsAdapter.notifyDataSetChanged();
                            updateCurrentTabView();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MemberManagementActivity.this,
                                "거절에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // 기존 join_requests 사용
                    firebaseManager.rejectJoinRequest(clubId, member.getUserId(), new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MemberManagementActivity.this,
                                member.getName() + "님의 가입이 거절되었습니다", Toast.LENGTH_SHORT).show();
                            joinRequestsList.remove(member);
                            joinRequestsAdapter.notifyDataSetChanged();
                            updateCurrentTabView();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MemberManagementActivity.this,
                                "거절에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    // Withdrawal request approve/reject (using new WithdrawalRequest model)
    private void approveWithdrawalRequest(WithdrawalRequest request) {
        new AlertDialog.Builder(this)
            .setTitle("탈퇴 승인")
            .setMessage(request.getUserName() + "님의 탈퇴 요청을 승인하시겠습니까?\n\n이 회원은 동아리에서 제외됩니다.")
            .setPositiveButton("승인", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                firebaseManager.approveWithdrawalRequest(request.getId(), clubId, request.getUserId(), new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MemberManagementActivity.this,
                            request.getUserName() + "님의 탈퇴가 승인되었습니다", Toast.LENGTH_SHORT).show();
                        withdrawalRequestsList.remove(request);
                        withdrawalRequestAdapter.updateData(withdrawalRequestsList);

                        // Remove from members list
                        membersList.removeIf(m -> m.getUserId().equals(request.getUserId()));
                        membersAdapter.notifyDataSetChanged();

                        updateCurrentTabView();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MemberManagementActivity.this,
                            "탈퇴 승인에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void rejectWithdrawalRequest(WithdrawalRequest request) {
        new AlertDialog.Builder(this)
            .setTitle("탈퇴 거절")
            .setMessage(request.getUserName() + "님의 탈퇴 요청을 거절하시겠습니까?\n\n해당 회원은 동아리원으로 유지됩니다.")
            .setPositiveButton("거절", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);
                firebaseManager.rejectWithdrawalRequest(request.getId(), new FirebaseManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MemberManagementActivity.this,
                            request.getUserName() + "님의 탈퇴 요청이 거절되었습니다", Toast.LENGTH_SHORT).show();
                        withdrawalRequestsList.remove(request);
                        withdrawalRequestAdapter.updateData(withdrawalRequestsList);
                        updateCurrentTabView();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MemberManagementActivity.this,
                            "거절에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("취소", null)
            .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 직급 설정 다이얼로그
    private void showSetRoleDialog(Member member) {
        String[] roles = {"부회장", "총무", "회계", "회원"};

        new AlertDialog.Builder(this)
            .setTitle("직급 설정")
            .setItems(roles, (dialog, which) -> {
                String selectedRole = roles[which];
                setMemberRole(member, selectedRole);
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void setMemberRole(Member member, String role) {
        progressBar.setVisibility(View.VISIBLE);
        firebaseManager.setMemberRole(clubId, member.getUserId(), role, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    member.getName() + "님의 직급이 '" + role + "'(으)로 설정되었습니다", Toast.LENGTH_SHORT).show();
                member.setRole(role);
                membersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MemberManagementActivity.this,
                    "직급 설정에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
