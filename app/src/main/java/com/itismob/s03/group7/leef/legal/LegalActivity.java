package com.itismob.s03.group7.leef.legal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.itismob.s03.group7.leef.R;
import com.itismob.s03.group7.leef.databinding.ActivityLegalBinding;
import com.itismob.s03.group7.leef.databinding.ItemLegalSectionBinding;
import com.itismob.s03.group7.leef.ui.LeefEdgeToEdge;

/**
 * Shows the Terms of Service or the Privacy Policy as a readable, scrolling page. Opened from the
 * "I agree to the Terms of Service and Privacy Policy" line on Create Account and Complete Profile.
 */
public class LegalActivity extends AppCompatActivity {

    private static final String EXTRA_DOCUMENT = "document";

    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull LegalDocument document) {
        return new Intent(context, LegalActivity.class).putExtra(EXTRA_DOCUMENT, document.name());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeefEdgeToEdge.enable(this);
        LegalDocument document = LegalDocument.fromName(getIntent().getStringExtra(EXTRA_DOCUMENT));

        ActivityLegalBinding binding = ActivityLegalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.legalRoot, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.backButton.setOnClickListener(v -> finish());
        binding.legalTitle.setText(document.getTitleRes());
        ViewCompat.setAccessibilityHeading(binding.legalTitle, true);

        String[] headings = getResources().getStringArray(document.getHeadingsRes());
        String[] bodies = getResources().getStringArray(document.getBodiesRes());
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = Math.min(headings.length, bodies.length);
        for (int i = 0; i < count; i++) {
            ItemLegalSectionBinding section = ItemLegalSectionBinding.inflate(inflater, binding.sections, false);
            section.heading.setText(getString(R.string.legal_section_heading, i + 1, headings[i]));
            section.body.setText(bodies[i]);
            ViewCompat.setAccessibilityHeading(section.heading, true);
            binding.sections.addView(section.getRoot());
        }
    }
}
