package com.itismob.s03.group7.leef;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.auth.SessionRouter;
import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;
import com.itismob.s03.group7.leef.databinding.ActivityMainBinding;
import com.itismob.s03.group7.leef.feedback.FeedbackPrompter;
import com.itismob.s03.group7.leef.messages.MessagesFragment;
import com.itismob.s03.group7.leef.myevents.MyEventsFragment;
import com.itismob.s03.group7.leef.onboarding.OnboardingActivity;
import com.itismob.s03.group7.leef.profile.ProfileFragment;
import com.itismob.s03.group7.leef.reminder.ReminderSync;
import com.itismob.s03.group7.leef.search.SearchActivity;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * The app shell once someone is signed in: the current tab's screen above the bottom navigation bar.
 * Home is the first tab (Screen 4); Explore opens Search & Filter (Screen 5); My Events is the second
 * tab (Screen 8); Messages is the fourth (Screen 10); Profile is the last tab.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_MY_EVENTS = "my_events";
    private static final String TAG_MESSAGES = "messages";
    private static final String TAG_PROFILE = "profile";
    /** Home is the fragment declared in the layout; it has no tag. */
    private static final String TAB_HOME = "";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);

        AccountRepository accounts = new FirebaseAccountRepository();
        if (!accounts.isSignedIn()) {
            // Signed out (for example the account was removed): back to the welcome screen.
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Screens draw behind the status bar; keep their content below it. The bottom bar pads itself
        // for the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        binding.bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        // Back from My Events goes to Home instead of leaving the app.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int selected = binding.bottomNav.getSelectedItemId();
                if (selected == R.id.nav_my_events || selected == R.id.nav_messages
                        || selected == R.id.nav_profile) {
                    binding.bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Make sure every event the person registered for has its reminder (new phone, reinstall...).
        ReminderSync.run(getApplicationContext());

        // Once an event is over, ask the person to rate it (once per event).
        FeedbackPrompter.run(this);

        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getProfileMissing().observe(this, missing -> {
            if (Boolean.TRUE.equals(missing)) {
                viewModel.profileMissingHandled();
                openProfileStep(accounts);
            }
        });
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            showTab(TAB_HOME);
            return true;
        }
        if (id == R.id.nav_my_events) {
            showTab(TAG_MY_EVENTS);
            return true;
        }
        if (id == R.id.nav_messages) {
            showTab(TAG_MESSAGES);
            return true;
        }
        if (id == R.id.nav_profile) {
            showTab(TAG_PROFILE);
            return true;
        }
        // Explore opens Search & Filter on top of Home, so the bar stays on Home.
        if (id == R.id.nav_explore) {
            startActivity(SearchActivity.createIntent(this, false));
        }
        return false;
    }

    /** Switches to the Profile tab (the avatar on Home opens it). */
    public void openProfileTab() {
        if (binding != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }

    /** Switches tabs. All the tabs stay alive, so Home keeps its scroll and filters. */
    private void showTab(@NonNull String tab) {
        Fragment home = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (home == null) {
            return;
        }
        Fragment myEvents = getSupportFragmentManager().findFragmentByTag(TAG_MY_EVENTS);
        Fragment messages = getSupportFragmentManager().findFragmentByTag(TAG_MESSAGES);
        Fragment profile = getSupportFragmentManager().findFragmentByTag(TAG_PROFILE);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().setReorderingAllowed(true);
        if (TAG_MY_EVENTS.equals(tab) && myEvents == null) {
            myEvents = new MyEventsFragment();
            transaction.add(R.id.fragmentContainer, myEvents, TAG_MY_EVENTS);
        }
        if (TAG_MESSAGES.equals(tab) && messages == null) {
            messages = new MessagesFragment();
            transaction.add(R.id.fragmentContainer, messages, TAG_MESSAGES);
        }
        if (TAG_PROFILE.equals(tab) && profile == null) {
            profile = new ProfileFragment();
            transaction.add(R.id.fragmentContainer, profile, TAG_PROFILE);
        }
        setTabVisible(transaction, home, TAB_HOME.equals(tab));
        setTabVisible(transaction, myEvents, TAG_MY_EVENTS.equals(tab));
        setTabVisible(transaction, messages, TAG_MESSAGES.equals(tab));
        setTabVisible(transaction, profile, TAG_PROFILE.equals(tab));
        transaction.commit();
    }

    private static void setTabVisible(@NonNull FragmentTransaction transaction, @Nullable Fragment tab,
                                      boolean visible) {
        if (tab == null) {
            return;
        }
        if (visible) {
            transaction.show(tab);
        } else {
            transaction.hide(tab);
        }
    }

    /** A signed-in account with no LEEF profile (a Google sign-in that stopped part-way) finishes it first. */
    private void openProfileStep(@NonNull AccountRepository accounts) {
        String uid = accounts.getSignedInUid();
        if (uid == null) {
            return;
        }
        startActivity(SessionRouter.profileMissingIntent(this, uid));
        finish();
    }
}
