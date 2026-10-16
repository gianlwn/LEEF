package com.itismob.s03.group7.leef;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.itismob.s03.group7.leef.auth.SessionRouter;
import com.itismob.s03.group7.leef.data.AccountRepository;
import com.itismob.s03.group7.leef.data.FirebaseAccountRepository;
import com.itismob.s03.group7.leef.databinding.ActivityMainBinding;
import com.itismob.s03.group7.leef.onboarding.OnboardingActivity;
import com.itismob.s03.group7.leef.search.SearchActivity;
import com.itismob.s03.group7.leef.ui.ComingSoon;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * The app shell once someone is signed in: the current tab's screen above the bottom navigation bar.
 * Home is the first tab (Screen 4); Explore opens Search & Filter (Screen 5); My Events, Messages and
 * Profile are added as their screens are built.
 */
public class MainActivity extends AppCompatActivity {

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
            return true;
        }
        // Explore opens Search & Filter on top of Home, so the bar stays on Home. The other tabs arrive
        // with their own screens.
        if (id == R.id.nav_explore) {
            startActivity(SearchActivity.createIntent(this, false));
        } else if (id == R.id.nav_my_events) {
            ComingSoon.show(this, R.string.feature_my_events);
        } else if (id == R.id.nav_messages) {
            ComingSoon.show(this, R.string.feature_messages);
        } else if (id == R.id.nav_profile) {
            ComingSoon.show(this, R.string.feature_profile);
        }
        return false;
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
